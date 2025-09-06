package dev.local.ai.core.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Base implementation of Event interface.
 * Provides common functionality for all events.
 */
public abstract class BaseEvent implements Event {
    private final UUID eventId;
    private final Instant timestamp;
    private final String eventType;
    private final String source;
    
    protected BaseEvent(String eventType, String source) {
        this.eventId = UUID.randomUUID();
        this.timestamp = Instant.now();
        this.eventType = eventType;
        this.source = source;
    }
    
    @Override
    public UUID getEventId() {
        return eventId;
    }
    
    @Override
    public Instant getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String getEventType() {
        return eventType;
    }
    
    @Override
    public String getSource() {
        return source;
    }
    
    @Override
    public String toString() {
        return String.format("%s{id=%s, type=%s, source=%s, timestamp=%s}", 
            getClass().getSimpleName(), eventId, eventType, source, timestamp);
    }
}
