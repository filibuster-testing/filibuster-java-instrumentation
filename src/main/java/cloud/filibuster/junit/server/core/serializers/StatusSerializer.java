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
        jsonObject.put(Keys.CAUSE_KEY, status.getCause());
        jsonObject.put(Keys.DESCRIPTION_KEY, status.getDescription());

        return jsonObject;
    }

    public static Status fromJSONObject(JSONObject jsonObject) {
        String codeStr = jsonObject.getString(Keys.CODE_KEY);
        Status.Code code = Status.Code.valueOf(codeStr);
        return Status.fromCode(code);
    }
}
