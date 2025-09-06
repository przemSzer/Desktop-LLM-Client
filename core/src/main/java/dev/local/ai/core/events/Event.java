package dev.local.ai.core.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all events in the application.
 * Events are immutable data carriers that represent something that happened.
 */
public interface Event {
    /**
     * Unique identifier for this event instance
     */
    UUID getEventId();
    
    /**
     * Timestamp when the event occurred
     */
    Instant getTimestamp();
    
    /**
     * Type of the event (for routing and filtering)
     */
    String getEventType();
    
    /**
     * Source that generated this event
     */
    String getSource();
}
