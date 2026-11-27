package dev.local.ai.ui.about;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Modal About window loaded from {@code /fxml/AboutDialog.fxml}.
 */
public final class AboutDialog {

    private static final Logger logger = LoggerFactory.getLogger(AboutDialog.class);
    private static final String FXML = "/fxml/AboutDialog.fxml";

    private AboutDialog() {
    }

    public static void show(Window owner) {
        try {
            createStage(owner, AppInfo.load(), OssLibraries.load()).showAndWait();
        } catch (IOException | RuntimeException e) {
            logger.error("Failed to open About dialog", e);
            showError(owner);
        }
    }

    static Stage createStage(Window owner, AppInfo info, List<OssLibrary> libraries) throws IOException {
        Parent root = load(info, libraries);
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle("About");
        stage.setMinWidth(root.minWidth(-1));
        stage.setMinHeight(root.minHeight(-1));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        applyWindowIcon(stage, root);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stage.close();
                event.consume();
            }
        });
        return stage;
    }

    static Parent load(AppInfo info, List<OssLibrary> libraries) throws IOException {
        URL fxml = AboutDialog.class.getResource(FXML);
        if (fxml == null) {
            throw new IllegalStateException("Missing classpath resource " + FXML);
        }
        FXMLLoader loader = new FXMLLoader(fxml);
        Parent root = loader.load();
        AboutController controller = loader.getController();
        controller.setDetails(info, libraries);
        return root;
    }

    /**
     * Title-bar icon, same Feather info glyph as the sidebar button that opens this dialog.
     */
    private static void applyWindowIcon(Stage stage, Parent root) {
        if (!(root instanceof Pane pane)) {
            return;
        }
        FontIcon icon = new FontIcon("fth-info");
        icon.getStyleClass().add("ikonli-font-icon");
        icon.setManaged(false);
        pane.getChildren().add(icon);
        try {
            root.applyCss();
            root.layout();
            Image small = snapshotIcon(icon, 16);
            Image large = snapshotIcon(icon, 32);
            if (hasVisiblePixels(small)) {
                stage.getIcons().add(small);
            }
            if (hasVisiblePixels(large)) {
                stage.getIcons().add(large);
            }
        } finally {
            pane.getChildren().remove(icon);
        }
    }

    private static Image snapshotIcon(FontIcon icon, int size) {
        icon.setIconSize(size);
        icon.applyCss();
        icon.autosize();
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return icon.snapshot(params, null);
    }

    private static boolean hasVisiblePixels(Image image) {
        if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
            return false;
        }
        PixelReader reader = image.getPixelReader();
        if (reader == null) {
            return false;
        }
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (reader.getColor(x, y).getOpacity() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void showError(Window owner) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle("About");
        alert.setHeaderText("Could not open the About dialog");
        alert.setContentText("Application information could not be loaded.");
        alert.showAndWait();
    }
}
