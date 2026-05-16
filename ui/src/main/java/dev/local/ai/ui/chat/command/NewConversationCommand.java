package dev.local.ai.ui.chat.command;

import dev.local.ai.core.storage.conversations.ConversationStore;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;
import dev.local.ai.ui.commands.ICommand;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a new empty conversation on disk and switches the UI to it.
 */
public final class NewConversationCommand implements ICommand {

    private static final Logger logger = LoggerFactory.getLogger(NewConversationCommand.class);

    private final ConversationStore conversationStore;
    private final ChatViewModel viewModel;

    public NewConversationCommand(ConversationStore conversationStore, ChatViewModel viewModel) {
        this.conversationStore = conversationStore;
        this.viewModel = viewModel;
    }

    @Override
    public boolean execute() {
        if (!canExecute()) {
            logger.warn("Cannot start new conversation while a message is in progress");
            return false;
        }
        String id = conversationStore.createConversation();
        viewModel.loadConversation(id);
        return true;
    }

    @Override
    public boolean undo() {
        return false;
    }

    @Override
    public boolean canExecute() {
        return conversationStore != null
                && viewModel != null
                && !viewModel.sendingMessageInProgressProperty().get();
    }

    @Override
    public String getDescription() {
        return "New conversation";
    }
}
