package dev.local.ai.context;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.local.ai.core.chat.ILLMChat;
import dev.local.ai.core.chat.streaming.StreamingChat;
import dev.local.ai.core.storage.conversations.ConversationSummary;
import dev.local.ai.ui.chat.controller.ChatController;
import dev.local.ai.ui.chat.converters.MessageConverter;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;
import dev.local.ai.ui.connection.controller.ConnectionsViewController;
import javafx.util.Callback;

public final class ControllerFactory implements Callback<Class<?>, Object> {

    private static final Logger logger = LoggerFactory.getLogger(ControllerFactory.class);

    private final AppContext app;

    public ControllerFactory(AppContext app) {
        this.app = app;
    }

    @Override
    public Object call(Class<?> type) {
        try {
            if (type == ChatController.class) {
                String conversationId = app.conversationStore.getLastConversation()
                        .map(ConversationSummary::id)
                        .orElseGet(app.conversationStore::createConversation);

                ChatMemory chatMemory = buildChatMemory(conversationId);
                ILLMChat chat = buildChat(chatMemory);
                ChatViewModel viewModel = new ChatViewModel(chat, app.commandManager, app.eventBus);
                viewModel.selectedModelProperty().set(app.lastSelectedModel.get().orElse(null));

                populateViewModelFromMemory(viewModel, chatMemory);

                return new ChatController(viewModel, app.toolProvider, app.eventBus,
                    app.connectionsStore, app.commandManager, this);
            }
            if (type == ConnectionsViewController.class) {
                return new ConnectionsViewController(app.commandManager);
            }

            logger.debug("No explicit mapping for controller {}, using default constructor", type.getName());
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create controller: " + type.getName(), e);
        }
    }

    private ChatMemory buildChatMemory(String conversationId) {
        return MessageWindowChatMemory.builder()
                .id(conversationId)
                .chatMemoryStore(app.chatMemoryStore)
                .alwaysKeepSystemMessageFirst(true)
                .maxMessages(1000)
                .build();
    }

    private ILLMChat buildChat(ChatMemory chatMemory) {
        return app.lastSelectedModel.get()
            .map(llm -> {
                logger.info("Building chat from last selected model: {} on connection {}",
                    llm.modelInfo().id(), llm.connection().id());
                var streamingModel = app.modelsProvider.createStreamingChatModel(llm);
                return (ILLMChat) new StreamingChat(streamingModel, chatMemory, app.toolProvider,
                    app.eventBus, app.modelsProvider);
            })
            .orElseGet(() -> buildFallbackChat(chatMemory));
    }

    private ILLMChat buildFallbackChat(ChatMemory chatMemory) {
        logger.warn("No last selected model found; falling back to local Ollama gemma3n:latest");
        var streamingModel = OllamaStreamingChatModel.builder()
            .baseUrl("http://localhost:11434")
            .modelName("gemma3n:latest")
            .timeout(Duration.ofMinutes(5))
            .logRequests(true)
            .logResponses(true)
            .build();
        return new StreamingChat(streamingModel, chatMemory, app.toolProvider,
            app.eventBus, app.modelsProvider);
    }

    private void populateViewModelFromMemory(ChatViewModel viewModel, ChatMemory chatMemory) {
        var converter = new MessageConverter();
        for (var chatMessage : chatMemory.messages()) {
            var coreMessage = dev.local.ai.core.chat.streaming.MessageToChatMessageConverter.toCoreMessage(chatMessage);
            if (coreMessage == null) {
                continue;
            }
            converter.convert(coreMessage).ifPresent(vm -> viewModel.getChatMessages().add(vm));
        }
        if (!chatMemory.messages().isEmpty()) {
            logger.info("Restored {} messages from last conversation", chatMemory.messages().size());
        }
    }
}
