package cloud.filibuster.junit.server.core;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.dei.implementations.DistributedExecutionIndexV1;
import cloud.filibuster.exceptions.filibuster.FilibusterCoreLogicException;
import cloud.filibuster.exceptions.filibuster.FilibusterFaultInjectionException;
import cloud.filibuster.exceptions.filibuster.FilibusterLatencyInjectionException;
import cloud.filibuster.junit.FilibusterSearchStrategy;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration;
import cloud.filibuster.junit.configuration.FilibusterAnalysisConfiguration.MatcherType;
import cloud.filibuster.junit.configuration.FilibusterConfiguration;
import cloud.filibuster.junit.configuration.FilibusterCustomAnalysisConfigurationFile;
import cloud.filibuster.junit.server.core.test_execution_reports.TestExecutionAggregateReport;
import cloud.filibuster.junit.server.core.test_executions.ConcreteTestExecution;
import cloud.filibuster.junit.server.core.test_executions.AbstractTestExecution;
import cloud.filibuster.junit.server.core.test_executions.TestExecution;
import cloud.filibuster.junit.server.latency.FilibusterLatencyProfile;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"Varifier", "Var"})
public class FilibusterCore {
    private static final Logger logger = Logger.getLogger(FilibusterCore.class.getName());

    // The current instance of the FilibusterCore.
    // Required as the instrumentation has no direct way of being instantiated with this object.
    private static FilibusterCore currentInstance;

    // The current instance of the FilibusterCore.
    // Required as the instrumentation has no direct way of being instantiated with this object.
    public static synchronized FilibusterCore getCurrentInstance() {
        if (currentInstance == null) {
            throw new FilibusterCoreLogicException("Current instance is null, this indicates a problem!");
        }

        return currentInstance;
    }

    public static synchronized boolean hasCurrentInstance() {
        return currentInstance != null;
    }

    public FilibusterCore(FilibusterConfiguration filibusterConfiguration) {
        currentInstance = this;

        this.filibusterConfiguration = filibusterConfiguration;
        this.testExecutionAggregateReport = new TestExecutionAggregateReport();
        testExecutionAggregateReport.writeOutPlaceholder();

        if (filibusterConfiguration.getSearchStrategy() == FilibusterSearchStrategy.DFS) {
            this.exploredTestExecutions = new TestExecutionStack<>();
            this.unexploredTestExecutions = new TestExecutionStack<>();
        } else if (filibusterConfiguration.getSearchStrategy() == FilibusterSearchStrategy.BFS) {
            this.exploredTestExecutions = new TestExecutionQueue<>();
            this.unexploredTestExecutions = new TestExecutionQueue<>();
        } else {
            throw new FilibusterCoreLogicException("Unsupported search strategy: " + filibusterConfiguration.getSearchStrategy());
        }
    }

    // Aggregate test execution report.
    private final TestExecutionAggregateReport testExecutionAggregateReport;

    // The current configuration of Filibuster being used.
    private final FilibusterConfiguration filibusterConfiguration;

    // Queue containing the unexplored test executions.
    // These are abstract executions, as they are only prefix executions.
    private final TestExecutionCollection<AbstractTestExecution> unexploredTestExecutions;

    // Queue containing the test executions searched.
    // This includes both abstract executions we attempted to explore and the actual realized concrete executions.
    private final TestExecutionCollection<TestExecution> exploredTestExecutions;

    // The abstract test execution that we are exploring currently.
    @Nullable
    private AbstractTestExecution currentAbstractTestExecution;

    // The concrete test execution that we are exploring currently.
    //
    // Contains:
    // * a prefix execution that matches the abstract test execution.
    // * the same fault profile of the current, concrete test execution.
    @Nullable
    private ConcreteTestExecution currentConcreteTestExecution = new ConcreteTestExecution();

    // Analysis file, populated only once received from the test suite.
    // TODO: Could just bypass this completely because we have the FilibusterConfiguration?
    @Nullable
    private FilibusterCustomAnalysisConfigurationFile filibusterCustomAnalysisConfigurationFile;

