package dev.local.ai.core.tools.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolSettings(List<String> alwaysAllowedTools) {

    public ToolSettings {
        alwaysAllowedTools = alwaysAllowedTools == null
                ? List.of()
                : List.copyOf(alwaysAllowedTools);
    }

    public static ToolSettings empty() {
        return new ToolSettings(List.of());
    }
}
