package cloud.filibuster.junit.server;

import cloud.filibuster.junit.configuration.FilibusterConfiguration;

public interface FilibusterServerBackend {
    boolean start(FilibusterConfiguration filibusterConfiguration) throws Throwable;

    boolean stop(FilibusterConfiguration filibusterConfiguration) throws Throwable;
}