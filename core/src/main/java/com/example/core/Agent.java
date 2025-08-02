package com.example.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core agent class representing the main business logic.
 */
public class Agent {
    private static final Logger logger = LoggerFactory.getLogger(Agent.class);
    
    private String name;
    private AgentStatus status;
    
    public Agent(String name) {
        this.name = name;
        this.status = AgentStatus.IDLE;
        logger.info("Agent '{}' created", name);
    }
    
    public String getName() {
        return name;
    }
    
    public AgentStatus getStatus() {
        return status;
    }
    
    public void setStatus(AgentStatus status) {
        this.status = status;
        logger.info("Agent '{}' status changed to: {}", name, status);
    }
    
    public void performTask(String task) {
        logger.info("Agent '{}' performing task: {}", name, task);
        setStatus(AgentStatus.BUSY);
        // Simulate task execution
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Task interrupted for agent '{}'", name);
        }
        setStatus(AgentStatus.IDLE);
        logger.info("Agent '{}' completed task: {}", name, task);
    }
    
    @Override
    public String toString() {
        return "Agent{name='" + name + "', status=" + status + "}";
    }
} 