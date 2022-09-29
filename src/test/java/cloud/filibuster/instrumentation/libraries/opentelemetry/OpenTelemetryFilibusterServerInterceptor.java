package cloud.filibuster.instrumentation.libraries.opentelemetry;

import cloud.filibuster.instrumentation.datatypes.RequestId;
import cloud.filibuster.instrumentation.instrumentors.FilibusterServerInstrumentor;
import cloud.filibuster.instrumentation.storage.ContextStorage;

import io.grpc.Contexts;
import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;

import javax.annotation.Nullable;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationEnabledProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationServerCommunicationEnabledProperty;

public class OpenTelemetryFilibusterServerInterceptor implements ServerInterceptor {
    private static final Logger logger = Logger.getLogger(OpenTelemetryFilibusterServerInterceptor.class.getName());

    @SuppressWarnings("FieldCanBeFinal")
    protected String serviceName;

    @SuppressWarnings("FieldCanBeFinal")
    protected ContextStorage contextStorage;

    public static Boolean disableServerCommunication = false;
    public static Boolean disableInstrumentation = false;

    @Nullable
    private String requestId;

    @Nullable
    private final Instrumenter<GrpcRequest, Status> serverInstrumentor;

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

    public OpenTelemetryFilibusterServerInterceptor(Instrumenter<GrpcRequest, Status> serverInstrumentor) {
        this.serviceName = System.getenv("SERVICE_NAME");
        this.contextStorage = new OpenTelemetryContextStorage();
        this.serverInstrumentor = serverInstrumentor;
    }

    public OpenTelemetryFilibusterServerInterceptor(String serviceName, Instrumenter<GrpcRequest, Status> serverInstrumentor) {
        this.serviceName = serviceName;
        this.contextStorage = new OpenTelemetryContextStorage();
        this.serverInstrumentor = serverInstrumentor;
    }

    // ******************************************************************************************
    // Accessors for metadata.
    // ******************************************************************************************

    public String getRequestIdFromMetadata(Metadata requestHeaders) {
        String requestId = requestHeaders.get(Metadata.Key.of("x-filibuster-request-id", Metadata.ASCII_STRING_MARSHALLER));

        if (requestId == null) {
            requestId = RequestId.generateNewRequestId().toString();
        }

        logger.log(Level.INFO, "requestId: " + requestId);

        return requestId;
    }

    public String getGeneratedIdFromMetadata(Metadata requestHeaders) {
        String generatedId = requestHeaders.get(
                Metadata.Key.of("x-filibuster-generated-id", Metadata.ASCII_STRING_MARSHALLER));
        logger.log(Level.INFO, "generateId: " + generatedId);
        return generatedId;
    }

    public String getVectorClockFromMetadata(Metadata requestHeaders) {
        String vclock = requestHeaders.get(
                Metadata.Key.of("x-filibuster-vclock", Metadata.ASCII_STRING_MARSHALLER));
        logger.log(Level.INFO, "vclock: " + vclock);
        return vclock;
    }

    public String getOriginVectorClockFromMetadata(Metadata requestHeaders) {
        String originVclock = requestHeaders.get(
                Metadata.Key.of("x-filibuster-origin-vclock", Metadata.ASCII_STRING_MARSHALLER));
        logger.log(Level.INFO, "originVclock: " + originVclock);
        return originVclock;
    }

    public String getDistributedExecutionIndexFromMetadata(Metadata requestHeaders) {
        String distributedExecutionIndex = requestHeaders.get(
                Metadata.Key.of("x-filibuster-execution-index", Metadata.ASCII_STRING_MARSHALLER));
        logger.log(Level.INFO, "executionIndex: " + distributedExecutionIndex);
        return distributedExecutionIndex;
    }

    @Override
    public <REQUEST, RESPONSE> ServerCall.Listener<REQUEST> interceptCall(
            ServerCall<REQUEST, RESPONSE> call,
            Metadata headers,
            ServerCallHandler<REQUEST, RESPONSE> next) {
        GrpcRequest request =
                new GrpcRequest(
                        call.getMethodDescriptor(),
                        headers,
                        call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR));
        Context context;

        if (serverInstrumentor != null) {
            context = serverInstrumentor.start(Context.current(), request);
        } else {
            context = Context.current();
        }

