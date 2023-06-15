package cloud.filibuster.instrumentation.libraries.lettuce;

import cloud.filibuster.exceptions.filibuster.FilibusterFaultInjectionException;
import cloud.filibuster.exceptions.filibuster.FilibusterRuntimeException;
import cloud.filibuster.instrumentation.datatypes.Callsite;
import cloud.filibuster.instrumentation.datatypes.CallsiteArguments;
import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;
import cloud.filibuster.instrumentation.storage.ContextStorage;
import cloud.filibuster.instrumentation.storage.ThreadLocalContextStorage;
import cloud.filibuster.junit.configuration.examples.db.byzantine.types.ByzantineFaultType;
import cloud.filibuster.junit.server.core.transformers.Accumulator;
import com.google.gson.Gson;
import io.lettuce.core.RedisBusyException;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandInterruptedException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.cluster.PartitionSelectorException;
import io.lettuce.core.cluster.UnknownPartitionException;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.dynamic.batch.BatchException;
import io.lettuce.core.dynamic.intercept.MethodInterceptor;
import io.lettuce.core.dynamic.intercept.MethodInvocation;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URI;

import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationEnabledProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationServerCommunicationEnabledProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getRedisTestPortNondeterminismProperty;

public class RedisInterceptor<T> implements MethodInterceptor {
    public static final Boolean disableInstrumentation = false;
    private static final Logger logger = Logger.getLogger(RedisInterceptor.class.getName());
    protected final ContextStorage contextStorage;
    public static final Boolean disableServerCommunication = false;
    private final String redisServiceName;
    private final String redisConnectionString;
    private final T interceptedObject;
    private static final String logPrefix = "[FILIBUSTER-REDIS_INTERCEPTOR]: ";

    private FilibusterClientInstrumentor filibusterClientInstrumentor;

    public RedisInterceptor(T interceptedObject, String redisConnectionString) {
        this.contextStorage = new ThreadLocalContextStorage();
        this.redisConnectionString = redisConnectionString;
        this.redisServiceName = getRedisServiceName(redisConnectionString);
        this.interceptedObject = interceptedObject;
    }

    public String getRedisServiceName() {
        return this.redisServiceName;
    }

