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

    public BufferedImage getTile(String name, int x, int y) {
        if ("floor".equals(name)) {
            if (isRockyFloor(x, y)) {
                BufferedImage rocky = getTile("floor/floor2");
                if (rocky != null) {
                    return rocky;
                }
            }
            return getTile("floor/floor1");
        }
        return getTile(name);
    }

    private boolean isRockyFloor(int x, int y) {
        // Deterministic organic cluster noise formula with offsets to avoid corners,
        // and higher threshold to make clusters smaller and less frequent.
        double val = Math.sin((x + 3) * 0.45) * Math.cos((y + 3) * 0.45) + Math.sin(x * 0.2 + y * 0.3);
        return val > 0.65;
    }

    public BufferedImage getTile(String name) {
        if (name == null)
            return null;

        if ("floor".equals(name)) {
            BufferedImage img = getTile("floor/floor1");
            if (img != null) {
                tileCache.put(name, img);
                return img;
            }
        }

        if (tileCache.containsKey(name)) {
            return tileCache.get(name);
        }

        File foundFile = null;
        String[] prefixes = {
            "resources/images/tiles/",
            "resources/images/",
            "resources/"
        };

        for (String prefix : prefixes) {
            String path = prefix + name;
            File f1 = new File(path);
            if (f1.exists() && f1.isFile()) {
                foundFile = f1;
                break;
            }
            File f2 = new File("../" + path);
            if (f2.exists() && f2.isFile()) {
                foundFile = f2;
                break;
            }
            if (!path.toLowerCase().endsWith(".png")) {
                File f3 = new File(path + ".png");
                if (f3.exists() && f3.isFile()) {
                    foundFile = f3;
                    break;
                }
                File f4 = new File("../" + path + ".png");
                if (f4.exists() && f4.isFile()) {
                    foundFile = f4;
                    break;
                }
            }
        }

        if (foundFile == null) {
            File f1 = new File(name);
            if (f1.exists() && f1.isFile()) {
                foundFile = f1;
            } else {
                File f2 = new File("../" + name);
                if (f2.exists() && f2.isFile()) {
                    foundFile = f2;
                }
            }
        }

        if (foundFile != null) {
            try {
                BufferedImage img = ImageIO.read(foundFile);
                tileCache.put(name, img);
                return img;
            } catch (IOException e) {
                System.err.println("Tile yüklenirken hata (" + name + "): " + e.getMessage());
            }
        }

        // Eğer zemin varyasyonlarından biriyse ve bulunamadıysa (örneğin floor_crack), normal floor döndür.
        if (name.startsWith("floor_")) {
            System.out.println("Varyasyon bulunamadı (" + name + "), normal floor kullanılıyor.");
            return getTile("floor");
        }

        return null;
    }

    public int getTileSize() {
        return ACTUAL_SIZE;
    }
}