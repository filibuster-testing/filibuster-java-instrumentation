package cloud.filibuster.integration.examples.test_servers;

import cloud.filibuster.integration.examples.armeria.grpc.test_services.MyWorldService;
import cloud.filibuster.instrumentation.TestHelper;
import cloud.filibuster.instrumentation.datatypes.FilibusterExecutor;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.grpc.FilibusterServerInterceptor;
import cloud.filibuster.instrumentation.libraries.armeria.http.FilibusterDecoratingHttpService;

import cloud.filibuster.instrumentation.libraries.opentelemetry.OpenTelemetryFilibusterDecoratingHttpService;
import cloud.filibuster.instrumentation.libraries.opentelemetry.OpenTelemetryFilibusterServerInterceptor;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.RequestHeaders;
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

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WorldServer {
    private static final Logger logger = Logger.getLogger(WorldServer.class.getName());

    final private static String serviceName = "world";

    public static boolean shouldReturnServerError = false;

    public static boolean useOtelServerInterceptor = false;

    private WorldServer() {

    }
    @SuppressWarnings("Java8ApiChecker")
    public static Server serve() throws IOException {
        ServerBuilder sb = Server.builder();
        sb.workerGroup(FilibusterExecutor.getNewEventLoopGroup(), /* shutdownOnStop= */true);
        sb.http(Networking.getPort(serviceName));

        // Default gRPC route.
        ServerServiceDefinition interceptService;

        if (useOtelServerInterceptor) {
            interceptService = ServerInterceptors.intercept(new MyWorldService(serviceName), List.of(new OpenTelemetryFilibusterServerInterceptor(serviceName, null)));
        } else {
            interceptService = ServerInterceptors.intercept(new MyWorldService(serviceName), List.of(new FilibusterServerInterceptor(serviceName)));
        }

        sb.service(GrpcService.builder().addService(interceptService).build());

        sb.service("/health-check", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("status", "OK");
                return HttpResponse.of(jsonObject.toString());
            }
        });

        sb.service("/external", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                String baseURI = "http://" + Networking.getHost("external") + ":" + Networking.getPort("external") + "/";
                WebClient webClient1 = TestHelper.getTestWebClient(baseURI, serviceName);
                RequestHeaders getHeaders1 = RequestHeaders.of(HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");

                return HttpResponse.from(webClient1.execute(getHeaders1).aggregate()
                        .handle((aggregatedHttpResponse, cause) -> {
                            logger.log(Level.INFO, "/request completed.");

                            if (cause != null) {
                                return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE); // or whatever the error response.
                            }

                            ResponseHeaders headers = aggregatedHttpResponse.headers();
                            String statusCode = headers.get(HttpHeaderNames.STATUS);

                            if (Objects.equals(statusCode, "200")) {
                                return HttpResponse.of("Hello, world!");
                            } else {
                                return HttpResponse.of(HttpStatus.NOT_FOUND);
                            }
                        }));
            }
        }.decorate(delegate -> new FilibusterDecoratingHttpService(delegate, serviceName)));

        sb.service("/external-otel", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                String baseURI = "http://" + Networking.getHost("external") + ":" + Networking.getPort("external") + "/";
                WebClient webClient1 = TestHelper.getTestWebClientWithOpenTelemetryInstrumentation(baseURI, serviceName);
                RequestHeaders getHeaders1 = RequestHeaders.of(HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");

                return HttpResponse.from(webClient1.execute(getHeaders1).aggregate()
                        .handle((aggregatedHttpResponse, cause) -> {
                            logger.log(Level.INFO, "/request completed.");

                            if (cause != null) {
                                return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE); // or whatever the error response.
                            }

                            ResponseHeaders headers = aggregatedHttpResponse.headers();
                            String statusCode = headers.get(HttpHeaderNames.STATUS);

                            if (Objects.equals(statusCode, "200")) {
                                return HttpResponse.of("Hello, world!");
                            } else {
                                return HttpResponse.of(HttpStatus.NOT_FOUND);
                            }
                        }));
            }
        }.decorate(delegate -> new OpenTelemetryFilibusterDecoratingHttpService(delegate, serviceName, null)));

        sb.service("/hello", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
                WebClient webClient1 = TestHelper.getTestWebClient(baseURI, serviceName);
                RequestHeaders getHeaders1 = RequestHeaders.of(HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");

                return HttpResponse.from(webClient1.execute(getHeaders1).aggregate()
                        .handle((aggregatedHttpResponse, cause) -> {
                            logger.log(Level.INFO, "/request completed.");

                            if (cause != null) {
                                return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE); // or whatever the error response.
                            }

                            ResponseHeaders headers = aggregatedHttpResponse.headers();
                            String statusCode = headers.get(HttpHeaderNames.STATUS);

                            if (Objects.equals(statusCode, "200")) {
                                return HttpResponse.of("Hello, world!");
                            } else {
                                return HttpResponse.of(HttpStatus.NOT_FOUND);
                            }
                        }));
            }
        }.decorate(delegate -> new FilibusterDecoratingHttpService(delegate, serviceName)));

        sb.service("/hello2", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
                WebClient webClient1 = TestHelper.getTestWebClient(baseURI, serviceName);
                RequestHeaders getHeaders1 = RequestHeaders.of(HttpMethod.GET, "/world-otel", HttpHeaderNames.ACCEPT, "application/json");

                return HttpResponse.from(webClient1.execute(getHeaders1).aggregate()
                        .handle((aggregatedHttpResponse, cause) -> {
                            logger.log(Level.INFO, "/request completed.");

                            if (cause != null) {
                                return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE); // or whatever the error response.
                            }

                            ResponseHeaders headers = aggregatedHttpResponse.headers();
                            String statusCode = headers.get(HttpHeaderNames.STATUS);

                            if (Objects.equals(statusCode, "200")) {
                                return HttpResponse.of("Hello, world!");
                            } else {
                                return HttpResponse.of(HttpStatus.NOT_FOUND);
                            }
                        }));
            }
        }.decorate(delegate -> new FilibusterDecoratingHttpService(delegate, serviceName)));

        sb.service("/", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                if (shouldReturnServerError) {
                    return HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR);
                } else {
                    return HttpResponse.of(HttpStatus.OK);
                }
            }
        }.decorate(delegate -> new FilibusterDecoratingHttpService(delegate, serviceName)));

        return sb.build();
    }
}
