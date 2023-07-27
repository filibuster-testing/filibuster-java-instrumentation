package cloud.filibuster.integration.examples.test_servers;
import algorithmjacob.jacobservices.MyBService;
import cloud.filibuster.instrumentation.datatypes.FilibusterExecutor;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.armeria.http.FilibusterDecoratingHttpService;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterServerInterceptor;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterServerInvocationInterceptor;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.AbstractHttpService;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.grpc.GrpcService;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BServer {
    final private static String serviceName = "B";


    @SuppressWarnings("Java8ApiChecker")
    public static Server serve() {
        ServerBuilder sb = Server.builder();
        sb.workerGroup(FilibusterExecutor.getNewEventLoopGroup(), /* shutdownOnStop= */true);
        sb.http(Networking.getPort(serviceName));

        ServerServiceDefinition interceptService = ServerInterceptors.intercept(new MyBService(), List.of(new FilibusterServerInterceptor(serviceName), new FilibusterServerInvocationInterceptor()));
        sb.service(GrpcService.builder().addService(interceptService).build());

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
    public static void main(String[] args) throws IOException {
        //System.out.println("here\n");
        Server server = serve();
        CompletableFuture<Void> future = server.start();
        future.join();
    }
}
