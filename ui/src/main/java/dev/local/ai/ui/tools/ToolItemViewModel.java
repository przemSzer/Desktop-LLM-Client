package dev.local.ai.ui.tools;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ToolItemViewModel {

    private final StringProperty toolId;
    private final StringProperty displayName;
    private final BooleanProperty enabled;
    /** When true, the tool asks for approval before running (locked). */
    private final BooleanProperty requiresApproval;

    public ToolItemViewModel(String toolId, String displayName, boolean enabled) {
        this.toolId = new SimpleStringProperty(toolId);
        this.displayName = new SimpleStringProperty(displayName);
        this.enabled = new SimpleBooleanProperty(enabled);
        this.requiresApproval = new SimpleBooleanProperty(true);
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

    public BooleanProperty requiresApprovalProperty() {
        return requiresApproval;
    }

    public boolean isRequiresApproval() {
        return requiresApproval.get();
    }

    public void setRequiresApproval(boolean requiresApproval) {
        this.requiresApproval.set(requiresApproval);
    }

    @Override
    public String toString() {
        return String
                .format("ToolItem{id='%s', name='%s', enabled=%s, requiresApproval=%s}",
                    getToolId(), getDisplayName(), isEnabled(), isRequiresApproval()
                );
    }
}
