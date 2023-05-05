package cloud.filibuster.instrumentation.libraries.lettuce;

import cloud.filibuster.instrumentation.datatypes.Callsite;
import cloud.filibuster.instrumentation.datatypes.CallsiteArguments;
import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;
import cloud.filibuster.instrumentation.storage.ContextStorage;
import cloud.filibuster.instrumentation.storage.ThreadLocalContextStorage;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.dynamic.intercept.MethodInterceptor;
import io.lettuce.core.dynamic.intercept.MethodInvocation;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationEnabledProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationServerCommunicationEnabledProperty;


public class RedisClientInterceptor implements MethodInterceptor {
    public static Boolean disableInstrumentation = false;
    private static final Logger logger = Logger.getLogger(RedisInterceptorFactory.class.getName());
    private final StatefulRedisConnection<String, String> redisConnection; //Will be needed later when data failures are injected
    protected ContextStorage contextStorage;
    public static Boolean disableServerCommunication = false;
    private final String redisConnectionString;
    private static FilibusterClientInstrumentor filibusterClientInstrumentor;


    public RedisClientInterceptor(StatefulRedisConnection<String, String> redisConnection, String redisConnectionString) {
        this.redisConnection = redisConnection;
        this.contextStorage = new ThreadLocalContextStorage();
        this.redisConnectionString = redisConnectionString;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        logger.log(Level.INFO, "RedisIntermediaryInterceptor: invoke() called");
        logger.log(Level.INFO, "shouldInstrument() is" +  shouldInstrument());

        // ******************************************************************************************
        // Extract callsite information.
        // ******************************************************************************************

        String redisModuleName = redisConnection.getClass().getPackage().getName();
        String redisMethodName = invocation.getMethod().getName();
        logger.log(Level.INFO, "moduleName: " + redisModuleName);
        logger.log(Level.INFO, "methodName: " + redisMethodName);

        // ******************************************************************************************
        // Construct preliminary call site information.
        // ******************************************************************************************

        CallsiteArguments callsiteArguments = new CallsiteArguments(invocation.getClass(), Arrays.toString(invocation.getArguments()));

        Callsite callsite = new Callsite(
                redisConnectionString,
                redisModuleName,
                redisMethodName,
                callsiteArguments
        );

        filibusterClientInstrumentor = new FilibusterClientInstrumentor(
                redisConnectionString,
                shouldCommunicateWithServer(),
                contextStorage,
                callsite
        );

        filibusterClientInstrumentor.prepareForInvocation();

        // ******************************************************************************************
        // Record invocation.
        // ******************************************************************************************

        filibusterClientInstrumentor.beforeInvocation();

        // ******************************************************************************************
        // Attach metadata to outgoing request.
        // ******************************************************************************************

        logger.log(Level.INFO, "requestId: " + filibusterClientInstrumentor.getOutgoingRequestId());

        // ******************************************************************************************
        // Get failure information.
        // ******************************************************************************************

        JSONObject forcedException = filibusterClientInstrumentor.getForcedException();
        JSONObject failureMetadata = filibusterClientInstrumentor.getFailureMetadata();

        logger.log(Level.INFO, "forcedException: " + forcedException);
        logger.log(Level.INFO, "failureMetadata: " + failureMetadata);

        if (forcedException != null && filibusterClientInstrumentor.shouldAbort()) {
            logger.log(Level.INFO, "RedisIntermediaryInterceptor: invoke() throwing forced exception");
        }

        // ******************************************************************************************
        // If we need to override the response, do it now before proceeding.
        // ******************************************************************************************

        if (failureMetadata != null && filibusterClientInstrumentor.shouldAbort()) {
            filibusterClientInstrumentor.afterInvocationWithException(failureMetadata.toString(), null, null);
            return null;
        }

        // ******************************************************************************************
        // If we need to throw, this is where we throw.
        // ******************************************************************************************

        if (forcedException != null && filibusterClientInstrumentor.shouldAbort()) {
            filibusterClientInstrumentor.afterInvocationWithException(forcedException.toString(), null, null);
            return null;
        }

        // ******************************************************************************************
        // Invoke.
        // ******************************************************************************************

        Class<?>[] paramTypes = new Class[invocation.getMethod().getParameterCount()];
        Arrays.fill(paramTypes, Object.class);
        Method method = redisConnection.sync().getClass().getDeclaredMethod(invocation.getMethod().getName(),
                paramTypes);
        Object result = method.invoke(redisConnection.sync(), invocation.getArguments());

        HashMap<String, String> returnValueProperties = new HashMap<>();
        returnValueProperties.put("toString", result.toString());

        filibusterClientInstrumentor.afterInvocationComplete(result.getClass().getName(), returnValueProperties);

        return result;
    }
    private static boolean shouldInstrument() {
        return getInstrumentationEnabledProperty() && !disableInstrumentation;
    }
    private static boolean shouldCommunicateWithServer() {
        return getInstrumentationServerCommunicationEnabledProperty() && !disableServerCommunication;
    }
}
