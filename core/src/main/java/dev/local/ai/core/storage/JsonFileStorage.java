package dev.local.ai.core.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.local.ai.core.connections.ModelProviderConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple JSON file-based storage implementation.
 * Stores data in human-readable JSON format.
 */
public class JsonFileStorage implements DataStorage {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonFileStorage.class);
    private final ObjectMapper objectMapper;
    private final Path dataDirectory;
    private final Path connectionsFile;
    
    public JsonFileStorage() {
        this(Paths.get(System.getProperty("user.home"), ".local-ai"), "connections.json");
    }

    public JsonFileStorage(Path dataDirectory, String fileName) {
        this.objectMapper = new ObjectMapper();

        this.dataDirectory = dataDirectory;
        this.connectionsFile = dataDirectory.resolve(fileName);
        
        createDataDirectoryIfNotExists();
    }
    
    private void createDataDirectoryIfNotExists() {
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
                logger.info("Created data directory: {}", dataDirectory);
            }
        } catch (IOException e) {
            logger.error("Failed to create data directory: {}", dataDirectory, e);
        }
    }
    
    @Override
    public List<ModelProviderConnection> loadConnections() {
        try {
            if (!Files.exists(connectionsFile)) {
                logger.info("Connections file does not exist, returning empty list");
                return new ArrayList<>();
            }
            
            List<ModelProviderConnection> connections = objectMapper.readValue(
                connectionsFile.toFile(), new TypeReference<List<ModelProviderConnection>>() {});
            
            logger.info("Loaded {} connections from {}", connections.size(), connectionsFile);
            return connections;
            
        } catch (IOException e) {
            logger.error("Failed to load connections from {}", connectionsFile, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public boolean saveConnections(List<ModelProviderConnection> connections) {
        try {
            objectMapper
            .writerFor(new TypeReference<List<ModelProviderConnection>>() {})
            .withDefaultPrettyPrinter()
            .writeValue(connectionsFile.toFile(), connections);
            
            logger.info("Saved {} connections to {}", connections.size(), connectionsFile);
            return true;
            
        } catch (IOException e) {
            logger.error("Failed to save connections to {}", connectionsFile, e);
            return false;
        }
    }
    
    @Override
    public boolean saveConnection(ModelProviderConnection connection) {
        List<ModelProviderConnection> connections = loadConnections();
        
        // Remove existing connection with same ID if it exists
        connections.removeIf(c -> c.id().equals(connection.id()));
        
        // Add the new/updated connection
        connections.add(connection);
        
        return saveConnections(connections);
    }
    
    @Override
    public boolean deleteConnection(String connectionId) {
        List<ModelProviderConnection> connections = loadConnections();
        
        boolean removed = connections.removeIf(c -> c.id().equals(connectionId));
        
        if (removed) {
            return saveConnections(connections);
        }
        
        return false;
    }

    public Path getConnectionsFile() {
        return connectionsFile;
    }
}
