package dev.local.ai.ui.chat.controller;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.core.connections.ConnectionsStore;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.tools.IToolProvider;
import dev.local.ai.ui.chat.controls.ChatWebView;
import dev.local.ai.ui.chat.viewmodel.ChatMessageViewModel;
import dev.local.ai.ui.chat.viewmodel.ChatMessageViewModel.MessageType;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;
import dev.local.ai.ui.commands.CommandManager;
import dev.local.ai.ui.connection.viewmodel.ConnectionViewModel;
import dev.local.ai.ui.files.controls.FileAttachmentControl;
import dev.local.ai.ui.models.model.LLMInfoViewModel;
import dev.local.ai.ui.models.view.LLMSelectorView;
import dev.local.ai.ui.tools.ToolsSelectorView;
import javafx.util.Callback;

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
    private Label statusLabel;

    @FXML
    private Button clearChatButton;
    
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
    private final Callback<Class<?>, Object> controllerFactory;

    public ChatController(ChatViewModel chatViewModel,
                          IToolProvider toolProvider,
                          CoreEventBus eventBus,
                          ConnectionsStore connectionsStore,
                          CommandManager commandManager,
                          Callback<Class<?>, Object> controllerFactory) {
        this.chatViewModel = chatViewModel;
        this.toolProvider = toolProvider;
        this.eventBus = eventBus;
        this.connectionsStore = connectionsStore;
        this.commandManager = commandManager;
        this.controllerFactory = controllerFactory;
    }
    
    @FXML
    public void initialize() {
        try {
            logger.debug("Initializing ChatController");
            
            toolsSelectorView.init(toolProvider, eventBus);
            modelSelectorView.init(connectionsStore, eventBus, controllerFactory);
            fileAttachmentControl.init(commandManager);
            systemMessageFileAttachments.init(commandManager);
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
        fileAttachmentControl.attachedFilesProperty()
            .bind(chatViewModel.attachedFilesProperty());
        sendButton.visibleProperty().bind(chatViewModel.sendingMessageInProgressProperty().not());
        stopButton.visibleProperty().bind(chatViewModel.sendingMessageInProgressProperty());
        sendingMessageProgress.visibleProperty().bind(chatViewModel.sendingMessageInProgressProperty());
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

        clearChatButton.setOnAction(event -> {
            chatViewModel.clearChat();
        });
        
        logger.debug("Event handlers setup completed");
    }
    
    public ChatViewModel getChatViewModel() {
        return chatViewModel;
    }
} 