package cloud.filibuster.instrumentation.libraries.dynamic.proxy;

import cloud.filibuster.exceptions.filibuster.FilibusterFaultInjectionException;
import cloud.filibuster.exceptions.filibuster.FilibusterRuntimeException;
import cloud.filibuster.instrumentation.datatypes.Callsite;
import cloud.filibuster.instrumentation.datatypes.CallsiteArguments;
import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;
import cloud.filibuster.instrumentation.storage.ContextStorage;
import cloud.filibuster.instrumentation.storage.ThreadLocalContextStorage;
import cloud.filibuster.junit.server.core.transformers.Accumulator;
import cloud.filibuster.junit.server.core.transformers.DBException;
import com.datastax.oss.driver.api.core.servererrors.InvalidQueryException;
import com.datastax.oss.driver.api.core.servererrors.OverloadedException;
import com.datastax.oss.driver.api.core.servererrors.ReadFailureException;
import com.datastax.oss.driver.api.core.servererrors.ReadTimeoutException;
import com.datastax.oss.driver.api.core.servererrors.WriteFailureException;
import com.datastax.oss.driver.api.core.servererrors.WriteTimeoutException;
import com.google.gson.Gson;
import io.lettuce.core.RedisBusyException;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandInterruptedException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.cluster.PartitionSelectorException;
import io.lettuce.core.cluster.UnknownPartitionException;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.dynamic.batch.BatchException;
import org.json.JSONObject;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.RequestLimitExceededException;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationEnabledProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationServerCommunicationEnabledProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getRedisTestPortNondeterminismProperty;


public final class DynamicProxyInterceptor<T> implements InvocationHandler {

    private static final Logger logger = Logger.getLogger(DynamicProxyInterceptor.class.getName());
    private final T targetObject;
    public static final Boolean disableInstrumentation = false;
    private final String futureInvokeExceptionOnMethod;
    private final JSONObject futureExceptionMetadata;
    private final ContextStorage contextStorage;
    public static final Boolean disableServerCommunication = false;
    private final String serviceName;
    private final String connectionString;
    private static final String logPrefix = "[FILIBUSTER-PROXY_INTERCEPTOR]: ";
    private FilibusterClientInstrumentor filibusterClientInstrumentor;
    private boolean trackedMethodInvoked;

    private DynamicProxyInterceptor(T targetObject, String connectionString) {
        this(targetObject, connectionString, null, null, /* trackedMethodInvoked= */false);
    }

    private DynamicProxyInterceptor(T targetObject, String connectionString, @Nullable String futureInvokeExceptionOnMethod, @Nullable JSONObject futureExceptionMetadata, boolean trackedMethodInvoked) {
        logger.log(Level.INFO, logPrefix + "Constructor was called");
        this.targetObject = targetObject;
        this.contextStorage = new ThreadLocalContextStorage();
        this.connectionString = connectionString;
        this.serviceName = extractServiceFromConnection(connectionString);
        this.futureInvokeExceptionOnMethod = futureInvokeExceptionOnMethod;
        this.futureExceptionMetadata = futureExceptionMetadata;
        this.trackedMethodInvoked = trackedMethodInvoked;
    }

    private static String extractServiceFromConnection(String connectionString) {
        // If PortNondeterminism is set, extract the host name from the complete connection string. Otherwise, leave
        // the connection string unchanged.
        if (getRedisTestPortNondeterminismProperty()) {
            try {
                URI fullServiceName = new URI(connectionString);
                connectionString = fullServiceName.getHost();
            } catch (Throwable e) {
                throw new FilibusterRuntimeException("DB connection string could not be processed. URI is probably malformed: ", e);
            }
        }
        return connectionString;
    }

    @Override
    @Nullable
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        logger.log(Level.INFO, logPrefix + "invoke() called");
        logger.log(Level.INFO, logPrefix + "shouldInstrument() is " + shouldInstrument());

        // ******************************************************************************************
        // Extract callsite information.
        // ******************************************************************************************

        String classNameOfInvokedMethod = method.getDeclaringClass().getName();
        String simpleMethodName = method.getName();

