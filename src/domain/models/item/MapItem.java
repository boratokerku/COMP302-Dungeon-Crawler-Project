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
    public double getBaseRotationAngle() {
        return Math.toRadians(45.0); // Default melee swing angle (45 degrees)
    }

    // Default item stat bonuses for Armors and Rings
    public int getDefBonus() { return 0; }
    public int getStrBonus() { return 0; }
    
    // Ranged weapon metadata
    public boolean isRanged() { return false; }
    public int getManaCost() { return 0; }
    public String getProjectileType() { return "ARROW"; }

    /**
     * Weighted random weapon drop based on rarity tiers:
     * - Wooden Sword (Common, 40%)
     * - Knight Sword (Common, 30%)
     * - Battle Axe (Rare, 15%)
     * - Hunting Bow (Rare, 10%)
     * - Fire Wand (Epic, 3%)
     * - Samurai Katana (Epic, 1.5%)
     * - Diamond Sword (Legendary, 0.5%)
     */
    public static MapItem createRandomWeapon(int x, int y) {
        double roll = Math.random() * 100.0;
        if (roll < 40.0) {
            return new WoodenSwordItem(x, y);
        } else if (roll < 70.0) {
            return new SwordItem(x, y);
        } else if (roll < 85.0) {
            return new AxeItem(x, y);
        } else if (roll < 95.0) {
            return new BowItem(x, y);
        } else if (roll < 98.0) {
            return new FireWandItem(x, y);
        } else if (roll < 99.5) {
            return new SamuraiSwordItem(x, y);
        } else {
            return new DiamondSwordItem(x, y);
        }
    }

    public static MapItem createRandomItem(int x, int y) {
        double roll = Math.random() * 100.0;
        if (roll < 80.0) {
            return createRandomWeapon(x, y); // 80% chance for a weapon
        } else if (roll < 90.0) {
            return new ArmorItem(x, y);      // 10% chance for Steel Armor
        } else {
            return new RingItem(x, y);       // 10% chance for Ring of Might
        }
    }
}
