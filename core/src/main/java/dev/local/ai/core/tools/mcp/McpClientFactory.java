package dev.local.ai.core.tools.mcp;

import dev.langchain4j.mcp.client.McpClient;
import dev.local.ai.core.tools.mcp.config.McpServerConfig;

/**
 * Builds an {@link McpClient} for a configured server.
 * Implementations may talk to the network or spawn a process.
 */
@FunctionalInterface
public interface McpClientFactory {

    McpClient create(McpServerConfig config);
}
