package dev.local.ai.ui.connection.openai;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the OpenAI connection form.
 * Handles the form fields and validation for OpenAI connection configuration.
 */
public class OpenAIConnectionForm implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIConnectionForm.class);
    
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
    private TextField apiKeyTextField;
        
    @FXML
    private Button saveButton;
    
    @FXML
    private Button cancelButton;
    
    // Callbacks
    private Runnable onSaveCallback;
    private Runnable onCancelCallback;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.debug("Initializing OpenAIConnectionForm");
        
        setupDefaultValues();
        
        setupEventHandlers();
        
        logger.debug("OpenAIConnectionForm initialized");
    }
    
    private void setupDefaultValues() {
        titleLabel.setText("New OpenAI Connection");
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
        
        if (apiKeyTextField.getText() == null || apiKeyTextField.getText().trim().isEmpty()) {
            apiKeyTextField.setStyle("-fx-border-color: red;");
            isValid = false;
        } else {
            apiKeyTextField.setStyle("");
        }
        
        return isValid;
    }
    
    
    public String getName() {
        return nameTextField.getText() != null ? nameTextField.getText().trim() : "";
    }
    
    public String getDescription() {
        return descriptionTextArea.getText() != null ? descriptionTextArea.getText().trim() : "";
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
        apiKeyTextField.clear();
    }
}
