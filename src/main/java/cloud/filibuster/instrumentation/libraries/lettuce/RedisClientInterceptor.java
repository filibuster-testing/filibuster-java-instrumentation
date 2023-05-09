package cloud.filibuster.instrumentation.libraries.lettuce;

import cloud.filibuster.exceptions.filibuster.FilibusterFaultInjectionException;
import cloud.filibuster.instrumentation.datatypes.Callsite;
import cloud.filibuster.instrumentation.datatypes.CallsiteArguments;
import cloud.filibuster.instrumentation.instrumentors.FilibusterClientInstrumentor;
import cloud.filibuster.instrumentation.storage.ContextStorage;
import cloud.filibuster.instrumentation.storage.ThreadLocalContextStorage;
import com.linecorp.armeria.client.UnprocessedRequestException;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.dynamic.intercept.MethodInterceptor;
import io.lettuce.core.dynamic.intercept.MethodInvocation;
import io.netty.channel.ConnectTimeoutException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.instrumentation.Constants.REDIS_MODULE_NAME;
import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationEnabledProperty;
import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationServerCommunicationEnabledProperty;


public class RedisClientInterceptor<T> implements MethodInterceptor {
    public static Boolean disableInstrumentation = false;
    private static final Logger logger = Logger.getLogger(RedisInterceptorFactory.class.getName());
    private final RedisClient redisClient;
    protected ContextStorage contextStorage;
    public static Boolean disableServerCommunication = false;
    private final String redisConnectionString;
    private T redisConnection;
    private final Class<T> commandType;


    public RedisClientInterceptor(RedisClient redisClient, String redisConnectionString, Class<T> commandType) {
        this.redisClient = redisClient;
        this.contextStorage = new ThreadLocalContextStorage();
        this.redisConnectionString = redisConnectionString;
        this.commandType = commandType;
        this.createRedisConnection();
    }

    private void createRedisConnection() {
        if (commandType.equals(RedisCommands.class)) {
            this.redisConnection = commandType.cast(this.redisClient.connect().sync());
        } else if (commandType.equals(RedisAsyncCommands.class)) {
            this.redisConnection = commandType.cast(this.redisClient.connect().async());
        } else if (commandType.equals(RedisReactiveCommands.class)) {
            this.redisConnection = commandType.cast(this.redisClient.connect().reactive());
        } else {
            throw new IllegalArgumentException("Lettuce command type is unknown.");
        }
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        logger.log(Level.INFO, "RedisIntermediaryInterceptor: invoke() called");
        logger.log(Level.INFO, "shouldInstrument() is" + shouldInstrument());

        // ******************************************************************************************
        // Extract callsite information.
        // ******************************************************************************************

        String redisMethodName = invocation.getMethod().getName();
        logger.log(Level.INFO, "methodName: " + redisMethodName);

        // ******************************************************************************************
        // Construct preliminary call site information.
        // ******************************************************************************************

        CallsiteArguments callsiteArguments = new CallsiteArguments(invocation.getClass(), Arrays.toString(invocation.getArguments()));

        Callsite callsite = new Callsite(
                redisConnectionString,
                REDIS_MODULE_NAME,
                redisMethodName,
                callsiteArguments
        );

        FilibusterClientInstrumentor filibusterClientInstrumentor = new FilibusterClientInstrumentor(
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
        // If we need to throw, this is where we throw.
        // ******************************************************************************************

        if (forcedException != null && filibusterClientInstrumentor.shouldAbort()) {
            generateAndThrowException(filibusterClientInstrumentor, forcedException);
        }

        // ******************************************************************************************
        // Invoke.
        // ******************************************************************************************

        Class<?>[] paramTypes = new Class[invocation.getMethod().getParameterCount()];
        Arrays.fill(paramTypes, Object.class);
        Method method = redisConnection.getClass().getMethod(invocation.getMethod().getName(),
                paramTypes);
        Object result = method.invoke(redisConnection, invocation.getArguments());
        HashMap<String, String> returnValueProperties = new HashMap<>();
        returnValueProperties.put("toString", result.toString());
        filibusterClientInstrumentor.afterInvocationComplete(result.getClass().getName(), returnValueProperties);
        return result;
    }

    private static void generateAndThrowException(
            FilibusterClientInstrumentor filibusterClientInstrumentor,
            JSONObject forcedException
    ) {
        String exceptionNameString = forcedException.getString("name");
        JSONObject forcedExceptionMetadata = forcedException.getJSONObject("metadata");
        String causeString = forcedExceptionMetadata.getString("cause");

        RuntimeException exceptionToThrow;

        if (exceptionNameString.equals("RedisConnectionException") && causeString.contains("java.net.UnknownHostException: Failed to resolve")) {
            String message = "An error occurred";
            ConnectTimeoutException cause = new ConnectTimeoutException(message);
            exceptionToThrow = UnprocessedRequestException.of(cause);
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
