package dev.local.ai.ui.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.local.ai.core.connections.ConnectionsStore;
import dev.local.ai.core.connections.ModelProviderConnection;
import dev.local.ai.ui.commands.ICommand;

public class AddNewConnectionCommand implements ICommand {
    
    private INewConnectionDialog<?> newConnectionDialog;
    private final ConnectionsStore connectionsStore;
    private Logger logger = LoggerFactory.getLogger(AddNewConnectionCommand.class);

    public AddNewConnectionCommand(INewConnectionDialog<?> newConnectionDialog, ConnectionsStore connectionsStore) {
        this.newConnectionDialog = newConnectionDialog;
        this.newConnectionDialog.onSave(this::onSave);
        this.connectionsStore = connectionsStore;
    }

    private boolean onSave(ModelProviderConnection connection) {
        try{
            connectionsStore.save(connection);
        }catch(Exception e){
            logger.error("Failed to save connection", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean execute() {
        newConnectionDialog.show();
        return true;
    }

    @Override
    public boolean undo() {
        return false;
    }

    @Override
    public boolean canExecute() {
        return true;
    }

    @Override
    public String getDescription() {
        return "Add new connection";
    }
}
