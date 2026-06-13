package dev.local.ai.core.chat;

import dev.local.ai.core.chat.messages.Message;

/**
 * Simple callback interface for chat events.
 * Pure Java, no GUI dependencies.
 */
public interface IChatListener {
    /**
     * Called when a new message is added to the chat
     * @param message the message content
     * @param requestId id of the request to the LLM, during which message was created
     */
    void onMessageAdded(Message message, String requestId);
    
    /**
     * Called when an error occurs during message processing
     * @param errorMessage description of the error
     * @param exception the exception that occurred
     */
    void onError(String errorMessage, Exception exception);
    
    void onCancel();

    /**
     * Called when the chat memory is cleared
     */
    void onMemoryCleared();
}
