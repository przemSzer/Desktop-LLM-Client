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

/**
 * Manual-testing IToolProvider that combines the built-in tools (web download,
 * command line) with one MCP server reached over Streamable HTTP.
 *
 * <p><b>Not for production use.</b> The MCP client is constructed eagerly in
 * the singleton initializer, with no graceful failure handling beyond a
 * shutdown hook. Replace with the {@code McpRegistry} from sequencing step 3
 * before any UI work depends on it.
 */
public class ToolsProviderWithMCP implements IToolProvider {

    private static final Logger logger = LoggerFactory.getLogger(ToolsProviderWithMCP.class);

    // --- Edit these to point at a different MCP server -------------------
    private static final String MCP_SERVER_ID = "local-mcp";
    private static final String MCP_SERVER_DISPLAY_NAME = "Local MCP";
    private static final String MCP_SERVER_URL = "http://localhost:8088/mcp";
    private static final Duration MCP_TIMEOUT = Duration.ofSeconds(30);
    // --------------------------------------------------------------------

    private final List<ToolDescriptor> descriptors;
    private final McpServerToolProvider mcpServer;

    public ToolsProviderWithMCP() {
        McpClient mcpClient = initializeMCPClient();
        this.mcpServer = new McpServerToolProvider(MCP_SERVER_ID, MCP_SERVER_DISPLAY_NAME, mcpClient);

        var combined = new ArrayList<ToolDescriptor>();
        combined.add(WebPageDownloaderTools.getInstance().toDescriptor());
        combined.add(CommandLineTools.getInstance().toDescriptor());
        combined.addAll(mcpServer.getToolDescriptors());
        this.descriptors = List.copyOf(combined);

        Runtime.getRuntime().addShutdownHook(new Thread(this::closeQuietly, "mcp-shutdown"));

        logger.info("ToolsProviderWithMCP initialized with {} descriptors total", descriptors.size());
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

    private void closeQuietly() {
        try {
            mcpServer.close();
        } catch (Exception e) {
            logger.warn("Failed to close MCP server '{}': {}", MCP_SERVER_ID, e.getMessage());
        }
    }

    private static class InternalInstanceHolder {
        private static final ToolsProviderWithMCP INSTANCE = new ToolsProviderWithMCP();
    }

    public static IToolProvider getInstance() {
        return InternalInstanceHolder.INSTANCE;
    }

    @Override
    public List<ToolDescriptor> getToolDescriptors() {
        return descriptors;
    }
}
