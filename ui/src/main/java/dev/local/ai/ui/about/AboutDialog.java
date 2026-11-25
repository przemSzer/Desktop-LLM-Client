package dev.local.ai.ui.about;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TabPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
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
        stage.setScene(new Scene(root));
        return stage;
    }

    static TabPane load(AppInfo info, List<OssLibrary> libraries) throws IOException {
        URL fxml = AboutDialog.class.getResource(FXML);
        if (fxml == null) {
            throw new IllegalStateException("Missing classpath resource " + FXML);
        }
        FXMLLoader loader = new FXMLLoader(fxml);
        TabPane root = loader.load();
        AboutController controller = loader.getController();
        controller.setDetails(info, libraries);
        return root;
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
