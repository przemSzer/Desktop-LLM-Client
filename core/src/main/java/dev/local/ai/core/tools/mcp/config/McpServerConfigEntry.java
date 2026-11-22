package dev.local.ai.core.tools.mcp.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpServerConfigEntry(
        String name,
        Boolean enabled,
        String url,
        Map<String, String> headers,
        String command,
        List<String> args,
        Map<String, String> env,
        String workingDirectory,
        Integer timeoutSeconds,
        Boolean serializeRequests
) {

    public McpServerConfig toConfig(String id) {
        return new McpServerConfig(
                id,
                name,
                enabled,
                url,
                headers,
                command,
                args,
                env,
                workingDirectory,
                timeoutSeconds,
                serializeRequests
        );
    }
}
