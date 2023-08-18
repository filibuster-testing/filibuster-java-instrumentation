package cloud.filibuster.integration.examples.test_servers;

import cloud.filibuster.examples.APIServiceGrpc;
import cloud.filibuster.instrumentation.datatypes.FilibusterExecutor;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.armeria.http.FilibusterDecoratingHttpService;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterServerInterceptor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterServerInvocationInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.MyAPIService;
import com.linecorp.armeria.common.HttpData;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpResponseWriter;
import com.linecorp.armeria.common.ResponseHeaders;
import com.linecorp.armeria.server.AbstractHttpService;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.grpc.GrpcService;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class APIServer {
    private static final String serviceName = "api_server";

    private APIServer() {

    }

    @SuppressWarnings("Java8ApiChecker")
    public static Server serve() {
        ServerBuilder sb = Server.builder();
        sb.workerGroup(FilibusterExecutor.getNewEventLoopGroup(), /* shutdownOnStop= */true);
        sb.http(Networking.getPort(serviceName));

        ServerServiceDefinition interceptService = ServerInterceptors.intercept(new MyAPIService(), List.of(new FilibusterServerInterceptor(serviceName), new FilibusterServerInvocationInterceptor(APIServiceGrpc.class)));
        sb.service(GrpcService.builder().addService(interceptService).build());

        sb.service("/chunked-json", (ctx, req) -> {
            final HttpResponseWriter streaming = HttpResponse.streaming();
            streaming.write(ResponseHeaders.of(200));
            streaming.write(HttpData.ofUtf8("{"));
            streaming.whenConsumed().thenAccept(v -> {
                streaming.write(HttpData.ofUtf8("\"foo\": \"bar\""));
                streaming.whenConsumed().thenAccept(v2 -> {
                    streaming.write(HttpData.ofUtf8("}"));
                    streaming.close();
                });
            });
            return streaming;
        });

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
                return HttpResponse.of("Hello, world!");
            }
        }.decorate(delegate -> new FilibusterDecoratingHttpService(delegate, serviceName)));

        return sb.build();
    }

    @SuppressWarnings("VoidMissingNullable")
    public static void main(String[] args) {
        Server server = serve();
        CompletableFuture<Void> future = server.start();
        future.join();
    }
}
