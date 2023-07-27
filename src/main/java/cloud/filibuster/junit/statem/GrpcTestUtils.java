package cloud.filibuster.junit.statem;

import java.util.concurrent.atomic.AtomicReference;

public final class GrpcTestUtils {
    private static final AtomicReference<Boolean> insideOfAssertOnExceptionBlock = new AtomicReference<>(false);
    private static final AtomicReference<Boolean> insideOfAssertOnFaultBlock = new AtomicReference<>(false);
    private static final AtomicReference<Boolean> insideOfFailureBlock = new AtomicReference<>(false);
    private static final AtomicReference<Boolean> insideOfAssertStubBlock = new AtomicReference<>(false);
    private static final AtomicReference<Boolean> insideOfStubBlock = new AtomicReference<>(false);

    public static synchronized boolean isInsideOfAssertOnExceptionBlock() {
        return GrpcTestUtils.insideOfAssertOnExceptionBlock.get();
    }

    public static synchronized void setInsideOfAssertOnExceptionBlock(boolean insideOfAssertOnExceptionBlock) {
        GrpcTestUtils.insideOfAssertOnExceptionBlock.set(insideOfAssertOnExceptionBlock);
    }

    public static synchronized boolean isInsideOfAssertOnFaultBlock() {
        return GrpcTestUtils.insideOfAssertOnFaultBlock.get();
    }

    public static synchronized void setInsideOfAssertOnFaultBlock(boolean insideOfAssertOnFaultBlock) {
        GrpcTestUtils.insideOfAssertOnFaultBlock.set(insideOfAssertOnFaultBlock);
    }

    public static synchronized boolean isInsideOfFailureBlock() {
        return GrpcTestUtils.insideOfFailureBlock.get();
    }

    public static synchronized void setInsideOfFailureBlock(boolean insideOfFailureBlock) {
        GrpcTestUtils.insideOfFailureBlock.set(insideOfFailureBlock);
    }

    public static synchronized boolean getInsideOfAssertStubBlock() {
        return GrpcTestUtils.insideOfAssertStubBlock.get();
    }

    public static synchronized void setInsideOfAssertStubBlock(boolean insideOfAssertStubBlock) {
        GrpcTestUtils.insideOfAssertStubBlock.set(insideOfAssertStubBlock);
    }

    public static synchronized boolean isInsideOfStubBlock() {
        return GrpcTestUtils.insideOfStubBlock.get();
    }

    public static synchronized void setInsideOfStubBlock(boolean insideOfStubBlock) {
        GrpcTestUtils.insideOfStubBlock.set(insideOfStubBlock);
    }
}