        try (Scope ignored = context.makeCurrent()) {
            if (shouldInstrument()) {
                logger.log(Level.INFO, "Entering server interceptor...");

                // ******************************************************************************************
                // Setup Filibuster instrumentation.
                // ******************************************************************************************

                logger.log(Level.INFO, "!!! Entering constructor.");

                FilibusterServerInstrumentor filibusterServerInstrumentor = new FilibusterServerInstrumentor(
                        serviceName,
                        shouldCommunicateWithServer(),
                        getRequestIdFromMetadata(headers),
                        getGeneratedIdFromMetadata(headers),
                        getVectorClockFromMetadata(headers),
                        getOriginVectorClockFromMetadata(headers),
                        getDistributedExecutionIndexFromMetadata(headers),
                        contextStorage
                );

                logger.log(Level.INFO, "!!! Leaving constructor.");

                // ******************************************************************************************
                // Force sleep if necessary.
                // ******************************************************************************************

                String sleepIntervalStr = headers.get(
                        Metadata.Key.of("x-filibuster-forced-sleep", Metadata.ASCII_STRING_MARSHALLER));

                if (sleepIntervalStr == null) {
                    sleepIntervalStr = "0";
                }

                int sleepInterval = Integer.parseInt(sleepIntervalStr);
                if (sleepInterval > 0) {
                    try {
                        Thread.sleep(sleepInterval * 1000L);
                    } catch (InterruptedException e) {
                        // Do nothing.
                    }
                }

                // ******************************************************************************************
                // Notify Filibuster before delegation.
                // ******************************************************************************************

                logger.log(Level.INFO, "!!! Entering beforeInvocation.");

                filibusterServerInstrumentor.beforeInvocation();

                logger.log(Level.INFO, "!!! Leaving beforeInvocation.");

                // ******************************************************************************************
                // Delegate to underlying service.
                // ******************************************************************************************

                logger.log(Level.INFO, "Leaving server interceptor...");
            }

            return new FilibusterServerCall<>(call, context, request, headers).start(headers, next);
        } catch (Throwable e) {
            if (serverInstrumentor != null) {
                serverInstrumentor.end(context, request, null, e);
            }

            throw e;
        }
    }

    final class FilibusterServerCall<REQUEST, RESPONSE>
            extends ForwardingServerCall.SimpleForwardingServerCall<REQUEST, RESPONSE> {
        @SuppressWarnings("FieldCanBeLocal")
        final private Metadata requestHeaders;

        @SuppressWarnings("FieldCanBeLocal")
        private final Context context;

        @SuppressWarnings("FieldCanBeLocal")
        private final GrpcRequest request;

        public FilibusterServerCall(ServerCall<REQUEST, RESPONSE> delegate, Context context, GrpcRequest request, Metadata requestHeaders) {
            super(delegate);
            this.requestHeaders = requestHeaders;
            this.context = context;
            this.request = request;
        }

        FilibusterServerCallListener start(Metadata headers, ServerCallHandler<REQUEST, RESPONSE> next) {
            return new FilibusterServerCallListener(
                    Contexts.interceptCall(io.grpc.Context.current(), this, headers, next), context, request);
        }

        // ******************************************************************************************
        // Implementation.
        // ******************************************************************************************

        @Override
        public void sendMessage(RESPONSE message) {
            try (Scope ignored = context.makeCurrent()) {
                super.sendMessage(message);
            }
        }

        @Override
        public void close(Status status, Metadata trailers) {
            try {
                delegate().close(status, trailers);
            } catch (Throwable e) {
                if (serverInstrumentor != null) {
                    serverInstrumentor.end(context, request, status, e);
                }
                throw e;
            }

            if (serverInstrumentor != null) {
                serverInstrumentor.end(context, request, status, status.getCause());
            }
        }

        @Override
        public void sendHeaders(Metadata responseHeaders) {
            if (!shouldInstrument()) {
                try (Scope ignored = context.makeCurrent()) {
                    super.sendHeaders(responseHeaders);
                }
            } else {
                try (Scope ignored = context.makeCurrent()) {
                    super.sendHeaders(responseHeaders);
                }
            }
        }

        final class FilibusterServerCallListener
                extends ForwardingServerCallListener.SimpleForwardingServerCallListener<REQUEST> {
            private final Context context;
            private final GrpcRequest request;

            FilibusterServerCallListener(ServerCall.Listener<REQUEST> delegate, Context context, GrpcRequest request) {
                super(delegate);
                this.context = context;
                this.request = request;
            }

            @Override
            public void onMessage(REQUEST message) {
                delegate().onMessage(message);
            }

            @Override
            public void onHalfClose() {
                try {
                    delegate().onHalfClose();
                } catch (Throwable e) {
                    if (serverInstrumentor != null) {
                        serverInstrumentor.end(context, request, null, e);
                    }
                    throw e;
                }
            }

            @Override
            public void onCancel() {
                try {
                    delegate().onCancel();
                } catch (Throwable e) {
                    if (serverInstrumentor != null) {
                        serverInstrumentor.end(context, request, null, e);
                    }
                    throw e;
                }
                if (serverInstrumentor != null) {
                    serverInstrumentor.end(context, request, null, null);
                }
            }

            @Override
            public void onComplete() {
                try {
                    delegate().onComplete();
                } catch (Throwable e) {
                    if (serverInstrumentor != null) {
                        serverInstrumentor.end(context, request, null, e);
                    }
                    throw e;
                }
            }

            @Override
            public void onReady() {
                try {
                    delegate().onReady();
                } catch (Throwable e) {
                    if (serverInstrumentor != null) {
                        serverInstrumentor.end(context, request, null, e);
                    }
                    throw e;
                }
            }
        }
    }
}
