package cloud.filibuster.daikon;

import com.google.protobuf.GeneratedMessageV3;

public class DaikonPpt {
    private DaikonPpt() {

    }

    public static String generatePpt(String fullMethodName, DaikonPptType daikonPptType, GeneratedMessageV3 generatedMessageV3) {
        // Update program point name.
        String messageName = generatedMessageV3.getDescriptorForType().getName();
        fullMethodName = fullMethodName.replace("/", ".");
        String ppt;

        if (daikonPptType.equals(DaikonPptType.EXIT)) {
            ppt = fullMethodName + "(" + messageName + "):::" + daikonPptType + "0"; // assume single exit point for GRPC for now, no way to really know currently.
        } else {
            ppt = fullMethodName + "(" + messageName + "):::" + daikonPptType;
        }

        return ppt;
    }
}
