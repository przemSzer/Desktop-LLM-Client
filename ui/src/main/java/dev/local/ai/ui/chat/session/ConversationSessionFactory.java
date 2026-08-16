package dev.local.ai.ui.chat.session;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.local.ai.core.chat.streaming.StreamingChat;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.models.StreamingChatModelsProvider;
import dev.local.ai.core.storage.models.LastSelectedModel;
import dev.local.ai.core.tools.IToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConversationSessionFactory {

    private static final Logger logger = LoggerFactory.getLogger(ConversationSessionFactory.class);
    private static final int MAX_MESSAGES_IN_MEMORY = 1000;

    private final ChatMemoryStore chatMemoryStore;
    private final LastSelectedModel lastSelectedModel;
    private final StreamingChatModelsProvider modelsProvider;
    private final IToolProvider toolProvider;
    private final CoreEventBus eventBus;

    public ConversationSessionFactory(ChatMemoryStore chatMemoryStore,
                                      LastSelectedModel lastSelectedModel,
                                      StreamingChatModelsProvider modelsProvider,
                                      IToolProvider toolProvider,
                                      CoreEventBus eventBus) {
        this.chatMemoryStore = chatMemoryStore;
        this.lastSelectedModel = lastSelectedModel;
        this.modelsProvider = modelsProvider;
        this.toolProvider = toolProvider;
        this.eventBus = eventBus;
    }

    public ChatSession openConversation(String conversationId) {
        ChatMemory memory = buildChatMemory(conversationId);
        StreamingChat chat = buildStreamingChat(memory);
        return new ChatSession(conversationId, memory, chat);
    }

    private ChatMemory buildChatMemory(String conversationId) {
        return MessageWindowChatMemory.builder()
                .id(conversationId)
                .chatMemoryStore(chatMemoryStore)
                .alwaysKeepSystemMessageFirst(true)
                .maxMessages(MAX_MESSAGES_IN_MEMORY)
                .build();
    }

    private StreamingChat buildStreamingChat(ChatMemory chatMemory) {
        var lastModelMaybe = lastSelectedModel.get();
        if (lastModelMaybe.isEmpty()) {
            throw new RuntimeException("no last model selected");
        }
        var llm = lastModelMaybe.get();
        logger.info("Building chat from last selected model: {} on connection {}",
                llm.modelInfo().id(), llm.connection().id());
        StreamingChatModel streamingModel = modelsProvider.createStreamingChatModel(llm);
        return new StreamingChat(streamingModel, llm, chatMemory, toolProvider, eventBus, modelsProvider);
    }

//    private StreamingChat buildFallbackChat(ChatMemory chatMemory) {
//        logger.warn("No last selected model found; falling back to local Ollama gemma3n:latest");
//        StreamingChatModel streamingModel = OllamaStreamingChatModel.builder()
//                .baseUrl("http://localhost:11434")
//                .modelName("gemma3n:latest")
//                .timeout(Duration.ofMinutes(5))
//                .logRequests(true)
//                .logResponses(true)
//                .build();
//        return new StreamingChat(streamingModel, chatMemory, toolProvider, eventBus, modelsProvider);
//    }
}
