package cloud.filibuster.instrumentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FilibusterJsonSerializer {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private FilibusterJsonSerializer() {

    }

    public static String toJson(Object obj) throws JsonProcessingException {
        return objectMapper.writeValueAsString(obj);
    }

    public static Object fromJson(String json, Class cls) throws JsonProcessingException {
        return objectMapper.readValue(json, cls);
    }
}
