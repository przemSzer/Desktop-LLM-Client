package dev.local.ai.core.tools.mcp.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record McpServersFile(
        @JsonDeserialize(as = LinkedHashMap.class)
        Map<String, McpServerConfigEntry> mcpServers
) {

    public McpServersFile {
        if (mcpServers == null) {
            mcpServers = Map.of();
        } else {
            mcpServers = Collections.unmodifiableMap(new LinkedHashMap<>(mcpServers));
        }
    }

    public static McpServersFile empty() {
        return new McpServersFile(Map.of());
    }

    public Map<String, McpServerConfig> toConfigs() {
        Map<String, McpServerConfig> configs = new LinkedHashMap<>();
        for (var entry : mcpServers.entrySet()) {
            String id = entry.getKey();
            if (id == null || id.isBlank() || entry.getValue() == null) {
                continue;
            }
            configs.put(id, entry.getValue().toConfig(id));
        }
        return Collections.unmodifiableMap(configs);
    }
}
