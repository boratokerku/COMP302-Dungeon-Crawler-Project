package domain.models.item;

import domain.models.entity.GameObject;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public abstract class MapItem extends GameObject {
    private BufferedImage sprite;

    public MapItem(String name, int x, int y, String imagePath) {
        // GameObject constructor takes (int x, int y, String imageName, boolean passable)
        super(x, y, "item_placeholder", true);
        loadSprite(imagePath);
    }

    private void loadSprite(String imagePath) {
        try {
            String path = "resources/" + imagePath;
            File tileFile = new File(path);
            if (!tileFile.exists()) {
                tileFile = new File("../" + path);
            }

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
}
