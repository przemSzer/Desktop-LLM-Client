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

    int getMessageCount() ;

    void setCallback(IChatListener callback) ;

}
