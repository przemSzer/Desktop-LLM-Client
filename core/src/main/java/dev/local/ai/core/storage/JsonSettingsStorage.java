package dev.local.ai.core.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JsonSettingsStorage implements SettingsStorage {

    private static final Logger logger = LoggerFactory.getLogger(JsonSettingsStorage.class);
    private static final String DEFAULT_FILE_NAME = "settings.json";

    private final ObjectMapper objectMapper;
    private final Path settingsFile;

    public JsonSettingsStorage() {
        this(Paths.get(System.getProperty("user.home"), ".local-ai"), DEFAULT_FILE_NAME);
    }

    public JsonSettingsStorage(Path directory, String fileName) {
        this.objectMapper = new ObjectMapper();
        this.settingsFile = directory.resolve(fileName);
        createDirectoryIfNotExists(directory);
    }

    @Override
    public <T> void save(String key, T value) {
        Map<String, String> settings = loadAll();
        try {
            String json = objectMapper.writeValueAsString(value);
            settings.put(key, json);
            writeAll(settings);
            logger.debug("Saved setting '{}' to {}", key, settingsFile);
        } catch (IOException e) {
            logger.error("Failed to save setting '{}': {}", key, e.getMessage());
        }
    }

    @Override
    public <T> Optional<T> read(String key, Class<T> type) {
        Map<String, String> settings = loadAll();
        String json = settings.get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            T value = objectMapper.readValue(json, type);
            return Optional.of(value);
        } catch (IOException e) {
            logger.error("Failed to read setting '{}': {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, String> loadAll() {
        if (!Files.exists(settingsFile)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(
                    settingsFile.toFile(),
                    new TypeReference<Map<String, String>>() {}
            );
        } catch (IOException e) {
            logger.error("Failed to load settings from {}: {}", settingsFile, e.getMessage());
            return new HashMap<>();
        }
    }

    private void writeAll(Map<String, String> settings) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(settingsFile.toFile(), settings);
        } catch (IOException e) {
            logger.error("Failed to write settings to {}: {}", settingsFile, e.getMessage());
        }
    }

    private void createDirectoryIfNotExists(Path directory) {
        try {
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
                logger.info("Created settings directory: {}", directory);
            }
        } catch (IOException e) {
            logger.error("Failed to create settings directory: {}", directory, e);
        }
    }
}
