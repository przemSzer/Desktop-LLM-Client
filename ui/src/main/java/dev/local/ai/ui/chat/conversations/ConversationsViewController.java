package dev.local.ai.ui.chat.conversations;

import dev.local.ai.core.storage.conversations.ConversationStore;
import dev.local.ai.core.storage.conversations.ConversationSummariesListener;
import dev.local.ai.core.storage.conversations.ConversationSummary;
import dev.local.ai.ui.chat.viewmodel.ChatViewModel;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;

public final class ConversationsViewController {

    private static final Logger logger = LoggerFactory.getLogger(ConversationsViewController.class);

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());

    @FXML
    private TableView<ConversationSummary> conversationsTable;

    @FXML
    private Button openButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button renameButton;

    @FXML
    private Label statusLabel;

    private Stage dialogStage;

    private final ConversationStore conversationStore;
    private final ChatViewModel chatViewModel;

    private ConversationsViewModel viewModel;

    private ObservableList<ConversationSummary> tableSummariesItems;
    private ConversationSummariesListener tableSummariesListener;

    public ConversationsViewController(ConversationStore conversationStore, ChatViewModel chatViewModel) {
        this.conversationStore = conversationStore;
        this.chatViewModel = chatViewModel;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    @FXML
    public void initialize() {
        logger.debug("Initializing ConversationsViewController");

        viewModel = new ConversationsViewModel(conversationStore, chatViewModel);
        tableSummariesItems = FXCollections.observableArrayList(conversationStore.getConversationSummaries());
        conversationsTable.setItems(tableSummariesItems);

        tableSummariesListener =
                summaries -> Platform.runLater(() -> tableSummariesItems.setAll(summaries));
        conversationStore.addConversationSummariesListener(tableSummariesListener);

        TableColumn<ConversationSummary, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setPrefWidth(340);
        titleColumn.setCellValueFactory(param -> {
            ConversationSummary s = param.getValue();
            if (s == null) {
                return new ReadOnlyStringWrapper("");
            }
            String t = s.title();
            String display = t == null || t.isBlank() ? "Untitled" : t;
            return new ReadOnlyStringWrapper(display);
        });

        TableColumn<ConversationSummary, Instant> updatedColumn = new TableColumn<>("Updated");
        updatedColumn.setPrefWidth(180);
        updatedColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(
                param.getValue() != null ? param.getValue().updatedAt() : null));
        updatedColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Instant item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(DATE_TIME.format(item));
                }
            }
        });

        TableColumn<ConversationSummary, Instant> createdColumn = new TableColumn<>("Created");
        createdColumn.setPrefWidth(180);
        createdColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(
                param.getValue() != null ? param.getValue().createdAt() : null));
        createdColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Instant item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(DATE_TIME.format(item));
                }
            }
        });

        conversationsTable.getColumns().clear();
        conversationsTable.getColumns().add(titleColumn);
        conversationsTable.getColumns().add(updatedColumn);
        conversationsTable.getColumns().add(createdColumn);

        conversationsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) ->
                viewModel.setSelectedConversation(newSel));

        conversationsTable.setRowFactory(tv -> {
            TableRow<ConversationSummary> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    viewModel.setSelectedConversation(row.getItem());
                    if (viewModel.openSelected()) {
                        closeDialogIfPresent();
                    }
                }
            });
            return row;
        });

        statusLabel.textProperty().bind(viewModel.statusMessageProperty());

        openButton.disableProperty().bind(viewModel.selectedConversationProperty().isNull());
        deleteButton.disableProperty().bind(viewModel.selectedConversationProperty().isNull());
        renameButton.disableProperty().bind(viewModel.selectedConversationProperty().isNull());

        openButton.setOnAction(event -> {
            logger.debug("Open conversation clicked");
            if (viewModel.openSelected()) {
                closeDialogIfPresent();
            }
        });

        deleteButton.setOnAction(event -> {
            logger.debug("Delete conversation clicked");
            viewModel.deleteSelected();
            conversationsTable.getSelectionModel().clearSelection();
        });

        renameButton.setOnAction(event -> {
            ConversationSummary sel = viewModel.selectedConversationProperty().get();
            if (sel == null) {
                return;
            }
            TextInputDialog dialog = new TextInputDialog(sel.title() != null ? sel.title() : "");
            dialog.setTitle("Rename conversation");
            dialog.setHeaderText("Enter a new title");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(viewModel::renameSelected);
        });

        logger.debug("ConversationsViewController initialized");
    }

    /**
     * Unregisters store listeners; call when the dialog is closed to avoid leaks.
     */
    public void dispose() {
        if (tableSummariesListener != null) {
            conversationStore.removeConversationSummariesListener(tableSummariesListener);
            tableSummariesListener = null;
        }
    }

    private void closeDialogIfPresent() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}