        // E.g., java.sql.Connection/getSchema or java.sql.Connection/createStatement
        String fullMethodName = String.format("%s/%s", classNameOfInvokedMethod, simpleMethodName);
        logger.log(Level.INFO, logPrefix + "fullMethodName: " + fullMethodName);

        // ******************************************************************************************
        // Construct preliminary call site information.
        // ******************************************************************************************

        // If the invocation has args, pass their class to CallsiteArguments. Otherwise, pass Object[].class.
        Class<?> argsClass = args != null ? args.getClass() : Object[].class;

        // If the invocation has args, pass them as a stringified array to CallsiteArguments. Otherwise, pass "[]".
        String argsString = args != null ? Arrays.toString(args) : "[]";

        CallsiteArguments callsiteArguments = new CallsiteArguments(argsClass, argsString);

        Callsite callsite = new Callsite(serviceName, classNameOfInvokedMethod, fullMethodName, callsiteArguments);

        filibusterClientInstrumentor = new FilibusterClientInstrumentor(serviceName, shouldCommunicateWithServer(), contextStorage, callsite);

        filibusterClientInstrumentor.prepareForInvocation();

        // ******************************************************************************************
        // Record invocation.
        // ******************************************************************************************

        filibusterClientInstrumentor.beforeInvocation();

        // ******************************************************************************************
        // Attach metadata to outgoing request.
        // ******************************************************************************************

        logger.log(Level.INFO, logPrefix + "requestId: " + filibusterClientInstrumentor.getOutgoingRequestId());

        // ******************************************************************************************
        // Get forcedException information.
        // ******************************************************************************************

        JSONObject forcedException = filibusterClientInstrumentor.getForcedException();
        JSONObject failureMetadata = filibusterClientInstrumentor.getFailureMetadata();
        JSONObject transformerFault = filibusterClientInstrumentor.getTransformerFault();

        logger.log(Level.INFO, logPrefix + "forcedException: " + forcedException);
        logger.log(Level.INFO, logPrefix + "failureMetadata: " + failureMetadata);
        logger.log(Level.INFO, logPrefix + "transformerFault: " + transformerFault);

        // ******************************************************************************************
        // If we need to throw, this is where we throw.
        // ******************************************************************************************

        if (failureMetadata != null && filibusterClientInstrumentor.shouldAbort()) {
            generateExceptionFromFailureMetadata();
        }

        String futureInvokeExceptionOnMethod = null;
        JSONObject futureExceptionMetadata = null;

        if (transformerFault != null && filibusterClientInstrumentor.shouldAbort()) {
            Object transformerFaultValue = getTransformerFaultValue(filibusterClientInstrumentor, transformerFault, method.getReturnType());

            if (transformerFaultValue instanceof DBException) {
                forcedException = getJsonExceptionFromTransformerFault((DBException) transformerFaultValue);
            } else {
                return transformerFaultValue;
            }
        }

        if (forcedException != null && filibusterClientInstrumentor.shouldAbort()) {
            boolean shouldInjectExceptionOnCurrentMethod = shouldInjectExceptionOnCurrentMethod(forcedException, fullMethodName);

            if (shouldInjectExceptionOnCurrentMethod) {
                generateAndThrowException(filibusterClientInstrumentor, forcedException);
            } else {
                // If the forced exception should not be injected on the current method, we store the metadata of the exception
                // and the name of the method on which it should be injected. This information will be used later, when the correct
                // method is invoked, to throw the exception.
                futureInvokeExceptionOnMethod = getExceptionMethodName(forcedException, fullMethodName);
                futureExceptionMetadata = forcedException;
            }
        }

        if (this.futureExceptionMetadata != null) {
            currentMethodIsTracked(this.futureExceptionMetadata, fullMethodName);

            if (this.futureInvokeExceptionOnMethod != null && this.futureInvokeExceptionOnMethod.equals(fullMethodName) && filibusterClientInstrumentor.shouldAbort()) {
                trackedMethodInvoked = false;  // Reset trackedMethodInvoked since the futureInvokeExceptionOnMethod  is the current method
                generateAndThrowException(filibusterClientInstrumentor, this.futureExceptionMetadata);
            }
        }

        // ******************************************************************************************
        // Invoke.
        // ******************************************************************************************

