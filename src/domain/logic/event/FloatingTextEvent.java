package domain.logic.event;

import java.awt.Color;

/**
 * Event fired when floating damage/feedback text should appear on screen.
 * Domain classes fire this; GameView renders it.
 */
public class FloatingTextEvent implements GameEvent {

    private final double x;
    private final double y;
    private final String text;
    private final Color color;

    public FloatingTextEvent(double x, double y, String text, Color color) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = color;
    }

    @Override
    public GameEvent.Type getType() {
        return GameEvent.Type.FLOATING_TEXT;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public String getText() { return text; }
    public Color getColor() { return color; }
}
