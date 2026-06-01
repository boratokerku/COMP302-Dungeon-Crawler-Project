package domain.logic.event;

/**
 * Listener interface for GameEventBus subscribers.
 * Implement this in any class that needs to react to domain events
 * (e.g., GameView for rendering, SoundManager for audio).
 */
@FunctionalInterface
public interface GameEventListener {
    void onEvent(GameEvent event);
}
