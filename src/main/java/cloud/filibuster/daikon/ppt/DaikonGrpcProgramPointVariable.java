package cloud.filibuster.daikon.ppt;

import javax.annotation.Nullable;

public class DaikonGrpcProgramPointVariable {
    private final String name;
    private final String varKind;
    private final String decType;
    private final String repType;
    private final String flags;
    private final String comparability;

    private final String enclosingVar;

    public DaikonGrpcProgramPointVariable(
            String name,
            String varKind,
            @Nullable String enclosingVar,
            String decType,
            String repType,
            String flags,
            String comparability
    ) {
        this.name = name;
        this.varKind = varKind;
        this.enclosingVar = enclosingVar;
        this.decType = decType;
        this.repType = repType;
        this.flags = flags;
        this.comparability = comparability;
    }

    public String getName() {
        return this.name;
    }

    public String getVarKind() {
        return this.varKind;
    }

    public String getDecType() {
        return decType;
    }

    public String getRepType() {
        return repType;
    }

    public String getFlags() {
        return flags;
    }

    public String getComparability() {
        return comparability;
    }

    @Nullable public String getEnclosingVar() {
        return enclosingVar;
    }
}
