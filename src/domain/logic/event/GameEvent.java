package domain.logic.event;

/**
 * Base interface for all game events published through the GameEventBus.
 * Domain classes fire events; View/Sound layers listen and react.
 * This decouples the domain from the view layer (Model-View Separation).
 */
public interface GameEvent {

    enum Type {
        FLOATING_TEXT,
        SOUND,
        TRAP_FLASH
    }

    Type getType();
}
