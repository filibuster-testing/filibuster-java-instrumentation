package cloud.filibuster.instrumentation.instrumentors;

import cloud.filibuster.RpcType;
import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.dei.DistributedExecutionIndexType;
import cloud.filibuster.exceptions.filibuster.FilibusterRuntimeException;
import cloud.filibuster.instrumentation.datatypes.Callsite;
import cloud.filibuster.instrumentation.datatypes.CallsiteArguments;
import cloud.filibuster.instrumentation.datatypes.FilibusterExecutor;
import cloud.filibuster.instrumentation.datatypes.RequestId;
import cloud.filibuster.instrumentation.datatypes.VectorClock;

import cloud.filibuster.instrumentation.helpers.Networking;
import cloud.filibuster.instrumentation.helpers.Response;
import cloud.filibuster.instrumentation.storage.ContextStorage;
import cloud.filibuster.exceptions.filibuster.FilibusterServerBadResponseException;
import cloud.filibuster.junit.server.core.FilibusterCore;
import cloud.filibuster.junit.server.core.transformers.Accumulator;
import com.google.gson.Gson;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.ResponseHeaders;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.RequestHeaders;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.instrumentation.helpers.Counterexample.canLoadCounterexample;
import static cloud.filibuster.instrumentation.helpers.Counterexample.loadCounterexampleAsJSONObjectFromEnvironment;
import static cloud.filibuster.instrumentation.helpers.Counterexample.loadTestExecutionFromCounterexample;
import static cloud.filibuster.instrumentation.helpers.Counterexample.shouldFailRequestWithOrDefault;

import static cloud.filibuster.instrumentation.helpers.Property.getClientInstrumentorUseOverrideRequestIdProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getServerBackendCanInvokeDirectlyProperty;
import static cloud.filibuster.instrumentation.instrumentors.FilibusterLocks.distributedExecutionIndexLock;
import static cloud.filibuster.instrumentation.instrumentors.FilibusterLocks.vectorClockLock;

/**
 * Client instrumentor for Filibuster.
 */
@SuppressWarnings({"StronglyTypeTime", "Varifier"})
final public class FilibusterClientInstrumentor {
    private static final Logger logger = Logger.getLogger(FilibusterClientInstrumentor.class.getName());

    /**
     * Mapping between requests and the current vector clock for that request.
     */
    private static HashMap<String, HashMap<String, VectorClock>> vectorClocksByRequest = new HashMap<>();

    private final String outgoingRequestId;

    /**
     * Get the vector clock request mapping.
     *
     * @return vector clock request map.
     */
    public static HashMap<String, HashMap<String, VectorClock>> getVectorClocksByRequest() {
        return vectorClocksByRequest;
    }

    /**
     * Mapping between requests and the current execution index for that request.
     */
    private static Map<String, HashMap<String, DistributedExecutionIndex>> distributedExecutionIndexByRequest = new HashMap<>();

    /**
     * Get the execution index request mapping.
     *
     * @return execution index request map.
     */
    public static Map<String, HashMap<String, DistributedExecutionIndex>> getDistributedExecutionIndexByRequest() {
        return distributedExecutionIndexByRequest;
    }

    /**
     * Set the current vector clock for a given request id.
     *
     * @param requestId   request identifier.
     * @param vectorClock vector clock.
     */
    public static void setVectorClockForRequestId(String serviceName, String requestId, VectorClock vectorClock) {
        if (vectorClocksByRequest.containsKey(serviceName)) {
            HashMap<String, VectorClock> vectorClockMap = vectorClocksByRequest.get(serviceName);
            vectorClockMap.put(requestId, vectorClock);
        } else {
            HashMap<String, VectorClock> vectorClockMap = new HashMap<>();
            vectorClockMap.put(requestId, vectorClock);
            vectorClocksByRequest.put(serviceName, vectorClockMap);
        }
    }

    /**
     * Does an entry exist in the vector clock request mapping for a particular request id?
     *
     * @param serviceName the service name.
     * @param requestId   the request identifier.
     * @return whether a mapping exists.
     */
    public static boolean vectorClockForRequestIdExists(String serviceName, String requestId) {
        if (!vectorClocksByRequest.containsKey(serviceName)) {
            return false;
        }

        HashMap<String, VectorClock> vectorClockMap = vectorClocksByRequest.get(serviceName);
        return vectorClockMap.containsKey(requestId);
    }

    /**
     * Reset the vector clock request mapping.
     *
     * @param serviceName the service name.
     */
    public static void clearVectorClockForRequestId(String serviceName) {
        vectorClocksByRequest.remove(serviceName);
    }

    /**
     * Reset vector clock mapping.
     */
    public static void clearVectorClockForRequestId() {
        vectorClocksByRequest = new HashMap<>();
    }

