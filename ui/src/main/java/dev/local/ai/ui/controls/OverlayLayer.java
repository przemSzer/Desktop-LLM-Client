package dev.local.ai.ui.controls;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

/**
 * Lightweight in-window replacement for ControlsFX {@code PopOver}. Content is shown inside the
 * existing scene instead of a separate window, which avoids the focus, multi-monitor and
 * auto-hide glitches that appear when nested popups (e.g. ComboBox dropdowns) live inside a PopOver.
 */
public final class OverlayLayer {

    public enum Placement {
        BELOW_LEFT, BELOW_RIGHT, ABOVE_LEFT, ABOVE_RIGHT
    }

    private static final double GAP = 8;
    private static final double EDGE_MARGIN = 8;

    private final Pane layer;
    private final Region scrim = new Region();
    private final ObjectProperty<Region> activeContent = new SimpleObjectProperty<>();

    private Node shownOwner;
    private Placement shownPlacement;
    private boolean positioning;
    private final InvalidationListener contentLayoutListener = observable -> {
        if (!positioning && activeContent.get() != null) {
            Platform.runLater(this::reposition);
        }
    };

    public OverlayLayer(Pane layer) {
        this.layer = layer;
        scrim.getStyleClass().add("overlay-scrim");
        scrim.prefWidthProperty().bind(layer.widthProperty());
        scrim.prefHeightProperty().bind(layer.heightProperty());
        scrim.setOnMousePressed(event -> {
            hide();
            event.consume();
        });
        layer.setVisible(false);
    }

    public ReadOnlyObjectProperty<Region> activeContentProperty() {
        return activeContent;
    }

    public boolean isShowing() {
        return activeContent.get() != null;
    }

    public void toggle(Region content, Node owner, Placement placement) {
        if (activeContent.get() == content) {
            hide();
        } else {
            show(content, owner, placement);
        }
    }

    public void show(Region content, Node owner, Placement placement) {
        detachLayoutListener();
        shownOwner = owner;
        shownPlacement = placement;
        layer.getChildren().setAll(scrim, content);
        layer.setVisible(true);
        layer.applyCss();
        layer.layout();
        content.layoutBoundsProperty().addListener(contentLayoutListener);
        position(content, owner, placement);
        activeContent.set(content);
    }

    public void hide() {
        if (activeContent.get() == null) {
            return;
        }
        detachLayoutListener();
        shownOwner = null;
        shownPlacement = null;
        layer.getChildren().clear();
        layer.setVisible(false);
        activeContent.set(null);
    }

    /**
     * Re-measures and re-places the active content. Call after the content's preferred size
     * changes while the overlay is open (for example when a section expands).
     */
    public void reposition() {
        Region content = activeContent.get();
        if (content == null || shownOwner == null || shownPlacement == null) {
            return;
        }
        position(content, shownOwner, shownPlacement);
    }

    private void detachLayoutListener() {
        Region content = activeContent.get();
        if (content != null) {
            content.layoutBoundsProperty().removeListener(contentLayoutListener);
        }
    }

    private void position(Region content, Node owner, Placement placement) {
        positioning = true;
        try {
            Bounds ownerBounds = owner.localToScene(owner.getBoundsInLocal());
            Point2D ownerTopLeft = layer.sceneToLocal(ownerBounds.getMinX(), ownerBounds.getMinY());
            Point2D ownerBottomRight = layer.sceneToLocal(ownerBounds.getMaxX(), ownerBounds.getMaxY());

            content.applyCss();
            content.setMaxHeight(Double.MAX_VALUE);
            double width = content.prefWidth(-1);
            double preferredHeight = content.prefHeight(width);
            double maxAllowedHeight = Math.max(0, layer.getHeight() - 2 * EDGE_MARGIN);
            double height = Math.min(preferredHeight, maxAllowedHeight);
            content.setMaxHeight(maxAllowedHeight);
            content.setPrefHeight(height);
            content.resize(width, height);
            content.layout();

            boolean alignRight = placement == Placement.BELOW_RIGHT || placement == Placement.ABOVE_RIGHT;
            boolean above = placement == Placement.ABOVE_LEFT || placement == Placement.ABOVE_RIGHT;

            double x = alignRight ? ownerBottomRight.getX() - width : ownerTopLeft.getX();
            double y = above ? ownerTopLeft.getY() - height - GAP : ownerBottomRight.getY() + GAP;

            x = clamp(x, EDGE_MARGIN, layer.getWidth() - width - EDGE_MARGIN);
            y = clamp(y, EDGE_MARGIN, layer.getHeight() - height - EDGE_MARGIN);

            content.relocate(x, y);
        } finally {
            positioning = false;
        }
    }

    private static double clamp(double value, double min, double max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(value, max));
    }
}
