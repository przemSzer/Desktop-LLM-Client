package dev.local.ai.core.connections;

import dev.local.ai.core.storage.DataStorage;
import dev.local.ai.core.storage.StorageFactory;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Store for managing AI model provider connections.
 * Delegates actual storage operations to a configurable DataStorage implementation.
 */
public class ConnectionsStore {

    private final Logger logger = LoggerFactory.getLogger(ConnectionsStore.class);
    private final DataStorage storage;

    public ConnectionsStore() {
        this(StorageFactory.createDefaultStorage());
    }
    
    public ConnectionsStore(DataStorage storage) {
        this.storage = storage;
        logger.info("ConnectionsStore initialized with storage: {}", storage.getClass().getSimpleName());
    }

    
    public List<ModelProviderConnection> readAll() {
        return storage.loadConnections();
    }

    public Optional<ModelProviderConnection> findById(String connectionId) {
        if (connectionId == null) {
            return Optional.empty();
        }
        return readAll().stream()
            .filter(connection -> connectionId.equals(connection.id()))
            .findFirst();
    }

    public boolean save(ModelProviderConnection connection) {
        logger.info("Saving connection: {}", connection.name());
        return storage.saveConnection(connection);
    }
    
    
    public boolean delete(String connectionId) {
        logger.info("Deleting connection with ID: {}", connectionId);
        return storage.deleteConnection(connectionId);
    }
    
    
    public boolean saveAll(List<ModelProviderConnection> connections) {
        logger.info("Saving {} connections", connections.size());
        return storage.saveConnections(connections);
    }
}
