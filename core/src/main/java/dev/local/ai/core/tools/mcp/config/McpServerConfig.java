package dev.local.ai.core.tools.mcp.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record McpServerConfig(
        String id,
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

    public static final int DEFAULT_TIMEOUT_SECONDS = 30;

    public McpServerConfig {
        Objects.requireNonNull(id, "id");
        name = name == null || name.isBlank() ? id : name;
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        args = args == null ? List.of() : List.copyOf(args);
        env = env == null ? Map.of() : Map.copyOf(env);
    }

    public boolean isEnabled() {
        return enabled == null || enabled;
    }

    public String displayName() {
        return name;
    }

    public int resolvedTimeoutSeconds() {
        return timeoutSeconds == null || timeoutSeconds <= 0
                ? DEFAULT_TIMEOUT_SECONDS
                : timeoutSeconds;
    }

    /**
     * When true, the client serializes RPC on this server (needed for some broken stdio stacks).
     * Omitted or false means parallel calls are allowed.
     */
    public boolean shouldSerializeRequests() {
        return Boolean.TRUE.equals(serializeRequests);
    }

    public McpTransportType transportType() {
        boolean hasUrl = url != null && !url.isBlank();
        boolean hasCommand = command != null && !command.isBlank();
        if (hasUrl && hasCommand) {
            return null;
        }
        if (hasUrl) {
            return McpTransportType.HTTP;
        }
        if (hasCommand) {
            return McpTransportType.STDIO;
        }
        return null;
    }

    public boolean isValid() {
        return transportType() != null;
    }

    public List<String> commandLine() {
        if (command == null || command.isBlank()) {
            return List.of();
        }
        var line = new java.util.ArrayList<String>();
        line.add(command);
        line.addAll(args);
        return List.copyOf(line);
    }
}
