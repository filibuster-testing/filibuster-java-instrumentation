package cloud.filibuster.instrumentation.libraries.armeria.http;

import cloud.filibuster.exceptions.filibuster.FilibusterFaultInjectionException;
import cloud.filibuster.instrumentation.datatypes.Callsite;
import cloud.filibuster.instrumentation.datatypes.CallsiteArguments;
import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;
import cloud.filibuster.instrumentation.storage.ContextStorage;
import cloud.filibuster.instrumentation.storage.ThreadLocalContextStorage;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.linecorp.armeria.client.*;
import com.linecorp.armeria.common.*;
import com.linecorp.armeria.common.logging.RequestLogBuilder;
import com.linecorp.armeria.common.stream.CancelledSubscriptionException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.netty.channel.ConnectTimeoutException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.instrumentation.helpers.Networking.attemptHostnameResolution;
import static cloud.filibuster.instrumentation.helpers.Networking.extractHostnameAndPortFromURI;
import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationEnabledProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationServerCommunicationEnabledProperty;

public class FilibusterDecoratingHttpClient extends SimpleDecoratingHttpClient {
    private static final Logger logger = Logger.getLogger(FilibusterDecoratingHttpClient.class.getName());

    @SuppressWarnings("FieldCanBeFinal")
    protected ContextStorage contextStorage;

    @SuppressWarnings("FieldCanBeFinal")
    protected String serviceName;

    public static Boolean disableServerCommunication = false;

    public static Boolean disableInstrumentation = false;

    private static final String logPrefix = "[FILIBUSTER-ARMERIA_HTTP_CLIENT]: ";


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

    /**
     * Tell if the request is a GRPC-as-HTTP request or an HTTP request issued using WebClient.
     * @param request outgoing HttpRequest
     * @return boolean
     */
    private static boolean isRequestGrpcAsHttp(HttpRequest request) {
        String contentType = request.headers().get("content-type");
        return contentType != null && contentType.contains("grpc");
    }

    /**
     * Tell if the request is a GRPC-as-HTTP request or an HTTP request issued using WebClient.
     * @param responseHeaders headers for the actual HTTP response.
     * @return boolean
     */
    private static boolean isResponseGrpcAsHttp(ResponseHeaders responseHeaders) {
        String contentType = responseHeaders.get("content-type");
        return contentType != null && contentType.contains("grpc");
    }

    public FilibusterDecoratingHttpClient(HttpClient delegate) {
        super(delegate);
        this.serviceName = System.getenv("SERVICE_NAME");
        this.contextStorage = new ThreadLocalContextStorage();
    }

    public FilibusterDecoratingHttpClient(HttpClient delegate, String serviceName) {
        super(delegate);
        this.serviceName = serviceName;
        this.contextStorage = new ThreadLocalContextStorage();
    }

    public FilibusterDecoratingHttpClient(HttpClient delegate, String serviceName, boolean grpcRpcType) {
        super(delegate);
        this.serviceName = serviceName;
        this.contextStorage = new ThreadLocalContextStorage();
        this.grpcRpcType = true;
    }

    private boolean grpcRpcType = false;

    // ******************************************************************************************
    // Overloads for custom clients.
    // ******************************************************************************************

    protected void setupContext(ClientRequestContext ctx, HttpRequest req) {

    }

    protected void contextWhenComplete(ClientRequestContext ctx) {

    }

    protected HttpResponse delegateWithContext(ClientRequestContext ctx, HttpRequest req) throws Exception {
        return unwrap().execute(ctx, req);
    }

    // ******************************************************************************************
    // Implementation.
    // ******************************************************************************************

