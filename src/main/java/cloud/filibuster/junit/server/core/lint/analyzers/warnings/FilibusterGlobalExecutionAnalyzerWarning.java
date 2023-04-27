package cloud.filibuster.junit.server.core.lint.analyzers.warnings;

public abstract class FilibusterGlobalExecutionAnalyzerWarning {

    private final String details;

    public FilibusterGlobalExecutionAnalyzerWarning(String details) {
        this.details = details;
    }

    public abstract String getName();

    public abstract String getDescription();

    public abstract String getRecommendations();

    public abstract String getImpact();

    public String getDetails() {
        return this.details;
    }
}
