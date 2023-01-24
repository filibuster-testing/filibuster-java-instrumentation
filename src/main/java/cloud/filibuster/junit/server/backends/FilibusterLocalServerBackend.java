package cloud.filibuster.junit.server.backends;

import cloud.filibuster.junit.configuration.FilibusterConfiguration;
import cloud.filibuster.junit.server.FilibusterServerBackend;
import cloud.filibuster.junit.server.core.FilibusterCore;
import cloud.filibuster.junit.server.local.FilibusterServer;
import com.linecorp.armeria.server.Server;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static cloud.filibuster.instrumentation.helpers.Property.setServerBackendCanInvokeDirectlyProperty;

@SuppressWarnings("Varifier")
public class FilibusterLocalServerBackend implements FilibusterServerBackend {
    private static final Logger logger = Logger.getLogger(FilibusterLocalServerBackend.class.getName());

    @Nullable
    private static Server filibusterServer;

    @Override
    public synchronized boolean start(FilibusterConfiguration filibusterConfiguration) throws InterruptedException {
        FilibusterCore filibusterCore = new FilibusterCore(filibusterConfiguration);

        if (filibusterServer == null) {
            filibusterServer = FilibusterServer.serve(filibusterCore);

            if (filibusterServer != null) {
                CompletableFuture<Void> filibusterServerFuture = filibusterServer.start();
            }
        }

        setServerBackendCanInvokeDirectlyProperty(true);

        return true;
    }

    @Override
    public synchronized boolean stop(FilibusterConfiguration filibusterConfiguration) throws InterruptedException {
        if (filibusterServer != null) {
            filibusterServer.close();
            filibusterServer.blockUntilShutdown();
            filibusterServer = null;
        }

        setServerBackendCanInvokeDirectlyProperty(false);

        return true;
    }
}
