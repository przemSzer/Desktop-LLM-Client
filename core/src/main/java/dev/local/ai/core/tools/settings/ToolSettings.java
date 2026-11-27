package dev.local.ai.core.tools.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolSettings(List<String> alwaysAllowedTools, List<String> enabledToolIds) {

    public ToolSettings {
        alwaysAllowedTools = alwaysAllowedTools == null
                ? List.of()
                : List.copyOf(alwaysAllowedTools);
        enabledToolIds = enabledToolIds == null
                ? List.of()
                : List.copyOf(enabledToolIds);
    }

    public ToolSettings(List<String> alwaysAllowedTools) {
        this(alwaysAllowedTools, List.of());
    }

    public static ToolSettings empty() {
        return new ToolSettings(List.of(), List.of());
    }
}
