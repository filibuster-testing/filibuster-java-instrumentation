package cloud.filibuster.junit.server.core.serializers;

import io.grpc.Status;
import org.json.JSONObject;

public class StatusSerializer {
    static class Keys {
        public static final String CLASS_KEY = "class";
        public static final String CODE_KEY = "code";
        public static final String CAUSE_KEY = "cause";
        public static final String DESCRIPTION_KEY = "description";
    }

    public static JSONObject toJSONObject(Status status) {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put(Keys.CLASS_KEY, "io.grpc.Status");
        jsonObject.put(Keys.CODE_KEY, status.getCode().toString());

        Throwable cause = status.getCause();

        if (cause != null) {
            jsonObject.put(Keys.CAUSE_KEY, status.getCause().toString());
        } 

        jsonObject.put(Keys.DESCRIPTION_KEY, status.getDescription());

        return jsonObject;
    }

    public static Status fromJSONObject(JSONObject jsonObject) {
        String codeStr = jsonObject.getString(Keys.CODE_KEY);
        Status.Code code = Status.Code.valueOf(codeStr);
        Status status = Status.fromCode(code);

        if (jsonObject.has(Keys.DESCRIPTION_KEY)) {
            String descriptionStr = jsonObject.getString(Keys.DESCRIPTION_KEY);
            status = Status.fromCode(code).withDescription(descriptionStr);
        }

        // cause does not serialize across service boundaries, therefore it's ignored as part of service profile creation/restoration.

        return status;
    }
}
