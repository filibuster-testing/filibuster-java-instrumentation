package cloud.filibuster.daikon;

import cloud.filibuster.exceptions.filibuster.FilibusterRuntimeException;
import cloud.filibuster.junit.server.core.transformers.JsonUtils;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class DaikonGrpcDataTraceRecord {
    public String programPointName;
    public String thisInvocationNonce = "this_invocation_nonce";
    public String nonceString;
    public List<DaikonGrpcDataTraceVariable> variables;

    public DaikonGrpcDataTraceRecord(String programPointName, String nonceString, List<DaikonGrpcDataTraceVariable> variables) {
        this.programPointName = programPointName;
        this.nonceString = nonceString;
        this.variables = variables;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append(programPointName + "\n");
        builder.append(thisInvocationNonce + "\n");
        builder.append(nonceString + "\n");

        for (DaikonGrpcDataTraceVariable variable : variables) {
            builder.append(variable.getName() + "\n");
            builder.append("\"" + variable.getValue() + "\"\n");
            builder.append(variable.getModified() + "\n");
        }

        return builder.toString();
    }

    public static DaikonGrpcDataTraceRecord fromGeneratedMessageV3(String nonceString, DaikonPptType daikonPptType, String programPointName, GeneratedMessageV3 generatedMessageV3) {
        // Update program point name.
        String messageName = generatedMessageV3.getDescriptorForType().getName();
        programPointName = programPointName.replace("/", ".");
        String concreteProgramPointName;

        if (daikonPptType.equals(DaikonPptType.EXIT)) {
            concreteProgramPointName = programPointName + "(" + messageName + "):::" + daikonPptType + "0"; // assume single exit point for GRPC for now, no way to really know currently.
        } else {
            concreteProgramPointName = programPointName + "(" + messageName + "):::" + daikonPptType;
        }

        // Convert to JSON and include all default key names.
        try {
            String serializedMessage = JsonFormat.printer().preservingProtoFieldNames().includingDefaultValueFields().print(generatedMessageV3);
            JSONObject jsonObject = new JSONObject(serializedMessage);

            // Flatten to uniformly (easily) handle nested GRPC messages.
            jsonObject = JsonUtils.flatten(jsonObject);

            // Iterate all the keys.
            Iterator<String> keys = jsonObject.keys();
            List<DaikonGrpcDataTraceVariable> variables = new ArrayList<>();

            while(keys.hasNext()) {
                String key = keys.next();
                Object value = jsonObject.get(key);
                variables.add(new DaikonGrpcDataTraceVariable(key, value.toString()));
            }

            // Generate the trace record.
            return new DaikonGrpcDataTraceRecord(concreteProgramPointName, nonceString, variables);
        } catch (InvalidProtocolBufferException e) {
            throw new FilibusterRuntimeException(e);
        }
    }

    public static List<DaikonGrpcDataTraceRecord> onRequestAndResponse(String fullMethodName, GeneratedMessageV3 request, GeneratedMessageV3 response) {
        String nonceString = UUID.randomUUID().toString();
        List<DaikonGrpcDataTraceRecord> traceRecords = new ArrayList<>();
        traceRecords.add(onRequest(nonceString, fullMethodName, request));
        traceRecords.add(onResponse(nonceString, fullMethodName, response));
        return traceRecords;
    }

    public static DaikonGrpcDataTraceRecord onRequest(String nonceString, String fullMethodName, GeneratedMessageV3 request) {
        return fromGeneratedMessageV3(nonceString, DaikonPptType.ENTER, fullMethodName, request);
    }

    public static DaikonGrpcDataTraceRecord onResponse(String nonceString, String fullMethodName, GeneratedMessageV3 response) {
        return fromGeneratedMessageV3(nonceString, DaikonPptType.EXIT, fullMethodName, response);
    }
}
