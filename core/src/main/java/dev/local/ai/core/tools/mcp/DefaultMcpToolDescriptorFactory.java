package dev.local.ai.core.tools.mcp;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import dev.local.ai.core.tools.ITool;
import dev.local.ai.core.tools.ToolDescriptor;
import dev.local.ai.core.tools.mcp.config.McpServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Builds {@link ToolDescriptor}s from an MCP client's tool list.
 * By default each tool uses {@link McpToolAdapter}; callers may supply another executor factory
 * (for example to wrap with {@link HeldMcpTool}).
 */
public class DefaultMcpToolDescriptorFactory implements McpToolDescriptorFactory {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMcpToolDescriptorFactory.class);

    private final BiFunction<McpClient, ToolBinding, ITool> toolFactory;

    public DefaultMcpToolDescriptorFactory() {
        this((client, binding) -> new McpToolAdapter(client, binding.serverId(), binding.originalToolName()));
    }

    public DefaultMcpToolDescriptorFactory(BiFunction<McpClient, ToolBinding, ITool> toolFactory) {
        this.toolFactory = Objects.requireNonNull(toolFactory, "toolFactory");
    }

    @Override
    public List<ToolDescriptor> create(McpServerConfig config, McpClient client) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(client, "client");
        List<ToolSpecification> specs = client.listTools();
        if (specs.isEmpty()) {
            logger.info("MCP server '{}' exposed no tools", config.id());
            return List.of();
        }
        List<ToolDescriptor> descriptors = new ArrayList<>(specs.size());
        for (ToolSpecification spec : specs) {
            String toolName = MCPUtils.toolNameForToolFromMCP(config.id(), spec.name());
            ToolSpecification renamed = ToolSpecification.builder()
                    .name(toolName)
                    .description(spec.description())
                    .parameters(spec.parameters())
                    .metadata(spec.metadata())
                    .build();
            ITool executor = toolFactory.apply(client, new ToolBinding(config.id(), spec.name()));
            descriptors.add(new ToolDescriptor(
                    toolName,
                    spec.name(),
                    config.displayName(),
                    renamed,
                    executor
            ));
        }
        return List.copyOf(descriptors);
    }

    public record ToolBinding(String serverId, String originalToolName) {
        public ToolBinding {
            Objects.requireNonNull(serverId, "serverId");
            Objects.requireNonNull(originalToolName, "originalToolName");
        }
    }
}
