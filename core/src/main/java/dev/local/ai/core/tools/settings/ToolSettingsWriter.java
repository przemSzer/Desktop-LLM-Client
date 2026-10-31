package dev.local.ai.core.tools.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class ToolSettingsWriter {

    private static final Logger logger = LoggerFactory.getLogger(ToolSettingsWriter.class);

    private final ObjectMapper objectMapper;

    public ToolSettingsWriter() {
        this(new ObjectMapper());
    }

    public ToolSettingsWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(Path file, ToolSettings settings) {
        if (file == null) {
            logger.error("Cannot write tool settings: file path is null");
            return;
        }
        try {
            Path parent = file.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(file.toFile(), settings == null ? ToolSettings.empty() : settings);
            logger.debug("Wrote tool settings to {}", file);
        } catch (Exception e) {
            logger.error("Failed to write tool settings to {}: {}", file, e.getMessage());
        }
    }
}
