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
        new File("resources/images").mkdirs(); // Search for images in this directory
    }

    public BufferedImage getTile(String name) {
        if (name == null)
            return null;

        if (tileCache.containsKey(name)) {
            return tileCache.get(name);
        }

        try {
            File tileFile = findImageFile(new File("resources/images"), name); // Find image file
            if (tileFile != null && tileFile.exists()) {
                BufferedImage img = ImageIO.read(tileFile);
                tileCache.put(name, img);
                return img;
            }
        } catch (IOException e) {
            System.err.println("Tile yüklenirken hata (" + name + "): " + e.getMessage());
        }

        return null;
    }

    private File findImageFile(File dir, String targetName) {
        if (!dir.exists() || !dir.isDirectory()) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isDirectory()) {
                File found = findImageFile(f, targetName);
                if (found != null) return found;
            } else if (f.getName().equals(targetName + ".png")) {
                return f;
            }
        }
        return null;
    }

    public int getTileSize() {
        return ACTUAL_SIZE;
    }
}