    @Override
    public HttpResponse execute(ClientRequestContext ctx, HttpRequest req) throws Exception {
//        logger.log(Level.INFO, logPrefix +"req.headers().contains(\"X-Filibuster-Instrumentation\"): " + req.headers().contains("X-Filibuster-Instrumentation"));

        if (! shouldInstrument() || req.headers().contains("X-Filibuster-Instrumentation")) {
            boolean shouldInstrument = shouldInstrument();
            boolean isInstrumentationRequest = req.headers().contains("X-Filibuster-Instrumentation");
//            logger.log(Level.INFO, logPrefix +"shouldInstrument(): " + shouldInstrument());
//            logger.log(Level.INFO, logPrefix +"req.headers().contains(\"X-Filibuster-Instrumentation\"): " + req.headers().contains("X-Filibuster-Instrumentation"));
//            logger.log(Level.INFO, logPrefix +"req.method().toString(): " + req.method());
//            logger.log(Level.INFO, logPrefix +"req.uri().toString(): " + req.uri());
//            logger.log(Level.INFO, logPrefix +"!!!! Bailing out of client instrumentation early !!!!");
            return unwrap().execute(ctx, req);
        }

        // ******************************************************************************************
        // Construct call site information.
        // ******************************************************************************************

        ArrayList<String> serializedArguments = new ArrayList<>();
        serializedArguments.add(req.headers().uri().toString());

        Callsite callsite;

        if (!isRequestGrpcAsHttp(req)){

            if (req.method().toString().equals("POST") || req.method().toString().equals("PUT")) {
                int payloadHashCode = -1;

                try {
                    // From: https://stackoverflow.com/questions/1196192/how-to-read-the-value-of-a-private-field-from-a-different-class-in-java
                    Field f1 = req.getClass().getDeclaredField("delegate"); //NoSuchFieldException
                    f1.setAccessible(true);
                    Object f1Delegate = f1.get(req); //IllegalAccessException

                    Field f2 = f1Delegate.getClass().getSuperclass().getDeclaredField("obj"); //NoSuchFieldException
                    f2.setAccessible(true);
                    HttpData f2ByteArray = (HttpData) f2.get(f1Delegate); //IllegalAccessException

                    payloadHashCode = f2ByteArray.hashCode();
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    // Ignore.
                    logger.log(Level.SEVERE, "!!! Possible dynamic reduction risk: could not serialize arguments for callsite identification");
                }

                serializedArguments.add(String.valueOf(payloadHashCode));
            }

            String classOrModuleName = "WebClient";
            callsite = new Callsite(
                    serviceName,
                    classOrModuleName,
                    req.method().toString(),
                    new CallsiteArguments(req.getClass(), String.join("-", serializedArguments)));
        } else { // GRPC call
            String classOrModuleName = "GrpcClient";
            String path = req.path();
            String grpcFullMethodName = path.indexOf("/") == 0? path.substring(path.indexOf("/") + 1): path;
            String grpcServiceName = grpcFullMethodName.substring(0, grpcFullMethodName.indexOf("/"));
            callsite = new Callsite(
                    serviceName,
                    grpcServiceName,
                    grpcFullMethodName,
                    new CallsiteArguments(req.getClass(), String.join("-", serializedArguments)));
        }


        // ******************************************************************************************
        // Prepare for invocation.
        // ******************************************************************************************

        FilibusterClientInstrumentor filibusterClientInstrumentor = new FilibusterClientInstrumentor(
                serviceName,
                shouldCommunicateWithServer(),
                contextStorage,
                callsite
        );
        filibusterClientInstrumentor.prepareForInvocation();

        // ******************************************************************************************
        // Setup context.
        // ******************************************************************************************

        setupContext(ctx, req);

        // ******************************************************************************************
        // Record invocation.
        // ******************************************************************************************

        if (grpcRpcType) {
            filibusterClientInstrumentor.setRpcType("grpc"); // TODO, enum?
        }
        filibusterClientInstrumentor.beforeInvocation();

        JSONObject forcedException = filibusterClientInstrumentor.getForcedException();
        JSONObject failureMetadata = filibusterClientInstrumentor.getFailureMetadata();

        logger.log(Level.INFO, logPrefix +"forcedException: " + forcedException);
        logger.log(Level.INFO, logPrefix +"failureMetadata: " + failureMetadata);

        // ******************************************************************************************
        // Attach metadata to outgoing request.
        // ******************************************************************************************

        RequestHeadersBuilder newHeaders = req.headers().toBuilder();

        String outgoingRequestId = filibusterClientInstrumentor.getOutgoingRequestId();
        newHeaders.add("X-Filibuster-Request-Id", outgoingRequestId);

        if (filibusterClientInstrumentor.getGeneratedId() > -1) {
            newHeaders.add("X-Filibuster-Generated-Id", String.valueOf(filibusterClientInstrumentor.getGeneratedId()));
        }
        newHeaders.add("X-Filibuster-VClock", filibusterClientInstrumentor.getVectorClock().toString());
        newHeaders.add("X-Filibuster-Origin-VClock", filibusterClientInstrumentor.getOriginVectorClock().toString());
        newHeaders.add("X-Filibuster-Execution-Index", filibusterClientInstrumentor.getDistributedExecutionIndex().toString());

        if (forcedException != null) {
            JSONObject forcedExceptionMetadata = forcedException.getJSONObject("metadata");

            if (forcedExceptionMetadata.has("sleep")) {
                int sleepInterval = forcedExceptionMetadata.getInt("sleep");
                newHeaders.add("X-Filibuster-Forced-Sleep", String.valueOf(sleepInterval));
            } else {
                newHeaders.add("X-Filibuster-Forced-Sleep", String.valueOf(0));
            }
        }

        req = req.withHeaders(newHeaders);
        ctx.updateRequest(req);

        // ******************************************************************************************
        // Resolve the name of the destination.
        // ******************************************************************************************

        String uri = req.uri().toString();
        Map.Entry<String, String> hostnameAndPort = extractHostnameAndPortFromURI(uri);
        String hostname = hostnameAndPort.getKey();
        String port = hostnameAndPort.getValue();
        String hostnameForExceptionBody = attemptHostnameResolution(hostname, uri);

        // ******************************************************************************************
        // If we need to override the response, do it now before proceeding.
        // ******************************************************************************************

        if (failureMetadata != null) {

            if (isRequestGrpcAsHttp(req)) {
                String statusCodeMsg = failureMetadata.getJSONObject("exception").getJSONObject("metadata")
                        .getString("code");
                int statusCode = Status.Code.valueOf(statusCodeMsg).value();
                HashMap<String, String> additionalMetadata = new HashMap<>();
                additionalMetadata.put("code", statusCodeMsg);
                String exceptionName = "io.grpc.StatusRuntimeException";
                // exception cause is always null, because it doesn't serialize and pass through even if provided.
                filibusterClientInstrumentor.afterInvocationWithException(exceptionName, null, additionalMetadata);

                return generateResponseWithErrorHeaders(ctx, statusCode);
            } else {
                // Create the response.
                JSONObject returnValue = failureMetadata.getJSONObject("return_value");
                String statusCode = returnValue.getString("status_code");

                // This could be any subclass of HttpResponse: AggregatedHttpResponse, FilteredHttpResponse, etc.
                // Therefore, take the super -- I don't think Filibuster really does anything with this anyway.
                String className = "com.linecorp.armeria.common.HttpResponse";

                // Notify Filibuster.
                HashMap<String, String> returnValueProperties = new HashMap<>();
                returnValueProperties.put("status_code", statusCode);
                filibusterClientInstrumentor.afterInvocationComplete(className, returnValueProperties);

                // Return the response.
                return HttpResponse.of(HttpStatus.valueOf(statusCode));
            }

        }

        // ******************************************************************************************
        // If we need to throw, this is where we throw.
        // ******************************************************************************************

        if (forcedException != null && filibusterClientInstrumentor.shouldAbort()) {
            if (isRequestGrpcAsHttp(req)) { // we return error header in response, keep uniform with metadata failure
                String statusCodeMsg = forcedException.getJSONObject("metadata")
                        .getString("code");
                int statusCode = Status.Code.valueOf(statusCodeMsg).value();
                HashMap<String, String> additionalMetadata = new HashMap<>();
                additionalMetadata.put("code", statusCodeMsg);
                String exceptionName = "io.grpc.StatusRuntimeException";
                // exception cause is always null, because it doesn't serialize and pass through even if provided.
                filibusterClientInstrumentor.afterInvocationWithException(exceptionName, null, additionalMetadata);
                return generateResponseWithErrorHeaders(ctx, statusCode);
            } else {
                generateAndThrowException(filibusterClientInstrumentor, forcedException, hostname, hostnameForExceptionBody, port);
            }
        }

        // ******************************************************************************************
        // Issue request.
        // ******************************************************************************************

        logger.log(Level.INFO, logPrefix +"Issuing request!");
        HttpResponse response = delegateWithContext(ctx, req);

        // ******************************************************************************************
        // Callback that fires if the request throws an exception.
        // ******************************************************************************************

        response.whenComplete().handle((result, cause) -> {
            // Only if this fires with an exception.
            if (cause != null) {
                logger.log(Level.INFO, logPrefix +"cause: " + cause);

                // Notify Filibuster.
                if (!(cause instanceof CancelledSubscriptionException)) {
                    filibusterClientInstrumentor.afterInvocationWithException(cause);
                }

            }

            return null;
        });

        // ******************************************************************************************
        // Completion callback.
        // ******************************************************************************************

        contextWhenComplete(ctx);

        // ******************************************************************************************
        // Callback that fires if the request succeeds.
        // ******************************************************************************************

        return new FilteredHttpResponse(response) {

            @Override
            @CanIgnoreReturnValue
            protected HttpObject filter(HttpObject obj) {

                // We were supposed to perform fault injection, but only after the request succeeds.
                // abort == false
                if (! filibusterClientInstrumentor.shouldAbort()) {

                    if (forcedException != null) {
                        generateAndThrowException(filibusterClientInstrumentor, forcedException, hostname, hostnameForExceptionBody, port);
                    }

                } else {
                    if (obj instanceof ResponseHeaders) {
                        // Get response headers, extract class name and status.
                        ResponseHeaders responseHeaders = (ResponseHeaders) obj;

                        if (isResponseGrpcAsHttp(responseHeaders)) {
                            // if it's a grpc-as-http request and throws exception, we record exception in filibuster
                            if (responseHeaders.get("grpc-status") != null &&
                                    !Objects.equals(responseHeaders.get("grpc-status"), "0")) {
                                HashMap<String, String> additionalMetadata = new HashMap<>();
                                int grpcErrorCode = Integer.parseInt(responseHeaders.get("grpc-status"));
                                additionalMetadata.put("code", Status.Code.values()[grpcErrorCode].toString());
                                String exceptionName = "io.grpc.StatusRuntimeException";
                                // exception cause is always null, because it doesn't serialize and pass through even if provided.
                                filibusterClientInstrumentor.afterInvocationWithException(exceptionName, null, additionalMetadata);
                            } else { // a grpc request and with no failure
                                // Notify Filibuster of complete invocation with the proper response.
                                String className = "io.grpc.StatusRuntimeException"; // Assumed, we don't actually have it.
                                HashMap<String, String> returnValueProperties = new HashMap<>();
                                filibusterClientInstrumentor.afterInvocationComplete(className, returnValueProperties);
                            }
                        } else {
                            // This could be any subclass of HttpResponse: AggregatedHttpResponse, FilteredHttpResponse, etc.
                            // Therefore, take the super -- I don't think Filibuster really does anything with this anyway.
                            String className = "com.linecorp.armeria.common.HttpResponse";
                            String statusCode = responseHeaders.get(HttpHeaderNames.STATUS);

                            logger.log(Level.INFO, logPrefix +"responseHeaders: " + responseHeaders);
                            logger.log(Level.INFO, logPrefix +"statusCode: " + statusCode);

                            // Notify Filibuster.
                            logger.log(Level.INFO, logPrefix +"Notifying Filibuster!!!");
                            HashMap<String, String> returnValueProperties = new HashMap<>();
                            returnValueProperties.put("status_code", statusCode);
                            filibusterClientInstrumentor.afterInvocationComplete(className, returnValueProperties);
                        }

                    }
                }

                return obj;
            }
        };
    }

