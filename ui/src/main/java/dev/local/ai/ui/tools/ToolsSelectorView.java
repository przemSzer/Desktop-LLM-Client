package dev.local.ai.ui.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.tools.IToolProvider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;

public class ToolsSelectorView extends FlowPane {

    private static final Logger logger = LoggerFactory.getLogger(ToolsSelectorView.class);

    private ToolsSelectorViewModel viewModel;

    public ToolsSelectorView() {
        setHgap(10.0);
        setVgap(10.0);
        addPlaceholderButtons();
    }

    public void init(IToolProvider toolProvider, CoreEventBus eventBus) {
        getChildren().clear();
        this.viewModel = new ToolsSelectorViewModel(toolProvider, eventBus);
        buildToggleButtons();
        logger.info("ToolsSelectorView initialized with {} tools", viewModel.getTools().size());
    }

    private void addPlaceholderButtons() {
        getChildren().addAll(
                new ToggleButton("Download web page"),
                new ToggleButton("Execute local commands"),
                new ToggleButton("Execute code"),
                new ToggleButton("Search the web"));
    }

    private void buildToggleButtons() {
        for (ToolItemViewModel tool : viewModel.getTools()) {
            ToggleButton button = new ToggleButton(tool.getDisplayName());
            button.selectedProperty().bindBidirectional(tool.enabledProperty());
            getChildren().add(button);
        }
    }

    public ToolsSelectorViewModel getViewModel() {
        return viewModel;
    }
}
