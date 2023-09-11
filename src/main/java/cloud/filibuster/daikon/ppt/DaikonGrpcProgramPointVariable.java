package cloud.filibuster.daikon.ppt;

import java.util.Collections;
import java.util.List;

public class DaikonGrpcProgramPointVariable {
    private final String name;
    private static final String varKind = "variable";
    private static final String decType = "java.lang.String";
    private static final String repType = "java.lang.String";
    private final List<String> flags = Collections.singletonList("is_param");
    private static final String comparability = "1";

    public DaikonGrpcProgramPointVariable(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String getVarKind() {
        return varKind;
    }

    public String getDecType() {
        return decType;
    }

    public String getRepType() {
        return repType;
    }

    public String getFlags() {
        return String.join(" ", this.flags);
    }

    public String getComparability() {
        return comparability;
    }
}
