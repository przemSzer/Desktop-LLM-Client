package dev.local.ai.core.tools.mcp;

/**
 * Tool name cached after a successful {@code listTools}, for the menu
 * when the server is not currently running.
 */
public record McpCachedTool(String id, String displayName, String group) {
}
