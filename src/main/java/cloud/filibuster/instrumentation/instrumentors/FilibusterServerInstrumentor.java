package cloud.filibuster.instrumentation.instrumentors;

import cloud.filibuster.instrumentation.datatypes.FilibusterExecutor;
import cloud.filibuster.instrumentation.datatypes.VectorClock;
import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.helpers.Response;
import cloud.filibuster.instrumentation.storage.ContextStorage;
import cloud.filibuster.exceptions.filibuster.FilibusterServerBadResponseException;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.RequestHeaders;
import com.linecorp.armeria.common.ResponseHeaders;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.instrumentation.helpers.Counterexample.canLoadCounterexample;
import static cloud.filibuster.instrumentation.helpers.Counterexample.loadCounterexampleAsJsonObjectFromEnvironment;
import static cloud.filibuster.instrumentation.helpers.Counterexample.loadTestExecutionFromCounterexample;

/**
 * Server instrumentor for Filibuster.
 */
@SuppressWarnings("StronglyTypeTime")
final public class FilibusterServerInstrumentor {
    private static final Logger logger = Logger.getLogger(FilibusterServerInstrumentor.class.getName());

    final private String serviceName;
    final private String generatedId;
    private String distributedExecutionIndex;

    final private boolean shouldCommunicateWithServer;

    final private ContextStorage contextStorage;

    @Nullable
    private JSONObject counterexample;

    @Nullable
    private JSONObject counterexampleTestExecution;

    /**
     * @param serviceName name of the service that is running the server instrumentor.
     * @param shouldCommunicateWithServer whether the Filibuster server should be contacted.
     * @param requestId the request identifier.
     * @param generatedId the generated identifier for the request.
     * @param vclockStr a serialized vector clock.
     * @param originVclockStr a serialized origin vector clock.
     * @param distributedExecutionIndex a serialized execution index.
     */
    public FilibusterServerInstrumentor(
            String serviceName,
            boolean shouldCommunicateWithServer,
            String requestId,
            String generatedId,
            @Nullable String vclockStr,
            @Nullable String originVclockStr,
            String distributedExecutionIndex,
            ContextStorage contextStorage
    ) {
        this.serviceName = serviceName;

        this.shouldCommunicateWithServer = shouldCommunicateWithServer;

        this.generatedId = generatedId;

        this.contextStorage = contextStorage;

        VectorClock vectorClock;
        if (vclockStr != null) {
            vectorClock = new VectorClock();
            vectorClock.fromString(vclockStr);
        } else {
            vectorClock = new VectorClock();
        }

        VectorClock originVectorClock;
        if (originVclockStr != null) {
            originVectorClock = new VectorClock();
            originVectorClock.fromString(originVclockStr);
        } else {
            originVectorClock = new VectorClock();
        }

        this.distributedExecutionIndex = distributedExecutionIndex;

        logger.log(Level.INFO, "requestId: " + requestId);
        logger.log(Level.INFO, "generatedId: " + generatedId);
        logger.log(Level.INFO, "vclockStr: " + vclockStr);
        logger.log(Level.INFO, "originVclockStr: " + originVclockStr);
        logger.log(Level.INFO, "executionIndex: " + distributedExecutionIndex);

        contextStorage.setRequestId(requestId);
        contextStorage.setVectorClock(vectorClock);
        contextStorage.setOriginVectorClock(originVectorClock);
        contextStorage.setDistributedExecutionIndex(distributedExecutionIndex);

        if (canLoadCounterexample()) {
            this.counterexample = loadCounterexampleAsJsonObjectFromEnvironment();
            this.counterexampleTestExecution = loadTestExecutionFromCounterexample(counterexample);
        }
    }

    private boolean counterexampleNotProvided() {
        return counterexample == null;
    }

    /**
     * Returns the generated identifier associated with the request.  This will be null unless provided by the caller.
     *
     * @return generated identifier associated with the request.
     */
    public String getGeneratedId() {
        return this.generatedId;
    }

    /**
     * Returns the execution index associated with the request.  This will be null unless provided by the caller.
     *
     * @return execution index associated with this request.
     */
    public String getDistributedExecutionIndex() {
        return this.distributedExecutionIndex;
    }

    /**
     * Invoked before the server processes the request to contact the Filibuster server and notify it as to what
     * service this request has been terminated at.
     */
    @SuppressWarnings("VoidMissingNullable")
    public void beforeInvocation() {
        logger.log(Level.INFO, "beforeInvocation [server]: about to make call.");

        if (getDistributedExecutionIndex() != null) {
            JSONObject payload = new JSONObject();
            payload.put("instrumentation_type", "request_received");
            payload.put("generated_id", getGeneratedId());
            payload.put("target_service_name", serviceName);
            payload.put("execution_index", getDistributedExecutionIndex());

            logger.log(Level.INFO, "payload: " + payload);

            if (shouldCommunicateWithServer && counterexampleNotProvided()) {
                CompletableFuture<String> updateFuture = CompletableFuture.supplyAsync(() -> {
                    String uri = "http://" + Networking.getFilibusterHost() + ":" + Networking.getFilibusterPort() + "/";
                    logger.log(Level.INFO, "making call to filibuster server, update with body: " + payload);
                    logger.log(Level.INFO, "URI: " + uri);
                    WebClient webClient = FilibusterExecutor.getWebClient("http://" + Networking.getFilibusterHost() + ":" + Networking.getFilibusterPort() + "/");

                    RequestHeaders postJson = RequestHeaders.of(
                            HttpMethod.POST,
                            "/filibuster/update",
                            HttpHeaderNames.CONTENT_TYPE,
                            "application/json",
                            "X-Filibuster-Instrumentation",
                            "true",
                            "X-Filibuster-Is-Update",
                            String.valueOf(false));
                    AggregatedHttpResponse response = webClient.execute(postJson, payload.toString()).aggregate().join();

                    ResponseHeaders headers = response.headers();
                    String statusCode = headers.get(HttpHeaderNames.STATUS);

                    if (statusCode == null) {
                        FilibusterServerBadResponseException.logAndThrow("beforeInvocation, statusCode: null");
                    }

                    if (!Objects.equals(statusCode, "200")) {
                        FilibusterServerBadResponseException.logAndThrow("beforeInvocation, statusCode: " + statusCode);
                    }

                    JSONObject jsonObject = Response.aggregatedHttpResponseToJsonObject(response);

                    if (jsonObject.has("execution_index")) {
                        distributedExecutionIndex = jsonObject.getString("execution_index");
                        return distributedExecutionIndex;
                    }

                    return null;
                }, FilibusterExecutor.getExecutorService());

                try {
                    String newDistributedExecutionIndex = updateFuture.get();

                    if (newDistributedExecutionIndex != null) {
                        logger.log(Level.INFO, "rewriting EI from: " + contextStorage.getDistributedExecutionIndex() + " to " + distributedExecutionIndex);
                        contextStorage.setDistributedExecutionIndex(distributedExecutionIndex);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    logger.log(Level.SEVERE, "cannot get information from Filibuster server: " + e);
                }

                logger.log(Level.INFO, "beforeInvocation [server]: finished.");

                logger.log(Level.INFO, "call complete.");
            } else {
                logger.log(Level.INFO, "skipping!");
            }
        }
    }
}
