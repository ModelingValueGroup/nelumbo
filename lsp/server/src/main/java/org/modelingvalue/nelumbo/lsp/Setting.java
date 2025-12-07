package org.modelingvalue.nelumbo.lsp;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

public record Setting(Formatting formatting,
                      Classpath classpath) {
    private static final ObjectMapper jacksonObjectMapper = new ObjectMapper()
                                                                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public Setting() {
        this(new Formatting(), new Classpath());
    }

    @Override
    public String toString() {
        return "Setting{" +
               "formatting=" + formatting +
               ", classpath=" + classpath +
               '}';
    }

    public record Formatting(PropsSpaceLine propsSpaceLine) {
        public Formatting() {
            this(PropsSpaceLine.HAS_ANNOTATION);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Formatting that = (Formatting) o;
            return propsSpaceLine == that.propsSpaceLine;
        }

        @Override
        public String toString() {
            return "Formatting{" +
                   "propsSpaceLine=" + propsSpaceLine +
                   '}';
        }

        public enum PropsSpaceLine {
            ALWAYS, NEVER, HAS_ANNOTATION
        }
    }

    public record Classpath(boolean findConfiguration,
                            boolean findOtherProject) {
        public Classpath() {
            this(true, true);
        }

        @Override
        public String toString() {
            return "Classpath{" +
                   "findConfiguration=" + findConfiguration +
                   ", findOtherProject=" + findOtherProject +
                   '}';
        }
    }

    public static Setting read(Path path) {
        try {
            return jacksonObjectMapper
                           .setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE)
                           .readValue(path.toFile(), Setting.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void save(Path path) {
        try {
            jacksonObjectMapper
                    .setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE)
                    .writeValue(path.toFile(), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
