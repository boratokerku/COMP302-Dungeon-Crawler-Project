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
            String path = "resources/images/tiles/" + name + ".png";
            File tileFile = new File(path);
            if (!tileFile.exists()) {
                tileFile = new File("../" + path);
            }
            
            if (tileFile.exists()) {
                BufferedImage img = ImageIO.read(tileFile);
                tileCache.put(name, img);
                return img;
            }
        } catch (IOException e) {
            System.err.println("Tile yüklenirken hata (" + name + "): " + e.getMessage());
        }

        // Eğer zemin varyasyonlarından biriyse ve bulunamadıysa (örneğin floor_crack), normal floor döndür.
        if (name != null && name.startsWith("floor_")) {
            System.out.println("Varyasyon bulunamadı (" + name + "), normal floor kullanılıyor.");
            return getTile("floor");
        }

        return null;
    }

    public int getTileSize() {
        return ACTUAL_SIZE;
    }
}