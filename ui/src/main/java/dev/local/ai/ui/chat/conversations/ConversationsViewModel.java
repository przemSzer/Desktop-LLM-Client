package dev.local.ai.ui.chat.conversations;

import dev.local.ai.core.storage.conversations.ConversationStore;
import dev.local.ai.core.storage.conversations.ConversationSummary;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Objects;

public final class ConversationsViewModel {

    private final ConversationStore conversationStore;
    private final ChatViewModel chatViewModel;

    private final ObjectProperty<ConversationSummary> selectedConversation = new SimpleObjectProperty<>();
    private final StringProperty statusMessage = new SimpleStringProperty("Ready");

    public ConversationsViewModel(ConversationStore conversationStore, ChatViewModel chatViewModel) {
        this.conversationStore = conversationStore;
        this.chatViewModel = chatViewModel;
    }

    public ObjectProperty<ConversationSummary> selectedConversationProperty() {
        return selectedConversation;
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public void setSelectedConversation(ConversationSummary summary) {
        selectedConversation.set(summary);
    }

    public boolean openSelected() {
        ConversationSummary sel = selectedConversation.get();
        if (sel == null) {
            statusMessage.set("Select a conversation");
            return false;
        }
        chatViewModel.loadConversation(sel.id());
        return true;
    }

    public void renameSelected(String newTitle) {
        ConversationSummary sel = selectedConversation.get();
        if (sel == null) {
            statusMessage.set("Select a conversation");
            return;
        }
        conversationStore.rename(sel.id(), newTitle);
        statusMessage.set("Conversation renamed");
    }

    public void deleteSelected() {
        ConversationSummary sel = selectedConversation.get();
        if (sel == null) {
            statusMessage.set("Select a conversation");
            return;
        }
        String id = sel.id();
        boolean isCurrent = Objects.equals(id, chatViewModel.getCurrentConversationId());
        conversationStore.deleteConversation(id);
        if (isCurrent) {
            String fallback = conversationStore.getLastConversation()
                    .map(ConversationSummary::id)
                    .orElseGet(conversationStore::createConversation);
            chatViewModel.loadConversation(fallback);
            statusMessage.set("Deleted — switched to another conversation");
        } else {
            statusMessage.set("Conversation deleted");
        }
    }
}
