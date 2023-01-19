package cloud.filibuster.instrumentation.libraries.grpc;

import cloud.filibuster.exceptions.FilibusterFaultInjectionException;
import cloud.filibuster.instrumentation.datatypes.Callsite;
import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;
import cloud.filibuster.instrumentation.storage.ContextStorage;
import cloud.filibuster.instrumentation.storage.ThreadLocalContextStorage;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationEnabledProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationServerCommunicationEnabledProperty;
import static java.util.Objects.requireNonNull;

public class FilibusterClientInterceptor implements ClientInterceptor {
    private static final Logger logger = Logger.getLogger(FilibusterClientInterceptor.class.getName());

    @SuppressWarnings("FieldCanBeFinal")
    protected String serviceName;

    @SuppressWarnings("FieldCanBeFinal")
    protected ContextStorage contextStorage;

    public static Boolean disableServerCommunication = false;
    public static Boolean disableInstrumentation = false;

    private static boolean shouldInstrument() {
        if (getInstrumentationEnabledProperty() && !disableInstrumentation) {
            return true;
        }

        return false;
    }

    private static boolean shouldCommunicateWithServer() {
        if (getInstrumentationServerCommunicationEnabledProperty() && !disableServerCommunication) {
            return true;
        }

        return false;
    }

    private static Status generateCorrectStatusForAbort(FilibusterClientInstrumentor filibusterClientInstrumentor) {
        JSONObject forcedException = filibusterClientInstrumentor.getForcedException();
        JSONObject forcedExceptionMetadata = forcedException.getJSONObject("metadata");
        String codeStr = forcedExceptionMetadata.getString("code");
        Status.Code code = Status.Code.valueOf(codeStr);
        Status status = Status.fromCode(code);
        return status;
    }

    @SuppressWarnings("Varifier")
    public static Status generateExceptionFromForcedException(FilibusterClientInstrumentor filibusterClientInstrumentor) {
        JSONObject forcedException = filibusterClientInstrumentor.getForcedException();
        requireNonNull(forcedException);

        // Get description of the fault.
        String exceptionNameString = forcedException.getString("name");
        JSONObject forcedExceptionMetadata = forcedException.getJSONObject("metadata");
        String causeString = forcedExceptionMetadata.getString("cause");
        String codeStr = forcedExceptionMetadata.getString("code");

        // Status object to return to the user.
        Status status;

        if (!causeString.isEmpty()) {
            // Cause always takes priority in gRPC because it implies a UNKNOWN response.
            try {
                Throwable throwable = (Throwable) Class.forName(causeString).getConstructor(new Class[] { String.class }).newInstance("Filibuster generated exception.");
                status = Status.fromThrowable(throwable);
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException e) {
                throw new FilibusterFaultInjectionException("Unable to generate custom exception from string '" + causeString + "':" + e, e);
            }
        } else if (!codeStr.isEmpty()) {
            // Code is checked secondary and ignored if cause is present.
            Status.Code code = Status.Code.valueOf(codeStr);
            status = Status.fromCode(code);
        } else {
            // Otherwise, we do not know what to inject.
            throw new FilibusterFaultInjectionException("No code or cause provided for injection of io.grpc.StatusRuntimeException.");
        }

        // Notify Filibuster of failure.
        HashMap<String, String> additionalMetadata = new HashMap<>();
        additionalMetadata.put("code", codeStr);
        filibusterClientInstrumentor.afterInvocationWithException(exceptionNameString, causeString, additionalMetadata);

        return status;
    }

