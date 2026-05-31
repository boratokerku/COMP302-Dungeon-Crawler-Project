package domain.models.item;

import domain.models.entity.GameObject;
import domain.models.item.usables.PotionItem;
import domain.models.item.wearables.ArmorItem;
import domain.models.item.wearables.AxeItem;
import domain.models.item.wearables.BowItem;
import domain.models.item.wearables.DiamondSwordItem;
import domain.models.item.wearables.FireWandItem;
import domain.models.item.wearables.RingItem;
import domain.models.item.wearables.BlueRing;
import domain.models.item.wearables.RedRing;
import domain.models.item.wearables.GreenRing;
import domain.models.item.wearables.SamuraiSwordItem;
import domain.models.item.wearables.SwordItem;
import domain.models.item.wearables.WoodenSwordItem;

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
            if (!tileFile.exists()) {
                tileFile = new File("../resources/" + imagePath);
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

    // Default weapon rendering metadata for high-quality custom offsets
    public double getWeaponPivotX() {
        return 0.5;
    }

    public double getWeaponPivotY() {
        return 0.5;
    }

    public double getWeaponAngleOffset() {
        return 0.0;
    }

    public int getHandOffsetX() {
        return 0;
    }

    public int getHandOffsetY() {
        return 0;
    }

    public double getBaseRotationAngle() {
        return Math.toRadians(45.0); // Default melee swing angle (45 degrees)
    }

    // Default item stat bonuses for Armors and Rings
    public boolean isWeapon() {
        return !(this instanceof PotionItem || 
                 this instanceof ArmorItem || 
                 this instanceof RingItem || 
                 this.getClass().getSimpleName().contains("Key") || 
                 this.getClass().getSimpleName().contains("Coin"));
    }

    public int getDefBonus() {
        return 0;
    }

    public int getStrBonus() {
        return 0;
    }

    public int getHpBonus() {
        return 0;
    }

    public int getEnergyBonus() {
        return 0;
    }

    public int getManaCostReduction() {
        return 0;
    }

    // Ranged weapon metadata
    public boolean isRanged() {
        return false;
    }

    public int getManaCost() {
        return 0;
    }

    public String getProjectileType() {
        return "ARROW";
    }

    /**
     * @requires No specific preconditions.
     * @modifies This method does not modify any existing objects (it is a creator).
     * @effects Returns a new, non-null Weapon object with randomized attributes
     *          (e.g., damage, weight).
     *
     *          Weighted random weapon drop based on rarity tiers:
     *          - Wooden Sword (Common, 40%)
     *          - Knight Sword (Common, 30%)
     *          - Battle Axe (Rare, 15%)
     *          - Hunting Bow (Rare, 10%)
     *          - Fire Wand (Epic, 3%)
     *          - Samurai Katana (Epic, 1.5%)
     *          - Diamond Sword (Legendary, 0.5%)
     */
    public static MapItem createRandomWeapon(int x, int y) {
        double roll = Math.random() * 100.0;
        if (roll < 20.0) {
            return new WoodenSwordItem(x, y);
        } else if (roll < 40.0) {
            return new SwordItem(x, y);
        } else if (roll < 55.0) {
            return new AxeItem(x, y);
        } else if (roll < 65.0) {
            return new BowItem(x, y);
        } else if (roll < 72.0) {
            return new FireWandItem(x, y);
        } else if (roll < 77.0) {
            return new SamuraiSwordItem(x, y);
        } else if (roll < 80.0) {
            return new DiamondSwordItem(x, y);
        } else if (roll < 87.0) {
            return new domain.models.item.wearables.SteelSwordItem(x, y);
        } else if (roll < 94.0) {
            return new domain.models.item.wearables.GoldenSwordItem(x, y);
        } else {
            return new domain.models.item.wearables.KnightHammerItem(x, y);
        }
    }

    public static MapItem createRandomItem(int x, int y) {
        double roll = Math.random() * 100.0;
        if (roll < 80.0) {
            return createRandomWeapon(x, y); // 80% chance for a weapon
        } else if (roll < 90.0) {
            return new ArmorItem(x, y); // 10% chance for Steel Armor
        } else {
            double ringRoll = Math.random();
            if (ringRoll < 0.33) {
                return new RingItem(new BlueRing("Mana Ring"), x, y, "images/items/ring/blue_ring.png");
            } else if (ringRoll < 0.66) {
                return new RingItem(new RedRing("Health Ring"), x, y, "images/items/ring/red_ring.png");
            } else {
                return new RingItem(new GreenRing("Energy Ring"), x, y, "images/items/ring/green_ring.png");
            }
        }
    }
}
