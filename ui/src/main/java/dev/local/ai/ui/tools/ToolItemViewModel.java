package dev.local.ai.ui.tools;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ToolItemViewModel {

    private final StringProperty toolId;
    private final StringProperty displayName;
    private final BooleanProperty enabled;

    public ToolItemViewModel(String toolId, String displayName, boolean enabled) {
        this.toolId = new SimpleStringProperty(toolId);
        this.displayName = new SimpleStringProperty(displayName);
        this.enabled = new SimpleBooleanProperty(enabled);
    }

    public StringProperty toolIdProperty() {
        return toolId;
    }

    public String getToolId() {
        return toolId.get();
    }

    public StringProperty displayNameProperty() {
        return displayName;
    }

    public String getDisplayName() {
        return displayName.get();
    }

    public BooleanProperty enabledProperty() {
        return enabled;
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    @Override
    public String toString() {
        return String.format("ToolItem{id='%s', name='%s', enabled=%s}", 
            getToolId(), getDisplayName(), isEnabled());
    }
}
