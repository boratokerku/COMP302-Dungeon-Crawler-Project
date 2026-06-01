package domain.logic.event;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight publish-subscribe event bus.
 *
 * <p>Domain classes (Hero, Actions) PUBLISH events here instead of calling
 * view.GameView or util.helpers.SoundManager directly. This enforces
 * Model-View Separation (GRASP) and Low Coupling.</p>
 *
 * <p>The view layer (GameView, DemoRunner setup code) SUBSCRIBES via
 * {@link #addListener(GameEventListener)} to react to domain events.</p>
 *
 * <p>Pure Fabrication pattern: this class has no domain counterpart — it
 * exists purely to reduce coupling between layers.</p>
 */
public final class GameEventBus {

    private static final List<GameEventListener> listeners = new ArrayList<>();

    // Private constructor — utility class, not instantiatable
    private GameEventBus() {}

    /** Register a listener that will receive all published events. */
    public static void addListener(GameEventListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /** Remove a previously registered listener. */
    public static void removeListener(GameEventListener listener) {
        listeners.remove(listener);
    }

    /** Remove all listeners (call when tearing down a game session). */
    public static void clearListeners() {
        listeners.clear();
    }

    /**
     * Publish an event to all registered listeners.
     * Listeners are notified synchronously on the calling thread.
     */
    public static void publish(GameEvent event) {
        for (GameEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }

    // ── Convenience helpers ────────────────────────────────────────────────

    /** Convenience: publish a floating text event. */
    public static void fireFloatingText(double x, double y, String text, java.awt.Color color) {
        publish(new FloatingTextEvent(x, y, text, color));
    }

    /** Convenience: publish a sound event. */
    public static void fireSound(SoundEvent.SoundType type) {
        publish(new SoundEvent(type));
    }

    /** Convenience: publish a trap flash event. */
    public static void fireTrapFlash(int frames) {
        publish(new TrapFlashEvent(frames));
    }
}
