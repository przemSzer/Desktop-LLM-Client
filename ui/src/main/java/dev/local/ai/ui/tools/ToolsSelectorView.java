package dev.local.ai.ui.tools;

import org.jspecify.annotations.NonNull;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ToolsSelectorView extends VBox {

    private static final Logger logger = LoggerFactory.getLogger(ToolsSelectorView.class);
    private static final String TOOLTIP_ASK = "Ask before running";
    private static final String TOOLTIP_ALWAYS_ALLOW = "Always allow in this chat";

    private ToolsSelectorViewModel viewModel;

    public ToolsSelectorView() {
        setSpacing(10.0);
    }

    public void init(ToolsSelectorViewModel viewModel) {
        getChildren().clear();
        this.viewModel = viewModel;
        buildToolRows();
        logger.info("ToolsSelectorView initialized with {} tools", viewModel.getTools().size());
    }

    private void buildToolRows() {
        for (ToolItemViewModel tool : viewModel.getTools()) {
            getChildren().add(buildRow(tool));
        }
    }

    private HBox buildRow(ToolItemViewModel tool) {
        var enableButton = createAndBindEnableButton(tool);
        HBox.setHgrow(enableButton, Priority.ALWAYS);

        var approvalGateButton = createAndBindApprovalGateButton(tool);

        HBox row = new HBox(8, enableButton, approvalGateButton);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static @NonNull ToggleButton createAndBindApprovalGateButton(ToolItemViewModel tool) {
        ToggleButton approvalGateButton = new ToggleButton();
        approvalGateButton.getStyleClass().addAll("flat", "composer-icon-button");
        approvalGateButton.selectedProperty().bindBidirectional(tool.requiresApprovalProperty());

        FontIcon icon = new FontIcon("fth-eye");
        icon.setIconSize(16);
        approvalGateButton.setGraphic(icon);

        Tooltip tooltip = new Tooltip();
        approvalGateButton.setTooltip(tooltip);

        tool.requiresApprovalProperty()
                .addListener((obs, wasRequired, isRequired) ->
                updateApprovalGatePresentation(icon, tooltip, Boolean.TRUE.equals(isRequired)));
        updateApprovalGatePresentation(icon, tooltip, tool.isRequiresApproval());
        return approvalGateButton;
    }

    private static @NonNull ToggleButton createAndBindEnableButton(ToolItemViewModel tool) {
        ToggleButton enableButton = new ToggleButton(tool.getDisplayName());
        enableButton.selectedProperty()
                .bindBidirectional(tool.enabledProperty());
        enableButton.setMaxWidth(Double.MAX_VALUE);
        return enableButton;
    }

    private static void updateApprovalGatePresentation(FontIcon icon, Tooltip tooltip, boolean requiresApproval) {
        icon.setIconLiteral(requiresApproval ? "fth-eye" : "fth-eye-off");
        tooltip.setText(requiresApproval ? TOOLTIP_ASK : TOOLTIP_ALWAYS_ALLOW);
    }

    public ToolsSelectorViewModel getViewModel() {
        return viewModel;
    }
}
