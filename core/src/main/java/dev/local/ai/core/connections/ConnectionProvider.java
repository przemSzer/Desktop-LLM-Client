package dev.local.ai.core.connections;

public enum ConnectionProvider {
    OPENAI("OpenAI", "/images/openai-icon.png"),
    OLLAMA("Ollama", "/images/ollama-icon.png");

    private final String displayName;
    private final String iconPath;

    ConnectionProvider(String displayName, String iconPath) {
        this.displayName = displayName;
        this.iconPath = iconPath;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconPath() {
        return iconPath;
    }
}
