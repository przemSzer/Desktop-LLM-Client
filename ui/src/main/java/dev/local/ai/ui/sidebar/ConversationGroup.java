package dev.local.ai.ui.sidebar;

import dev.local.ai.core.storage.conversations.ConversationSummary;
import javafx.collections.ObservableList;

public record ConversationGroup(String label, ObservableList<ConversationSummary> items) {
}
