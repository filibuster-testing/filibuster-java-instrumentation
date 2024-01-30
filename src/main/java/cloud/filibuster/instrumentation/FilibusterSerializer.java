package cloud.filibuster.instrumentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FilibusterSerializer {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private FilibusterSerializer() {

    }

    public static String toJson(Object obj) throws JsonProcessingException {
        return objectMapper.writeValueAsString(obj);
    }

    public static Object fromJson(String json, Class cls) throws JsonProcessingException {
        return objectMapper.readValue(json, cls);
    }
}
