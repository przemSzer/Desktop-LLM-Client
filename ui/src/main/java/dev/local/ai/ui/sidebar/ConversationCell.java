package dev.local.ai.ui.sidebar;

import dev.local.ai.core.storage.conversations.ConversationSummary;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.Clock;

final class ConversationCell extends ListCell<Object> {

    private final Label headerLabel = new Label();
    private final Label titleLabel = new Label();
    private final Label timeLabel = new Label();
    private final VBox conversationBox = new VBox(2);
    private final Clock clock;

    ConversationCell(Clock clock) {
        this.clock = clock;
        headerLabel.getStyleClass().add("sidebar-group-header");
        titleLabel.getStyleClass().add("conversation-cell-title");
        timeLabel.getStyleClass().add("conversation-cell-time");
        conversationBox.getStyleClass().add("conversation-cell");
        HBox titleRow = new HBox(8, titleLabel, timeLabel);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        timeLabel.setAlignment(Pos.CENTER_RIGHT);
        conversationBox.getChildren().add(titleRow);
        setPadding(new Insets(2, 8, 2, 8));
    }

    @Override
    protected void updateItem(Object item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            getStyleClass().remove("conversation-cell-selected");
            return;
        }
        if (item instanceof String header) {
            headerLabel.setText(header.toUpperCase());
            setGraphic(headerLabel);
            setText(null);
            setDisable(true);
            getStyleClass().remove("conversation-cell-selected");
            return;
        }
        setDisable(false);
        ConversationSummary summary = (ConversationSummary) item;
        titleLabel.setText(displayTitle(summary));
        timeLabel.setText(RelativeTimeFormatter.format(summary.updatedAt(), clock));
        setGraphic(conversationBox);
        setText(null);
        if (isSelected()) {
            if (!getStyleClass().contains("conversation-cell-selected")) {
                getStyleClass().add("conversation-cell-selected");
            }
        } else {
            getStyleClass().remove("conversation-cell-selected");
        }
    }

    @Override
    public void updateSelected(boolean selected) {
        super.updateSelected(selected);
        if (getItem() instanceof ConversationSummary) {
            if (selected) {
                if (!getStyleClass().contains("conversation-cell-selected")) {
                    getStyleClass().add("conversation-cell-selected");
                }
            } else {
                getStyleClass().remove("conversation-cell-selected");
            }
        }
    }

    private static String displayTitle(ConversationSummary summary) {
        String title = summary.title();
        if (title == null || title.isBlank()) {
            return "New conversation";
        }
        return title;
    }
}
