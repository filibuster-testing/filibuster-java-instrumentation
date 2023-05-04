package cloud.filibuster.instrumentation.libraries.lettuce;

import io.grpc.Metadata;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.dynamic.intercept.MethodInterceptor;
import io.lettuce.core.dynamic.intercept.MethodInvocation;

import javax.annotation.Nullable;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationEnabledProperty;


public class RedisIntermediaryInterceptor implements MethodInterceptor {
    public static Boolean disableInstrumentation = false;
    @Nullable
    private Metadata headers;
    private static final Logger logger = Logger.getLogger(FilibusterRedisClientInterceptor.class.getName());
    private final StatefulRedisConnection<String, String> redisConnection; //Will be needed later when data failures are injected
    public static boolean isFaultInjected = false;

    public RedisIntermediaryInterceptor(StatefulRedisConnection<String, String> redisConnection) {
        this.redisConnection = redisConnection;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // ******************************************************************************************
        // Figure out if we are inside of instrumentation.
        // ******************************************************************************************

        String instrumentationRequestStr = headers.get(
                Metadata.Key.of("x-filibuster-instrumentation", Metadata.ASCII_STRING_MARSHALLER));
        logger.log(Level.INFO, "instrumentationRequestStr: " + instrumentationRequestStr);
        boolean instrumentationRequest = Boolean.parseBoolean(instrumentationRequestStr);
        logger.log(Level.INFO, "instrumentationRequest: " + instrumentationRequest);
        if (! shouldInstrument() || instrumentationRequest) {
            logger.log(Level.INFO, "should instrument here: " + shouldInstrument());
        }

        if (isFaultInjected) {
            throw new RedisCommandTimeoutException("An exception was thrown at LettuceInterceptor");
        }
        return invocation.proceed();
    }
    private static boolean shouldInstrument() {
        return getInstrumentationEnabledProperty() && !disableInstrumentation;
    }
}
