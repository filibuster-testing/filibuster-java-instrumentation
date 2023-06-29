package cloud.filibuster.junit.statem;

import cloud.filibuster.junit.Assertions;
import io.grpc.MethodDescriptor;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

import static org.grpcmock.GrpcMock.calledMethod;
import static org.grpcmock.GrpcMock.times;
import static org.grpcmock.GrpcMock.unaryMethod;

public class GrpcMock {
    private static HashMap<String, Integer> adjustedExpectationsForMethods = new HashMap<>();

    private static HashMap<Object, Integer> adjustedExpectationsForRequests = new HashMap<>();

    public static void resetAdjustedExpectations() {
        adjustedExpectationsForMethods = new HashMap<>();
        adjustedExpectationsForRequests = new HashMap<>();
    }

    public static <ReqT> void adjustExpectation(
            @Nonnull MethodDescriptor<ReqT, ?> method,
            @Nonnull int count
    ) {
        adjustedExpectationsForMethods.put(method.getFullMethodName(), count);
    }

    public static <ReqT> void adjustExpectation(
            @Nonnull MethodDescriptor<ReqT, ?> method,
            @Nonnull ReqT request,
            int count
    ) {
        adjustedExpectationsForRequests.put(request, count);
    }

    public static HashMap<String, Boolean> verifyThatMapping = new HashMap<>();

    public static void resetVerifyThatMapping() {
        verifyThatMapping = new HashMap<>();
    }

    public static <ReqT, RespT> void stubFor(
            @Nonnull MethodDescriptor<ReqT, RespT> method,
            @Nonnull ReqT request,
            @Nonnull RespT response
            ) {
        verifyThatMapping.put(method.getFullMethodName(), false);

        org.grpcmock.GrpcMock.stubFor(unaryMethod(method).withRequest(request).willReturn(response));
    }

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

        org.grpcmock.GrpcMock.verifyThat(calledMethod(method), times(count));
    }

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

        for (Map.Entry<Object, Integer> adjustedExpectationsForRequest : adjustedExpectationsForRequests.entrySet()){
            if (adjustedExpectationsForRequest.getKey().equals(request)) {
                count = adjustedExpectationsForRequest.getValue();
            }
        }

        org.grpcmock.GrpcMock.verifyThat(calledMethod(method).withRequest(request), times(count));
    }
}
