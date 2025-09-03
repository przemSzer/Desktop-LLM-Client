package dev.local.ai.core.storage;

import dev.local.ai.core.connections.ModelProviderConnection;
import java.util.List;

/**
 * Interface for data storage operations.
 * Provides a common contract for different storage implementations.
 */
public interface DataStorage {
    
    /**
     * Load all connections from storage.
     * @return List of all stored connections
     */
    List<ModelProviderConnection> loadConnections();
    
    /**
     * Save all connections to storage.
     * @param connections List of connections to save
     * @return true if successful, false otherwise
     */
    boolean saveConnections(List<ModelProviderConnection> connections);
    
    /**
     * Save a single connection to storage.
     * @param connection Connection to save
     * @return true if successful, false otherwise
     */
    boolean saveConnection(ModelProviderConnection connection);
    
    /**
     * Delete a connection from storage.
     * @param connectionId ID of the connection to delete
     * @return true if successful, false otherwise
     */
    boolean deleteConnection(String connectionId);
}
