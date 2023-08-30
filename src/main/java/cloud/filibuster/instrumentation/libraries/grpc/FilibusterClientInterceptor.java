package cloud.filibuster.instrumentation.libraries.grpc;

import cloud.filibuster.RpcType;
import cloud.filibuster.exceptions.filibuster.FilibusterFaultInjectionException;
import cloud.filibuster.exceptions.filibuster.FilibusterInstrumentationMissingDelegateException;
import cloud.filibuster.instrumentation.datatypes.Callsite;
import cloud.filibuster.instrumentation.datatypes.CallsiteArguments;
import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;
import cloud.filibuster.instrumentation.storage.ContextStorage;
import cloud.filibuster.instrumentation.storage.ThreadLocalContextStorage;
import cloud.filibuster.junit.server.core.transformers.Accumulator;
import com.google.gson.Gson;
import com.google.protobuf.GeneratedMessageV3;
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
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationEnabledProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationServerCommunicationEnabledProperty;
import static cloud.filibuster.instrumentation.instrumentors.FilibusterGrpcHeaders.FILIBUSTER_EXCEPTION_CAUSE;
import static cloud.filibuster.instrumentation.instrumentors.FilibusterGrpcHeaders.FILIBUSTER_EXCEPTION_CAUSE_MESSAGE;
import static cloud.filibuster.instrumentation.instrumentors.FilibusterGrpcHeaders.FILIBUSTER_EXCEPTION_CODE;
import static cloud.filibuster.instrumentation.instrumentors.FilibusterGrpcHeaders.FILIBUSTER_EXCEPTION_DESCRIPTION;
import static cloud.filibuster.instrumentation.instrumentors.FilibusterGrpcHeaders.FILIBUSTER_EXCEPTION_NAME;
import static cloud.filibuster.instrumentation.instrumentors.FilibusterShared.generateExceptionFromForcedException;
import static cloud.filibuster.instrumentation.instrumentors.FilibusterShared.getForcedExceptionMetadataValue;
import static cloud.filibuster.instrumentation.instrumentors.FilibusterShared.getForcedExceptionValue;
import static java.util.Objects.requireNonNull;

public class FilibusterClientInterceptor implements ClientInterceptor {
    private static final Logger logger = Logger.getLogger(FilibusterClientInterceptor.class.getName());

    @SuppressWarnings("FieldCanBeFinal")
    protected String serviceName;

    @SuppressWarnings("FieldCanBeFinal")
    protected ContextStorage contextStorage;

    public static Boolean disableServerCommunication = false;
    public static Boolean disableInstrumentation = false;

    private static final String logPrefix = "[FILIBUSTER-GRPC_CLIENT_INTERCEPTOR]: ";

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

    @Nullable
    private static <REQUEST> REQUEST injectTransformerFault(FilibusterClientInstrumentor filibusterClientInstrumentor, JSONObject transformerFault, REQUEST originalRequest) {
        try {
            if (transformerFault.has("value") && transformerFault.has("accumulator")) {

                // Extract the transformer fault value from the transformerFault JSONObject.
                Object transformerFaultValue = transformerFault.get("value");
                String sTransformerValue = transformerFaultValue.toString();
                @SuppressWarnings("unchecked")
                REQUEST castedValue = (REQUEST) transformerFaultValue;
                logger.log(Level.INFO, logPrefix + "Injecting the transformed fault value: " + sTransformerValue);

                // Extract the accumulator from the transformerFault JSONObject.
                Accumulator<?, ?> accumulator = new Gson().fromJson(transformerFault.get("accumulator").toString(), Accumulator.class);

                // Notify Filibuster.
                filibusterClientInstrumentor.afterInvocationWithTransformerFault(sTransformerValue,
                        originalRequest.getClass().toString(), accumulator);

                // Return the transformer fault value.
                if (castedValue == JSONObject.NULL) {
                    return null;
                }
                return castedValue;
            } else {
                String missingKey;
                if (transformerFault.has("value")) {
                    missingKey = "accumulator";
                } else {
                    missingKey = "value";
                }
                logger.log(Level.WARNING, logPrefix + "injectTransformerFault: The transformerFault does not have the required key " + missingKey);
                throw new FilibusterFaultInjectionException("injectTransformerFault: The transformerFault does not have the required key " + missingKey);
            }
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, logPrefix + "Could not inject transformer fault. The cast was probably not successful:", e);
            throw new FilibusterFaultInjectionException("Could not inject transformer fault. The cast was probably not successful:", e);
        }
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

