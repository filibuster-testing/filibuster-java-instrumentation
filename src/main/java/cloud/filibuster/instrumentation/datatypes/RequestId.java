package cloud.filibuster.instrumentation.datatypes;

import java.util.UUID;

public class RequestId {
    private RequestId() {

    }

    public static UUID generateNewRequestId() {
        return UUID.randomUUID();
    }
}
