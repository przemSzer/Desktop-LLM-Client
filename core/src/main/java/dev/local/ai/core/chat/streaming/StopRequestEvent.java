package dev.local.ai.core.chat.streaming;

import dev.local.ai.core.events.BaseEvent;

public class StopRequestEvent extends BaseEvent{

    public static final String EVENT_TYPE = "StopRequestEvent";

    public StopRequestEvent(String source) {
        super(EVENT_TYPE, source);
    }
}
