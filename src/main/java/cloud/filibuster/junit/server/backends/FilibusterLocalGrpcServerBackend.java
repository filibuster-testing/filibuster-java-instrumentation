package cloud.filibuster.junit.server.backends;

import cloud.filibuster.junit.configuration.FilibusterConfiguration;
import cloud.filibuster.junit.server.FilibusterServerBackend;
import cloud.filibuster.junit.server.core.FilibusterCore;
import cloud.filibuster.junit.server.local_grpc.FilibusterServer;
import com.linecorp.armeria.server.Server;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class FilibusterLocalGrpcServerBackend implements FilibusterServerBackend {
    private static final Logger logger = Logger.getLogger(FilibusterLocalGrpcServerBackend.class.getName());

    @Nullable
    private static Server filibusterServer;

    @Override
    public synchronized boolean start(FilibusterConfiguration filibusterConfiguration) throws InterruptedException {
        FilibusterCore filibusterCore = new FilibusterCore();

        if (filibusterServer == null) {
            filibusterServer = FilibusterServer.serve(filibusterCore);
            CompletableFuture<Void> filibusterServerFuture = filibusterServer.start();
        }

        return true;
    }

    @Override
    public synchronized boolean stop(FilibusterConfiguration filibusterConfiguration) throws InterruptedException {
        if (filibusterServer != null) {
            filibusterServer.close();
            filibusterServer.blockUntilShutdown();
            filibusterServer = null;
        }

        return true;
    }
}
