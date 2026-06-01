package view.design;

import domain.models.GameObject;
import java.awt.image.BufferedImage;
import java.util.function.BiFunction;

public class PaletteItem {
    public final String label;
    public final String iconPath;
    public final boolean isTileIcon;
    public final BiFunction<Integer, Integer, GameObject> factory;
    public BufferedImage icon;

    public PaletteItem(String label, String iconPath, boolean isTileIcon,
                       BiFunction<Integer, Integer, GameObject> factory) {
        this.label = label;
        this.iconPath = iconPath;
        this.isTileIcon = isTileIcon;
        this.factory = factory;
    }
}
