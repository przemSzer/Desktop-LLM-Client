package dev.local.ai.ui.tools;

import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.tools.IToolProvider;
import dev.local.ai.core.tools.ToolsSelectionChangedEvent;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class ToolsSelectorViewModel {

    private static final Logger logger = LoggerFactory.getLogger(ToolsSelectorViewModel.class);

    private final ObservableList<ToolItemViewModel> tools;
    private final CoreEventBus eventBus;
    private final IToolProvider toolProvider;
    private BiConsumer<String, Boolean> alwaysAllowChangedHandler;
    private boolean refreshingApprovalGate;

    public ToolsSelectorViewModel(IToolProvider toolProvider, CoreEventBus eventBus) {
        this.eventBus = eventBus;
        this.tools = FXCollections.observableArrayList();
        this.toolProvider = toolProvider;
        loadTools();
    }

    public void setOnAlwaysAllowChanged(BiConsumer<String, Boolean> handler) {
        this.alwaysAllowChangedHandler = handler;
    }

    /**
     * Applies the session's always-allowed tool ids to the UI.
     * Tools in the set do not require approval (unlocked / brave mode).
     */
    public void refreshAlwaysAllowed(Set<String> alwaysAllowedToolIds) {
        Set<String> alwaysAllowedTools = alwaysAllowedToolIds == null ? Set.of() : alwaysAllowedToolIds;
        refreshingApprovalGate = true;
        try {
            for (ToolItemViewModel tool : tools) {
                var needApproval = !alwaysAllowedTools.contains(tool.getToolId());
                tool.setRequiresApproval(needApproval);
            }
        } finally {
            refreshingApprovalGate = false;
        }
    }

    private void loadTools() {
        for (var descriptor : toolProvider.getAllToolDescriptors()) {
            var toolItem = new ToolItemViewModel(descriptor.id(), descriptor.displayName(), false);
            toolItem.enabledProperty()
                    .addListener(this::toolEnablingChanged);
            toolItem
                    .requiresApprovalProperty()
                    .addListener((obs, wasRequired, isRequired) -> {
                        logger.info("Tool '{}' requires approval: {} -> {}",
                                toolItem.getToolId(), wasRequired, isRequired);
                        requiresApprovalChangedForItem(isRequired, toolItem);
                    });
            tools.add(toolItem);
        }
        logger.info("Loaded {} tools for selection", tools.size());
    }

    private void requiresApprovalChangedForItem(Boolean isRequired, ToolItemViewModel toolItem) {
        if (refreshingApprovalGate) {
            return;
        }
        boolean alwaysAllowed = !Boolean.TRUE.equals(isRequired);

        if (alwaysAllowChangedHandler != null) {
            alwaysAllowChangedHandler.accept(toolItem.getToolId(), alwaysAllowed);
        }
    }

    private void toolEnablingChanged(ObservableValue<? extends Boolean> obs, boolean wasEnabled, boolean isEnabled) {
        publishToolsSelectionChanged();
    }

    private void publishToolsSelectionChanged() {
        Set<String> enabledToolIds = tools.stream()
            .filter(ToolItemViewModel::isEnabled)
            .map(ToolItemViewModel::getToolId)
            .collect(Collectors.toSet());

        var event = new ToolsSelectionChangedEvent(getClass().getSimpleName(), enabledToolIds);
        eventBus.publish(event);
        logger.debug("Published ToolsSelectionChangedEvent with enabled tools: {}", enabledToolIds);
    }

    public ObservableList<ToolItemViewModel> getTools() {
        return tools;
    }
}
