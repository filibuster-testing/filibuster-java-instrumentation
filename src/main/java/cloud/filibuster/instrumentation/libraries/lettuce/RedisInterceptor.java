package cloud.filibuster.instrumentation.libraries.lettuce;

import cloud.filibuster.exceptions.filibuster.FilibusterFaultInjectionException;
import cloud.filibuster.instrumentation.datatypes.Callsite;
import cloud.filibuster.instrumentation.datatypes.CallsiteArguments;
import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;
import cloud.filibuster.instrumentation.storage.ContextStorage;
import cloud.filibuster.instrumentation.storage.ThreadLocalContextStorage;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.dynamic.intercept.MethodInterceptor;
import io.lettuce.core.dynamic.intercept.MethodInvocation;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.instrumentation.Constants.REDIS_MODULE_NAME;
import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationEnabledProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationServerCommunicationEnabledProperty;

public class RedisInterceptor<T> implements MethodInterceptor {
    public static Boolean disableInstrumentation = false;
    private static final Logger logger = Logger.getLogger(RedisInterceptorFactory.class.getName());
    protected ContextStorage contextStorage;
    public static Boolean disableServerCommunication = false;
    private final String redisConnectionString;
    private final T interceptedObject;
    private final String logPrefix = "[FILIBUSTER-REDIS_INTERCEPTOR]: ";

    private FilibusterClientInstrumentor filibusterClientInstrumentor;

    public RedisInterceptor(T interceptedObject, String redisConnectionString) {
        this.contextStorage = new ThreadLocalContextStorage();
        this.redisConnectionString = redisConnectionString;
        this.interceptedObject = interceptedObject;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        logger.log(Level.INFO, "RedisInterceptor: invoke() called");
        logger.log(Level.INFO, logPrefix + "shouldInstrument() is" + shouldInstrument());

        // ******************************************************************************************
        // Extract callsite information.
        // ******************************************************************************************

        String redisMethodName = invocation.getMethod().getName();  // Possible Redis methods are get, set, sync, async, ...
        logger.log(Level.INFO, logPrefix + "methodName: " + redisMethodName);

        // ******************************************************************************************
        // Construct preliminary call site information.
        // ******************************************************************************************

        CallsiteArguments callsiteArguments = new CallsiteArguments(invocation.getClass(), Arrays.toString(invocation.getArguments()));

        Callsite callsite = new Callsite(redisConnectionString, REDIS_MODULE_NAME, redisMethodName, callsiteArguments);

        filibusterClientInstrumentor = new FilibusterClientInstrumentor(redisConnectionString, shouldCommunicateWithServer(), contextStorage, callsite);

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

        logger.log(Level.INFO, logPrefix + "forcedException: " + forcedException);

        // ******************************************************************************************
        // If we need to throw, this is where we throw.
        // ******************************************************************************************

        if (forcedException != null && filibusterClientInstrumentor.shouldAbort()) {
            generateAndThrowException(filibusterClientInstrumentor, forcedException);
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
            if (invocation.getMethod().getReturnType().isInterface()) {
                invocationResult = new RedisInterceptorFactory<>(invocationResult, redisConnectionString).getProxy(invocation.getMethod().getReturnType());
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

    private static void generateAndThrowException(FilibusterClientInstrumentor filibusterClientInstrumentor, JSONObject forcedException) {
        String exceptionNameString = forcedException.getString("name");
        JSONObject forcedExceptionMetadata = forcedException.getJSONObject("metadata");
        String causeString = forcedExceptionMetadata.getString("cause");

        RuntimeException exceptionToThrow;

        if (exceptionNameString.equals("io.lettuce.core.RedisCommandTimeoutException")) {
            exceptionToThrow = new RedisCommandTimeoutException(causeString);
        } else if (exceptionNameString.equals("io.lettuce.core.RedisConnectionException")) {
            exceptionToThrow = new RedisConnectionException(causeString);
        } else {
            throw new FilibusterFaultInjectionException("Cannot determine the execution cause to throw: " + causeString);
        }

        // Notify Filibuster.
        filibusterClientInstrumentor.afterInvocationWithException(exceptionToThrow);

        // Throw callsite exception.
        throw exceptionToThrow;
    }

    private static boolean shouldInstrument() {
        return getInstrumentationEnabledProperty() && !disableInstrumentation;
    }

    private static boolean shouldCommunicateWithServer() {
        return getInstrumentationServerCommunicationEnabledProperty() && !disableServerCommunication;
    }
}
