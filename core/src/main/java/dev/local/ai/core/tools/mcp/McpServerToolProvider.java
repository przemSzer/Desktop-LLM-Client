package dev.local.ai.core.tools.mcp;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import dev.local.ai.core.tools.IToolProvider;
import dev.local.ai.core.tools.ToolDescriptor;

/**
 * Exposes the tools of a single MCP server as {@link IToolProvider} descriptors.
 *
 * <p>Granularity: one {@link ToolDescriptor} per remote tool (Option B). The
 * descriptor id is {@code serverId + "/" + toolName} which guarantees
 * uniqueness across servers and lets {@code FilterableToolProvider} key its
 * UI toggles per individual tool.
 *
 * <p>The result of {@link McpClient#listTools()} is cached on first access –
 * it is a remote round-trip that must not be repeated for every chat turn.
 * If the server's tool catalogue changes at runtime, a new instance of this
 * provider should be created (handled by the registry in a later step).
 */
public class McpServerToolProvider implements IToolProvider, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(McpServerToolProvider.class);

    private final String serverId;
    private final String serverDisplayName;
    private final McpClient mcpClient;

    private volatile List<ToolDescriptor> descriptorsCache;

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
        return new ToolDescriptor(
                serverId + "/" + spec.name(),
                serverDisplayName + ": " + spec.name(),
                List.of(spec),
                new McpToolExecutorAdapter(mcpClient, Set.of(spec.name()))
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
