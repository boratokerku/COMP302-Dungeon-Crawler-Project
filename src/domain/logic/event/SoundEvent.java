package domain.logic.event;

/**
 * Event fired when a sound effect should be played.
 * Domain classes fire this; SoundManager listens and plays the sound.
 */
public class SoundEvent implements GameEvent {

    public enum SoundType {
        WALK, SWING, HEAL, SHOOT, UNLOCK, ENEMY_HIT, VICTORY
    }

    private final SoundType soundType;

    public SoundEvent(SoundType soundType) {
        this.soundType = soundType;
    }

    @Override
    public GameEvent.Type getType() {
        return GameEvent.Type.SOUND;
    }

    public SoundType getSoundType() { return soundType; }
}
