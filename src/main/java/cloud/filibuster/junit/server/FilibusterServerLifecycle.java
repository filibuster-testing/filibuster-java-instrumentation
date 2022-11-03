package cloud.filibuster.junit.server;

import cloud.filibuster.instrumentation.exceptions.FilibusterServerUnavailabilityException;
import cloud.filibuster.junit.configuration.FilibusterConfiguration;
import com.linecorp.armeria.client.WebClient;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.junit.server.FilibusterServerAPI.healthCheck;

public class FilibusterServerLifecycle {
    private static boolean started = false;

    private static final Logger logger = Logger.getLogger(FilibusterServerLifecycle.class.getName());

    public static synchronized void startServer(FilibusterConfiguration filibusterConfiguration, WebClient webClient) throws Throwable {
        if (!started) {
            started = true;

            FilibusterServerBackend filibusterServerBackend = filibusterConfiguration.getFilibusterServerBackend();
            filibusterServerBackend.start(filibusterConfiguration);

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
        }
    }

    @SuppressWarnings("BusyWait")
    public static synchronized void stopServer(FilibusterConfiguration filibusterConfiguration, WebClient webClient) throws Throwable {
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
    }
}