    private static Status generateExceptionFromFailureMetadata(FilibusterClientInstrumentor filibusterClientInstrumentor) {
        JSONObject failureMetadata = filibusterClientInstrumentor.getFailureMetadata();
        requireNonNull(failureMetadata);

        JSONObject exception = failureMetadata.getJSONObject("exception");
        JSONObject exceptionMetadata = exception.getJSONObject("metadata");

        // Create the exception to throw.
        String exceptionNameString = "io.grpc.StatusRuntimeException";
        String codeStr = exceptionMetadata.getString("code");
        Status.Code code = Status.Code.valueOf(codeStr);
        // Ignored cause here because this call path is used for cross-language failures and cause doesn't propagate across RPC boundaries.
        String causeString = "";

        // Notify Filibuster of failure.
        HashMap<String, String> additionalMetadata = new HashMap<>();
        additionalMetadata.put("name", exceptionNameString);
        additionalMetadata.put("code", codeStr);
        filibusterClientInstrumentor.afterInvocationWithException(exceptionNameString, causeString, additionalMetadata);

        // Return status.
        return Status.fromCode(code);
    }

    public FilibusterClientInterceptor() {
        this.serviceName = System.getenv("SERVICE_NAME");
        this.contextStorage = new ThreadLocalContextStorage();
    }

    public FilibusterClientInterceptor(String serviceName) {
        this.serviceName = serviceName;
        this.contextStorage = new ThreadLocalContextStorage();
    }

