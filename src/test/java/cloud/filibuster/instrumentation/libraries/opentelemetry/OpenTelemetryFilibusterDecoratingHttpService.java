package cloud.filibuster.instrumentation.libraries.opentelemetry;

import cloud.filibuster.instrumentation.libraries.armeria.http.FilibusterDecoratingHttpService;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.logging.RequestLog;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.ServiceRequestContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;

import javax.annotation.Nullable;
import java.util.logging.Logger;

public class OpenTelemetryFilibusterDecoratingHttpService extends FilibusterDecoratingHttpService {
    private static final Logger logger = Logger.getLogger(OpenTelemetryFilibusterDecoratingHttpService.class.getName());

    @Nullable
    private final Instrumenter<ServiceRequestContext, RequestLog> serverInstrumenter;

    @SuppressWarnings("NullAway")
    private Context context;

    public OpenTelemetryFilibusterDecoratingHttpService(HttpService delegate, String serviceName, Instrumenter<ServiceRequestContext, RequestLog> serverInstrumenter) {
        super(delegate);
        this.serviceName = serviceName;
        this.serverInstrumenter = serverInstrumenter;
        this.contextStorage = new OpenTelemetryContextStorage();
    }

    public OpenTelemetryFilibusterDecoratingHttpService(HttpService delegate, Instrumenter<ServiceRequestContext, RequestLog> serverInstrumenter) {
        super(delegate);
        this.serviceName = System.getenv("SERVICE_NAME");
        this.serverInstrumenter = serverInstrumenter;
        this.contextStorage = new OpenTelemetryContextStorage();
    }

    @Override
    protected void setupContext(ServiceRequestContext ctx, HttpRequest req) {
        OpenTelemetryContextStorage openTelemetryContextStorage = (OpenTelemetryContextStorage) this.contextStorage;
        context = openTelemetryContextStorage.getContext();

        if (serverInstrumenter != null) {
            context = serverInstrumenter.start(context, ctx);
        }
    }

    @Override
    protected void contextWhenComplete(ServiceRequestContext ctx) {
        ctx.log().whenComplete().thenAccept(log -> {
            if (serverInstrumenter != null) {
                serverInstrumenter.end(context, ctx, log, log.responseCause());
            }
        });
    }

    @Override
    protected HttpResponse delegateWithContext(ServiceRequestContext ctx, HttpRequest req) throws Exception {
        try (Scope ignored = context.makeCurrent()) {
            HttpService delegate = (HttpService) unwrap();
            return delegate.serve(ctx, req);
        }
    }
}
