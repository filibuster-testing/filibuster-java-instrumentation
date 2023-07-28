package cloud.filibuster.junit.statem;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestInternalRuntimeException;
import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException.FilibusterGrpcAmbiguousThrowAndErrorPropagationException;
import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException.FilibusterGrpcAmbiguousFailureHandlingException;
import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException.FilibusterGrpcAssertionUsedOutsideFailureBlockException;
import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException.FilibusterGrpcAssertionsDidNotHoldUnderErrorResponseException;
import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException.FilibusterGrpcAssertionsForAssertOnExceptionFailedException;
import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException.FilibusterGrpcAssertOnFaultException;
import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException.FilibusterGrpcAssertTestBlockFailedException;
import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException.FilibusterGrpcFailedRPCException;
import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException.FilibusterGrpcInjectedFaultHasUnspecifiedFailureBehaviorException;
import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException.FilibusterGrpcInvokedRPCUnimplementedException;
import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException.FilibusterGrpcMissingAssertionForStatusCodeException;
import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException.FilibusterGrpcMultipleFaultsInjectedException;
import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException.FilibusterGrpcReadOnlyRPCUsedOutsideAssertOnExceptionAndAssertOnFaultException;
import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException.FilibusterGrpcSideEffectingRPCUsedOutsideAssertOnExceptionAndAssertOnFaultException;
import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException.FilibusterGrpcStubbedRPCHasNoAssertionsException;
import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException.FilibusterGrpcSuppressedStatusCodeException;
import cloud.filibuster.exceptions.filibuster.FilibusterGrpcTestRuntimeException.FilibusterGrpcThrownExceptionHasUnspecifiedFailureBehaviorException;
import cloud.filibuster.instrumentation.datatypes.Pair;
import cloud.filibuster.junit.assertions.Helpers;

import cloud.filibuster.junit.statem.keys.CompositeFaultKey;
import cloud.filibuster.junit.statem.keys.FaultKey;
import cloud.filibuster.junit.statem.keys.SingleFaultKey;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static cloud.filibuster.junit.Assertions.getExecutedRPCs;
import static cloud.filibuster.junit.Assertions.getFailedRPCs;
import static cloud.filibuster.junit.Assertions.getFaultsInjected;
import static cloud.filibuster.junit.statem.GrpcTestUtils.isInsideOfAssertOnExceptionBlock;
import static cloud.filibuster.junit.statem.GrpcTestUtils.isInsideOfAssertOnFaultBlock;
import static cloud.filibuster.junit.statem.GrpcTestUtils.isInsideOfFailureBlock;
import static cloud.filibuster.junit.statem.GrpcTestUtils.setInsideOfAssertOnExceptionBlock;
import static cloud.filibuster.junit.statem.GrpcTestUtils.setInsideOfAssertOnFaultBlock;
import static cloud.filibuster.junit.statem.GrpcTestUtils.setInsideOfFailureBlock;
import static cloud.filibuster.junit.statem.GrpcTestUtils.setInsideOfStubBlock;
import static cloud.filibuster.junit.statem.GrpcTestUtils.setInsideOfAssertStubBlock;
import static cloud.filibuster.junit.statem.keys.CompositeFaultKey.findMatchingFaultKey;
import static cloud.filibuster.junit.statem.keys.CompositeFaultKey.findMatchingFaultKeys;

/**
 * Testing interface for writing tests of services that issue GRPCs.  Provides a number of features:
 *
 * <ul>
 * <li>Ensures that setup, test, and teardown are logically separated, making fault injection only active when the test is in progress.</li>
 * <li>Ensures that developers stub all dependencies and indicate precisely which dependencies will be invoked how many times.</li>
 * <li>Forces developers to specifically indicate failure handling behavior for all RPCs issued by the application.</li>
 * <li>Applies compositional reasoning to minimize the number of failure scenarios the user has to specify.</li>
 * </ul>
 *
 * <p>Requires that developers implement this interface and create a test method annotated with
 * {@link cloud.filibuster.junit.TestWithFilibuster @TestWithFilibuster}
 * that just executes the interface function {@link #execute() super.execute()}.</p>
 */
public interface FilibusterGrpcTest {
    AtomicReference<GeneratedMessageV3> response = new AtomicReference<GeneratedMessageV3>();

    /**
     * Set the GRPc response.  For use in {@link #executeTestBlock()}.
     *
     * @param response GRPC response
     */
    default void setResponse(GeneratedMessageV3 response) {
        FilibusterGrpcTest.response.set(response);
    }

    /**
     * Get the GRPC response.  For use in {@link #assertTestBlock()}.
     *
     * @return the GRPC response
     */
    default GeneratedMessageV3 getResponse() {
        return FilibusterGrpcTest.response.get();
    }

    /**
     * Test authors should place failure specification in this method.  For example,
     * {@link #assertFaultThrows} to indicate that a method,
     * when failed, will cause the service to return an exception.
     */
    void failureBlock();

    /**
     * Test authors should put code for setup of the test in here.  Use of this block inhibits fault injection for any
     * RPCs issued in this block.  For example, if using the service-under-test's API to stage state for running a test,
     * any downstream dependencies that are invoked as part of execution of the setup block will not be tested for
     * faults.  Thereby, this ensures that faults do not prevent proper staging before execution of the actual test.
     */
    void setupBlock();

    /**
     * Test authors should use this block for stubbing any downstream dependencies that need to be stubbed for the test
     * to pass.  Stubbing of these dependencies should be performed using the Filibuster-provided
     * {@link GrpcMock#stubFor(MethodDescriptor, Object, Object) GrpcMock.stubFor(MethodDescriptor, ReqT, ResT)}
     * method and should NOT use GrpcMock directly as Filibuster needs to interpose on these mocks for fault injection
     * testing.
     */
    void stubBlock();