    private int numberOfAbstractExecutionsAttempted = 0;

    private int numberOfAbstractExecutionsExecuted = 0;

    private int numberOfConcreteExecutionsExecuted = 0;

    // RPC hooks.

    // Record an outgoing RPC and conditionally inject faults.
    public synchronized JSONObject beginInvocation(JSONObject payload) {
        logger.info("[FILIBUSTER-CORE]: beginInvocation called, payload: " + payload.toString(4));

        if (currentConcreteTestExecution == null) {
            throw new FilibusterCoreLogicException("currentConcreteTestExecution should not be null at this point, something fatal occurred.");
        }

        // Register the RPC using the distributed execution index.
        String distributedExecutionIndexString = payload.getString("execution_index");
        DistributedExecutionIndex distributedExecutionIndex = new DistributedExecutionIndexV1().deserialize(distributedExecutionIndexString);
        logger.info("[FILIBUSTER-CORE]: beginInvocation called, distributedExecutionIndex: " + distributedExecutionIndex);
        currentConcreteTestExecution.addDistributedExecutionIndexWithRequestPayload(distributedExecutionIndex, payload);

        // Get next generated id.
        int generatedId = currentConcreteTestExecution.incrementGeneratedId();

        // Generate new abstract executions to run and queue them into the unexplored list.
        if (filibusterCustomAnalysisConfigurationFile != null) {
            // TODO: Only works for GRPC right now.
            String moduleName = payload.getString("module");
            String methodName = payload.getString("method");

            boolean shouldGenerateNewAbstractExecutions;

            if (currentAbstractTestExecution == null) {
                // Initial execution.
                shouldGenerateNewAbstractExecutions = true;
            } else {
                // ...or, we already scheduled the faults, so don't.
                shouldGenerateNewAbstractExecutions = !currentAbstractTestExecution.sawInConcreteTestExecution(distributedExecutionIndex);
            }

            if (shouldGenerateNewAbstractExecutions) {
                generateFaultsUsingAnalysisConfiguration(filibusterConfiguration, distributedExecutionIndex, moduleName, methodName);
            }
        }

        // Return either success or fault (if, this execution contains a fault to inject.)
        JSONObject response = new JSONObject();

        if (currentAbstractTestExecution != null && currentAbstractTestExecution.shouldFault(distributedExecutionIndex)) {
            JSONObject faultObject = currentAbstractTestExecution.getFault(distributedExecutionIndex);

            // This is a bit redundant, we could just take the fault object and insert it directly into the response
            // if we change the API we call.
            if (faultObject.has("forced_exception")) {
                JSONObject forcedExceptionFaultObject = faultObject.getJSONObject("forced_exception");
                logger.info("[FILIBUSTER-CORE]: beginInvocation, injecting faults using forced_exception: " + forcedExceptionFaultObject.toString(4));
                response.put("forced_exception", forcedExceptionFaultObject);
            } else if (faultObject.has("failure_metadata")) {
                JSONObject failureMetadataFaultObject = faultObject.getJSONObject("failure_metadata");
                logger.info("[FILIBUSTER-CORE]: beginInvocation, injecting faults using failure_metadata: " + failureMetadataFaultObject.toString(4));
                response.put("failure_metadata", failureMetadataFaultObject);
            } else {
                logger.info("[FILIBUSTER-CORE]: beginInvocation, failing to inject unknown fault: " + faultObject.toString(4));
                throw new FilibusterFaultInjectionException("Unknown fault configuration: " + faultObject);
            }
        }

        // Legacy, not used, but helpful in debugging and required by instrumentation libraries.
        response.put("generated_id", generatedId);

        // TODO: This could be returned to the client and the delay done there.
        FilibusterLatencyProfile filibusterLatencyProfile = filibusterConfiguration.getLatencyProfile();

        if (filibusterLatencyProfile != null) {
            int totalSleepMs = 0;

            // Only works for GRPC right now.
            int serviceSleepMs = filibusterLatencyProfile.getMsLatencyForService(payload.getString("module"));
            int methodSleepMs = filibusterLatencyProfile.getMsLatencyForMethod(payload.getString("method"));

            totalSleepMs += serviceSleepMs;
            totalSleepMs += methodSleepMs;

            logger.info("\n" +
                    "[FILIBUSTER-CORE]: sleep based on latency profile: \n" +
                    "serviceSleepMs: " + serviceSleepMs + "\n" +
                    "methodSleepMs: " + methodSleepMs + "\n" +
                    "totalSleepMs: " + totalSleepMs + "\n");

            try {
                Thread.sleep(totalSleepMs);
            } catch (InterruptedException e) {
                throw new FilibusterLatencyInjectionException("Failed to inject latency for call: ", e);
            }
        }

        logger.info("[FILIBUSTER-CORE]: beginInvocation returning, response: " + response.toString(4));

        return response;
    }