        Object invocationResult = invokeOnInterceptedObject(method, args);
        HashMap<String, String> returnValueProperties = new HashMap<>();

        // invocationResult could be null (e.g., when querying a key in that does not exist)
        if (invocationResult != null) {
            String sInvocationResult;

            // Remove memory references from the response
            // They can differ for each test execution and can cause non-termination
            if ((invocationResult.getClass().getName().contains("jdk.proxy") || // Matches Redis
                    invocationResult.getClass().getName().contains("org.postgresql.jdbc")) // Matches Postgres
                    && invocationResult.toString().matches(".*\\..*@.*"))  // Match the pattern of having class names, separated by ".", then "@" and then the memory reference
            {
                // Avoid encoding memory reference in the RPC response. Instead, encode only the class name
                sInvocationResult = invocationResult.toString().split("@")[0];
            } else {
                sInvocationResult = invocationResult.toString();
            }

            returnValueProperties.put("toString", sInvocationResult);
            returnValueProperties.put("trackedMethodInvoked", String.valueOf(trackedMethodInvoked));

            // If "invocationResult" is an interface, return an intercepted proxy
            // (e.g., when calling StatefulRedisConnection.sync() where StatefulRedisConnection is an intercepted proxy,
            // the returned RedisCommands object should also be an intercepted proxy)
            if (method.getReturnType().isInterface() &&
                    method.getReturnType().getClassLoader() != null) {
                invocationResult = DynamicProxyInterceptor.createInterceptor(invocationResult, connectionString, futureInvokeExceptionOnMethod, futureExceptionMetadata, trackedMethodInvoked);
                filibusterClientInstrumentor.afterInvocationComplete(method.getReturnType().getName(), returnValueProperties);
            } else {
                filibusterClientInstrumentor.afterInvocationComplete(invocationResult.getClass().getName(), returnValueProperties, /* isUpdate= */false, invocationResult);
            }
        } else {
            returnValueProperties.put("toString", null);
            filibusterClientInstrumentor.afterInvocationComplete(method.getReturnType().getName(), returnValueProperties);
        }

