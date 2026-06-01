package domain.logic.event;

/**
 * Event fired when the screen should flash red (trap triggered).
 * Domain classes fire this; GameView listens and applies the visual effect.
 */
public class TrapFlashEvent implements GameEvent {

    private final int frames;

    public TrapFlashEvent(int frames) {
        this.frames = frames;
    }

    @Override
    public GameEvent.Type getType() {
        return GameEvent.Type.TRAP_FLASH;
    }

    public int getFrames() { return frames; }
}
