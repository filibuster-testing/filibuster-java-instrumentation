package cloud.filibuster.junit.server.core;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.dei.implementations.DistributedExecutionIndexV1;
import cloud.filibuster.exceptions.FilibusterFaultInjectionException;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;
import cloud.filibuster.junit.server.core.test_executions.ConcreteTestExecution;
import cloud.filibuster.junit.server.core.test_executions.PartialTestExecution;
import cloud.filibuster.junit.server.core.test_executions.TestExecution;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("Varifier")
public class FilibusterCore {
    private static final Logger logger = Logger.getLogger(FilibusterCore.class.getName());

    // Queue containing the unexplored test executions.
    // These are partial executions, as they are only prefix executions.
    Queue<PartialTestExecution> unexploredTestExecutions = new LinkedBlockingDeque<>();

    // Queue containing the test executions searched.
    // This includes both partial executions we attempted to explore and the actual realized concrete executions.
    Queue<TestExecution> exploredTestExecutions = new LinkedBlockingDeque<>();

    // The partial test execution that we are exploring currently.
    @Nullable
    PartialTestExecution currentPartialTestExecution;

    // The concrete test execution that we are exploring currently.
    // Contains:
    // * a prefix execution that matches the partial test execution.
    // * the same fault profile of the current, concrete test execution.
    @Nullable
    ConcreteTestExecution currentConcreteTestExecution = new ConcreteTestExecution();

    @Nullable
    private FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    // RPC hooks.

    // Record an outgoing RPC and conditionally inject faults.
    public JSONObject beginInvocation(JSONObject payload) {
        // Register the RPC using the distributed execution index.
        String distributedExecutionIndexString = payload.getString("execution_index");
        DistributedExecutionIndex distributedExecutionIndex = new DistributedExecutionIndexV1().deserialize(distributedExecutionIndexString);
        int generatedId = currentConcreteTestExecution.addDistributedExecutionIndexWithPayload(distributedExecutionIndex, payload);

        // Generate new partial executions to run and queue them into the unexplored list.
        if (filibusterCustomAnalysisConfigurationFile != null) {
            String serviceName = payload.getString("module");
            String methodName = payload.getString("method");
            generateFaultsUsingAnalysisConfiguration(distributedExecutionIndex, serviceName, methodName);
        }

        // Return either success or fault (if, this execution contains a fault to inject.)
        JSONObject response = new JSONObject();

        if (currentPartialTestExecution.shouldFault(distributedExecutionIndex)) {
            JSONObject faultObject = currentPartialTestExecution.getFault(distributedExecutionIndex);

            // This is a bit redundant, we could just take the fault object and insert it directly into the response
            // if we change the API we call.
            if (faultObject.has("forced_exception")) {
                response.put("forced_exception", faultObject.getJSONObject("forced_exception"));
            } else if (faultObject.has("failure_metadata")) {
                response.put("failure_metadata", faultObject.getJSONObject("failure_metadata"));
            } else {
                throw new FilibusterFaultInjectionException("Unknown fault configuration: " + faultObject);
            }
        }

        // Legacy, not used, but helpful in debugging and required by instrumentation libraries.
        response.put("generated_id", generatedId);

        return response;
    }

    // JUnit hooks.

    // This is an old callback used to exit the Python server with code = 1 or code = 0 upon failure.
    public void completeIteration(int currentIteration) {
        logger.info("[FILIBUSTER-CORE]: completeIteration called, currentIteration: " + currentIteration);
        if (currentConcreteTestExecution != null) {
            currentConcreteTestExecution.printRPCs();
        } else {
            throw new FilibusterCoreLogicException("currentConcreteTestExecution should not be null at this point, something fatal occurred.");
        }
    }

    // This is an old callback used to exit the Python server with code = 1 or code = 0 upon failure.
    public void completeIteration(int currentIteration, int exceptionOccurred) {
        logger.info("[FILIBUSTER-CORE]: completeIteration called, currentIteration: " + currentIteration + ", exceptionOccurred: " + exceptionOccurred);
        if (currentConcreteTestExecution != null) {
            currentConcreteTestExecution.printRPCs();
        } else {
            throw new FilibusterCoreLogicException("currentConcreteTestExecution should not be null at this point, something fatal occurred.");
        }
    }

    // Is there a test execution?
    public boolean hasNextIteration(int currentIteration) {
        logger.info("[FILIBUSTER-CORE]: hasNextiteration called, currentIteration: " + currentIteration);
        return currentConcreteTestExecution != null;
    }