    /**
     * Reset the execution index request mapping.
     *
     * @param serviceName the service name.
     */
    public static void clearDistributedExecutionIndexForRequestId(String serviceName) {
        distributedExecutionIndexByRequest.remove(serviceName);
    }

    /**
     * Reset execution index mapping.
     */
    public static void clearDistributedExecutionIndexForRequestId() {
        distributedExecutionIndexByRequest = new HashMap<>();
    }

    /**
     * Set the current execution index for a given request id.
     *
     * @param requestId                 request identifier.
     * @param distributedExecutionIndex execution index.
     */
    public static void setDistributedExecutionIndexForRequestId(String serviceName, String requestId, DistributedExecutionIndex distributedExecutionIndex) {
        if (distributedExecutionIndexByRequest.containsKey(serviceName)) {
            HashMap<String, DistributedExecutionIndex> distributedExecutionIndexMap = distributedExecutionIndexByRequest.get(serviceName);
            distributedExecutionIndexMap.put(requestId, distributedExecutionIndex);
        } else {
            HashMap<String, DistributedExecutionIndex> distributedExecutionIndexMap = new HashMap<>();
            distributedExecutionIndexMap.put(requestId, distributedExecutionIndex);
            distributedExecutionIndexByRequest.put(serviceName, distributedExecutionIndexMap);
        }
    }

    /**
     * Return the current vector clock for a given request id.
     *
     * @param requestId request identifier.
     * @return vector clock.
     */
    public static VectorClock getVectorClockForServiceNameAndRequestId(
            String serviceName, String requestId, VectorClock defaultVectorClock) {
        HashMap<String, VectorClock> vectorClocksByRequestId = vectorClocksByRequest.getOrDefault(serviceName, new HashMap<>());
        return vectorClocksByRequestId.getOrDefault(requestId, defaultVectorClock);
    }

    /**
     * Return the current execution index for a given request id.
     *
     * @param requestId request identifier.
     * @return execution index.
     */
    @SuppressWarnings("NullAway")
    public static DistributedExecutionIndex getDistributedExecutionIndexForServiceNameAndRequestId(
            String serviceName, String requestId, DistributedExecutionIndex defaultExecutionIndex) {
        HashMap<String, DistributedExecutionIndex> distributedExecutionIndexByRequestId = distributedExecutionIndexByRequest.getOrDefault(serviceName, new HashMap<>());
        return distributedExecutionIndexByRequestId.getOrDefault(requestId, defaultExecutionIndex);
    }

    final private String filibusterHost;
    final private int filibusterPort;
    final private String serviceName;
    final private boolean shouldCommunicateWithServer;
    private Callsite callsite;

    private int generatedId;
    private VectorClock vectorClock;
    private DistributedExecutionIndex distributedExecutionIndex;

    @Nullable
    private VectorClock originVectorClock;

    @Nullable
    private JSONObject forcedException;

    @Nullable
    private JSONObject failureMetadata;

    @Nullable
    private JSONObject byzantineFault;

    @Nullable
    private JSONObject transformerFault;

    private String requestId;
    public static String overrideRequestId;
    private final ContextStorage contextStorage;

    final private String filibusterBaseUri;

    @Nullable
    private JSONObject counterexample;

    @Nullable
    private JSONObject counterexampleTestExecution;

    @Nullable
    private DistributedExecutionIndex preliminaryDistributedExecutionIndex;

    @Nullable
    RpcType rpcType;

    final private static String filibusterServiceName = "filibuster-instrumentation";

    /**
     * Create an instance of a Filibuster client instrumentor for controlling fault injection.
     *
     * @param serviceName                 the name of the service being instrumented.
     * @param shouldCommunicateWithServer whether the system should communicate with the Filibuster server.
     * @param contextStorage              context storage.
     */
    public FilibusterClientInstrumentor(
            String serviceName,
            boolean shouldCommunicateWithServer,
            ContextStorage contextStorage,
            Callsite callsite
    ) {
        this.filibusterHost = Networking.getFilibusterHost();
        this.filibusterPort = Networking.getFilibusterPort();
        this.filibusterBaseUri = "http://" + getFilibusterHost() + ":" + getFilibusterPort() + "/";

        this.serviceName = serviceName;
        this.callsite = callsite;
        this.shouldCommunicateWithServer = shouldCommunicateWithServer;
        this.contextStorage = contextStorage;

        this.requestId = contextStorage.getRequestId();

        this.outgoingRequestId = RequestId.generateNewRequestId().toString();

        this.originVectorClock = contextStorage.getOriginVectorClock();

        this.distributedExecutionIndex = DistributedExecutionIndexType.getImplType().createImpl();

        this.generatedId = -1;

        if (canLoadCounterexample()) {
            this.counterexample = loadCounterexampleAsJSONObjectFromEnvironment();
            this.counterexampleTestExecution = loadTestExecutionFromCounterexample(counterexample);
        }
    }

