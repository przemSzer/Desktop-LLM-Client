package dev.local.ai.ui.notifications;

import dev.local.ai.core.chat.LLMChangedEvent;
import dev.local.ai.core.events.CoreEventBus;
import dev.local.ai.core.events.EventListener;
import dev.local.ai.core.models.LLMInfoAndConnection;
import dev.local.ai.ui.utils.MainStageProvider;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

public final class NotificationService implements AutoCloseable {

    private static final String CARD_STYLE = """
            -fx-background-color: #2b2f38;
            -fx-background-radius: 10;
            -fx-border-radius: 10;
            -fx-border-color: #3b4250;
            -fx-border-width: 1;
            -fx-padding: 12 16 12 16;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 12, 0, 0, 3);
            """;
    private static final String TITLE_STYLE =
            "-fx-text-fill: #f5f7fa; -fx-font-size: 13px; -fx-font-weight: bold;";
    private static final String TEXT_STYLE =
            "-fx-text-fill: #c5ccd6; -fx-font-size: 12px;";

    private final CoreEventBus eventBus;
    private final MainStageProvider mainStageProvider;
    private final EventListener<LLMChangedEvent> modelChangedListener = this::onModelChanged;

    public NotificationService(CoreEventBus eventBus, MainStageProvider mainStageProvider) {
        this.eventBus = eventBus;
        this.mainStageProvider = mainStageProvider;
    }

    public void start() {
        eventBus.subscribe(LLMChangedEvent.EVENT_TYPE, modelChangedListener);
    }

    private void onModelChanged(LLMChangedEvent event) {
        LLMInfoAndConnection model = event.getModelInfo();
        if (model == null) {
            return;
        }
        String summary = model.connection().name() + " · " + model.modelInfo().name();
        show("Model changed", summary);
    }

    public void show(String title, String text) {
        Platform.runLater(() -> {
            Notifications notifications = Notifications.create()
                    .graphic(buildCard(title, text))
                    .position(Pos.TOP_CENTER)
                    .hideAfter(Duration.seconds(4));

            Stage owner = currentStage();
            if (owner != null) {
                notifications.owner(owner);
            }
            notifications.show();
        });
    }

    private Region buildCard(String title, String text) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle(TITLE_STYLE);

        Label textLabel = new Label(text);
        textLabel.setStyle(TEXT_STYLE);
        textLabel.setWrapText(true);

        VBox card = new VBox(2, titleLabel, textLabel);
        card.setStyle(CARD_STYLE);
        card.setMinWidth(260);
        card.setMaxWidth(360);
        return card;
    }

    private Stage currentStage() {
        try {
            return mainStageProvider.getMainStage();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    @Override
    public void close() {
        eventBus.unsubscribe(LLMChangedEvent.EVENT_TYPE, modelChangedListener);
    }
}
