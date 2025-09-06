package dev.local.ai.core.events;

/**
 * Interface for components that want to listen to events.
 * @param <T> the type of events this listener handles
 */
@FunctionalInterface
public interface EventListener<T extends Event> {
    /**
     * Called when an event of type T is published
     * @param event the event that occurred
     */
    void onEvent(T event);

    default String name(){
        return getClass().getSimpleName();
    }
}
