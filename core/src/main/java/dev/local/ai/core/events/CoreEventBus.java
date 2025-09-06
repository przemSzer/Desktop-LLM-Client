package dev.local.ai.core.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Core event bus for business logic events.
 * Thread-safe, can be used from any thread with cached executor.
 */
public class CoreEventBus {
    private static final Logger logger = LoggerFactory.getLogger(CoreEventBus.class);
    
    private final Map<String, List<EventListener<Event>>> listeners = new ConcurrentHashMap<>();
    private final ExecutorService executor;
    private volatile boolean shutdown = false;
    
    public static class CoreEventBusThreadFactory implements ThreadFactory {
        private int threadCounter = 0;
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "core-event-bus-" + (++threadCounter));
            t.setDaemon(true);
            return t;
        }
    };

    public CoreEventBus() {
        this.executor = Executors.newCachedThreadPool(new CoreEventBusThreadFactory());
        logger.info("CoreEventBus initialized with cached thread pool");        
    }
        

    /**
     * Subscribe to events of a specific type
     * @param eventType the type of events to listen for
     * @param listener the listener to call when events occur
     * @param <T> the event type
     */
    public <T extends Event> void subscribe(String eventType, EventListener<T> listener) {
        if (shutdown) {
            logger.warn("Cannot subscribe to event bus after shutdown");
            return;
        }
        
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add((EventListener<Event>) listener);
        
        logger.debug("Subscribed listener named {} (class:{}) to event type {}", 
            listener.name(), listener.getClass().getSimpleName(), eventType);
    }
    
    /**
     * Unsubscribe a listener from a specific event type
     * @param eventType the event type
     * @param listener the listener to remove
     */
    public void unsubscribe(String eventType, EventListener<?> listener) {
        List<EventListener<Event>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
            logger.debug(
                "Unsubscribed listener named:{} (class:{}) from event type {}", 
                    listener.name(), listener.getClass().getSimpleName(), eventType
                );
        }
    }
    
    /**
     * Publish an event asynchronously using cached executor
     * @param event the event to publish
     */
    public void publish(Event event) {
        if (shutdown) {
            logger.warn("Cannot publish events after shutdown");
            return;
        }
        
        String eventType = event.getEventType();
        List<EventListener<Event>> eventListeners = listeners.get(eventType);
        
        if (eventListeners != null && !eventListeners.isEmpty()) {
            logger.debug("Publishing event {} to {} listeners", event, eventListeners.size());
            
            // Submit to cached executor for asynchronous processing
            executor.submit(() -> {
                for (EventListener<Event> listener : eventListeners) {
                    try {
                        listener.onEvent(event);
                    } catch (Exception e) {
                        logger.error("Error in event listener {} for event {}", 
                            listener.getClass().getSimpleName(), event, e);
                    }
                }
            });
        } else {
            logger.debug("No listeners for event type: {}", eventType);
        }
    }
    
    /**
     * Publish an event synchronously (for testing or immediate processing)
     * @param event the event to publish
     */
    public void publishSync(Event event) {
        if (shutdown) {
            logger.warn("Cannot publish events after shutdown");
            return;
        }
        
        String eventType = event.getEventType();
        List<EventListener<Event>> eventListeners = listeners.get(eventType);
        
        if (eventListeners != null && !eventListeners.isEmpty()) {
            logger.debug("Publishing event {} synchronously to {} listeners", event, eventListeners.size());
            
            for (EventListener<Event> listener : eventListeners) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    logger.error("Error in event listener {} for event {}", 
                        listener.getClass().getSimpleName(), event, e);
                }
            }
        } else {
            logger.debug("No listeners for event type: {}", eventType);
        }
    }
    
    /**
     * Get the number of listeners for a specific event type
     * @param eventType the event type
     * @return number of listeners
     */
    public int getListenerCount(String eventType) {
        List<EventListener<Event>> eventListeners = listeners.get(eventType);
        return eventListeners != null ? eventListeners.size() : 0;
    }
    
    /**
     * Get all registered event types
     * @return set of event types
     */
    public Set<String> getRegisteredEventTypes() {
        return new HashSet<>(listeners.keySet());
    }
    
    /**
     * Get statistics about the event bus
     * @return EventBusStats object
     */
    public EventBusStats getStats() {
        int totalListeners = listeners.values().stream()
            .mapToInt(List::size)
            .sum();
        
        return new EventBusStats(
            listeners.size(),
            totalListeners,
            executor.isShutdown()
        );
    }
    
    /**
     * Shutdown the event bus and executor
     */
    public void shutdown() {
        shutdown = true;
        executor.shutdown();
        
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                logger.warn("Event bus executor did not terminate gracefully");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("CoreEventBus shutdown");
    }
    
    /**
     * Statistics about the event bus
     */
    public static class EventBusStats {
        private final int totalEventTypes;
        private final int totalListeners;
        private final boolean isShutdown;
        
        public EventBusStats(int totalEventTypes, int totalListeners, boolean isShutdown) {
            this.totalEventTypes = totalEventTypes;
            this.totalListeners = totalListeners;
            this.isShutdown = isShutdown;
        }
        
        public int getTotalEventTypes() { return totalEventTypes; }
        public int getTotalListeners() { return totalListeners; }
        public boolean isShutdown() { return isShutdown; }
        
        @Override
        public String toString() {
            return String.format("EventBusStats{eventTypes=%d, listeners=%d, shutdown=%s}", 
                totalEventTypes, totalListeners, isShutdown);
        }
    }
}


