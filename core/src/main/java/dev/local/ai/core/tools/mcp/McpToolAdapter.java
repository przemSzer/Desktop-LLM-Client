package dev.local.ai.core.tools.mcp;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.local.ai.core.tools.ITool;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

public class McpToolAdapter implements ITool {

    private static final Logger logger = LoggerFactory.getLogger(McpToolAdapter.class);

    private final McpClient mcpClient;
    private final String serverId;
    private final String originalName;
    private final @NonNull String supportedToolName;

    public McpToolAdapter(McpClient mcpClient, String serverId, String originalName) {
        this.mcpClient = Objects.requireNonNull(mcpClient, "mcpClient");
        this.serverId = serverId;
        this.originalName = originalName;
        this.supportedToolName = MCPUtils.toolNameForToolFromMCP(serverId,originalName);
    }

    @Override
    public Optional<ToolExecutionResultMessage> execute(ToolExecutionRequest request) {
        var toolNameIsInvalid = !supportedToolName.equals(request.name());
        if (toolNameIsInvalid){
            return Optional.empty();
        }
        var requestWithOriginalName = ToolExecutionRequest.builder()
                .name(originalName)
                .arguments(request.arguments())
                .id(request.id())
                .build();
        try {
            logger.debug("Executing MCP tool on server id: {} tool request {}",  serverId, requestWithOriginalName);
            var result = mcpClient.executeTool(requestWithOriginalName);
            return Optional.of(toMessage(request, result));
        } catch (Exception e) {
            logger.error("MCP tool '{}' failed: {}", request.name(), e.getMessage(), e);
            return Optional.of(errorMessage(request, "MCP error: " + e.getMessage()));
        }
    }

    private static ToolExecutionResultMessage toMessage(ToolExecutionRequest request, ToolExecutionResult result) {
        return ToolExecutionResultMessage.builder()
            .id(request.id())
            .toolName(request.name())
            .text(result.resultText())
            .isError(result.isError())
            .build();     
    }

    private static ToolExecutionResultMessage errorMessage(ToolExecutionRequest request, String text) {
        return ToolExecutionResultMessage.builder()
            .id(request.id())
            .toolName(request.name())
            .text(text)
            .isError(true)
            .build();
    }
}
