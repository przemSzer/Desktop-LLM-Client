package dev.local.ai.ui.chat.viewmodel;

import dev.local.ai.core.chat.messages.Statistics;
import dev.local.ai.ui.files.viewmodel.AttachedFileViewModel;
import javafx.beans.property.*;

import java.time.LocalDateTime;
import java.util.List;

public class ChatMessageViewModel {

    private final StringProperty content;
    private final ObjectProperty<MessageTypeView> type;
    private final ObjectProperty<LocalDateTime> timestamp;
    private final ObjectProperty<List<AttachedFileViewModel>> attachedFiles;
    private final ObjectProperty<Statistics> statistics;
    private final BooleanProperty isComplete;
    private final String id;

    public ChatMessageViewModel(String content, MessageTypeView type, List<AttachedFileViewModel> attachedFiles, Statistics statistics, String id) {
        this.content = new SimpleStringProperty(content);
        this.type = new SimpleObjectProperty<>(type);
        this.timestamp = new SimpleObjectProperty<>(LocalDateTime.now());
        this.attachedFiles = new SimpleObjectProperty<>(attachedFiles);
        this.statistics = new SimpleObjectProperty<>(statistics);
        this.isComplete = new SimpleBooleanProperty(false);
        this.id = id;
    }

    public StringProperty contentProperty() {
        return content;
    }
    
    public String getContent() {
        return content.get();
    }
    
    public void setContent(String content) {
        this.content.set(content);
    }
    
    public ObjectProperty<MessageTypeView> typeProperty() {
        return type;
    }
    
    public MessageTypeView getType() {
        return type.get();
    }
    
    public void setType(MessageTypeView type) {
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
    
    public ObjectProperty<List<AttachedFileViewModel>> attachedFilesProperty() {
        return attachedFiles;
    }
    
    public List<AttachedFileViewModel> getAttachedFiles() {
        return attachedFiles.get();
    }
    
    public void setAttachedFiles(List<AttachedFileViewModel> attachedFiles) {
        this.attachedFiles.set(attachedFiles);
    }
    
    public ObjectProperty<Statistics> statisticsProperty() {
        return statistics;
    }
    
    public Statistics getStatistics() {
        return statistics.get();
    }

    public boolean isComplete() {
        return isComplete.get();
    }

    public BooleanProperty isCompleteProperty() {
        return isComplete;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", getType().getDisplayName(), getContent());
    }


}
