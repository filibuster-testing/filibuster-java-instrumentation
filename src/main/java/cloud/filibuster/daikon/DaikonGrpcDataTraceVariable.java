package cloud.filibuster.daikon;

public class DaikonGrpcDataTraceVariable {
    private String name;

    private String value;

    private int modified = 1;

    public DaikonGrpcDataTraceVariable(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public DaikonGrpcDataTraceVariable(String name, String value, int modified) {
        this.name = name;
        this.value = value;
        this.modified = modified;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public int getModified() {
        return modified;
    }
}
