package dev.local.ai.core.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration class for storage settings.
 * Reads storage configuration from application properties or environment variables.
 */
public class StorageConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(StorageConfiguration.class);
    private static final String DEFAULT_STORAGE_TYPE = "JSON_FILE";
    
    private final StorageFactory.StorageType storageType;
    private final String dataDirectory;
    
    public StorageConfiguration() {
        this.storageType = loadStorageType();
        this.dataDirectory = loadDataDirectory();
        
        logger.info("Storage configuration: type={}, directory={}", storageType, dataDirectory);
    }
    
    private StorageFactory.StorageType loadStorageType() {
        // Try to load from system property first
        String storageTypeStr = System.getProperty("local.ai.storage.type");
        
        // Then try environment variable
        if (storageTypeStr == null) {
            storageTypeStr = System.getenv("LOCAL_AI_STORAGE_TYPE");
        }
        
        // Then try application properties
        if (storageTypeStr == null) {
            storageTypeStr = loadFromProperties("storage.type");
        }
        
        // Default fallback
        if (storageTypeStr == null) {
            storageTypeStr = DEFAULT_STORAGE_TYPE;
        }
        
        try {
            return StorageFactory.StorageType.valueOf(storageTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid storage type '{}', using default: {}", storageTypeStr, DEFAULT_STORAGE_TYPE);
            return StorageFactory.StorageType.valueOf(DEFAULT_STORAGE_TYPE);
        }
    }
    
    private String loadDataDirectory() {
        // Try to load from system property first
        String directory = System.getProperty("local.ai.data.directory");
        
        // Then try environment variable
        if (directory == null) {
            directory = System.getenv("LOCAL_AI_DATA_DIRECTORY");
        }
        
        // Then try application properties
        if (directory == null) {
            directory = loadFromProperties("data.directory");
        }
        
        // Default fallback to user home
        if (directory == null) {
            directory = System.getProperty("user.home") + "/.local-ai";
        }
        
        return directory;
    }
    
    private String loadFromProperties(String key) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);
                return props.getProperty("local.ai." + key);
            }
        } catch (IOException e) {
            logger.debug("Could not load application.properties: {}", e.getMessage());
        }
        return null;
    }
    
    public StorageFactory.StorageType getStorageType() {
        return storageType;
    }
    
    public String getDataDirectory() {
        return dataDirectory;
    }
    
    /**
     * Create a storage instance based on this configuration.
     * @return Configured DataStorage instance
     */
    public DataStorage createStorage() {
        return StorageFactory.createStorage(storageType);
    }
}
