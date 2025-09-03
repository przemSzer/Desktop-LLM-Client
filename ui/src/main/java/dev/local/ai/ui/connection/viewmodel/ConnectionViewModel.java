package dev.local.ai.ui.connection.viewmodel;

import dev.local.ai.core.connections.ConnectionProvider;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;

/**
 * Model class representing a connection to an AI provider.
 * Used in the MVVM pattern to structure connection data.
 */
public class ConnectionViewModel {
        
    private final ObjectProperty<ConnectionProvider> providerType;
    private final StringProperty name;
    private final StringProperty description;
    private final ObjectProperty<Image> icon;
    private final String id;
    
    public ConnectionViewModel(ConnectionProvider providerType, String name, String description, String id) {
        this.providerType = new SimpleObjectProperty<>(providerType);
        this.name = new SimpleStringProperty(name);
        this.description = new SimpleStringProperty(description);
        this.icon = new SimpleObjectProperty<>();
        this.id = id;
        loadIcon();
    }
    
    public String getId() {
        return id;
    }
    
    private void loadIcon() {
        try {
            String iconPath = providerType.get().getIconPath();
            Image iconImage = new Image(getClass().getResourceAsStream(iconPath));
            this.icon.set(iconImage);
        } catch (Exception e) {
            this.icon.set(null);
        }
    }
    
    public ObjectProperty<ConnectionProvider> providerTypeProperty() {
        return providerType;
    }
    
    public ConnectionProvider getProviderType() {
        return providerType.get();
    }
    
    public void setProviderType(ConnectionProvider providerType) {
        this.providerType.set(providerType);
        loadIcon();
    }
    
    public StringProperty nameProperty() {
        return name;
    }
    
    public String getName() {
        return name.get();
    }
    
    public void setName(String name) {
        this.name.set(name);
    }
    
    public StringProperty descriptionProperty() {
        return description;
    }
    
    public String getDescription() {
        return description.get();
    }
    
    public void setDescription(String description) {
        this.description.set(description);
    }
    
    public ObjectProperty<Image> iconProperty() {
        return icon;
    }
    
    public Image getIcon() {
        return icon.get();
    }
    
    public void setIcon(Image icon) {
        this.icon.set(icon);
    }
    
    @Override
    public String toString() {
        return String.format("%s: %s", getProviderType().getDisplayName(), getName());
    }
}
