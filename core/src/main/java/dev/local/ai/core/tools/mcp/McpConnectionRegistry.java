package dev.local.ai.core.tools.mcp;

import dev.langchain4j.mcp.client.McpClient;
import dev.local.ai.core.tools.ToolDescriptor;
import dev.local.ai.core.tools.mcp.config.McpServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * At most one {@link McpClient} per server id. Sessions acquire and release;
 * the client starts on the first acquire and closes on the last release
 * after in-flight tool calls finish.
 */
public class McpConnectionRegistry implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(McpConnectionRegistry.class);

    private final Map<String, McpServerConfig> configsById;
    private final McpClientFactory clientFactory;
    private final McpToolDescriptorFactory descriptorFactory;
    private final Executor connectExecutor;
    private final boolean ownsExecutor;

    private final ReentrantLock serversInitialisationLock = new ReentrantLock();
    private final Map<String, RunningServer> running = new LinkedHashMap<>();
    private final Map<String, List<McpCachedTool>> toolListCache = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Void>> connectionInProgress = new ConcurrentHashMap<>();

    private volatile boolean closed;

    public McpConnectionRegistry(List<McpServerConfig> configs, McpClientFactory clientFactory) {
        this(
                configs,
                clientFactory,
                null,
                Executors.newThreadPerTaskExecutor(
                        Thread.ofVirtual()
                                .name("MCP-Connection-Registry-", 0)
                                .factory()
                ),
                true
        );
    }

    public McpConnectionRegistry(List<McpServerConfig> configs,
                                 McpClientFactory clientFactory,
                                 Executor connectExecutor) {
        this(configs, clientFactory, null, connectExecutor, false);
    }

    public McpConnectionRegistry(List<McpServerConfig> configs,
                                 McpClientFactory clientFactory,
                                 McpToolDescriptorFactory descriptorFactory,
                                 Executor connectExecutor) {
        this(configs, clientFactory, descriptorFactory, connectExecutor, false);
    }

    private McpConnectionRegistry(List<McpServerConfig> configs,
                                  McpClientFactory clientFactory,
                                  McpToolDescriptorFactory descriptorFactory,
                                  Executor connectExecutor,
                                  boolean ownsExecutor) {
        Objects.requireNonNull(configs, "configs");
        this.clientFactory = Objects.requireNonNull(clientFactory, "clientFactory");
        this.connectExecutor = Objects.requireNonNull(connectExecutor, "connectExecutor");
        this.ownsExecutor = ownsExecutor;
        Map<String, McpServerConfig> map = new LinkedHashMap<>();
        for (McpServerConfig config : configs) {
            if (config != null && config.isValid() && config.isEnabled()) {
                map.put(config.id(), config);
            }
        }
        this.configsById = Map.copyOf(map);
        this.descriptorFactory = descriptorFactory != null
                ? descriptorFactory
                : defaultDescriptorFactory();
    }

    private McpToolDescriptorFactory defaultDescriptorFactory() {
        return new DefaultMcpToolDescriptorFactory((client, binding) ->
                new HeldMcpTool(
                        this,
                        binding.serverId(),
                        new McpToolAdapter(client, binding.serverId(), binding.originalToolName())
                )
        );
    }

    public List<McpServerConfig> configuredServers() {
        return List.copyOf(configsById.values());
    }

    public Optional<List<McpCachedTool>> cachedTools(String serverId) {
        List<McpCachedTool> cached = toolListCache.get(serverId);
        if (cached == null) {
            return Optional.empty();
        }
        return Optional.of(List.copyOf(cached));
    }

    /**
     * Tools from servers that currently have an open client.
     */
    public List<ToolDescriptor> connectedToolDescriptors() {
        serversInitialisationLock.lock();
        try {
            return running.values().stream()
                    .flatMap(server -> server.descriptors().stream())
                    .toList();
        } finally {
            serversInitialisationLock.unlock();
        }
    }

    /**
     * Increments the session reference for {@code serverId}. Starts the client on the first acquire.
     */
    public CompletableFuture<List<ToolDescriptor>> acquire(String serverId) {
        if (closed) {
            return CompletableFuture.failedFuture(new IllegalStateException("MCP registry is closed"));
        }
        if (!configsById.containsKey(serverId)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Unknown or disabled MCP server: " + serverId)
            );
        }
        return ensureStarted(serverId).thenApply(ignored -> {
            serversInitialisationLock.lock();
            try {
                RunningServer started = running.get(serverId);
                if (started == null) {
                    throw new IllegalStateException("MCP server '" + serverId + "' failed to start");
                }
                started.sessionRefs().incrementAndGet();
                logger.info(
                        "Acquired MCP server '{}' (sessionRefs={})",
                        serverId,
                        started.sessionRefs().get()
                );
                return started.descriptors();
            } finally {
                serversInitialisationLock.unlock();
            }
        });
    }
    /**
     * Ensures the tool-name cache is filled. Starts the server if needed and closes it again
     * when no session holds a reference.
     */
    public CompletableFuture<List<McpCachedTool>> cacheToolsConnectingIfNeeded(String serverId) {
        Optional<List<McpCachedTool>> cached = cachedTools(serverId);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached.get());
        }
        return ensureStarted(serverId)
                .thenApply(ignored -> {
                    closeIfUnused(serverId);
                    return cachedTools(serverId).orElse(List.of());
                });
    }

    public void release(String serverId) {
        McpClient toClose = null;
        serversInitialisationLock.lock();
        try {
            RunningServer existing = running.get(serverId);
            if (existing == null) {
                return;
            }
            if (existing.sessionRefs().get() > 0) {
                existing.sessionRefs().decrementAndGet();
            }
            logger.info(
                    "Released MCP server '{}' (sessionRefs={}, inFlight={})",
                    serverId,
                    existing.sessionRefs().get(),
                    existing.inFlight().get()
            );
            if (existing.sessionRefs().get() == 0 && existing.inFlight().get() == 0) {
                running.remove(serverId);
                toClose = existing.client();
            }
        } finally {
            serversInitialisationLock.unlock();
        }
        closeClientQuietly(serverId, toClose);
    }

    public boolean isRunning(String serverId) {
        serversInitialisationLock.lock();
        try {
            RunningServer existing = running.get(serverId);
            return existing != null && existing.sessionRefs().get() > 0;
        } finally {
            serversInitialisationLock.unlock();
        }
    }

    private CompletableFuture<Void> ensureStarted(String serverId) {
        McpServerConfig config = configsById.get(serverId);
        if (config == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Unknown or disabled MCP server: " + serverId)
            );
        }
        serversInitialisationLock.lock();
        try {
            boolean alreadyRunning = running.containsKey(serverId);
            if (alreadyRunning) {
                return CompletableFuture.completedFuture(null);
            }
            var connectionIsInProgress = this.connectionInProgress.get(serverId);
            if (connectionIsInProgress != null) {
                return connectionIsInProgress;
            }
            var connectionFinishedFuture = new CompletableFuture<Void>();
            this.connectionInProgress.put(serverId, connectionFinishedFuture);
            connectExecutor.execute(() -> startServer(config, connectionFinishedFuture));
            return connectionFinishedFuture;
        } finally {
            serversInitialisationLock.unlock();
        }
    }

    private void closeIfUnused(String serverId) {
        McpClient toClose = null;
        serversInitialisationLock.lock();
        try {
            RunningServer existing = running.get(serverId);
            if (existing == null) {
                return;
            }
            if (existing.sessionRefs().get() == 0 && existing.inFlight().get() == 0) {
                running.remove(serverId);
                toClose = existing.client();
                logger.info("Closing unused MCP server '{}' after tool-list refresh", serverId);
            }
        } finally {
            serversInitialisationLock.unlock();
        }
        closeClientQuietly(serverId, toClose);
    }

    private void startServer(McpServerConfig config, CompletableFuture<Void> connectionFinishedFuture) {
        String serverId = config.id();
        try {
            logger.debug("Connecting to a MCP server '{}'", serverId);
            McpClient client = clientFactory.create(config);
            List<ToolDescriptor> descriptors = descriptorFactory.create(config, client);
            serversInitialisationLock.lock();
            try {
                if (closed) {
                    closeClientQuietly(serverId, client);
                    connectionFinishedFuture.completeExceptionally(new IllegalStateException("MCP registry is closed"));
                    return;
                }
                running.put(serverId, new RunningServer(client, descriptors));
                toolListCache.put(serverId, toCachedTools(descriptors));
                connectionInProgress.remove(serverId);
                connectionFinishedFuture.complete(null);
            } finally {
                serversInitialisationLock.unlock();
            }
            logger.info("MCP server '{}' started with {} tool(s)", serverId, descriptors.size());
        } catch (Exception e) {
            serversInitialisationLock.lock();
            try {
                connectionInProgress.remove(serverId);
            } finally {
                serversInitialisationLock.unlock();
            }
            logger.error("Failed to start MCP server '{}': {}", serverId, e.getMessage(), e);
            connectionFinishedFuture.completeExceptionally(e);
        }
    }

    private static List<McpCachedTool> toCachedTools(List<ToolDescriptor> descriptors) {
        return descriptors.stream()
                .map(d -> new McpCachedTool(d.id(), d.displayName(), d.group()))
                .toList();
    }

    void beginExecution(String serverId) {
        serversInitialisationLock.lock();
        try {
            RunningServer existing = running.get(serverId);
            if (existing == null) {
                throw new IllegalStateException("MCP server '" + serverId + "' is not running");
            }
            existing.inFlight().incrementAndGet();
        } finally {
            serversInitialisationLock.unlock();
        }
    }

    void endExecution(String serverId) {
        McpClient toClose = null;
        serversInitialisationLock.lock();
        try {
            RunningServer existing = running.get(serverId);
            if (existing == null) {
                return;
            }
            if (existing.inFlight().get() > 0) {
                existing.inFlight().decrementAndGet();
            }
            if (existing.sessionRefs().get() == 0 && existing.inFlight().get() == 0) {
                running.remove(serverId);
                toClose = existing.client();
            }
        } finally {
            serversInitialisationLock.unlock();
        }
        closeClientQuietly(serverId, toClose);
    }

    private static void closeClientQuietly(String serverId, McpClient client) {
        if (client == null) {
            return;
        }
        try {
            logger.info("Closing MCP client for server '{}'", serverId);
            client.close();
        } catch (Exception e) {
            logger.warn("Failed to close MCP server '{}': {}", serverId, e.getMessage());
        }
    }

    @Override
    public void close() {
        closed = true;
        List<McpClient> toClose = new ArrayList<>();
        serversInitialisationLock.lock();
        try {
            for (var entry : running.entrySet()) {
                toClose.add(entry.getValue().client());
            }
            running.clear();
            connectionInProgress.values().forEach(f -> f.completeExceptionally(
                    new IllegalStateException("MCP registry is closed")
            ));
            connectionInProgress.clear();
        } finally {
            serversInitialisationLock.unlock();
        }
        for (McpClient client : toClose) {
            try {
                client.close();
            } catch (Exception e) {
                logger.warn("Failed to close MCP client during shutdown: {}", e.getMessage());
            }
        }
        if (ownsExecutor && connectExecutor instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                logger.warn("Failed to close MCP connect executor: {}", e.getMessage());
            }
        }
    }

    private record RunningServer(
            McpClient client,
            List<ToolDescriptor> descriptors,
            AtomicInteger sessionRefs,
            AtomicInteger inFlight
    ) {
        private RunningServer(McpClient client, List<ToolDescriptor> descriptors) {
            this(client, List.copyOf(descriptors), new AtomicInteger(), new AtomicInteger());
        }
    }
}