    private static void generateAndThrowException(
            FilibusterClientInstrumentor filibusterClientInstrumentor,
            JSONObject forcedException,
            String hostname,
            String hostnameForExceptionBody,
            String port
    ) {
        String exceptionNameString = forcedException.getString("name");
        JSONObject forcedExceptionMetadata = forcedException.getJSONObject("metadata");
        String causeString = forcedExceptionMetadata.getString("cause");

        RuntimeException exceptionToThrow;

        if (exceptionNameString.equals("com.linecorp.armeria.client.UnprocessedRequestException")) {
            if (causeString.equals("io.netty.channel.ConnectTimeoutException")) {
                String message = "connection timed out: " + hostname + "/" + hostnameForExceptionBody + ":" + port;
                ConnectTimeoutException cause = new ConnectTimeoutException(message);
                exceptionToThrow = UnprocessedRequestException.of(cause);
            } else {
                throw new FilibusterFaultInjectionException("Cannot determine the execution cause to throw: " + causeString);
            }
        } else if (Objects.equals(exceptionNameString, "io.grpc.StatusRuntimeException")){
            String grpcErrorCode = forcedException.getJSONObject("metadata").get("code").toString();
            exceptionToThrow = new StatusRuntimeException(Status.fromCode(Status.Code.valueOf(grpcErrorCode)));
        } else {
            throw new FilibusterFaultInjectionException("Cannot determine the execution to throw: " + exceptionNameString);
        }

        if (exceptionToThrow != null) {
            // Notify Filibuster.
            filibusterClientInstrumentor.afterInvocationWithException(exceptionToThrow);

            // Throw callsite exception.
            throw exceptionToThrow;
        } else {
            throw new FilibusterFaultInjectionException("Exception is supposed to be thrown, but is null because we could not find a match.");
        }
    }

