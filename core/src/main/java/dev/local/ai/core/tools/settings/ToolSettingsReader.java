package dev.local.ai.core.tools.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class ToolSettingsReader {

    private static final Logger logger = LoggerFactory.getLogger(ToolSettingsReader.class);

    private final ObjectMapper objectMapper;

    public ToolSettingsReader() {
        this(new ObjectMapper());
    }

    public ToolSettingsReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ToolSettings read(Path file) {
        if (file == null || !Files.exists(file)) {
            return ToolSettings.empty();
        }
        try {
            var settings = objectMapper.readValue(file.toFile(), ToolSettings.class);
            return settings == null ? ToolSettings.empty() : settings;
        } catch (Exception e) {
            logger.error("Failed to read tool settings from {}", file, e);
            return ToolSettings.empty();
        }
    }
}
