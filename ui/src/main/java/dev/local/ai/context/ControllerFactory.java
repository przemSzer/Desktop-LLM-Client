package dev.local.ai.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.core.storage.conversations.ConversationSummary;
import dev.local.ai.ui.chat.controller.ChatController;
import dev.local.ai.ui.chat.session.ChatSession;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;
import dev.local.ai.ui.connection.controller.ConnectionsViewController;
import dev.local.ai.ui.main.MainController;
import javafx.util.Callback;

public final class ControllerFactory implements Callback<Class<?>, Object> {

    private static final Logger logger = LoggerFactory.getLogger(ControllerFactory.class);

    private final AppContext app;
    private ChatViewModel chatViewModel;

    public ControllerFactory(AppContext app) {
        this.app = app;
    }

    @Override
    public Object call(Class<?> type) {
        try {
            if (type == MainController.class) {
                return new MainController(chatViewModel(), app.conversationStore,
                        app.settingsStorage);
            }
            if (type == ChatController.class) {
                return new ChatController(chatViewModel(), app.toolProvider, app.eventBus,
                    app.connectionsStore, app.commandManager, app.conversationStore,
                    this, app.fileSelector, app.mainStageProvider);
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

    private ChatViewModel chatViewModel() {
        if (chatViewModel == null) {
            String conversationId = app.conversationStore.getLastConversation()
                    .map(ConversationSummary::id)
                    .orElseGet(app.conversationStore::createConversation);

            ChatSession session = app.conversationSessionFactory.openConversation(conversationId);
            chatViewModel = new ChatViewModel(session, app.conversationSessionFactory,
                    app.conversationStore, app.commandManager, app.eventBus);
            chatViewModel.selectedModelProperty().set(app.lastSelectedModel.get().orElse(null));
        }
        return chatViewModel;
    }
}
