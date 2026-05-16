package dev.local.ai.ui.chat.session;

import dev.langchain4j.memory.ChatMemory;
import dev.local.ai.core.chat.streaming.StreamingChat;

public final class ChatSession implements AutoCloseable {

    private final String conversationId;
    private final ChatMemory chatMemory;
    private final StreamingChat chat;

    public ChatSession(String conversationId, ChatMemory chatMemory, StreamingChat chat) {
        this.conversationId = conversationId;
        this.chatMemory = chatMemory;
        this.chat = chat;
    }

    public String conversationId() {
        return conversationId;
    }

    public ChatMemory chatMemory() {
        return chatMemory;
    }

    public StreamingChat chat() {
        return chat;
    }

    @Override
    public void close() {
        chat.close();
    }
}
