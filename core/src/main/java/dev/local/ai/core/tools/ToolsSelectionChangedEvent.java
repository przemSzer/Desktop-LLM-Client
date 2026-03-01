package dev.local.ai.core.tools;

import java.util.Set;

import dev.local.ai.core.events.BaseEvent;

public class ToolsSelectionChangedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "ToolsSelectionChangedEvent";

    private final Set<String> enabledToolIds;

    public ToolsSelectionChangedEvent(String source, Set<String> enabledToolIds) {
        super(EVENT_TYPE, source);
        this.enabledToolIds = Set.copyOf(enabledToolIds);
    }

    public Set<String> getEnabledToolIds() {
        return enabledToolIds;
    }
}