    private boolean counterexampleNotProvided() {
        return counterexample == null;
    }

    /**
     * Set the value of the preliminary execution index.
     *
     * @param preliminaryDistributedExecutionIndex execution index.
     */
    public void setPreliminaryDistributedExecutionIndex(DistributedExecutionIndex preliminaryDistributedExecutionIndex) {
        this.preliminaryDistributedExecutionIndex = preliminaryDistributedExecutionIndex;
    }

    /**
     * Get the value of the preliminary execution index.
     *
     * @return execution index.
     */
    public DistributedExecutionIndex getPreliminaryDistributedExecutionIndex() {
        return preliminaryDistributedExecutionIndex;
    }

    /**
     * Allow re-specification of the callsite.
     *
     * @param callsite callsite
     */
    public void updateCallsite(Callsite callsite) {
        this.callsite = callsite;

        // ******************************************************************************************
        // Repeat execution index work.
        // ******************************************************************************************

        synchronized (distributedExecutionIndexLock) {
            DistributedExecutionIndex incrementedDistributedExecutionIndex = FilibusterClientInstrumentor.getDistributedExecutionIndexForServiceNameAndRequestId(serviceName, getRequestId(), DistributedExecutionIndexType.getImplType().createImpl());

            // Now, we increment the execution index to reflect the request we're about to make.
            incrementedDistributedExecutionIndex.push(callsite);

            // Clone, and store in this object, so we have the EI associated with this request
            // and can then issue a request to Filibuster server when the call completes.
            distributedExecutionIndex = (DistributedExecutionIndex) incrementedDistributedExecutionIndex.clone();

            // Pop on the shared value in the global context (stored by reference) to reset it back to before
            // this request, but ensures that counters associated with the EI advance.
            incrementedDistributedExecutionIndex.pop();

            FilibusterClientInstrumentor.setDistributedExecutionIndexForRequestId(serviceName, getRequestId(), incrementedDistributedExecutionIndex);
        }
    }

    public void setRpcType(@Nullable RpcType rpcType) {
        this.rpcType = rpcType;
    }

    /**
     * Return the current callsite.
     *
     * @return callsite
     */
    public Callsite getCallsite() {
        return callsite;
    }

    /**
     * Return the host of the Filibuster instrumentation server.
     *
     * @return host.
     */
    public String getFilibusterHost() {
        return this.filibusterHost;
    }

    /**
     * Return the port of the Filibuster instrumentation server.
     *
     * @return port number.
     */
    public int getFilibusterPort() {
        return this.filibusterPort;
    }

    /**
     * Return the Filibuster request identifier associated with this instrumentation call.
     * This will return null until the invocation has been prepared, unless it has been provided by the caller in
     * the constructor.
     *
     * @return request identifier.
     */
    public String getRequestId() {
        if (getClientInstrumentorUseOverrideRequestIdProperty()) {
            return overrideRequestId;
        }

        return this.requestId;
    }

    /**
     * Get outgoing request id.
     *
     * @return request identifier.
     */
    public String getOutgoingRequestId() {
        return this.outgoingRequestId;
    }

    /**
     * Set the request identifier.
     *
     * @param requestId a request identifier.
     */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     * Return the vector clock associated with this instrumentation call.
     * This will return null until the invocation has been prepared.
     *
     * @return vector clock.
     */
    public VectorClock getVectorClock() {
        return this.vectorClock;
    }

    /**
     * Return the execution index associated with this instrumentation call.
     * This will return null until the invocation has been prepared.
     *
     * @return execution index.
     */
    public DistributedExecutionIndex getDistributedExecutionIndex() {
        return this.distributedExecutionIndex;
    }

    /**
     * Return the origin vector clock associated with this instrumentation call.
     * This value is provided by the caller in the constructor.
     *
     * @return vector clock.
     */
    public VectorClock getOriginVectorClock() {
        return this.originVectorClock;
    }

    /**
     * Return the generated identifier for the request (as assigned by the Filibuster server.)
     * This value will be null until the Filibuster server has been contacted for this request.
     *
     * @return generated id.
     */
    public int getGeneratedId() {
        return this.generatedId;
    }

    /**
     * Return exception fault that needs to be injected.
     * This value will be null until the Filibuster server has been contacted for this request.
     *
     * @return JSON object containing exception to inject.
     */
    public JSONObject getForcedException() {
        return this.forcedException;
    }

