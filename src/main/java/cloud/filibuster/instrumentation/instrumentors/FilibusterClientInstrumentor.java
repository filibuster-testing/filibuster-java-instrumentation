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
import cloud.filibuster.junit.server.core.serializers.GeneratedMessageV3Serializer;
import cloud.filibuster.junit.server.core.serializers.StatusSerializer;
import cloud.filibuster.junit.server.core.transformers.Accumulator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.ResponseHeaders;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.RequestHeaders;
import io.grpc.Status;
import org.json.JSONObject;

import javax.annotation.Generated;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.RpcType.GRPC;
import static cloud.filibuster.instrumentation.helpers.Counterexample.canLoadCounterexample;
import static cloud.filibuster.instrumentation.helpers.Counterexample.loadCounterexampleAsJsonObjectFromEnvironment;
import static cloud.filibuster.instrumentation.helpers.Counterexample.loadTestExecutionFromCounterexample;
import static cloud.filibuster.instrumentation.helpers.Counterexample.shouldFailRequestWithOrDefault;

import static cloud.filibuster.instrumentation.helpers.Property.getClientInstrumentorUseOverrideRequestIdProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getServerBackendCanInvokeDirectlyProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getTestV2Arguments;
import static cloud.filibuster.instrumentation.helpers.Property.getTestV2Exception;
import static cloud.filibuster.instrumentation.helpers.Property.getTestV2ReturnValue;
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
    private static Map<String, Map<String, VectorClock>> vectorClocksByRequest = new HashMap<>();

    private final String outgoingRequestId;

    /**
     * Get the vector clock request mapping.
     *
     * @return vector clock request map.
     */
    public static Map<String, Map<String, VectorClock>> getVectorClocksByRequest() {
        return vectorClocksByRequest;
    }

    /**
     * Mapping between requests and the current execution index for that request.
     */
    private static Map<String, Map<String, DistributedExecutionIndex>> distributedExecutionIndexByRequest = new HashMap<>();

    /**
     * Get the execution index request mapping.
     *
     * @return execution index request map.
     */
    public static Map<String, Map<String, DistributedExecutionIndex>> getDistributedExecutionIndexByRequest() {
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
            Map<String, VectorClock> vectorClockMap = vectorClocksByRequest.get(serviceName);
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

        Map<String, VectorClock> vectorClockMap = vectorClocksByRequest.get(serviceName);
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
            Map<String, DistributedExecutionIndex> distributedExecutionIndexMap = distributedExecutionIndexByRequest.get(serviceName);
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
        Map<String, VectorClock> vectorClocksByRequestId = vectorClocksByRequest.getOrDefault(serviceName, new HashMap<>());
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
        Map<String, DistributedExecutionIndex> distributedExecutionIndexByRequestId = distributedExecutionIndexByRequest.getOrDefault(serviceName, new HashMap<>());
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

    @Nullable
    GeneratedMessageV3 requestMessage;

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
            this.counterexample = loadCounterexampleAsJsonObjectFromEnvironment();
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

                if (forcedExceptionMetadata.has("defer") && forcedExceptionMetadata.getBoolean("defer")) {
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

    public void prepareForInvocation(GeneratedMessageV3 message) {
        this.requestMessage = message;
        prepareForInvocation();
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

        if (getTestV2Arguments() && requestMessage != null) {
            JSONObject serializedRequestArgumentsV2 = GeneratedMessageV3Serializer.toJsonObject(requestMessage);
            invocationPayload.put("args_v2", serializedRequestArgumentsV2);
        }

        invocationPayload.put("kwargs", new JSONObject());
        invocationPayload.put("callsite_file", callsite.getFileName());
        invocationPayload.put("callsite_line", callsite.getLineNumber());
        invocationPayload.put("full_traceback", callsite.getSerializedStackTrace());
        invocationPayload.put("metadata", invocationMetadata);
        invocationPayload.put("vclock", vectorClock.toJsonObject());
        invocationPayload.put("origin_vclock", originVectorClock.toJsonObject());
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
            Map<String, String> additionalMetadata
    ) {
        String exceptionName = throwable.getClass().getName();
        String exceptionCause = throwable.getCause() != null ? throwable.getCause().getClass().getName() : null;
        afterInvocationWithException(exceptionName, exceptionCause, additionalMetadata);
    }

    public void afterInvocationWithException(
            String exceptionName,
            String exceptionCause,
            Map<String, String> additionalMetadata
    ) {
        afterInvocationWithException(exceptionName, exceptionCause, additionalMetadata, null);
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
            Map<String, String> additionalMetadata,
            @Nullable Object exceptionDetails
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
            invocationCompletePayload.put("vclock", vectorClock.toJsonObject());
            invocationCompletePayload.put("exception", exception);

            // In the future, find a way to be a bit smarter about this.
            if(getTestV2Exception() && exceptionDetails != null) {
                if (exceptionDetails instanceof Status) {
                    Status responseStatus = (Status) exceptionDetails;
                    JSONObject serializedExceptionV2 = StatusSerializer.toJsonObject(responseStatus);
                    invocationCompletePayload.put("exception_v2", serializedExceptionV2);
                }
            }

            invocationCompletePayload.put("module", callsite.getClassOrModuleName());
            invocationCompletePayload.put("method", callsite.getMethodOrFunctionName());

            if (preliminaryDistributedExecutionIndex != null) {
                invocationCompletePayload.put("preliminary_execution_index", preliminaryDistributedExecutionIndex.toString());
            }

            recordInvocationComplete(invocationCompletePayload, /* isUpdate= */false);
        }
    }


    /**
     * Invoked after a remote call has been completed if the remote call injects a transformer value.
     *
     * @param value       the transformer value that was injected.
     * @param type        type of the injected transformer value (e.g., String).
     * @param accumulator containing any additional information that should be communicated to the server and used in
     *                    subsequent transformer faults (e.g., original value before mutation and idx of mutated char in
     *                    case of a transformer string transformation).
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
            invocationCompletePayload.put("vclock", vectorClock.toJsonObject());
            invocationCompletePayload.put("transformer_fault", transformerFault);
            invocationCompletePayload.put("module", callsite.getClassOrModuleName());
            invocationCompletePayload.put("method", callsite.getMethodOrFunctionName());

            if (preliminaryDistributedExecutionIndex != null) {
                invocationCompletePayload.put("preliminary_execution_index", preliminaryDistributedExecutionIndex.toString());
            }

            recordInvocationComplete(invocationCompletePayload, /* isUpdate= */false);
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
            Map<String, String> returnValueProperties
    ) {
        afterInvocationComplete(className, returnValueProperties, /* isUpdate= */false, null);
    }


    public void afterInvocationComplete(
            String className,
            Map<String, String> returnValueProperties,
            boolean isUpdate,
            @Nullable Object returnValue
    ) {
        afterInvocationComplete(className, returnValueProperties, isUpdate, returnValue, null);
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
            Map<String, String> returnValueProperties,
            boolean isUpdate,
            @Nullable Object returnValue,
            @Nullable GeneratedMessageV3 responseMessage
    ) {
        // Only if instrumented request, we should communicate, and we aren't inside of Filibuster instrumentation.
        logger.log(Level.INFO, "generatedId: " + generatedId);
        logger.log(Level.INFO, "shouldCommunicateWithServer: " + shouldCommunicateWithServer);

        if (generatedId > -1 && shouldCommunicateWithServer && counterexampleNotProvided()) {
            JSONObject returnValueJsonObject = new JSONObject();

            returnValueJsonObject.put("__class__", className);
            if (returnValue != null && returnValue != JSONObject.NULL && returnValue != "") {  // Only serialise if return value is not null or empty.
                try {
                    String serializedReturnValue;

                    if (rpcType != null && rpcType.equals(GRPC)) {
                        if (returnValue instanceof GeneratedMessageV3) {
                            GeneratedMessageV3 returnValueAsGrpcMessage = (GeneratedMessageV3) returnValue;
                            serializedReturnValue = JsonFormat.printer().preservingProtoFieldNames().includingDefaultValueFields().print(returnValueAsGrpcMessage);
                        } else {
                            serializedReturnValue = new ObjectMapper().writeValueAsString(returnValue);
                        }
                    } else {
                        serializedReturnValue = new Gson().toJson(returnValue);
                    }
                    returnValueJsonObject.put("value", serializedReturnValue);
                } catch (RuntimeException | InvalidProtocolBufferException | JsonProcessingException e) {
                    logger.log(Level.WARNING, "Could not serialise return value to JSON: " + e);
                }
            } else {
                returnValueJsonObject.put("value", returnValue);
            }

            for (Map.Entry<String, String> entry : returnValueProperties.entrySet()) {
                // JSONObject does not allow null values.
                // If the value in the HashMap is null, we need to put in JSONObject.NULL instead of null.
                returnValueJsonObject.put(entry.getKey(), entry.getValue() == null ? JSONObject.NULL : entry.getValue());
            }

            JSONObject invocationCompletePayload = new JSONObject();
            invocationCompletePayload.put("instrumentation_type", "invocation_complete");
            invocationCompletePayload.put("generated_id", getGeneratedId());
            invocationCompletePayload.put("execution_index", distributedExecutionIndex.toString());
            invocationCompletePayload.put("vclock", getVectorClock().toJsonObject());
            invocationCompletePayload.put("return_value", returnValueJsonObject);

            if (getTestV2ReturnValue() && responseMessage != null) {
                JSONObject serializedResponseArgumentsV2 = GeneratedMessageV3Serializer.toJsonObject(responseMessage);
                invocationCompletePayload.put("return_value_v2", serializedResponseArgumentsV2);
            }

            invocationCompletePayload.put("module", callsite.getClassOrModuleName());
            invocationCompletePayload.put("method", callsite.getMethodOrFunctionName());

            if (preliminaryDistributedExecutionIndex != null) {
                invocationCompletePayload.put("preliminary_execution_index", preliminaryDistributedExecutionIndex.toString());
            }

            recordInvocationComplete(invocationCompletePayload, isUpdate);
        }
    }

    @SuppressWarnings("VoidMissingNullable")
    private void recordInvocationComplete(JSONObject invocationCompletePayload, boolean isUpdate) {
        logger.log(Level.INFO, "invocationCompletePayload: about to make call.");
        logger.log(Level.INFO, "invocationCompletePayload: " + invocationCompletePayload);

        if (getServerBackendCanInvokeDirectlyProperty()) {
            if (FilibusterCore.hasCurrentInstance()) {
                FilibusterCore.getCurrentInstance().endInvocation(invocationCompletePayload, isUpdate);
            } else {
                throw new FilibusterRuntimeException("No current filibuster core instance, this could indicate a problem.");
            }
        } else {
            // The only thing that's updated here is the TestExecutionReport that is only supported via the Java server
            // so, don't call again -- this actually won't break anything in the Python server by calling it again
            // but checking that it gets called again isn't really useful.
            //
            // Just don't call it: eventually, we will need to fix this so that in the multi-server configuraiton if
            // some service calls the Java server with the header, it's handled correctly.  Right now, I assume about
            // 1000 things are broken in the multi-server runs because we haven't supported that style of testing
            // for over two years.
            //
            // Long term fix: deprecate the Python client, always call this for the Java server, fix all
            // the integration tests to check for this.  We'll do this when someone actually needs this feature.
            //
            if (!isUpdate) {
                CompletableFuture<Void> updateFuture = CompletableFuture.supplyAsync(() -> {
                    // Call instrumentation using instrumentation to verify short-circuit.
                    WebClient webClient = FilibusterExecutor.getDecoratedWebClient(filibusterBaseUri, filibusterServiceName);

                    RequestHeaders postJson = RequestHeaders.of(
                            HttpMethod.POST,
                            "/filibuster/update",
                            HttpHeaderNames.CONTENT_TYPE,
                            "application/json",
                            "X-Filibuster-Instrumentation",
                            "true",
                            "X-Filibuster-Is-Update",
                            String.valueOf(isUpdate));
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
}
