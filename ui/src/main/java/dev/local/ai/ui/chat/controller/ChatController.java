package dev.local.ai.ui.chat.controller;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.controlsfx.control.PopOver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.core.connections.ConnectionsStore;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.storage.conversations.ConversationStore;
import dev.local.ai.core.tools.IToolProvider;
import dev.local.ai.ui.chat.command.NewConversationCommand;
import dev.local.ai.ui.chat.command.RenameConversationCommand;
import dev.local.ai.ui.chat.controls.ChatWebView;
import dev.local.ai.ui.chat.viewmodel.ChatMessageViewModel;
import dev.local.ai.ui.chat.viewmodel.ChatMessageViewModel.MessageType;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;
import dev.local.ai.ui.commands.CommandManager;
import dev.local.ai.ui.connection.viewmodel.ConnectionViewModel;
import dev.local.ai.ui.chat.conversations.ConversationsViewController;
import dev.local.ai.ui.files.controls.FileAttachmentControl;
import dev.local.ai.ui.files.dialogs.FileSelector;
import dev.local.ai.ui.files.viewmodel.FileAttachmentViewModel;
import dev.local.ai.ui.models.model.LLMInfoViewModel;
import dev.local.ai.ui.models.view.LLMSelectorView;
import dev.local.ai.ui.tools.ToolsSelectorView;
import dev.local.ai.ui.utils.MainStageProvider;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;

