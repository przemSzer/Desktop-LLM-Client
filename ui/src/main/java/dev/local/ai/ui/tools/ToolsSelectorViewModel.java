package dev.local.ai.ui.tools;

import dev.local.ai.core.tools.AppToolsCatalog;
import dev.local.ai.core.tools.ToolDescriptor;
import dev.local.ai.core.tools.mcp.config.McpServerConfig;
import dev.local.ai.core.tools.session.SessionToolSelection;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

public class ToolsSelectorViewModel {

    private static final Logger logger = LoggerFactory.getLogger(ToolsSelectorViewModel.class);

    private final ObservableList<ToolGroupViewModel> groups;
    private final AppToolsCatalog catalog;
    private SessionToolSelection sessionToolSelection;
    private BiConsumer<String, Boolean> alwaysAllowChangedHandler;
    private boolean refreshingApprovalGate;
    private boolean refreshingEnabled;
    private boolean updatingGroup;

    public ToolsSelectorViewModel(AppToolsCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.groups = FXCollections.observableArrayList();
        buildGroups();
    }

    public void bindSession(SessionToolSelection selection) {
        this.sessionToolSelection = selection;
        refreshEnabledFromSession();
        refreshAlwaysAllowed(Set.of());
    }

    public void setOnAlwaysAllowChanged(BiConsumer<String, Boolean> handler) {
        this.alwaysAllowChangedHandler = handler;
    }

    public void refreshAlwaysAllowed(Set<String> alwaysAllowedToolIds) {
        Set<String> alwaysAllowedTools = alwaysAllowedToolIds == null ? Set.of() : alwaysAllowedToolIds;
        refreshingApprovalGate = true;
        try {
            for (ToolGroupViewModel group : groups) {
                for (ToolItemViewModel tool : group.getTools()) {
                    tool.setRequiresApproval(!alwaysAllowedTools.contains(tool.getToolId()));
                }
            }
        } finally {
            refreshingApprovalGate = false;
        }
    }

    public void refreshEnabledFromSession() {
        if (sessionToolSelection == null) {
            return;
        }
        refreshingEnabled = true;
        try {
            Set<String> enabled = sessionToolSelection.enabledToolIds();
            for (ToolGroupViewModel group : groups) {
                if (group.isMcpServer()) {
                    reloadMcpGroupTools(group, false);
                }
                for (ToolItemViewModel tool : group.getTools()) {
                    tool.setEnabled(enabled.contains(tool.getToolId()));
                }
                group.refreshSelectionState();
            }
        } finally {
            refreshingEnabled = false;
        }
    }

    private void buildGroups() {
        ToolGroupViewModel local = new ToolGroupViewModel("local", ToolDescriptor.LOCAL_GROUP, false);
        for (ToolDescriptor descriptor : catalog.localTools()) {
            local.getTools().add(createToolItem(descriptor.id(), descriptor.displayName()));
        }
        local.refreshSelectionState();
        groups.add(local);

        for (McpServerConfig server : catalog.mcpServers()) {
            ToolGroupViewModel group = new ToolGroupViewModel(
                    server.id(),
                    server.displayName(),
                    true
            );
            reloadMcpGroupTools(group, false);
            group.refreshSelectionState();
            groups.add(group);
        }
        logger.info("Loaded {} tool groups", groups.size());
    }

    private void reloadMcpGroupTools(ToolGroupViewModel group, boolean acquireIfMissing) {
        List<ToolDescriptor> known = catalog.knownToolsForServer(group.getGroupId());
        Set<String> existingIds = new HashSet<>();
        for (ToolItemViewModel tool : group.getTools()) {
            existingIds.add(tool.getToolId());
        }
        for (ToolDescriptor descriptor : known) {
            if (!existingIds.contains(descriptor.id())) {
                group.getTools().add(createToolItem(descriptor.id(), descriptor.displayName()));
            }
        }
        if (known.isEmpty() && acquireIfMissing && sessionToolSelection != null) {
            group.setStatusText("Connecting…");
            catalog.mcpRegistry().cacheToolsConnectingIfNeeded(group.getGroupId())
                    .whenComplete((_,ex ) ->
                            groupToolsCached(ex, group)
                    );
        }
    }

