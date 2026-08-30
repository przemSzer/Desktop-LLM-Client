package dev.local.ai.core.tools.mcp;

import org.jspecify.annotations.NonNull;

public class MCPUtils {
    private MCPUtils() {}

    @NonNull
    public static String toolNameForToolFromMCP(String serverId, String toolName) {
        return serverId + "-" + toolName;
    }
}