    /**
     * Return failure that needs to be injected.
     * This value will be null until the Filibuster server has been contacted for this request.
     *
     * @return JSON object containing failure to inject.
     */
    public JSONObject getFailureMetadata() {
        return this.failureMetadata;
    }

    /**
     * Return byzantine fault that needs to be injected.
     * This value will be null until the Filibuster server has been contacted for this request.
     *
     * @return JSON object containing failure to inject.
     */
    public JSONObject getByzantineFault() {
        return this.byzantineFault;
    }

    /**
     * Return transformer fault that needs to be injected.
     * This value will be null until the Filibuster server has been contacted for this request.
     *
     * @return JSON object containing failure to inject.
     */
    public JSONObject getTransformerFault() {
        return this.transformerFault;
    }

    /**
     * Should this request be allowed to reach the remote service or should it be skipped?
     *
     * @return whether it should be aborted or not.
     */
    public boolean shouldAbort() {
        // Only forcedException or failureMetadata will be set at a given time.

        // forcedException set, therefore, if metadata is present and abort is present and not true, return false;
        if (forcedException != null) {
            if (forcedException.has("metadata")) {
                JSONObject forcedExceptionMetadata = forcedException.getJSONObject("metadata");

                if (forcedExceptionMetadata.has("abort") && !forcedExceptionMetadata.getBoolean("abort")) {
                    return false;
                }
            }
        }

        // failureMetadata set, therefore, if metadata is present and abort is present and not true, return false;
        if (failureMetadata != null) {
            return !failureMetadata.has("abort") || failureMetadata.getBoolean("abort");
        }

        // Otherwise, assume true;
        return true;
    }

    /**
     * Determine if the client instrumentor should reset the service's vector clock.  Used by the testing infrastructure for a soft reset.
     *
     * @return whether the service's vector clock should be reset.
     */
    public boolean shouldResetClocks() {
        logger.log(Level.INFO, "shouldResetClocks: about to make call.");

        if (shouldCommunicateWithServer && counterexampleNotProvided()) {
            if (getServerBackendCanInvokeDirectlyProperty()) {
                if (FilibusterCore.hasCurrentInstance()) {
                    return FilibusterCore.getCurrentInstance().isNewTestExecution(serviceName);
                } else {
                    throw new FilibusterRuntimeException("No current filibuster core instance, this could indicate a problem.");
                }
            } else {
                CompletableFuture<Boolean> shouldResetClocks = CompletableFuture.supplyAsync(() -> {
                    try {
                        // Call instrumentation using instrumentation to verify short-circuit.
                        WebClient webClient = FilibusterExecutor.getDecoratedWebClient(filibusterBaseUri, filibusterServiceName);

                        RequestHeaders postJson = RequestHeaders.of(
                                HttpMethod.GET,
                                "/filibuster/new-test-execution/" + serviceName,
                                HttpHeaderNames.ACCEPT,
                                "application/json",
                                "X-Filibuster-Instrumentation",
                                "true");
                        AggregatedHttpResponse response = webClient.execute(postJson).aggregate().join();

                        ResponseHeaders headers = response.headers();
                        String statusCode = headers.get(HttpHeaderNames.STATUS);

                        if (statusCode == null) {
                            FilibusterServerBadResponseException.logAndThrow("shouldResetClocks, statusCode: null");
                        }

                        if (!Objects.equals(statusCode, "200")) {
                            FilibusterServerBadResponseException.logAndThrow("shouldResetClocks, statusCode: " + statusCode);
                        }

                        JSONObject jsonObject = Response.aggregatedHttpResponseToJsonObject(response);

                        return jsonObject.getBoolean("new-test-execution");
                    } catch (RuntimeException e) {
                        logger.log(Level.SEVERE, "cannot connect to the Filibuster server: " + e);
                        return false;
                    }
                }, FilibusterExecutor.getExecutorService());

                try {
                    logger.log(Level.INFO, "shouldResetClocks: finished.");
                    return shouldResetClocks.get();
                } catch (InterruptedException | ExecutionException e) {
                    logger.log(Level.SEVERE, "cannot get information from Filibuster server: " + e);
                    return false;
                }
            }
        }

        return false;
    }

