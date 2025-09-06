package dev.local.ai.core.events;

/**
 * Provider for the global core event bus instance.
 * Implements singleton pattern using the Initialization-on-demand holder idiom
 * for thread-safe, lazy initialization without synchronization overhead.
 */
public class CoreEventBusProvider {
    
    /**
     * Private constructor to prevent instantiation
     */
    private CoreEventBusProvider() {
        // Prevent instantiation
    }
    
    /**
     * Static inner class that holds the singleton instance.
     * This approach is thread-safe because the JVM guarantees that class loading
     * is thread-safe, and the instance is only created when the inner class
     * is first accessed.
     */
    private static class CoreEventBusHolder {
        private static final CoreEventBus INSTANCE = new CoreEventBus();
    }
    
    /**
     * Get the global core event bus instance
     * @return the core event bus instance
     */
    public static CoreEventBus getInstance() {
        return CoreEventBusHolder.INSTANCE;
    }
       
}