    // Is there a test execution?
    public boolean hasNextIteration(int currentIteration, String caller) {
        logger.info("[FILIBUSTER-CORE]: hasNextiteration called, currentIteration: " + currentIteration + ", caller: " + caller);
        return currentConcreteTestExecution != null;
    }

    // A test has completed and all callbacks have fired.
    public void teardownsCompleted(int currentIteration) {
        logger.info("[FILIBUSTER-CORE]: teardownsCompleted called, currentIteration: " + currentIteration);

        if (currentConcreteTestExecution != null) {
            // We're executing a test and not just running empty iterations (i.e., JUnit maxIterations > number of actual tests.)

            // Add both the current concrete and partial execution to the explored list.
            // * currentPartialTestExecution: the prefix of the concrete execution that was realized by the concrete execution.
            //   this may or may not be set if it's the initial execution.
            // * currentConcreteTestExecution: the actual concrete, realized trace of the test execution.
            if (currentPartialTestExecution != null) {
                exploredTestExecutions.add(currentPartialTestExecution);
            }
            exploredTestExecutions.add(currentConcreteTestExecution);

            // Unset fields.
            currentPartialTestExecution = null;
            currentConcreteTestExecution = null;

            // If we have another test to run (it will be partial...)
            if (!unexploredTestExecutions.isEmpty()) {
                PartialTestExecution nextPartialTestExecution = unexploredTestExecutions.remove();

                // Set the partial execution, which drives fault injection and copy the faults into the concrete execution for the record.
                currentPartialTestExecution = nextPartialTestExecution;
                currentConcreteTestExecution = new ConcreteTestExecution(nextPartialTestExecution);
            }
        }
    }

    // Fault injection helpers.

    // Was any fault injected?
    public boolean wasFaultInjected() {
        logger.info("[FILIBUSTER-CORE]: wasFaultInjected called");
        return currentPartialTestExecution.wasFaultInjected();
    }

    // Was a fault injected on a particular service?
    public boolean wasFaultInjectedOnService(String serviceName) {
        logger.info("[FILIBUSTER-CORE]: wasFaultInjected called, serviceName: " + serviceName);
        return currentPartialTestExecution.wasFaultInjectedOnService(serviceName);
    }

    // Was a fault injected on a particular GRPC call?
    public boolean wasFaultInjectedOnMethod(String serviceName, String methodName) {
        logger.info("[FILIBUSTER-CORE]: wasFaultInjected called, serviceName: " + serviceName + ", methodName:" + methodName);
        return currentPartialTestExecution.wasFaultInjectedOnMethod(serviceName, methodName);
    }

    // Record that an RPC completed with a particular value.
    // Only needed for:
    // 1. Dynamic Reduction because we need to keep track of responses.
    // 2. HTTP calls, so we know which service we actually invoked.
    public JSONObject endInvocation(JSONObject payload) {
        String distributedExecutionIndexString = payload.getString("execution_index");
        DistributedExecutionIndex distributedExecutionIndex = new DistributedExecutionIndexV1().deserialize(distributedExecutionIndexString);

        logger.info("[FILIBUSTER-CORE]: endInvocation called, distributedExecutionIndex: " + distributedExecutionIndex);

        JSONObject response = new JSONObject();
        response.put("execution_index", payload.getString("execution_index"));
        return response;
    }

    // Is this the first time that we are seeing an RPC from this service?
    // Used to control when vclocks, etc. are reset to ensure they are consistent across executions.
    public boolean isNewTestExecution(String serviceName) {
        logger.info("[FILIBUSTER-CORE]: isNewTestExecution called, serviceName: " + serviceName);

        if (!currentConcreteTestExecution.hasSeenFirstRequestromService(serviceName)) {
            currentConcreteTestExecution.registerFirstRequestFromService(serviceName);
            return true;
        } else {
            return false;
        }
    }

    // This callback was used to terminate the Filibuster python server -- required if using certain backends for
    // writing counterexample files, etc., but should automatically be handled by the JUnit invocation interceptors now.
    public void terminateFilibuster() {
        logger.info("[FILIBUSTER-CORE]: terminate called.");
        // Nothing.
    }

    // Configuration.