    @SuppressWarnings({"Nullable", "ParameterMissingNullable"})
    private static HttpResponse generateResponseWithErrorHeaders(ClientRequestContext ctx, int statusCode) {
        return generateResponseWithErrorHeaders(ctx, statusCode, "Injected fault from Filibuster, status code: " + statusCode);
    }

    @SuppressWarnings("Nullable")
    private static HttpResponse generateResponseWithErrorHeaders(ClientRequestContext ctx, int statusCode, String errorMessage) {
        ResponseHeadersBuilder responseHeadersBuilder = ResponseHeaders.builder().status(200)
                .add("content-type", "application/grpc")
                .add("content-length", String.valueOf(0))
                .add("grpc-status", String.valueOf(statusCode));
        responseHeadersBuilder.endOfStream(true);
        if (errorMessage != null) { // Custom assertion from application code, unknown when fault injecting
            responseHeadersBuilder.add("grpc-message", errorMessage);
        }

        ResponseHeaders responseHeaders = responseHeadersBuilder.build();

        RequestLogBuilder logBuilder = ctx.logBuilder();
        logBuilder.responseHeaders(responseHeaders);
        logBuilder.responseTrailers(responseHeaders);

        return HttpResponse.of(responseHeaders, HttpData.empty(), responseHeaders);
    }
}