    private static String getRedisServiceName(String redisConnectionString) {
        // If redisPortNondeterminism is set, extract the redis host name from the complete connection string. Otherwise, leave
        // the redis connection string unchanged.
        if (getRedisTestPortNondeterminismProperty()) {
            try {
                URI fullRedisServiceName = new URI(redisConnectionString);
                redisConnectionString = fullRedisServiceName.getHost();
            } catch (Throwable e) {
                throw new FilibusterRuntimeException("Redis connection string could not be processed. URI is probably malformed: ", e);
            }
        }
        return redisConnectionString;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        logger.log(Level.INFO, logPrefix + "RedisInterceptor: invoke() called");
        logger.log(Level.INFO, logPrefix + "shouldInstrument() is " + shouldInstrument());

        // ******************************************************************************************
        // Extract callsite information.
        // ******************************************************************************************


        String classNameOfInvokedMethod = invocation.getMethod().getDeclaringClass().getName();  // e.g., io.lettuce.core.api.sync.RedisStringCommands
        String simpleMethodName = invocation.getMethod().getName();  // e.g., get

        // Possible values of redisFullMethodName are
        //  io.lettuce.core.api.sync.RedisStringCommands/get,
        //  io.lettuce.core.api.async.RedisStringAsyncCommands/get,
        //  io.lettuce.core.api.sync.RedisStringCommands/set
        String redisFullMethodName = String.format("%s/%s", classNameOfInvokedMethod, simpleMethodName);
        logger.log(Level.INFO, logPrefix + "redisFullMethodName: " + redisFullMethodName);

        // ******************************************************************************************
        // Construct preliminary call site information.
        // ******************************************************************************************

        CallsiteArguments callsiteArguments = new CallsiteArguments(invocation.getArguments().getClass(), Arrays.toString(invocation.getArguments()));

        Callsite callsite = new Callsite(redisServiceName, classNameOfInvokedMethod, redisFullMethodName, callsiteArguments);

        filibusterClientInstrumentor = new FilibusterClientInstrumentor(redisServiceName, shouldCommunicateWithServer(), contextStorage, callsite);

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
        JSONObject byzantineFault = filibusterClientInstrumentor.getByzantineFault();
        JSONObject transformerFault = filibusterClientInstrumentor.getTransformerFault();

        logger.log(Level.INFO, logPrefix + "forcedException: " + forcedException);
        logger.log(Level.INFO, logPrefix + "failureMetadata: " + failureMetadata);
        logger.log(Level.INFO, logPrefix + "byzantineFault: " + byzantineFault);
        logger.log(Level.INFO, logPrefix + "transformerFault: " + transformerFault);

        // ******************************************************************************************
        // If we need to throw, this is where we throw.
        // ******************************************************************************************

        if (failureMetadata != null && filibusterClientInstrumentor.shouldAbort()) {
            generateExceptionFromFailureMetadata();
        }

        if (forcedException != null && filibusterClientInstrumentor.shouldAbort()) {
            generateAndThrowException(filibusterClientInstrumentor, forcedException);
        }

        if (byzantineFault != null && filibusterClientInstrumentor.shouldAbort()) {
            return injectByzantineFault(filibusterClientInstrumentor, byzantineFault, invocation.getMethod().getReturnType());
        }

        if (transformerFault != null && filibusterClientInstrumentor.shouldAbort()) {
            return injectTransformerFault(filibusterClientInstrumentor, transformerFault, invocation.getMethod().getReturnType());
        }

        // ******************************************************************************************
        // Invoke.
        // ******************************************************************************************

        Object invocationResult = invokeOnInterceptedObject(invocation);
        HashMap<String, String> returnValueProperties = new HashMap<>();

        // invocationResult could be null (e.g., when querying a key in Redis that does not exist). If it is null, skip
        // execute the following block
        if (invocationResult != null) {
            returnValueProperties.put("toString", invocationResult.toString());
            // If "invocationResult" is an interface, return an intercepted proxy
            // (e.g., when calling StatefulRedisConnection.sync() where StatefulRedisConnection is an intercepted proxy,
            // the returned RedisCommands object should also be an intercepted proxy)
            if (invocation.getMethod().getReturnType().isInterface() &&
                    invocation.getMethod().getReturnType().getClassLoader() != null) {
                invocationResult = new RedisInterceptorFactory<>(invocationResult, redisConnectionString)
                        .getProxy(invocation.getMethod().getReturnType());
            }
        } else {
            returnValueProperties.put("toString", null);
        }

        filibusterClientInstrumentor.afterInvocationComplete(invocation.getMethod().getReturnType().getName(), returnValueProperties);

        return invocationResult;
    }

    private Object invokeOnInterceptedObject(MethodInvocation invocation) throws InvocationTargetException, IllegalAccessException {
        try {
            Method method = invocation.getMethod();
            Object[] args = invocation.getArguments();
            return method.invoke(interceptedObject, args);
        } catch (Throwable t) {
            logger.log(Level.INFO, logPrefix + "An exception was thrown in invokeOnInterceptedObject ", t.getMessage());
            // method.invoke could throw. In that case, catch the thrown exception, communicate it to
            // the filibusterClientInstrumentor, and then throw the exception
            filibusterClientInstrumentor.afterInvocationWithException(t);
            throw t;
        }
    }

