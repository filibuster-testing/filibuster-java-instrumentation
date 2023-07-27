package cloud.filibuster.instrumentation.datatypes;

import org.json.JSONObject;

public class CallsiteArguments {
    public Class clazz;
    public String toStringResult;

    public CallsiteArguments(Class clazz, String toStringResult) {
        this.clazz = clazz;
        this.toStringResult = toStringResult;
    }

    public String getStringClass() {
        return this.clazz.toString();
    }

    public String getToStringResult() {
        return this.toStringResult;
    }

    public JSONObject toJSONObject() {
        JSONObject response = new JSONObject();
        response.put("__class__", clazz.getName());
        response.put("toString", toStringResult);
        return response;
    }
}
