package dev.local.ai.core.chat;

import dev.local.ai.core.events.BaseEvent;
import dev.local.ai.core.models.LLMInfoAndConnection;

public class LLMChangedEvent extends BaseEvent{

    private final LLMInfoAndConnection modelInfo;

    public static final String EVENT_TYPE = "LLMChangedEvent";

    public LLMChangedEvent(String source, LLMInfoAndConnection modelInfo) {
        super(EVENT_TYPE, source);
        this.modelInfo = modelInfo;
    }

    public LLMInfoAndConnection getModelInfo() {
        return modelInfo;
    }

}
