package view.design;
import domain.models.staticObjects.Sign;
import domain.models.staticObjects.DoubleCrate;
import domain.models.staticObjects.Crate;
import domain.models.staticObjects.SearchableObject;
import domain.models.staticObjects.Column;
import domain.models.staticObjects.Chest;

import domain.models.entity.*;
import domain.models.item.MapItem;
import domain.models.item.usables.*;
import domain.models.item.wearables.*;
import domain.models.staticObjects.Decoration;
import domain.models.item.KeyItem;
import view.TileManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaletteManager {
    private final List<PaletteItem> obstaclePalette = new ArrayList<>();
    private final List<PaletteItem> itemPalette = new ArrayList<>();
    private final List<PaletteItem> wallItemPalette = new ArrayList<>();

    private String selectedPanel = null; // "OBSTACLE", "ITEM", "WALL_ITEM"
    private int selectedPaletteIdx = -1; // -1 = none

    private final TileManager tileManager;
    private final Map<String, BufferedImage> imgCache = new HashMap<>();

    public PaletteManager(TileManager tileManager) {
        this.tileManager = tileManager;
        buildPalette();
    }

    public List<PaletteItem> getObstaclePalette() { return obstaclePalette; }
    public List<PaletteItem> getItemPalette() { return itemPalette; }
    public List<PaletteItem> getWallItemPalette() { return wallItemPalette; }

    public String getSelectedPanel() { return selectedPanel; }
    public int getSelectedPaletteIdx() { return selectedPaletteIdx; }

    public void selectItem(String panel, int idx) {
        this.selectedPanel = panel;
        this.selectedPaletteIdx = idx;
    }

    public void clearSelection() {
        this.selectedPanel = null;
        this.selectedPaletteIdx = -1;
    }

    public PaletteItem getSelectedPaletteItem() {
        if (selectedPaletteIdx < 0 || selectedPanel == null)
            return null;
        if ("OBSTACLE".equals(selectedPanel) && selectedPaletteIdx < obstaclePalette.size())
            return obstaclePalette.get(selectedPaletteIdx);
        if ("ITEM".equals(selectedPanel) && selectedPaletteIdx < itemPalette.size())
            return itemPalette.get(selectedPaletteIdx);
        if ("WALL_ITEM".equals(selectedPanel) && selectedPaletteIdx < wallItemPalette.size())
            return wallItemPalette.get(selectedPaletteIdx);
        return null;
    }

    public BufferedImage getIcon(PaletteItem item) {
        if (item.icon != null && !(item.iconPath != null && item.iconPath.startsWith("torch/"))) {
            return item.icon;
        }

        if (item.isTileIcon) {
            String tileName = item.iconPath;
            if (item.iconPath.startsWith("torch/")) {
                long now = System.currentTimeMillis();
                int[] frames = { 1, 2, 3, 4, 6, 7, 8 };
                int frame = frames[(int) ((now / 120) % frames.length)];
                tileName = "torch/torch_" + frame;
                item.icon = tileManager.getTile(tileName);
                return item.icon;
            }
            item.icon = tileManager.getTile(tileName);
            return item.icon;
        }

        String key = item.iconPath;
        if (imgCache.containsKey(key)) {
            item.icon = imgCache.get(key);
            return item.icon;
        }

        BufferedImage img = null;
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(key);
            if (is != null) {
                img = ImageIO.read(is);
                is.close();
            }
        } catch (Exception e) {
            System.err.println("Failed to load icon: " + key);
        }

        if (img != null) {
            imgCache.put(key, img);
            item.icon = img;
        }
        return img;
    }

    private void buildPalette() {
        // ── 1. OBSTACLES ───────────────────────────────────────────────────────
        obstaclePalette.add(new PaletteItem("Crate", "crate", true, (x, y) -> new Crate("Crate", x, y)));
        obstaclePalette.add(new PaletteItem("Brown Crate", "containers/crate_brown", true,
                (x, y) -> new Crate("Brown Crate", x, y)));
        obstaclePalette.add(new PaletteItem("Double Crate", "double_crate", true,
                (x, y) -> new DoubleCrate("Double Crate", x, y)));

        obstaclePalette.add(new PaletteItem("Brown Chest", "containers/chest_brown", true,
                (x, y) -> new Chest("Brown Chest", x, y, true, "containers/chest_brown")));
        obstaclePalette.add(new PaletteItem("Red Chest", "containers/chest_red", true,
                (x, y) -> new Chest("Red Chest", x, y, true, "containers/chest_red")));
        obstaclePalette.add(new PaletteItem("Blue Chest", "containers/chest_white", true,
                (x, y) -> new Chest("Blue Chest", x, y, true, "containers/chest_white")));
        obstaclePalette.add(new PaletteItem("Gold Chest", "containers/gold_chest_closed", true,
                (x, y) -> new Chest("Gold Chest", x, y, true, "containers/gold_chest_closed")));
        obstaclePalette.add(new PaletteItem("Silver Chest", "containers/silver_chest_closed", true,
                (x, y) -> new Chest("Silver Chest", x, y, true, "containers/silver_chest_closed")));
        obstaclePalette.add(new PaletteItem("Bag", "containers/bag", true,
                (x, y) -> new Chest("Bag", x, y, false, "containers/bag")));

        obstaclePalette.add(new PaletteItem("Skull", "images/containers/skull.png", false,
                (x, y) -> new Decoration("Skull", x, y, "images/containers/skull.png")));
        obstaclePalette.add(new PaletteItem("Statue", "images/containers/statue.png", false,
                (x, y) -> new Decoration("Statue", x, y, "images/containers/statue.png")));

        obstaclePalette.add(new PaletteItem("Torch", "torch/torch_1", true,
                (x, y) -> new Decoration("Torch", x, y, "torch/torch_1")));

        obstaclePalette.add(new PaletteItem("Column", "colon/gray_colon_whole", true,
                (x, y) -> new Column("Column", x, y, "colon/gray_colon_whole")));
        obstaclePalette.add(new PaletteItem("Purple Column", "colon/purple_colon_whole", true,
                (x, y) -> new Column("Purple Column", x, y, "colon/purple_colon_whole")));

        // ── 2. ITEMS ───────────────────────────────────────────────────────────
        itemPalette.add(new PaletteItem("Health Potion", "images/items/potion/red_potion.png", false,
                (x, y) -> new PotionItem(new HealthPotion("Health Potion", 5), x, y,
                        "images/items/potion/red_potion.png")));
        itemPalette.add(new PaletteItem("Mana Potion", "images/items/potion/blue_potion.png", false,
                (x, y) -> new PotionItem(new ManaPotion("Mana Potion", 20), x, y,
                        "images/items/potion/blue_potion.png")));
        itemPalette.add(new PaletteItem("Energy Potion", "images/items/potion/green_potion.png", false,
                (x, y) -> new PotionItem(new EnergyPotion("Energy Potion", 30), x, y,
                        "images/items/potion/green_potion.png")));

        itemPalette.add(new PaletteItem("Gold Key", "images/items/key/golden_key_1.png", false,
                (x, y) -> new KeyItem("Gold Key", x, y, "images/items/key/golden_key_1.png")));

        itemPalette.add(new PaletteItem("Energy Ring", "images/items/ring/green_ring.png", false,
                (x, y) -> new RingItem(new GreenRing("Energy Ring"), x, y, "images/items/ring/green_ring.png")));
        itemPalette.add(new PaletteItem("Health Ring", "images/items/ring/red_ring.png", false,
                (x, y) -> new RingItem(new RedRing("Health Ring"), x, y, "images/items/ring/red_ring.png")));
        itemPalette.add(new PaletteItem("Mana Ring", "images/items/ring/blue_ring.png", false,
                (x, y) -> new RingItem(new BlueRing("Mana Ring"), x, y, "images/items/ring/blue_ring.png")));

        itemPalette.add(new PaletteItem("Sword", "images/weapons/knight_sword.png", false, (x, y) -> new SwordItem(x, y)));
        itemPalette.add(new PaletteItem("Wooden Sword", "images/weapons/wooden_sword.png", false,
                (x, y) -> new WoodenSwordItem(x, y)));
        itemPalette.add(new PaletteItem("Katana", "images/weapons/samurai_sword.png", false,
                (x, y) -> new SamuraiSwordItem(x, y)));
        itemPalette.add(new PaletteItem("Diamond Sword", "images/weapons/diamond_sword_1.png", false,
                (x, y) -> new DiamondSwordItem(x, y)));
        itemPalette.add(new PaletteItem("Steel Sword", "images/weapons/steel_sword_1.png", false,
                (x, y) -> new SteelSwordItem(x, y)));
        itemPalette.add(new PaletteItem("Golden Sword", "images/weapons/golden_sword_1.png", false,
                (x, y) -> new GoldenSwordItem(x, y)));
        itemPalette.add(new PaletteItem("Axe", "images/weapons/axe.png", false, (x, y) -> new AxeItem(x, y)));
        itemPalette.add(new PaletteItem("Bow", "images/weapons/bow.png", false, (x, y) -> new BowItem(x, y)));
        itemPalette.add(new PaletteItem("Fire Wand", "images/weapons/fire_wand.png", false, (x, y) -> new FireWandItem(x, y)));
        itemPalette.add(new PaletteItem("Knight Hammer", "images/weapons/knight_hammer.png", false,
                (x, y) -> new KnightHammerItem(x, y)));

        itemPalette.add(new PaletteItem("Armor", "images/items/steel_armor.png", false, (x, y) -> new ArmorItem(x, y)));

        itemPalette.add(new PaletteItem("Shadow Clone", "images/items/readings/totem_3.png", false,
                (x, y) -> new PotionItem("Shadow Clone", x, y, "images/items/readings/totem_3.png")));

        // ── 3. WALL ITEM ───────────────────────────────────────────────────────
        wallItemPalette.add(new PaletteItem("Wall Torch", "images/WallDecoration/torch1.png", false,
                (x, y) -> new domain.models.staticObjects.WallObject("Wall Torch", x, y,
                        "images/WallDecoration/torch1.png")));
        wallItemPalette.add(new PaletteItem("Chain", "images/WallDecoration/chain.png", false,
                (x, y) -> new domain.models.staticObjects.WallObject("Chain", x, y,
                        "images/WallDecoration/chain.png")));
        wallItemPalette.add(new PaletteItem("Moss", "images/WallDecoration/moss.png", false,
                (x, y) -> new domain.models.staticObjects.WallObject("Moss", x, y, "images/WallDecoration/moss.png")));
        wallItemPalette.add(new PaletteItem("Crack", "images/WallDecoration/crack.png", false,
                (x, y) -> new domain.models.staticObjects.WallObject("Crack", x, y,
                        "images/WallDecoration/crack.png")));
        wallItemPalette.add(new PaletteItem("Cobweb", "images/WallDecoration/cobweb.png", false,
                (x, y) -> new domain.models.staticObjects.WallObject("Cobweb", x, y,
                        "images/WallDecoration/cobweb.png")));
        wallItemPalette.add(new PaletteItem("Red Banner", "images/WallDecoration/red_flag.png", false,
                (x, y) -> new domain.models.staticObjects.WallObject("Red Banner", x, y,
                        "images/WallDecoration/red_flag.png")));
        wallItemPalette.add(new PaletteItem("Green Banner", "images/WallDecoration/green_flag.png", false,
                (x, y) -> new domain.models.staticObjects.WallObject("Green Banner", x, y,
                        "images/WallDecoration/green_flag.png")));
        wallItemPalette.add(new PaletteItem("Blue Banner", "images/WallDecoration/blue_flag.png", false,
                (x, y) -> new domain.models.staticObjects.WallObject("Blue Banner", x, y,
                        "images/WallDecoration/blue_flag.png")));
        wallItemPalette.add(new PaletteItem("Acid Ooze", "images/WallDecoration/acid_ooze.png", false,
                (x, y) -> new domain.models.staticObjects.WallObject("Acid Ooze", x, y,
                        "images/WallDecoration/acid_ooze.png")));
        wallItemPalette.add(new PaletteItem("Blood Stain", "images/WallDecoration/blood_stain.png", false,
                (x, y) -> new domain.models.staticObjects.WallObject("Blood Stain", x, y,
                        "images/WallDecoration/blood_stain.png")));

        wallItemPalette.add(new PaletteItem("Missing Brick", "images/WallSearchable/missing_brick.png", false,
                (x, y) -> new SearchableObject("Missing Brick", x, y,
                        "images/WallSearchable/missing_brick.png", "images/WallSearchable/missing_brick.png")));
        wallItemPalette.add(new PaletteItem("Wall Grill", "images/WallSearchable/wall_grill.png", false,
                (x, y) -> new SearchableObject("Wall Grill", x, y,
                        "images/WallSearchable/wall_grill.png", "images/WallSearchable/wall_grill.png")));
        wallItemPalette.add(new PaletteItem("Pipe Hole", "images/WallSearchable/pipe_hole.png", false,
                (x, y) -> new SearchableObject("Pipe Hole", x, y,
                        "images/WallSearchable/pipe_hole.png", "images/WallSearchable/pipe_hole.png")));
        wallItemPalette.add(new PaletteItem("Gargoyle", "images/WallSearchable/gargoyle.png", false,
                (x, y) -> new SearchableObject("Gargoyle", x, y,
                        "images/WallSearchable/gargoyle.png", "images/WallSearchable/gargoyle.png")));
        wallItemPalette.add(new PaletteItem("Wall Cavity", "images/WallSearchable/wall_cavity.png", false,
                (x, y) -> new SearchableObject("Wall Cavity", x, y,
                        "images/WallSearchable/wall_cavity.png", "images/WallSearchable/wall_cavity.png")));
        wallItemPalette.add(new PaletteItem("Loose Stone", "images/WallSearchable/loose_stone.png", false,
                (x, y) -> new SearchableObject("Loose Stone", x, y,
                        "images/WallSearchable/loose_stone.png", "images/WallSearchable/loose_stone.png")));
    }
}