    // Record that an RPC completed with a particular value.
    // Only needed for:
    // 1. Dynamic Reduction because we need to keep track of responses.
    // 2. HTTP calls, so we know which service we actually invoked.
    public synchronized JSONObject endInvocation(JSONObject payload) {
        logger.info("[FILIBUSTER-CORE]: endInvocation called");

        String distributedExecutionIndexString = payload.getString("execution_index");
        DistributedExecutionIndex distributedExecutionIndex = new DistributedExecutionIndexV1().deserialize(distributedExecutionIndexString);

        logger.info("[FILIBUSTER-CORE]: endInvocation called, distributedExecutionIndex: " + distributedExecutionIndex);

        if (currentConcreteTestExecution == null) {
            throw new FilibusterCoreLogicException("currentConcreteTestExecution should not be null at this point, something fatal occurred.");
        }

        currentConcreteTestExecution.addDistributedExecutionIndexWithResponsePayload(distributedExecutionIndex, payload);

        JSONObject response = new JSONObject();
        response.put("execution_index", payload.getString("execution_index"));

        logger.info("[FILIBUSTER-CORE]: endInvocation returning: " + response.toString(4));

        return response;
    }

    // Is this the first time that we are seeing an RPC from this service?
    // Used to control when vector clocks, etc. are reset to ensure they are consistent across executions.
    public synchronized boolean isNewTestExecution(String serviceName) {
        logger.info("[FILIBUSTER-CORE]: isNewTestExecution called, serviceName: " + serviceName);

        boolean result = false;

        if (currentConcreteTestExecution == null) {
            // Doesn't really matter, because if this isn't set, no tests will execute.
            result = false;
        } else {
            if (!currentConcreteTestExecution.hasSeenFirstRequestFromService(serviceName)) {
                currentConcreteTestExecution.registerFirstRequestFromService(serviceName);
                result = true;
            } else {
                result = false;
            }
        }

        logger.info("[FILIBUSTER-CORE]: isNewTestExecution returning: " + result);

        return result;
    }

    // JUnit hooks.

    // This is an old callback used to exit the Python server with code = 1 or code = 0 upon failure.
    public synchronized void completeIteration(int currentIteration) {
        logger.info("[FILIBUSTER-CORE]: completeIteration called, currentIteration: " + currentIteration);

        if (currentConcreteTestExecution != null) {
            currentConcreteTestExecution.printRPCs();
            currentConcreteTestExecution.writeTestExecutionReport(currentIteration, /* exceptionOccurred= */ false);
        } else {
            throw new FilibusterCoreLogicException("currentConcreteTestExecution should not be null at this point, something fatal occurred.");
        }

        printSummary();

        logger.info("[FILIBUSTER-CORE]: completeIteration returning");
    }

    // This is an old callback used to exit the Python server with code = 1 or code = 0 upon failure.
    public synchronized void completeIteration(int currentIteration, int exceptionOccurred) {
        logger.info("[FILIBUSTER-CORE]: completeIteration called, currentIteration: " + currentIteration + ", exceptionOccurred: " + exceptionOccurred);

        if (currentConcreteTestExecution != null) {
            currentConcreteTestExecution.printRPCs();
            currentConcreteTestExecution.writeTestExecutionReport(currentIteration, /* exceptionOccurred= */ exceptionOccurred != 0);
        } else {
            throw new FilibusterCoreLogicException("currentConcreteTestExecution should not be null at this point, something fatal occurred.");
        }

        printSummary();

        logger.info("[FILIBUSTER-CORE]: completeIteration returning");
    }

