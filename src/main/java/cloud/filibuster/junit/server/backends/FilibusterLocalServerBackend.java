package cloud.filibuster.junit.server.backends;

import cloud.filibuster.exceptions.filibuster.FilibusterServerNullException;
import cloud.filibuster.junit.FilibusterSearchStrategy;
import cloud.filibuster.junit.configuration.FilibusterConfiguration;
import cloud.filibuster.junit.server.FilibusterServerBackend;
import cloud.filibuster.junit.server.core.FilibusterCore;
import cloud.filibuster.junit.server.local.FilibusterServer;
import com.linecorp.armeria.server.Server;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static cloud.filibuster.instrumentation.helpers.Property.setServerBackendCanInvokeDirectlyProperty;

@SuppressWarnings("Varifier")
public class FilibusterLocalServerBackend implements FilibusterServerBackend {
    private static final Logger logger = Logger.getLogger(FilibusterLocalServerBackend.class.getName());

    @Nullable
    private static Server filibusterServer;

    @Override
    public synchronized boolean start(FilibusterConfiguration filibusterConfiguration) throws InterruptedException {
        // Even though the value of the new appears unused. It is necessary since this FilibusterCore
        // object can be accessed via FilibusterCore.getCurrentInstance
        new FilibusterCore(filibusterConfiguration);

        if (filibusterServer == null) {
            filibusterServer = FilibusterServer.serve();
        }
        if(filibusterServer == null)
        {
            throw new FilibusterServerNullException("The Filibuster Server should not be null at this point.");
        }
        filibusterServer.start();

        setServerBackendCanInvokeDirectlyProperty(true);

        return true;
    }

    @Override
    public synchronized boolean stop(FilibusterConfiguration filibusterConfiguration) {
        if (filibusterServer != null) {
            filibusterServer.stop();
        }

        // Poke the GC once we dereference the FilibusterCore object (via FilibusterServer.)
        System.gc();

        setServerBackendCanInvokeDirectlyProperty(false);

        return true;
    }

    @Override
    public List<FilibusterSearchStrategy> supportedSearchStrategies() {
        return Arrays.asList(FilibusterSearchStrategy.BFS, FilibusterSearchStrategy.DFS);
    }

    @Override
    public FilibusterSearchStrategy defaultSearchStrategy() {
        return FilibusterSearchStrategy.BFS;
    }

    @Override
    public boolean latencyProfileSupported() {
        return true;
    }
}
