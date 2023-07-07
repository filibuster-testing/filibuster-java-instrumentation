package cloud.filibuster.integration.examples.test_servers;

import cloud.filibuster.instrumentation.libraries.grpc.FilibusterServerInvocationInterceptor;
import cloud.filibuster.integration.examples.armeria.grpc.test_services.MyHelloService;
import cloud.filibuster.integration.instrumentation.TestHelper;
import cloud.filibuster.instrumentation.datatypes.FilibusterExecutor;
import cloud.filibuster.instrumentation.datatypes.Pair;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.armeria.http.FilibusterDecoratingHttpService;

import cloud.filibuster.instrumentation.libraries.grpc.FilibusterServerInterceptor;
import cloud.filibuster.integration.instrumentation.libraries.opentelemetry.OpenTelemetryFilibusterDecoratingHttpService;
import cloud.filibuster.integration.instrumentation.libraries.opentelemetry.OpenTelemetryFilibusterServerInterceptor;
import cloud.filibuster.instrumentation.storage.ThreadLocalContextStorage;
import com.google.common.util.concurrent.Uninterruptibles;
import com.linecorp.armeria.client.WebClient;

import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.RequestHeaders;
import com.linecorp.armeria.common.ResponseHeaders;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.HttpRequest;

import com.linecorp.armeria.server.AbstractHttpService;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.grpc.GrpcService;

