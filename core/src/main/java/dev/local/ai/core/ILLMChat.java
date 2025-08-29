package dev.local.ai.core;

public interface ILLMChat {

    String getSystemMessage() ;

    void setSystemMessage(String message) ;

    void sendMessage(String message) ;

    void clearMemory() ;

    int getMessageCount() ;

    void setCallback(ChatListener callback) ;

}