    public void analysisFile(JSONObject analysisFile) {
        logger.info("[FILIBUSTER-CORE]: analysisFile called.");

        FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder = new FilibusterCustomAnalysisConfigurationFile.Builder();

        for (String name : analysisFile.keySet()) {
            FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilder = new FilibusterAnalysisConfiguration.Builder();
            filibusterAnalysisConfigurationBuilder.name(name);

            JSONObject nameObject = analysisFile.getJSONObject(name);

            if (nameObject.has("pattern")) {
                filibusterAnalysisConfigurationBuilder.pattern(nameObject.getString("pattern"));
            }

            if (nameObject.has("exceptions")) {
                JSONArray jsonArray = nameObject.getJSONArray("exceptions");

                for (Object obj : jsonArray) {
                    JSONObject exceptionObject = (JSONObject) obj;

                    String exceptionName = exceptionObject.getString("name");
                    JSONObject exceptionMetadata = exceptionObject.getJSONObject("metadata");

                    HashMap<String, String> exceptionMetadataMap = new HashMap<>();
                    for (String metadataObjectKey : exceptionMetadata.keySet()) {
                        exceptionMetadataMap.put(metadataObjectKey, exceptionMetadata.getString(metadataObjectKey));
                    }

                    filibusterAnalysisConfigurationBuilder.exception(exceptionName, exceptionMetadataMap);
                }
            }

            if (nameObject.has("errors")) {
                JSONArray jsonArray = nameObject.getJSONArray("errors");

                for (Object obj : jsonArray) {
                    JSONObject errorObject = (JSONObject) obj;

                    String errorServiceName = errorObject.getString("service_name");
                    JSONArray errorTypes = errorObject.getJSONArray("types");

                    List<JSONObject> errorTypesList = new ArrayList<>();
                    for (Object errorType : errorTypes) {
                        errorTypesList.add((JSONObject) errorType);
                    }

                    filibusterAnalysisConfigurationBuilder.error(errorServiceName, errorTypesList);
                }
            }

            FilibusterAnalysisConfiguration filibusterAnalysisConfiguration = filibusterAnalysisConfigurationBuilder.build();
            filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfiguration);
        }

        filibusterCustomAnalysisConfigurationFile = filibusterCustomAnalysisConfigurationFileBuilder.build();
    }

    private void generateFaultsUsingAnalysisConfiguration(
            DistributedExecutionIndex distributedExecutionIndex,
            String serviceName,
            String methodName
    ) {
        for (FilibusterAnalysisConfiguration filibusterAnalysisConfiguration : filibusterCustomAnalysisConfigurationFile.getFilibusterAnalysisConfigurations()) {
            if (filibusterAnalysisConfiguration.isPatternMatch(methodName)) {
                // Exceptions.
                List<JSONObject> exceptionFaultObjects = filibusterAnalysisConfiguration.getExceptionFaultObjects();

                for(JSONObject faultObject : exceptionFaultObjects) {
                    createAndSchedulePartialTestExecution(distributedExecutionIndex, faultObject);
                }

                // Errors.
                List<JSONObject> errorFaultObjects = filibusterAnalysisConfiguration.getErrorFaultObjects();

                for(JSONObject faultObject : errorFaultObjects) {
                    JSONObject failureMetadataObject = faultObject.getJSONObject("failure_metadata");

                    String faultServiceName = failureMetadataObject.getString("service_name");
                    Pattern faultServiceNamePattern = Pattern.compile(faultServiceName, Pattern.CASE_INSENSITIVE);
                    Matcher matcher = faultServiceNamePattern.matcher(serviceName);

                    List<Object> faultTypesArray = failureMetadataObject.getJSONArray("types").toList();

                    if (matcher.find()) {
                        for (Object obj : faultTypesArray) {
                            JSONObject faultTypeObject = (JSONObject) obj;
                            createAndSchedulePartialTestExecution(distributedExecutionIndex, faultTypeObject);
                        }
                    }
                }
            }
        }
    }

    private void createAndSchedulePartialTestExecution(DistributedExecutionIndex distributedExecutionIndex, JSONObject faultObject) {
        if (currentConcreteTestExecution != null) {
            PartialTestExecution partialTestExecution = currentConcreteTestExecution.cloneToPartialTestExecution();
            partialTestExecution.addFaultToInject(distributedExecutionIndex, faultObject);

            boolean partialIsExploredExecution = exploredTestExecutions.contains(partialTestExecution);
            boolean partialIsScheduledExecution = unexploredTestExecutions.contains(partialTestExecution);
            boolean partialIsCurrentExecution = currentPartialTestExecution == null ? false : currentPartialTestExecution.equals(partialTestExecution);

            if (!partialIsExploredExecution && !partialIsScheduledExecution && !partialIsCurrentExecution) {
                unexploredTestExecutions.add(partialTestExecution);
            }
        }
    }
}