    @Override
    @SuppressWarnings("UnnecessaryFinal")
    public <REQUEST, RESPONSE> ClientCall<REQUEST, RESPONSE> interceptCall(
            MethodDescriptor<REQUEST, RESPONSE> method, CallOptions callOptions, Channel next) {

        if (method.getType() != MethodDescriptor.MethodType.UNARY) {
            return next.newCall(method, callOptions);
        }

        // return the filibuster client interceptor.
        return new ForwardingClientCall<REQUEST, RESPONSE>() {
            @Nullable
            private ClientCall<REQUEST, RESPONSE> delegate;
            private Listener<RESPONSE> responseListener;

            @Nullable
            private Metadata headers;
            private int requestTokens;
            private FilibusterClientInstrumentor filibusterClientInstrumentor;

            @Override protected ClientCall<REQUEST, RESPONSE> delegate() {
                if (delegate == null) {
                    throw new UnsupportedOperationException();
                }
                return delegate;
            }

            @Override
            public void start(Listener<RESPONSE> responseListener, Metadata headers) {
                logger.log(Level.INFO, "INSIDE: start!");

                this.headers = headers;
                this.responseListener = responseListener;
            }

            @Override
            public void request(int requests) {
                if (delegate == null) {
                    requestTokens += requests;
                    return;
                }
                super.request(requests);
            }

            // This method is invoked with the message from the Client to the Service.
            // message: type of the message issued from the Client (e.g., Hello$HelloRequest)
            @Override
            public void sendMessage(REQUEST message) {
                logger.log(Level.INFO, "INSIDE: sendMessage!");
                logger.log(Level.INFO, "message: " + message.toString());

                // ******************************************************************************************
                // Figure out if we are inside of instrumentation.
                // ******************************************************************************************

                String instrumentationRequestStr = headers.get(
                        Metadata.Key.of("x-filibuster-instrumentation", Metadata.ASCII_STRING_MARSHALLER));
                logger.log(Level.INFO, "instrumentationRequestStr: " + instrumentationRequestStr);
                boolean instrumentationRequest = Boolean.parseBoolean(instrumentationRequestStr);
                logger.log(Level.INFO, "instrumentationRequest: " + instrumentationRequest);

                if (! shouldInstrument() || instrumentationRequest) {
                    delegate = next.newCall(method, callOptions);
                    super.start(responseListener, headers);
                    headers = null;
                    if (requestTokens > 0) {
                        super.request(requestTokens);
                        requestTokens = 0;
                    }
                } else {
                    // ******************************************************************************************
                    // Extract callsite information.
                    // ******************************************************************************************

                    String grpcFullMethodName = method.getFullMethodName();
                    String grpcServiceName = grpcFullMethodName.substring(0, grpcFullMethodName.indexOf("/"));
                    String grpcRpcName = grpcFullMethodName.replace(grpcServiceName + "/", "");

                    logger.log(Level.INFO, "method: " + method);
                    logger.log(Level.INFO, "grpcFullMethodName: " + grpcFullMethodName);
                    logger.log(Level.INFO, "grpcServiceName: " + grpcServiceName);
                    logger.log(Level.INFO, "grpcRpcName: " + grpcRpcName);

                    // ******************************************************************************************
                    // Construct preliminary call site information.
                    // ******************************************************************************************

                    Callsite callsite = new Callsite(
                            serviceName,
                            grpcServiceName,
                            grpcFullMethodName,
                            message.toString()
                    );

                    // ******************************************************************************************
                    // Prepare for invocation.
                    // ******************************************************************************************

                    this.filibusterClientInstrumentor = new FilibusterClientInstrumentor(
                            serviceName,
                            shouldCommunicateWithServer(),
                            contextStorage,
                            callsite
                    );
                    filibusterClientInstrumentor.prepareForInvocation();

                    // ******************************************************************************************
                    // Record invocation.
                    // ******************************************************************************************

                    filibusterClientInstrumentor.beforeInvocation();

                    // ******************************************************************************************
                    // Attach metadata to outgoing request.
                    // ******************************************************************************************

                    logger.log(Level.INFO, "requestId: " + filibusterClientInstrumentor.getOutgoingRequestId());

                    if (filibusterClientInstrumentor.getOutgoingRequestId() != null) {
                        headers.put(
                                Metadata.Key.of("x-filibuster-request-id", Metadata.ASCII_STRING_MARSHALLER),
                                filibusterClientInstrumentor.getOutgoingRequestId()
                        );
                    }

                    if (filibusterClientInstrumentor.getGeneratedId() > -1) {
                        headers.put(
                                Metadata.Key.of("x-filibuster-generated-id", Metadata.ASCII_STRING_MARSHALLER),
                                String.valueOf(filibusterClientInstrumentor.getGeneratedId())
                        );
                    }

                    headers.put(
                            Metadata.Key.of("x-filibuster-vclock", Metadata.ASCII_STRING_MARSHALLER),
                            filibusterClientInstrumentor.getVectorClock().toString()
                    );
                    headers.put(
                            Metadata.Key.of("x-filibuster-origin-vclock", Metadata.ASCII_STRING_MARSHALLER),
                            filibusterClientInstrumentor.getOriginVectorClock().toString()
                    );
                    headers.put(
                            Metadata.Key.of("x-filibuster-execution-index", Metadata.ASCII_STRING_MARSHALLER),
                            filibusterClientInstrumentor.getDistributedExecutionIndex().toString()
                    );

                    String x = filibusterClientInstrumentor.getDistributedExecutionIndex().toString();

                    // ******************************************************************************************
                    // Get failure information.
                    // ******************************************************************************************

                    JSONObject forcedException = filibusterClientInstrumentor.getForcedException();
                    JSONObject failureMetadata = filibusterClientInstrumentor.getFailureMetadata();

                    logger.log(Level.INFO, "forcedException: " + forcedException);
                    logger.log(Level.INFO, "failureMetadata: " + failureMetadata);

                    // ******************************************************************************************
                    // Setup additional failure headers, if necessary.
                    // ******************************************************************************************

                    if (forcedException != null) {
                        JSONObject forcedExceptionMetadata = forcedException.getJSONObject("metadata");

                        if (forcedExceptionMetadata.has("sleep")) {
                            int sleepInterval = forcedExceptionMetadata.getInt("sleep");
                            headers.put(
                                    Metadata.Key.of("x-filibuster-forced-sleep", Metadata.ASCII_STRING_MARSHALLER),
                                    String.valueOf(sleepInterval)
                            );
                        } else {
                            headers.put(
                                    Metadata.Key.of("x-filibuster-forced-sleep", Metadata.ASCII_STRING_MARSHALLER),
                                    String.valueOf(0)
                            );
                        }
                    }

                    // ******************************************************************************************
                    // If we need to override the response, do it now before proceeding.
                    // ******************************************************************************************

                    if (failureMetadata != null && filibusterClientInstrumentor.shouldAbort()) {
                        delegate = new NoopClientCall<REQUEST, RESPONSE>();
                        Status status = generateExceptionFromFailureMetadata(filibusterClientInstrumentor);
                        responseListener.onClose(status, new Metadata());
                        return;
                    }

                    // ******************************************************************************************
                    // If we need to throw, this is where we throw.
                    // ******************************************************************************************

                    if (forcedException != null && filibusterClientInstrumentor.shouldAbort()) {
                        delegate = new NoopClientCall<REQUEST, RESPONSE>();
                        Status status = generateExceptionFromForcedException(filibusterClientInstrumentor);
                        responseListener.onClose(status, new Metadata());
                        return;
                    }

                    delegate = next.newCall(method, callOptions);
                    super.start(new FilibusterClientCallListener<>(responseListener, filibusterClientInstrumentor), headers);
                    headers = null;
                    if (requestTokens > 0) {
                        super.request(requestTokens);
                        requestTokens = 0;
                    }
                }

                super.sendMessage(message);
            }
        };
    }