    // Is there a test execution?
    public synchronized boolean hasNextIteration(int currentIteration) {
        logger.info("[FILIBUSTER-CORE]: hasNextIteration called, currentIteration: " + currentIteration);
        boolean result = currentConcreteTestExecution != null;
        logger.info("[FILIBUSTER-CORE]: hasNextIteration returning: " + result);
        return result;
    }

    // Is there a test execution?
    public synchronized boolean hasNextIteration(int currentIteration, String caller) {
        logger.info("[FILIBUSTER-CORE]: hasNextIteration called, currentIteration: " + currentIteration + ", caller: " + caller);
        boolean result = currentConcreteTestExecution != null;
        logger.info("[FILIBUSTER-CORE]: hasNextIteration returning: " + result);
        return result;
    }

    // A test has completed and all callbacks have fired.
    public synchronized void teardownsCompleted(int currentIteration) {
        logger.info("[FILIBUSTER-CORE]: teardownsCompleted called, currentIteration: " + currentIteration);

        if (currentConcreteTestExecution != null) {
            // Add the test report to the aggregate report.
            testExecutionAggregateReport.addTestExecutionReport(currentConcreteTestExecution.getTestExecutionReport());

            // We're executing a test and not just running empty iterations (i.e., JUnit maxIterations > number of actual tests.)

            // Add both the current concrete and abstract execution to the explored list.
            // * currentAbstractTestExecution: the prefix of the concrete execution that was realized by the concrete execution.
            //   this may or may not be set if it's the initial execution.
            // * currentConcreteTestExecution: the actual concrete, realized trace of the test execution.
            if (currentAbstractTestExecution != null) {
                numberOfAbstractExecutionsAttempted++;

                if (!exploredTestExecutions.containsTestExecution(currentAbstractTestExecution)) {
                    // Don't add to explored queue if it's already there.
                    numberOfAbstractExecutionsExecuted++;

                    exploredTestExecutions.addTestExecution(currentAbstractTestExecution);
                } else {
                    logger.severe("[FILIBUSTER-CORE]: teardownsCompleted called, currentAbstractTestExecution already exists in the explored queue, this could indicate a problem in Filibuster.");
                }
            }

            if (!exploredTestExecutions.containsTestExecution(currentConcreteTestExecution)) {
                exploredTestExecutions.addTestExecution(currentConcreteTestExecution);
            }
            numberOfConcreteExecutionsExecuted++;

            // Unset fields.
            currentAbstractTestExecution = null;
            currentConcreteTestExecution = null;

            // If we have another test to run (it will be abstract...)
            if (!unexploredTestExecutions.isEmpty()) {
                logger.info("[FILIBUSTER-CORE]: teardownsCompleted, scheduling next test execution.");

                AbstractTestExecution nextAbstractTestExecution = unexploredTestExecutions.removeAndReturnNextTestExecution();

                // Set the abstract execution, which drives fault injection and copy the faults into the concrete execution for the record.
                currentAbstractTestExecution = nextAbstractTestExecution;
                currentConcreteTestExecution = new ConcreteTestExecution(nextAbstractTestExecution);
            }
        }

        logger.info("[FILIBUSTER-CORE]: teardownsCompleted returning.");
    }

    // Fault injection helpers.

    public synchronized boolean wasFaultInjected() {
        logger.info("[FILIBUSTER-CORE]: wasFaultInjected called");

        if (currentConcreteTestExecution == null) {
            return false;
        }

        boolean result = currentConcreteTestExecution.wasFaultInjected();

        logger.info("[FILIBUSTER-CORE]: wasFaultInjected returning: " + result);

        return result;
    }

