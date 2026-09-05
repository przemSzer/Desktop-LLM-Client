package dev.local.ai.ui.chat.session;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.local.ai.core.chat.streaming.StreamingChat;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.models.StreamingChatModelsProvider;
import dev.local.ai.core.storage.models.LastSelectedModel;
import dev.local.ai.core.tools.IToolExecutor;
import dev.local.ai.core.tools.IToolProvider;
import dev.local.ai.core.tools.exectuor.DefaultToolsExecutor;
import dev.local.ai.core.tools.gates.MultipleToolExecutionGate;
import dev.local.ai.core.tools.gates.WaitForApprovalGate;

public final class ChatSessionFactory {

    private static final int MAX_MESSAGES_IN_MEMORY = 1000;

    private final ChatMemoryStore chatMemoryStore;
    private final LastSelectedModel lastSelectedModel;
    private final StreamingChatModelsProvider modelsProvider;
    private final IToolProvider toolProvider;
    private final CoreEventBus eventBus;

    public ChatSessionFactory(ChatMemoryStore chatMemoryStore,
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
        var alwaysAskGate = new WaitForApprovalGate();
        var toolExecutor = new DefaultToolsExecutor(toolProvider, new MultipleToolExecutionGate(alwaysAskGate));
        StreamingChat chat = buildStreamingChat(memory, toolExecutor);
        return new ChatSession(
                conversationId,
                memory,
                chat,
                alwaysAskGate::setApprovalProvider
                );
    }

    private ChatMemory buildChatMemory(String conversationId) {
        return MessageWindowChatMemory.builder()
                .id(conversationId)
                .chatMemoryStore(chatMemoryStore)
                .alwaysKeepSystemMessageFirst(true)
                .maxMessages(MAX_MESSAGES_IN_MEMORY)
                .build();
    }

    private StreamingChat buildStreamingChat(ChatMemory chatMemory, IToolExecutor toolExecutor) {
        var lastModelMaybe = lastSelectedModel.get();
        if (lastModelMaybe.isEmpty()) {
            return new StreamingChat(null, chatMemory, toolExecutor, eventBus, modelsProvider);
        }
        var llm = lastModelMaybe.get();
        var streamingModel = modelsProvider.createStreamingChatModel(llm);
        return new StreamingChat(streamingModel, chatMemory, toolExecutor, eventBus, modelsProvider);
    }

}
