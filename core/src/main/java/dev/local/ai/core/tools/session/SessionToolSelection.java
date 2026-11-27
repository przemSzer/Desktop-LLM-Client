package dev.local.ai.core.tools.session;

import dev.local.ai.core.tools.AppToolsCatalog;
import dev.local.ai.core.tools.IToolProvider;
import dev.local.ai.core.tools.SessionFilteringToolProvider;
import dev.local.ai.core.tools.ToolDescriptor;
import dev.local.ai.core.tools.mcp.McpConnectionRegistry;
import dev.local.ai.core.tools.settings.ToolSettings;
import dev.local.ai.core.tools.settings.ToolSettingsWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-chat enabled tools. Acquires MCP servers when their tools are enabled
 * and releases them when the last tool from that server is turned off or the session closes.
 */
public final class SessionToolSelection implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(SessionToolSelection.class);

    private final AppToolsCatalog catalog;
    private final McpConnectionRegistry registry;
    private final ToolSettingsWriter writer;
    private final Path settingsFile;
    private final SessionToolApprovalConfiguration approval;

    private final Set<String> enabledToolIds;
    private final Map<String, List<ToolDescriptor>> acquiredServers = new ConcurrentHashMap<>();
    private final IToolProvider toolProvider;

    public SessionToolSelection(AppToolsCatalog catalog,
                                ToolSettingsWriter writer,
                                Path settingsFile,
                                SessionToolApprovalConfiguration approval,
                                Set<String> initialEnabledToolIds) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.registry = catalog.mcpRegistry();
        this.writer = Objects.requireNonNull(writer, "writer");
        this.settingsFile = Objects.requireNonNull(settingsFile, "settingsFile");
        this.approval = Objects.requireNonNull(approval, "approval");
        this.enabledToolIds = initialEnabledToolIds == null
                ? new LinkedHashSet<>()
                : initialEnabledToolIds;
        this.toolProvider = new SessionFilteringToolProvider(
                catalog,
                () -> Set.copyOf(enabledToolIds),
                this::acquiredMcpTools
        );
    }

    public IToolProvider toolProvider() {
        return toolProvider;
    }

    public Set<String> enabledToolIds() {
        return Set.copyOf(enabledToolIds);
    }

    /**
     * Restores servers that have tools in the enabled set. Call after construction.
     */
    public CompletableFuture<Void> restoreAcquiredServers() {
        Set<String> serverIds = new LinkedHashSet<>();
        for (String toolId : enabledToolIds) {
            serverIdForTool(toolId).ifPresent(serverIds::add);
        }
        if (serverIds.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (String serverId : serverIds) {
            futures.add(acquireServer(serverId).thenAccept(ignored -> {
            }));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    public CompletableFuture<List<ToolDescriptor>> enableServer(String serverId) {
        return acquireServer(serverId).thenApply(descriptors -> {
            boolean changed = false;
            for (ToolDescriptor descriptor : descriptors) {
                changed |= enabledToolIds.add(descriptor.id());
            }
            if (changed) {
                persist();
            }
            return descriptors;
        });
    }

    public void disableServer(String serverId) {
        String prefix = serverId + "-";
        boolean changed = enabledToolIds.removeIf(id -> id.startsWith(prefix));
        releaseServer(serverId);
        if (changed) {
            persist();
        }
    }

    public CompletableFuture<Void> setToolEnabled(String toolId, boolean enabled) {
        if (toolId == null || toolId.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }
        if (isLocalTool(toolId)) {
            boolean changed = enabled ? enabledToolIds.add(toolId) : enabledToolIds.remove(toolId);
            if (changed) {
                persist();
            }
            return CompletableFuture.completedFuture(null);
        }
        var serverId = serverIdForTool(toolId);
        if (serverId.isEmpty()) {
            logger.warn("Cannot toggle unknown tool '{}'", toolId);
            return CompletableFuture.completedFuture(null);
        }
        String id = serverId.get();
        if (enabled) {
            return acquireServer(id).thenAccept(descriptors -> {
                boolean known = descriptors.stream().anyMatch(d -> d.id().equals(toolId));
                if (!known) {
                    logger.warn("Tool '{}' not exposed by MCP server '{}'", toolId, id);
                    return;
                }
                if (enabledToolIds.add(toolId)) {
                    persist();
                }
            });
        }
        boolean changed = enabledToolIds.remove(toolId);
        if (!hasEnabledToolForServer(id)) {
            releaseServer(id);
        }
        if (changed) {
            persist();
        }
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<List<ToolDescriptor>> acquireServer(String serverId) {
        if (acquiredServers.containsKey(serverId)) {
            return CompletableFuture.completedFuture(acquiredServers.get(serverId));
        }
        return registry.acquire(serverId).thenApply(descriptors -> {
            acquiredServers.put(serverId, List.copyOf(descriptors));
            return descriptors;
        });
    }

    private void releaseServer(String serverId) {
        if (acquiredServers.remove(serverId) != null) {
            registry.release(serverId);
        }
    }

    private boolean hasEnabledToolForServer(String serverId) {
        String prefix = serverId + "-";
        return enabledToolIds.stream().anyMatch(id -> id.startsWith(prefix));
    }

    private boolean isLocalTool(String toolId) {
        return catalog.localTools().stream().anyMatch(d -> d.id().equals(toolId));
    }

    private java.util.Optional<String> serverIdForTool(String toolId) {
        for (var config : registry.configuredServers()) {
            String prefix = config.id() + "-";
            if (toolId.startsWith(prefix)) {
                return java.util.Optional.of(config.id());
            }
        }
        return java.util.Optional.empty();
    }

    private List<ToolDescriptor> acquiredMcpTools() {
        List<ToolDescriptor> tools = new ArrayList<>();
        for (List<ToolDescriptor> descriptors : acquiredServers.values()) {
            tools.addAll(descriptors);
        }
        return List.copyOf(tools);
    }

    private void persist() {
        writer.write(
                settingsFile,
                new ToolSettings(
                        approval.alwaysAllowedTools().stream().toList(),
                        enabledToolIds.stream().toList()
                )
        );
    }

    @Override
    public void close() {
        for (String serverId : Set.copyOf(acquiredServers.keySet())) {
            releaseServer(serverId);
        }
        enabledToolIds.clear();
    }
}
