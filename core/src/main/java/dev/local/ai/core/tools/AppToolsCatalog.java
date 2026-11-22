package dev.local.ai.core.tools;

import dev.local.ai.core.tools.local.CommandLineTools;
import dev.local.ai.core.tools.mcp.McpCachedTool;
import dev.local.ai.core.tools.mcp.McpConnectionRegistry;
import dev.local.ai.core.tools.mcp.config.McpServerConfig;
import dev.local.ai.core.tools.web.WebPageDownloaderTools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Built-in tools plus the MCP registry. Does not connect MCP servers at construction.
 */
public class AppToolsCatalog implements IToolProvider, AutoCloseable {

    private final List<ToolDescriptor> localTools;
    private final McpConnectionRegistry mcpRegistry;

    public AppToolsCatalog(McpConnectionRegistry mcpRegistry) {
        this.mcpRegistry = Objects.requireNonNull(mcpRegistry, "mcpRegistry");
        this.localTools = List.of(
                WebPageDownloaderTools.getInstance().toDescriptor(),
                CommandLineTools.getInstance().toDescriptor()
        );
    }

    public McpConnectionRegistry mcpRegistry() {
        return mcpRegistry;
    }

    public List<ToolDescriptor> localTools() {
        return localTools;
    }

    public List<McpServerConfig> mcpServers() {
        return mcpRegistry.configuredServers();
    }

    /**
     * Built-in tools and tools from MCP servers that currently have an open client.
     */
    @Override
    public List<ToolDescriptor> getToolDescriptors() {
        List<ToolDescriptor> all = new ArrayList<>(localTools);
        all.addAll(mcpRegistry.connectedToolDescriptors());
        return List.copyOf(all);
    }

    @Override
    public List<ToolDescriptor> getAllToolDescriptors() {
        return getToolDescriptors();
    }

    /**
     * Tools known for a server: live descriptors if the client is open, otherwise cached names.
     * Cached entries have a no-op executor and are for the menu only.
     */
    public List<ToolDescriptor> knownToolsForServer(String serverId) {
        String prefix = serverId + "-";
        var connected = mcpRegistry
                .connectedToolDescriptors()
                .stream()
                .filter(d -> d.id().startsWith(prefix))
                .toList();
        if (!connected.isEmpty()) {
            return connected;
        }
        return mcpRegistry.cachedTools(serverId)
                .orElse(List.of())
                .stream()
                .map(this::toDisplayOnlyDescriptor)
                .toList();
    }

    private ToolDescriptor toDisplayOnlyDescriptor(McpCachedTool cached) {
        var spec = dev.langchain4j.agent.tool.ToolSpecification.builder()
                .name(cached.id())
                .description(cached.displayName())
                .build();
        return new ToolDescriptor(
                cached.id(),
                cached.displayName(),
                cached.group(),
                spec,
                request -> java.util.Optional.empty()
        );
    }

    @Override
    public void close() {
        mcpRegistry.close();
    }
}
