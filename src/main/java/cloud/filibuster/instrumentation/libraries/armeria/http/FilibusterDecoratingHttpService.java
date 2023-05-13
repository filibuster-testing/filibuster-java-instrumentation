package cloud.filibuster.instrumentation.libraries.armeria.http;

import cloud.filibuster.instrumentation.datatypes.RequestId;
import cloud.filibuster.instrumentation.instrumentors.FilibusterServerInstrumentor;
import cloud.filibuster.instrumentation.storage.ContextStorage;
import cloud.filibuster.instrumentation.storage.ThreadLocalContextStorage;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.SimpleDecoratingHttpService;

import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationEnabledProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationServerCommunicationEnabledProperty;

public class FilibusterDecoratingHttpService extends SimpleDecoratingHttpService {
    private static final Logger logger = Logger.getLogger(FilibusterDecoratingHttpService.class.getName());

    @SuppressWarnings({"FieldCanBeFinal", "NullAway"})
    protected String serviceName;

    @SuppressWarnings({"FieldCanBeFinal", "NullAway"})
    protected ContextStorage contextStorage;

    public static Boolean disableServerCommunication = false;
    public static Boolean disableInstrumentation = false;

    private final String logPrefix = "[FILIBUSTER-ARMERIA_HTTP_SERVICE]: ";

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

    public FilibusterDecoratingHttpService(HttpService delegate) {
        super(delegate);
        this.serviceName = System.getenv("SERVICE_NAME");
        this.contextStorage = new ThreadLocalContextStorage();
    }

    public FilibusterDecoratingHttpService(HttpService delegate, String serviceName) {
        super(delegate);
        this.serviceName = serviceName;
        this.contextStorage = new ThreadLocalContextStorage();
    }

    // ******************************************************************************************
    // Overloads for custom clients.
    // ******************************************************************************************

    protected void setupContext(ServiceRequestContext ctx, HttpRequest req) {

    }

    protected void contextWhenComplete(ServiceRequestContext ctx) {

    }

    protected HttpResponse delegateWithContext(ServiceRequestContext ctx, HttpRequest req) throws Exception {
        HttpService delegate = (HttpService) unwrap();
        return delegate.serve(ctx, req);
    }

    // ******************************************************************************************
    // Accessors for metadata.
    // ******************************************************************************************

    @SuppressWarnings("NullAway")
    public String getRequestIdFromRequestMetadata(HttpRequest req) {
        return req.headers().get("X-Filibuster-Request-Id", RequestId.generateNewRequestId().toString());
    }

    @SuppressWarnings("NullAway")
    public String getGeneratedIdFromRequestMetadata(HttpRequest req) {
        return req.headers().get("X-Filibuster-Generated-Id");
    }

    @SuppressWarnings("NullAway")
    public String getVectorClockFromRequestMetadata(HttpRequest req) {
        return req.headers().get("X-Filibuster-VClock");
    }

    @SuppressWarnings("NullAway")
    public String getOriginVectorClockFromRequestMetadata(HttpRequest req) {
        return req.headers().get("X-Filibuster-Origin-VClock");
    }

    @SuppressWarnings("NullAway")
    public String getDistributedExecutionIndexFromRequestMetadata(HttpRequest req) {
        return req.headers().get("X-Filibuster-Execution-Index");
    }

    // ******************************************************************************************
    // Implementation.
    // ******************************************************************************************

    @Override
    public HttpResponse serve(ServiceRequestContext ctx, HttpRequest req) throws Exception {
        if (! shouldInstrument()) {
            HttpService delegate = (HttpService) unwrap();
            return delegate.serve(ctx, req);
        }

        // ******************************************************************************************
        // Setup Filibuster instrumentation.
        // ******************************************************************************************

        logger.log(Level.SEVERE, logPrefix + "INVOKING CONSTRUCTOR");
        FilibusterServerInstrumentor filibusterServerInstrumentor = new FilibusterServerInstrumentor(
                serviceName,
                shouldCommunicateWithServer(),
                getRequestIdFromRequestMetadata(req),
                getGeneratedIdFromRequestMetadata(req),
                getVectorClockFromRequestMetadata(req),
                getOriginVectorClockFromRequestMetadata(req),
                getDistributedExecutionIndexFromRequestMetadata(req),
                contextStorage
        );
        logger.log(Level.SEVERE, logPrefix + "DONE");

        logger.log(Level.SEVERE, logPrefix + "INVOKING SETUP CONTEXT");
        setupContext(ctx, req);
        logger.log(Level.SEVERE, logPrefix + "DONE INVOKING SETUP CONTEXT");

        // ******************************************************************************************
        // Force sleep if necessary.
        // ******************************************************************************************

        String sleepIntervalStr = req.headers().get("X-Filibuster-Forced-Sleep", "0");
        int sleepInterval = Integer.parseInt(sleepIntervalStr);
        if (sleepInterval > 0) {
            Thread.sleep(sleepInterval * 1000L);
        }

        // ******************************************************************************************
        // Notify Filibuster before delegation.
        // ******************************************************************************************

        filibusterServerInstrumentor.beforeInvocation();

        // ******************************************************************************************
        // Setup completion context assignment.
        // ******************************************************************************************

        contextWhenComplete(ctx);

        // ******************************************************************************************
        // Delegate to underlying service.
        // ******************************************************************************************

        return delegateWithContext(ctx, req);
    }
}