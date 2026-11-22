package dev.local.ai.ui.chat.session;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.local.ai.core.chat.streaming.StreamingChat;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.models.StreamingChatModelsProvider;
import dev.local.ai.core.storage.models.LastSelectedModel;
import dev.local.ai.core.tools.AppToolsCatalog;
import dev.local.ai.core.tools.IToolExecutor;
import dev.local.ai.core.tools.exectuor.DefaultToolsExecutor;
import dev.local.ai.core.tools.gates.MultipleToolExecutionGate;
import dev.local.ai.core.tools.gates.WaitForApprovalGate;
import dev.local.ai.core.tools.settings.ToolSettings;
import dev.local.ai.core.tools.settings.ToolSettingsReader;
import dev.local.ai.core.tools.settings.ToolSettingsWriter;

import java.nio.file.Path;
import java.util.LinkedHashSet;

public final class ChatSessionFactory {

    private static final int MAX_MESSAGES_IN_MEMORY = 1000;
    public static final String TOOL_SETTINGS_FILE = "tool-settings.json";

    private final ChatMemoryStore chatMemoryStore;
    private final LastSelectedModel lastSelectedModel;
    private final StreamingChatModelsProvider modelsProvider;
    private final AppToolsCatalog toolsCatalog;
    private final CoreEventBus eventBus;
    private final Path chatsDirectory;
    private final ToolSettingsReader toolSettingsReader;
    private final ToolSettingsWriter toolSettingsWriter;

    public ChatSessionFactory(ChatMemoryStore chatMemoryStore,
                              LastSelectedModel lastSelectedModel,
                              StreamingChatModelsProvider modelsProvider,
                              AppToolsCatalog toolsCatalog,
                              CoreEventBus eventBus,
                              Path chatsDirectory,
                              ToolSettingsReader toolSettingsReader,
                              ToolSettingsWriter toolSettingsWriter) {
        this.chatMemoryStore = chatMemoryStore;
        this.lastSelectedModel = lastSelectedModel;
        this.modelsProvider = modelsProvider;
        this.toolsCatalog = toolsCatalog;
        this.eventBus = eventBus;
        this.chatsDirectory = chatsDirectory;
        this.toolSettingsReader = toolSettingsReader;
        this.toolSettingsWriter = toolSettingsWriter;
    }

    public ChatSession openConversation(String conversationId) {
        ChatMemory memory = buildChatMemory(conversationId);
        var alwaysAskGate = new WaitForApprovalGate();
        var multipleGate = new MultipleToolExecutionGate(alwaysAskGate);
        Path settingsFile = toolSettingsFile(conversationId);
        ToolSettings settings = toolSettingsReader.read(settingsFile);

        var enabledIds = new LinkedHashSet<>(settings.enabledToolIds());
        var alwaysAllowed = new LinkedHashSet<>(settings.alwaysAllowedTools());

        var toolApproval = new SessionToolApprovalConfiguration(
                multipleGate,
                toolSettingsWriter,
                settingsFile,
                alwaysAllowed,
                () -> enabledIds.stream().toList()
        );

        var toolSelection = new SessionToolSelection(
                toolsCatalog,
                toolSettingsWriter,
                settingsFile,
                toolApproval,
                enabledIds
        );
        toolSelection.restoreAcquiredServers();

        var toolExecutor = new DefaultToolsExecutor(toolSelection.toolProvider(), multipleGate);
        StreamingChat chat = buildStreamingChat(memory, toolExecutor);
        return new ChatSession(
                conversationId,
                memory,
                chat,
                alwaysAskGate,
                toolApproval,
                toolSelection
        );
    }

    Path toolSettingsFile(String conversationId) {
        return chatsDirectory.resolve(conversationId).resolve(TOOL_SETTINGS_FILE);
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
