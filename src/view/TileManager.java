package view;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class TileManager {
    private Map<String, BufferedImage> tileCache = new HashMap<>();
    private final int TILE_SIZE = 64;
    private final int ACTUAL_SIZE = TILE_SIZE;

    public TileManager() {
        new File("resources/images/tiles").mkdirs();
    }

    public BufferedImage getTile(String name) {
        if (name == null)
            return null;

        if (tileCache.containsKey(name)) {
            return tileCache.get(name);
        }

        try {
            File tileFile = new File("resources/images/tiles/" + name + ".png");
            if (tileFile.exists()) {
                BufferedImage img = ImageIO.read(tileFile);
                tileCache.put(name, img);
                return img;
            }
        } catch (IOException e) {
            System.err.println("Tile yüklenirken hata (" + name + "): " + e.getMessage());
        }

        return null;
    }

    public int getTileSize() {
        return ACTUAL_SIZE;
    }
}