package cloud.filibuster.instrumentation.libraries.grpc;

import io.grpc.ClientCall;
import io.grpc.Metadata;

public class NoopClientCall<REQUEST, RESPONSE> extends ClientCall<REQUEST, RESPONSE> {
    @Override public void start(ClientCall.Listener<RESPONSE> listener, Metadata headers) {}

    @Override public void request(int numMessages) {}

    @Override public void cancel(String message, Throwable cause) {}

    @Override public void halfClose() {}

    @Override public void sendMessage(REQUEST message) {}
}
