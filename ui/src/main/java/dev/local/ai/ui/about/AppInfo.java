package dev.local.ai.ui.about;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Application name, version, and description filtered from the parent POM.
 */
public record AppInfo(String name, String version, String description) {

    private static final String RESOURCE = "/about/app.properties";

    public static AppInfo load() {
        InputStream in = AppInfo.class.getResourceAsStream(RESOURCE);
        if (in == null) {
            throw new IllegalStateException("Missing classpath resource " + RESOURCE);
        }
        try (InputStream stream = in) {
            return parse(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + RESOURCE, e);
        }
    }

    static AppInfo parse(InputStream in) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return new AppInfo(
                required(properties.getProperty("name"), "name"),
                required(properties.getProperty("version"), "version"),
                required(properties.getProperty("description"), "description"));
    }

    private static String required(String value, String key) {
        if (value == null || value.isBlank() || value.contains("${")) {
            throw new IllegalStateException("Unresolved or missing application property '" + key + "'");
        }
        return value.trim();
    }
}
