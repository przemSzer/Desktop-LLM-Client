package dev.local.ai.ui.tools;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * A group of tools in the tools menu (Built-in or one MCP server).
 * {@link #selectionStateProperty()} is 0 = none, 1 = mixed, 2 = all.
 */
public class ToolGroupViewModel {

    public static final int SELECTION_NONE = 0;
    public static final int SELECTION_MIXED = 1;
    public static final int SELECTION_ALL = 2;

    private final StringProperty groupId;
    private final StringProperty displayName;
    private final BooleanProperty expanded;
    private final IntegerProperty selectionState;
    private final StringProperty statusText;
    private final ObservableList<ToolItemViewModel> tools;
    private final boolean mcpServer;

    public ToolGroupViewModel(String groupId, String displayName, boolean mcpServer) {
        this.groupId = new SimpleStringProperty(groupId);
        this.displayName = new SimpleStringProperty(displayName);
        this.expanded = new SimpleBooleanProperty(!mcpServer);
        this.selectionState = new SimpleIntegerProperty(SELECTION_NONE);
        this.statusText = new SimpleStringProperty("");
        this.tools = FXCollections.observableArrayList();
        this.mcpServer = mcpServer;
    }

    public String getGroupId() {
        return groupId.get();
    }

    public StringProperty displayNameProperty() {
        return displayName;
    }

    public String getDisplayName() {
        return displayName.get();
    }

    public BooleanProperty expandedProperty() {
        return expanded;
    }

    public boolean isExpanded() {
        return expanded.get();
    }

    public void setExpanded(boolean expanded) {
        this.expanded.set(expanded);
    }

    public IntegerProperty selectionStateProperty() {
        return selectionState;
    }

    public int getSelectionState() {
        return selectionState.get();
    }

    public void setSelectionState(int state) {
        this.selectionState.set(state);
    }

    public StringProperty statusTextProperty() {
        return statusText;
    }

    public String getStatusText() {
        return statusText.get();
    }

    public void setStatusText(String text) {
        this.statusText.set(text == null ? "" : text);
    }

    public ObservableList<ToolItemViewModel> getTools() {
        return tools;
    }

    public boolean isMcpServer() {
        return mcpServer;
    }

    public void refreshSelectionState() {
        if (tools.isEmpty()) {
            setSelectionState(SELECTION_NONE);
            setStatusText("0/0");
            return;
        }
        long enabled = tools.stream().filter(ToolItemViewModel::isEnabled).count();
        setStatusText(enabled + "/" + tools.size());
        if (enabled == 0) {
            setSelectionState(SELECTION_NONE);
        } else if (enabled == tools.size()) {
            setSelectionState(SELECTION_ALL);
        } else {
            setSelectionState(SELECTION_MIXED);
        }
    }
}
