package cloud.filibuster.integration.instrumentation;

import cloud.filibuster.instrumentation.datatypes.FilibusterExecutor;
import cloud.filibuster.instrumentation.exceptions.FilibusterServerUnavailabilityException;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.libraries.armeria.http.FilibusterDecoratingHttpClient;
import cloud.filibuster.integration.examples.test_servers.*;

import cloud.filibuster.integration.instrumentation.libraries.opentelemetry.OpenTelemetryFilibusterDecoratingHttpClient;

import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.client.grpc.GrpcClientBuilder;
import com.linecorp.armeria.client.grpc.GrpcClients;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.RequestHeaders;
import com.linecorp.armeria.common.ResponseHeaders;
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats;
import com.linecorp.armeria.server.Server;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestHelper {
    private static final Logger logger = Logger.getLogger(TestHelper.class.getName());

    private static Server apiServer;

    private static Server helloServer;

    private static Server aServer;

    private static Server bServer;

    private static Server cServer;

    private static Server dServer;
    private static Server worldServer;

    private static Server externalServer;

    @Nullable
    private static Server filibusterServer;

    final private static int HEALTH_CHECK_TIMEOUT = 10;

    final public static int HTTP_TIMEOUT = 60;

    private TestHelper() {

    }

    @SuppressWarnings("unchecked")
    public static void setEnv(String key, String value) {
        try {
            Map<String, String> env = System.getenv();
            Field field = env.getClass().getDeclaredField("m");
            field.setAccessible(true);
            ((Map<String, String>) field.get(env)).put(key, value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set environment variable", e);
        }
    }

    @SuppressWarnings("VoidMissingNullable")
    public static synchronized void startMockFilibusterServerAndWaitUntilAvailable() throws IOException, InterruptedException {
        FilibusterServerFake.resetPayloadsReceived();

        if (filibusterServer == null) {
            filibusterServer = FilibusterServerFake.serve();
            CompletableFuture<Void> filibusterServerFuture = filibusterServer.start();
        }

        boolean online = false;

        for (int i = 0; i < 10; i++) {
            logger.log(Level.INFO, "Waiting for FilibusterServer to come online...");

            try {
                // Get remote resource.
                String baseURI = "http://" + Networking.getFilibusterHost() + ":" + Networking.getFilibusterPort() + "/";
                WebClient webClient = getTestWebClient(baseURI);
                RequestHeaders getHeaders = RequestHeaders.of(
                        HttpMethod.GET, "/health-check", HttpHeaderNames.ACCEPT, "application/json");
                AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

                // Get headers and verify a 200 OK response.
                ResponseHeaders headers = response.headers();
                String statusCode = headers.get(HttpHeaderNames.STATUS);

                if (statusCode.equals("200")) {
                    logger.log(Level.INFO, "Available!");
                    online = true;
                    break;
                } else {
                    logger.log(Level.INFO, "Didn't get proper response, status code: " + statusCode);
                }
            } catch (RuntimeException e) {
                // Nothing, we'll try again.
            }

            logger.log(Level.INFO, "Sleeping one second...");
            Thread.sleep(1000);
        }

        if (!online) {
            logger.log(Level.INFO, "FilibusterServer never came online!");
            throw new FilibusterServerUnavailabilityException();
        }
    }

    public static synchronized void stopMockFilibusterServerAndWaitUntilUnavailable() throws InterruptedException {
        if (filibusterServer != null) {
            filibusterServer.close();
            filibusterServer.blockUntilShutdown();
            filibusterServer = null;
        }

        boolean offline = false;

        while (!offline) {
            logger.log(Level.INFO, "Waiting for FilibusterServer to stop...");

            try {
                // Get remote resource.
                String baseURI = "http://" + Networking.getFilibusterHost() + ":" + Networking.getFilibusterPort() + "/";
                WebClient webClient = getTestWebClient(baseURI);
                RequestHeaders getHeaders = RequestHeaders.of(
                        HttpMethod.GET, "/health-check", HttpHeaderNames.ACCEPT, "application/json");
                AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

                // Get headers and verify a 200 OK response.
                ResponseHeaders headers = response.headers();
                String statusCode = headers.get(HttpHeaderNames.STATUS);

                if (statusCode.equals("200")) {
                    logger.log(Level.INFO, "Still available!");
                } else {
                    logger.log(Level.INFO, "Status code: " + statusCode);
                }
            } catch (RuntimeException e) {
                offline = true;
                break;
            }

            logger.log(Level.INFO, "Sleeping one second; offline: " + offline);
            Thread.sleep(1000);
        }

        logger.log(Level.INFO, "Filibuster server stopped!");
    }

    @SuppressWarnings("VoidMissingNullable")
    public static void startWorldServerAndWaitUntilAvailable() throws InterruptedException, IOException {
        worldServer = WorldServer.serve();
        CompletableFuture<Void> worldServerFuture = worldServer.start();

        // Wait up to 10 seconds for HelloServer to start.
        boolean online = false;

        for (int i = 0; i < 10; i++) {
            logger.log(Level.INFO, "Waiting for WorldServer to come online...");

            try {
                // Get remote resource.
                String baseURI = "http://" + Networking.getHost("world") + ":" + Networking.getPort("world") + "/";
                WebClient webClient = getTestWebClient(baseURI);
                RequestHeaders getHeaders = RequestHeaders.of(
                        HttpMethod.GET, "/health-check", HttpHeaderNames.ACCEPT, "application/json");
                AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

                // Get headers and verify a 200 OK response.
                ResponseHeaders headers = response.headers();
                String statusCode = headers.get(HttpHeaderNames.STATUS);

                if (statusCode.equals("200")) {
                    logger.log(Level.INFO, "Available, still returning 200.");
                    online = true;
                    break;
                } else {
                    logger.log(Level.INFO, "Didn't get proper response, status code: " + statusCode);
                }
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "Runtime exception occurred: " + e);
            }

            logger.log(Level.INFO, "Sleeping one second...");
            Thread.sleep(1000);
        }

        if (!online) {
            logger.log(Level.INFO, "WorldServer never came online!");
            throw new FilibusterServerUnavailabilityException();
        }
    }

    public static void stopWorldServerAndWaitUntilUnavailable() throws InterruptedException {
        worldServer.close();
        worldServer.blockUntilShutdown();
    }

    @SuppressWarnings("VoidMissingNullable")
    public static void startHelloServerAndWaitUntilAvailable() throws InterruptedException, IOException {
        helloServer = HelloServer.serve();
        CompletableFuture<Void> helloServerFuture = helloServer.start();

        // Wait up to 10 seconds for HelloServer to start.
        boolean online = false;

        for (int i = 0; i < 10; i++) {
            logger.log(Level.INFO, "Waiting for HelloServer to come online...");

            try {
                // Get remote resource.
                String baseURI = "http://" + Networking.getHost("hello") + ":" + Networking.getPort("hello") + "/";
                WebClient webClient = getTestWebClient(baseURI);
                RequestHeaders getHeaders = RequestHeaders.of(
                        HttpMethod.GET, "/health-check", HttpHeaderNames.ACCEPT, "application/json");
                AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

                // Get headers and verify a 200 OK response.
                ResponseHeaders headers = response.headers();
                String statusCode = headers.get(HttpHeaderNames.STATUS);

                if (statusCode.equals("200")) {
                    logger.log(Level.INFO, "Available!");
                    online = true;
                    break;
                } else {
                    logger.log(Level.INFO, "Didn't get proper response, status code: " + statusCode);
                }
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "Runtime exception occurred: " + e);
            }

            logger.log(Level.INFO, "Sleeping one second...");
            Thread.sleep(1000);
        }

        if (!online) {
            logger.log(Level.INFO, "HelloServer never came online!");
            throw new FilibusterServerUnavailabilityException();
        }
    }
    public static void startAServerAndWaitUntilAvailable() throws InterruptedException, IOException {
        aServer = AServer.serve();
        CompletableFuture<Void> helloServerFuture = aServer.start();

        // Wait up to 10 seconds for HelloServer to start.
        boolean online = false;

        for (int i = 0; i < 10; i++) {
            logger.log(Level.INFO, "Waiting for HelloServer to come online...");

            try {
                // Get remote resource.
                String baseURI = "http://" + Networking.getHost("A") + ":" + Networking.getPort("A") + "/";
                WebClient webClient = getTestWebClient(baseURI);
                RequestHeaders getHeaders = RequestHeaders.of(
                        HttpMethod.GET, "/health-check", HttpHeaderNames.ACCEPT, "application/json");
                AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

                // Get headers and verify a 200 OK response.
                ResponseHeaders headers = response.headers();
                String statusCode = headers.get(HttpHeaderNames.STATUS);

                if (statusCode.equals("200")) {
                    logger.log(Level.INFO, "Available!");
                    online = true;
                    break;
                } else {
                    logger.log(Level.INFO, "Didn't get proper response, status code: " + statusCode);
                }
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "Runtime exception occurred: " + e);
            }

            logger.log(Level.INFO, "Sleeping one second...");
            Thread.sleep(1000);
        }

        if (!online) {
            logger.log(Level.INFO, "HelloServer never came online!");
            throw new FilibusterServerUnavailabilityException();
        }
    }

    public static void startBServerAndWaitUntilAvailable() throws InterruptedException, IOException {
        bServer = BServer.serve();
        CompletableFuture<Void> helloServerFuture = bServer.start();

        // Wait up to 10 seconds for HelloServer to start.
        boolean online = false;

        for (int i = 0; i < 10; i++) {
            logger.log(Level.INFO, "Waiting for HelloServer to come online...");

            try {
                // Get remote resource.
                String baseURI = "http://" + Networking.getHost("B") + ":" + Networking.getPort("B") + "/";
                WebClient webClient = getTestWebClient(baseURI);
                RequestHeaders getHeaders = RequestHeaders.of(
                        HttpMethod.GET, "/health-check", HttpHeaderNames.ACCEPT, "application/json");
                AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

                // Get headers and verify a 200 OK response.
                ResponseHeaders headers = response.headers();
                String statusCode = headers.get(HttpHeaderNames.STATUS);

                if (statusCode.equals("200")) {
                    logger.log(Level.INFO, "Available!");
                    online = true;
                    break;
                } else {
                    logger.log(Level.INFO, "Didn't get proper response, status code: " + statusCode);
                }
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "Runtime exception occurred: " + e);
            }

            logger.log(Level.INFO, "Sleeping one second...");
            Thread.sleep(1000);
        }

        if (!online) {
            logger.log(Level.INFO, "HelloServer never came online!");
            throw new FilibusterServerUnavailabilityException();
        }
    }

    public static void startCServerAndWaitUntilAvailable() throws InterruptedException, IOException {
        cServer = CServer.serve();
        CompletableFuture<Void> helloServerFuture = cServer.start();

        // Wait up to 10 seconds for HelloServer to start.
        boolean online = false;

        for (int i = 0; i < 10; i++) {
            logger.log(Level.INFO, "Waiting for HelloServer to come online...");

            try {
                // Get remote resource.
                String baseURI = "http://" + Networking.getHost("C") + ":" + Networking.getPort("C") + "/";
                WebClient webClient = getTestWebClient(baseURI);
                RequestHeaders getHeaders = RequestHeaders.of(
                        HttpMethod.GET, "/health-check", HttpHeaderNames.ACCEPT, "application/json");
                AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

                // Get headers and verify a 200 OK response.
                ResponseHeaders headers = response.headers();
                String statusCode = headers.get(HttpHeaderNames.STATUS);

                if (statusCode.equals("200")) {
                    logger.log(Level.INFO, "Available!");
                    online = true;
                    break;
                } else {
                    logger.log(Level.INFO, "Didn't get proper response, status code: " + statusCode);
                }
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "Runtime exception occurred: " + e);
            }

            logger.log(Level.INFO, "Sleeping one second...");
            Thread.sleep(1000);
        }

        if (!online) {
            logger.log(Level.INFO, "HelloServer never came online!");
            throw new FilibusterServerUnavailabilityException();
        }
    }

    public static void startDServerAndWaitUntilAvailable() throws InterruptedException, IOException {
        dServer = DServer.serve();
        CompletableFuture<Void> helloServerFuture = dServer.start();

        // Wait up to 10 seconds for HelloServer to start.
        boolean online = false;

        for (int i = 0; i < 10; i++) {
            logger.log(Level.INFO, "Waiting for HelloServer to come online...");

            try {
                // Get remote resource.
                String baseURI = "http://" + Networking.getHost("D") + ":" + Networking.getPort("D") + "/";
                WebClient webClient = getTestWebClient(baseURI);
                RequestHeaders getHeaders = RequestHeaders.of(
                        HttpMethod.GET, "/health-check", HttpHeaderNames.ACCEPT, "application/json");
                AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

                // Get headers and verify a 200 OK response.
                ResponseHeaders headers = response.headers();
                String statusCode = headers.get(HttpHeaderNames.STATUS);

                if (statusCode.equals("200")) {
                    logger.log(Level.INFO, "Available!");
                    online = true;
                    break;
                } else {
                    logger.log(Level.INFO, "Didn't get proper response, status code: " + statusCode);
                }
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "Runtime exception occurred: " + e);
            }

            logger.log(Level.INFO, "Sleeping one second...");
            Thread.sleep(1000);
        }

        if (!online) {
            logger.log(Level.INFO, "HelloServer never came online!");
            throw new FilibusterServerUnavailabilityException();
        }
    }

    public static void stopHelloServerAndWaitUntilUnavailable() throws InterruptedException {
        helloServer.close();
        helloServer.blockUntilShutdown();
    }

    @SuppressWarnings("VoidMissingNullable")
    public static void startExternalServerAndWaitUntilAvailable() throws InterruptedException, IOException {
        externalServer = ExternalServer.serve();
        CompletableFuture<Void> worldServerFuture = externalServer.start();

        // Wait up to 10 seconds for HelloServer to start.
        boolean online = false;

        for (int i = 0; i < 10; i++) {
            logger.log(Level.INFO, "Waiting for ExternalServer to come online...");

            try {
                // Get remote resource.
                String baseURI = "http://" + Networking.getHost("external") + ":" + Networking.getPort("external") + "/";
                WebClient webClient = getTestWebClient(baseURI);
                RequestHeaders getHeaders = RequestHeaders.of(
                        HttpMethod.GET, "/health-check", HttpHeaderNames.ACCEPT, "application/json");
                AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

                // Get headers and verify a 200 OK response.
                ResponseHeaders headers = response.headers();
                String statusCode = headers.get(HttpHeaderNames.STATUS);

                if (statusCode.equals("200")) {
                    logger.log(Level.INFO, "Available, still returning 200.");
                    online = true;
                    break;
                } else {
                    logger.log(Level.INFO, "Didn't get proper response, status code: " + statusCode);
                }
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "Runtime exception occurred: " + e);
            }

            logger.log(Level.INFO, "Sleeping one second...");
            Thread.sleep(1000);
        }

        if (!online) {
            logger.log(Level.INFO, "ExternalServer never came online!");
            throw new FilibusterServerUnavailabilityException();
        }
    }

    public static void stopExternalServerAndWaitUntilUnavailable() throws InterruptedException {
        externalServer.close();
        externalServer.blockUntilShutdown();
    }

    // Used by Filibuster internal telemetry calls and test suite.
    public static WebClient getTestWebClient(String baseURI) {
        return WebClient.builder(baseURI)
                .factory(FilibusterExecutor.getNewClientFactory(1))
                .build();
    }

    // Used by Filibuster internal telemetry calls and test suite.
    public static WebClient getTestWebClient(String baseURI, String serviceName) {
        return WebClient.builder(baseURI)
                .factory(FilibusterExecutor.getNewClientFactory(1))
                .decorator(delegate -> new FilibusterDecoratingHttpClient(delegate, serviceName))
                .build();
    }

    // Used by Filibuster internal telemetry calls and test suite.
    public static WebClient getTestWebClientWithOpenTelemetryInstrumentation(String baseURI, String serviceName) {
        return WebClient.builder(baseURI)
                .factory(FilibusterExecutor.getNewClientFactory(1))
                .decorator(delegate -> new OpenTelemetryFilibusterDecoratingHttpClient(delegate, serviceName, null))
                .build();
    }

    public static GrpcClientBuilder getGrpcClientBuilder(String baseURI, String serviceName) {

        return GrpcClients.builder(baseURI)
                .serializationFormat(GrpcSerializationFormats.PROTO)
                .responseTimeoutMillis(10000)
                .decorator(delegate -> new FilibusterDecoratingHttpClient(delegate, serviceName));
    }

    public static void startAPIServerAndWaitUntilAvailable() throws InterruptedException, IOException {
        apiServer = APIServer.serve();
        CompletableFuture<Void> apiServerFuture = apiServer.start();

        // Wait up to 10 seconds for APIServer to start.
        boolean online = false;

        for (int i = 0; i < 10; i++) {
            logger.log(Level.INFO, "Waiting for APIServer to come online...");

            try {
                // Get remote resource.
                String baseURI = "http://" + Networking.getHost("api_server") + ":" + Networking.getPort("api_server") + "/";
                WebClient webClient = getTestWebClient(baseURI);
                RequestHeaders getHeaders = RequestHeaders.of(
                        HttpMethod.GET, "/health-check", HttpHeaderNames.ACCEPT, "application/json");
                AggregatedHttpResponse response = webClient.execute(getHeaders).aggregate().join();

                // Get headers and verify a 200 OK response.
                ResponseHeaders headers = response.headers();
                String statusCode = headers.get(HttpHeaderNames.STATUS);

                if (statusCode.equals("200")) {
                    logger.log(Level.INFO, "Available!");
                    online = true;
                    break;
                } else {
                    logger.log(Level.INFO, "Didn't get proper response, status code: " + statusCode);
                }
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "Runtime exception occurred: " + e);
            }

            logger.log(Level.INFO, "Sleeping one second...");
            Thread.sleep(1000);
        }

        if (!online) {
            logger.log(Level.INFO, "APIServer never came online!");
            throw new FilibusterServerUnavailabilityException();
        }
    }

    public static void stopAPIServerAndWaitUntilUnavailable() throws InterruptedException {
        apiServer.close();
        apiServer.blockUntilShutdown();
    }
}
