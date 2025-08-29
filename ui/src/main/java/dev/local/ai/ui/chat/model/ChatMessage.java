package dev.local.ai.ui.chat.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.time.LocalDateTime;

/**
 * Model class representing a chat message.
 * Used in the MVVM pattern to structure message data.
 */
public class ChatMessage {
    
    public enum MessageType {
        USER("User"),
        AI("AI"),
        PARTIAL("Partial"),
        SYSTEM("System"),
        ERROR("Error");
        
        private final String displayName;
        
        MessageType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // Properties for data binding
    private final StringProperty content;
    private final ObjectProperty<MessageType> type;
    private final ObjectProperty<LocalDateTime> timestamp;
    
    public ChatMessage(String content, MessageType type) {
        this.content = new SimpleStringProperty(content);
        this.type = new SimpleObjectProperty<>(type);
        this.timestamp = new SimpleObjectProperty<>(LocalDateTime.now());
    }
    
    public ChatMessage(String content, MessageType type, LocalDateTime timestamp) {
        this.content = new SimpleStringProperty(content);
        this.type = new SimpleObjectProperty<>(type);
        this.timestamp = new SimpleObjectProperty<>(timestamp);
    }
    
    // Properties for data binding
    public StringProperty contentProperty() {
        return content;
    }
    
    public String getContent() {
        return content.get();
    }
    
    public void setContent(String content) {
        this.content.set(content);
    }
    
    public ObjectProperty<MessageType> typeProperty() {
        return type;
    }
    
    public MessageType getType() {
        return type.get();
    }
    
    public void setType(MessageType type) {
        this.type.set(type);
    }
    
    public ObjectProperty<LocalDateTime> timestampProperty() {
        return timestamp;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp.get();
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp.set(timestamp);
    }
    
    @Override
    public String toString() {
        return String.format("%s: %s", getType().getDisplayName(), getContent());
    }
}