    /**
     * Invoked before the remote call is handled to set up internal state of the instrumentor.
     */
    public void prepareForInvocation() {
        // ******************************************************************************************
        // Start clock reset.
        // ******************************************************************************************

        logger.log(Level.INFO, "requestId: " + getRequestId());

        // Should we reset the clocks?
        if (shouldResetClocks()) {
            // Clear out existing clocks.
            FilibusterClientInstrumentor.clearVectorClockForRequestId(serviceName);
            FilibusterClientInstrumentor.clearDistributedExecutionIndexForRequestId(serviceName);
        }

        // Setup new vector clock for this request.
        if (!vectorClockForRequestIdExists(serviceName, getRequestId())) {
            FilibusterClientInstrumentor.setVectorClockForRequestId(serviceName, getRequestId(), new VectorClock());
        }

        // ******************************************************************************************
        // Populate execution maps (and potentially rewrite request identifier.)
        // ******************************************************************************************

        FilibusterContextHelpers.populateExecutionMapsFromContextStorage(serviceName, contextStorage, getRequestId());

        logger.log(Level.INFO, "NEW requestId: " + getRequestId());

        // ******************************************************************************************
        // Start vector clock work.
        // ******************************************************************************************

        synchronized (vectorClockLock) {
            VectorClock incrementedVectorClock = FilibusterClientInstrumentor.getVectorClockForServiceNameAndRequestId(serviceName, getRequestId(), new VectorClock());
            incrementedVectorClock.incrementClock(serviceName);
            FilibusterClientInstrumentor.setVectorClockForRequestId(serviceName, getRequestId(), incrementedVectorClock);
            vectorClock = incrementedVectorClock.clone();
        }

        // ******************************************************************************************
        // Start execution index work.
        // ******************************************************************************************

        synchronized (distributedExecutionIndexLock) {
            DistributedExecutionIndex incrementedDistributedExecutionIndex = FilibusterClientInstrumentor.getDistributedExecutionIndexForServiceNameAndRequestId(serviceName, getRequestId(), DistributedExecutionIndexType.getImplType().createImpl());

            // Now, we increment the execution index to reflect the request we're about to make.
            incrementedDistributedExecutionIndex.push(callsite);

            // Clone, and store in this object, so we have the EI associated with this request
            // and can then issue a request to Filibuster server when the call completes.
            distributedExecutionIndex = (DistributedExecutionIndex) incrementedDistributedExecutionIndex.clone();

            // Pop on the shared value in the global context (stored by reference) to reset it back to before
            // this request, but ensures that counters associated with the EI advance.
            incrementedDistributedExecutionIndex.pop();

            FilibusterClientInstrumentor.setDistributedExecutionIndexForRequestId(serviceName, getRequestId(), incrementedDistributedExecutionIndex);
        }

        // ******************************************************************************************
        // Start origin vector clock work.
        // ******************************************************************************************

        if (originVectorClock == null) {
            originVectorClock = new VectorClock();
        }

        logger.log(Level.INFO, "originVectorClock: " + originVectorClock);
    }

