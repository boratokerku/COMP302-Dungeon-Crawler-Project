package domain.logic;

import domain.models.GameState;
import domain.models.GameObject;
import domain.models.item.*;
import domain.models.item.usables.*;
import domain.models.item.wearables.*;

/**
 * Factory for creating GameObjects from serialized records (save files / load game).
 *
 * <p>GRASP Creator: this class knows all item types and is the appropriate creator
 * for reconstructing game objects from string-based records.</p>
 *
 * <p>Pure Fabrication: this class has no direct domain counterpart — it exists
 * purely to centralize object creation logic that was previously scattered across
 * DemoRunner (violating High Cohesion and Single Responsibility).</p>
 */
public final class GameObjectFactory {

    // Utility class — not instantiatable
    private GameObjectFactory() {}

    /**
     * Create a GameObject from a saved ItemRecord.
     *
     * @param rec  The item record loaded from a save file.
     * @return     A fully initialized GameObject, or null if type is unknown.
     */
    public static GameObject createFromRecord(GameState.ItemRecord rec) {
        if (rec == null || rec.type == null) return null;
        return create(rec.type, rec.name, rec.x, rec.y, rec.isLocked, rec.imageName);
    }

    /**
     * Create a GameObject by type name and position.
     * Convenience overload — no lock state or image override.
     */
    public static GameObject create(String type, String name, int x, int y) {
        return create(type, name, x, y, false, null);
    }

    /**
     * Create a GameObject by type name, position, lock state, and optional image override.
     */
    public static GameObject create(String type, String name, int x, int y,
                                    boolean isLocked, String imgName) {
        if (type == null) return null;

        String displayName = (name != null && !name.isEmpty()) ? name : type;

        switch (type) {
            // ── Potions ────────────────────────────────────────────────────────
            case "PotionItem":
            case "HealthPotionItem":
                return new PotionItem(
                        new HealthPotion("Health Potion", 5), x, y,
                        resolveImage(imgName, "images/items/potion/red_potion.png"));
            case "ManaPotionItem":
                return new PotionItem(
                        new ManaPotion("Mana Potion", 20), x, y,
                        resolveImage(imgName, "images/items/potion/blue_potion.png"));
            case "EnergyPotionItem":
                return new PotionItem(
                        new EnergyPotion("Energy Potion", 30), x, y,
                        resolveImage(imgName, "images/items/potion/green_potion.png"));

            // ── Weapons ────────────────────────────────────────────────────────
            case "SwordItem":          return new SwordItem(x, y);
            case "SteelSwordItem":     return new SteelSwordItem(x, y);
            case "GoldenSwordItem":    return new GoldenSwordItem(x, y);
            case "KnightHammerItem":   return new KnightHammerItem(x, y);
            case "AxeItem":            return new AxeItem(x, y);
            case "WoodenSwordItem":    return new WoodenSwordItem(x, y);
            case "SamuraiSwordItem":   return new SamuraiSwordItem(x, y);
            case "DiamondSwordItem":   return new DiamondSwordItem(x, y);
            case "BowItem":            return new BowItem(x, y);
            case "FireWandItem":       return new FireWandItem(x, y);

            // ── Wearables ──────────────────────────────────────────────────────
            case "ArmorItem":          return new ArmorItem(x, y);
            case "RingItem":           return createRing(displayName, imgName, x, y);

            // ── Keys ───────────────────────────────────────────────────────────
            case "KeyItem":
                return new domain.models.item.KeyItem(displayName, x, y,
                        resolveImage(imgName, "images/items/key/golden_key_1.png"));
            case "LevelKey":
                return new domain.models.item.LevelKey(displayName, x, y,
                        resolveImage(imgName, "images/items/key/skull_key.png"));

            // ── Containers ─────────────────────────────────────────────────────
            case "Column":
                return new domain.models.staticObjects.Column(displayName, x, y, imgName);
            case "Sign":
                return new domain.models.staticObjects.Sign(displayName, x, y,
                        imgName != null ? imgName : "sign/sign_brown");
            case "DoubleCrate":
                return new domain.models.staticObjects.DoubleCrate(displayName, x, y);
            case "Crate":
                return new domain.models.staticObjects.Crate(displayName, x, y);
            case "Chest":
                return (imgName != null && !imgName.isEmpty())
                        ? new domain.models.staticObjects.Chest(displayName, x, y, isLocked, imgName)
                        : new domain.models.staticObjects.Chest(displayName, x, y, isLocked);

            // ── Static / Decoration ────────────────────────────────────────────
            case "SearchableObject":
                return new domain.models.staticObjects.SearchableObject(displayName, x, y, imgName);
            case "Decoration":
                return new domain.models.staticObjects.Decoration(displayName, x, y, imgName);
            case "WallObject":
                return new domain.models.staticObjects.WallObject(displayName, x, y, imgName);
            case "Door": {
                domain.models.staticObjects.Door door =
                        new domain.models.staticObjects.Door(displayName, x, y, isLocked);
                if (!isLocked) door.open();
                return door;
            }
            case "LevelDoor":
                return new domain.models.staticObjects.LevelDoor(displayName, x, y);

            // ── Special ────────────────────────────────────────────────────────
            case "VictoryCoin":
                return new VictoryCoin(x, y);

            default:
                System.err.println("[GameObjectFactory] Unknown item type: " + type);
                return null;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String resolveImage(String provided, String fallback) {
        return (provided != null && !provided.isEmpty()) ? provided : fallback;
    }

    private static GameObject createRing(String displayName, String imgName, int x, int y) {
        if (imgName != null && !imgName.isEmpty()) {
            if (imgName.contains("blue") || imgName.contains("mana")) {
                return new RingItem(new BlueRing("Mana Ring"), x, y, imgName);
            } else if (imgName.contains("red") || imgName.contains("health")) {
                return new RingItem(new RedRing("Health Ring"), x, y, imgName);
            } else {
                return new RingItem(new GreenRing("Energy Ring"), x, y, imgName);
            }
        }
        if (displayName != null) {
            String lower = displayName.toLowerCase();
            if (lower.contains("blue") || lower.contains("mana")) {
                return new RingItem(new BlueRing("Mana Ring"), x, y, "images/items/ring/blue_ring.png");
            } else if (lower.contains("red") || lower.contains("health")) {
                return new RingItem(new RedRing("Health Ring"), x, y, "images/items/ring/red_ring.png");
            }
        }
        return new RingItem(new GreenRing("Energy Ring"), x, y, "images/items/ring/green_ring.png");
    }
}
