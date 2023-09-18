package cloud.filibuster.daikon.traces;

import cloud.filibuster.daikon.DaikonPptType;
import cloud.filibuster.exceptions.filibuster.FilibusterRuntimeException;
import cloud.filibuster.junit.server.core.transformers.JsonUtils;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.util.JsonFormat;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static cloud.filibuster.daikon.DaikonPpt.generatePpt;

public class DaikonGrpcDataTraceRecord {
    public String nonceString;
    private final String ppt;
    private final List<DaikonGrpcDataTraceVariable> variables;

    public DaikonGrpcDataTraceRecord(String nonceString, String fullMethodName, DaikonPptType daikonPptType, GeneratedMessageV3 requestMessage, @Nullable GeneratedMessageV3 responseMessage) {
        this.ppt = generatePpt(fullMethodName, daikonPptType, requestMessage);
        this.nonceString = nonceString;

        try {
            List<DaikonGrpcDataTraceVariable> variables = new ArrayList<>();

            if (daikonPptType.equals(DaikonPptType.ENTER) || daikonPptType.equals(DaikonPptType.EXIT)) {
                variables.add(new DaikonGrpcDataTraceVariable("this", null, requestMessage.hashCode()));

                String serializedMessage = JsonFormat.printer().preservingProtoFieldNames().includingDefaultValueFields().print(requestMessage);
                JSONObject jsonObject = new JSONObject(serializedMessage);
                jsonObject = JsonUtils.flatten(jsonObject);
                Iterator<String> keys = jsonObject.keys();

                while(keys.hasNext()) {
                    String key = keys.next();
                    Object value = jsonObject.get(key);
                    variables.add(new DaikonGrpcDataTraceVariable("this." + key, value.toString(), value.hashCode()));
                }
            }

            if (daikonPptType.equals(DaikonPptType.EXIT)) {
                variables.add(new DaikonGrpcDataTraceVariable("return", null, responseMessage.hashCode()));

                String serializedMessage = JsonFormat.printer().preservingProtoFieldNames().includingDefaultValueFields().print(responseMessage);
                JSONObject jsonObject = new JSONObject(serializedMessage);
                jsonObject = JsonUtils.flatten(jsonObject);
                Iterator<String> keys = jsonObject.keys();

                while(keys.hasNext()) {
                    String key = keys.next();
                    Object value = jsonObject.get(key);
                    variables.add(new DaikonGrpcDataTraceVariable("return." + key, value.toString(), value.hashCode()));
                }
            }

            this.variables = variables;
        } catch (Throwable t) {
            throw new FilibusterRuntimeException(t);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append(ppt + "\n");

        for (DaikonGrpcDataTraceVariable variable : variables) {
            builder.append(variable.getName() + "\n");
            if (variable.getValue() == null) {
                builder.append(variable.getHashCode() + "\n");
            } else {
                builder.append("\"" + variable.getValue() + "\"\n");
            }
            builder.append(variable.getModified() + "\n");
        }

        return builder.toString();
    }

    public static List<DaikonGrpcDataTraceRecord> onRequestAndResponse(String fullMethodName, GeneratedMessageV3 request, GeneratedMessageV3 response) {
        String nonceString = UUID.randomUUID().toString();
        List<DaikonGrpcDataTraceRecord> traceRecords = new ArrayList<>();
        traceRecords.add(new DaikonGrpcDataTraceRecord(nonceString, fullMethodName, DaikonPptType.ENTER, request, null));
        traceRecords.add(new DaikonGrpcDataTraceRecord(nonceString, fullMethodName, DaikonPptType.EXIT, request, response));
        return traceRecords;
    }
}
