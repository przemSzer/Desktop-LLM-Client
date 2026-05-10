package dev.local.ai.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.core.connections.ConnectionsStore;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.models.StreamingChatModelsProvider;
import dev.local.ai.core.storage.DataStorage;
import dev.local.ai.core.storage.JsonFileStorage;
import dev.local.ai.core.storage.JsonSettingsStorage;
import dev.local.ai.core.storage.SettingsStorage;
import dev.local.ai.core.storage.models.LastSelectedModel;
import dev.local.ai.core.tools.FilterableToolProvider;
import dev.local.ai.core.tools.IToolProvider;
import dev.local.ai.core.tools.ToolsProvider;
import dev.local.ai.core.tools.local.CommandLineTools;
import dev.local.ai.core.tools.web.WebPageDownloaderTools;
import dev.local.ai.ui.commands.CommandManager;

public final class AppContext implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(AppContext.class);

    public final CoreEventBus eventBus;
    public final SettingsStorage settingsStorage;
    public final DataStorage dataStorage;

    public final ConnectionsStore connectionsStore;
    public final StreamingChatModelsProvider modelsProvider;
    public final LastSelectedModel lastSelectedModel;

    public final WebPageDownloaderTools webPageDownloaderTools;
    public final CommandLineTools commandLineTools;
    public final IToolProvider toolProvider;

    public final CommandManager commandManager;

    public AppContext() {
        logger.info("Initializing AppContext");

        this.eventBus = new CoreEventBus();
        this.settingsStorage = new JsonSettingsStorage();
        this.dataStorage = new JsonFileStorage();

        this.connectionsStore = new ConnectionsStore(dataStorage);
        this.modelsProvider = new StreamingChatModelsProvider();
        this.lastSelectedModel = new LastSelectedModel(eventBus, settingsStorage, connectionsStore);

        this.webPageDownloaderTools = new WebPageDownloaderTools();
        this.commandLineTools = new CommandLineTools();

        IToolProvider baseTools = new ToolsProvider();
        this.toolProvider = new FilterableToolProvider(baseTools, eventBus);

        this.commandManager = new CommandManager();

        logger.info("AppContext initialized");
    }

    @Override
    public void close() {
        logger.info("Shutting down AppContext");
        eventBus.shutdown();
    }
}
