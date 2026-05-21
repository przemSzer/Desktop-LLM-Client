package dev.local.ai.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.core.storage.conversations.ConversationSummary;
import dev.local.ai.ui.chat.controller.ChatController;
import dev.local.ai.ui.chat.session.ChatSession;
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

                ChatSession session = app.conversationSessionFactory.openConversation(conversationId);
                ChatViewModel viewModel = new ChatViewModel(session, app.conversationSessionFactory,
                        app.conversationStore, app.commandManager, app.eventBus);
                viewModel.selectedModelProperty().set(app.lastSelectedModel.get().orElse(null));

                return new ChatController(viewModel, app.toolProvider, app.eventBus,
                    app.connectionsStore, app.commandManager, app.conversationStore, this, app.fileSelector);
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
}
