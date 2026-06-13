package dev.local.ai.core.chat.messages;

public enum MessageType {
    USER,
    AI,
    TOOL_CALL,
    TOOL_RESULT,
    PARTIAL,
    SYSTEM,
    PARTIAL_THINKING,
    ERROR;    
}
