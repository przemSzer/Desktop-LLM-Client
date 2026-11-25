package dev.local.ai.ui.about;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Arrays;
import java.util.List;

/**
 * Window icons for the About dialog, rasterized from the same Feather info glyph
 * as the sidebar button that opens the dialog ({@code fth-info}).
 */
final class AboutIcons {

    static final Ikon ICON = Feather.INFO;

    /** Dark neutral, so the glyph stays visible on a light window title bar. */
    private static final Color ICON_COLOR = Color.web("#2e3440");
    private static final int[] WINDOW_SIZES = {16, 32, 64};

    private AboutIcons() {
    }

    static List<Image> windowIcons() {
        return Arrays.stream(WINDOW_SIZES).mapToObj(size -> image(ICON, size)).toList();
    }

    static Image image(Ikon icon, int size) {
        FontIcon node = FontIcon.of(icon, size, ICON_COLOR);
        Group group = new Group(node);
        Scene scene = new Scene(group, Color.TRANSPARENT);
        scene.getRoot().applyCss();

        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        return node.snapshot(parameters, null);
    }
}
