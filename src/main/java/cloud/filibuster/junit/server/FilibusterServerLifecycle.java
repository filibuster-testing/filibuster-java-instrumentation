package cloud.filibuster.junit.server;

import cloud.filibuster.instrumentation.datatypes.FilibusterExecutor;
import cloud.filibuster.instrumentation.exceptions.FilibusterServerUnavailabilityException;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.junit.configuration.FilibusterConfiguration;
import com.linecorp.armeria.client.WebClient;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.junit.server.FilibusterServerAPI.healthCheck;

public class FilibusterServerLifecycle {
    private static boolean started = false;

    private static final Logger logger = Logger.getLogger(FilibusterServerLifecycle.class.getName());

    private final static WebClient getNewWebClient() {
        String filibusterBaseUri =  "http://" + Networking.getFilibusterHost() + ":" + Networking.getFilibusterPort() + "/";

        return WebClient.builder(filibusterBaseUri)
                .factory(FilibusterExecutor.getNewClientFactory(1))
                .build();
    }

    public static synchronized WebClient startServer(FilibusterConfiguration filibusterConfiguration) throws Throwable {
        if (!started) {
            started = true;

            FilibusterServerBackend filibusterServerBackend = filibusterConfiguration.getFilibusterServerBackend();
            filibusterServerBackend.start(filibusterConfiguration);

            WebClient webClient = getNewWebClient();

            boolean online = false;

            for (int i = 0; i < 10; i++) {
                logger.log(Level.INFO, "Waiting for FilibusterServer to come online...");

                try {
                    online = healthCheck(webClient);
                    if (online) {
                        break;
                    }
                } catch (RuntimeException | ExecutionException e) {
                    // Nothing, try again.
                }

                logger.log(Level.INFO, "Sleeping one second...");
                Thread.sleep(1000);
            }

            if (!online) {
                logger.log(Level.INFO, "FilibusterServer never came online!");
                throw new FilibusterServerUnavailabilityException();
            }

            return webClient;
        } else {
            return getNewWebClient();
        }
    }

    @SuppressWarnings("BusyWait")
    @Nullable
    public static synchronized WebClient stopServer(FilibusterConfiguration filibusterConfiguration, WebClient webClient) throws Throwable {
        if (started) {

            FilibusterServerBackend filibusterServerBackend = filibusterConfiguration.getFilibusterServerBackend();
            filibusterServerBackend.stop(filibusterConfiguration);

            while (true) {
                logger.log(Level.INFO, "Waiting for FilibusterServer to stop...");

                try {
                    healthCheck(webClient);
                } catch (RuntimeException | ExecutionException e) {
                    break;
                }

                logger.log(Level.INFO, "Sleeping one second until offline.");
                Thread.sleep(1000);
            }

            logger.log(Level.INFO, "Filibuster server stopped!");

            started = false;
        }

        return null;
    }
}
