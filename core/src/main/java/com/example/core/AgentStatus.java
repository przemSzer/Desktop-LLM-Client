package com.example.core;

/**
 * Enum representing the possible statuses of an agent.
 */
public enum AgentStatus {
    IDLE("Idle"),
    BUSY("Busy"),
    OFFLINE("Offline"),
    ERROR("Error");
    
    private final String displayName;
    
    AgentStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
} 