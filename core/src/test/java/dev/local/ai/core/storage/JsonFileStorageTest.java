package dev.local.ai.core.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.local.ai.core.connections.OllamaConnection;
import dev.local.ai.core.connections.OpenAIConnection;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class JsonFileStorageTest {

    @TempDir
    private Path tempDir;

    private JsonFileStorage storage;

    private String folderName = "test-connections";

    @BeforeEach
    void setUp() {  
        storage = new JsonFileStorage(tempDir.resolve(folderName), "connections.json");
    }
    
    @AfterEach
    void tearDown() {
        storage.getConnectionsFile().toFile().delete();
        tempDir.resolve(folderName).toFile().delete();
    }

    @Test
    void testDeleteConnection() {        
        var ollamaConnection = new OllamaConnection("test-ollama", "Test Ollama Connection");
        var openaiConnection = new OpenAIConnection("test-openai", "Test OpenAI Connection");
        
        storage.saveConnections(List.of(ollamaConnection, openaiConnection));
                
        storage.deleteConnection(ollamaConnection.id());
        
        assertThat(storage.loadConnections())
            .hasSize(1)
            .contains(openaiConnection);
    }

    @Test
    void deletingNonExistingConnection() {
        var connection = new OllamaConnection(UUID.randomUUID().toString(), "test-ollama", "Test Ollama Connection", "http://non-localhost:234234/");
        storage.saveConnections(List.of(connection));
        
        assertThat(storage.deleteConnection(UUID.randomUUID().toString()))
            .isFalse();

        var connections = storage.loadConnections();
        assertThat(connections)
            .hasSize(1)
            .contains(connection);
    }


    @Test
    void testLoadConnections() {
        var connection = new OllamaConnection(UUID.randomUUID().toString(), "test-ollama", "Test Ollama Connection", "http://non-localhost:234234/");
        var openaiConnection = new OpenAIConnection(UUID.randomUUID().toString(), "test-openai", "Test OpenAI Connection", "API_KEY");
        storage.saveConnections(List.of(connection, openaiConnection));

        var connections = storage.loadConnections();
        assertThat(connections)
            .hasSize(2)
            .contains(connection, openaiConnection);
    }

    @Test
    void ifNoConnectionsThenEmptyList() {
        assertThat(storage.loadConnections())
            .isEmpty();
    }

    @Test
    void testSaveSingleConnection() {
        var connection = new OllamaConnection(UUID.randomUUID().toString(), "test-ollama", "Test Ollama Connection", "http://non-localhost:234234/");
        var openaiConnection = new OpenAIConnection(UUID.randomUUID().toString(), "test-openai", "Test OpenAI Connection", "API_KEY");
        
        storage.saveConnections(List.of(connection, openaiConnection));

        var newConnection = new OllamaConnection(UUID.randomUUID().toString(), "test-ollama2", "Test Ollama Connection 2", "http://non-localhost:2234/");        
        
        storage.saveConnection(newConnection);

        var connections = storage.loadConnections();
        assertThat(connections)
            .hasSize(3)
            .contains(connection, openaiConnection, newConnection);
    }

    @Test
    void testSaveConnections() {
        var connection = new OllamaConnection(UUID.randomUUID().toString(), "test-ollama", "Test Ollama Connection", "http://non-localhost:234234/");
        var openaiConnection = new OpenAIConnection(UUID.randomUUID().toString(), "test-openai", "Test OpenAI Connection", "API_KEY");
        
        storage.saveConnections(List.of(connection, openaiConnection));

        var connections = storage.loadConnections();
        assertThat(connections)
            .hasSize(2)
            .contains(connection, openaiConnection);
    }

}
