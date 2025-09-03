package dev.local.ai.core.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating storage implementations.
 * Provides a centralized way to configure and create storage instances.
 */
public class StorageFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(StorageFactory.class);
    
    public enum StorageType {
        JSON_FILE
    }
    
    /**
     * Create a storage instance based on the specified type.
     * @param type The type of storage to create
     * @return DataStorage instance
     */
    public static DataStorage createStorage(StorageType type) {
        return switch (type) {
            case JSON_FILE -> {
                logger.info("Creating JSON file storage");
                yield new JsonFileStorage();
            }        
        };
    }
    
    /**
     * Create the default storage implementation.
     * Currently defaults to JSON file storage for simplicity.
     * @return Default DataStorage instance
     */
    public static DataStorage createDefaultStorage() {
        return createStorage(StorageType.JSON_FILE);
    }
}