    public synchronized boolean wasFaultInjectedOnService(String serviceName) {
        logger.info("[FILIBUSTER-CORE]: wasFaultInjectedOnService called, serviceName: " + serviceName);

        if (currentConcreteTestExecution == null) {
            return false;
        }

        boolean result = currentConcreteTestExecution.wasFaultInjectedOnService(serviceName);

        logger.info("[FILIBUSTER-CORE]: wasFaultInjectedOnService returning: " + result);

        return result;
    }

    public synchronized boolean wasFaultInjectedOnMethod(String serviceName, String methodName) {
        logger.info("[FILIBUSTER-CORE]: wasFaultInjectedOnMethod called, serviceName: " + serviceName + ", methodName: " + methodName);

        if (currentConcreteTestExecution == null) {
            return false;
        }

        boolean result = currentConcreteTestExecution.wasFaultInjectedOnMethod(serviceName, methodName);

        logger.info("[FILIBUSTER-CORE]: wasFaultInjectedOnMethod returning: " + result);

        return result;
    }

    public synchronized boolean wasFaultInjectedOnRequest(String serializedRequest) {
        logger.info("[FILIBUSTER-CORE]: wasFaultInjectedOnRequest called, serializedRequest: " + serializedRequest);

        if (currentConcreteTestExecution == null) {
            return false;
        }

        boolean result = currentConcreteTestExecution.wasFaultInjectedOnRequest(serializedRequest);

        logger.info("[FILIBUSTER-CORE]: wasFaultInjectedOnRequest returning: " + result);

        return result;
    }

    public synchronized boolean wasFaultInjectedOnMethodWhereRequestContains(String serviceName, String methodName, String contains) {
        logger.info("[FILIBUSTER-CORE]: wasFaultInjectedOnMethodWherePayloadContains called, serviceName: " + serviceName + ", methodName: " + methodName + ", contains: " + contains);

        if (currentConcreteTestExecution == null) {
            return false;
        }

        boolean result = currentConcreteTestExecution.wasFaultInjectedOnMethodWhereRequestContains(serviceName, methodName, contains);

        logger.info("[FILIBUSTER-CORE]: wasFaultInjectedOnMethodWherePayloadContains returning: " + result);

        return result;
    }

    // This callback was used to terminate the Filibuster python server -- required if using certain backends for
    // writing counterexample files, etc., but should automatically be handled by the JUnit invocation interceptors now.
    public synchronized void terminateFilibuster() {
        logger.info("[FILIBUSTER-CORE]: terminate called.");

        if (testExecutionAggregateReport != null) {
            testExecutionAggregateReport.writeTestExecutionAggregateReport();
        }

        logger.info("[FILIBUSTER-CORE]: terminate returning.");
    }

    // Configuration.

    public synchronized void analysisFile(JSONObject analysisFile) {
        logger.info("[FILIBUSTER-CORE]: analysisFile called, payload: " + analysisFile.toString(4));

        FilibusterCustomAnalysisConfigurationFile.Builder filibusterCustomAnalysisConfigurationFileBuilder = new FilibusterCustomAnalysisConfigurationFile.Builder();

        for (String name : analysisFile.keySet()) {
            FilibusterAnalysisConfiguration.Builder filibusterAnalysisConfigurationBuilder = new FilibusterAnalysisConfiguration.Builder();
            filibusterAnalysisConfigurationBuilder.name(name);

            JSONObject nameObject = analysisFile.getJSONObject(name);

            if (nameObject.has("pattern")) {
                filibusterAnalysisConfigurationBuilder.pattern(nameObject.getString("pattern"));
            }

            if (nameObject.has("latencies")) {
                JSONArray jsonArray = nameObject.getJSONArray("latencies");

                for (Object obj : jsonArray) {
                    JSONObject latencyObject = (JSONObject) obj;

                    MatcherType matcherType = MatcherType.valueOf(latencyObject.getString("type"));
                    String matcher = latencyObject.getString("matcher");
                    int milliseconds = latencyObject.getInt("milliseconds");

                    filibusterAnalysisConfigurationBuilder.latency(matcherType, matcher, milliseconds);
                    logger.info("[FILIBUSTER-CORE]: analysisFile, found new configuration, matcherType: " + matcherType + ", matcher: " + matcher + ", milliseconds: " + milliseconds);
                }
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
                    logger.info("[FILIBUSTER-CORE]: analysisFile, found new configuration, exceptionName: " + exceptionName + ", exceptionMetadataMap: " + exceptionMetadataMap);
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
                    logger.info("[FILIBUSTER-CORE]: analysisFile, found new configuration, errorServiceName: " + errorServiceName + ", errorTypesList: " + errorTypesList);
                }
            }

            FilibusterAnalysisConfiguration filibusterAnalysisConfiguration = filibusterAnalysisConfigurationBuilder.build();
            filibusterCustomAnalysisConfigurationFileBuilder.analysisConfiguration(filibusterAnalysisConfiguration);
        }

