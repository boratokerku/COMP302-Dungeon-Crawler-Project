package ui.design;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class ActionBtn {
    public final String label;
    public final Color bgColor;
    public final String spritePath;
    public final Runnable action;
    public BufferedImage sprite;
    public Rectangle bounds = new Rectangle();

    public ActionBtn(String label, Color bgColor, String spritePath, Runnable action) {
        this.label = label;
        this.bgColor = bgColor;
        this.spritePath = spritePath;
        this.action = action;
    }
}