    /**
     * Invoked directly before a remote call is issued.
     * <p>
     * Notifies Filibuster of the remote call that is about to
     * occur and determines if the remote call should instead, return a fault.
     */
    @SuppressWarnings("VoidMissingNullable")
    public void beforeInvocation() {
        logger.log(Level.INFO, "beforeInvocation: about to make call.");

        JSONObject invocationMetadata = new JSONObject();

        if (rpcType != null) {
            invocationMetadata.put("rpc_type", rpcType.toString());
        } else {
            invocationMetadata.put("rpc_type", "");
        }

        JSONObject invocationPayload = new JSONObject();
        invocationPayload.put("instrumentation_type", "invocation");
        invocationPayload.put("source_service_name", serviceName);
        invocationPayload.put("module", callsite.getClassOrModuleName());
        invocationPayload.put("method", callsite.getMethodOrFunctionName());
        CallsiteArguments callsiteArguments = callsite.getCallsiteArguments();
        JSONObject invocationArguments = callsiteArguments.toJsonObject();
        invocationPayload.put("args", invocationArguments);
        invocationPayload.put("kwargs", new JSONObject());
        invocationPayload.put("callsite_file", callsite.getFileName());
        invocationPayload.put("callsite_line", callsite.getLineNumber());
        invocationPayload.put("full_traceback", callsite.getSerializedStackTrace());
        invocationPayload.put("metadata", invocationMetadata);
        invocationPayload.put("vclock", vectorClock.toJSONObject());
        invocationPayload.put("origin_vclock", originVectorClock.toJSONObject());
        invocationPayload.put("execution_index", distributedExecutionIndex.toString());

        if (preliminaryDistributedExecutionIndex != null) {
            invocationPayload.put("preliminary_execution_index", preliminaryDistributedExecutionIndex.toString());
        }

        if (!counterexampleNotProvided()) {
            logger.log(Level.INFO, "Not contacting server; replaying from counterexample file.");

            JSONObject jsonObject = shouldFailRequestWithOrDefault(distributedExecutionIndex.toString(), counterexampleTestExecution);

            if (jsonObject.has("forced_exception")) {
                forcedException = jsonObject.getJSONObject("forced_exception");
            }

            if (jsonObject.has("failure_metadata")) {
                failureMetadata = jsonObject.getJSONObject("failure_metadata");
            }

            if (jsonObject.has("byzantine_fault")) {
                byzantineFault = jsonObject.getJSONObject("byzantine_fault");
            }

            if (jsonObject.has("transformer_fault")) {
                transformerFault = jsonObject.getJSONObject("transformer_fault");
            }
        } else if (shouldCommunicateWithServer && counterexampleNotProvided()) {
            if (getServerBackendCanInvokeDirectlyProperty()) {
                if (FilibusterCore.hasCurrentInstance()) {
                    JSONObject jsonObject = FilibusterCore.getCurrentInstance().beginInvocation(invocationPayload);
                    generatedId = jsonObject.getInt("generated_id");

                    if (jsonObject.has("forced_exception")) {
                        forcedException = jsonObject.getJSONObject("forced_exception");
                    }

                    if (jsonObject.has("failure_metadata")) {
                        failureMetadata = jsonObject.getJSONObject("failure_metadata");
                    }

                    if (jsonObject.has("byzantine_fault")) {
                        byzantineFault = jsonObject.getJSONObject("byzantine_fault");
                    }

                    if (jsonObject.has("transformer_fault")) {
                        transformerFault = jsonObject.getJSONObject("transformer_fault");
                    }
                } else {
                    throw new FilibusterRuntimeException("No current filibuster core instance, this could indicate a problem.");
                }
            } else {
                CompletableFuture<Void> createFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        // Call instrumentation using instrumentation to verify short-circuit.
                        WebClient webClient = FilibusterExecutor.getDecoratedWebClient(filibusterBaseUri, filibusterServiceName);

                        RequestHeaders postJson = RequestHeaders.of(
                                HttpMethod.PUT,
                                "/filibuster/create",
                                HttpHeaderNames.CONTENT_TYPE,
                                "application/json",
                                "X-Filibuster-Instrumentation",
                                "true");
                        AggregatedHttpResponse response = webClient.execute(
                                postJson, invocationPayload.toString()).aggregate().join();

                        ResponseHeaders headers = response.headers();
                        String statusCode = headers.get(HttpHeaderNames.STATUS);

                        if (statusCode == null) {
                            FilibusterServerBadResponseException.logAndThrow("beforeInvocation, statusCode: null");
                        }

                        if (!Objects.equals(statusCode, "200")) {
                            FilibusterServerBadResponseException.logAndThrow("beforeInvocation, statusCode: " + statusCode);
                        }

                        JSONObject jsonObject = Response.aggregatedHttpResponseToJsonObject(response);
                        generatedId = jsonObject.getInt("generated_id");

                        if (jsonObject.has("forced_exception")) {
                            forcedException = jsonObject.getJSONObject("forced_exception");
                        }

                        if (jsonObject.has("failure_metadata")) {
                            failureMetadata = jsonObject.getJSONObject("failure_metadata");
                        }

                        if (jsonObject.has("byzantine_fault")) {
                            byzantineFault = jsonObject.getJSONObject("byzantine_fault");
                        }

                        if (jsonObject.has("transformer_fault")) {
                            transformerFault = jsonObject.getJSONObject("transformer_fault");
                        }
                    } catch (RuntimeException e) {
                        logger.log(Level.SEVERE, "cannot connect to the Filibuster server: " + e);
                    }

                    return null;
                }, FilibusterExecutor.getExecutorService());

