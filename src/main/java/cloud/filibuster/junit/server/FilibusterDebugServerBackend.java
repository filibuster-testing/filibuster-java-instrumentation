package cloud.filibuster.junit.server;

import cloud.filibuster.junit.configuration.FilibusterConfiguration;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FilibusterDebugServerBackend implements FilibusterServerBackend {
    private static final Logger logger = Logger.getLogger(FilibusterDebugServerBackend.class.getName());

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
}
