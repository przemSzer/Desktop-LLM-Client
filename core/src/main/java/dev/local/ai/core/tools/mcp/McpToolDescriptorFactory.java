package dev.local.ai.core.tools.mcp;

import dev.langchain4j.mcp.client.McpClient;
import dev.local.ai.core.tools.ToolDescriptor;
import dev.local.ai.core.tools.mcp.config.McpServerConfig;

import java.util.List;

@FunctionalInterface
public interface McpToolDescriptorFactory {

    List<ToolDescriptor> create(McpServerConfig config, McpClient client);
}
