import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ExplodeTiles {
    public static void main(String[] args) throws Exception {
        File spriteSheetFile = new File("resources/images/walls and statics x2.png");
        if (!spriteSheetFile.exists()) {
            System.err.println("Spritesheet not found!");
            return;
        }
        BufferedImage sheet = ImageIO.read(spriteSheetFile);
        File tilesDir = new File("resources/images/tiles");
        tilesDir.mkdirs();

        // Wall Face (Based on previous coordinates: 320, 64)
        BufferedImage wall = sheet.getSubimage(320, 64, 64, 64);
        ImageIO.write(wall, "png", new File(tilesDir, "wall.png"));

        // Floor (Based on previous coordinates: 192, 128)
        BufferedImage floor = sheet.getSubimage(192, 128, 64, 64);
        ImageIO.write(floor, "png", new File(tilesDir, "floor.png"));

        System.out.println("Tiles exploded successfully into resources/images/tiles/");
    }
}
