package dev.local.ai.core.chat;

import java.util.List;

import dev.local.ai.core.chat.messages.Message;
import dev.local.ai.core.chat.messages.MessageType;

public interface ILLMChat {

    String getSystemMessage() ;

    default void setSystemMessage(String message) {
        setSystemMessage(new Message(message, List.of(), MessageType.SYSTEM));
    }

    void setSystemMessage(Message message) ;

    default void sendMessage(String message) {
        sendMessage(new Message(message));
    }

    void sendMessage(Message message) ;

    void clearMemory() ;

    /**
     * Removes all non-system messages from memory while keeping the conversation id
     * and on-disk conversation directory. Used instead of {@link #clearMemory()}
     * when the conversation folder must be preserved (file-backed store).
     */
    default void emptyNonSystemMessages() {
        clearMemory();
    }

    int getMessageCount() ;

    void setCallback(IChatListener callback) ;

}
