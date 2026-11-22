package dev.local.ai.core.tools.mcp;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpGetPromptResult;
import dev.langchain4j.mcp.client.McpPrompt;
import dev.langchain4j.mcp.client.McpReadResourceResult;
import dev.langchain4j.mcp.client.McpResource;
import dev.langchain4j.mcp.client.McpResourceTemplate;
import dev.langchain4j.mcp.client.McpRoot;
import dev.langchain4j.service.tool.ToolExecutionResult;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Serializes traffic to an MCP client that shares one stdio pipe.
 * Parallel {@code executeTool} calls (and health checks) otherwise race on the
 * server's unicast outbound sink and can kill the child process.
 */
final class SerializingMcpClient implements McpClient {

    private final McpClient delegate;
    private final ReentrantLock lock = new ReentrantLock();

    SerializingMcpClient(McpClient delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public String key() {
        return delegate.key();
    }

    @Override
    public String instructions() {
        return withLock(delegate::instructions);
    }

    @Override
    public List<ToolSpecification> listTools() {
        return withLock(delegate::listTools);
    }

    @Override
    public List<ToolSpecification> listTools(InvocationContext invocationContext) {
        return withLock(() -> delegate.listTools(invocationContext));
    }

    @Override
    public ToolExecutionResult executeTool(ToolExecutionRequest executionRequest) {
        return withLock(() -> delegate.executeTool(executionRequest));
    }

    @Override
    public ToolExecutionResult executeTool(ToolExecutionRequest executionRequest, InvocationContext invocationContext) {
        return withLock(() -> delegate.executeTool(executionRequest, invocationContext));
    }

    @Override
    public List<McpResource> listResources() {
        return withLock(delegate::listResources);
    }

    @Override
    public List<McpResource> listResources(InvocationContext invocationContext) {
        return withLock(() -> delegate.listResources(invocationContext));
    }

    @Override
    public List<McpResourceTemplate> listResourceTemplates() {
        return withLock(delegate::listResourceTemplates);
    }

    @Override
    public List<McpResourceTemplate> listResourceTemplates(InvocationContext invocationContext) {
        return withLock(() -> delegate.listResourceTemplates(invocationContext));
    }

    @Override
    public McpReadResourceResult readResource(String uri) {
        return withLock(() -> delegate.readResource(uri));
    }

    @Override
    public McpReadResourceResult readResource(String uri, InvocationContext invocationContext) {
        return withLock(() -> delegate.readResource(uri, invocationContext));
    }

    @Override
    public void subscribeToResource(String uri) {
        withLockRunnable(() -> delegate.subscribeToResource(uri));
    }

    @Override
    public void unsubscribeFromResource(String uri) {
        withLockRunnable(() -> delegate.unsubscribeFromResource(uri));
    }

    @Override
    public List<McpPrompt> listPrompts() {
        return withLock(delegate::listPrompts);
    }

    @Override
    public McpGetPromptResult getPrompt(String name, Map<String, Object> arguments) {
        return withLock(() -> delegate.getPrompt(name, arguments));
    }

    @Override
    public void checkHealth() {
        withLockRunnable(delegate::checkHealth);
    }

    @Override
    public void setRoots(List<McpRoot> roots) {
        withLockRunnable(() -> delegate.setRoots(roots));
    }

    @Override
    public void close() throws Exception {
        lock.lock();
        try {
            delegate.close();
        } finally {
            lock.unlock();
        }
    }

    private <T> T withLock(Supplier<T> action) {
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    private void withLockRunnable(Runnable action) {
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }
}
