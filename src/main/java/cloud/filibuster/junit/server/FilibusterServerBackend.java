package cloud.filibuster.junit.server;

import cloud.filibuster.junit.FilibusterSearchStrategy;
import cloud.filibuster.junit.configuration.FilibusterConfiguration;

import java.util.List;

public interface FilibusterServerBackend {
    boolean start(FilibusterConfiguration filibusterConfiguration) throws Throwable;

    boolean stop(FilibusterConfiguration filibusterConfiguration) throws Throwable;

    List<FilibusterSearchStrategy> supportedSearchStrategies();

    FilibusterSearchStrategy defaultSearchStrategy();

    boolean latencyProfileSupported();
}