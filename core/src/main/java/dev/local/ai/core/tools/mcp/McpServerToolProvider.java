package dev.local.ai.core.tools.mcp;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import dev.local.ai.core.tools.IToolProvider;
import dev.local.ai.core.tools.ToolDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class McpServerToolProvider implements IToolProvider, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(McpServerToolProvider.class);

    private final String serverId;
    private final String serverDisplayName;
    private final McpClient mcpClient;

    private List<ToolDescriptor> descriptorsCache;

    public McpServerToolProvider(String serverId, String serverDisplayName, McpClient mcpClient) {
        this.serverId = Objects.requireNonNull(serverId, "serverId");
        this.serverDisplayName = Objects.requireNonNull(serverDisplayName, "serverDisplayName");
        this.mcpClient = Objects.requireNonNull(mcpClient, "mcpClient");
    }

    @Override
    public List<ToolDescriptor> getToolDescriptors() {
        var local = descriptorsCache;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (descriptorsCache == null) {
                descriptorsCache = buildDescriptors();
            }
            return descriptorsCache;
        }
    }

    private List<ToolDescriptor> buildDescriptors() {
        List<ToolSpecification> specs = mcpClient.listTools();
        if (specs.isEmpty()) {
            logger.info("MCP server '{}' exposed no tools", serverId);
            return List.of();
        }

        List<ToolDescriptor> descriptors = specs.stream()
            .map(this::toToolDescriptor)
            .toList();

        logger.info("MCP server '{}' exposed {} tool(s)", serverId, descriptors.size());
        return descriptors;
    }

    private ToolDescriptor toToolDescriptor(ToolSpecification spec) {
        var toolName = MCPUtils.toolNameForToolFromMCP(serverId,spec.name());
        var spectWithChangedName = ToolSpecification.builder()
                .name(toolName)
                .description(spec.description())
                .parameters(spec.parameters())
                .metadata(spec.metadata())
                .build();
        return new ToolDescriptor(
                toolName,
                serverDisplayName + ": " + spec.name(),
                spectWithChangedName,
                new McpToolAdapter(mcpClient, serverId, spec.name())
        );
    }

    public String getServerId() {
        return serverId;
    }

    @Override
    public void close() throws Exception {
        logger.info("Closing MCP client for server '{}'", serverId);
        mcpClient.close();
    }
}
