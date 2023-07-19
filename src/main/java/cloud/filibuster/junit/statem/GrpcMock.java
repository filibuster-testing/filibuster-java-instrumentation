package cloud.filibuster.junit.statem;

import cloud.filibuster.junit.Assertions;
import io.grpc.MethodDescriptor;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

import static org.grpcmock.GrpcMock.calledMethod;
import static org.grpcmock.GrpcMock.times;
import static org.grpcmock.GrpcMock.atLeast;
import static org.grpcmock.GrpcMock.unaryMethod;

/**
 * Filibuster wrapper for {@link org.grpcmock.GrpcMock Grpc} with a slightly different interface to allow
 * for dynamically adjusting mocks when faults are injected. Designed for use with {@link FilibusterGrpcTest} and
 * will not work in isolation (at the moment!)
 */
public class GrpcMock {
    private static HashMap<String, Integer> adjustedExpectationsForMethods = new HashMap<>();

    private static HashMap<String, Integer> adjustedExpectationsForRequests = new HashMap<>();

    /**
     * Clear out expectations for stub invocations made using {@link #adjustExpectation(MethodDescriptor, int)}
     * or {@link #adjustExpectation(MethodDescriptor, Object, int) adjustExpectation(MethodDescriptor, ReqT, int)}.
     * Called implicitly by {@link FilibusterGrpcTest#execute()}.
     */
    public static void resetAdjustedExpectations() {
        adjustedExpectationsForMethods = new HashMap<>();
        adjustedExpectationsForRequests = new HashMap<>();
    }

    /**
     * Adjust the expected number of invocations specified using {@link #verifyThat(MethodDescriptor, int) verifyThat}.
     *
     * @param method the GRPC method descriptor
     * @param count the number of expected invocations
     *
     */
    public static <ReqT> void adjustExpectation(
            @Nonnull MethodDescriptor<ReqT, ?> method,
            int count
    ) {
        adjustedExpectationsForMethods.put(method.getFullMethodName(), count);
    }

    /**
     * Adjust the expected number of invocations specified using {@link #verifyThat(MethodDescriptor, Object, int) verifyThat}.
     *
     * @param method the GRPC method descriptor
     * @param request the request
     * @param count the number of expected invocations
     * @param <ReqT> the request type
     *
     */
    public static <ReqT> void adjustExpectation(
            @Nonnull MethodDescriptor<ReqT, ?> method,
            @Nonnull ReqT request,
            int count
    ) {
        adjustedExpectationsForRequests.put(method.getFullMethodName() + request, count);
    }

    private static HashMap<String, Boolean> verifyThatMapping = new HashMap<>();

    /**
     * Return the mapping containing whether all stubs have had either {@link #verifyThat(MethodDescriptor, int)} or
     * {@link #verifyThat(MethodDescriptor, Object, int) verifyThat(MethodDescriptor, ReqT, int)} invoked on them.
     *
     * @return {@link #verifyThatMapping}
     *
     */
    public static HashMap<String, Boolean> getVerifyThatMapping() {
        return verifyThatMapping;
    }

    /**
     * Clear out expectations for stub invocations made using {@link #verifyThat(MethodDescriptor, int)} or
     * {@link #verifyThat(MethodDescriptor, Object, int) verifyThat(MethodDescriptor, ReqT, int)}.
     * Called implicitly by {@link FilibusterGrpcTest#execute()}.
     */
    public static void resetVerifyThatMapping() {
        verifyThatMapping = new HashMap<>();
    }

    /**
     * Stub a GRPC method with a given request providing a particular response.
     *
     * @param method the GRPC method descriptor
     * @param request the request
     * @param response the response
     * @param <ReqT> the request type
     * @param <RespT> the response type
     *
     */
    public static <ReqT, RespT> void stubFor(
            @Nonnull MethodDescriptor<ReqT, RespT> method,
            @Nonnull ReqT request,
            @Nonnull RespT response
            ) {
        verifyThatMapping.put(method.getFullMethodName(), false);

        org.grpcmock.GrpcMock.stubFor(unaryMethod(method).withRequest(request).willReturn(response));
    }

    /**
     * Set an expectation that a stub will be invoked a particular number of times.
     * This function prohibits the developer stating "any times" and requires a precise invocation count.
     *
     * @param method the GRPC method descriptor
     * @param count the number of expected invocations
     * @param <ReqT> the request type
     *
     */
    public static <ReqT> void verifyThat(
            @Nonnull MethodDescriptor<ReqT, ?> method,
            int count
    ) {
        verifyThatMapping.put(method.getFullMethodName(), true);

        if (Assertions.wasFaultInjectedOnMethod(method.getFullMethodName())) {
            if (count > 0) {
                count = count - 1;
            }
        }

        if (adjustedExpectationsForMethods.containsKey(method.getFullMethodName())) {
            count = adjustedExpectationsForMethods.get(method.getFullMethodName());
        }

        if (count == -1) {
            org.grpcmock.GrpcMock.verifyThat(calledMethod(method), atLeast(0));
        } else {
            org.grpcmock.GrpcMock.verifyThat(calledMethod(method), times(count));
        }
    }

    /**
     * Set an expectation that a stub will be invoked, with a given request, a particular number of times.
     * This function prohibits the developer stating "any times" and requires a precise invocation count.
     *
     * @param method the GRPC method descriptor
     * @param request the request
     * @param count the number of expected invocations
     * @param <ReqT> the request type
     *
     */
    public static <ReqT> void verifyThat(
            @Nonnull MethodDescriptor<ReqT, ?> method,
            @Nonnull ReqT request,
            int count
    ) {
        verifyThatMapping.put(method.getFullMethodName(), true);

        if (Assertions.wasFaultInjectedOnRequest(request.toString())) {
            if (count > 0) {
                count = count - 1;
            }
        }

        for (Map.Entry<String, Integer> adjustedExpectationsForRequest : adjustedExpectationsForRequests.entrySet()){
            if (adjustedExpectationsForRequest.getKey().equals(method.getFullMethodName() + request)) {
                count = adjustedExpectationsForRequest.getValue();
            }
        }

        if (count == -1) {
            org.grpcmock.GrpcMock.verifyThat(calledMethod(method).withRequest(request), atLeast(0));
        } else {
            org.grpcmock.GrpcMock.verifyThat(calledMethod(method).withRequest(request), times(count));
        }
    }
}
