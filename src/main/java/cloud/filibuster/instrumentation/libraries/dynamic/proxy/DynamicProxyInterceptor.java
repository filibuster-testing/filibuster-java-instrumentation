package cloud.filibuster.instrumentation.libraries.dynamic.proxy;

import cloud.filibuster.exceptions.filibuster.FilibusterFaultInjectionException;
import cloud.filibuster.exceptions.filibuster.FilibusterRuntimeException;
import cloud.filibuster.instrumentation.datatypes.Callsite;
import cloud.filibuster.instrumentation.datatypes.CallsiteArguments;
import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;
import cloud.filibuster.instrumentation.libraries.lettuce.RedisInterceptorFactory;
import cloud.filibuster.instrumentation.storage.ContextStorage;
import cloud.filibuster.instrumentation.storage.ThreadLocalContextStorage;
import cloud.filibuster.junit.configuration.examples.redis.byzantine.types.ByzantineFaultType;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.instrumentation.helpers.Property.*;

public class DynamicProxyInterceptor<T> implements InvocationHandler {

    private static final Logger logger = Logger.getLogger(DynamicProxyInterceptor.class.getName());
    private final T targetObject;

    public static final Boolean disableInstrumentation = false;

    protected final ContextStorage contextStorage;

    public static final Boolean disableServerCommunication = false;
    private final String serviceName;
    private final String connectionString;
    private static final String logPrefix = "[FILIBUSTER-DB_INTERCEPTOR]: ";
    private final String moduleName;
    private FilibusterClientInstrumentor filibusterClientInstrumentor;

    private DynamicProxyInterceptor(T targetObject, String connectionString, String moduleName) {
        logger.log(Level.INFO, "DynamicProxyInterceptor: Constructor was called");
        this.targetObject = targetObject;
        this.contextStorage = new ThreadLocalContextStorage();
        this.connectionString = connectionString;
        this.serviceName = getServiceName(connectionString);
        this.moduleName = moduleName;
    }

    public String getServiceName() {
        return this.serviceName;
    }

    private static String getServiceName(String connectionString) {
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

        String fullMethodName = this.moduleName + "/" + method.getName();
        logger.log(Level.INFO, logPrefix + "fullMethodName: " + fullMethodName);

        // ******************************************************************************************
        // Construct preliminary call site information.
        // ******************************************************************************************

        CallsiteArguments callsiteArguments = new CallsiteArguments(args.getClass(), Arrays.toString(args));

        Callsite callsite = new Callsite(serviceName, moduleName, fullMethodName, callsiteArguments);

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

        Object invocationResult = invokeOnInterceptedObject(method, args);
        HashMap<String, String> returnValueProperties = new HashMap<>();

        // invocationResult could be null (e.g., when querying a key in that does not exist). If it is null, skip
        // execute the following block
        if (invocationResult != null) {
            returnValueProperties.put("toString", invocationResult.toString());
            // If "invocationResult" is an interface, return an intercepted proxy
            // (e.g., when calling StatefulRedisConnection.sync() where StatefulRedisConnection is an intercepted proxy,
            // the returned RedisCommands object should also be an intercepted proxy)
            if (method.getReturnType().isInterface() &&
                    method.getReturnType().getClassLoader() != null) {
                invocationResult = new RedisInterceptorFactory<>(invocationResult, connectionString)
                        .getProxy(method.getReturnType());
            }
        }

        filibusterClientInstrumentor.afterInvocationComplete(method.getReturnType().getName(), returnValueProperties);

        return invocationResult;
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
    private static Object injectByzantineFault(FilibusterClientInstrumentor filibusterClientInstrumentor, JSONObject byzantineFault) {
        if (byzantineFault.has("type") && byzantineFault.has("metadata")) {
            ByzantineFaultType<?> byzantineFaultType = (ByzantineFaultType<?>) byzantineFault.get("type");
            JSONObject byzantineFaultMetadata = byzantineFault.getJSONObject("metadata");

            // If a value was assigned, return it. Otherwise, return null.
            Object byzantineFaultValue = byzantineFaultMetadata.has("value") ? byzantineFaultMetadata.get("value") : null;

            // Cast the byzantineFaultValue to the correct type.
            byzantineFaultValue = byzantineFaultType.cast(byzantineFaultValue);

            logger.log(Level.INFO, logPrefix + "byzantineFaultType: " + byzantineFaultType);
            logger.log(Level.INFO, logPrefix + "byzantineFaultValue: " + byzantineFaultValue);

            // Build the additional metadata used to notify Filibuster.
            HashMap<String, String> additionalMetadata = new HashMap<>();
            String byzantineFaultValueString = byzantineFaultValue != null ? byzantineFaultValue.toString() : "null";
            additionalMetadata.put("name", byzantineFaultType.toString());
            additionalMetadata.put("value", byzantineFaultValueString);

            // Notify Filibuster.
            filibusterClientInstrumentor.afterInvocationWithException(byzantineFaultType.toString(), byzantineFaultValueString, additionalMetadata);

            return byzantineFaultValue;
        } else {
            logger.log(Level.WARNING, logPrefix + "The byzantineFault either does not have the required key 'type' or 'metadata'");
            return null;
        }
    }

    private static void generateAndThrowException(FilibusterClientInstrumentor filibusterClientInstrumentor, JSONObject forcedException) {
        String exceptionNameString = forcedException.getString("name");
        JSONObject forcedExceptionMetadata = forcedException.getJSONObject("metadata");
        String causeString = forcedExceptionMetadata.getString("cause");

        RuntimeException exceptionToThrow = null;

        switch (exceptionNameString) {
            case "abc":  // TODO: Replace with actual exception name.
                logger.log(Level.INFO, "Do something");
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

    @SuppressWarnings("unchecked")
    public static <T> T createInterceptor(T target, String connectionString, String moduleName) {
        logger.log(Level.INFO, "CockroachInterceptor: createInterceptor was called");
        return (T) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                target.getClass().getInterfaces(),
                new DynamicProxyInterceptor<>(target, connectionString, moduleName)
        );
    }

}
