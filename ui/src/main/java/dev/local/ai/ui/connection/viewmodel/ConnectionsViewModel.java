package dev.local.ai.ui.connection.viewmodel;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.core.connections.ConnectionsStore;
import dev.local.ai.core.connections.ConnectionProvider;
import dev.local.ai.ui.commands.CommandManager;
import dev.local.ai.ui.commands.CommandManagerProvider;
import dev.local.ai.ui.connection.AddNewConnectionCommand;
import dev.local.ai.ui.connection.INewConnectionDialog;
import dev.local.ai.ui.connection.google.GoogleConnectionDialog;
import dev.local.ai.ui.connection.ollama.OllamaConnectionDialog;
import dev.local.ai.ui.connection.openai.OpenAIConnectionDialog;

/**
 * ViewModel for the Connections UI following MVVM pattern.
 * Manages the observable data and commands for the connections interface.
 */
public class ConnectionsViewModel {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionsViewModel.class);
    
    private final ListProperty<ConnectionViewModel> connections;
    private final StringProperty statusMessage;
    private final ObjectProperty<ConnectionViewModel> selectedConnection;
    private ConnectionsStore connectionsStore;
    private CommandManager commandManager;

    public ConnectionsViewModel() {
        this.connections = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.statusMessage = new SimpleStringProperty("Ready");
        this.selectedConnection = new SimpleObjectProperty<>();
        this.connectionsStore = new ConnectionsStore();

        reloadAllConnections();

        logger.info("ConnectionsViewModel initialized");
        this.commandManager = CommandManagerProvider.get();
    }

    private void reloadAllConnections() {
        var mappedConnections = connectionsStore
            .readAll().stream()
            .map(connection -> new ConnectionViewModel(connection.providerType(), connection.name(), connection.description(), connection.id()))
            .toList();
        this.connections.set(FXCollections.observableArrayList(mappedConnections));
    }

    public ListProperty<ConnectionViewModel> connectionsProperty() {
        return connections;
    }

    public ObservableList<ConnectionViewModel> getConnections() {
        return connections.get();
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public String getStatusMessage() {
        return statusMessage.get();
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage.set(statusMessage);
    }

    public ObjectProperty<ConnectionViewModel> selectedConnectionProperty() {
        return selectedConnection;
    }

    public ConnectionViewModel getSelectedConnection() {
        return selectedConnection.get();
    }

    public void setSelectedConnection(ConnectionViewModel connection) {
        this.selectedConnection.set(connection);
        updateStatusMessage();
    }

    private void updateStatusMessage() {
        ConnectionViewModel selected = getSelectedConnection();
        if (selected != null) {
            setStatusMessage("Selected: " + selected.getName());
        } else {
            setStatusMessage("Ready");
        }
    }
    
    public void deleteConnection(ConnectionViewModel connection) {
        if (connection != null) {
            connections.remove(connection);
            if (getSelectedConnection() == connection) {
                setSelectedConnection(null);
            }
            //TODO: provide an action for it
            connectionsStore.delete(connection.getId());
            setStatusMessage("Deleted connection: " + connection.getName());
            logger.info("Deleted connection: {}", connection.getName());
        }
    }

    public void deleteSelectedConnection() {
        ConnectionViewModel selected = getSelectedConnection();
        if (selected != null) {
            deleteConnection(selected);
        }
    }

    public boolean canDeleteConnection() {
        return getSelectedConnection() != null;
    }

    public void newConnectionFor(ConnectionProvider providerType) {
        var dialogForProvider = getDialog(providerType);
        commandManager.executeCommand(new AddNewConnectionCommand(dialogForProvider, connectionsStore));
        reloadAllConnections();
    }

    private INewConnectionDialog<?> getDialog(ConnectionProvider providerType) {
        switch (providerType) {
            case ConnectionProvider.OPENAI:
                return new OpenAIConnectionDialog();                
            case ConnectionProvider.OLLAMA:
                return new OllamaConnectionDialog();
            case ConnectionProvider.GOOGLE:
                return new GoogleConnectionDialog();
            default:
                throw new IllegalArgumentException("Unknown provider type: " + providerType);
        }        
    }
}
