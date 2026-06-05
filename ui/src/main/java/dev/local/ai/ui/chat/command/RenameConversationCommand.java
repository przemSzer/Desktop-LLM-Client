package dev.local.ai.ui.chat.command;

import dev.local.ai.core.storage.conversations.ConversationStore;
import dev.local.ai.core.storage.conversations.ConversationSummary;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;
import dev.local.ai.ui.commands.ICommand;
//TODO: we can change this so that it does not depend on the ChatViewModel
//this way we can use this command in the ConversationsViewController, chat etc.
public final class RenameConversationCommand implements ICommand {

    private final ConversationStore conversationStore;
    private final ChatViewModel chatViewModel;
    private final String conversationId;
    private final String newTitle;
    private String previousTitle;

    public RenameConversationCommand(ConversationStore conversationStore,
                                     ChatViewModel chatViewModel,
                                     String conversationId,
                                     String newTitle) {
        this.conversationStore = conversationStore;
        this.chatViewModel = chatViewModel;
        this.conversationId = conversationId;
        this.newTitle = newTitle;
    }

    @Override
    public boolean execute() {
        if (!canExecute()) {
            return false;
        }
        previousTitle = conversationStore.findSummary(conversationId)
                .map(ConversationSummary::title)
                .orElse(null);
        conversationStore.rename(conversationId, newTitle);
        chatViewModel.refreshConversationTitle();
        return true;
    }

    @Override
    public boolean undo() {
        if (previousTitle == null && conversationId == null) {
            return false;
        }
        conversationStore.rename(conversationId, previousTitle);
        chatViewModel.refreshConversationTitle();
        return true;
    }

    @Override
    public boolean canExecute() {
        return conversationId != null
                && !conversationId.isBlank()
                && conversationStore != null
                && chatViewModel != null;
    }

    @Override
    public String getDescription() {
        return "Rename conversation";
    }

    @Override
    public boolean supportsUndo() {
        return true;
    }
}
