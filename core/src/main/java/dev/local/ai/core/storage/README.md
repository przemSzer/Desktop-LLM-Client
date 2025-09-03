# Storage Options for Local AI Application

This package provides storage implementations for the Local AI desktop application. Currently, JSON file storage is the primary implementation, with the architecture designed to support additional storage types in the future.

## Available Storage Implementations

### JSON File Storage (`JsonFileStorage`)
**Current Implementation** - **Best for:** Development, small-scale applications, human-readable data

- **Pros:**
  - Human-readable format
  - Easy to debug and modify manually
  - No external dependencies
  - Cross-platform compatible
  - Good for version control

- **Cons:**
  - No concurrent access support
  - Limited querying capabilities
  - Manual data validation needed
  - Performance degrades with large datasets

- **Dependencies:** Jackson (com.fasterxml.jackson)

## Future Storage Implementations (Planned)

The storage architecture is designed to support additional implementations:

### SQLite Storage (`SqliteStorage`)
**Best for:** Production applications, complex queries, data integrity

- **Pros:**
  - File-based database (no server needed)
  - SQL querying capabilities
  - ACID compliance
  - Good performance for desktop apps
  - Easy backup (just copy the file)
  - Supports complex queries and relationships

- **Cons:**
  - Requires SQLite JDBC driver dependency
  - Slightly more complex than file-based solutions
  - Binary format (not human-readable)

- **Dependencies:** SQLite JDBC driver

## Usage Examples

### Basic Usage
```java
// Use default storage (JSON file)
ConnectionsStore store = new ConnectionsStore();

// Or specify a storage type (currently only JSON_FILE available)
DataStorage storage = StorageFactory.createStorage(StorageFactory.StorageType.JSON_FILE);
ConnectionsStore store = new ConnectionsStore(storage);
```

### Configuration-Based Usage
```java
StorageConfiguration config = new StorageConfiguration();
DataStorage storage = config.createStorage();
ConnectionsStore store = new ConnectionsStore(storage);
```

### Direct Storage Usage
```java
DataStorage storage = new JsonFileStorage();

// Save a connection
ModelProviderConnection connection = new OllamaConnection("My Ollama", "Local instance");
storage.saveConnection(connection);

// Load all connections
List<ModelProviderConnection> connections = storage.loadConnections();

// Delete a connection
storage.deleteConnection(connection.id());
```

## Configuration

### System Properties
```bash
-Dlocal.ai.storage.type=JSON_FILE
-Dlocal.ai.data.directory=/path/to/data
```

### Environment Variables
```bash
export LOCAL_AI_STORAGE_TYPE=JSON_FILE
export LOCAL_AI_DATA_DIRECTORY=/path/to/data
```

### Application Properties
Create `src/main/resources/application.properties`:
```properties
local.ai.storage.type=JSON_FILE
local.ai.data.directory=/path/to/data
```

## Storage Type Comparison

| Feature | JSON File (Current) | Properties (Planned) | SQLite (Planned) | In-Memory (Planned) |
|---------|-------------------|---------------------|------------------|-------------------|
| Human Readable | ✅ | ✅ | ❌ | N/A |
| Concurrent Access | ❌ | ❌ | ✅ | ✅ |
| Complex Queries | ❌ | ❌ | ✅ | ❌ |
| Data Persistence | ✅ | ✅ | ✅ | ❌ |
| Performance | Medium | Fast | Fast | Fastest |
| Dependencies | Jackson | None | SQLite JDBC | None |
| Backup | Copy file | Copy file | Copy file | N/A |
| Data Integrity | Manual | Manual | ACID | Manual |

## Recommendations

### Current Implementation
- **JSON File Storage**: Currently the only available option, perfect for development and small-scale applications

### Future Implementations (When Available)
- **For Development**: JSON File Storage (current) or In-Memory Storage for fast testing
- **For Production**: SQLite Storage for robust, queryable, ACID compliance
- **For Configuration**: Properties File Storage for settings and preferences

## Migration Between Storage Types

The `DataStorage` interface ensures compatibility between different storage implementations. When additional storage types are implemented, you can easily switch storage types by changing the factory call:

```java
// Future example: Switch from JSON to SQLite
DataStorage oldStorage = new JsonFileStorage();
DataStorage newStorage = new SqliteStorage(); // When implemented

// Migrate data
List<ModelProviderConnection> connections = oldStorage.loadConnections();
newStorage.saveConnections(connections);
```

## File Locations

By default, data is stored in:
- **Windows**: `%USERPROFILE%\.local-ai\`
- **Linux/Mac**: `~/.local-ai/`

Files created:
- JSON: `connections.json` (current implementation)
- Properties: `connections.properties` (planned)
- SQLite: `local-ai.db` (planned)

## Error Handling

All storage implementations include comprehensive error handling and logging. Failed operations return `false` and log appropriate error messages. The application continues to function even if storage operations fail.

## Thread Safety

- **JSON File (Current)**: Not thread-safe (single-user desktop app assumption)
- **Properties File (Planned)**: Not thread-safe (single-user desktop app assumption)
- **SQLite (Planned)**: Thread-safe with proper connection management
- **In-Memory (Planned)**: Thread-safe using `CopyOnWriteArrayList`

## Performance Considerations

### Current Implementation (JSON File Storage)
- **Small datasets (< 100 connections)**: Works well
- **Medium datasets (100-1000 connections)**: Acceptable performance
- **Large datasets (> 1000 connections)**: May experience performance degradation
- **High-frequency operations**: Not recommended for high-frequency operations

### Future Implementations (When Available)
- **Small datasets (< 100 connections)**: Any storage type will work well
- **Medium datasets (100-1000 connections)**: JSON or SQLite recommended
- **Large datasets (> 1000 connections)**: SQLite recommended
- **High-frequency operations**: In-memory or SQLite recommended
