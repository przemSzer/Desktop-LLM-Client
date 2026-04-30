package dev.local.ai.core.tools.mcp;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;
import dev.local.ai.core.tools.IToolExecutor;

/**
 * Bridges a single MCP server (represented by an {@link McpClient}) to the
 * application's {@link IToolExecutor} contract.
 *
 * <p>Routing rule: this executor returns a non-empty {@link Optional} only for
 * tool names it actually owns. {@code StreamingChat#executeTools} flat-maps the
 * results across all executors, so non-owned requests must be silently skipped
 * to avoid duplicate execution.
 *
 * <p>Error propagation:
 * <ul>
 *   <li>If the underlying MCP call throws (transport error, server crash) we
 *       still return a non-empty result, marked {@code isError=true}, so the
 *       chat memory keeps a coherent tool-call/tool-result pair.</li>
 *   <li>If the MCP server returns a {@link ToolExecutionResult} with
 *       {@code isError=true} we surface that flag instead of throwing.</li>
 * </ul>
 */
public class McpToolExecutorAdapter implements IToolExecutor {

    private static final Logger logger = LoggerFactory.getLogger(McpToolExecutorAdapter.class);

    private final McpClient mcpClient;
    private final Set<String> ownedToolNames;

    public McpToolExecutorAdapter(McpClient mcpClient, Set<String> ownedToolNames) {
        this.mcpClient = Objects.requireNonNull(mcpClient, "mcpClient");
        this.ownedToolNames = Set.copyOf(Objects.requireNonNull(ownedToolNames, "ownedToolNames"));
    }

    @Override
    public Optional<ToolExecutionResultMessage> execute(ToolExecutionRequest request) {
        if (!ownedToolNames.contains(request.name())) {
            return Optional.empty();
        }
        try {
            ToolExecutionResult result = mcpClient.executeTool(request);
            return Optional.of(toMessage(request, result));
        } catch (Exception e) {
            logger.error("MCP tool '{}' failed: {}", request.name(), e.getMessage(), e);
            return Optional.of(errorMessage(request, "MCP error: " + e.getMessage()));
        }
    }

    private static ToolExecutionResultMessage toMessage(ToolExecutionRequest request, ToolExecutionResult result) {
        // return ToolExecutionResultMessage.from(request, result.resultText());   
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
