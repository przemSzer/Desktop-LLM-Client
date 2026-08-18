package dev.local.ai.context;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.core.connections.ConnectionsStore;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.models.StreamingChatModelsProvider;
import dev.local.ai.core.storage.ApplicationDirectory;
import dev.local.ai.core.storage.DataStorage;
import dev.local.ai.core.storage.JsonFileStorage;
import dev.local.ai.core.storage.JsonSettingsStorage;
import dev.local.ai.core.storage.SettingsStorage;
import dev.local.ai.core.storage.conversations.ConversationStore;
import dev.local.ai.core.storage.conversations.JsonFileChatMemoryStore;
import dev.local.ai.core.storage.models.LastSelectedModel;
import dev.local.ai.core.tools.FilterableToolProvider;
import dev.local.ai.core.tools.IToolProvider;
import dev.local.ai.core.tools.ToolsProviderWithMCP;
import dev.local.ai.ui.chat.session.ConversationSessionFactory;
import dev.local.ai.ui.commands.CommandManager;
import dev.local.ai.ui.files.dialogs.FileSelector;
import dev.local.ai.ui.files.dialogs.OpenFilesDialog;
import dev.local.ai.ui.notifications.NotificationService;
import dev.local.ai.ui.theme.ThemeManager;
import dev.local.ai.ui.utils.MainStageProvider;

public final class AppContext implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(AppContext.class);

    public final CoreEventBus eventBus;
    public final SettingsStorage settingsStorage;
    public final DataStorage dataStorage;

    public final ConnectionsStore connectionsStore;
    public final StreamingChatModelsProvider modelsProvider;
    public final LastSelectedModel lastSelectedModel;

    public final IToolProvider toolProvider;

    public final CommandManager commandManager;

    public final ConversationStore conversationStore;
    public final JsonFileChatMemoryStore chatMemoryStore;

    public final ConversationSessionFactory conversationSessionFactory;

    private final List<AutoCloseable> closeables;

    public final MainStageProvider mainStageProvider;
    public final FileSelector fileSelector;

    public final ThemeManager themeManager;
    public final NotificationService notificationService;

    public AppContext() {
        this.closeables = new ArrayList<>();
        logger.info("Initializing AppContext");

        this.eventBus = new CoreEventBus();
        this.settingsStorage = new JsonSettingsStorage();
        this.dataStorage = new JsonFileStorage();

        Path chatsDir = ApplicationDirectory.chats();
        this.conversationStore = new ConversationStore(chatsDir);
        this.chatMemoryStore = new JsonFileChatMemoryStore(chatsDir);
        this.chatMemoryStore.setConversationStore(conversationStore);

        this.connectionsStore = new ConnectionsStore(dataStorage);
        this.modelsProvider = new StreamingChatModelsProvider();
        this.lastSelectedModel = new LastSelectedModel(eventBus, settingsStorage, connectionsStore);

        ToolsProviderWithMCP baseTools = new ToolsProviderWithMCP();
        this.closeables.add(baseTools);

        this.toolProvider = new FilterableToolProvider(baseTools, eventBus);

        this.commandManager = new CommandManager();

        this.conversationSessionFactory = new ConversationSessionFactory(
            chatMemoryStore, lastSelectedModel, modelsProvider, toolProvider, eventBus
        );

        this.mainStageProvider = new MainStageProvider();
        this.fileSelector = new OpenFilesDialog(mainStageProvider);

        this.themeManager = new ThemeManager(settingsStorage);
        this.notificationService = new NotificationService(eventBus, mainStageProvider);
        this.notificationService.start();
        this.closeables.add(notificationService);

        logger.info("AppContext initialized");
    }

    @Override
    public void close() {
        logger.info("Shutting down AppContext");
        for (var closeable : closeables) {
            try {
                closeable.close();
            } catch (Exception e) {
                logger.error("Error while closing closeable", e);
            }
        }
        eventBus.shutdown();
    }
}