            @Override
            protected ClientCall<REQUEST, RESPONSE> delegate() {
                if (delegate == null) {
                    throw new FilibusterInstrumentationMissingDelegateException("Delegate is null, something threw inside of the Filibuster interceptor previously, scroll to see previous exception.");
                }
                return delegate;
            }

            @Override
            public void start(Listener<RESPONSE> responseListener, Metadata headers) {
                logger.log(Level.INFO, logPrefix + "INSIDE: start!");

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
                logger.log(Level.INFO, logPrefix + "INSIDE: sendMessage!");
                logger.log(Level.INFO, logPrefix + "message: " + message.toString());

                // ******************************************************************************************
                // Figure out if we are inside of instrumentation.
                // ******************************************************************************************

                String instrumentationRequestStr = headers.get(
                        Metadata.Key.of("x-filibuster-instrumentation", Metadata.ASCII_STRING_MARSHALLER));
                logger.log(Level.INFO, logPrefix + "instrumentationRequestStr: " + instrumentationRequestStr);
                boolean instrumentationRequest = Boolean.parseBoolean(instrumentationRequestStr);
                logger.log(Level.INFO, logPrefix + "instrumentationRequest: " + instrumentationRequest);

                if (!shouldInstrument() || instrumentationRequest) {
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

                    logger.log(Level.INFO, logPrefix + "method: " + method);
                    logger.log(Level.INFO, logPrefix + "grpcFullMethodName: " + grpcFullMethodName);
                    logger.log(Level.INFO, logPrefix + "grpcServiceName: " + grpcServiceName);
                    logger.log(Level.INFO, logPrefix + "grpcRpcName: " + grpcRpcName);

                    // ******************************************************************************************
                    // Construct preliminary call site information.
                    // ******************************************************************************************

                    CallsiteArguments callsiteArguments = new CallsiteArguments(message.getClass(), message.toString());

                    Callsite callsite = new Callsite(
                            serviceName,
                            grpcServiceName,
                            grpcFullMethodName,
                            callsiteArguments
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

                    if (message instanceof GeneratedMessageV3) {
                        GeneratedMessageV3 generatedMessageV3 = (GeneratedMessageV3) message;
                        filibusterClientInstrumentor.prepareForInvocation(generatedMessageV3);
                    } else {
                        filibusterClientInstrumentor.prepareForInvocation();
                    }

                    // ******************************************************************************************
                    // Record invocation.
                    // ******************************************************************************************

                    filibusterClientInstrumentor.setRpcType(RpcType.GRPC);
                    filibusterClientInstrumentor.beforeInvocation();

                    // ******************************************************************************************
                    // Attach metadata to outgoing request.
                    // ******************************************************************************************

                    logger.log(Level.INFO, logPrefix + "requestId: " + filibusterClientInstrumentor.getOutgoingRequestId());

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
                    JSONObject transformerFault = filibusterClientInstrumentor.getTransformerFault();

                    logger.log(Level.INFO, logPrefix + "forcedException: " + forcedException);
                    logger.log(Level.INFO, logPrefix + "failureMetadata: " + failureMetadata);
                    logger.log(Level.INFO, logPrefix + "transformerFault: " + transformerFault);

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

                        if (forcedExceptionMetadata.has("defer") && forcedExceptionMetadata.getBoolean("defer")) {
                            String exceptionNameString = getForcedExceptionValue(forcedException, "name", "");
                            String codeStr = getForcedExceptionMetadataValue(forcedException, "code", "");
                            String descriptionStr = getForcedExceptionMetadataValue(forcedException, "description", "");
                            String causeString = getForcedExceptionMetadataValue(forcedException, "cause", "");
                            String causeMessageString = getForcedExceptionMetadataValue(forcedException, "cause_message", "");

                            headers.put(
                                    Metadata.Key.of(FILIBUSTER_EXCEPTION_NAME, Metadata.ASCII_STRING_MARSHALLER),
                                    exceptionNameString
                            );
                            headers.put(
                                    Metadata.Key.of(FILIBUSTER_EXCEPTION_CODE, Metadata.ASCII_STRING_MARSHALLER),
                                    codeStr
                            );
                            headers.put(
                                    Metadata.Key.of(FILIBUSTER_EXCEPTION_DESCRIPTION, Metadata.ASCII_STRING_MARSHALLER),
                                    descriptionStr
                            );
                            headers.put(
                                    Metadata.Key.of(FILIBUSTER_EXCEPTION_CAUSE, Metadata.ASCII_STRING_MARSHALLER),
                                    causeString
                            );
                            headers.put(
                                    Metadata.Key.of(FILIBUSTER_EXCEPTION_CAUSE_MESSAGE, Metadata.ASCII_STRING_MARSHALLER),
                                    causeMessageString
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

                    // ******************************************************************************************
                    // Inject transformer fault, if necessary.
                    // ******************************************************************************************

                    if (transformerFault != null && filibusterClientInstrumentor.shouldAbort()) {
                        message = injectTransformerFault(filibusterClientInstrumentor, transformerFault, message);
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
            logger.log(Level.INFO, logPrefix + "INSIDE: onMessage!");
            logger.log(Level.INFO, logPrefix + "message: " + message);

            if (!filibusterClientInstrumentor.shouldAbort()) {
                // Request completed normally, but we want to throw the exception anyway, generate and throw.
                generateExceptionFromForcedException(filibusterClientInstrumentor);
            } else {
                // Request completed normally.

                // Notify Filibuster of complete invocation with the proper response.
                String className = message.getClass().getName();
                HashMap<String, String> returnValueProperties = new HashMap<>();
                returnValueProperties.put("toString", message.toString());

                if (message instanceof GeneratedMessageV3) {
                    GeneratedMessageV3 generatedMessageV3 = (GeneratedMessageV3) message;
                    filibusterClientInstrumentor.afterInvocationComplete(className, returnValueProperties, /* isUpdate= */false, message, generatedMessageV3);
                } else {
                    filibusterClientInstrumentor.afterInvocationComplete(className, returnValueProperties, /* isUpdate= */false, message);
                }

                // Delegate.
                delegate().onMessage(message);
            }
        }

        // invoked on an error: status set to a status message
        // Status.code = FAILED_PRECONDITION, description = ..., cause = ...
        // trailers metadata headers.
        @Override
        public void onClose(Status status, Metadata trailers) {
            logger.log(Level.INFO, logPrefix + "INSIDE: onClose!");
            logger.log(Level.INFO, logPrefix + "status: " + status);
            logger.log(Level.INFO, logPrefix + "trailers: " + trailers);

            if (!filibusterClientInstrumentor.shouldAbort()) {
                Status rewrittenStatus = generateCorrectStatusForAbort(filibusterClientInstrumentor);
                delegate().onClose(rewrittenStatus, trailers);
            }

            if (!status.isOk()) {
                // Request completed -- if it completed with a failure, it will be coming here for
                // the first time (didn't call onMessage) and therefore, we need to notify the Filibuster
                // server that the call completed with failure.  If it completed successfully, we would
                // have already notified the Filibuster server in the onMessage callback.

                // Notify Filibuster of error.
                HashMap<String, String> additionalMetadata = new HashMap<>();
                additionalMetadata.put("code", status.getCode().toString());
                additionalMetadata.put("description", status.getDescription());
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
            logger.log(Level.INFO, logPrefix + "INSIDE: onReady!");
            delegate().onReady();
        }
    }
}
