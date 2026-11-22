package dev.local.ai.core.tools.mcp;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.local.ai.core.tools.ITool;

import java.util.Optional;

/**
 * Keeps the MCP client alive for the duration of a tool call even if the
 * session has already released the server.
 */
final class HeldMcpTool implements ITool {

    private final McpConnectionRegistry registry;
    private final String serverId;
    private final ITool delegate;

    HeldMcpTool(McpConnectionRegistry registry, String serverId, ITool delegate) {
        this.registry = registry;
        this.serverId = serverId;
        this.delegate = delegate;
    }

    @Override
    public Optional<ToolExecutionResultMessage> execute(ToolExecutionRequest request) {
        registry.beginExecution(serverId);
        try {
            return delegate.execute(request);
        } finally {
            registry.endExecution(serverId);
        }
    }
}
