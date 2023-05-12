package cloud.filibuster.instrumentation.libraries.grpc;

import cloud.filibuster.instrumentation.datatypes.RequestId;
import cloud.filibuster.instrumentation.instrumentors.FilibusterServerInstrumentor;
import cloud.filibuster.instrumentation.storage.ContextStorage;
import cloud.filibuster.instrumentation.storage.ThreadLocalContextStorage;
import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

import javax.annotation.Nullable;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationEnabledProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationServerCommunicationEnabledProperty;

public class FilibusterServerInterceptor implements ServerInterceptor {
    private static final Logger logger = Logger.getLogger(FilibusterServerInterceptor.class.getName());

    @SuppressWarnings("FieldCanBeFinal")
    protected String serviceName;

    @SuppressWarnings("FieldCanBeFinal")
    protected ContextStorage contextStorage;

    public static Boolean disableServerCommunication = false;
    public static Boolean disableInstrumentation = false;

    private final String logPrefix = "[FILIBUSTER-GRPC_SERVER_INTERCEPTOR]: ";

    @Nullable
    private String requestId;

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

    public FilibusterServerInterceptor() {
        this.serviceName = System.getenv("SERVICE_NAME");
        this.contextStorage = new ThreadLocalContextStorage();
    }

    public FilibusterServerInterceptor(String serviceName) {
        this.serviceName = serviceName;
        this.contextStorage = new ThreadLocalContextStorage();
    }

    // ******************************************************************************************
    // Accessors for metadata.
    // ******************************************************************************************

    public String getRequestIdFromMetadata(Metadata requestHeaders) {
        String requestId = requestHeaders.get(Metadata.Key.of("x-filibuster-request-id", Metadata.ASCII_STRING_MARSHALLER));

        if (requestId == null) {
            requestId = RequestId.generateNewRequestId().toString();
        }

        logger.log(Level.INFO, logPrefix + "requestId: " + requestId);

        return requestId;
    }

    public String getGeneratedIdFromMetadata(Metadata requestHeaders) {
        String generatedId = requestHeaders.get(
                Metadata.Key.of("x-filibuster-generated-id", Metadata.ASCII_STRING_MARSHALLER));
        logger.log(Level.INFO, logPrefix + "generatedId: " + generatedId);
        return generatedId;
    }

    public String getVectorClockFromMetadata(Metadata requestHeaders) {
        String vclock = requestHeaders.get(
                Metadata.Key.of("x-filibuster-vclock", Metadata.ASCII_STRING_MARSHALLER));
        logger.log(Level.INFO, logPrefix + "vclock: " + vclock);
        return vclock;
    }

    public String getOriginVectorClockFromMetadata(Metadata requestHeaders) {
        String originVclock = requestHeaders.get(
                Metadata.Key.of("x-filibuster-origin-vclock", Metadata.ASCII_STRING_MARSHALLER));
        logger.log(Level.INFO, logPrefix + "originVclock: " + originVclock);
        return originVclock;
    }

    public String getDistributedExecutionIndexFromMetadata(Metadata requestHeaders) {
        String distributedExecutionIndex = requestHeaders.get(
                Metadata.Key.of("x-filibuster-execution-index", Metadata.ASCII_STRING_MARSHALLER));
        logger.log(Level.INFO, logPrefix + "executionIndex: " + distributedExecutionIndex);
        return distributedExecutionIndex;
    }

    @Override
    @SuppressWarnings("UnnecessaryFinal")
    public <REQUEST, RESPONSE> ServerCall.Listener<REQUEST> interceptCall(
            ServerCall<REQUEST, RESPONSE> call,
            final Metadata headers,
            ServerCallHandler<REQUEST, RESPONSE> next) {

        if (shouldInstrument()) {
            logger.log(Level.INFO, logPrefix + "Entering server interceptor...");

            // ******************************************************************************************
            // Setup Filibuster instrumentation.
            // ******************************************************************************************

            logger.log(Level.INFO, logPrefix + "!!! Entering constructor.");

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

            logger.log(Level.INFO, logPrefix + "!!! Leaving constructor.");

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

            logger.log(Level.INFO, logPrefix + "!!! Entering beforeInvocation.");

            filibusterServerInstrumentor.beforeInvocation();

            logger.log(Level.INFO, logPrefix + "!!! Leaving beforeInvocation.");

            // ******************************************************************************************
            // Delegate to underlying service.
            // ******************************************************************************************

            logger.log(Level.INFO, logPrefix + "Leaving server interceptor...");
        }

        return next.startCall(new FilibusterServerCall<>(call), headers);
    }

    @SuppressWarnings("ClassCanBeStatic")
    final class FilibusterServerCall<REQUEST, RESPONSE>
            extends ForwardingServerCall.SimpleForwardingServerCall<REQUEST, RESPONSE> {
        public FilibusterServerCall(ServerCall<REQUEST, RESPONSE> delegate) {
            super(delegate);
        }
    }
}
