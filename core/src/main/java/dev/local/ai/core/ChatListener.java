package dev.local.ai.core;

import dev.local.ai.core.chat.messages.Message;

/**
 * Simple callback interface for chat events.
 * Pure Java, no GUI dependencies.
 */
public interface ChatListener {
    /**
     * Called when a new message is added to the chat
     * @param message the message content
     * @param isUserMessage true if it's a user message, false if it's an AI response
     */
    void onMessageAdded(Message message, boolean isUserMessage);
    
    /**
     * Called when an error occurs during message processing
     * @param errorMessage description of the error
     * @param exception the exception that occurred
     */
    void onError(String errorMessage, Exception exception);
    
    /**
     * Called when the chat memory is cleared
     */
    void onMemoryCleared();
}
