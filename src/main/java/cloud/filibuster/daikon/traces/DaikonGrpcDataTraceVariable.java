package cloud.filibuster.daikon.traces;

public class DaikonGrpcDataTraceVariable {
    private final String name;

    private final String value;

    public DaikonGrpcDataTraceVariable(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public int getModified() {
        return 1;
    }
}