    private void groupToolsCached(Throwable error, ToolGroupViewModel group) {
        runOnFxThread(
                () -> {
                    if (error != null) {
                        logger.error("Failed to load tools for MCP server '{}'", group.getGroupId(), error);
                        group.setStatusText("Failed");
                        return;
                    }
                    reloadMcpGroupTools(group, false);
                    refreshEnabledFromSession();
                    group.refreshSelectionState();
                });
    }

    private ToolItemViewModel createToolItem(String toolId, String displayName) {
        var toolItem = new ToolItemViewModel(toolId, displayName, false);
        toolItem.enabledProperty().addListener((obs, was, isEnabled) -> toolEnabledChanged(toolItem, isEnabled));
        toolItem.requiresApprovalProperty().addListener((obs, wasRequired, isRequired) -> {
            if (refreshingApprovalGate) {
                return;
            }
            boolean alwaysAllowed = !Boolean.TRUE.equals(isRequired);
            if (alwaysAllowChangedHandler != null) {
                alwaysAllowChangedHandler.accept(toolItem.getToolId(), alwaysAllowed);
            }
        });
        return toolItem;
    }

    private void toolEnabledChanged(ToolItemViewModel toolItem, boolean isEnabled) {
        if (refreshingEnabled || updatingGroup || sessionToolSelection == null) {
            return;
        }
        sessionToolSelection.setToolEnabled(toolItem.getToolId(), isEnabled)
                .whenComplete((ignored, error) -> runOnFxThread(() -> {
                    if (error != null) {
                        logger.error("Failed to set tool '{}' enabled={}", toolItem.getToolId(), isEnabled, error);
                        refreshingEnabled = true;
                        try {
                            toolItem.setEnabled(!isEnabled);
                        } finally {
                            refreshingEnabled = false;
                        }
                    }
                    for (ToolGroupViewModel group : groups) {
                        group.refreshSelectionState();
                    }
                }));
    }

    public void toggleGroup(ToolGroupViewModel group) {
        if (sessionToolSelection == null || updatingGroup) {
            return;
        }
        boolean enable = group.getSelectionState() != ToolGroupViewModel.SELECTION_ALL;
        updatingGroup = true;
        try {
            if (group.isMcpServer()) {
                if (enable) {
                    group.setStatusText("Connecting…");
                    sessionToolSelection.enableServer(group.getGroupId())
                            .whenComplete((descriptors, error) -> runOnFxThread(() -> {
                                updatingGroup = false;
                                if (error != null) {
                                    logger.error("Failed to enable MCP server '{}'", group.getGroupId(), error);
                                    group.setStatusText("Failed");
                                    return;
                                }
                                reloadMcpGroupTools(group, false);
                                refreshingEnabled = true;
                                try {
                                    for (ToolItemViewModel tool : group.getTools()) {
                                        tool.setEnabled(true);
                                    }
                                } finally {
                                    refreshingEnabled = false;
                                }
                                group.refreshSelectionState();
                            }));
                    return;
                }
                sessionToolSelection.disableServer(group.getGroupId());
                refreshingEnabled = true;
                try {
                    for (ToolItemViewModel tool : group.getTools()) {
                        tool.setEnabled(false);
                    }
                } finally {
                    refreshingEnabled = false;
                }
                group.refreshSelectionState();
            } else {
                for (ToolItemViewModel tool : group.getTools()) {
                    sessionToolSelection.setToolEnabled(tool.getToolId(), enable);
                    tool.setEnabled(enable);
                }
                group.refreshSelectionState();
            }
        } finally {
            if (!group.isMcpServer() || !enable) {
                updatingGroup = false;
            }
        }
    }

    public void onGroupExpanded(ToolGroupViewModel group, boolean expanded) {
        group.setExpanded(expanded);
        if (expanded && group.isMcpServer() && group.getTools().isEmpty()) {
            reloadMcpGroupTools(group, true);
        }
    }

    public ObservableList<ToolGroupViewModel> getGroups() {
        return groups;
    }

    public int enabledToolCount() {
        return groups.stream()
                .flatMap(g -> g.getTools().stream())
                .mapToInt(t -> t.isEnabled() ? 1 : 0)
                .sum();
    }

    private static void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        try {
            Platform.runLater(action);
        } catch (IllegalStateException _) {
            action.run();
        }
    }
}