        return invocationResult;
    }

    private static JSONObject getJsonExceptionFromTransformerFault(DBException transformerFaultValue) {
        JSONObject forcedException = new JSONObject();
        forcedException.put("name", transformerFaultValue.getName());
        forcedException.put("metadata", transformerFaultValue.getMetadata());
        forcedException.put("isTransformerFault", true);
        return forcedException;
    }

    private Object invokeOnInterceptedObject(Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        try {
            return method.invoke(targetObject, args);
        } catch (Throwable t) {
            logger.log(Level.INFO, logPrefix + "An exception was thrown in invokeOnInterceptedObject ", t.getMessage());
            // method.invoke could throw. In that case, catch the thrown exception, communicate it to
            // the filibusterClientInstrumentor, and then throw the exception
            filibusterClientInstrumentor.afterInvocationWithException(t);
            throw t;
        }
    }

    @Nullable
    private static Object getTransformerFaultValue(FilibusterClientInstrumentor filibusterClientInstrumentor, JSONObject transformerFault, Class<?> returnType) {
        try {
            if (transformerFault.has("value") && transformerFault.has("accumulator")) {

                // Extract the transformer fault value from the transformerFault JSONObject and return it.
                Object transformerFaultValue = transformerFault.get("value");
                // Extract the transformer fault value from the transformerFault JSONObject.
                String sTransformerValue = String.valueOf(transformerFaultValue);

                logger.log(Level.INFO, logPrefix + "Injecting the transformed fault value: " + sTransformerValue);

                // Extract the accumulator from the transformerFault JSONObject.
                Accumulator<?, ?> accumulator = new Gson().fromJson(transformerFault.get("accumulator").toString(), Accumulator.class);

                // Notify Filibuster.
                if (transformerFaultValue instanceof DBException) {
                    filibusterClientInstrumentor.afterInvocationWithTransformerFault(sTransformerValue,
                            ((DBException) transformerFaultValue).getName(), accumulator);
                } else {
                    filibusterClientInstrumentor.afterInvocationWithTransformerFault(sTransformerValue,
                            returnType.toString(), accumulator);
                }

                // Return the transformer fault value.
                return transformerFaultValue == JSONObject.NULL ? null : transformerFaultValue;
            } else {
                String missingKey;
                if (transformerFault.has("value")) {
                    missingKey = "accumulator";
                } else {
                    missingKey = "value";
                }
                logger.log(Level.WARNING, logPrefix + "injectTransformerFault: The transformerFault does not have the required key " + missingKey);
                throw new FilibusterFaultInjectionException("injectTransformerFault: The transformerFault does not have the required key " + missingKey);
            }
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, logPrefix + "Could not inject transformer fault. The cast was probably not successful:", e);
            throw new FilibusterFaultInjectionException("Could not inject transformer fault. The cast was probably not successful:", e);
        }
    }

    // Return true if exception should be injected on current method, otherwise false
    private static boolean shouldInjectExceptionOnCurrentMethod(JSONObject forcedException, String currentMethodName) {
        JSONObject forcedExceptionMetadata = forcedException.getJSONObject("metadata");

        if (forcedExceptionMetadata.has("injectOn")) {
            String injectOn = forcedExceptionMetadata.getString("injectOn");

            return injectOn.equals(currentMethodName);
        }
        return true;
    }

    private void currentMethodIsTracked(JSONObject forcedException, String currentMethodName) {
        JSONObject forcedExceptionMetadata = forcedException.getJSONObject("metadata");

        if (forcedExceptionMetadata.has("trackInvocationOf")) {
            String trackInvocationOf = forcedExceptionMetadata.getString("trackInvocationOf");
            trackedMethodInvoked = trackedMethodInvoked || Pattern.compile(trackInvocationOf).matcher(currentMethodName).find();
        }
    }

    private static String getExceptionMethodName(JSONObject forcedException, String currentMethodName) {
        JSONObject forcedExceptionMetadata = forcedException.getJSONObject("metadata");

        if (forcedExceptionMetadata.has("injectOn")) {
            return forcedExceptionMetadata.getString("injectOn");
        }
        return currentMethodName;
    }

    private static void generateAndThrowException(FilibusterClientInstrumentor filibusterClientInstrumentor, JSONObject forcedException) throws Exception {
        String sExceptionName = forcedException.getString("name");
        JSONObject forcedExceptionMetadata = forcedException.getJSONObject("metadata");
        String sCause = forcedExceptionMetadata.getString("cause");
        String sCode = forcedExceptionMetadata.getString("code");
        boolean isTransformerFault = forcedException.has("isTransformerFault") && forcedException.getBoolean("isTransformerFault");

        throwExceptionAndNotifyFilibuster(filibusterClientInstrumentor, sExceptionName, sCause, sCode, isTransformerFault);
    }

    private static void throwExceptionAndNotifyFilibuster(FilibusterClientInstrumentor filibusterClientInstrumentor, String exceptionName, String cause, String code, boolean isTransformerFault) throws Exception {
        Exception exceptionToThrow;
        Random rand = new Random(0);

        switch (exceptionName) {  // TODO: Refactor the switch to an interface
            case "org.postgresql.util.PSQLException":
                exceptionToThrow = new PSQLException(new ServerErrorMessage(cause));
                break;
            case "java.sql.SQLException":
                exceptionToThrow = new SQLException(cause);
                break;
            case "java.sql.SQLTimeoutException":
                exceptionToThrow = new SQLTimeoutException(cause);
                break;
            case "com.datastax.oss.driver.api.core.servererrors.OverloadedException":
                exceptionToThrow = new OverloadedException(null);
                break;
            case "com.datastax.oss.driver.api.core.servererrors.InvalidQueryException":
                exceptionToThrow = new InvalidQueryException(null, cause);
                break;
            case "com.datastax.oss.driver.api.core.servererrors.ReadFailureException":
                exceptionToThrow = new ReadFailureException(null, null, rand.nextInt(), rand.nextInt(), rand.nextInt(), /* dataPresent= */true, null);
                break;
            case "com.datastax.oss.driver.api.core.servererrors.ReadTimeoutException":
                exceptionToThrow = new ReadTimeoutException(null, null, rand.nextInt(), rand.nextInt(), /* dataPresent= */true);
                break;
            case "com.datastax.oss.driver.api.core.servererrors.WriteTimeoutException":
                exceptionToThrow = new WriteTimeoutException(null, null, rand.nextInt(), rand.nextInt(), null);
                break;
            case "com.datastax.oss.driver.api.core.servererrors.WriteFailureException":
                exceptionToThrow = new WriteFailureException(null, null, rand.nextInt(), rand.nextInt(), null, rand.nextInt(), null);
                break;
            case "software.amazon.awssdk.services.dynamodb.model.RequestLimitExceededException":
                exceptionToThrow = RequestLimitExceededException.builder().message(cause).statusCode(Integer.parseInt(code))
                        .requestId(UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT)).build();
                break;
            case "software.amazon.awssdk.services.dynamodb.model.SdkClientException":
                exceptionToThrow = SdkClientException.builder().message(cause).build();
                break;
            case "software.amazon.awssdk.services.dynamodb.model.DynamoDbException":
                exceptionToThrow = DynamoDbException.builder().message(cause).statusCode(Integer.parseInt(code))
                        .requestId(UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT)).build();
                break;
            case "software.amazon.awssdk.services.dynamodb.model.AwsServiceException":
                exceptionToThrow = AwsServiceException.builder().message(cause).statusCode(Integer.parseInt(code))
                        .requestId(UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT)).build();
                break;
            case "io.lettuce.core.RedisCommandTimeoutException":
                exceptionToThrow = new RedisCommandTimeoutException(cause);
                break;
            case "io.lettuce.core.RedisConnectionException":
                exceptionToThrow = new RedisConnectionException(cause);
                break;
            case "io.lettuce.core.RedisBusyException":
                exceptionToThrow = new RedisBusyException(cause);
                break;
            case "io.lettuce.core.RedisCommandExecutionException":
                exceptionToThrow = new RedisCommandExecutionException(cause);
                break;
            case "io.lettuce.core.RedisCommandInterruptedException":
                exceptionToThrow = new RedisCommandInterruptedException(new Throwable(cause));
                break;
            case "io.lettuce.core.cluster.UnknownPartitionException":
                exceptionToThrow = new UnknownPartitionException(cause);
                break;
            case "io.lettuce.core.cluster.PartitionSelectorException":
                exceptionToThrow = new PartitionSelectorException(cause, new Partitions());
                break;
            case "io.lettuce.core.dynamic.batch.BatchException":
                exceptionToThrow = new BatchException(new ArrayList<>());
                break;
            default:
                throw new FilibusterFaultInjectionException("Cannot determine the execution cause to throw: " + cause);
        }

        // Notify Filibuster.
        if (!isTransformerFault) {
            filibusterClientInstrumentor.afterInvocationWithException(exceptionToThrow);
        }

        // Throw callsite exception.
        throw exceptionToThrow;
    }

    private static void generateExceptionFromFailureMetadata() {
        throw new FilibusterFaultInjectionException("Failure metadata not supported for Lettuce.");
    }

    private static boolean shouldInstrument() {
        return getInstrumentationEnabledProperty() && !disableInstrumentation;
    }

    private static boolean shouldCommunicateWithServer() {
        return getInstrumentationServerCommunicationEnabledProperty() && !disableServerCommunication;
    }

    @SuppressWarnings("unchecked")
    private static <T> T createProxy(T target, DynamicProxyInterceptor<?> dynamicProxyInterceptor) {
        logger.log(Level.INFO, logPrefix + "createProxy was called");
        return (T) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                target.getClass().getInterfaces(),
                dynamicProxyInterceptor);
    }

    private static <T> T createInterceptor(T target, String connectionString, String futureInvokeExceptionOnMethod, JSONObject futureExceptionMetadata, boolean trackedMethodInvoked) {
        logger.log(Level.INFO, logPrefix + "createInterceptor was called");
        return createProxy(target, new DynamicProxyInterceptor<>(target, connectionString, futureInvokeExceptionOnMethod, futureExceptionMetadata, trackedMethodInvoked));
    }

    public static <T> T createInterceptor(T target, String connectionString) {
        logger.log(Level.INFO, logPrefix + "createInterceptor was called");
        return createProxy(target, new DynamicProxyInterceptor<>(target, connectionString));
    }
}
