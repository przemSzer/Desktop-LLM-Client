package dev.local.ai.core.chat;

import dev.local.ai.core.chat.messages.Message;

public interface ILLMChat {

    String getSystemMessage() ;

    default void setSystemMessage(String message) {
        setSystemMessage(new Message(message));
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
