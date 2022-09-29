package cloud.filibuster.examples.test_servers;

import cloud.filibuster.instrumentation.datatypes.FilibusterExecutor;

import cloud.filibuster.instrumentation.helpers.Networking;
import com.linecorp.armeria.common.AggregatedHttpRequest;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.server.AbstractHttpService;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;

import com.linecorp.armeria.server.annotation.ConsumesJson;
import com.linecorp.armeria.server.annotation.Post;
import com.linecorp.armeria.server.annotation.ProducesJson;
import com.linecorp.armeria.server.annotation.Put;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;

public class ExternalServer {
    final private static String serviceName = "external";

    private ExternalServer() {

    }

    public static Server serve() throws IOException {
        ServerBuilder sb = Server.builder();
        sb.workerGroup(FilibusterExecutor.getNewEventLoopGroup(), /* shutdownOnStop= */true);
        sb.http(Networking.getPort(serviceName));

        sb.service("/health-check", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("status", "OK");
                return HttpResponse.of(jsonObject.toString());
            }
        });

        sb.service("/", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                return HttpResponse.of(HttpStatus.OK);
            }
        });

        sb.annotatedService(new Object() {
            @Post("/post")
            @ProducesJson
            @ConsumesJson
            public HttpResponse update(AggregatedHttpRequest request) {
                return HttpResponse.of(HttpStatus.OK);
            }
        });

        sb.annotatedService(new Object() {
            @Put("/put")
            @ProducesJson
            @ConsumesJson
            public HttpResponse create(AggregatedHttpRequest request) {
                return HttpResponse.of(HttpStatus.OK);
            }
        });

        sb.service("/404", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                return HttpResponse.of(HttpStatus.NOT_FOUND);
            }
        });

        return sb.build();
    }
}
