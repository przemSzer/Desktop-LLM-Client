package dev.local.ai.core.tools.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.local.ai.core.storage.ApplicationDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class McpServersStore {

    private static final Logger logger = LoggerFactory.getLogger(McpServersStore.class);

    public static final String FILE_NAME = "mcp-servers.json";

    private final Path file;
    private final ObjectMapper objectMapper;

    public McpServersStore() {
        this(ApplicationDirectory.root().resolve(FILE_NAME));
    }

    public McpServersStore(Path file) {
        this(file, new ObjectMapper());
    }

    public McpServersStore(Path file, ObjectMapper objectMapper) {
        this.file = file;
        this.objectMapper = objectMapper;
    }

    public Path file() {
        return file;
    }

    /**
     * Returns configured servers in file order. Invalid entries are skipped with a warning.
     * Servers with {@code enabled: false} are included so the UI can show them as unavailable.
     */
    public List<McpServerConfig> readAll() {
        McpServersFile parsed = readFile();
        Map<String, McpServerConfig> configs = parsed.toConfigs();
        List<McpServerConfig> result = new ArrayList<>();
        for (McpServerConfig config : configs.values()) {
            if (!config.isValid()) {
                logger.warn(
                        "Skipping MCP server '{}': set either url or command (not both)",
                        config.id()
                );
                continue;
            }
            result.add(config);
        }
        return List.copyOf(result);
    }

    /**
     * Returns servers a chat may start ({@code enabled} omitted or true).
     */
    public List<McpServerConfig> readStartable() {
        return readAll().stream()
                .filter(McpServerConfig::isEnabled)
                .toList();
    }

    public void writeAll(List<McpServerConfig> configs) {
        Map<String, McpServerConfigEntry> map = new LinkedHashMap<>();
        if (configs != null) {
            for (McpServerConfig config : configs) {
                if (config == null || config.id() == null || config.id().isBlank()) {
                    continue;
                }
                map.put(config.id(), new McpServerConfigEntry(
                        config.name(),
                        config.enabled(),
                        config.url(),
                        config.headers(),
                        config.command(),
                        config.args(),
                        config.env(),
                        config.workingDirectory(),
                        config.timeoutSeconds(),
                        config.serializeRequests()
                ));
            }
        }
        writeFile(new McpServersFile(map));
    }

    private McpServersFile readFile() {
        if (file == null || !Files.exists(file)) {
            return McpServersFile.empty();
        }
        try {
            var parsed = objectMapper.readValue(file.toFile(), McpServersFile.class);
            return parsed == null ? McpServersFile.empty() : parsed;
        } catch (Exception e) {
            logger.error("Failed to read MCP servers from {}", file, e);
            return McpServersFile.empty();
        }
    }

    private void writeFile(McpServersFile serversFile) {
        if (file == null) {
            logger.error("Cannot write MCP servers: file path is null");
            return;
        }
        try {
            Path parent = file.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(file.toFile(), serversFile);
            logger.debug("Wrote MCP servers to {}", file);
        } catch (Exception e) {
            logger.error("Failed to write MCP servers to {}: {}", file, e.getMessage());
        }
    }
}
