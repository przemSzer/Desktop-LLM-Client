package dev.local.ai.ui.models.model;

import dev.local.ai.core.models.ModelInfo;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class LLMInfoViewModel {
    
    private final StringProperty name;
    private final StringProperty description;
    private final ModelInfo coreModelInfo;
    
    public LLMInfoViewModel(ModelInfo coreModelInfo) {
        this.coreModelInfo = coreModelInfo;
        this.name = new SimpleStringProperty(coreModelInfo.name());
        this.description = new SimpleStringProperty(coreModelInfo.description());
    }
    
    public ModelInfo getCoreModelInfo() {
        return coreModelInfo;
    }
        
    public String getName() {
        return name.get();
    }
    
    public StringProperty nameProperty() {
        return name;
    }
    
    public void setName(String name) {
        this.name.set(name);
    }
    
    public String getDescription() {
        return description.get();
    }
    
    public StringProperty descriptionProperty() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description.set(description);
    }
    
    @Override
    public String toString() {
        return getName();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LLMInfoViewModel that = (LLMInfoViewModel) obj;
        return coreModelInfo.equals(that.coreModelInfo);
    }
    
    @Override
    public int hashCode() {
        return coreModelInfo.hashCode();
    }
}
