package dev.local.ai.ui.connection.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.local.ai.core.connections.ConnectionProvider;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ConnectionsViewModel to verify MVVM implementation.
 */
public class ConnectionsViewModelTest {
    
    private ConnectionsViewModel viewModel;
    
    // @BeforeEach
    // void setUp() {
    //     viewModel = new ConnectionsViewModel();
    // }
    
    // @Test
    // void testInitialState() {
    //     // Verify initial state
    //     assertNotNull(viewModel.getConnections());
    //     assertTrue(viewModel.getConnections().size() > 0); // Should have sample data
    //     assertEquals("Ready", viewModel.getStatusMessage());
    //     assertNull(viewModel.getSelectedConnection());
    // }
    
    // @Test
    // void testAddConnection() {
    //     int initialSize = viewModel.getConnections().size();
        
    //     // Add a new connection
    //     viewModel.newConnectionFor(ConnectionProvider.OPENAI);
        
    //     // Verify connection was added
    //     assertEquals(initialSize + 1, viewModel.getConnections().size());
    //     assertTrue(viewModel.getConnections().stream()
    //         .anyMatch(conn -> "Test Connection".equals(conn.getName())));
    // }
    
    // @Test
    // void testDeleteConnection() {
    //     // Get initial size
    //     int initialSize = viewModel.getConnections().size();
    //     assertTrue(initialSize > 0);
        
    //     // Select first connection
    //     ConnectionViewModel firstConnection = viewModel.getConnections().get(0);
    //     viewModel.setSelectedConnection(firstConnection);
        
    //     // Delete selected connection
    //     viewModel.deleteSelectedConnection();
        
    //     // Verify connection was deleted
    //     assertEquals(initialSize - 1, viewModel.getConnections().size());
    //     assertFalse(viewModel.getConnections().contains(firstConnection));
    //     assertNull(viewModel.getSelectedConnection());
    // }
    
    // @Test
    // void testCanDeleteConnection() {
    //     // Initially no connection selected
    //     assertFalse(viewModel.canDeleteConnection());
        
    //     // Select a connection
    //     ConnectionViewModel connection = viewModel.getConnections().get(0);
    //     viewModel.setSelectedConnection(connection);
        
    //     // Should be able to delete
    //     assertTrue(viewModel.canDeleteConnection());
    // }
    
    // @Test
    // void testStatusMessageUpdates() {
    //     // Test status message updates when selecting connection
    //     ConnectionViewModel connection = viewModel.getConnections().get(0);
    //     viewModel.setSelectedConnection(connection);
        
    //     assertTrue(viewModel.getStatusMessage().contains("Selected:"));
    //     assertTrue(viewModel.getStatusMessage().contains(connection.getName()));
    // }
}
