package dev.local.ai.core.connections;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Test class to verify that connection objects are properly serialized and deserialized.
 */
public class ConnectionSerializationTest {

    @Test
    public void testOllamaConnectionSerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        
        // Create an OllamaConnection
        OllamaConnection original = new OllamaConnection("test-ollama", "Test Ollama Connection");
        
        // Serialize to JSON
        String json = mapper.writeValueAsString(original);
        System.out.println("Serialized OllamaConnection: " + json);
        
        // Verify that all fields are present in JSON
        assertTrue(json.contains("\"id\""));
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("\"description\""));
        assertTrue(json.contains("\"baseUrl\""));
        assertTrue(json.contains("\"type\""));
        assertTrue(json.contains("\"ollama\""));
        
        // Deserialize back to object
        OllamaConnection deserialized = mapper.readValue(json, OllamaConnection.class);
        
        // Verify all fields are preserved
        assertEquals(original.id(), deserialized.id());
        assertEquals(original.name(), deserialized.name());
        assertEquals(original.description(), deserialized.description());
        assertEquals(original.baseUrl(), deserialized.baseUrl());
        assertEquals(original.providerType(), deserialized.providerType());
    }

    @Test
    public void testOpenAIConnectionSerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        
        // Create an OpenAIConnection
        OpenAIConnection original = new OpenAIConnection("test-openai", "Test OpenAI Connection");
        
        // Serialize to JSON
        String json = mapper.writeValueAsString(original);
        System.out.println("Serialized OpenAIConnection: " + json);
        
        // Verify that all fields are present in JSON
        assertTrue(json.contains("\"id\""));
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("\"description\""));
        assertTrue(json.contains("\"apiKey\""));
        assertTrue(json.contains("\"type\""));
        assertTrue(json.contains("\"openai\""));
        
        // Deserialize back to object
        OpenAIConnection deserialized = mapper.readValue(json, OpenAIConnection.class);
        
        // Verify all fields are preserved
        assertEquals(original.id(), deserialized.id());
        assertEquals(original.name(), deserialized.name());
        assertEquals(original.description(), deserialized.description());
        assertEquals(original.apiKey(), deserialized.apiKey());
        assertEquals(original.providerType(), deserialized.providerType());
    }

    @Test
    public void testGoogleConnectionSerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        GoogleConnection original = new GoogleConnection("test-google", "Test Google Connection");

        String json = mapper.writeValueAsString(original);
        System.out.println("Serialized GoogleConnection: " + json);

        assertTrue(json.contains("\"id\""));
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("\"description\""));
        assertTrue(json.contains("\"apiKey\""));
        assertTrue(json.contains("\"type\""));
        assertTrue(json.contains("\"google\""));

        GoogleConnection deserialized = mapper.readValue(json, GoogleConnection.class);

        assertEquals(original.id(), deserialized.id());
        assertEquals(original.name(), deserialized.name());
        assertEquals(original.description(), deserialized.description());
        assertEquals(original.apiKey(), deserialized.apiKey());
        assertEquals(original.providerType(), deserialized.providerType());
    }

    @Test
    public void testPolymorphicSerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        
        // Create a list with different connection types
        List<ModelProviderConnection> connections = List.of(
            new OllamaConnection("ollama-1", "Ollama Connection 1"),
            new OpenAIConnection("openai-1", "OpenAI Connection 1"),
            new GoogleConnection("google-1", "Google Connection 1")
        );
        
        // Serialize the list
        String json = mapper.writerFor(new TypeReference<List<ModelProviderConnection>>() {})
            .writeValueAsString(connections);
                
        
        assertTrue(json.contains("\"type\":\"ollama\""));
        assertTrue(json.contains("\"type\":\"openai\""));
        assertTrue(json.contains("\"type\":\"google\""));

        List<ModelProviderConnection> deserialized = mapper.readValue(json, new TypeReference<List<ModelProviderConnection>>() {});
        deserialized.forEach(connection -> {
            if (connection instanceof OllamaConnection) {
                assertEquals("ollama-1", connection.name());
            } else if (connection instanceof OpenAIConnection) {
                assertEquals("openai-1", connection.name());
            } else if (connection instanceof GoogleConnection) {
                assertEquals("google-1", connection.name());
            } else {
                fail("Unexpected connection type: " + connection.getClass().getName());
            }
        });
    }
}