public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @FXML
    private HBox chatHeader;

    @FXML
    private TextArea messageInput;

    @FXML
    private Button sendButton;

    @FXML
    private Button stopButton;

    @FXML
    private ProgressIndicator sendingMessageProgress;

    @FXML
    private ChatWebView chatWebView;

    @FXML
    private Label conversationTitleLabel;

    @FXML
    private Button undoButton;

    @FXML
    private Button redoButton;

    @FXML
    private ToggleButton systemMessageButton;

    @FXML
    private Label systemMessageIndicator;

    @FXML
    private Label statusLabel;

    @FXML
    private SplitMenuButton newConversationSplitMenuButton;

    @FXML
    private Button openConversationsButton;

    @FXML
    private MenuItem emptyCurrentConversationMenuItem;

    @FXML
    private FileAttachmentControl fileAttachmentControl;

    @FXML
    private ToolsSelectorView toolsSelectorView;

    @FXML
    private LLMSelectorView modelSelectorView;

    private TextArea systemMessageTextArea;
    private FileAttachmentControl systemMessageFileAttachments;
    private PopOver systemMessagePopover;
    private TextField titleEditField;
    private boolean titleBound;

    private final ChatViewModel chatViewModel;
    private final IToolProvider toolProvider;
    private final CoreEventBus eventBus;
    private final ConnectionsStore connectionsStore;
    private final CommandManager commandManager;
    private final ConversationStore conversationStore;
    private final Callback<Class<?>, Object> controllerFactory;
    private final FileSelector fileSelector;
    private final MainStageProvider mainStageProvider;

    public ChatController(ChatViewModel chatViewModel,
                          IToolProvider toolProvider,
                          CoreEventBus eventBus,
                          ConnectionsStore connectionsStore,
                          CommandManager commandManager,
                          ConversationStore conversationStore,
                          Callback<Class<?>, Object> controllerFactory,
                          FileSelector fileSelector,
                          MainStageProvider mainStageProvider) {
        this.chatViewModel = chatViewModel;
        this.toolProvider = toolProvider;
        this.eventBus = eventBus;
        this.connectionsStore = connectionsStore;
        this.commandManager = commandManager;
        this.conversationStore = conversationStore;
        this.controllerFactory = controllerFactory;
        this.fileSelector = fileSelector;
        this.mainStageProvider = mainStageProvider;
    }

    @FXML
    public void initialize() {
        try {
            logger.debug("Initializing ChatController");

            toolsSelectorView.init(toolProvider, eventBus);
            modelSelectorView.init(connectionsStore, eventBus, controllerFactory, mainStageProvider);
            fileAttachmentControl.init(new FileAttachmentViewModel(commandManager, fileSelector));

            setupSystemMessagePopover();
            setupHeader();
            setupDataBinding();
            setupEventHandlers();
            renderExistingMessages();

            logger.debug("ChatController initialized.");
        } catch (Exception e) {
            logger.error("Error initializing ChatController", e);
        }
    }

    //TODO: we can move this to a separate control
    private void setupSystemMessagePopover() {
        systemMessageTextArea = new TextArea();
        systemMessageTextArea.setPromptText("Enter system message here");
        systemMessageTextArea.setMinHeight(120);
        systemMessageTextArea.setPrefRowCount(6);
        systemMessageTextArea.setWrapText(true);

        systemMessageFileAttachments = new FileAttachmentControl();
        systemMessageFileAttachments.init(new FileAttachmentViewModel(commandManager, fileSelector));

        VBox content = new VBox(8, systemMessageTextArea, systemMessageFileAttachments);
        content.setPadding(new Insets(12));
        content.setPrefWidth(420);
        content.getStyleClass().add("system-popover-content");

        systemMessagePopover = new PopOver(content);
        systemMessagePopover.setArrowLocation(PopOver.ArrowLocation.TOP_RIGHT);
        systemMessagePopover.setDetachable(false);
        systemMessagePopover.setHeaderAlwaysVisible(false);

        URL stylesheet = getClass().getResource("/css/styles.css");
        if (stylesheet != null) {
            String css = stylesheet.toExternalForm();
            systemMessagePopover.setOnShown(shown -> {
                if (content.getScene() != null && content.getScene().getRoot() != null
                        && !content.getScene().getRoot().getStylesheets().contains(css)) {
                    content.getScene().getRoot().getStylesheets().add(css);
                }
            });
        }

        systemMessageButton.setOnAction(event -> {
            if (systemMessagePopover.isShowing()) {
                systemMessagePopover.hide();
            } else {
                systemMessagePopover.show(systemMessageButton);
            }
        });
    }

    private void setupHeader() {
        bindConversationTitle();
        conversationTitleLabel.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                startTitleEdit();
            }
        });

        undoButton.setOnAction(event -> commandManager.undo());
        redoButton.setOnAction(event -> commandManager.redo());
        undoButton.disableProperty().bind(chatViewModel.canUndoProperty().not());
        redoButton.disableProperty().bind(chatViewModel.canRedoProperty().not());

        updateSystemMessageIndicator();
        chatViewModel.systemMessageProperty().addListener((obs, oldVal, newVal) ->
                updateSystemMessageIndicator());
    }

    private void bindConversationTitle() {
        if (!titleBound) {
            conversationTitleLabel.textProperty().bind(chatViewModel.currentConversationTitleProperty());
            titleBound = true;
        }
    }

    private void unbindConversationTitle() {
        if (titleBound) {
            conversationTitleLabel.textProperty().unbind();
            titleBound = false;
        }
    }

    private void startTitleEdit() {
        if (titleEditField != null) {
            return;
        }
        unbindConversationTitle();
        titleEditField = new TextField(chatViewModel.getCurrentConversationTitle());
        titleEditField.getStyleClass().add("conversation-title");
        HBox.setHgrow(titleEditField, javafx.scene.layout.Priority.ALWAYS);
        int index = chatHeader.getChildren().indexOf(conversationTitleLabel);
        chatHeader.getChildren().set(index, titleEditField);
        conversationTitleLabel.setVisible(false);
        conversationTitleLabel.setManaged(false);
        titleEditField.requestFocus();
        titleEditField.selectAll();

        titleEditField.setOnAction(event -> commitTitleEdit());
        titleEditField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                commitTitleEdit();
            }
        });
        titleEditField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                cancelTitleEdit();
                event.consume();
            }
        });
    }

    private void commitTitleEdit() {
        if (titleEditField == null) {
            return;
        }
        String newTitle = titleEditField.getText();
        String conversationId = chatViewModel.getCurrentConversationId();
        commandManager.executeCommand(
                new RenameConversationCommand(conversationStore, chatViewModel, conversationId, newTitle));
        finishTitleEdit();
    }

    private void cancelTitleEdit() {
        finishTitleEdit();
    }

    private void finishTitleEdit() {
        if (titleEditField == null) {
            return;
        }
        int index = chatHeader.getChildren().indexOf(titleEditField);
        chatHeader.getChildren().set(index, conversationTitleLabel);
        conversationTitleLabel.setVisible(true);
        conversationTitleLabel.setManaged(true);
        titleEditField = null;
        bindConversationTitle();
    }

    private void updateSystemMessageIndicator() {
        boolean hasSystemMessage = chatViewModel.getSystemMessage() != null
                && !chatViewModel.getSystemMessage().isBlank();
        systemMessageIndicator.setVisible(hasSystemMessage);
        systemMessageIndicator.setManaged(hasSystemMessage);
    }

    private void setupDataBinding() {
        systemMessageTextArea.textProperty().bindBidirectional(chatViewModel.systemMessageProperty());
        systemMessageFileAttachments.attachedFilesProperty()
                .bind(chatViewModel.systemMessageAttachedFilesProperty());

        chatViewModel.getChatMessages().addListener((ListChangeListener<ChatMessageViewModel>) change -> {
            while (change.next()) {
                if (change.wasReplaced()) {
                    for (int i = change.getFrom(); i < change.getTo(); i++) {
                        ChatMessageViewModel msg = change.getList().get(i);
                        if (msg == null) continue;
                        chatWebView.removePartialMessage();
                        chatWebView.addMessage(msg);
                    }
                } else if (change.wasAdded()) {
                    for (ChatMessageViewModel msg : change.getAddedSubList()) {
                        if (msg == null) continue;
                        if (msg.getType() == MessageType.PARTIAL) {
                            chatWebView.setPartialMessage(msg.getContent());
                            msg.contentProperty().addListener((obs, oldVal, newVal) ->
                                    chatWebView.setPartialMessage(newVal));
                        } else {
                            chatWebView.addMessage(msg);
                        }
                    }
                }
                if (change.wasRemoved() && change.getList().isEmpty()) {
                    chatWebView.clearMessages();
                }
            }
        });

        messageInput.textProperty().bindBidirectional(chatViewModel.inputMessageProperty());
        statusLabel.textProperty().bind(chatViewModel.statusMessageProperty());
        fileAttachmentControl.attachedFilesProperty()
                .bind(chatViewModel.attachedFilesProperty());
        sendButton.visibleProperty().bind(chatViewModel.sendingMessageInProgressProperty().not());
        stopButton.visibleProperty().bind(chatViewModel.sendingMessageInProgressProperty());
        sendingMessageProgress.visibleProperty().bind(chatViewModel.sendingMessageInProgressProperty());
        newConversationSplitMenuButton.disableProperty().bind(chatViewModel.sendingMessageInProgressProperty());
        openConversationsButton.disableProperty().bind(chatViewModel.sendingMessageInProgressProperty());
        emptyCurrentConversationMenuItem.disableProperty().bind(chatViewModel.sendingMessageInProgressProperty());
        var selectedModel = chatViewModel.selectedModelProperty().get();
        if (selectedModel != null) {
            modelSelectorView
                    .getViewModel()
                    .setSelectedModel(new LLMInfoViewModel(selectedModel.modelInfo()));
            modelSelectorView
                    .getViewModel()
                    .setSelectedConnection(new ConnectionViewModel(
                            selectedModel.connection().providerType(),
                            selectedModel.connection().name(),
                            selectedModel.connection().description(),
                            selectedModel.connection().id()));
        }
        chatViewModel.selectedModelProperty()
                .map(llm -> new LLMInfoViewModel(llm.modelInfo()))
                .addListener((obs, oldVal, newVal) -> modelSelectorView.getViewModel().setSelectedModel(newVal));

        chatViewModel.selectedModelProperty()
                .map(llm -> new ConnectionViewModel(
                        llm.connection().providerType(),
                        llm.connection().name(),
                        llm.connection().description(),
                        llm.connection().id()))
                .addListener((obs, oldVal, newVal) -> modelSelectorView.getViewModel().setSelectedConnection(newVal));
        logger.debug("Data binding setup completed");
    }

    private void renderExistingMessages() {
        if (!chatViewModel.getChatMessages().isEmpty()) {
            logger.debug("Rendered {} existing messages from restored conversation",
                    chatViewModel.getChatMessages().size());
        }
        for (ChatMessageViewModel msg : chatViewModel.getChatMessages()) {
            chatWebView.addMessage(msg);
        }
    }

    private void setupEventHandlers() {
        sendButton.setOnAction(event -> {
            logger.debug("Send button clicked");
            chatViewModel.sendMessage();
        });

        stopButton.setOnAction(event -> {
            logger.debug("Stop button clicked");
            chatViewModel.stopMessage();
        });

        messageInput.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER") && event.isControlDown()) {
                event.consume();
                chatViewModel.sendMessage();
            }
        });

        var newConversationCommand = new NewConversationCommand(conversationStore, chatViewModel);
        newConversationSplitMenuButton.setOnAction(event -> newConversationCommand.execute());

        emptyCurrentConversationMenuItem.setOnAction(event -> chatViewModel.clearChat());

        openConversationsButton.setOnAction(event -> showConversationsDialog());

        logger.debug("Event handlers setup completed");
    }

    private void showConversationsDialog() {
        try {
            URL fxmlUrl = getClass().getResource("/fxml/ConversationsView.fxml");
            if (fxmlUrl == null) {
                logger.error("ConversationsView.fxml not found on classpath");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            ConversationsViewController conversationsController =
                    new ConversationsViewController(conversationStore, chatViewModel);
            loader.setController(conversationsController);

            Parent root = loader.load();

            Stage dialogStage = new Stage();
            conversationsController.setDialogStage(dialogStage);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(mainStageProvider.getMainWindow());
            dialogStage.setTitle("Conversations");
            dialogStage.setScene(new Scene(root));
            dialogStage.setMinWidth(720);
            dialogStage.setMinHeight(440);

            try {
                dialogStage.showAndWait();
            } finally {
                conversationsController.dispose();
            }

            logger.info("Conversations dialog closed");
        } catch (IOException e) {
            logger.error("Failed to open Conversations dialog", e);
        }
    }

    public ChatViewModel getChatViewModel() {
        return chatViewModel;
    }
}