    /**
     * Test authors should place test execution code here.  Any downstream RPCs that are invoked as a result of a method
     * invocation inside of this block will be subject to fault injection.  Test authors should store any responses
     * that are required for assertions -- placed in the assertion block {@link #assertTestBlock()} -- in instance
     * variables.
     */
    void executeTestBlock();

    /**
     * Test authors should place test assertions here.  Use of this block inhibits fault injection, which is necessary
     * when using the very API under test to perform assertions.  For example, if your test looks up the user to subscribe
     * them to a service and looks up the user to verify they were subscribed, you only want to inject a fault on the
     * user lookup that's part of subscribing -- not verifying that they did indeed get subscribed.
     */
    void assertTestBlock();

    /**
     * Test authors should use this block for performing assertions on stub invocations.  This should be done using the
     * Filibuster-provided {@link GrpcMock#verifyThat(MethodDescriptor, int)} method and NOT using GrpcMock directly, as
     * Filibuster must interpose on these calls to automatically adjust the expected invocation count when faults are
     * injected.
     */
    void assertStubBlock();

    /**
     * Test authors should use this block for performing test teardown.  Similar to the {@link #setupBlock()} method,
     * fault injection is inhibited for any downstream dependencies that are invoked as part of any method invoked in
     * this block.
     */
    void teardownBlock();

    // *****************************************************************************************************************
    // Fault API: specify state of the system when exception is thrown.
    // *****************************************************************************************************************

    HashMap<Status.Code, Runnable> errorAssertions = new HashMap<>();

    /**
     * Use of this method informs Filibuster that different assertions will hold true when this error code is
     * returned to the user, as part of a {@link StatusRuntimeException}.  These assertions should be placed in the
     * associated {@link Runnable}.  This block will replace the assertions in {@link #assertTestBlock()}.
     *
     * @param code     the thrown exception's status code when a fault is injected
     * @param runnable assertion block
     */
    default void assertOnException(Status.Code code, Runnable runnable) {
        if (isInsideOfFailureBlock()) {
            errorAssertions.put(code, runnable);
        } else {
            throw new FilibusterGrpcAssertionUsedOutsideFailureBlockException("assertOnException");
        }
    }

    // *****************************************************************************************************************
    // Fault API: specify faults that throw.
    // *****************************************************************************************************************

    HashMap<FaultKey, Map.Entry<Status.Code, String>> faultKeysThatThrow = new HashMap<>();

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC endpoint will result in the service
     * returning a {@link StatusRuntimeException} with the specified code and message.
     *
     * @param compositeFaultSpecification {@link CompositeFaultSpecification}
     * @param thrownCode                  the thrown exception's status code when a fault is injected
     * @param thrownMessage               the thrown exception's message that is returned when a fault is injected
     */
    default void assertFaultThrows(
            CompositeFaultSpecification compositeFaultSpecification,
            Status.Code thrownCode,
            String thrownMessage
    ) {
        if (isInsideOfFailureBlock()) {
            faultKeysThatThrow.put(new CompositeFaultKey(compositeFaultSpecification), Pair.of(thrownCode, thrownMessage));
        } else {
            throw new FilibusterGrpcAssertionUsedOutsideFailureBlockException("assertFaultThrows");
        }
    }

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC endpoint will result in the service
     * returning a {@link StatusRuntimeException} with the specified code and message.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param thrownCode       the thrown exception's status code when a fault is injected
     * @param thrownMessage    the thrown exception's message that is returned when a fault is injected
     * @param <ReqT>           the request type for this method
     * @param <ResT>           the response type for this method
     */
    default <ReqT, ResT> void assertFaultThrows(
            MethodDescriptor<ReqT, ResT> methodDescriptor,
            Status.Code thrownCode,
            String thrownMessage
    ) {
        if (isInsideOfFailureBlock()) {
            faultKeysThatThrow.put(new SingleFaultKey<>(methodDescriptor), Pair.of(thrownCode, thrownMessage));
        } else {
            throw new FilibusterGrpcAssertionUsedOutsideFailureBlockException("assertFaultThrows");
        }
    }

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC endpoint will result in the service
     * returning a {@link StatusRuntimeException} with the specified code and message.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param code             the status code of the fault injected
     * @param thrownCode       the thrown exception's status code when a fault is injected
     * @param thrownMessage    the thrown exception's message that is returned when a fault is injected
     * @param <ReqT>           the request type for this method
     * @param <ResT>           the response type for this method
     */
    default <ReqT, ResT> void assertFaultThrows(
            MethodDescriptor<ReqT, ResT> methodDescriptor,
            Status.Code code,
            Status.Code thrownCode,
            String thrownMessage
    ) {
        if (isInsideOfFailureBlock()) {
            faultKeysThatThrow.put(new SingleFaultKey<>(methodDescriptor, code), Pair.of(thrownCode, thrownMessage));
        } else {
            throw new FilibusterGrpcAssertionUsedOutsideFailureBlockException("assertFaultThrows");
        }
    }

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC endpoint will result in the service
     * returning a {@link StatusRuntimeException} with the specified code and message.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param request          the request the fault was injected on
     * @param thrownCode       the thrown exception's status code when a fault is injected
     * @param thrownMessage    the thrown exception's message that is returned when a fault is injected
     * @param <ReqT>           the request type for this method
     * @param <ResT>           the response type for this method
     */
    default <ReqT, ResT> void assertFaultThrows(
            MethodDescriptor<ReqT, ResT> methodDescriptor,
            ReqT request,
            Status.Code thrownCode,
            String thrownMessage
    ) {
        if (isInsideOfFailureBlock()) {
            faultKeysThatThrow.put(new SingleFaultKey<>(methodDescriptor, request), Pair.of(thrownCode, thrownMessage));
        } else {
            throw new FilibusterGrpcAssertionUsedOutsideFailureBlockException("assertFaultThrows");
        }
    }

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC endpoint will result in the service
     * returning a {@link StatusRuntimeException} with the specified code and message.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param code             the status code of the fault injected
     * @param request          the request the fault was injected on
     * @param thrownCode       the thrown exception's status code when a fault is injected
     * @param thrownMessage    the thrown exception's message that is returned when a fault is injected
     * @param <ReqT>           the request type for this method
     * @param <ResT>           the response type for this method
     */
    default <ReqT, ResT> void assertFaultThrows(
            MethodDescriptor<ReqT, ResT> methodDescriptor,
            Status.Code code,
            ReqT request,
            Status.Code thrownCode,
            String thrownMessage
    ) {
        if (isInsideOfFailureBlock()) {
            faultKeysThatThrow.put(new SingleFaultKey<>(methodDescriptor, code, request), Pair.of(thrownCode, thrownMessage));
        } else {
            throw new FilibusterGrpcAssertionUsedOutsideFailureBlockException("assertFaultThrows");
        }
    }

