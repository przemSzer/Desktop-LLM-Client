package dev.local.ai.ui.chat.viewmodel;

public enum MessageTypeView {
    USER("User"),
    AI("AI"),
    PARTIAL_AI("Partial"),
    PARTIAL_THINKING("Partial_Thinking"),
    TOOL_RESULT("Tool result"),
    TOOL_CALL("Tool call"),
    SYSTEM("System"),
    ERROR("Error");

    private final String displayName;

    MessageTypeView(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
