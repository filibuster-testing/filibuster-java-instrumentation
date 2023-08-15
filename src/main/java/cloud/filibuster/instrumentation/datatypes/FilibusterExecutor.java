package cloud.filibuster.instrumentation.datatypes;

import cloud.filibuster.instrumentation.libraries.armeria.http.FilibusterDecoratingHttpClient;
import com.linecorp.armeria.client.ClientFactory;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.util.EventLoopGroups;
import io.netty.channel.EventLoopGroup;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Helper class for building executor services, thread pools, event loop groups, and web clients.
 */
public class FilibusterExecutor {
    private static final int MAX_FILIBUSTER_INSTRUMENTATION_THREADS = 200;

    // ExecutorService used explicitly for sending messages to the Filibuster instrumentation server.
    private static final ExecutorService executorService = Executors.newFixedThreadPool(MAX_FILIBUSTER_INSTRUMENTATION_THREADS);

    /**
     * Get access to the executor service that's a reserved pool of threads for instrumentation calls to the
     * Filibuster server.
     *
     * @return reference to executor service for instrumentation calls to the Filibuster server.
     */
    public static ExecutorService getExecutorService() {
        return executorService;
    }

    // Private thread group for event loops.
    private static final int MAX_SERVER_EVENT_LOOP_THREADS = 100;

    /**
     * Return a new event loop group of the default size.
     *
     * @return new event loop group, per call.
     */
    public static EventLoopGroup getNewEventLoopGroup() {
        return EventLoopGroups.newEventLoopGroup(MAX_SERVER_EVENT_LOOP_THREADS);
    }

    /**
     * Return a new event loop group of a custom size.
     *
     * @param numThreads the number of threads for the event loop group.
     * @return new event loop group, per call.
     */
    public static EventLoopGroup getClientEventLoopGroup(int numThreads) {
        return EventLoopGroups.newEventLoopGroup(numThreads);
    }

    /**
     * Return a new client factory for web clients with a specified number of threads in the worker group.
     *
     * @param numThreads the number of threads for the worker group.
     * @return a new client factory.
     */
    public static ClientFactory getNewClientFactory(int numThreads) {
        return ClientFactory.builder()
                // or you can just use Integer.MAX_VALUE to use all EventLoops
                .maxNumEventLoopsPerEndpoint(Integer.MAX_VALUE)
                .maxNumEventLoopsPerHttp1Endpoint(Integer.MAX_VALUE)
                .workerGroup(getClientEventLoopGroup(numThreads), /* shutdownOnClose= */true)
                .build();
    }

    /**
     * Return a web client from the common pool.
     *
     * @param baseUri the base URI for the web client.
     * @return a new web client.
     */
    public static WebClient getWebClient(String baseUri) {
        return WebClient.builder(baseUri).build();
    }

    /**
     * Return a decorated web client from the common pool.
     *
     * @param baseUri the base URI for the web client.
     * @param serviceName the name of the service issuing the call.
     * @return a new web client.
     */
    public static WebClient getDecoratedWebClient(String baseUri, String serviceName) {
        return WebClient.builder(baseUri)
                .decorator(delegate -> new FilibusterDecoratingHttpClient(delegate, serviceName))
                .build();
    }
}