    // *********************************************************************
    // Client caller listener.

    @SuppressWarnings("ClassCanBeStatic")
    final class FilibusterClientCallListener<RESPONSE>
            extends ForwardingClientCallListener.SimpleForwardingClientCallListener<RESPONSE> {

        private final FilibusterClientInstrumentor filibusterClientInstrumentor;

        FilibusterClientCallListener(ClientCall.Listener<RESPONSE> delegate,
                                     FilibusterClientInstrumentor filibusterClientInstrumentor) {
            super(delegate);
            this.filibusterClientInstrumentor = filibusterClientInstrumentor;
        }

        // invoked on successful response with the message from the Server to the Client
        // message: type of message issued from the Server to the Client (e.g., Hello$HelloReply)
        @Override
        public void onMessage(RESPONSE message) {
            logger.log(Level.INFO, "INSIDE: onMessage!");
            logger.log(Level.INFO, "message: " + message);

            if (! filibusterClientInstrumentor.shouldAbort()) {
                // Request completed normally, but we want to throw the exception anyway, generate and throw.
                generateExceptionFromForcedException(filibusterClientInstrumentor);
            } else {
                // Request completed normally.

                // Notify Filibuster of complete invocation with the proper response.
                String className = message.getClass().getName();
                HashMap<String, String> returnValueProperties = new HashMap<>();
                filibusterClientInstrumentor.afterInvocationComplete(className, returnValueProperties);

                // Delegate.
                delegate().onMessage(message);
            }
        }

        // invoked on an error: status set to a status message
        // Status.code = FAILED_PRECONDITION, description = ..., cause = ...
        // trailers metadata headers.
        @Override
        public void onClose(Status status, Metadata trailers) {
            logger.log(Level.INFO, "INSIDE: onClose!");
            logger.log(Level.INFO, "status: " + status);
            logger.log(Level.INFO, "trailers: " + trailers);

            if (! filibusterClientInstrumentor.shouldAbort()) {
                Status rewrittenStatus = generateCorrectStatusForAbort(filibusterClientInstrumentor);
                delegate().onClose(rewrittenStatus, trailers);
            }

            if (! status.isOk()) {
                // Request completed -- if it completed with a failure, it will be coming here for
                // the first time (didn't call onMessage) and therefore, we need to notify the Filibuster
                // server that the call completed with failure.  If it completed successfully, we would
                // have already notified the Filibuster server in the onMessage callback.

                // Notify Filibuster of error.
                HashMap<String, String> additionalMetadata = new HashMap<>();
                additionalMetadata.put("code", status.getCode().toString());
                String exceptionName = "io.grpc.StatusRuntimeException";

                // Exception is always null because it doesn't propagate across RPC boundaries.
                String cause = null;

                // ...unless the exception is generated by an interceptor on the client side before or after the call is made.
                if (status.getCause() != null) {
                    cause = status.getCause().getClass().toString();
                }

                // exception cause is always null, because it doesn't serialize and pass through even if provided.
                filibusterClientInstrumentor.afterInvocationWithException(exceptionName, cause, additionalMetadata);
            }

            delegate().onClose(status, trailers);
        }

        @Override
        public void onReady() {
            logger.log(Level.INFO, "INSIDE: onReady!");
            delegate().onReady();
        }
    }
}
