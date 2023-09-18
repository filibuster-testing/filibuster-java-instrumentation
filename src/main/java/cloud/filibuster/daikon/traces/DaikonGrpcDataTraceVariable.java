package cloud.filibuster.daikon.traces;

import javax.annotation.Nullable;

public class DaikonGrpcDataTraceVariable {
    private final String name;

    private final String value;

    private final int hashCode;

    public DaikonGrpcDataTraceVariable(String name, @Nullable String value, int hashCode) {
        this.name = name;
        this.value = value;
        this.hashCode = hashCode;
    }

    public String getName() {
        return name;
    }

    @Nullable public String getValue() {
        return value;
    }

    public int getHashCode() {
        return hashCode;
    }

    public int getModified() {
        return 1;
    }
}
