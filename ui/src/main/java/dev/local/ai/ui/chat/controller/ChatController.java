package dev.local.ai.ui.chat.controller;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import dev.local.ai.ui.controls.OverlayLayer;
import dev.local.ai.ui.controls.OverlayLayer.Placement;
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
import dev.local.ai.ui.chat.viewmodel.MessageTypeView;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;
import dev.local.ai.ui.commands.CommandManager;
import dev.local.ai.ui.connection.viewmodel.ConnectionViewModel;
import dev.local.ai.ui.files.controls.FileAttachmentControl;
import dev.local.ai.ui.files.dialogs.FileSelector;
import dev.local.ai.ui.files.viewmodel.AttachedFileViewModel;
import dev.local.ai.ui.files.viewmodel.FileAttachmentViewModel;
import dev.local.ai.ui.files.viewmodel.FileStatus;
import dev.local.ai.ui.models.model.LLMInfoViewModel;
import dev.local.ai.ui.models.view.LLMSelectorView;
import dev.local.ai.ui.theme.ThemeManager;
import dev.local.ai.ui.tools.ToolItemViewModel;
import dev.local.ai.ui.tools.ToolsSelectorView;
import dev.local.ai.ui.utils.MainStageProvider;
import javafx.util.Callback;

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
    private FlowPane attachmentChips;

    @FXML
    private Button attachButton;

    @FXML
    private ToggleButton toolsButton;

    @FXML
    private Button modelButton;

    @FXML
    private Pane overlayLayer;

    private final ToolsSelectorView toolsSelectorView = new ToolsSelectorView();
    private final LLMSelectorView modelSelectorView = new LLMSelectorView();

    private TextArea systemMessageTextArea;
    private FileAttachmentControl systemMessageFileAttachments;
    private OverlayLayer overlay;
    private VBox systemMessageContent;
    private VBox toolsContent;
    private VBox modelContent;
    private TextField titleEditField;
    private boolean titleBound;

    private FileAttachmentViewModel composerFiles;

    private final ChatViewModel chatViewModel;
    private final IToolProvider toolProvider;
    private final CoreEventBus eventBus;
    private final ConnectionsStore connectionsStore;
    private final CommandManager commandManager;
    private final ConversationStore conversationStore;
    private final Callback<Class<?>, Object> controllerFactory;
    private final FileSelector fileSelector;
    private final MainStageProvider mainStageProvider;
    private final ThemeManager themeManager;

    public ChatController(ChatViewModel chatViewModel,
                          IToolProvider toolProvider,
                          CoreEventBus eventBus,
                          ConnectionsStore connectionsStore,
                          CommandManager commandManager,
                          ConversationStore conversationStore,
                          Callback<Class<?>, Object> controllerFactory,
                          FileSelector fileSelector,
                          MainStageProvider mainStageProvider,
                          ThemeManager themeManager) {
        this.chatViewModel = chatViewModel;
        this.toolProvider = toolProvider;
        this.eventBus = eventBus;
        this.connectionsStore = connectionsStore;
        this.commandManager = commandManager;
        this.conversationStore = conversationStore;
        this.controllerFactory = controllerFactory;
        this.fileSelector = fileSelector;
        this.mainStageProvider = mainStageProvider;
        this.themeManager = themeManager;
    }

    @FXML
    public void initialize() {
        try {
            logger.debug("Initializing ChatController");

            toolsSelectorView.init(toolProvider, eventBus);
            modelSelectorView.init(connectionsStore, eventBus, controllerFactory, mainStageProvider);

            composerFiles = new FileAttachmentViewModel(commandManager, fileSelector);

            overlay = new OverlayLayer(overlayLayer);

            setupSystemMessagePopover();
            setupToolsPopover();
            setupModelPopover();
            setupOverlaySelectionSync();
            setupHeader();
            setupComposer();
            setupDataBinding();
            setupEventHandlers();
            setupShortcuts();
            renderExistingMessages();

            themeManager.setDarkModeConsumer(isDark -> chatWebView.setDarkMode(Boolean.TRUE.equals(isDark)));

            logger.debug("ChatController initialized.");
        } catch (Exception e) {
            logger.error("Error initializing ChatController", e);
        }
    }

    private void setupSystemMessagePopover() {
        systemMessageTextArea = new TextArea();
        systemMessageTextArea.setPromptText("Enter system message here");
        systemMessageTextArea.setMinHeight(120);
        systemMessageTextArea.setPrefRowCount(6);
        systemMessageTextArea.setWrapText(true);

        systemMessageFileAttachments = new FileAttachmentControl();
        systemMessageFileAttachments.init(new FileAttachmentViewModel(commandManager, fileSelector));

        systemMessageContent = new VBox(8, systemMessageTextArea, systemMessageFileAttachments);
        systemMessageContent.setPadding(new Insets(12));
        systemMessageContent.setPrefWidth(420);
        systemMessageContent.getStyleClass().add("system-popover-content");

        systemMessageButton.setOnAction(event ->
                overlay.toggle(systemMessageContent, systemMessageButton, Placement.BELOW_RIGHT));
    }

    private void setupToolsPopover() {
        toolsContent = new VBox(toolsSelectorView);
        toolsContent.setPadding(new Insets(30));
        toolsContent.setPrefWidth(360);
        toolsContent.getStyleClass().add("system-popover-content");

        toolsButton.setOnAction(event ->
                overlay.toggle(toolsContent, toolsButton, Placement.ABOVE_LEFT));

        Label badge = new Label();
        badge.getStyleClass().add("tools-badge");
        IntegerBinding enabledCount = enabledToolCount();
        badge.textProperty().bind(enabledCount.asString());
        badge.visibleProperty().bind(enabledCount.greaterThan(0));
        badge.managedProperty().bind(badge.visibleProperty());
        toolsButton.setGraphic(badge);
        toolsButton.setContentDisplay(ContentDisplay.RIGHT);
    }

    private IntegerBinding enabledToolCount() {
        var tools = toolsSelectorView.getViewModel().getTools();
        Observable[] dependencies = tools.stream()
                .map(ToolItemViewModel::enabledProperty)
                .toArray(Observable[]::new);
        return Bindings.createIntegerBinding(
                () -> (int) tools.stream().filter(ToolItemViewModel::isEnabled).count(),
                dependencies);
    }

    private void setupModelPopover() {
        modelContent = new VBox(modelSelectorView);
        modelContent.setPadding(new Insets(12));
        modelContent.setPrefWidth(420);
        modelContent.getStyleClass().add("system-popover-content");

        modelButton.setOnAction(event ->
                overlay.toggle(modelContent, modelButton, Placement.ABOVE_RIGHT));

        var modelViewModel = modelSelectorView.getViewModel();
        modelButton.textProperty().bind(Bindings.createStringBinding(
            () -> 
            {
                ConnectionViewModel connection = modelViewModel.getSelectedConnection();
                LLMInfoViewModel model = modelViewModel.getSelectedModel();
                String provider = connection != null ? connection.getName() : "No connection";
                String name = model != null ? model.getName() : "Select model";
                return provider + " · " + name + " ▾";
            }, 
            modelViewModel.selectedConnectionProperty(), 
            modelViewModel.selectedModelProperty())
        );
    }

    private void setupOverlaySelectionSync() {
        overlay.activeContentProperty().addListener((obs, oldContent, newContent) -> {
            systemMessageButton.setSelected(newContent == systemMessageContent);
            toolsButton.setSelected(newContent == toolsContent);
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

    private void setupComposer() {
        attachButton.setOnAction(event -> composerFiles.selectAndAddFiles());
        chatViewModel.attachedFilesProperty().addListener(
                (ListChangeListener<AttachedFileViewModel>) change -> rebuildAttachmentChips());
        rebuildAttachmentChips();
    }

    private void rebuildAttachmentChips() {
        attachmentChips.getChildren().clear();
        for (AttachedFileViewModel file : chatViewModel.attachedFilesProperty()) {
            attachmentChips.getChildren().add(createChip(file));
        }
        boolean hasFiles = !chatViewModel.attachedFilesProperty().isEmpty();
        attachmentChips.setVisible(hasFiles);
        attachmentChips.setManaged(hasFiles);
    }

    private Node createChip(AttachedFileViewModel file) {
        Label fileName = new Label(file.getFileName());
        fileName.getStyleClass().add("chip-name");

        Button removeButton = new Button("✕");
        removeButton.getStyleClass().addAll("flat", "chip-remove");
        removeButton.setOnAction(event -> composerFiles.removeFile(file));

        HBox chip = new HBox(4, buildStatusNode(file.getStatus()), fileName, removeButton);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("file-chip");

        file.statusProperty().addListener((obs, oldStatus, newStatus) ->
                chip.getChildren().set(0, buildStatusNode(newStatus)));
        return chip;
    }

    private Node buildStatusNode(FileStatus status) {
        if (status == FileStatus.VALID) {
            return chipStatusLabel("✓", "chip-status-valid", status);
        }
        if (status == FileStatus.ERROR) {
            return chipStatusLabel("✕", "chip-status-error", status);
        }
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(12, 12);
        spinner.setMinSize(12, 12);
        spinner.setMaxSize(12, 12);
        spinner.getStyleClass().add("chip-spinner");
        installChipStatusTooltip(spinner, status);
        return spinner;
    }

    private Label chipStatusLabel(String glyph, String styleClass, FileStatus status) {
        Label label = new Label(glyph);
        label.getStyleClass().addAll("chip-status", styleClass);
        installChipStatusTooltip(label, status);
        return label;
    }

    private void installChipStatusTooltip(Node node, FileStatus status) {
        if (status != null) {
            Tooltip.install(node, new Tooltip(status.getDisplayName()));
        }
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
                        if (msg.getType() == MessageTypeView.PARTIAL_AI) {
                            chatWebView.setPartialMessage(msg.getContent());
                            msg.contentProperty().addListener((obs, oldVal, newVal) ->
                                    chatWebView.setPartialMessage(newVal));
                        } else if (msg.getType() == MessageTypeView.PARTIAL_THINKING) {
                            int thinkingMsgId = chatWebView.setPartialThinkingMessage(msg.getContent());
                            msg.contentProperty()
                                .addListener((obs, oldVal, newVal) ->
                                    chatWebView.setPartialThinkingMessage(newVal, thinkingMsgId)
                                );
                            msg.isCompleteProperty()
                                    .addListener(
                                            (obs, oldVal, newVal) ->
                                            {
                                                if (msg.isComplete()) {
                                                    chatWebView.thinkingFinished(thinkingMsgId);
                                                }
                                            });
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
        composerFiles.attachedFilesProperty().bind(chatViewModel.attachedFilesProperty());
        sendButton.visibleProperty().bind(chatViewModel.sendingMessageInProgressProperty().not());
        sendButton.managedProperty().bind(sendButton.visibleProperty());
        stopButton.visibleProperty().bind(chatViewModel.sendingMessageInProgressProperty());
        stopButton.managedProperty().bind(stopButton.visibleProperty());
        sendingMessageProgress.visibleProperty().bind(chatViewModel.sendingMessageInProgressProperty());
        sendingMessageProgress.managedProperty().bind(sendingMessageProgress.visibleProperty());

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

        logger.debug("Event handlers setup completed");
    }

    private void setupShortcuts() {
        messageInput.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && event.isControlDown()) {
                event.consume();
                chatViewModel.sendMessage();
            }
        });

        //TODO: this is not a good place for this, it should be in the main view
        chatHeader.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                installSceneShortcuts(newScene);
            }
        });
    }

    private void installSceneShortcuts(Scene scene) {
        scene.getAccelerators().putIfAbsent(
                new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN),
                chatViewModel::clearChat);
        scene.getAccelerators().putIfAbsent(
                new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN),
                () -> new NewConversationCommand(conversationStore, chatViewModel).execute());
        scene.getAccelerators().putIfAbsent(
                new KeyCodeCombination(KeyCode.K, KeyCombination.CONTROL_DOWN),
                () -> messageInput.requestFocus());

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && hideOpenPopover()) {
                event.consume();
            }
        });
    }

    private boolean hideOpenPopover() {
        if (overlay != null && overlay.isShowing()) {
            overlay.hide();
            return true;
        }
        return false;
    }

    public ChatViewModel getChatViewModel() {
        return chatViewModel;
    }
}
