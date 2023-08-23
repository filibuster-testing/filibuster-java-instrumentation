package cloud.filibuster.junit.server.core.transformers;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Map;

public class DBException {

    private final String name;
    private final Map<String, String> metadata;

    public DBException(String name, Map<String, String> metadata) {
        this.name = name;
        this.metadata = metadata;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public static class Builder {
        private String name;
        private Map<String, String> metadata;

        @CanIgnoreReturnValue
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @CanIgnoreReturnValue
        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        @CanIgnoreReturnValue
        public DBException build() {
            return new DBException(name, metadata);
        }
    }
}
