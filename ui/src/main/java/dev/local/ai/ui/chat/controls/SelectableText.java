package dev.local.ai.ui.chat.controls;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;

/**
 * A read-only {@link TextArea} that auto-sizes its height to fit content
 * and is styled to look like a plain label while supporting text selection.
 */
public class SelectableText extends TextArea {

    private static final double PADDING = 15;
    private static final double MIN_HEIGHT = 24;

    public SelectableText() {
        this("");
    }

    public SelectableText(String text) {
        super(text);
        setEditable(false);
        setWrapText(true);
        setFocusTraversable(true);
        getStyleClass().add("selectable-text");

        textProperty().addListener((obs, oldVal, newVal) -> requestAutoSize());
        widthProperty().addListener((obs, oldVal, newVal) -> requestAutoSize());
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                requestAutoSize();
            }
        });
    }

    private void requestAutoSize() {
        Platform.runLater(() -> {
            applyCss();
            layout();
            Text textNode = (Text) lookup(".text");
            if (textNode != null) {
                double h = textNode.getBoundsInLocal().getHeight() + PADDING;
                double target = Math.max(h, MIN_HEIGHT);
                setPrefHeight(target);
                setMinHeight(target);
            }
        });
    }
}
