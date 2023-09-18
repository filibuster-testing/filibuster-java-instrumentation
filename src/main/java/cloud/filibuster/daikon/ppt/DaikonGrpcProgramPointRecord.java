package cloud.filibuster.daikon.ppt;

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
import java.util.Locale;
import java.util.Objects;

import static cloud.filibuster.daikon.DaikonPpt.generatePpt;

public class DaikonGrpcProgramPointRecord {
    private final String ppt;
    private final DaikonPptType daikonPptType;
    private final List<DaikonGrpcProgramPointVariable> variables;

    public DaikonGrpcProgramPointRecord(String fullMethodName, DaikonPptType daikonPptType, GeneratedMessageV3 requestMessage, @Nullable GeneratedMessageV3 responseMessage) {
        this.ppt = generatePpt(fullMethodName, daikonPptType, requestMessage);
        this.daikonPptType = daikonPptType;

        try {

            List<DaikonGrpcProgramPointVariable> variables = new ArrayList<>();

            if (daikonPptType.equals(DaikonPptType.ENTER) || daikonPptType.equals(DaikonPptType.EXIT)) {
                variables.add(new DaikonGrpcProgramPointVariable(
                        "this",
                        "variable",
                        null,
                        "java.lang.Object",
                        "hashcode",
                        "is_param nomod",
                        "1"));

                String serializedMessage = JsonFormat.printer().preservingProtoFieldNames().includingDefaultValueFields().print(requestMessage);
                JSONObject jsonObject = new JSONObject(serializedMessage);
                jsonObject = JsonUtils.flatten(jsonObject);
                Iterator<String> keys = jsonObject.keys();

                while(keys.hasNext()) {
                    String key = keys.next();
                    variables.add(new DaikonGrpcProgramPointVariable(
                            "this." + key,
                            "field " + key,
                            "this",
                            "java.lang.String",
                            "java.lang.String",
                            "nomod not_ordered to_string",
                            "1"));
                }
            }

            if (daikonPptType.equals(DaikonPptType.EXIT)) {
                variables.add(new DaikonGrpcProgramPointVariable(
                        "return",
                        "return",
                        null,
                        "java.lang.Object",
                        "hashcode",
                        null,
                        "1"));

                String serializedMessage = JsonFormat.printer().preservingProtoFieldNames().includingDefaultValueFields().print(responseMessage);
                JSONObject jsonObject = new JSONObject(serializedMessage);
                jsonObject = JsonUtils.flatten(jsonObject);
                Iterator<String> keys = jsonObject.keys();

                while(keys.hasNext()) {
                    String key = keys.next();
                    variables.add(new DaikonGrpcProgramPointVariable(
                            "return." + key,
                            "field " + key,
                            "return",
                            "java.lang.String",
                            "java.lang.String",
                            "not_ordered to_string",
                            "1"));
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
        builder.append("ppt " + ppt + "\n");
        builder.append("ppt-type " + daikonPptType.toString().toLowerCase(Locale.ROOT) + "\n");

        for (DaikonGrpcProgramPointVariable variable : variables) {
            builder.append("variable " + variable.getName() + "\n");
            builder.append("  var-kind " + variable.getVarKind() + "\n");
            if (variable.getEnclosingVar() != null) {
                builder.append("  enclosing-var " + variable.getEnclosingVar() + "\n");
            }
            builder.append("  dec-type " + variable.getDecType() + "\n");
            builder.append("  rep-type " + variable.getRepType() + "\n");
            if (variable.getFlags() != null) {
                builder.append("  flags " + variable.getFlags() + "\n");
            }
            builder.append("  comparability " + variable.getComparability() + "\n");
        }

        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.ppt);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof DaikonGrpcProgramPointRecord)) {
            return false;
        }

        DaikonGrpcProgramPointRecord r = (DaikonGrpcProgramPointRecord) o;
        return Objects.equals(this.ppt, r.ppt);
    }

    public static List<DaikonGrpcProgramPointRecord> onRequestAndResponse(String fullMethodName, GeneratedMessageV3 request, GeneratedMessageV3 response) {
        List<DaikonGrpcProgramPointRecord> declRecords = new ArrayList<>();
        declRecords.add(new DaikonGrpcProgramPointRecord(fullMethodName, DaikonPptType.ENTER, request, null));
        declRecords.add(new DaikonGrpcProgramPointRecord(fullMethodName, DaikonPptType.EXIT, request, response));
        return declRecords;
    }
}