import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.instrumentation.Constants.FILIBUSTER_EXECUTION_INDEX;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HelloServer {
    private static final Logger logger = Logger.getLogger(HelloServer.class.getName());

    private static final int NUM_THREADS_IN_POOL = 1;

    private static final String serviceName = "hello";

    @Nullable
    private static String initialDistributedExecutionIndex;

    public static boolean useOtelServerInterceptor = false;

    public static int numThreadsSchedulingNondeterminism = 2;

    public static int numRequestsSchedulingNondeterminism = numThreadsSchedulingNondeterminism * 2;

    public static void setInitialDistributedExecutionIndex(@Nullable String ei) {
        initialDistributedExecutionIndex = ei;
    }

    public static void resetInitialDistributedExecutionIndex() {
        initialDistributedExecutionIndex = null;
    }

    public static void setupLocalFixtures() {
        if (initialDistributedExecutionIndex != null) {
            ThreadLocalContextStorage.set(FILIBUSTER_EXECUTION_INDEX, initialDistributedExecutionIndex);
            setInitialDistributedExecutionIndex(null); // mimics behavior where this is unset by the instrumentation after being read once.
        } else {
            ThreadLocalContextStorage.set(FILIBUSTER_EXECUTION_INDEX, null);
        }
    }

    private HelloServer() {

    }

    public static String makeRequestToExternalService() {
        String baseURI = "http://" + Networking.getHost("external") + ":" + Networking.getPort("external") + "/";
        WebClient webClient = TestHelper.getTestWebClient(baseURI, serviceName);
        RequestHeaders getHeaders1 = RequestHeaders.of(HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
        AggregatedHttpResponse response = webClient.execute(getHeaders1).aggregate().join();
        ResponseHeaders headers = response.headers();
        return headers.get(HttpHeaderNames.STATUS);
    }

    @SuppressWarnings("Java8ApiChecker")
    public static Server serve() throws IOException {
        ServerBuilder sb = Server.builder();
        sb.workerGroup(FilibusterExecutor.getNewEventLoopGroup(), /* shutdownOnStop= */true);
        sb.http(Networking.getPort(serviceName));

        // Default gRPC route.
        ServerServiceDefinition interceptService;

        if (useOtelServerInterceptor) {
            interceptService = ServerInterceptors.intercept(new MyHelloService(), List.of(new OpenTelemetryFilibusterServerInterceptor(serviceName, null)));
        } else {
            interceptService = ServerInterceptors.intercept(new MyHelloService(), List.of(new FilibusterServerInterceptor(serviceName), new FilibusterServerInvocationInterceptor()));
        }

        sb.service(GrpcService.builder().addService(interceptService).build());

        sb.service("/not-found", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                return HttpResponse.of(HttpStatus.NOT_FOUND);
            }
        }.decorate(delegate -> new FilibusterDecoratingHttpService(delegate, serviceName)));

        sb.service("/multithreaded", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                setupLocalFixtures();

                class FirstExternalRequestThread extends Thread {
                    private final AtomicReference<String> statusCodeAtomic;

                    FirstExternalRequestThread(AtomicReference<String> statusCodeAtomic) {
                        this.statusCodeAtomic = statusCodeAtomic;
                    }

                    @Override
                    public void run() {
                        String baseURI = "http://" + Networking.getHost("external") + ":" + Networking.getPort("external") + "/";
                        WebClient webClient = TestHelper.getTestWebClient(baseURI, serviceName);
                        RequestHeaders getHeaders1 = RequestHeaders.of(HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
                        AggregatedHttpResponse response = webClient.execute(getHeaders1).aggregate().join();
                        ResponseHeaders headers = response.headers();
                        String statusCode = headers.get(HttpHeaderNames.STATUS);
                        statusCodeAtomic.set(statusCode);
                    }
                }

                class SecondExternalRequestThread extends Thread {
                    private final AtomicReference<String> statusCodeAtomic;

                    SecondExternalRequestThread(AtomicReference<String> statusCodeAtomic) {
                        this.statusCodeAtomic = statusCodeAtomic;
                    }

                    @Override
                    public void run() {
                        String baseURI = "http://" + Networking.getHost("external") + ":" + Networking.getPort("external") + "/";
                        WebClient webClient = TestHelper.getTestWebClient(baseURI, serviceName);
                        RequestHeaders getHeaders1 = RequestHeaders.of(HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
                        AggregatedHttpResponse response = webClient.execute(getHeaders1).aggregate().join();
                        ResponseHeaders headers = response.headers();
                        String statusCode = headers.get(HttpHeaderNames.STATUS);
                        statusCodeAtomic.set(statusCode);
                    }
                }

                // Create thread pool.
                ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_THREADS_IN_POOL);

                // First request.
                AtomicReference<String> firstThreadStatusCode = new AtomicReference<>();
                executor.execute(new FirstExternalRequestThread(firstThreadStatusCode));

                // Second request.
                AtomicReference<String> secondThreadStatusCode = new AtomicReference<>();
                executor.execute(new SecondExternalRequestThread(secondThreadStatusCode));

                // Wait for all to finish, then terminate the thread pool.
                // Fail on any exception.
                while (executor.getTaskCount() != executor.getCompletedTaskCount()){
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
                    }
                }
                executor.shutdown();

                try {
                    executor.awaitTermination(60, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
                }

                if (firstThreadStatusCode.get().equals("200") && secondThreadStatusCode.get().equals("200")) {
                    return HttpResponse.of(HttpStatus.OK);
                } else {
                    return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
                }
            }
        }.decorate(delegate -> new FilibusterDecoratingHttpService(delegate, serviceName)));

        sb.service("/multithreaded-with-same-ei", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                setupLocalFixtures(); // mimics the server instrumentor behavior by setting up ThreadLocal context.

                class ExternalRequestThread extends Thread {
                    private final AtomicReference<String> statusCodeAtomic;

                    ExternalRequestThread(AtomicReference<String> statusCodeAtomic) {
                        this.statusCodeAtomic = statusCodeAtomic;
                    }

                    @Override
                    public void run() {
                        String baseURI = "http://" + Networking.getHost("external") + ":" + Networking.getPort("external") + "/";
                        WebClient webClient = TestHelper.getTestWebClient(baseURI, serviceName);
                        RequestHeaders getHeaders1 = RequestHeaders.of(HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
                        AggregatedHttpResponse response = webClient.execute(getHeaders1).aggregate().join();
                        ResponseHeaders headers = response.headers();
                        String statusCode = headers.get(HttpHeaderNames.STATUS);
                        statusCodeAtomic.set(statusCode);
                    }
                }

                // Create thread pool.
                ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_THREADS_IN_POOL);

                // First request.
                AtomicReference<String> firstThreadStatusCode = new AtomicReference<>();
                executor.execute(new ExternalRequestThread(firstThreadStatusCode));

                // Second request.
                AtomicReference<String> secondThreadStatusCode = new AtomicReference<>();
                executor.execute(new ExternalRequestThread(secondThreadStatusCode));

                // Wait for all to finish, then terminate the thread pool.
                // Fail on any exception.
                while (executor.getTaskCount() != executor.getCompletedTaskCount()){
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
                    }
                }
                executor.shutdown();

                try {
                    executor.awaitTermination(60, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
                }

                if (firstThreadStatusCode.get().equals("200") && secondThreadStatusCode.get().equals("200")) {
                    return HttpResponse.of(HttpStatus.OK);
                } else {
                    return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
                }
            }
        }.decorate(delegate -> new FilibusterDecoratingHttpService(delegate, serviceName)));

        sb.service("/multithreaded-with-futures", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                setupLocalFixtures();

                String firstThreadStatusCode;
                String secondThreadStatusCode;

                CompletableFuture<String> firstRequestFuture = CompletableFuture.supplyAsync(() -> {
                    AtomicReference<String> threadStatusCode = new AtomicReference<>();

                    Thread firstRequestThread = new Thread(() -> {
                        String baseURI = "http://" + Networking.getHost("external") + ":" + Networking.getPort("external") + "/";
                        WebClient webClient = TestHelper.getTestWebClient(baseURI, serviceName);
                        RequestHeaders getHeaders1 = RequestHeaders.of(HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
                        AggregatedHttpResponse response = webClient.execute(getHeaders1).aggregate().join();
                        ResponseHeaders headers = response.headers();
                        String statusCode = headers.get(HttpHeaderNames.STATUS);
                        threadStatusCode.set(statusCode);
                    });
                    firstRequestThread.start();
                    Uninterruptibles.joinUninterruptibly(firstRequestThread);
                    String threadStatusCodeStr = threadStatusCode.get();
                    return threadStatusCodeStr;
                }, FilibusterExecutor.getExecutorService());

                try {
                    firstThreadStatusCode = firstRequestFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    logger.log(Level.SEVERE, "cannot complete firstRequestThread: " + e);
                    return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
                }

                CompletableFuture<String> secondRequestFuture = CompletableFuture.supplyAsync(() -> {
                    AtomicReference<String> threadStatusCode = new AtomicReference<>();

                    Thread secondRequestThread = new Thread(() -> {
                        String baseURI = "http://" + Networking.getHost("external") + ":" + Networking.getPort("external") + "/";
                        WebClient webClient = TestHelper.getTestWebClient(baseURI, serviceName);
                        RequestHeaders getHeaders1 = RequestHeaders.of(HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");
                        AggregatedHttpResponse response = webClient.execute(getHeaders1).aggregate().join();
                        ResponseHeaders headers = response.headers();
                        String statusCode = headers.get(HttpHeaderNames.STATUS);
                        threadStatusCode.set(statusCode);
                    });
                    secondRequestThread.start();
                    Uninterruptibles.joinUninterruptibly(secondRequestThread);
                    return threadStatusCode.get();
                }, FilibusterExecutor.getExecutorService());

                try {
                    secondThreadStatusCode = secondRequestFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    logger.log(Level.SEVERE, "cannot complete firstRequestThread: " + e);
                    return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
                }

                if (firstThreadStatusCode.equals("200") && secondThreadStatusCode.equals("200")) {
                    return HttpResponse.of(HttpStatus.OK);
                } else {
                    return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
                }
            }
        }.decorate(delegate -> new FilibusterDecoratingHttpService(delegate, serviceName)));

        sb.service("/multithreaded-with-futures-with-same-ei", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                setupLocalFixtures();

                String firstThreadStatusCode;
                String secondThreadStatusCode;

                CompletableFuture<String> firstRequestFuture = CompletableFuture.supplyAsync(() -> {
                    AtomicReference<String> threadStatusCode = new AtomicReference<>();
                    Thread firstRequestThread = new Thread(() -> threadStatusCode.set(makeRequestToExternalService()));
                    firstRequestThread.start();
                    Uninterruptibles.joinUninterruptibly(firstRequestThread);
                    String threadStatusCodeStr = threadStatusCode.get();
                    return threadStatusCodeStr;
                }, FilibusterExecutor.getExecutorService());

                try {
                    firstThreadStatusCode = firstRequestFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    logger.log(Level.SEVERE, "cannot complete firstRequestThread: " + e);
                    return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
                }

                CompletableFuture<String> secondRequestFuture = CompletableFuture.supplyAsync(() -> {
                    AtomicReference<String> threadStatusCode = new AtomicReference<>();
                    Thread secondRequestThread = new Thread(() -> threadStatusCode.set(makeRequestToExternalService()));
                    secondRequestThread.start();
                    Uninterruptibles.joinUninterruptibly(secondRequestThread);
                    return threadStatusCode.get();
                }, FilibusterExecutor.getExecutorService());

                try {
                    secondThreadStatusCode = secondRequestFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    logger.log(Level.SEVERE, "cannot complete firstRequestThread: " + e);
                    return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
                }

                if (firstThreadStatusCode.equals("200") && secondThreadStatusCode.equals("200")) {
                    return HttpResponse.of(HttpStatus.OK);
                } else {
                    return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
                }
            }
        }.decorate(delegate -> new FilibusterDecoratingHttpService(delegate, serviceName)));

        sb.service("/multithreaded-with-futures-with-same-ei-twice", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                setupLocalFixtures();

                CompletableFuture<Map.Entry<String, String>> requestFuture = CompletableFuture.supplyAsync(() -> {
                    AtomicReference<String> firstRequestStatusCode = new AtomicReference<>();
                    AtomicReference<String> secondRequestStatusCode = new AtomicReference<>();

                    Thread firstRequestThread = new Thread(() -> firstRequestStatusCode.set(makeRequestToExternalService()));
                    firstRequestThread.start();
                    Uninterruptibles.joinUninterruptibly(firstRequestThread);

                    Thread secondRequestThread = new Thread(() -> secondRequestStatusCode.set(makeRequestToExternalService()));
                    secondRequestThread.start();
                    Uninterruptibles.joinUninterruptibly(secondRequestThread);

                    return Pair.of(firstRequestStatusCode.get(), secondRequestStatusCode.get());
                }, FilibusterExecutor.getExecutorService());

                try {
                    Map.Entry responseCodes = requestFuture.get();

                    if (responseCodes.getKey().equals("200") && responseCodes.getValue().equals("200")) {
                        return HttpResponse.of(HttpStatus.OK);
                    } else {
                        return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    logger.log(Level.SEVERE, "cannot complete firstRequestThread: " + e);
                    return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
                }
            }
        }.decorate(delegate -> new FilibusterDecoratingHttpService(delegate, serviceName)));

        sb.service("/external", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                String baseURI = "http://" + Networking.getHost("external") + ":" + Networking.getPort("external") + "/";
                WebClient webClient1 = TestHelper.getTestWebClient(baseURI, serviceName);
                RequestHeaders getHeaders1 = RequestHeaders.of(HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");

                logger.log(Level.INFO, "/test issuing request to " + baseURI);

                return HttpResponse.from(webClient1.execute(getHeaders1).aggregate().thenApply(aggregatedHttpResponse -> {
                    logger.log(Level.INFO, "/request completed.");

                    ResponseHeaders headers = aggregatedHttpResponse.headers();
                    String statusCode = headers.get(HttpHeaderNames.STATUS);

                    if (statusCode.equals("200")) {
                        return HttpResponse.of("Hello, world!");
                    } else {
                        return HttpResponse.of(HttpStatus.FAILED_DEPENDENCY);
                    }
                }));
            }
          }.decorate(delegate -> new FilibusterDecoratingHttpService(delegate, serviceName)));

        sb.service("/health-check", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("status", "OK");
                return HttpResponse.of(jsonObject.toString());
            }
        });

        sb.service("/world", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                setupLocalFixtures();

                String baseURI = "http://" + Networking.getHost("world") + ":" + Networking.getPort("world") + "/";
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

        sb.service("/world-twice", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                setupLocalFixtures();

                String baseURI = "http://" + Networking.getHost("world") + ":" + Networking.getPort("world") + "/";
                WebClient webClient1 = TestHelper.getTestWebClient(baseURI, serviceName);
                RequestHeaders getHeaders1 = RequestHeaders.of(HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");

                return HttpResponse.from(webClient1.execute(getHeaders1).aggregate()
                        .handle((aggregatedHttpResponse1, cause1) -> {
                            logger.log(Level.INFO, "/request completed.");

                            if (cause1 != null) {
                                return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE); // or whatever the error response.
                            }

                            ResponseHeaders headers1 = aggregatedHttpResponse1.headers();
                            String statusCode1 = headers1.get(HttpHeaderNames.STATUS);

                            if (Objects.equals(statusCode1, "200")) {

                                WebClient webClient2 = TestHelper.getTestWebClient(baseURI, serviceName);
                                RequestHeaders getHeaders2 = RequestHeaders.of(HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");

                                return HttpResponse.from(webClient2.execute(getHeaders2).aggregate()
                                        .handle((aggregatedHttpResponse2, cause2) -> {
                                            logger.log(Level.INFO, "/request completed.");

                                            if (cause2 != null) {
                                                return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE); // or whatever the error response.
                                            }

                                            ResponseHeaders headers2 = aggregatedHttpResponse2.headers();
                                            String statusCode2 = headers2.get(HttpHeaderNames.STATUS);

                                            if (Objects.equals(statusCode2, "200")) {
                                                return HttpResponse.of("Hello, world!");
                                            } else {
                                                return HttpResponse.of(HttpStatus.NOT_FOUND);
                                            }
                                        }));

                            } else {
                                return HttpResponse.of(HttpStatus.NOT_FOUND);
                            }
                        }));
            }
        }.decorate(delegate -> new FilibusterDecoratingHttpService(delegate, serviceName)));

        sb.service("/world-otel", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                setupLocalFixtures();

                String baseURI = "http://" + Networking.getHost("world") + ":" + Networking.getPort("world") + "/";
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

        sb.service("/world-otel-external", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                setupLocalFixtures();

                String baseURI = "http://" + Networking.getHost("world") + ":" + Networking.getPort("world") + "/";
                WebClient webClient1 = TestHelper.getTestWebClient(baseURI, serviceName);
                RequestHeaders getHeaders1 = RequestHeaders.of(HttpMethod.GET, "/external", HttpHeaderNames.ACCEPT, "application/json");

                return HttpResponse.from(webClient1.execute(getHeaders1).aggregate()
                        .handle((aggregatedHttpResponse, cause) -> {
                            logger.log(Level.INFO, "/request completed.");

                            if (cause != null) {
                                return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE); // or whatever the error response.
                            }

                            ResponseHeaders headers = aggregatedHttpResponse.headers();
                            String statusCode = headers.get(HttpHeaderNames.STATUS);

                            if (statusCode.equals("200")) {
                                return HttpResponse.of("Hello, world!");
                            } else {
                                return HttpResponse.of(HttpStatus.NOT_FOUND);
                            }
                        }));
            }
        }.decorate(delegate -> new FilibusterDecoratingHttpService(delegate, serviceName)));

        sb.service("/world-otel-external-otel", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                setupLocalFixtures();

                String baseURI = "http://" + Networking.getHost("world") + ":" + Networking.getPort("world") + "/";
                WebClient webClient1 = TestHelper.getTestWebClientWithOpenTelemetryInstrumentation(baseURI, serviceName);
                RequestHeaders getHeaders1 = RequestHeaders.of(HttpMethod.GET, "/external-otel", HttpHeaderNames.ACCEPT, "application/json");

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

        sb.service("/cycle", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                setupLocalFixtures();

                String baseURI = "http://" + Networking.getHost("world") + ":" + Networking.getPort("world") + "/";
                WebClient webClient1 = TestHelper.getTestWebClient(baseURI, serviceName);
                RequestHeaders getHeaders1 = RequestHeaders.of(HttpMethod.GET, "/hello", HttpHeaderNames.ACCEPT, "application/json");

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

        sb.service("/cycle2", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                setupLocalFixtures();

                String baseURI = "http://" + Networking.getHost("world") + ":" + Networking.getPort("world") + "/";
                WebClient webClient1 = TestHelper.getTestWebClient(baseURI, serviceName);
                RequestHeaders getHeaders1 = RequestHeaders.of(HttpMethod.GET, "/hello2", HttpHeaderNames.ACCEPT, "application/json");

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

        sb.service("/cycle3", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                setupLocalFixtures();

                String baseURI = "http://" + Networking.getHost("world") + ":" + Networking.getPort("world") + "/";
                WebClient webClient1 = TestHelper.getTestWebClient(baseURI, serviceName);
                RequestHeaders getHeaders1 = RequestHeaders.of(HttpMethod.GET, "/hello2", HttpHeaderNames.ACCEPT, "application/json");

                return HttpResponse.from(webClient1.execute(getHeaders1).aggregate()
                        .handle((aggregatedHttpResponse1, cause1) -> {
                            logger.log(Level.INFO, "/request completed.");

                            if (cause1 != null) {
                                return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE); // or whatever the error response.
                            }

                            ResponseHeaders headers1 = aggregatedHttpResponse1.headers();
                            String statusCode1 = headers1.get(HttpHeaderNames.STATUS);

                            if (Objects.equals(statusCode1, "200")) {
                                WebClient webClient2 = TestHelper.getTestWebClient(baseURI, serviceName);
                                RequestHeaders getHeaders2 = RequestHeaders.of(HttpMethod.GET, "/", HttpHeaderNames.ACCEPT, "application/json");

                                return HttpResponse.from(webClient2.execute(getHeaders2).aggregate()
                                        .handle((aggregatedHttpResponse2, cause2) -> {
                                            logger.log(Level.INFO, "/request completed.");

                                            if (cause2 != null) {
                                                return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE); // or whatever the error response.
                                            }

                                            ResponseHeaders headers2 = aggregatedHttpResponse2.headers();
                                            String statusCode2 = headers2.get(HttpHeaderNames.STATUS);

                                            assertNotNull(statusCode2);
                                            if (statusCode2.equals("200")) {
                                                return HttpResponse.of("Hello, world!");
                                            } else {
                                                return HttpResponse.of(HttpStatus.NOT_FOUND);
                                            }
                                        }));

                            } else {
                                return HttpResponse.of(HttpStatus.NOT_FOUND);
                            }
                        }));
            }
        }.decorate(delegate -> new FilibusterDecoratingHttpService(delegate, serviceName)));

        sb.service("/external-post", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                setupLocalFixtures();

                String baseURI = "http://" + Networking.getHost("external") + ":" + Networking.getPort("external") + "/";
                WebClient webClient1 = TestHelper.getTestWebClient(baseURI, serviceName);
                RequestHeaders getHeaders1 = RequestHeaders.of(HttpMethod.POST, "/post", HttpHeaderNames.ACCEPT, "application/json");

                JSONObject payload = new JSONObject();
                payload.put("key1", "value1");

                return HttpResponse.from(webClient1.execute(getHeaders1, payload.toString()).aggregate()
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

        sb.service("/external-put", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                setupLocalFixtures();

                String baseURI = "http://" + Networking.getHost("external") + ":" + Networking.getPort("external") + "/";
                WebClient webClient1 = TestHelper.getTestWebClient(baseURI, serviceName);
                RequestHeaders getHeaders1 = RequestHeaders.of(HttpMethod.PUT, "/put", HttpHeaderNames.ACCEPT, "application/json");

                JSONObject payload = new JSONObject();
                payload.put("key1", "value1");

                return HttpResponse.from(webClient1.execute(getHeaders1, payload.toString()).aggregate()
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

        sb.service("/multithreaded-with-same-ei-detect-scheduling-nondeterminism", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                setupLocalFixtures(); // mimics the server instrumentor behavior by setting up ThreadLocal context.

                class ExternalRequestThread extends Thread {
                    private final AtomicReference<String> statusCodeAtomic;
                    private final int threadNumber;

                    ExternalRequestThread(AtomicReference<String> statusCodeAtomic, int threadNumber) {
                        this.statusCodeAtomic = statusCodeAtomic;
                        this.threadNumber = threadNumber;
                    }

                    @Override
                    public void run() {
                        String baseURI = "http://" + Networking.getHost("external") + ":" + Networking.getPort("external") + "/";
                        WebClient webClient = TestHelper.getTestWebClient(baseURI, serviceName);
                        RequestHeaders getHeaders1 = RequestHeaders.of(HttpMethod.GET, "/?threadNumber=" + threadNumber, HttpHeaderNames.ACCEPT, "application/json");
                        AggregatedHttpResponse response = webClient.execute(getHeaders1).aggregate().join();
                        ResponseHeaders headers = response.headers();
                        String statusCode = headers.get(HttpHeaderNames.STATUS);
                        statusCodeAtomic.set(statusCode);
                    }
                }

                // Create thread pool.
                ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreadsSchedulingNondeterminism);

                for (int i = 0; i < numRequestsSchedulingNondeterminism; i++) {
                    AtomicReference<String> atomicReference = new AtomicReference<>();
                    executor.execute(new ExternalRequestThread(atomicReference, i));
                }

                // Wait for all to finish, then terminate the thread pool.
                // Fail on any exception.
                while (executor.getTaskCount() != executor.getCompletedTaskCount()){
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
                    }
                }
                executor.shutdown();

                return HttpResponse.of(HttpStatus.OK);
            }
        }.decorate(delegate -> new FilibusterDecoratingHttpService(delegate, serviceName)));

        sb.service("/", new AbstractHttpService() {
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                return HttpResponse.of("Hello, world!");
            }
        }.decorate(delegate -> new FilibusterDecoratingHttpService(delegate, serviceName)));

        sb.service("/echo-cookie", new AbstractHttpService() {
            // Return the cookie of the received request
            @Override
            protected @NotNull HttpResponse doGet(@NotNull ServiceRequestContext ctx, @NotNull HttpRequest req) {
                setupLocalFixtures();

                if(req.headers().get("cookie") != null) {
                    return HttpResponse.of(req.headers().get("cookie"));
                } else {
                    return HttpResponse.of(HttpStatus.NOT_FOUND);
                }
            }
        }.decorate(delegate -> new FilibusterDecoratingHttpService(delegate, serviceName)));

        return sb.build();
    }

    @SuppressWarnings("VoidMissingNullable")
    public static void main(String[] args) throws IOException {
        Server server = serve();
        CompletableFuture<Void> future = server.start();
        future.join();
    }
}
