package dev.local.ai.ui.main;

import dev.local.ai.core.storage.SettingsStorage;
import dev.local.ai.core.storage.conversations.ConversationStore;
import dev.local.ai.core.storage.conversations.ConversationSummary;
import dev.local.ai.ui.chat.command.NewConversationCommand;
import dev.local.ai.ui.chat.controller.ChatController;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;
import dev.local.ai.ui.sidebar.SidebarView;
import dev.local.ai.ui.sidebar.SidebarViewModel;
import dev.local.ai.ui.theme.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML
    private SplitPane contentSplitPane;

    @FXML
    private SidebarView sidebarView;

    @FXML
    private ChatController chatWindowController;

    private final ChatViewModel chatViewModel;
    private final ConversationStore conversationStore;
    private final SettingsStorage settingsStorage;
    private final ThemeManager themeManager;
    private boolean syncingSidebarSelection;

    public MainController(ChatViewModel chatViewModel,
                          ConversationStore conversationStore,
                          SettingsStorage settingsStorage,
                          ThemeManager themeManager) {
        this.chatViewModel = chatViewModel;
        this.conversationStore = conversationStore;
        this.settingsStorage = settingsStorage;
        this.themeManager = themeManager;
    }

    @FXML
    public void initialize() {
        logger.debug("Initializing MainController");

        SidebarViewModel sidebarViewModel = new SidebarViewModel(conversationStore);
        sidebarView.init(sidebarViewModel, conversationStore, settingsStorage, themeManager);
        sidebarView.newChatDisabledProperty().bind(chatViewModel.sendingMessageInProgressProperty());
        sidebarView.setOnNewChat(() ->
                new NewConversationCommand(conversationStore, chatViewModel).execute());

        sidebarView.selectedConversationProperty().addListener((obs, oldSelection, newSelection) ->
                handleSidebarSelection(newSelection));

        chatViewModel.currentConversationIdProperty().addListener((obs, oldId, newId) ->
                Platform.runLater(() -> selectCurrentConversationInSidebar(newId)));

        sidebarView.collapsedProperty().addListener((obs, wasCollapsed, isCollapsed) ->
                Platform.runLater(this::updateSidebarDivider));
        contentSplitPane
            .widthProperty()
            .addListener((obs, oldWidth, newWidth) -> updateSidebarDivider());

        selectCurrentConversationInSidebar(chatViewModel.getCurrentConversationId());
        Platform.runLater(this::updateSidebarDivider);
        logger.debug("MainController initialized with chat controller {}", chatWindowController);
    }

    private void updateSidebarDivider() {
        double width = contentSplitPane.getWidth();
        if (width <= 0) {
            return;
        }
        contentSplitPane.setDividerPosition(0, sidebarView.getTargetWidth() / width);
    }

    public void dispose() {
        sidebarView.dispose();
    }

    private void handleSidebarSelection(ConversationSummary selected) {
        if (syncingSidebarSelection || selected == null) {
            return;
        }
        if (!selected.id().equals(chatViewModel.getCurrentConversationId())) {
            chatViewModel.loadConversation(selected.id());
        }
    }

    private void selectCurrentConversationInSidebar(String conversationId) {
        syncingSidebarSelection = true;
        try {
            sidebarView.selectConversation(conversationId);
        } finally {
            syncingSidebarSelection = false;
        }
    }
}
