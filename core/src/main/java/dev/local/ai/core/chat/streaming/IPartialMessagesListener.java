package dev.local.ai.core.chat.streaming;

import dev.local.ai.core.chat.messages.MessageType;

public interface IPartialMessagesListener {

    void onPartialMessage(String message, MessageType type, String requestId);

}
