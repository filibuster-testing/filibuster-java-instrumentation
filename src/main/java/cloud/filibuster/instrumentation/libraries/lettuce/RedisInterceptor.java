package cloud.filibuster.instrumentation.libraries.lettuce;

import cloud.filibuster.exceptions.filibuster.FilibusterFaultInjectionException;
import cloud.filibuster.exceptions.filibuster.FilibusterRuntimeException;
import cloud.filibuster.instrumentation.datatypes.Callsite;
import cloud.filibuster.instrumentation.datatypes.CallsiteArguments;
import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;
import cloud.filibuster.instrumentation.storage.ContextStorage;
import cloud.filibuster.instrumentation.storage.ThreadLocalContextStorage;
import cloud.filibuster.junit.configuration.examples.byzantine.decoders.ByzantineDecoder;
import com.google.common.primitives.Bytes;
import io.lettuce.core.RedisBusyException;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandInterruptedException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.cluster.PartitionSelectorException;
import io.lettuce.core.cluster.UnknownPartitionException;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.dynamic.batch.BatchException;
import io.lettuce.core.dynamic.intercept.MethodInterceptor;
import io.lettuce.core.dynamic.intercept.MethodInvocation;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URI;

import static cloud.filibuster.instrumentation.Constants.REDIS_MODULE_NAME;
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

        // Possible Redis methods are RedisClient/get, RedisClient/set, RedisClient/sync, RedisClient/async, ...
        String redisFullMethodName = REDIS_MODULE_NAME + "/" + invocation.getMethod().getName();
        logger.log(Level.INFO, logPrefix + "redisFullMethodName: " + redisFullMethodName);

        // ******************************************************************************************
        // Construct preliminary call site information.
        // ******************************************************************************************

        CallsiteArguments callsiteArguments = new CallsiteArguments(invocation.getArguments().getClass(), Arrays.toString(invocation.getArguments()));

        Callsite callsite = new Callsite(redisServiceName, REDIS_MODULE_NAME, redisFullMethodName, callsiteArguments);

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

        logger.log(Level.INFO, logPrefix + "forcedException: " + forcedException);
        logger.log(Level.INFO, logPrefix + "failureMetadata: " + failureMetadata);
        logger.log(Level.INFO, logPrefix + "byzantineFault: " + byzantineFault);

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
            return injectByzantineFault(filibusterClientInstrumentor, byzantineFault);
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

    private static Object injectByzantineFault(FilibusterClientInstrumentor filibusterClientInstrumentor, JSONObject byzantineFault) {
        String byzantineFaultName = byzantineFault.getString("name");
        JSONObject byzantineFaultMetadata = byzantineFault.getJSONObject("metadata");
        Object byzantineDecoder = byzantineFault.get("decoder");

        Object byzantineFaultValue = byzantineFaultMetadata.get("value");

        // Cast the byzantineFaultValue to the correct type.
        byzantineFaultValue = castByzantineFaultValue(byzantineFaultValue, byzantineDecoder);

        logger.log(Level.INFO, logPrefix + "byzantineFaultName: " + byzantineFaultName);
        logger.log(Level.INFO, logPrefix + "byzantineDecoder: " + byzantineDecoder);
        logger.log(Level.INFO, logPrefix + "byzantineFaultValue: " + byzantineFaultValue.toString());

        // Build the additional metadata used to notify Filibuster.
        HashMap<String, String> additionalMetadata = new HashMap<>();
        additionalMetadata.put("name", byzantineFaultName);
        additionalMetadata.put("value", byzantineFaultValue.toString());
        additionalMetadata.put("decoder", byzantineDecoder.toString());

        // Notify Filibuster.
        filibusterClientInstrumentor.afterInvocationWithException(byzantineFaultName, byzantineFaultValue.toString(), additionalMetadata);

        return byzantineFaultValue;
    }

    // Cast the byzantineFaultValue to the correct type.
    private static Object castByzantineFaultValue(Object byzantineFaultValue, Object byzantineType) {
        // Get the decoder enum.
        ByzantineDecoder decoder = ByzantineDecoder.valueOf(byzantineType.toString());
        switch (decoder) {
            case STRING:
                return byzantineFaultValue.toString();
            case BYTE_ARRAY:
                List<Byte> byteArray = new ArrayList<>();
                // Cast the JSONArray to a byte array.
                ((JSONArray) byzantineFaultValue).toList().forEach(item -> byteArray.add(Byte.valueOf(item.toString())));
                return Bytes.toArray(byteArray);
        }
        throw new FilibusterRuntimeException("castByzantineFaultValue: Unknown ByzantineDecoder: " + decoder);
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
            case "io.lettuce.core.RedisConnectionException":
                exceptionToThrow = new RedisConnectionException(causeString);
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
