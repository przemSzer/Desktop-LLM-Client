package dev.local.ai.ui.chat.controller;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.core.connections.ConnectionsStore;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.storage.conversations.ConversationStore;
import dev.local.ai.core.tools.IToolProvider;
import dev.local.ai.ui.chat.command.NewConversationCommand;
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
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;

/**
 * Controller for the Chat UI following MVVM pattern.
 * Handles only UI events and delegates business logic to ViewModel.
 */
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    @FXML
    private TextArea systemMessageTextArea;

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
    private FileAttachmentControl systemMessageFileAttachments;

    @FXML
    private ToolsSelectorView toolsSelectorView;

    @FXML
    private LLMSelectorView modelSelectorView;

    private final ChatViewModel chatViewModel;
    private final IToolProvider toolProvider;
    private final CoreEventBus eventBus;
    private final ConnectionsStore connectionsStore;
    private final CommandManager commandManager;
    private final ConversationStore conversationStore;
    private final Callback<Class<?>, Object> controllerFactory;
    private final FileSelector fileSelectionCallback;

    public ChatController(ChatViewModel chatViewModel,
                          IToolProvider toolProvider,
                          CoreEventBus eventBus,
                          ConnectionsStore connectionsStore,
                          CommandManager commandManager,
                          ConversationStore conversationStore,
                          Callback<Class<?>, Object> controllerFactory,
                          FileSelector fileSelectionCallback) {
        this.chatViewModel = chatViewModel;
        this.toolProvider = toolProvider;
        this.eventBus = eventBus;
        this.connectionsStore = connectionsStore;
        this.commandManager = commandManager;
        this.conversationStore = conversationStore;
        this.controllerFactory = controllerFactory;
        this.fileSelectionCallback = fileSelectionCallback;
    }
    
    @FXML
    public void initialize() {
        try {
            logger.debug("Initializing ChatController");
            
            toolsSelectorView.init(toolProvider, eventBus);
            modelSelectorView.init(connectionsStore, eventBus, controllerFactory);
            fileAttachmentControl.init(new FileAttachmentViewModel(commandManager, fileSelectionCallback));
            systemMessageFileAttachments.init(new FileAttachmentViewModel(commandManager, fileSelectionCallback));
            setupDataBinding();
            setupEventHandlers();
            renderExistingMessages();
            
            logger.debug("ChatController initialized.");
        } catch (Exception e) {
            logger.error("Error initializing ChatController", e);         
        }
    }
    
    private void setupDataBinding() {        
        systemMessageTextArea.textProperty().bindBidirectional(chatViewModel.systemMessageProperty());
        systemMessageFileAttachments.attachedFilesProperty().bind(chatViewModel.systemMessageAttachedFilesProperty());

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
        conversationTitleLabel.textProperty().bind(chatViewModel.currentConversationTitleProperty());
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
                .setSelectedConnection(new ConnectionViewModel(selectedModel.connection().providerType(), selectedModel.connection().name(), selectedModel.connection().description(), selectedModel.connection().id()));
        }
        chatViewModel.selectedModelProperty()
            .map(llm -> new LLMInfoViewModel(llm.modelInfo()))
            .addListener((obs, oldVal, newVal) -> modelSelectorView.getViewModel().setSelectedModel(newVal));        

        chatViewModel.selectedModelProperty()
            .map(llm -> new ConnectionViewModel(llm.connection().providerType(), llm.connection().name(), llm.connection().description(), llm.connection().id()))
            .addListener((obs, oldVal, newVal) -> modelSelectorView.getViewModel().setSelectedConnection(newVal));        
        logger.debug("Data binding setup completed");
    }
    
    private void renderExistingMessages() {
        //TODO: architecture - it should be moved to a view model, which 
        //should check this and trigger rerender of the chat web view
        //maybe  in lazy way...
        if (!chatViewModel.getChatMessages().isEmpty()) {
            logger.debug("Rendered {} existing messages from restored conversation", chatViewModel.getChatMessages().size());
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
            dialogStage.initOwner(openConversationsButton.getScene().getWindow());
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