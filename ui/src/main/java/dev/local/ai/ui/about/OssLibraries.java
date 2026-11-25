package dev.local.ai.ui.about;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Open-source libraries distributed with the application whose licenses require attribution.
 */
public final class OssLibraries {

    private static final String RESOURCE = "/about/oss-libraries.txt";
    private static final int FIELD_COUNT = 6;

    private OssLibraries() {
    }

    public static List<OssLibrary> load() {
        InputStream in = OssLibraries.class.getResourceAsStream(RESOURCE);
        if (in == null) {
            throw new IllegalStateException("Missing classpath resource " + RESOURCE);
        }
        try (InputStream stream = in) {
            List<OssLibrary> libraries = parse(stream);
            if (libraries.isEmpty()) {
                throw new IllegalStateException("Open-source library list is empty");
            }
            return libraries;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + RESOURCE, e);
        }
    }

    static List<OssLibrary> parse(InputStream in) throws IOException {
        List<OssLibrary> libraries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                libraries.add(parseLine(trimmed, lineNumber));
            }
        }
        return List.copyOf(libraries);
    }

    private static OssLibrary parseLine(String line, int lineNumber) {
        String[] fields = line.split("\\|", -1);
        if (fields.length != FIELD_COUNT) {
            throw new IllegalStateException(
                    "Expected " + FIELD_COUNT + " fields at line " + lineNumber + " of " + RESOURCE);
        }
        return new OssLibrary(
                required(fields[0], "name", lineNumber),
                required(fields[1], "version", lineNumber),
                required(fields[2], "license", lineNumber),
                optional(fields[3], "licenseUrl", lineNumber),
                optional(fields[4], "projectUrl", lineNumber),
                optional(fields[5], "copyright", lineNumber));
    }

    private static String required(String value, String field, int lineNumber) {
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.contains("${")) {
            throw new IllegalStateException("Unresolved or missing " + field + " at line " + lineNumber);
        }
        return trimmed;
    }

    private static String optional(String value, String field, int lineNumber) {
        String trimmed = value.trim();
        if (trimmed.contains("${")) {
            throw new IllegalStateException("Unresolved " + field + " at line " + lineNumber);
        }
        return trimmed;
    }
}
