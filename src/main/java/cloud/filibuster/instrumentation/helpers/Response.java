package cloud.filibuster.instrumentation.helpers;

import com.linecorp.armeria.common.AggregatedHttpResponse;
import io.netty.buffer.ByteBuf;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class Response {
    private Response() {

    }

    public static JSONObject aggregatedHttpResponseToJsonObject(AggregatedHttpResponse response) {
        ByteBuf byteBuf = response.content().byteBuf();
        String body = byteBuf.readCharSequence(response.content().length(), StandardCharsets.UTF_8).toString();
        return new JSONObject(body);
    }
}
