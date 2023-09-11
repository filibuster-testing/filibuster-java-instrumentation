package cloud.filibuster.daikon.ppt;

import cloud.filibuster.daikon.DaikonPptType;
import cloud.filibuster.exceptions.filibuster.FilibusterRuntimeException;
import cloud.filibuster.junit.server.core.transformers.JsonUtils;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static cloud.filibuster.daikon.DaikonPpt.generatePpt;

public class DaikonGrpcProgramPointRecord {
    private final String ppt;
    private final DaikonPptType daikonPptType;
    private List<DaikonGrpcProgramPointVariable> variables = new ArrayList<>();

    public DaikonGrpcProgramPointRecord(String ppt, DaikonPptType daikonPptType, GeneratedMessageV3 generatedMessageV3) {
        this.ppt = ppt;
        this.daikonPptType = daikonPptType;

        try {
            String serializedMessage = JsonFormat.printer().preservingProtoFieldNames().includingDefaultValueFields().print(generatedMessageV3);
            JSONObject jsonObject = new JSONObject(serializedMessage);

            // Flatten to uniformly (easily) handle nested GRPC messages.
            jsonObject = JsonUtils.flatten(jsonObject);

            // Iterate all the keys.
            Iterator<String> keys = jsonObject.keys();
            List<DaikonGrpcProgramPointVariable> variables = new ArrayList<>();

            while(keys.hasNext()) {
                String key = keys.next();
                variables.add(new DaikonGrpcProgramPointVariable(key));
            }

            this.variables = variables;
        } catch (InvalidProtocolBufferException e) {
            throw new FilibusterRuntimeException(e);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ppt " + ppt + "\n");
        builder.append("ppt-type " + daikonPptType.toString().toLowerCase(Locale.ROOT) + "\n");

        for (DaikonGrpcProgramPointVariable variable : variables) {
            builder.append("variable " + variable.getName() + "\n");
            builder.append("  var-kind " + variable.getVarKind() + "\n");
            builder.append("  dec-type " + variable.getDecType() + "\n");
            builder.append("  rep-type " + variable.getRepType() + "\n");
            builder.append("  flags " + variable.getFlags() + "\n");
            builder.append("  comparability " + variable.getComparability() + "\n");
        }

        return builder.toString();
    }

    public static DaikonGrpcProgramPointRecord onRequest(String fullMethodName, GeneratedMessageV3 request) {
        return new DaikonGrpcProgramPointRecord(generatePpt(fullMethodName, DaikonPptType.ENTER, request), DaikonPptType.ENTER, request);
    }

    public static DaikonGrpcProgramPointRecord onResponse(String fullMethodName, GeneratedMessageV3 response) {
        return new DaikonGrpcProgramPointRecord(generatePpt(fullMethodName, DaikonPptType.EXIT, response), DaikonPptType.EXIT, response);
    }
}
