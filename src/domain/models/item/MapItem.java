package domain.models.item;

import domain.models.entity.GameObject;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public abstract class MapItem extends GameObject {
    private BufferedImage sprite;

    public MapItem(String name, int x, int y, String imagePath) {
        // Passable = true, we pass dummy image name for GameObjects compatibility
        super(name, x, y, "item_placeholder", true); 
        this.imageName = imagePath;
        loadSprite(imagePath);
    }

    private void loadSprite(String imagePath) {
        try {
            File tileFile = new File("resources/" + imagePath);
            if (tileFile.exists()) {
                this.sprite = ImageIO.read(tileFile);
            } else {
                System.err.println("Item resmi bulunamadı: " + tileFile.getAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Resim yüklenirken hata (" + imagePath + "): " + e.getMessage());
        }
    }

    public BufferedImage getSprite() {
        return sprite;
    }

    // Default weapon rendering metadata for high-quality custom offsets
    public double getWeaponPivotX() { return 0.5; }
    public double getWeaponPivotY() { return 0.5; }
    public double getWeaponAngleOffset() { return 0.0; }
    public int getHandOffsetX() { return 0; }
    public int getHandOffsetY() { return 0; }
}