    private static Object injectTransformerFault(FilibusterClientInstrumentor filibusterClientInstrumentor, JSONObject transformerFault, Class<?> returnType) {
        try {
            if (transformerFault.has("value") && transformerFault.has("accumulator")) {

                // Extract the transformer fault value from the transformerFault JSONObject.
                Object transformerFaultValue = transformerFault.get("value");
                String sTransformerValue = transformerFaultValue.toString();
                logger.log(Level.INFO, logPrefix + "Injecting the transformed fault value: " + sTransformerValue);

                // Extract the accumulator from the transformerFault JSONObject.
                Accumulator<?, ?> accumulator = new Gson().fromJson(transformerFault.get("accumulator").toString(), Accumulator.class);

                // Notify Filibuster.
                filibusterClientInstrumentor.afterInvocationWithTransformerFault(sTransformerValue,
                        returnType.toString(), accumulator);

                // Return the byzantine fault value.
                return transformerFaultValue;
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

    @Nullable
    private static Object injectByzantineFault(FilibusterClientInstrumentor filibusterClientInstrumentor, JSONObject byzantineFault, Class<?> returnType) {
        try {
            if (byzantineFault.has("type") && byzantineFault.has("value")) {
                ByzantineFaultType<?> byzantineFaultType = (ByzantineFaultType<?>) byzantineFault.get("type");
                Object value = byzantineFault.get("value");

                // Cast the byzantineFaultValue to the correct type.
                value = byzantineFaultType.cast(value);

                logger.log(Level.INFO, logPrefix + "byzantineFaultType: " + byzantineFaultType);
                logger.log(Level.INFO, logPrefix + "byzantineFaultValue: " + value);

                String sByzantineFaultValue = String.valueOf(value);

                // Notify Filibuster.
                filibusterClientInstrumentor.afterInvocationWithByzantineFault(sByzantineFaultValue, returnType.toString());

                return value;
            } else {
                String missingKey;
                if (byzantineFault.has("type")) {
                    missingKey = "value";
                } else {
                    missingKey = "type";
                }
                logger.log(Level.WARNING, logPrefix + "The byzantineFault does not have the required key " + missingKey);
                throw new FilibusterFaultInjectionException("injectByzantineFault: The byzantineFault does not have the required key " + missingKey);
            }
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, logPrefix + "Could not inject byzantine fault. The cast was probably not successful:", e);
            throw new FilibusterFaultInjectionException("Could not inject byzantine fault. The cast was probably not successful:", e);
        }
    }

    private static void generateAndThrowException(FilibusterClientInstrumentor filibusterClientInstrumentor, JSONObject forcedException) {
        String exceptionNameString = forcedException.getString("name");
        JSONObject forcedExceptionMetadata = forcedException.getJSONObject("metadata");
        String causeString = forcedExceptionMetadata.getString("cause");

        RuntimeException exceptionToThrow;

        switch (exceptionNameString) {
            case "io.lettuce.core.RedisCommandTimeoutException":
                exceptionToThrow = new RedisCommandTimeoutException(causeString);
                break;
            case "io.lettuce.core.RedisBusyException":
                exceptionToThrow = new RedisBusyException(causeString);
                break;
            case "io.lettuce.core.RedisCommandExecutionException":
                exceptionToThrow = new RedisCommandExecutionException(causeString);
                break;
            case "io.lettuce.core.RedisCommandInterruptedException":
                exceptionToThrow = new RedisCommandInterruptedException(new Throwable(causeString));
                break;
            case "io.lettuce.core.cluster.UnknownPartitionException":
                exceptionToThrow = new UnknownPartitionException(causeString);
                break;
            case "io.lettuce.core.cluster.PartitionSelectorException":
                exceptionToThrow = new PartitionSelectorException(causeString, new Partitions());
                break;
            case "io.lettuce.core.dynamic.batch.BatchException":
                exceptionToThrow = new BatchException(new ArrayList<>());
                break;
            default:
                throw new FilibusterFaultInjectionException("Cannot determine the execution cause to throw: " + causeString);
        }

        // Notify Filibuster.
        filibusterClientInstrumentor.afterInvocationWithException(exceptionToThrow);

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

}
