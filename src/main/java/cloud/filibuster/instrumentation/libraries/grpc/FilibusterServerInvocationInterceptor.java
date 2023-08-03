package cloud.filibuster.instrumentation.libraries.grpc;

import cloud.filibuster.junit.server.core.reports.ServerInvocationAndResponseReport;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

import java.util.UUID;

import static cloud.filibuster.instrumentation.helpers.Property.getInstrumentationEnabledProperty;

public class FilibusterServerInvocationInterceptor implements ServerInterceptor {
    public FilibusterServerInvocationInterceptor(String packageName) {
        ServerInvocationAndResponseReport.loadGrpcEndpoints(packageName);
    }

    public FilibusterServerInvocationInterceptor(Class clazz) {
        ServerInvocationAndResponseReport.loadGrpcEndpoints(clazz);
    }

    private static boolean shouldInstrument() {
        if (getInstrumentationEnabledProperty()) {
            return true;
        }

        return false;
    }

    private static class FilibusterServerCallListener<REQUEST> extends ForwardingServerCallListener.SimpleForwardingServerCallListener<REQUEST> {
        private final String requestId;

        protected FilibusterServerCallListener(Listener<REQUEST> delegate, String requestId) {
            super(delegate);
            this.requestId = requestId;
        }

        @Override
        public void onMessage(REQUEST message) {
            if (!shouldInstrument()) {
                super.onMessage(message);
            } else {
                GeneratedMessageV3 generatedMessage = (GeneratedMessageV3) message;
                ServerInvocationAndResponseReport.beginServerInvocation(requestId, generatedMessage);
                super.onMessage(message);
            }
        }
    }

    private static class FilibusterServerCall<REQUEST, RESPONSE> extends SimpleForwardingServerCall<REQUEST, RESPONSE> {
        private final String requestId;
        private final String fullMethodName;

        protected FilibusterServerCall(ServerCall<REQUEST, RESPONSE> delegate, String requestId, String fullMethodName) {
            super(delegate);
            this.requestId = requestId;
            this.fullMethodName = fullMethodName;
        }

        @Override
        public void sendMessage(RESPONSE message) {
            if (!shouldInstrument()) {
                super.sendMessage(message);
            } else {
                GeneratedMessageV3 generatedMessage = (GeneratedMessageV3) message;
                ServerInvocationAndResponseReport.endServerInvocation(requestId, fullMethodName, Status.OK, generatedMessage);
                super.sendMessage(message);
            }
        }

        @Override
        public void close(Status status, Metadata trailers) {
            if (!shouldInstrument()) {
                super.close(status, trailers);
            } else {
                if (!status.equals(Status.OK)) {
                    ServerInvocationAndResponseReport.endServerInvocation(requestId, fullMethodName, status, null);
                }
                super.close(status, trailers);
            }
        }
    }

    @Override
    public <REQUEST, RESPONSE> ServerCall.Listener<REQUEST> interceptCall(
            ServerCall<REQUEST, RESPONSE> call, Metadata metadata, ServerCallHandler<REQUEST, RESPONSE> next) {

        String fullMethodName = call.getMethodDescriptor().getFullMethodName();
        String requestId = UUID.randomUUID().toString();

        return new FilibusterServerCallListener<>(next.startCall(new FilibusterServerCall<REQUEST, RESPONSE>(call, requestId, fullMethodName), metadata), requestId);
    }
}