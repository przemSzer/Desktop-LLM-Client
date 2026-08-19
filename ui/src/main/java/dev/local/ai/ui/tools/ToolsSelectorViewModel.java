package dev.local.ai.ui.tools;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.tools.IToolProvider;
import dev.local.ai.core.tools.ToolsSelectionChangedEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ToolsSelectorViewModel {

    private static final Logger logger = LoggerFactory.getLogger(ToolsSelectorViewModel.class);

    private final ObservableList<ToolItemViewModel> tools;
    private final CoreEventBus eventBus;
    private final IToolProvider toolProvider;

    public ToolsSelectorViewModel(IToolProvider toolProvider, CoreEventBus eventBus) {
        this.eventBus = eventBus;
        this.tools = FXCollections.observableArrayList();
        this.toolProvider = toolProvider;
        loadTools();
    }

    private void loadTools() {
        for (var descriptor : toolProvider.getAllToolDescriptors()) {
            var toolItem = new ToolItemViewModel(descriptor.id(), descriptor.displayName(), false);
            toolItem.enabledProperty().addListener((obs, wasEnabled, isEnabled) -> {
                logger.info("Tool '{}' enabled: {} -> {}", toolItem.getToolId(), wasEnabled, isEnabled);
                publishToolsSelectionChanged();
            });
            tools.add(toolItem);
        }
        logger.info("Loaded {} tools for selection", tools.size());
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