    // *****************************************************************************************************************
    // Fault API: no error handling, should propagate upstream.
    // *****************************************************************************************************************

    List<FaultKey> faultKeysThatPropagate = new ArrayList<>();

    /**
     * Indicate that this RPC endpoint has no error handling, or an error handler that logs and rethrows, and that
     * any faults injected will be propagated directly back to the upstream.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param <ReqT>           the request type for this method
     * @param <ResT>           the response type for this method
     */
    default <ReqT, ResT> void assertFaultPropagates(
            MethodDescriptor<ReqT, ResT> methodDescriptor
    ) {
        if (isInsideOfFailureBlock()) {
            faultKeysThatPropagate.add(new SingleFaultKey<>(methodDescriptor));
        } else {
            throw new FilibusterGrpcAssertionUsedOutsideFailureBlockException("assertFaultPropagates");
        }
    }

    // *****************************************************************************************************************
    // Fault API: specify faults that have no impact.
    // *****************************************************************************************************************

    List<FaultKey> faultKeysWithNoImpact = new ArrayList<>();

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC method will result
     * in the primary assertions, placed in the {@link #assertTestBlock()} continuing to hold true.
     * Therefore, it has no effect on the test outcome when the fault is injected
     * (except changing the stub invocation counts, which will be automatically adjusted
     * when a fault is injected if the developer used the Filibuster-provided
     * {@link GrpcMock#stubFor(MethodDescriptor, Object, Object) GrpcMock.stubFor} and
     * {@link GrpcMock#verifyThat(MethodDescriptor, int) GrpcMock.verifyThat} methods.)
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param <ReqT>           the request type for this method
     * @param <ResT>           the response type for this method
     */
    default <ReqT, ResT> void assertFaultHasNoImpact(
            MethodDescriptor<ReqT, ResT> methodDescriptor
    ) {
        if (isInsideOfFailureBlock()) {
            faultKeysWithNoImpact.add(new SingleFaultKey<>(methodDescriptor));
        } else {
            throw new FilibusterGrpcAssertionUsedOutsideFailureBlockException("assertFaultHasNoImpact");
        }
    }

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC method
     * with this status code will result in the primary assertions, placed in the {@link #assertTestBlock()}
     * continuing to hold true.
     * Therefore, it has no effect on the test outcome when the fault is injected
     * (except changing the stub invocation counts, which will be automatically adjusted
     * when a fault is injected if the developer used the Filibuster-provided
     * {@link GrpcMock#stubFor(MethodDescriptor, Object, Object) GrpcMock.stubFor} and
     * {@link GrpcMock#verifyThat(MethodDescriptor, int) GrpcMock.verifyThat} methods.)
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param code             the status code
     * @param <ReqT>           the request type for this method
     * @param <ResT>           the response type for this method
     */
    default <ReqT, ResT> void assertFaultHasNoImpact(
            MethodDescriptor<ReqT, ResT> methodDescriptor,
            Status.Code code
    ) {
        if (isInsideOfFailureBlock()) {
            faultKeysWithNoImpact.add(new SingleFaultKey<>(methodDescriptor, code));
        } else {
            throw new FilibusterGrpcAssertionUsedOutsideFailureBlockException("assertFaultHasNoImpact");
        }
    }

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC method
     * with this request will result in the primary assertions, placed in the {@link #assertTestBlock()}
     * continuing to hold true.
     * Therefore, it has no effect on the test outcome when the fault is injected
     * (except changing the stub invocation counts, which will be automatically adjusted
     * when a fault is injected if the developer used the Filibuster-provided
     * {@link GrpcMock#stubFor(MethodDescriptor, Object, Object) GrpcMock.stubFor} and
     * {@link GrpcMock#verifyThat(MethodDescriptor, int) GrpcMock.verifyThat} methods.)
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param request          the request
     * @param <ReqT>           the request type for this method
     * @param <ResT>           the response type for this method
     */
    default <ReqT, ResT> void assertFaultHasNoImpact(
            MethodDescriptor<ReqT, ResT> methodDescriptor,
            ReqT request
    ) {
        if (isInsideOfFailureBlock()) {
            faultKeysWithNoImpact.add(new SingleFaultKey<>(methodDescriptor, request));
        } else {
            throw new FilibusterGrpcAssertionUsedOutsideFailureBlockException("assertFaultHasNoImpact");
        }
    }

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC method
     * with this status code and request will result in the primary assertions, placed in
     * the {@link #assertTestBlock()} continuing to hold true.
     * Therefore, it has no effect on the test outcome when the fault is injected
     * (except changing the stub invocation counts, which will be automatically adjusted
     * when a fault is injected if the developer used the Filibuster-provided
     * {@link GrpcMock#stubFor(MethodDescriptor, Object, Object) GrpcMock.stubFor} and
     * {@link GrpcMock#verifyThat(MethodDescriptor, int) GrpcMock.verifyThat} methods.)
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param code             the status code
     * @param request          the request
     * @param <ReqT>           the request type for this method
     * @param <ResT>           the response type for this method
     */
    default <ReqT, ResT> void assertFaultHasNoImpact(
            MethodDescriptor<ReqT, ResT> methodDescriptor,
            Status.Code code,
            ReqT request
    ) {
        if (isInsideOfFailureBlock()) {
            faultKeysWithNoImpact.add(new SingleFaultKey<>(methodDescriptor, code, request));
        } else {
            throw new FilibusterGrpcAssertionUsedOutsideFailureBlockException("assertFaultHasNoImpact");
        }
    }

    // *****************************************************************************************************************
    // Fault API: specify faults that contain different assertions.
    // *****************************************************************************************************************

    HashMap<FaultKey, Runnable> assertionsByFaultKey = new HashMap<>();

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC endpoint will result in possibly
     * different assertions being true (other than the default block.)  These assertions should be placed in the
     * associated {@link Runnable}.
     * This block will replace the assertions in {@link #assertTestBlock()}.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param runnable         assertion block
     * @param <ReqT>           the request type for this method
     * @param <ResT>           the response type for this method
     */
    default <ReqT, ResT> void assertOnFault(
            MethodDescriptor<ReqT, ResT> methodDescriptor,
            Runnable runnable
    ) {
        if (isInsideOfFailureBlock()) {
            assertionsByFaultKey.put(new SingleFaultKey<>(methodDescriptor), runnable);
        } else {
            throw new FilibusterGrpcAssertionUsedOutsideFailureBlockException("assertOnFault");
        }
    }

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC endpoint will result in possibly
     * different assertions being true (other than the default block.)  These assertions should be placed in the
     * associated {@link Runnable}.
     * This block will replace the assertions in {@link #assertTestBlock()}.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param code             the status code of the injected fault
     * @param runnable         assertion block
     * @param <ReqT>           the request type for this method
     * @param <ResT>           the response type for this method
     */
    default <ReqT, ResT> void assertOnFault(
            MethodDescriptor<ReqT, ResT> methodDescriptor,
            Status.Code code,
            Runnable runnable
    ) {
        if (isInsideOfFailureBlock()) {
            assertionsByFaultKey.put(new SingleFaultKey<>(methodDescriptor, code), runnable);
        } else {
            throw new FilibusterGrpcAssertionUsedOutsideFailureBlockException("assertOnFault");
        }
    }

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC endpoint will result in possibly
     * different assertions being true (other than the default block.)  These assertions should be placed in the
     * associated {@link Runnable}.
     * This block will replace the assertions in {@link #assertTestBlock()}.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param request          the request that the fault was injected on
     * @param runnable         assertion block
     * @param <ReqT>           the request type for this method
     * @param <ResT>           the response type for this method
     */
    default <ReqT, ResT> void assertOnFault(
            MethodDescriptor<ReqT, ResT> methodDescriptor,
            ReqT request,
            Runnable runnable
    ) {
        if (isInsideOfFailureBlock()) {
            assertionsByFaultKey.put(new SingleFaultKey<>(methodDescriptor, request), runnable);
        } else {
            throw new FilibusterGrpcAssertionUsedOutsideFailureBlockException("assertOnFault");
        }
    }

    /**
     * Use of this method informs Filibuster that any faults injected to this GRPC endpoint will result in possibly
     * different assertions being true (other than the default block.)  These assertions should be placed in the
     * associated {@link Runnable}.
     * This block will replace the assertions in {@link #assertTestBlock()}.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param code             the status code of the injected fault
     * @param request          the request that the fault was injected on
     * @param runnable         assertion block
     * @param <ReqT>           the request type for this method
     * @param <ResT>           the response type for this method
     */
    default <ReqT, ResT> void assertOnFault(
            MethodDescriptor<ReqT, ResT> methodDescriptor,
            Status.Code code,
            ReqT request,
            Runnable runnable
    ) {
        if (isInsideOfFailureBlock()) {
            assertionsByFaultKey.put(new SingleFaultKey<>(methodDescriptor, code, request), runnable);
        } else {
            throw new FilibusterGrpcAssertionUsedOutsideFailureBlockException("assertOnFault");
        }
    }

    /**
     * Use of this method informs Filibuster that these combined faults will result in possibly
     * different assertions being true (other than the default block.)  These assertions should be placed in the
     * associated {@link Runnable}.
     * This block will replace the assertions in {@link #assertTestBlock()}.
     * *
     *
     * @param compositeFaultSpecification {@link CompositeFaultSpecification}
     * @param runnable                    assertion block
     */
    default void assertOnFault(CompositeFaultSpecification compositeFaultSpecification, Runnable runnable) {
        if (isInsideOfFailureBlock()) {
            assertionsByFaultKey.put(new CompositeFaultKey(compositeFaultSpecification), runnable);
        } else {
            throw new FilibusterGrpcAssertionUsedOutsideFailureBlockException("assertOnFault");
        }
    }

    // *****************************************************************************************************************
    // Fault API: adjusted expectations for GRPC endpoints.
    // *****************************************************************************************************************

    /**
     * Indicates that this RPC has no side effects and therefore may be called 0 or more times.
     * Can only be used inside an {@link #assertOnException assertOnException} block.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param <ReqT>           the request type for this method
     * @param <ResT>           the response type for this method
     */
    default <ReqT, ResT> void readOnlyRPC(MethodDescriptor<ReqT, ResT> methodDescriptor) {
        if (isInsideOfAssertOnExceptionBlock() || isInsideOfAssertOnFaultBlock()) {
            GrpcMock.adjustExpectation(methodDescriptor, -1);
        } else {
            throw new FilibusterGrpcReadOnlyRPCUsedOutsideAssertOnExceptionAndAssertOnFaultException();
        }
    }

    /**
     * Indicates that this RPC has no side effects and therefore may be called 0 or more times.
     * Can only be used inside an {@link #assertOnException assertOnException} block.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param request          the request
     * @param <ReqT>           the request type for this method
     * @param <ResT>           the response type for this method
     */
    default <ReqT, ResT> void readOnlyRPC(MethodDescriptor<ReqT, ResT> methodDescriptor, ReqT request) {
        if (isInsideOfAssertOnExceptionBlock() || isInsideOfAssertOnFaultBlock()) {
            GrpcMock.adjustExpectation(methodDescriptor, request, -1);
        } else {
            throw new FilibusterGrpcReadOnlyRPCUsedOutsideAssertOnExceptionAndAssertOnFaultException();
        }
    }

    /**
     * Indicates that an RPC has side effects and therefore needs explicit invocation counts.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param count            the number of times invoked
     * @param <ReqT>           the request type for this method
     * @param <ResT>           the response type for this method
     */
    default <ReqT, ResT> void sideEffectingRPC(MethodDescriptor<ReqT, ResT> methodDescriptor, int count) {
        if (isInsideOfAssertOnExceptionBlock() || isInsideOfAssertOnFaultBlock()) {
            GrpcMock.adjustExpectation(methodDescriptor, count);
        } else {
            throw new FilibusterGrpcSideEffectingRPCUsedOutsideAssertOnExceptionAndAssertOnFaultException();
        }
    }

    /**
     * Indicates that an RPC has side effects and therefore needs explicit invocation counts.
     *
     * @param methodDescriptor a GRPC method descriptor
     * @param request          the request
     * @param count            the number of times invoked
     * @param <ReqT>           the request type for this method
     * @param <ResT>           the response type for this method
     */
    default <ReqT, ResT> void sideEffectingRPC(MethodDescriptor<ReqT, ResT> methodDescriptor, ReqT request, int count) {
        if (isInsideOfAssertOnExceptionBlock() || isInsideOfAssertOnFaultBlock()) {
            GrpcMock.adjustExpectation(methodDescriptor, request, count);
        } else {
            throw new FilibusterGrpcSideEffectingRPCUsedOutsideAssertOnExceptionAndAssertOnFaultException();
        }
    }

    // *****************************************************************************************************************
    // Internal
    // *****************************************************************************************************************

    default List<JSONObject> rpcsWhereFaultsInjected() {
        List<JSONObject> rpcsWhereFaultsInjected = new ArrayList<>();

        // Look up the RPCs that were executed and the faults that were injected.
        HashMap<DistributedExecutionIndex, JSONObject> executedRPCs = getExecutedRPCs();

        if (executedRPCs == null) {
            throw new FilibusterGrpcTestInternalRuntimeException("executedRPCs is null: this could indicate a problem!");
        }

        HashMap<DistributedExecutionIndex, JSONObject> faultsInjected = getFaultsInjected();

        if (faultsInjected == null) {
            throw new FilibusterGrpcTestInternalRuntimeException("faultsInjected is null: this could indicate a problem!");
        }

        for (Map.Entry<DistributedExecutionIndex, JSONObject> executedRPC : executedRPCs.entrySet()) {
            if (faultsInjected.containsKey(executedRPC.getKey())) {
                JSONObject faultInjected = faultsInjected.get(executedRPC.getKey());
                JSONObject finalExecutedRPC = executedRPC.getValue();
                if (faultInjected.has("forced_exception")) {
                    JSONObject forcedExceptionObject = faultInjected.getJSONObject("forced_exception");
                    finalExecutedRPC.put("forced_exception", forcedExceptionObject);
                }
                rpcsWhereFaultsInjected.add(finalExecutedRPC);
            }
        }

        return rpcsWhereFaultsInjected;
    }

    default boolean performSingleFaultChecking(JSONObject rpcWhereFaultInjected) {
        boolean shouldRunAssertionBlock = true;
        boolean searchComplete = false;

        // Find matching keys.
        for (FaultKey faultKey : SingleFaultKey.generateFaultKeysInDecreasingGranularity(rpcWhereFaultInjected)) {
            if (!searchComplete) {
                // Iterate each key and see if the user specified something for it.
                if (assertionsByFaultKey.containsKey(faultKey)) {
                    // We want to exit on the first find.
                    searchComplete = true;

                    // As long as we have a match, we skip the normal assertions.
                    shouldRunAssertionBlock = false;

                    // Try to run the runnable and abort if it throws.
                    try {
                        // Run the updated assertions.
                        Runnable runnable = assertionsByFaultKey.get(faultKey);

                        if (runnable != null) {
                            setInsideOfAssertOnFaultBlock(true);
                            runnable.run();
                        } else {
                            throw new FilibusterGrpcTestInternalRuntimeException("runnable is null: this could indicate a problem!");
                        }
                    } catch (Throwable t) {
                        throw new FilibusterGrpcAssertOnFaultException(t);
                    } finally {
                        setInsideOfAssertOnFaultBlock(false);
                    }
                }

                // Otherwise, find out if we should ignore the fault.
                if (faultKeysWithNoImpact.contains(faultKey)) {
                    searchComplete = true;
                    shouldRunAssertionBlock = true;
                }
            }
        }

        if (!searchComplete) {
            throw new FilibusterGrpcInjectedFaultHasUnspecifiedFailureBehaviorException();
        }

        return shouldRunAssertionBlock;
    }

    default boolean performMultipleFaultChecking(List<JSONObject> rpcsWhereFaultsInjected) {
        // Try to see if the user said something about this particular set of failures.
        FaultKey faultKey = findMatchingFaultKey(assertionsByFaultKey, rpcsWhereFaultsInjected);

        if (faultKey != null) {
            // Try to run the runnable and abort if it throws.
            try {
                // Run the updated assertions.
                Runnable runnable = assertionsByFaultKey.get(faultKey);

                if (runnable != null) {
                    setInsideOfAssertOnFaultBlock(true);
                    runnable.run();
                } else {
                    throw new FilibusterGrpcTestInternalRuntimeException("runnable is null: this could indicate a problem!");
                }
            } catch (Throwable t) {
                throw new FilibusterGrpcAssertOnFaultException(t);
            } finally {
                setInsideOfAssertOnFaultBlock(true);
            }

            // Don't run the normal assertions.
            return false;
        }

        // Otherwise, try to verify compositional-ly.
        Set<JSONObject> methodsWithFaultImpact = new HashSet<>();

        for (JSONObject rpcWhereFaultInjected : rpcsWhereFaultsInjected) {
            boolean found = false;

            for (FaultKey singleFaultKey : SingleFaultKey.generateFaultKeysInDecreasingGranularity(rpcWhereFaultInjected)) {
                if (faultKeysWithNoImpact.contains(singleFaultKey)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                methodsWithFaultImpact.add(rpcWhereFaultInjected);
            }
        }

        if (methodsWithFaultImpact.size() == 0) {
            return true;
        } else if (methodsWithFaultImpact.size() == 1) {
            List<JSONObject> rpcsWhereFaultsInjectedWithImpact = new ArrayList<>(methodsWithFaultImpact);
            return performSingleFaultChecking(rpcsWhereFaultsInjectedWithImpact.get(0));
        } else if (methodsWithFaultImpact.size() < rpcsWhereFaultsInjected.size()) {
            List<JSONObject> rpcsWhereFaultsInjectedWithImpact = new ArrayList<>(methodsWithFaultImpact);
            return performMultipleFaultChecking(rpcsWhereFaultsInjectedWithImpact);
        } else {
            throw new FilibusterGrpcAmbiguousFailureHandlingException();
        }
    }

    default void execute() {
        // For each test execution, clear out the adjusted test expectations.
        GrpcMock.resetAdjustedExpectations();

        // For each test execution, clear out verifyThat mapping.
        GrpcMock.resetVerifyThatMapping();

        // Clear out any user-provided failure handling logic before starting the next execution,
        // as we will set all of this up again when we execute the failureBlock().
        response.set(null);
        errorAssertions.clear();
        faultKeysThatThrow.clear();
        faultKeysThatPropagate.clear();
        faultKeysWithNoImpact.clear();
        assertionsByFaultKey.clear();

        // Execute setup blocks.
        Helpers.setupBlock(this::setupBlock);

        // Stub downstream dependencies.
        setInsideOfStubBlock(true);
        Helpers.setupBlock(this::stubBlock);
        setInsideOfStubBlock(false);

        // Execute failure block.
        setInsideOfFailureBlock(true);
        Helpers.setupBlock(this::failureBlock);
        setInsideOfFailureBlock(false);

        try {
            // Execute the test.
            Helpers.testBlock(this::executeTestBlock);

            // If a fault was injected, ask the developer to specify an alternative assertion block.
            boolean shouldRunAssertionBlock = true;
            List<JSONObject> rpcsWhereFaultsInjected = rpcsWhereFaultsInjected();

            if (rpcsWhereFaultsInjected == null) {
                throw new FilibusterGrpcTestInternalRuntimeException("rpcsWhereFaultsInjected is null: this could indicate a problem!");
            }

            if (rpcsWhereFaultsInjected.size() > 0) {
                if (rpcsWhereFaultsInjected.size() > 1) {
                    shouldRunAssertionBlock = performMultipleFaultChecking(rpcsWhereFaultsInjected);
                } else {
                    shouldRunAssertionBlock = performSingleFaultChecking(rpcsWhereFaultsInjected.get(0));
                }
            }

            if (shouldRunAssertionBlock) {
                // Only in the reference execution, execute the assertion block.
                // This assumes that the assertion block will not pass if the request returns error.
                try {
                    Helpers.assertionBlock(this::assertTestBlock);
                } catch (Throwable t) {
                    if (rpcsWhereFaultsInjected.size() > 1) {
                        throw new FilibusterGrpcMultipleFaultsInjectedException(t);
                    } else {
                        throw new FilibusterGrpcAssertTestBlockFailedException(t);
                    }
                }
            }

            // Verify stub invocations.
            setInsideOfAssertStubBlock(true);
            Helpers.assertionBlock(this::assertStubBlock);
        } catch (StatusRuntimeException statusRuntimeException) {
            // Look up the faults that were injected.
            List<JSONObject> rpcsWhereFaultsInjected = rpcsWhereFaultsInjected();

            if (rpcsWhereFaultsInjected == null) {
                throw new FilibusterGrpcTestInternalRuntimeException("rpcsWhereFaultsInjected is null: this could indicate a problem!");
            }

            // If we're in the reference execution, just rethrow.
            if (rpcsWhereFaultsInjected.size() == 0) {
                throw statusRuntimeException;
            }

            // If we are not in the reference execution.
            if (rpcsWhereFaultsInjected.size() > 1) {
                performMultipleExceptionChecking(rpcsWhereFaultsInjected, statusRuntimeException);
            } else {
                // Get the only fault injected.
                JSONObject rpcWhereFaultInjected = rpcsWhereFaultsInjected.get(0);

                // Get actual status.
                Status actualStatus = statusRuntimeException.getStatus();

                // Did the user indicate propagation of faults?
                FaultKey faultKeyIndicatingPropagationOfFaults = didUserIndicatePropagationOfFault(rpcWhereFaultInjected);

                if (faultKeyIndicatingPropagationOfFaults != null) {
                    validatePropagationOfFault(rpcWhereFaultInjected, actualStatus);
                }

                // Did the user indicate that this fault will result in exception?
                List<FaultKey> faultKeysIndicatingThrownExceptionFromFault = didUserIndicateThrownExceptionForFault(rpcWhereFaultInjected);

                if (faultKeysIndicatingThrownExceptionFromFault.size() > 0) {
                    validateThrownException(faultKeysIndicatingThrownExceptionFromFault, statusRuntimeException);
                }

                if (faultKeyIndicatingPropagationOfFaults == null && faultKeysIndicatingThrownExceptionFromFault.size() == 0) {
                    throw new FilibusterGrpcThrownExceptionHasUnspecifiedFailureBehaviorException(statusRuntimeException);
                }

                if (faultKeyIndicatingPropagationOfFaults != null && faultKeysIndicatingThrownExceptionFromFault.size() > 0) {
                    throw new FilibusterGrpcAmbiguousThrowAndErrorPropagationException();
                }

                // Verify that we have assertion block for thrown exception.
                if (!errorAssertions.containsKey(statusRuntimeException.getStatus().getCode())) {
                    throw new FilibusterGrpcMissingAssertionForStatusCodeException(actualStatus.getCode());
                }

                // Verify that assertion block runs successfully.
                for (Map.Entry<Status.Code, Runnable> errorAssertion : errorAssertions.entrySet()) {
                    if (errorAssertion.getKey().equals(statusRuntimeException.getStatus().getCode())) {
                        try {
                            setInsideOfAssertOnExceptionBlock(true);
                            errorAssertion.getValue().run();
                        } catch (Throwable t) {
                            throw new FilibusterGrpcAssertionsForAssertOnExceptionFailedException(actualStatus.getCode(), t);
                        } finally {
                            setInsideOfAssertOnExceptionBlock(false);
                        }
                    }
                }

                // Verify stub invocations.
                try {
                    setInsideOfAssertStubBlock(true);
                    Helpers.assertionBlock(this::assertStubBlock);
                } catch (Throwable t) {
                    throw new FilibusterGrpcAssertionsDidNotHoldUnderErrorResponseException(actualStatus.getCode(), t);
                } finally {
                    setInsideOfAssertStubBlock(false);
                }
                performSingleExceptionChecking(rpcsWhereFaultsInjected.get(0), statusRuntimeException);
            }
        } finally {
            setInsideOfAssertStubBlock(false);
            // Execute teardown.
            Helpers.teardownBlock(this::teardownBlock);
        }

        // Fail the test if any RPCs were left UNIMPLEMENTED (and, we didn't inject it!)
        HashMap<DistributedExecutionIndex, JSONObject> failedRPCs = getFailedRPCs();

        if (failedRPCs == null) {
            throw new FilibusterGrpcTestInternalRuntimeException("failedRPCs is null: this could indicate a problem!");
        }

        HashMap<DistributedExecutionIndex, JSONObject> faultsInjected = getFaultsInjected();

        if (faultsInjected == null) {
            throw new FilibusterGrpcTestInternalRuntimeException("faultsInjected is null: this could indicate a problem!");
        }

        for (Map.Entry<DistributedExecutionIndex, JSONObject> failedRPC : failedRPCs.entrySet()) {
            DistributedExecutionIndex distributedExecutionIndex = failedRPC.getKey();
            JSONObject jsonObject = failedRPC.getValue();

            Status.Code statusCode = getStatusCode("exception", jsonObject);

            if (statusCode == null) {
                throw new FilibusterGrpcTestInternalRuntimeException("statusCode is null: this could indicate a problem!");
            }

            String code = statusCode.toString();

            if (code.equals("UNIMPLEMENTED")) {
                boolean faultInjected = faultsInjected.containsKey(distributedExecutionIndex);

                if (!faultInjected) {
                    throw new FilibusterGrpcInvokedRPCUnimplementedException();
                }
            }
        }

        // Fail the test if something hasn't had a verifyThat called on it.
        for (Map.Entry<String, Boolean> verifyThat : GrpcMock.getVerifyThatMapping().entrySet()) {
            if (!verifyThat.getValue()) {
                throw new FilibusterGrpcStubbedRPCHasNoAssertionsException(verifyThat.getKey());
            }
        }
    }

    @Nullable
    default Status.Code getStatusCode(String exceptionFieldName, JSONObject jsonObject) {
        if (jsonObject.has(exceptionFieldName)) {
            JSONObject exceptionJsonObject = jsonObject.getJSONObject(exceptionFieldName);

            if (exceptionJsonObject.has("metadata")) {
                JSONObject metadataExceptionJsonObject = exceptionJsonObject.getJSONObject("metadata");

                if (metadataExceptionJsonObject.has("code")) {
                    return Status.Code.valueOf(metadataExceptionJsonObject.getString("code"));
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    default FaultKey didUserIndicatePropagationOfFault(JSONObject rpcWhereFaultInjected) {
        FaultKey foundFaultKey = null;

        for (FaultKey faultKey : SingleFaultKey.generateFaultKeysInDecreasingGranularity(rpcWhereFaultInjected)) {
            if (faultKeysThatPropagate.contains(faultKey)) {
                foundFaultKey = faultKey;
                break;
            }
        }

        return foundFaultKey;
    }

    default List<FaultKey> didUserIndicateThrownExceptionForFault(JSONObject rpcWhereFaultInjected) {
        List<FaultKey> matchingFaultKeys = new ArrayList<>();

        for (FaultKey faultKey : SingleFaultKey.generateFaultKeysInDecreasingGranularity(rpcWhereFaultInjected)) {
            if (faultKeysThatThrow.containsKey(faultKey)) {
                matchingFaultKeys.add(faultKey);
                break;
            }
        }

        return matchingFaultKeys;
    }

    default void validatePropagationOfFault(JSONObject rpcWhereFaultInjected, Status actualStatus) {
        Status.Code injectedFaultStatusCode = getStatusCode("forced_exception", rpcWhereFaultInjected);

        if (injectedFaultStatusCode != null) {
            if (!actualStatus.getCode().equals(injectedFaultStatusCode)) {
                throw new FilibusterGrpcSuppressedStatusCodeException();
            }
        } else {
            throw new FilibusterGrpcTestInternalRuntimeException("injectedFaultStatusCode is null; this could indicate a problem!");
        }
    }

    default void validateThrownException(List<FaultKey> matchingFaultKeys, StatusRuntimeException statusRuntimeException) {
        // Get status.
        Status actualStatus = statusRuntimeException.getStatus();

        // Find a status that matches the error code and description.
        boolean foundMatchingExpectedStatus = false;

        for (FaultKey matchingFaultKey : matchingFaultKeys) {
            Map.Entry<Status.Code, String> expectedException = faultKeysThatThrow.get(matchingFaultKey);

            if (expectedException == null) {
                throw new FilibusterGrpcTestInternalRuntimeException("expectedException is null; this could indicate a problem!");
            }

            Status expectedStatus = Status.fromCode(expectedException.getKey()).withDescription(expectedException.getValue());
            boolean codeMatches = expectedStatus.getCode().equals(actualStatus.getCode());
            boolean descriptionMatches = Objects.equals(expectedStatus.getDescription(), actualStatus.getDescription());

            if (codeMatches && descriptionMatches) {
                foundMatchingExpectedStatus = true;
                break;
            }
        }

        // If not found, throw error.
        if (!foundMatchingExpectedStatus) {
            throw new FilibusterGrpcFailedRPCException(statusRuntimeException);
        }
    }

    default void performMultipleExceptionChecking(List<JSONObject> rpcsWhereFaultsInjected, StatusRuntimeException statusRuntimeException) {
        // Get actual status.
        Status actualStatus = statusRuntimeException.getStatus();

        // Try to see if the user said something about this particular set of failures.
        List<FaultKey> faultKeysIndicatingThrownExceptionFromFault = findMatchingFaultKeys(faultKeysThatThrow, rpcsWhereFaultsInjected, actualStatus.getCode());

        if (faultKeysIndicatingThrownExceptionFromFault == null) {
            throw new FilibusterGrpcTestInternalRuntimeException("faultKeysIndicatingThrownExceptionFromFault is null: this could indicate a problem!");
        }

        if (faultKeysIndicatingThrownExceptionFromFault.size() > 0) {
            validateThrownException(faultKeysIndicatingThrownExceptionFromFault, statusRuntimeException);
            verifyAssertionBlockForThrownException(statusRuntimeException);
            return;
        }

        // Otherwise, try to verify compositional-ly.
        Set<JSONObject> methodsWithFaultImpact = new HashSet<>();

        for (JSONObject rpcWhereFaultInjected : rpcsWhereFaultsInjected) {
            boolean found = false;

            for (FaultKey singleFaultKey : SingleFaultKey.generateFaultKeysInDecreasingGranularity(rpcWhereFaultInjected)) {
                if (faultKeysWithNoImpact.contains(singleFaultKey)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                methodsWithFaultImpact.add(rpcWhereFaultInjected);
            }
        }

        if (methodsWithFaultImpact.size() == 0) {
            return;
        } else if (methodsWithFaultImpact.size() == 1) {
            List<JSONObject> rpcsWhereFaultsInjectedWithImpact = new ArrayList<>(methodsWithFaultImpact);
            performSingleExceptionChecking(rpcsWhereFaultsInjectedWithImpact.get(0), statusRuntimeException);
        } else if (methodsWithFaultImpact.size() < rpcsWhereFaultsInjected.size()) {
            List<JSONObject> rpcsWhereFaultsInjectedWithImpact = new ArrayList<>(methodsWithFaultImpact);
            performMultipleExceptionChecking(rpcsWhereFaultsInjectedWithImpact, statusRuntimeException);
        } else {
            throw new FilibusterGrpcAmbiguousFailureHandlingException(statusRuntimeException);
        }
    }

    default void performSingleExceptionChecking(JSONObject rpcWhereFaultInjected, StatusRuntimeException statusRuntimeException) {
        // Get actual status.
        Status actualStatus = statusRuntimeException.getStatus();

        // Did the user indicate propagation of faults?
        FaultKey faultKeyIndicatingPropagationOfFaults = didUserIndicatePropagationOfFault(rpcWhereFaultInjected);

        if (faultKeyIndicatingPropagationOfFaults != null) {
            validatePropagationOfFault(rpcWhereFaultInjected, actualStatus);
        }

        // Did the user indicate that this fault will result in exception?
        List<FaultKey> faultKeysIndicatingThrownExceptionFromFault = didUserIndicateThrownExceptionForFault(rpcWhereFaultInjected);

        if (faultKeysIndicatingThrownExceptionFromFault.size() > 0) {
            validateThrownException(faultKeysIndicatingThrownExceptionFromFault, statusRuntimeException);
        }

        if (faultKeyIndicatingPropagationOfFaults == null && faultKeysIndicatingThrownExceptionFromFault.size() == 0) {
            throw new FilibusterGrpcThrownExceptionHasUnspecifiedFailureBehaviorException(statusRuntimeException);
        }

        if (faultKeyIndicatingPropagationOfFaults != null && faultKeysIndicatingThrownExceptionFromFault.size() > 0) {
            throw new FilibusterGrpcAmbiguousThrowAndErrorPropagationException();
        }

        // Verify that we have assertion block for thrown exception.
        verifyAssertionBlockForThrownException(statusRuntimeException);
    }

    default void verifyAssertionBlockForThrownException(StatusRuntimeException statusRuntimeException) {
        // Get actual status.
        Status actualStatus = statusRuntimeException.getStatus();

        if (!errorAssertions.containsKey(statusRuntimeException.getStatus().getCode())) {
            throw new FilibusterGrpcMissingAssertionForStatusCodeException(actualStatus.getCode());
        }

        // Verify that assertion block runs successfully.
        for (Map.Entry<Status.Code, Runnable> errorAssertion : errorAssertions.entrySet()) {
            if (errorAssertion.getKey().equals(statusRuntimeException.getStatus().getCode())) {
                try {
                    setInsideOfAssertOnExceptionBlock(true);
                    errorAssertion.getValue().run();
                } catch (Throwable t) {
                    throw new FilibusterGrpcAssertionsForAssertOnExceptionFailedException(actualStatus.getCode(), t);
                } finally {
                    setInsideOfAssertOnExceptionBlock(false);
                }
            }
        }

        // Verify stub invocations.
        try {
            setInsideOfAssertStubBlock(true);
            Helpers.assertionBlock(this::assertStubBlock);
        } catch (Throwable t) {
            throw new FilibusterGrpcAssertionsDidNotHoldUnderErrorResponseException(actualStatus.getCode(), t);
        } finally {
            setInsideOfAssertStubBlock(false);
        }
    }
}
