package cloud.filibuster.junit.server.backends;

import cloud.filibuster.junit.FilibusterSearchStrategy;
import cloud.filibuster.junit.configuration.FilibusterConfiguration;
import cloud.filibuster.junit.server.FilibusterServerBackend;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FilibusterLocalProcessServerBackend implements FilibusterServerBackend {
    private static final Logger logger = Logger.getLogger(FilibusterLocalProcessServerBackend.class.getName());

    @Nullable
    private static Process filibusterServerProcess;

    @Override
    public synchronized boolean start(FilibusterConfiguration filibusterConfiguration) throws IOException {
        ProcessBuilder filibusterServerProcessBuilder = new ProcessBuilder().command(filibusterConfiguration.toExecutableCommand());
        filibusterServerProcess = filibusterServerProcessBuilder.start();
        return true;
    }

    @Override
    public synchronized boolean stop(FilibusterConfiguration filibusterConfiguration) throws InterruptedException {
        if (filibusterServerProcess != null) {
            filibusterServerProcess.destroyForcibly();
        }

        logger.log(Level.WARNING, "Waiting for Filibuster server to exit.");
        int exitCode = filibusterServerProcess.waitFor();
        logger.log(Level.WARNING, "Exit code for Filibuster:: " + exitCode);

        filibusterServerProcess = null;

        return true;
    }

    @Override
    public List<FilibusterSearchStrategy> supportedSearchStrategies() {
        return Arrays.asList(FilibusterSearchStrategy.DFS);
    }

    @Override
    public boolean latencyProfileSupported() {
        return false;
    }
}
