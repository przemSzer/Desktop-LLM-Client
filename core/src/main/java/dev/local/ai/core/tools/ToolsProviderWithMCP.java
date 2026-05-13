package dev.local.ai.core.tools;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.local.ai.core.tools.local.CommandLineTools;
import dev.local.ai.core.tools.mcp.McpServerToolProvider;
import dev.local.ai.core.tools.web.WebPageDownloaderTools;


public class ToolsProviderWithMCP implements IToolProvider, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ToolsProviderWithMCP.class);

    private static final String MCP_SERVER_ID = "local-mcp";
    private static final String MCP_SERVER_DISPLAY_NAME = "Local MCP";
    private static final String MCP_SERVER_URL = "http://localhost:8088/mcp";
    private static final Duration MCP_TIMEOUT = Duration.ofSeconds(30);

    private List<ToolDescriptor> descriptors;
    private McpServerToolProvider mcpServer;

    public ToolsProviderWithMCP() {        
        try {
            this.descriptors = new ArrayList<ToolDescriptor>();
            this.descriptors.add(WebPageDownloaderTools.getInstance().toDescriptor());
            this.descriptors.add(CommandLineTools.getInstance().toDescriptor());
            logger.info("Initializing MCP client");
            McpClient mcpClient = initializeMCPClient();

            this.mcpServer = new McpServerToolProvider(MCP_SERVER_ID, MCP_SERVER_DISPLAY_NAME, mcpClient);
            if (this.mcpServer != null) {
                this.descriptors.addAll(this.mcpServer.getToolDescriptors());
            }
            logger.info("ToolsProviderWithMCP initialized with {} descriptors total", descriptors.size());
        } catch (Exception e) {
            logger.error("Error initializing ToolsProviderWithMCP, will use only local tools", e);
        }
    }

    private McpClient initializeMCPClient() {
        logger.info("Connecting to MCP server '{}' at {}", MCP_SERVER_ID, MCP_SERVER_URL);
        McpTransport transport = new StreamableHttpMcpTransport.Builder()
            .url(MCP_SERVER_URL)
            .timeout(MCP_TIMEOUT)
            .logRequests(true)
            .logResponses(true)
            .build();

        return new DefaultMcpClient.Builder()
            .key(MCP_SERVER_ID)
            .transport(transport)
            .toolExecutionTimeout(MCP_TIMEOUT)
            .autoHealthCheck(true)
            .build();
    }

    @Override
    public void close() {
        if (mcpServer == null) {
            return;
        }
        try {
            logger.info("Closing MCP server '{}'", MCP_SERVER_ID);
            mcpServer.close();
        } catch (Exception e) {
            logger.warn("Failed to close MCP server '{}': {}", MCP_SERVER_ID, e.getMessage());
        }
    }

    @Override
    public List<ToolDescriptor> getToolDescriptors() {
        return descriptors;
    }
}
