package cloud.filibuster.instrumentation.libraries.lettuce;

import cloud.filibuster.instrumentation.datatypes.Callsite;
import cloud.filibuster.instrumentation.datatypes.CallsiteArguments;
import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;
import cloud.filibuster.instrumentation.storage.ContextStorage;
import cloud.filibuster.instrumentation.storage.ThreadLocalContextStorage;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.dynamic.intercept.MethodInterceptor;
import io.lettuce.core.dynamic.intercept.MethodInvocation;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationEnabledProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationServerCommunicationEnabledProperty;


public class RedisIntermediaryInterceptor implements MethodInterceptor {
    public static Boolean disableInstrumentation = false;
    private static final Logger logger = Logger.getLogger(FilibusterRedisClientInterceptor.class.getName());
    private final StatefulRedisConnection<String, String> redisConnection; //Will be needed later when data failures are injected
    public static boolean isFaultInjected = false;

    protected ContextStorage contextStorage;
    public static Boolean disableServerCommunication = false;
    private final String redisConnectionString;


    public RedisIntermediaryInterceptor(StatefulRedisConnection<String, String> redisConnection, String redisConnectionString) {
        this.redisConnection = redisConnection;
        this.contextStorage = new ThreadLocalContextStorage();
        this.redisConnectionString = redisConnectionString;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        logger.log(Level.INFO, "RedisIntermediaryInterceptor: invoke() called");

        // ******************************************************************************************
        // Figure out if we are inside of instrumentation.
        // ******************************************************************************************
        FilibusterClientInstrumentor filibusterClientInstrumentor;
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

        if (isFaultInjected) {
            throw new RedisCommandTimeoutException("An exception was thrown at LettuceInterceptor");
        }
        return invocation.proceed();
    }
    private static boolean shouldInstrument() {
        return getInstrumentationEnabledProperty() && !disableInstrumentation;
    }
    private static boolean shouldCommunicateWithServer() {
        return getInstrumentationServerCommunicationEnabledProperty() && !disableServerCommunication;
    }
}
