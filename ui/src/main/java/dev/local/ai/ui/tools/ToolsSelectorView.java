package dev.local.ai.ui.tools;

import org.jspecify.annotations.NonNull;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
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
    private Runnable contentSizeChangedHandler;

    public ToolsSelectorView() {
        setSpacing(10.0);
    }

    public void setOnContentSizeChanged(Runnable handler) {
        this.contentSizeChangedHandler = handler;
    }

    public void init(ToolsSelectorViewModel viewModel) {
        getChildren().clear();
        this.viewModel = viewModel;
        buildGroups();
        logger.info("ToolsSelectorView initialized with {} groups", viewModel.getGroups().size());
    }

    private void notifyContentSizeChanged() {
        if (contentSizeChangedHandler == null) {
            return;
        }
        Platform.runLater(contentSizeChangedHandler);
    }

    private void buildGroups() {
        for (ToolGroupViewModel group : viewModel.getGroups()) {
            getChildren().add(buildGroupPane(group));
        }
    }

    private TitledPane buildGroupPane(ToolGroupViewModel group) {
        CheckBox groupCheck = new CheckBox();
        groupCheck.selectedProperty().addListener((obs, was, isSelected) -> {
            if (groupCheck.isIndeterminate()) {
                return;
            }
            int state = group.getSelectionState();
            boolean shouldEnable = state != ToolGroupViewModel.SELECTION_ALL;
            if (Boolean.TRUE.equals(isSelected) != shouldEnable
                    && state != ToolGroupViewModel.SELECTION_MIXED) {
                // driven by selectionState binding below
            }
        });
        groupCheck.setOnAction(e -> viewModel.toggleGroup(group));

        Label title = new Label();
        title.textProperty().bind(group.displayNameProperty());

        Label count = new Label();
        count.textProperty().bind(group.statusTextProperty());
        count.getStyleClass().add("tools-group-count");

        HBox header = new HBox(8, groupCheck, title, count);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);

        VBox toolsBox = new VBox(6);
        for (ToolItemViewModel tool : group.getTools()) {
            toolsBox.getChildren().add(buildRow(tool));
        }
        group.getTools().addListener((javafx.collections.ListChangeListener<ToolItemViewModel>) change -> {
            toolsBox.getChildren().clear();
            for (ToolItemViewModel tool : group.getTools()) {
                toolsBox.getChildren().add(buildRow(tool));
            }
            notifyContentSizeChanged();
        });

        TitledPane pane = new TitledPane();
        pane.setGraphic(header);
        pane.setContent(toolsBox);
        pane.setExpanded(group.isExpanded());
        pane.expandedProperty().addListener((obs, was, isExpanded) -> {
            viewModel.onGroupExpanded(group, Boolean.TRUE.equals(isExpanded));
            notifyContentSizeChanged();
        });

        group.selectionStateProperty().addListener((obs, oldState, newState) ->
                applyGroupCheckState(groupCheck, newState.intValue()));
        applyGroupCheckState(groupCheck, group.getSelectionState());

        return pane;
    }

    private static void applyGroupCheckState(CheckBox groupCheck, int state) {
        switch (state) {
            case ToolGroupViewModel.SELECTION_ALL -> {
                groupCheck.setIndeterminate(false);
                groupCheck.setSelected(true);
            }
            case ToolGroupViewModel.SELECTION_MIXED -> {
                groupCheck.setIndeterminate(true);
            }
            default -> {
                groupCheck.setIndeterminate(false);
                groupCheck.setSelected(false);
            }
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
