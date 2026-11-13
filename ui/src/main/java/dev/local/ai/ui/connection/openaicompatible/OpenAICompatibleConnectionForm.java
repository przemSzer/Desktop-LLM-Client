package dev.local.ai.ui.connection.openaicompatible;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenAICompatibleConnectionForm implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(OpenAICompatibleConnectionForm.class);
    private static final String DEFAULT_BASE_URL = "http://localhost:1234/v1";

    @FXML
    private VBox rootVBox;

    @FXML
    private ImageView iconImageView;

    @FXML
    private Label titleLabel;

    @FXML
    private TextField nameTextField;

    @FXML
    private TextArea descriptionTextArea;

    @FXML
    private TextField baseUrlTextField;

    @FXML
    private PasswordField apiKeyTextField;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private Runnable onSaveCallback;
    private Runnable onCancelCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.debug("Initializing OpenAICompatibleConnectionForm");
        setupDefaultValues();
        setupEventHandlers();
        logger.debug("OpenAICompatibleConnectionForm initialized");
    }

    private void setupDefaultValues() {
        titleLabel.setText("New OpenAI Compatible Connection");
        baseUrlTextField.setText(DEFAULT_BASE_URL);
    }

    private void setupEventHandlers() {
        saveButton.setOnAction(event -> {
            logger.debug("Save button clicked");
            if (validateForm() && onSaveCallback != null) {
                onSaveCallback.run();
            }
        });

        cancelButton.setOnAction(event -> {
            logger.debug("Cancel button clicked");
            if (onCancelCallback != null) {
                onCancelCallback.run();
            }
        });

        nameTextField.setOnAction(event -> saveButton.fire());
        baseUrlTextField.setOnAction(event -> saveButton.fire());
        apiKeyTextField.setOnAction(event -> saveButton.fire());
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (nameTextField.getText() == null || nameTextField.getText().trim().isEmpty()) {
            nameTextField.setStyle("-fx-border-color: red;");
            isValid = false;
        } else {
            nameTextField.setStyle("");
        }

        if (baseUrlTextField.getText() == null || baseUrlTextField.getText().trim().isEmpty()) {
            baseUrlTextField.setStyle("-fx-border-color: red;");
            isValid = false;
        } else {
            baseUrlTextField.setStyle("");
        }

        return isValid;
    }

    public String getName() {
        return nameTextField.getText() != null ? nameTextField.getText().trim() : "";
    }

    public String getDescription() {
        return descriptionTextArea.getText() != null ? descriptionTextArea.getText().trim() : "";
    }

    public String getBaseUrl() {
        return baseUrlTextField.getText() != null ? baseUrlTextField.getText().trim() : "";
    }

    public String getApiKey() {
        return apiKeyTextField.getText() != null ? apiKeyTextField.getText().trim() : "";
    }

    public void setOnSave(Runnable onSave) {
        this.onSaveCallback = onSave;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancelCallback = onCancel;
    }

    public void clearForm() {
        nameTextField.clear();
        descriptionTextArea.clear();
        baseUrlTextField.setText(DEFAULT_BASE_URL);
        apiKeyTextField.clear();
    }
}
