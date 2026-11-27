package dev.local.ai.core.tools.mcp;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.local.ai.core.tools.mcp.config.McpServerConfig;
import dev.local.ai.core.tools.mcp.config.McpTransportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

public class DefaultMcpClientFactory implements McpClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMcpClientFactory.class);

    @Override
    public McpClient create(McpServerConfig config) {
        Objects.requireNonNull(config, "config");
        if (!config.isValid()) {
            throw new IllegalArgumentException(
                    "MCP server '" + config.id() + "' must set either url or command"
            );
        }
        Duration timeout = Duration.ofSeconds(config.resolvedTimeoutSeconds());
        McpTransport transport = buildTransport(config, timeout);
        logger.info(
                "Creating MCP client for server '{}' ({})",
                config.id(),
                config.transportType()
        );
        McpClient client = new DefaultMcpClient.Builder()
                .key(config.id())
                .transport(transport)
                .initializationTimeout(timeout)
                .toolExecutionTimeout(timeout)
                .autoHealthCheck(true)
                .build();
        if (config.shouldSerializeRequests()) {
            // Opt-in for servers that break under concurrent JSON-RPC (e.g. some Spring AI stdio stacks).
            return new SerializingMcpClient(client);
        }
        return client;
    }

    private static McpTransport buildTransport(McpServerConfig config, Duration timeout) {
        if (config.transportType() == McpTransportType.HTTP) {
            return new StreamableHttpMcpTransport.Builder()
                    .url(config.url())
                    .customHeaders(config.headers())
                    .timeout(timeout)
                    .logRequests(true)
                    .logResponses(true)
                    .build();
        }
        var builder = new StdioMcpTransport.Builder()
                .command(config.commandLine())
                .environment(config.env())
                .logEvents(true);
        if (config.workingDirectory() != null && !config.workingDirectory().isBlank()) {
            builder.workingDirectory(Path.of(config.workingDirectory()));
        }
        return builder.build();
    }
}