                try {
                    createFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    logger.log(Level.SEVERE, "cannot get information from Filibuster server: " + e);
                }
            }
        }

        logger.log(Level.INFO, "beforeInvocation: finished.");
    }

    /**
     * Invoked after a remote call has been completed if the remote call threw an exception.
     *
     * @param throwable the exception thrown.
     */
    public void afterInvocationWithException(
            Throwable throwable
    ) {
        HashMap<String, String> additionalMetadata = new HashMap<>();
        afterInvocationWithException(throwable, additionalMetadata);
    }

    /**
     * Invoked after a remote call has been completed if the remote call threw an exception.
     *
     * @param throwable          the exception thrown.
     * @param additionalMetadata any additional metadata that should be provided, such as if the request was aborted.
     */
    public void afterInvocationWithException(
            Throwable throwable,
            HashMap<String, String> additionalMetadata
    ) {
        String exceptionName = throwable.getClass().getName();
        String exceptionCause = throwable.getCause() != null ? throwable.getCause().getClass().getName() : null;
        afterInvocationWithException(exceptionName, exceptionCause, additionalMetadata);
    }

    /**
     * Invoked after a remote call has been completed if the remote call threw an exception.
     *
     * @param exceptionName      the fully qualified name of the exception thrown.
     * @param exceptionCause     the fully qualified name of the cause (an exception), if provided.
     * @param additionalMetadata any additional metadata that should be provided, such as if the request was aborted.
     */
    public void afterInvocationWithException(
            String exceptionName,
            String exceptionCause,
            HashMap<String, String> additionalMetadata
    ) {
        if (generatedId > -1 && shouldCommunicateWithServer && counterexampleNotProvided()) {
            JSONObject metadata = new JSONObject();

            if (getForcedException() != null) {
                JSONObject forcedExceptionMetadata = getForcedException().getJSONObject("metadata");

                if (forcedExceptionMetadata.has("sleep")) {
                    metadata.put("sleep", forcedExceptionMetadata.get("sleep"));
                }

                if (forcedExceptionMetadata.has("abort")) {
                    metadata.put("abort", forcedExceptionMetadata.get("abort"));
                }
            }

            metadata.put("cause", exceptionCause);

            for (Map.Entry<String, String> entry : additionalMetadata.entrySet()) {
                metadata.put(entry.getKey(), entry.getValue());
            }

            JSONObject exception = new JSONObject();
            exception.put("name", exceptionName);
            exception.put("metadata", metadata);

            JSONObject invocationCompletePayload = new JSONObject();
            invocationCompletePayload.put("instrumentation_type", "invocation_complete");
            invocationCompletePayload.put("generated_id", generatedId);
            invocationCompletePayload.put("execution_index", distributedExecutionIndex.toString());
            invocationCompletePayload.put("vclock", vectorClock.toJSONObject());
            invocationCompletePayload.put("exception", exception);
            invocationCompletePayload.put("module", callsite.getClassOrModuleName());
            invocationCompletePayload.put("method", callsite.getMethodOrFunctionName());

            if (preliminaryDistributedExecutionIndex != null) {
                invocationCompletePayload.put("preliminary_execution_index", preliminaryDistributedExecutionIndex.toString());
            }

            recordInvocationComplete(invocationCompletePayload);
        }
    }


    /**
     * Invoked after a remote call has been completed if the remote call injects a byzantine value.
     *
     * @param value the byzantine value that was injected.
     * @param type  type of the injected byzantine value (e.g., String).
     */
    public void afterInvocationWithByzantineFault(
            String value,
            String type
    ) {
        if (generatedId > -1 && shouldCommunicateWithServer && counterexampleNotProvided()) {

            JSONObject byzantineFault = new JSONObject();
            byzantineFault.put("value", value);
            byzantineFault.put("type", type);

            JSONObject invocationCompletePayload = new JSONObject();
            invocationCompletePayload.put("instrumentation_type", "invocation_complete");
            invocationCompletePayload.put("generated_id", generatedId);
            invocationCompletePayload.put("execution_index", distributedExecutionIndex.toString());
            invocationCompletePayload.put("vclock", vectorClock.toJSONObject());
            invocationCompletePayload.put("byzantine_fault", byzantineFault);
            invocationCompletePayload.put("module", callsite.getClassOrModuleName());
            invocationCompletePayload.put("method", callsite.getMethodOrFunctionName());

            if (preliminaryDistributedExecutionIndex != null) {
                invocationCompletePayload.put("preliminary_execution_index", preliminaryDistributedExecutionIndex.toString());
            }

            recordInvocationComplete(invocationCompletePayload);
        }
    }


    /**
     * Invoked after a remote call has been completed if the remote call injects a transformer value.
     *
     * @param value       the byzantine value that was injected.
     * @param type        type of the injected byzantine value (e.g., String).
     * @param accumulator containing any additional information that should be communicated to the server and used in
     *                    subsequent byzantine faults (e.g., original value before mutation and idx of mutated char in
     *                    case of a byzantine string transformation).
     */
    public void afterInvocationWithTransformerFault(
            String value,
            String type,
            Accumulator<?, ?> accumulator
    ) {
        if (generatedId > -1 && shouldCommunicateWithServer && counterexampleNotProvided()) {

            JSONObject transformerFault = new JSONObject();
            transformerFault.put("value", value);
            transformerFault.put("accumulator", new Gson().toJson(accumulator));
            transformerFault.put("type", type);

            JSONObject invocationCompletePayload = new JSONObject();
            invocationCompletePayload.put("instrumentation_type", "invocation_complete");
            invocationCompletePayload.put("generated_id", generatedId);
            invocationCompletePayload.put("execution_index", distributedExecutionIndex.toString());
            invocationCompletePayload.put("vclock", vectorClock.toJSONObject());
            invocationCompletePayload.put("transformer_fault", transformerFault);
            invocationCompletePayload.put("module", callsite.getClassOrModuleName());
            invocationCompletePayload.put("method", callsite.getMethodOrFunctionName());

            if (preliminaryDistributedExecutionIndex != null) {
                invocationCompletePayload.put("preliminary_execution_index", preliminaryDistributedExecutionIndex.toString());
            }

            recordInvocationComplete(invocationCompletePayload);
        }
    }


    /**
     * Invoked after a remote call has been completed if the remote call completed successfully.
     *
     * @param className             the fully qualified name of the response class.
     * @param returnValueProperties any properties of the response that make the response unique.
     */
    public void afterInvocationComplete(
            String className,
            HashMap<String, String> returnValueProperties
    ) {
        afterInvocationComplete(className, returnValueProperties, null);
    }


    /**
     * Invoked after a remote call has been completed if the remote call completed successfully.
     *
     * @param className             the fully qualified name of the response class.
     * @param returnValueProperties any properties of the response that make the response unique.
     * @param returnValue           the actual return value after invocation.
     */
    public void afterInvocationComplete(
            String className,
            HashMap<String, String> returnValueProperties,
            @Nullable Object returnValue
    ) {
        // Only if instrumented request, we should communicate, and we aren't inside of Filibuster instrumentation.
        logger.log(Level.INFO, "generatedId: " + generatedId);
        logger.log(Level.INFO, "shouldCommunicateWithServer: " + shouldCommunicateWithServer);

        if (generatedId > -1 && shouldCommunicateWithServer && counterexampleNotProvided()) {
            JSONObject returnValueJO = new JSONObject();

            returnValueJO.put("__class__", className);
            if (returnValue != null && returnValue != JSONObject.NULL && returnValue != "") {  // Only serialise if return value is not null or empty.
                try {
                    returnValueJO.put("value", new Gson().toJson(returnValue));
                } catch (RuntimeException e) {
                    logger.log(Level.WARNING, "Could not serialise return value to JSON: " + e);
                }
            } else {
                returnValueJO.put("value", returnValue);
            }

            for (Map.Entry<String, String> entry : returnValueProperties.entrySet()) {
                // JSONObject does not allow null values.
                // If the value in the HashMap is null, we need to put in JSONObject.NULL instead of null.
                returnValueJO.put(entry.getKey(), entry.getValue() == null ? JSONObject.NULL : entry.getValue());
            }

            JSONObject invocationCompletePayload = new JSONObject();
            invocationCompletePayload.put("instrumentation_type", "invocation_complete");
            invocationCompletePayload.put("generated_id", getGeneratedId());
            invocationCompletePayload.put("execution_index", distributedExecutionIndex.toString());
            invocationCompletePayload.put("vclock", getVectorClock().toJSONObject());
            invocationCompletePayload.put("return_value", returnValueJO);
            invocationCompletePayload.put("module", callsite.getClassOrModuleName());
            invocationCompletePayload.put("method", callsite.getMethodOrFunctionName());

            if (preliminaryDistributedExecutionIndex != null) {
                invocationCompletePayload.put("preliminary_execution_index", preliminaryDistributedExecutionIndex.toString());
            }

            recordInvocationComplete(invocationCompletePayload);
        }
    }


    @SuppressWarnings("VoidMissingNullable")
    private void recordInvocationComplete(JSONObject invocationCompletePayload) {
        logger.log(Level.INFO, "invocationCompletePayload: about to make call.");
        logger.log(Level.INFO, "invocationCompletePayload: " + invocationCompletePayload);

        if (getServerBackendCanInvokeDirectlyProperty()) {
            if (FilibusterCore.hasCurrentInstance()) {
                FilibusterCore.getCurrentInstance().endInvocation(invocationCompletePayload);
            } else {
                throw new FilibusterRuntimeException("No current filibuster core instance, this could indicate a problem.");
            }
        } else {
            CompletableFuture<Void> updateFuture = CompletableFuture.supplyAsync(() -> {
                // Call instrumentation using instrumentation to verify short-circuit.
                WebClient webClient = FilibusterExecutor.getDecoratedWebClient(filibusterBaseUri, filibusterServiceName);

                RequestHeaders postJson = RequestHeaders.of(
                        HttpMethod.POST,
                        "/filibuster/update",
                        HttpHeaderNames.CONTENT_TYPE,
                        "application/json",
                        "X-Filibuster-Instrumentation",
                        "true");
                webClient.execute(postJson, invocationCompletePayload.toString()).aggregate().join();

                return null;
            }, FilibusterExecutor.getExecutorService());

            try {
                updateFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.log(Level.SEVERE, "cannot get information from Filibuster server: " + e);
            }

            logger.log(Level.INFO, "invocationCompletePayload: finished.");
        }
    }
}
