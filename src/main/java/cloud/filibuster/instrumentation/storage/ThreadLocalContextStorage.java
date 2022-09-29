package cloud.filibuster.instrumentation.storage;

import cloud.filibuster.instrumentation.datatypes.VectorClock;

import java.util.HashMap;

import static cloud.filibuster.instrumentation.Constants.FILIBUSTER_EXECUTION_INDEX;
import static cloud.filibuster.instrumentation.Constants.FILIBUSTER_ORIGIN_VCLOCK;
import static cloud.filibuster.instrumentation.Constants.FILIBUSTER_REQUEST_ID;
import static cloud.filibuster.instrumentation.Constants.FILIBUSTER_VCLOCK;

public class ThreadLocalContextStorage implements ContextStorage {
    public static boolean useGlobalContext = false;

    final private static HashMap<String, InheritableThreadLocal> threadLocalDictionary = new HashMap<>();
    final private static HashMap<String, Object> globalDictionary = new HashMap<>();

    public static void clear() {
        threadLocalDictionary.clear();
        globalDictionary.clear();
    }

    @SuppressWarnings("unchecked")
    public static <T> void set(String key, T value) {
        if (!useGlobalContext) {
            InheritableThreadLocal<T> threadLocal;

            if (threadLocalDictionary.containsKey(key)) {
                threadLocal = threadLocalDictionary.get(key);
            } else {
                threadLocal = new InheritableThreadLocal<>();
                threadLocalDictionary.put(key, threadLocal);
            }
            threadLocal.set(value);
        } else {
            globalDictionary.put(key, value);
        }
    }

    @SuppressWarnings({"TypeParameterUnusedInFormals", "unchecked"})
    public static <T> T get(String key) {
        if (!useGlobalContext) {
            if (threadLocalDictionary.containsKey(key)) {
                InheritableThreadLocal<T> threadLocal = threadLocalDictionary.get(key);
                return threadLocal.get();
            } else {
                return null;
            }
        } else {
            if (globalDictionary.containsKey(key)) {
                return (T) globalDictionary.get(key);
            } else {
                return null;
            }
        }
    }

    @Override
    public String getRequestId() {
        return get(FILIBUSTER_REQUEST_ID);
    }

    @Override
    public VectorClock getVectorClock() {
        return get(FILIBUSTER_VCLOCK);
    }

    @Override
    public VectorClock getOriginVectorClock() {
        return get(FILIBUSTER_ORIGIN_VCLOCK);
    }

    @Override
    public String getDistributedExecutionIndex() {
        return get(FILIBUSTER_EXECUTION_INDEX);
    }

    @Override
    public void setRequestId(String requestId) {
        set(FILIBUSTER_REQUEST_ID, requestId);
    }

    @Override
    public void setVectorClock(VectorClock vectorClock) {
        set(FILIBUSTER_VCLOCK, vectorClock);
    }

    @Override
    public void setOriginVectorClock(VectorClock originVectorClock) {
        set(FILIBUSTER_ORIGIN_VCLOCK, originVectorClock);
    }

    @Override
    public void setDistributedExecutionIndex(String distributedExecutionIndex) {
        set(FILIBUSTER_EXECUTION_INDEX, distributedExecutionIndex);
    }
}
