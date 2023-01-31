package cloud.filibuster.integration.instrumentation.libraries.opentelemetry;

import cloud.filibuster.instrumentation.libraries.armeria.http.FilibusterDecoratingHttpClient;
import com.linecorp.armeria.client.ClientRequestContext;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.logging.RequestLog;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;

import javax.annotation.Nullable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OpenTelemetryFilibusterDecoratingHttpClient extends FilibusterDecoratingHttpClient {
    private static final Logger logger = Logger.getLogger(OpenTelemetryFilibusterDecoratingHttpClient.class.getName());

    @Nullable
    private final Instrumenter<ClientRequestContext, RequestLog> clientInstrumentor;

    @SuppressWarnings("NullAway")
    private Context parentContext;

    @SuppressWarnings("NullAway")
    private Context context;

    public OpenTelemetryFilibusterDecoratingHttpClient(HttpClient delegate, String serviceName, Instrumenter<ClientRequestContext, RequestLog> clientInstrumentor) {
        super(delegate);
        this.serviceName = serviceName;
        this.clientInstrumentor = clientInstrumentor;
        this.contextStorage = new OpenTelemetryContextStorage();
    }

    public OpenTelemetryFilibusterDecoratingHttpClient(HttpClient delegate, Instrumenter<ClientRequestContext, RequestLog> clientInstrumentor) {
        super(delegate);
        this.serviceName = System.getenv("SERVICE_NAME");
        this.clientInstrumentor = clientInstrumentor;
        this.contextStorage = new OpenTelemetryContextStorage();
    }

    @Override
    protected void setupContext(ClientRequestContext ctx, HttpRequest req) {
        this.parentContext = Context.current();
        this.context = Context.current();

        logger.log(Level.INFO, "****************************************************************");
        logger.log(Level.SEVERE, "CLIENT parentContext: " + parentContext.toString());
        logger.log(Level.INFO, "****************************************************************");

        if (clientInstrumentor != null) {
            this.context = clientInstrumentor.start(Context.current(), ctx);
        }

        logger.log(Level.INFO, "****************************************************************");
        logger.log(Level.SEVERE, "CLIENT context: " + context.toString());
        logger.log(Level.INFO, "****************************************************************");
    }

    @Override
    protected void contextWhenComplete(ClientRequestContext ctx) {
        ctx.log().whenComplete().thenAccept(log -> {
            if (clientInstrumentor != null) {
                clientInstrumentor.end(context, ctx, log, log.responseCause());
            }
        });
    }

    @Override
    protected HttpResponse delegateWithContext(ClientRequestContext ctx, HttpRequest req) throws Exception {
        HttpResponse response;

        try (Scope ignored = context.makeCurrent()) {
            logger.log(Level.INFO, "!!!!!!! with context: " + context.toString());
            response = unwrap().execute(ctx, req);
        }

        return response;
    }
}
