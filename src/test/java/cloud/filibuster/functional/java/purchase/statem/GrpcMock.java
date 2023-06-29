package cloud.filibuster.functional.java.purchase.statem;

import cloud.filibuster.dei.DistributedExecutionIndex;
import cloud.filibuster.junit.Assertions;
import io.grpc.MethodDescriptor;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static cloud.filibuster.junit.Assertions.getFaultsInjected;
import static org.grpcmock.GrpcMock.calledMethod;
import static org.grpcmock.GrpcMock.times;

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

    public static <ReqT> void verifyThat(
            @Nonnull MethodDescriptor<ReqT, ?> method,
            int count
    ) {
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
        String fullMethodName = method.getFullMethodName();
        HashMap<DistributedExecutionIndex, JSONObject> faultsInjected = getFaultsInjected();
        String toStringRequest = request.toString();

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
