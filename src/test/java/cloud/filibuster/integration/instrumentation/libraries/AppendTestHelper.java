package cloud.filibuster.integration.instrumentation.libraries;

import cloud.filibuster.instrumentation.datatypes.FilibusterExecutor;
import cloud.filibuster.instrumentation.exceptions.FilibusterServerUnavailabilityException;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.integration.examples.test_servers.AServer;
import cloud.filibuster.integration.examples.test_servers.BServer;
import cloud.filibuster.integration.examples.test_servers.CServer;
import cloud.filibuster.integration.examples.test_servers.DServer;
import cloud.filibuster.integration.instrumentation.TestHelper;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.*;
import com.linecorp.armeria.server.Server;
import org.eclipse.jetty.util.IO;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppendTestHelper {
    private static final Logger logger = Logger.getLogger(TestHelper.class.getName());
    private static Server aServer;

    private static Server bServer;

    private static Server cServer;

    private static Server dServer;


    public static void startAppendServerAndWaitUntilAvailable(String serverName) throws InterruptedException, IOException {
        switch (serverName) {
            case "A":
                aServer = AServer.serve();
                aServer.start();
                break;
            case "B":
                bServer = BServer.serve();
                break;
            case "C":
                cServer = CServer.serve();
                break;
            case "D":
                dServer = DServer.serve();
                break;
            default:
                logger.log(Level.SEVERE, "server does not exist");
                throw new IOException();
        }

        boolean online = false;

        for (int i = 0; i < 10; i++) {
            logger.log(Level.INFO, "Waiting for " + serverName + "Server to come online...");

            try {
                // Get remote resource.
                String baseURI = "http://" + Networking.getHost(serverName) + ":" + Networking.getPort(serverName) + "/";
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
            logger.log(Level.INFO,  serverName + "Server never came online!");
            throw new FilibusterServerUnavailabilityException();
        }
    }

    public static WebClient getTestWebClient(String baseURI) {
        return WebClient.builder(baseURI)
                .factory(FilibusterExecutor.getNewClientFactory(1))
                .build();
    }
}



