package dev.local.ai.ui.connection;

import dev.local.ai.ui.utils.MainStageProvider;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

public class ManageConnectionsDialog implements ConnectionsManagerDialog {

    private static final Logger logger = LoggerFactory.getLogger(ManageConnectionsDialog.class);

    private final Callback<Class<?>, Object> controllerFactory;
    private final MainStageProvider mainStageProvider;

    public ManageConnectionsDialog(
            Callback<Class<?>, Object> controllerFactory,
            MainStageProvider mainStageProvider) {
        if (controllerFactory == null) {
            throw new IllegalArgumentException("Controller factory cannot be null");
        }
        if (mainStageProvider == null) {
            throw new IllegalArgumentException("Main stage provider cannot be null");
        }
        this.controllerFactory = controllerFactory;
        this.mainStageProvider = mainStageProvider;
    }

    @Override
    public void show() {
        try {
            URL fxmlUrl = getClass().getResource("/fxml/ConnectionsView.fxml");
            if (fxmlUrl == null) {
                logger.error("ConnectionsView.fxml not found on classpath");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            loader.setControllerFactory(controllerFactory);
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(mainStageProvider.getMainWindow());
            dialogStage.setTitle("Manage Connections");
            dialogStage.setScene(new Scene(root));
            dialogStage.setMinWidth(600);
            dialogStage.setMinHeight(400);
            dialogStage.showAndWait();
        } catch (IOException e) {
            logger.error("Failed to open Connections dialog", e);
        }
    }
}
