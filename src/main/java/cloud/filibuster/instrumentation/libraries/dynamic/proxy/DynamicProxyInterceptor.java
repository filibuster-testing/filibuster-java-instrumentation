package cloud.filibuster.instrumentation.libraries.dynamic.proxy;

import cloud.filibuster.exceptions.filibuster.FilibusterFaultInjectionException;
import cloud.filibuster.exceptions.filibuster.FilibusterRuntimeException;
import cloud.filibuster.instrumentation.datatypes.Callsite;
import cloud.filibuster.instrumentation.datatypes.CallsiteArguments;
import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;
import cloud.filibuster.instrumentation.storage.ContextStorage;
import cloud.filibuster.instrumentation.storage.ThreadLocalContextStorage;
import cloud.filibuster.junit.configuration.examples.db.byzantine.types.ByzantineFaultType;
import com.datastax.oss.driver.api.core.servererrors.OverloadedException;
import org.json.JSONObject;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import software.amazon.awssdk.services.dynamodb.model.RequestLimitExceededException;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationEnabledProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationServerCommunicationEnabledProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getRedisTestPortNondeterminismProperty;


public class DynamicProxyInterceptor<T> implements InvocationHandler {

    private static final Logger logger = Logger.getLogger(DynamicProxyInterceptor.class.getName());
    private final T targetObject;

    public static final Boolean disableInstrumentation = false;

    protected final ContextStorage contextStorage;

    public static final Boolean disableServerCommunication = false;
    private final String serviceName;
    private final String connectionString;
    private static final String logPrefix = "[FILIBUSTER-PROXY_INTERCEPTOR]: ";
    private FilibusterClientInstrumentor filibusterClientInstrumentor;

    private DynamicProxyInterceptor(T targetObject, String connectionString) {
        logger.log(Level.INFO, logPrefix + "Constructor was called");
        this.targetObject = targetObject;
        this.contextStorage = new ThreadLocalContextStorage();
        this.connectionString = connectionString;
        this.serviceName = extractServiceFromConnection(connectionString);
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
                invocationResult = DynamicProxyInterceptor.createInterceptor(invocationResult, connectionString);
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
        try {
            if (byzantineFault.has("type") && byzantineFault.has("value")) {
                ByzantineFaultType<?> byzantineFaultType = (ByzantineFaultType<?>) byzantineFault.get("type");
                Object value = byzantineFault.get("value");

                // Cast the byzantineFaultValue to the correct type.
                value = byzantineFaultType.cast(value);

                logger.log(Level.INFO, logPrefix + "byzantineFaultType: " + byzantineFaultType);
                logger.log(Level.INFO, logPrefix + "byzantineFaultValue: " + value);

                String sByzantineFaultValue = value != null ? value.toString() : "null";

                // Notify Filibuster.
                filibusterClientInstrumentor.afterInvocationWithByzantineFault(sByzantineFaultValue, byzantineFaultType.toString(), null);

                return value;
            } else {
                String missingKey;
                if (byzantineFault.has("type")) {
                    missingKey = "value";
                } else {
                    missingKey = "type";
                }
                throw new FilibusterFaultInjectionException("injectByzantineFault: The byzantineFault does not have the required key " + missingKey);
            }
        } catch (Exception e) {
            throw new FilibusterFaultInjectionException("Could not inject byzantine fault. The cast was probably not successful:", e);
        }
    }

    private static void generateAndThrowException(FilibusterClientInstrumentor filibusterClientInstrumentor, JSONObject forcedException) throws Exception {
        String exceptionNameString = forcedException.getString("name");
        JSONObject forcedExceptionMetadata = forcedException.getJSONObject("metadata");
        String causeString = forcedExceptionMetadata.getString("cause");
        String codeString = forcedExceptionMetadata.getString("code");

        Exception exceptionToThrow;

        switch (exceptionNameString) {  // TODO: Refactor the switch to an interface
            case "org.postgresql.util.PSQLException":
                exceptionToThrow = new PSQLException(new ServerErrorMessage(causeString));
                break;
            case "com.datastax.oss.driver.api.core.servererrors.OverloadedException":
                exceptionToThrow = new OverloadedException(null);
                break;
            case "software.amazon.awssdk.services.dynamodb.model.RequestLimitExceededException":
                exceptionToThrow = RequestLimitExceededException.builder().message(causeString).statusCode(Integer.parseInt(codeString))
                        .requestId(UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT)).build();
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
    public static <T> T createInterceptor(T target, String connectionString) {
        logger.log(Level.INFO, logPrefix + "createInterceptor was called");
        return (T) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                target.getClass().getInterfaces(),
                new DynamicProxyInterceptor<>(target, connectionString)
        );
    }

}