        filibusterCustomAnalysisConfigurationFile = filibusterCustomAnalysisConfigurationFileBuilder.build();

        logger.info("[FILIBUSTER-CORE]: analysisFile, set instance variable, returning.");
    }

    // Private functions.

    private void generateFaultsUsingAnalysisConfiguration(
            FilibusterConfiguration filibusterConfiguration,
            DistributedExecutionIndex distributedExecutionIndex,
            String moduleName,
            String methodName
    ) {
        logger.info("[FILIBUSTER-CORE]: generateFaultsUsingAnalysisConfiguration called.");

        if (filibusterCustomAnalysisConfigurationFile != null) {
            for (FilibusterAnalysisConfiguration filibusterAnalysisConfiguration : filibusterCustomAnalysisConfigurationFile.getFilibusterAnalysisConfigurations()) {
                // Second check here (concat) is a legacy check for the old Python server compatibility.
                if (filibusterAnalysisConfiguration.isPatternMatch(methodName) || filibusterAnalysisConfiguration.isPatternMatch(moduleName + "." + methodName)) {
                    // Latency.
                    List<JSONObject> latencyFaultObjects = filibusterAnalysisConfiguration.getLatencyFaultObjects();

                    for(JSONObject faultObject : latencyFaultObjects) {
                        JSONObject latencyObject = faultObject.getJSONObject("latency");
                        String latencyObjectMatcherType = latencyObject.getString("type");
                        MatcherType matcherType = MatcherType.valueOf(latencyObjectMatcherType);
                        String latencyObjectMatcher = latencyObject.getString("matcher");
                        Pattern faultServiceNamePattern = Pattern.compile(latencyObjectMatcher, Pattern.CASE_INSENSITIVE);
                        Matcher matcher;

                        switch (matcherType) {
                            case SERVICE:
                                matcher = faultServiceNamePattern.matcher(moduleName);
                                break;
                            case METHOD:
                                matcher = faultServiceNamePattern.matcher(methodName);
                                break;
                            default:
                                throw new FilibusterFaultInjectionException("Unknown latency injection type: " + matcherType);
                        }

                        if (matcher.find()) {
                            createAndScheduleAbstractTestExecution(filibusterConfiguration, distributedExecutionIndex, faultObject);
                        }
                    }

                    // Exceptions.
                    List<JSONObject> exceptionFaultObjects = filibusterAnalysisConfiguration.getExceptionFaultObjects();

                    for(JSONObject faultObject : exceptionFaultObjects) {
                        createAndScheduleAbstractTestExecution(filibusterConfiguration, distributedExecutionIndex, faultObject);
                    }

                    // Errors.
                    List<JSONObject> errorFaultObjects = filibusterAnalysisConfiguration.getErrorFaultObjects();

                    for(JSONObject faultObject : errorFaultObjects) {
                        JSONObject failureMetadataObject = faultObject.getJSONObject("failure_metadata");

                        String faultServiceName = failureMetadataObject.getString("service_name");
                        Pattern faultServiceNamePattern = Pattern.compile(faultServiceName, Pattern.CASE_INSENSITIVE);
                        Matcher matcher = faultServiceNamePattern.matcher(moduleName);

                        List<Object> faultTypesArray = failureMetadataObject.getJSONArray("types").toList();

                        if (matcher.find()) {
                            for (Object obj : faultTypesArray) {
                                HashMap faultTypeMap = (HashMap) obj;
                                JSONObject faultTypeObject = new JSONObject();
                                faultTypeObject.put("failure_metadata", faultTypeMap);
                                createAndScheduleAbstractTestExecution(filibusterConfiguration, distributedExecutionIndex, faultTypeObject);
                            }
                        }
                    }
                }
            }
        }

        logger.info("[FILIBUSTER-CORE]: generateFaultsUsingAnalysisConfiguration returning.");
    }

    private void createAndScheduleAbstractTestExecution(
            FilibusterConfiguration filibusterConfiguration,
            DistributedExecutionIndex distributedExecutionIndex,
            JSONObject faultObject) {
        logger.info("[FILIBUSTER-CORE]: createAndScheduleAbstractTestExecution called.");

        if (currentConcreteTestExecution != null) {
            AbstractTestExecution abstractTestExecution = currentConcreteTestExecution.toAbstractTestExecution();
            abstractTestExecution.addFaultToInject(distributedExecutionIndex, faultObject);

            boolean abstractIsExploredExecution = exploredTestExecutions.containsAbstractTestExecution(abstractTestExecution);
            boolean abstractIsScheduledExecution = unexploredTestExecutions.containsAbstractTestExecution(abstractTestExecution);
            boolean abstractIsCurrentExecution = currentAbstractTestExecution != null && currentAbstractTestExecution.matchesAbstractTestExecution(abstractTestExecution);

            if (!abstractIsExploredExecution && !abstractIsScheduledExecution && !abstractIsCurrentExecution) {
                if (filibusterConfiguration.getSuppressCombinations()) {
                    if (!(abstractTestExecution.getFaultsToInjectSize() > 1)) {
                        unexploredTestExecutions.addTestExecution(abstractTestExecution);
                        logger.info("[FILIBUSTER-CORE]: createAndScheduleAbstractTestExecution, adding new execution to the queue.");
                    } else {
                        logger.info("[FILIBUSTER-CORE]: createAndScheduleAbstractTestExecution, not scheduling test execution because it contains > 1 fault.");
                    }
                } else {
                    logger.info("[FILIBUSTER-CORE]: createAndScheduleAbstractTestExecution, adding new execution to the queue.");
                    unexploredTestExecutions.addTestExecution(abstractTestExecution);
                }
            }
        }

        logger.info("[FILIBUSTER-CORE]: createAndScheduleAbstractTestExecution returning.");
    }

    private void printSummary() {
        logger.info(
                "" + "\n" +
                        "[FILIBUSTER-CORE]: Filibuster In-Progress Summary" + "\n" +
                        "" + "\n" +
                        "[FILIBUSTER-CORE]: Queue Statistics: " + "\n" +
                        "[FILIBUSTER-CORE]: * unexploredTestExecutions.size():           " + unexploredTestExecutions.size() + "\n" +
                        "[FILIBUSTER-CORE]: * exploredTestExecutions.size():             " + exploredTestExecutions.size() + " (+1, =" + (exploredTestExecutions.size() + 1) + ")" + "\n" +
                        "" + "\n" +
                        "[FILIBUSTER-CORE]: Test Summary: " + "\n" +
                        "[FILIBUSTER-CORE]: * numberOfAbstractExecutionsAttempted:       " + numberOfAbstractExecutionsAttempted + (currentAbstractTestExecution == null ? "" : " (+1, =" + (numberOfAbstractExecutionsAttempted + 1) + ")") + "\n" +
                        "[FILIBUSTER-CORE]: * numberOfAbstractExecutionsExecuted:        " + numberOfAbstractExecutionsExecuted + (currentAbstractTestExecution == null ? "" : " (+1, =" + (numberOfAbstractExecutionsExecuted + 1) + ")") + "\n" +
                        "[FILIBUSTER-CORE]: * numberOfConcreteExecutionsExecuted:        " + numberOfConcreteExecutionsExecuted + " (+1, =" + (numberOfConcreteExecutionsExecuted + 1) + ")" + "\n"
        );
    }
}
