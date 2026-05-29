package ui;

import domain.models.entity.Chest;
import domain.models.entity.Column;
import domain.models.entity.Crate;
import domain.models.entity.DoubleCrate;
import domain.models.entity.GameObject;
import domain.models.entity.SearchableObject;
import domain.models.entity.Sign;
import domain.models.item.*;
import domain.models.item.usables.EnergyPotion;
import domain.models.item.usables.HealthPotion;
import domain.models.item.usables.ManaPotion;
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
import domain.models.map.GameMap;
import domain.models.staticObjects.*;
import domain.models.tile.FloorTile;
import domain.models.tile.WallTile;
import view.TileManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Design Mode — Harita editörü.
 * Üstte toolbar (item palette), altta harita, altında aksiyon butonları.
 * Tıkla-seç → Tile'a tıkla/sürükle yerleştir. Sağ tık siler.
 */
public class DesignModeView extends JPanel {
    private static final int MAX_OBSTACLES = 40;
    private static final int MAX_ITEMS = 30;
    private static final int MAX_SEARCHABLE_PER_MAP = 6;
    private static final int MAX_DECORATIVE_PER_MAP = 12;

    // ── Callbacks ────────────────────────────────────────────────────────────
    private final Runnable onBackToMenu;
    private final java.util.function.Consumer<GameMap> onPlayMap;
    private final java.util.function.Consumer<GameMap> onPlayTeamMatchMap;

    // ── Core ─────────────────────────────────────────────────────────────────
    private final GameMap map;
    private final TileManager tileManager;

    // ── Layout ───────────────────────────────────────────────────────────────
    // Layout and sizing constants for panels
    private static final int ICON_SIZE = 48; // px — item ikonu
    private static final int ICON_GAP = 3; // px — ikonlar arası
    private static final int BOTTOM_BTN_H = 48; // px — alt buton şeridi

    private int tileSize = 32;
    private int mapOffsetX = 0;
    private int mapOffsetY = 0;

    // Hover'daki item için tooltip
    private String hoveredPaletteLabel = null;

    private int leftScrollY = 0;

    // ── Palet ────────────────────────────────────────────────────────────────
    // ── Palet ────────────────────────────────────────────────────────────────
    private final List<PaletteItem> obstaclePalette = new ArrayList<>();
    private final List<PaletteItem> itemPalette = new ArrayList<>();
    private final List<PaletteItem> wallItemPalette = new ArrayList<>();
    private String selectedPanel = null; // "OBSTACLE", "ITEM", "WALL_ITEM"
    private int selectedPaletteIdx = -1; // seçili item (-1 = hiçbiri)

    private static final int LEFT_PANEL_W = 340;

    private int getCategoryStartY(String category) {
        int y = 80 - leftScrollY; // Title panel height is ~50, margin ~30
        if (category.equals("OBSTACLE"))
            return y;

        int rowsObs = (int) Math.ceil(obstaclePalette.size() / 4.0);
        y += 46 + rowsObs * 58 + 10; // 46 for subtitle_panel, 58 per row, 10 gap

        if (category.equals("ITEM"))
            return y;

        int rowsItem = (int) Math.ceil(itemPalette.size() / 4.0);
        y += 46 + rowsItem * 58 + 10;

        return y;
    }

    private int getLeftPanelContentHeight() {
        int rowsObs = (int) Math.ceil(obstaclePalette.size() / 4.0);
        int rowsItem = (int) Math.ceil(itemPalette.size() / 4.0);
        int rowsWall = (int) Math.ceil(wallItemPalette.size() / 4.0);

        int y = 80;
        y += 46 + rowsObs * 58 + 10;
        y += 46 + rowsItem * 58 + 10;
        y += 46 + rowsWall * 58 + 30; // 30 is extra bottom margin
        return y;
    }

    private Rectangle getPaletteItemBounds(String category, int idx) {
        int startY = getCategoryStartY(category);
        int cols = 4;
        int row = idx / cols;
        int col = idx % cols;

        int iconSize = 48;
        // The left panel is ~340 wide. Centered items:
        int totalW = cols * iconSize + (cols - 1) * 8;
        int startX = (LEFT_PANEL_W - totalW) / 2;

        int x = startX + col * (iconSize + 8);
        int y = startY + 56 + row * (iconSize + 8); // 56 margin from subtitle panel
        return new Rectangle(x, y, iconSize, iconSize);
    }

    private PaletteItem getSelectedPaletteItem() {
        if (selectedPanel == null || selectedPaletteIdx < 0)
            return null;
        if ("OBSTACLE".equals(selectedPanel))
            return obstaclePalette.get(selectedPaletteIdx);
        if ("ITEM".equals(selectedPanel))
            return itemPalette.get(selectedPaletteIdx);
        if ("WALL_ITEM".equals(selectedPanel))
            return wallItemPalette.get(selectedPaletteIdx);
        return null;
    }

    // ── Hover ────────────────────────────────────────────────────────────────
    private int hoverTileX = -1;
    private int hoverTileY = -1;

    // ── Drag ─────────────────────────────────────────────────────────────────
    private boolean isDragging = false;

    // ── Butonlar (alt şerit) ─────────────────────────────────────────────────
    private final List<ActionBtn> actionBtns = new ArrayList<>();

    // ── Image cache ──────────────────────────────────────────────────────────
    private static final java.util.Map<String, BufferedImage> imgCache = new java.util.HashMap<>();

    private BufferedImage mainFrameImg;
    private BufferedImage titlePanelImg;
    private BufferedImage subtitlePanelImg;
    private BufferedImage buildModeBoxImg;
    private java.awt.Font vt323Font;

    // ─────────────────────────────────────────────────────────────────────────
    // CONSTRUCTOR
    // ─────────────────────────────────────────────────────────────────────────

    public DesignModeView(GameMap map, TileManager tileManager,
            Runnable onBackToMenu,
            java.util.function.Consumer<GameMap> onPlayMap,
            java.util.function.Consumer<GameMap> onPlayTeamMatchMap) {
        this.map = map;
        this.tileManager = tileManager;
        this.onBackToMenu = onBackToMenu;
        this.onPlayMap = onPlayMap;
        this.onPlayTeamMatchMap = onPlayTeamMatchMap;

        setBackground(new Color(42, 22, 38));
        setLayout(null);
        setDoubleBuffered(true);

        try {
            mainFrameImg = ImageIO.read(new File("resources/images/BuildMode/mainframe.png"));
            titlePanelImg = ImageIO.read(new File("resources/images/BuildMode/title_panel.png"));
            subtitlePanelImg = ImageIO.read(new File("resources/images/BuildMode/subtitle_panel.png"));
            buildModeBoxImg = ImageIO.read(new File("resources/images/BuildMode/buildmodebox.png"));
        } catch (Exception e) {
            System.err.println("Failed to load BuildMode images: " + e.getMessage());
        }

        try {
            vt323Font = java.awt.Font.createFont(
                    java.awt.Font.TRUETYPE_FONT,
                    new java.io.File("resources/fonts/VT323-Regular.ttf"));
            java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(vt323Font);
        } catch (Exception e) {
            System.err.println("Failed to load VT323-Regular font in BuildMode: " + e.getMessage());
            vt323Font = new java.awt.Font("Monospaced", java.awt.Font.BOLD, 18);
        }

        buildPalette();
        buildActionButtons();
        setupMouseListeners();

        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int mx = e.getX();
                int my = e.getY();
                int bottomStart = getHeight() - BOTTOM_BTN_H;
                if (mx < LEFT_PANEL_W && my < bottomStart) {
                    leftScrollY += e.getWheelRotation() * 25;
                    int viewH = bottomStart;
                    int contentH = getLeftPanelContentHeight();
                    int maxScrollY = Math.max(0, contentH - viewH);
                    if (leftScrollY < 0)
                        leftScrollY = 0;
                    if (leftScrollY > maxScrollY)
                        leftScrollY = maxScrollY;
                    repaint();
                } else {
                    // Hovered grid viewport area!
                    int tx = (mx - mapOffsetX) / tileSize;
                    int ty = (my - mapOffsetY) / tileSize;
                    if (map.isValidPosition(tx, ty)) {
                        GameObject obj = map.getObjectAt(tx, ty);
                        if (obj != null) {
                            GameObject target = obj;
                            if (obj instanceof WallTile) {
                                WallTile wall = (WallTile) obj;
                                if (wall.getDecoration() != null) {
                                    target = wall.getDecoration();
                                }
                            }
                            if (!(target instanceof WallTile) && !(target instanceof FloorTile)) {
                                double currentScale = target.getCustomScale();
                                if (e.getWheelRotation() < 0) {
                                    currentScale += 0.05; // scale up by 5%
                                } else {
                                    currentScale -= 0.05; // scale down by 5%
                                }
                                // Clamp between 0.15 and 3.0
                                currentScale = Math.max(0.15, Math.min(2.0, currentScale));
                                target.setCustomScale(currentScale);
                                repaint();
                            }
                        }
                    }
                }
            }
        });

        // Animasyonlar için (örn. Torch) periyodik repaint tetikleyici
        javax.swing.Timer animTimer = new javax.swing.Timer(120, e -> {
            if (isShowing()) {
                repaint();
            }
        });
        animTimer.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PALETTE SETUP
    // ─────────────────────────────────────────────────────────────────────────

    /** Her yerleştirilebilir nesne türü için palette kaydı */
    private static class PaletteItem {
        final String label;
        final String iconPath; // resources/... altındaki göreli yol (tile adı ya da images/... yolu)
        final boolean isTileIcon; // true → TileManager'dan yükle; false → doğrudan dosya yolu
        final java.util.function.BiFunction<Integer, Integer, GameObject> factory;
        BufferedImage icon;

        PaletteItem(String label, String iconPath, boolean isTileIcon,
                java.util.function.BiFunction<Integer, Integer, GameObject> factory) {
            this.label = label;
            this.iconPath = iconPath;
            this.isTileIcon = isTileIcon;
            this.factory = factory;
        }
    }

    private void buildPalette() {
        // ── 1. OBSTACLES ───────────────────────────────────────────────────────
        // Crates
        obstaclePalette.add(new PaletteItem("Crate", "crate", true, (x, y) -> new Crate("Crate", x, y)));
        obstaclePalette.add(new PaletteItem("Crate Brown", "containers/crate_brown", true,
                (x, y) -> new Crate("Crate Brown", x, y)));
        obstaclePalette
                .add(new PaletteItem("DbCrate", "double_crate", true, (x, y) -> new DoubleCrate("DoubleCrate", x, y)));

        // Chests & Bags (Floor interactive Chest obstacles)
        obstaclePalette.add(new PaletteItem("BrChest", "containers/chest_brown", true,
                (x, y) -> new domain.models.entity.Chest("Brown Chest", x, y, true, "containers/chest_brown")));
        obstaclePalette.add(new PaletteItem("RedChest", "containers/chest_red", true,
                (x, y) -> new domain.models.entity.Chest("Red Chest", x, y, true, "containers/chest_red")));
        obstaclePalette.add(new PaletteItem("WhChest", "containers/chest_white", true,
                (x, y) -> new domain.models.entity.Chest("White Chest", x, y, true, "containers/chest_white")));
        obstaclePalette.add(new PaletteItem("GoldChest", "containers/gold_chest_closed", true,
                (x, y) -> new domain.models.entity.Chest("Gold Chest", x, y, true, "containers/gold_chest_closed")));
        obstaclePalette.add(new PaletteItem("SilvChest", "containers/silver_chest_closed", true,
                (x, y) -> new domain.models.entity.Chest("Silver Chest", x, y, true,
                        "containers/silver_chest_closed")));
        obstaclePalette.add(new PaletteItem("Bag", "containers/bag", true,
                (x, y) -> new domain.models.entity.Chest("Bag", x, y, false, "containers/bag")));
        obstaclePalette.add(new PaletteItem("MagBag", "containers/magical_bag", true,
                (x, y) -> new domain.models.entity.Chest("Magical Bag", x, y, false, "containers/magical_bag")));

        // Skull & Statue (Obstacles)
        obstaclePalette.add(new PaletteItem("Skull", "images/containers/skull.png", false,
                (x, y) -> new domain.models.staticObjects.Decoration("Skull", x, y, "images/containers/skull.png")));
        obstaclePalette.add(new PaletteItem("Statue", "images/containers/statue.png", false,
                (x, y) -> new domain.models.staticObjects.Decoration("Statue", x, y, "images/containers/statue.png")));

        // Torch obstacle (resources/images/tiles/torch)
        obstaclePalette.add(new PaletteItem("Torch", "torch/torch_1", true,
                (x, y) -> new Decoration("Torch", x, y, "torch/torch_1")));

        // Optional original obstacles to maintain full functionality:
        obstaclePalette.add(new PaletteItem("Column", "colon/gray_colon_whole", true,
                (x, y) -> new Column("Column", x, y, "colon/gray_colon_whole")));
        obstaclePalette.add(new PaletteItem("PurpleCol", "colon/purple_colon_whole", true,
                (x, y) -> new Column("Column", x, y, "colon/purple_colon_whole")));
        obstaclePalette.add(
                new PaletteItem("Sign", "sign/sign_brown", true, (x, y) -> new Sign("Sign", x, y, "sign/sign_brown")));
        obstaclePalette.add(new PaletteItem("SignOrg", "sign/sign_orange", true,
                (x, y) -> new Sign("Sign", x, y, "sign/sign_orange")));

        // ── 2. ITEMS ───────────────────────────────────────────────────────────
        // Potions
        itemPalette.add(new PaletteItem("RedPotion", "images/items/potion/red_potion.png", false,
                (x, y) -> new PotionItem(new HealthPotion("Red Potion", 5), x, y,
                        "images/items/potion/red_potion.png")));
        itemPalette.add(new PaletteItem("BluePotion", "images/items/potion/blue_potion.png", false,
                (x, y) -> new PotionItem(new ManaPotion("Blue Potion", 20), x, y,
                        "images/items/potion/blue_potion.png")));
        itemPalette.add(new PaletteItem("GreenPotion", "images/items/potion/green_potion.png", false,
                (x, y) -> new PotionItem(new EnergyPotion("Green Potion", 30), x, y,
                        "images/items/potion/green_potion.png")));

        // Keys
        itemPalette.add(new PaletteItem("GoldKey1", "images/items/key/golden_key_1.png", false,
                (x, y) -> new KeyItem("Golden Key 1", x, y, "images/items/key/golden_key_1.png")));
        itemPalette.add(new PaletteItem("GoldKey2", "images/items/key/golden_key_2.png", false,
                (x, y) -> new KeyItem("Golden Key 2", x, y, "images/items/key/golden_key_2.png")));
        itemPalette.add(new PaletteItem("SilverKey", "images/items/key/silver_key.png", false,
                (x, y) -> new KeyItem("Silver Key", x, y, "images/items/key/silver_key.png")));

        // Rings
        itemPalette.add(
                new PaletteItem("GreenRing", "images/items/ring/green_ring.png", false, (x, y) -> new RingItem(x, y)));
        itemPalette.add(new PaletteItem("RedRing", "images/items/ring/red_ring.png", false,
                (x, y) -> new RingItem("Red Ring", x, y, "images/items/ring/red_ring.png")));
        itemPalette.add(new PaletteItem("BlueRing", "images/items/ring/blue_ring.png", false,
                (x, y) -> new RingItem("Blue Ring", x, y, "images/items/ring/blue_ring.png")));

        // Weapons
        itemPalette
                .add(new PaletteItem("Sword", "images/weapons/knight_sword.png", false, (x, y) -> new SwordItem(x, y)));
        itemPalette.add(new PaletteItem("WdSword", "images/weapons/wooden_sword.png", false,
                (x, y) -> new WoodenSwordItem(x, y)));
        itemPalette.add(new PaletteItem("Katana", "images/weapons/samurai_sword.png", false,
                (x, y) -> new SamuraiSwordItem(x, y)));
        itemPalette.add(new PaletteItem("SteelSword1", "images/weapons/steel_sword_1.png", false,
                (x, y) -> new PotionItem("Steel Sword 1", x, y, "images/weapons/steel_sword_1.png")));
        itemPalette.add(new PaletteItem("GoldSword1", "images/weapons/golden_sword_1.png", false,
                (x, y) -> new PotionItem("Golden Sword 1", x, y, "images/weapons/golden_sword_1.png")));
        itemPalette.add(new PaletteItem("IronSword1", "images/weapons/iron_sword_1.png", false,
                (x, y) -> new PotionItem("Iron Sword 1", x, y, "images/weapons/iron_sword_1.png")));
        itemPalette.add(new PaletteItem("Axe", "images/weapons/axe.png", false, (x, y) -> new AxeItem(x, y)));
        itemPalette.add(new PaletteItem("Bow", "images/weapons/bow.png", false, (x, y) -> new BowItem(x, y)));
        itemPalette.add(
                new PaletteItem("FireWand", "images/weapons/fire_wand.png", false, (x, y) -> new FireWandItem(x, y)));
        itemPalette.add(new PaletteItem("Wand", "images/weapons/wand.png", false,
                (x, y) -> new PotionItem("Wand", x, y, "images/weapons/wand.png")));
        itemPalette.add(new PaletteItem("SmallKnife", "images/weapons/small_knife.png", false,
                (x, y) -> new PotionItem("Small Knife", x, y, "images/weapons/small_knife.png")));
        itemPalette.add(new PaletteItem("KnightHammer", "images/weapons/knight_hammer.png", false,
                (x, y) -> new PotionItem("Knight Hammer", x, y, "images/weapons/knight_hammer.png")));
        itemPalette.add(new PaletteItem("Mace1", "images/weapons/mace_1.png", false,
                (x, y) -> new PotionItem("Mace 1", x, y, "images/weapons/mace_1.png")));

        // Armor
        itemPalette.add(new PaletteItem("Armor", "images/items/steel_armor.png", false, (x, y) -> new ArmorItem(x, y)));

        // Readings
        itemPalette.add(new PaletteItem("Book", "images/items/readings/book.png", false,
                (x, y) -> new PotionItem("Book", x, y, "images/items/readings/book.png")));
        itemPalette.add(new PaletteItem("Totem1", "images/items/readings/totem_1.png", false,
                (x, y) -> new PotionItem("Totem 1", x, y, "images/items/readings/totem_1.png")));

        // Coins
        itemPalette.add(new PaletteItem("Coins", "images/items/coin/coins.png", false,
                (x, y) -> new PotionItem("Coins", x, y, "images/items/coin/coins.png")));

        // ── 3. WALL ITEM ───────────────────────────────────────────────────────
        // Decorative (WallDecoration)
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
        wallItemPalette.add(new PaletteItem("RedFlag", "images/WallDecoration/red_flag.png", false,
                (x, y) -> new domain.models.staticObjects.WallObject("Red Flag", x, y,
                        "images/WallDecoration/red_flag.png")));
        wallItemPalette.add(new PaletteItem("GreenFlag", "images/WallDecoration/green_flag.png", false,
                (x, y) -> new domain.models.staticObjects.WallObject("Green Flag", x, y,
                        "images/WallDecoration/green_flag.png")));
        wallItemPalette.add(new PaletteItem("BlueFlag", "images/WallDecoration/blue_flag.png", false,
                (x, y) -> new domain.models.staticObjects.WallObject("Blue Flag", x, y,
                        "images/WallDecoration/blue_flag.png")));
        wallItemPalette.add(new PaletteItem("AcidOoze", "images/WallDecoration/acid_ooze.png", false,
                (x, y) -> new domain.models.staticObjects.WallObject("Acid Ooze", x, y,
                        "images/WallDecoration/acid_ooze.png")));
        wallItemPalette.add(new PaletteItem("BloodStain", "images/WallDecoration/blood_stain.png", false,
                (x, y) -> new domain.models.staticObjects.WallObject("Blood Stain", x, y,
                        "images/WallDecoration/blood_stain.png")));

        // Searchable (WallSearchable)
        wallItemPalette.add(new PaletteItem("MissingBrick", "images/WallSearchable/missing_brick.png", false,
                (x, y) -> new domain.models.entity.SearchableObject("Missing Brick", x, y,
                        "images/WallSearchable/missing_brick.png", "images/WallSearchable/missing_brick.png")));
        wallItemPalette.add(new PaletteItem("WallGrill", "images/WallSearchable/wall_grill.png", false,
                (x, y) -> new domain.models.entity.SearchableObject("Wall Grill", x, y,
                        "images/WallSearchable/wall_grill.png", "images/WallSearchable/wall_grill.png")));
        wallItemPalette.add(new PaletteItem("PipeHole", "images/WallSearchable/pipe_hole.png", false,
                (x, y) -> new domain.models.entity.SearchableObject("Pipe Hole", x, y,
                        "images/WallSearchable/pipe_hole.png", "images/WallSearchable/pipe_hole.png")));
        wallItemPalette.add(new PaletteItem("Gargoyle", "images/WallSearchable/gargoyle.png", false,
                (x, y) -> new domain.models.entity.SearchableObject("Gargoyle", x, y,
                        "images/WallSearchable/gargoyle.png", "images/WallSearchable/gargoyle.png")));
        wallItemPalette.add(new PaletteItem("WallCavity", "images/WallSearchable/wall_cavity.png", false,
                (x, y) -> new domain.models.entity.SearchableObject("Wall Cavity", x, y,
                        "images/WallSearchable/wall_cavity.png", "images/WallSearchable/wall_cavity.png")));
        wallItemPalette.add(new PaletteItem("LooseStone", "images/WallSearchable/loose_stone.png", false,
                (x, y) -> new domain.models.entity.SearchableObject("Loose Stone", x, y,
                        "images/WallSearchable/loose_stone.png", "images/WallSearchable/loose_stone.png")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTION BUTTONS (alt şerit)
    // ─────────────────────────────────────────────────────────────────────────

    private static class ActionBtn {
        final String label;
        final Color bgColor;
        final Runnable action;
        final String spritePath;
        BufferedImage sprite;
        Rectangle bounds = new Rectangle();

        ActionBtn(String label, Color bgColor, String spritePath, Runnable action) {
            this.label = label;
            this.bgColor = bgColor;
            this.spritePath = spritePath;
            this.action = action;
        }
    }

    private void buildActionButtons() {
        actionBtns.add(new ActionBtn("▶  Play", new Color(60, 140, 60),
                "images/DesignModeImages/DesignModeButtons/PlayButton.png", this::doPlay));
        actionBtns.add(new ActionBtn("▶  Team Match", new Color(60, 100, 160),
                "images/DesignModeImages/DesignModeButtons/PlayTeamMatchButton.png", this::doPlayTeamMatch));
        actionBtns.add(new ActionBtn("⚄  +5 Random", new Color(80, 60, 130),
                "images/DesignModeImages/DesignModeButtons/PlusFiveRandomButton.png", this::doAddRandom));
        actionBtns.add(new ActionBtn("🎲  Gen Map", new Color(110, 50, 130),
                "images/DesignModeImages/DesignModeButtons/GenerateRandomMapButton.png", this::doGenerateRandomMap));
        actionBtns.add(new ActionBtn("💾  Save Map", new Color(50, 90, 150),
                "images/DesignModeImages/DesignModeButtons/SaveMapButton.png", this::doSave));
        actionBtns.add(new ActionBtn("📂  Load Map", new Color(100, 80, 30),
                "images/DesignModeImages/DesignModeButtons/LoadMapButton.png", this::doLoad));
        actionBtns.add(new ActionBtn("🗑  Clear Map", new Color(140, 60, 40),
                "images/DesignModeImages/DesignModeButtons/ClearMapButton.png", this::doClear));
        actionBtns.add(new ActionBtn("✖  Exit Menu", new Color(80, 30, 50),
                "images/DesignModeImages/DesignModeButtons/ExitToMainMenuButton.png", onBackToMenu::run));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MOUSE LISTENERS
    // ─────────────────────────────────────────────────────────────────────────

    private void setupMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handlePress(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging = false;
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMove(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                handleDrag(e);
            }
        });
    }

    private void handlePress(MouseEvent e) {
        isDragging = true;
        int mx = e.getX();
        int my = e.getY();
        int bottomStart = getHeight() - BOTTOM_BTN_H;

        boolean inLeftPanel = (mx < LEFT_PANEL_W && my < bottomStart);

        if (inLeftPanel) {
            selectPaletteAt(mx, my);
            repaint();
            return;
        }

        if (my >= bottomStart) {
            fireActionBtn(mx, my);
            return;
        }

        // Harita alanı
        handleMapClick(e);
    }

    private void handleDrag(MouseEvent e) {
        if (!isDragging)
            return;
        int mx = e.getX();
        int my = e.getY();
        int bottomStart = getHeight() - BOTTOM_BTN_H;

        boolean inViewport = (mx >= LEFT_PANEL_W && my < bottomStart);

        if (inViewport) {
            updateHover(mx, my);
            if (selectedPaletteIdx >= 0 && selectedPanel != null) {
                placeOrErase(e);
            }
            repaint();
        }
    }

    private void handleMove(MouseEvent e) {
        int mx = e.getX();
        int my = e.getY();
        updateHover(mx, my);

        hoveredPaletteLabel = null;
        PaletteItem hovered = getPaletteItemAt(mx, my);
        if (hovered != null) {
            hoveredPaletteLabel = hovered.label;
        }
        repaint();
    }

    private void handleMapClick(MouseEvent e) {
        updateHover(e.getX(), e.getY());
        if (selectedPaletteIdx >= 0 && selectedPanel != null) {
            placeOrErase(e);
        }
        repaint();
    }

    private void updateHover(int mx, int my) {
        if (tileSize <= 0) {
            hoverTileX = hoverTileY = -1;
            return;
        }
        int bottomStart = getHeight() - BOTTOM_BTN_H;
        boolean inViewport = (mx >= LEFT_PANEL_W && my < bottomStart);

        if (!inViewport) {
            hoverTileX = hoverTileY = -1;
            return;
        }

        int tx = (mx - mapOffsetX) / tileSize;
        int ty = (my - mapOffsetY) / tileSize;
        if (map.isValidPosition(tx, ty)) {
            hoverTileX = tx;
            hoverTileY = ty;
        } else {
            hoverTileX = hoverTileY = -1;
        }
    }

    private void placeOrErase(MouseEvent e) {
        if (hoverTileX < 0 || hoverTileY < 0)
            return;
        PaletteItem item = getSelectedPaletteItem();
        if (item == null)
            return;

        boolean isRight = javax.swing.SwingUtilities.isRightMouseButton(e);
        boolean isLeft = javax.swing.SwingUtilities.isLeftMouseButton(e);

        // Sağ tık veya silgi → sil
        if (isRight || item.factory == null) {
            eraseAt(hoverTileX, hoverTileY);
            return;
        }

        if (!isLeft)
            return;

        GameObject existing = map.getObjectAt(hoverTileX, hoverTileY);
        if (existing instanceof domain.models.staticObjects.LevelDoor) {
            return; // Cannot overwrite the level door!
        }

        GameObject obj = item.factory.apply(hoverTileX, hoverTileY);

        boolean clickedIsWallMounted = (obj instanceof domain.models.staticObjects.WallObject ||
                (obj instanceof domain.models.entity.SearchableObject && obj.getImageName() != null
                        && obj.getImageName().contains("WallSearchable/")));
        if (clickedIsWallMounted && hoverTileY == 0) {
            int doorX = getLevelDoorX();
            if (doorX != -1 && (hoverTileX == doorX - 1 || hoverTileX == doorX + 1)) {
                return; // Silent return, nothing happens
            }
        }

        // Cannot place obstacles in front of the door (which is at y==0)
        if (hoverTileY == 1 && isObstacle(obj)) {
            GameObject above = map.getObjectAt(hoverTileX, 0);
            if (above instanceof domain.models.staticObjects.LevelDoor) {
                return; // Cannot block the front of the door!
            }
            if (above instanceof domain.models.tile.WallTile) {
                GameObject deco = ((domain.models.tile.WallTile) above).getDecoration();
                if (deco instanceof domain.models.entity.SearchableObject) {
                    JOptionPane.showMessageDialog(this, "Cannot place obstacle in front of a searchable wall object!");
                    return;
                }
            }
        }

        // Check bottom wall for searchable objects
        if (hoverTileY == map.getHeight() - 2 && isObstacle(obj)) {
            GameObject below = map.getObjectAt(hoverTileX, map.getHeight() - 1);
            if (below instanceof domain.models.tile.WallTile) {
                GameObject deco = ((domain.models.tile.WallTile) below).getDecoration();
                if (deco instanceof domain.models.entity.SearchableObject) {
                    JOptionPane.showMessageDialog(this, "Cannot place obstacle in front of a searchable wall object!");
                    return;
                }
            }
        }

        boolean isWallMounted = (obj instanceof domain.models.staticObjects.WallObject ||
                (obj instanceof domain.models.entity.SearchableObject && obj.getImageName() != null
                        && obj.getImageName().contains("WallSearchable/")));

        if (existing instanceof WallTile) {
            if (!isWallMounted) {
                return; // Normal items cannot be placed on wall tiles
            }
        }

        if (isWallMounted) {
            if (!(existing instanceof WallTile) ||
                    (hoverTileY != 0 && hoverTileY != map.getHeight() - 1)
                    ||
                    hoverTileX == 0 || hoverTileX == map.getWidth() - 1) {
                return; // WallObjects can only be placed on top and bottom non-side wall tiles
            }
        }

        if (obj instanceof domain.models.staticObjects.WallObject
                || obj instanceof domain.models.entity.SearchableObject) {
            String img = obj.getImageName();
            if (img != null) {
                if (img.contains("WallSearchable/")) {
                    boolean replacingSearchable = false;
                    if (existing instanceof WallTile) {
                        GameObject currentDeco = ((WallTile) existing).getDecoration();
                        if (currentDeco != null && currentDeco.getImageName() != null
                                && currentDeco.getImageName().contains("WallSearchable/")) {
                            replacingSearchable = true;
                        }
                    }
                    if (!replacingSearchable && countWallSearchable() >= MAX_SEARCHABLE_PER_MAP) {
                        if (e.getID() != MouseEvent.MOUSE_DRAGGED) {
                            showMaxWallDialog();
                        }
                        return;
                    }
                } else if (img.contains("WallDecoration/")) {
                    boolean replacingDecorative = false;
                    if (existing instanceof WallTile) {
                        GameObject currentDeco = ((WallTile) existing).getDecoration();
                        if (currentDeco != null && currentDeco.getImageName() != null
                                && currentDeco.getImageName().contains("WallDecoration/")) {
                            replacingDecorative = true;
                        }
                    }
                    if (!replacingDecorative && countWallDecorative() >= MAX_DECORATIVE_PER_MAP) {
                        if (e.getID() != MouseEvent.MOUSE_DRAGGED) {
                            showMaxWallDialog();
                        }
                        return;
                    }
                }
            }
        }

        boolean replacingSameCategory = false;
        if (isItem(obj)) {
            if (isItem(existing)) {
                replacingSameCategory = true;
            }
            if (!replacingSameCategory && countItems() >= MAX_ITEMS) {
                if (e.getID() != MouseEvent.MOUSE_DRAGGED) {
                    showMaxItemDialog();
                }
                return;
            }
        } else if (isObstacle(obj)) {
            if (isObstacle(existing)) {
                replacingSameCategory = true;
            }
            if (!replacingSameCategory && countObstacles() >= MAX_OBSTACLES) {
                if (e.getID() != MouseEvent.MOUSE_DRAGGED) {
                    showMaxObstacleDialog();
                }
                return;
            }
        }

        map.placeObject(obj, hoverTileX, hoverTileY);
    }

    private void eraseAt(int tx, int ty) {
        GameObject existing = map.getObjectAt(tx, ty);
        if (existing == null)
            return;
        if (existing instanceof domain.models.staticObjects.LevelDoor) {
            return; // Cannot erase the level door!
        }
        if (existing instanceof WallTile) {
            map.placeObject(null, tx, ty);
        } else {
            map.placeObject(new FloorTile(), tx, ty);
        }
    }

    private int countItems() {
        int count = 0;
        int w = map.getWidth();
        int h = map.getHeight();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj != null && isItem(obj)) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countObstacles() {
        int count = 0;
        int w = map.getWidth();
        int h = map.getHeight();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj != null && isObstacle(obj)) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countWallSearchable() {
        int count = 0;
        int w = map.getWidth();
        int h = map.getHeight();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj instanceof WallTile) {
                    GameObject deco = ((WallTile) obj).getDecoration();
                    if (deco != null && deco.getImageName() != null
                            && deco.getImageName().contains("WallSearchable/")) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private int countWallDecorative() {
        int count = 0;
        int w = map.getWidth();
        int h = map.getHeight();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj instanceof WallTile) {
                    GameObject deco = ((WallTile) obj).getDecoration();
                    if (deco != null && deco.getImageName() != null
                            && deco.getImageName().contains("WallDecoration/")) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private boolean isItem(GameObject obj) {
        if (obj == null)
            return false;
        return obj instanceof domain.models.item.MapItem
                || obj instanceof domain.models.staticObjects.KeyItem;
    }

    private boolean isObstacle(GameObject obj) {
        if (obj == null)
            return false;
        return obj instanceof domain.models.entity.Chest
                || obj instanceof domain.models.entity.Crate
                || obj instanceof domain.models.entity.DoubleCrate
                || obj instanceof domain.models.entity.Column
                || obj instanceof domain.models.entity.Sign
                || obj instanceof domain.models.staticObjects.Decoration
                || obj instanceof domain.models.entity.SearchableObject;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PALETTE SELECTION
    // ─────────────────────────────────────────────────────────────────────────

    private PaletteItem getPaletteItemAt(int mx, int my) {
        if (mx >= LEFT_PANEL_W)
            return null;

        for (int i = 0; i < obstaclePalette.size(); i++) {
            Rectangle b = getPaletteItemBounds("OBSTACLE", i);
            if (b.contains(mx, my))
                return obstaclePalette.get(i);
        }
        for (int i = 0; i < itemPalette.size(); i++) {
            Rectangle b = getPaletteItemBounds("ITEM", i);
            if (b.contains(mx, my))
                return itemPalette.get(i);
        }
        for (int i = 0; i < wallItemPalette.size(); i++) {
            Rectangle b = getPaletteItemBounds("WALL_ITEM", i);
            if (b.contains(mx, my))
                return wallItemPalette.get(i);
        }

        return null;
    }

    private void selectPaletteAt(int mx, int my) {
        if (mx >= LEFT_PANEL_W)
            return;

        for (int i = 0; i < obstaclePalette.size(); i++) {
            if (getPaletteItemBounds("OBSTACLE", i).contains(mx, my)) {
                if ("OBSTACLE".equals(selectedPanel) && selectedPaletteIdx == i) {
                    selectedPanel = null;
                    selectedPaletteIdx = -1;
                } else {
                    selectedPanel = "OBSTACLE";
                    selectedPaletteIdx = i;
                }
                return;
            }
        }
        for (int i = 0; i < itemPalette.size(); i++) {
            if (getPaletteItemBounds("ITEM", i).contains(mx, my)) {
                if ("ITEM".equals(selectedPanel) && selectedPaletteIdx == i) {
                    selectedPanel = null;
                    selectedPaletteIdx = -1;
                } else {
                    selectedPanel = "ITEM";
                    selectedPaletteIdx = i;
                }
                return;
            }
        }
        for (int i = 0; i < wallItemPalette.size(); i++) {
            if (getPaletteItemBounds("WALL_ITEM", i).contains(mx, my)) {
                if ("WALL_ITEM".equals(selectedPanel) && selectedPaletteIdx == i) {
                    selectedPanel = null;
                    selectedPaletteIdx = -1;
                } else {
                    selectedPanel = "WALL_ITEM";
                    selectedPaletteIdx = i;
                }
                return;
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTION BUTTON HANDLING
    // ─────────────────────────────────────────────────────────────────────────

    private void fireActionBtn(int mx, int my) {
        for (ActionBtn btn : actionBtns) {
            if (btn.bounds.contains(mx, my)) {
                btn.action.run();
                return;
            }
        }
    }

    private int countLockedChests() {
        int count = 0;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj instanceof Chest && ((Chest) obj).isLocked()) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countKeys() {
        int count = 0;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj instanceof KeyItem) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean validateChestKeyCounts() {
        int lockedChests = countLockedChests();
        int keys = countKeys();
        if (keys != lockedChests) {
            showMsg("Validation Error: The number of keys placed on the map (" + keys
                    + ") must be equal to the number of locked chests (" + lockedChests + ").",
                    "Invalid Map");
            return false;
        }
        return true;
    }

    private void doPlay() {
        if (!validateChestKeyCounts())
            return;
        if (onPlayMap != null)
            onPlayMap.accept(map);
    }

    private void doPlayTeamMatch() {
        if (!validateChestKeyCounts())
            return;
        if (onPlayTeamMatchMap != null)
            onPlayTeamMatchMap.accept(map);
    }

    // ── 2. Add 5 Random Items + 1 Hidden ─────────────────────────────────────
    private void doAddRandom() {
        List<int[]> freeTiles = getFreeTiles();
        if (freeTiles.isEmpty()) {
            showMsg("Boş tile yok! Önce bazı nesneleri silin.", "Uyarı");
            return;
        }

        int currentItems = countItems();
        if (currentItems >= MAX_ITEMS) {
            showMaxItemDialog();
            return;
        }

        Random rand = new Random();
        int placed = 0;
        int remainingSlots = MAX_ITEMS - currentItems;
        int toPlace = Math.min(5, remainingSlots);

        // 5 adet rastgele item
        while (placed < toPlace && !freeTiles.isEmpty()) {
            int[] pos = freeTiles.remove(rand.nextInt(freeTiles.size()));
            GameObject item = MapItem.createRandomItem(pos[0], pos[1]);
            // Container ise içini rastgele doldur
            if (item instanceof Chest) {
                populateContainer((Chest) item, rand);
            } else if (item instanceof Crate) {
                // Crate için özel davranış yok ama kurala uygun hale getirebiliriz
            }
            map.placeObject(item, pos[0], pos[1]);
            placed++;
        }

        repaint();
    }

    /**
     * Container (Chest) içine 1-10 roll ile item ekle.
     * Roll ≥ 8 → yeni item ekle, tekrar et; roll < 8 → dur.
     */
    private void populateContainer(Chest chest, Random rand) {
        // Mevcut mimariye göre Chest'in actions'ı var, ama içerik listesi
        // constructor'da yaratılıyor. Design modunda sadece marker olarak kullanıyoruz.
        // Gerçek oyun akışında OpenAction içeriği halleder.
        // Bu metodda gereksinim kuralını logluyoruz.
        int roll;
        int count = 0;
        do {
            roll = rand.nextInt(10) + 1; // 1-10
            count++;
        } while (roll >= 8 && count < 20); // sonsuz döngü koruması
    }

    private void doSave() {
        if (!validateChestKeyCounts())
            return;
        String name = JOptionPane.showInputDialog(this, "Harita adı girin:", "Save Map", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty())
            return;
        name = name.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");

        try {
            new File("saves/maps").mkdirs();
            String json = mapToJson(name);
            try (FileWriter fw = new FileWriter("saves/maps/" + name + ".mapjson")) {
                fw.write(json);
            }
            showMsg("Harita kaydedildi: saves/maps/" + name + ".mapjson", "Kayıt Başarılı");
        } catch (Exception ex) {
            showMsg("Kayıt hatası: " + ex.getMessage(), "Hata");
        }
    }

    // ── 4. Load Map ──────────────────────────────────────────────────────────
    private void doLoad() {
        File dir = new File("saves/maps");
        if (!dir.exists() || dir.listFiles() == null) {
            showMsg("Kayıtlı harita bulunamadı.", "Yükle");
            return;
        }
        File[] files = dir.listFiles((d, n) -> n.endsWith(".mapjson"));
        if (files == null || files.length == 0) {
            showMsg("Kayıtlı harita bulunamadı.", "Yükle");
            return;
        }
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++)
            names[i] = files[i].getName().replace(".mapjson", "");

        String chosen = (String) JOptionPane.showInputDialog(
                this, "Yüklenecek haritayı seçin:", "Load Map",
                JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
        if (chosen == null)
            return;

        try (FileReader fr = new FileReader("saves/maps/" + chosen + ".mapjson")) {
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = fr.read()) != -1)
                sb.append((char) c);
            loadMapFromJson(sb.toString());
            repaint();
            showMsg("Harita yüklendi: " + chosen, "Yükleme Başarılı");
        } catch (Exception ex) {
            showMsg("Yükleme hatası: " + ex.getMessage(), "Hata");
        }
    }

    private void doClear() {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        Frame parentFrame = (parentWindow instanceof Frame) ? (Frame) parentWindow : null;

        ClearMapDialog dialog = new ClearMapDialog(parentFrame);
        dialog.setVisible(true);

        if (!dialog.isConfirmed())
            return;

        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj instanceof WallTile) {
                    ((WallTile) obj).setDecoration(null);
                    continue;
                }
                if (obj == null || obj instanceof FloorTile || obj instanceof domain.models.staticObjects.LevelDoor)
                    continue;
                // Sadece yerleştirilen objeleri sil → FloorTile ile değiştir
                map.placeObject(new FloorTile(), x, y);
            }
        }
        repaint();
    }

    // ── 6. Generate Random Map ───────────────────────────────────────────────
    private void doGenerateRandomMap() {
        Random rand = new Random();
        int w = map.getWidth();
        int h = map.getHeight();

        // 1. Zemin ve duvarları sıfırla (mevcut duvar dekorasyonlarını temizle)
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj instanceof WallTile) {
                    ((WallTile) obj).setDecoration(null);
                }

                if (x == 0 || x == w - 1) {
                    map.placeObject(new domain.models.tile.WallTile("wall/wall_side"), x, y);
                } else if (y == 0) {
                    map.placeObject(new domain.models.tile.WallTile("wall/wall_1"), x, y);
                } else if (y == h - 1) {
                    map.placeObject(new domain.models.tile.WallTile("wall/wall_2"), x, y);
                } else {
                    map.placeObject(new domain.models.tile.FloorTile(), x, y);
                }
            }
        }

        // 2. Oyuncu başlangıç koruma alanı: (4, 4) etrafında bir bölge
        // Hero start is at (4,4) in DemoRunner. Let's keep a 3x3 area around it empty.
        boolean[][] reserved = new boolean[w][h];
        for (int rx = 3; rx <= 5; rx++) {
            for (int ry = 3; ry <= 5; ry++) {
                reserved[rx][ry] = true;
            }
        }

        // 3. Çıkış Kapısı yerleştirme (Her zaman 1 kapı yerleştirsin, kapı önü boş
        // olacak, en sağ/sol tile'lar hariç)
        int doorX = rand.nextInt(w - 4) + 2;
        map.placeObject(new domain.models.staticObjects.LevelDoor("Level Gate", doorX, 0), doorX, 0);
        map.placeObject(new domain.models.tile.FloorTile(), doorX, 1);
        reserved[doorX][1] = true; // Diğer objelerin kapı önünde spawn olması engellenir

        // 4. Sütunlar (Obstacles) yerleştir
        int numColumns = rand.nextInt(3) + 2; // 2 - 4
        for (int i = 0; i < numColumns; i++) {
            int cx, cy;
            int attempts = 0;
            do {
                cx = rand.nextInt(w - 4) + 2;
                cy = rand.nextInt(h - 4) + 2;
                attempts++;
            } while ((reserved[cx][cy] || !isFarEnough(cx, cy)) && attempts < 100);

            if (attempts < 100) {
                String colImg = rand.nextBoolean() ? "colon/gray_colon_whole" : "colon/purple_colon_whole";
                map.placeObject(new Column("Column", cx, cy, colImg), cx, cy);
                reserved[cx][cy] = true;
            }
        }

        // 5. Sandıklar (Chests) - 2 ila 4 adet
        int numChests = rand.nextInt(3) + 2; // 2 - 4
        int lockedChestCount = 0;
        for (int i = 0; i < numChests; i++) {
            int cx, cy;
            int attempts = 0;
            do {
                cx = rand.nextInt(w - 2) + 1;
                cy = rand.nextInt(h - 2) + 1;
                attempts++;
            } while ((reserved[cx][cy] || !isFarEnough(cx, cy)) && attempts < 100);

            if (attempts < 100) {
                boolean locked = (i == 0); // en az 1 tanesi kilitli sandık olsun
                if (locked)
                    lockedChestCount++;
                map.placeObject(new Chest("Chest", cx, cy, locked), cx, cy);
                reserved[cx][cy] = true;
            }
        }

        // Kasalar (Crates) - 4 ila 6 adet
        int numCrates = rand.nextInt(3) + 4; // 4 - 6
        for (int i = 0; i < numCrates; i++) {
            int cx, cy;
            int attempts = 0;
            do {
                cx = rand.nextInt(w - 2) + 1;
                cy = rand.nextInt(h - 2) + 1;
                attempts++;
            } while ((reserved[cx][cy] || !isFarEnough(cx, cy)) && attempts < 100);

            if (attempts < 100) {
                map.placeObject(new Crate("Crate", cx, cy), cx, cy);
                reserved[cx][cy] = true;
            }
        }

        // 6. Anahtar (KeyItem)
        for (int i = 0; i < lockedChestCount; i++) {
            int keyX, keyY;
            int attempts = 0;
            do {
                keyX = rand.nextInt(w - 4) + 2;
                keyY = rand.nextInt(h - 4) + 2;
                attempts++;
            } while ((reserved[keyX][keyY] || !isFarEnough(keyX, keyY)) && attempts < 100);
            if (attempts < 100) {
                map.placeObject(new KeyItem(keyX, keyY), keyX, keyY);
                reserved[keyX][keyY] = true;
            }
        }

        // 7. Diğer Eşyalar (Potions, Weapons, Armors, Rings)
        int numPotions = rand.nextInt(2) + 2; // 2 - 3
        for (int i = 0; i < numPotions; i++) {
            int px, py;
            int patts = 0;
            do {
                px = rand.nextInt(w - 2) + 1;
                py = rand.nextInt(h - 2) + 1;
                patts++;
            } while ((reserved[px][py] || !isFarEnough(px, py)) && patts < 100);
            if (patts < 100) {
                map.placeObject(PotionItem.createRandomPotionItem(px, py), px, py);
                reserved[px][py] = true;
            }
        }

        int numWeapons = rand.nextInt(3) + 2; // 2 - 4
        for (int i = 0; i < numWeapons; i++) {
            int wx, wy;
            int watts = 0;
            do {
                wx = rand.nextInt(w - 2) + 1;
                wy = rand.nextInt(h - 2) + 1;
                watts++;
            } while ((reserved[wx][wy] || !isFarEnough(wx, wy)) && watts < 100);
            if (watts < 100) {
                GameObject weapon = MapItem.createRandomItem(wx, wy);
                map.placeObject(weapon, wx, wy);
                reserved[wx][wy] = true;
            }
        }

        // 8. Dekorasyon (Torches)
        int numTorches = rand.nextInt(3) + 3; // 3 - 5
        for (int i = 0; i < numTorches; i++) {
            int tx, ty;
            int tatts = 0;
            do {
                tx = rand.nextInt(w - 2) + 1;
                ty = rand.nextInt(h - 2) + 1;
                tatts++;
            } while ((reserved[tx][ty] || !isFarEnough(tx, ty)) && tatts < 100);
            if (tatts < 100) {
                map.placeObject(new Decoration("Torch", tx, ty, "torch/torch_1"), tx, ty);
                reserved[tx][ty] = true;
            }
        }

        // Place random WallObjects
        List<PaletteItem> wallObjectItems = new ArrayList<>();
        for (PaletteItem item : wallItemPalette) {
            if (item.factory != null) {
                GameObject dummy = item.factory.apply(0, 0);
                if (dummy instanceof domain.models.staticObjects.WallObject
                        || dummy instanceof domain.models.entity.SearchableObject) {
                    wallObjectItems.add(item);
                }
            }
        }
        if (!wallObjectItems.isEmpty()) {
            int levelDoorXVal = -1;
            for (int x = 0; x < w; x++) {
                GameObject obj = map.getObjectAt(x, 0);
                if (obj instanceof domain.models.staticObjects.LevelDoor) {
                    levelDoorXVal = x;
                    break;
                }
            }

            List<int[]> wallTiles = new ArrayList<>();
            for (int x = 1; x < w - 1; x++) {
                if (levelDoorXVal != -1 && (x == levelDoorXVal - 1 || x == levelDoorXVal + 1)) {
                    // Skip these adjacent tiles for WallDecorations/Searchables!
                } else {
                    GameObject topWall = map.getObjectAt(x, 0);
                    if (topWall instanceof WallTile && ((WallTile) topWall).getDecoration() == null) {
                        wallTiles.add(new int[] { x, 0 });
                    }
                }
                GameObject botWall = map.getObjectAt(x, h - 1);
                if (botWall instanceof WallTile && ((WallTile) botWall).getDecoration() == null) {
                    wallTiles.add(new int[] { x, h - 1 });
                }
            }

            if (!wallTiles.isEmpty()) {
                java.util.Collections.shuffle(wallTiles);
                int numWallObjects = rand.nextInt(5) + 6; // Place 6 to 10 random WallObjects for a lush, premium look
                int placedWallObjs = 0;
                int placedSearchables = 0;
                int placedDecoratives = 0;
                for (int[] pos : wallTiles) {
                    if (placedWallObjs >= numWallObjects)
                        break;
                    // Filter candidates dynamically based on limits
                    List<PaletteItem> validCandidates = new ArrayList<>();
                    for (PaletteItem item : wallObjectItems) {
                        GameObject dummy = item.factory.apply(0, 0);
                        String img = dummy.getImageName();
                        if (img != null) {
                            if (img.contains("WallSearchable/") && placedSearchables < MAX_SEARCHABLE_PER_MAP) {
                                validCandidates.add(item);
                            } else if (img.contains("WallDecoration/") && placedDecoratives < MAX_DECORATIVE_PER_MAP) {
                                validCandidates.add(item);
                            }
                        }
                    }
                    if (validCandidates.isEmpty())
                        break;

                    PaletteItem selectedItem = validCandidates.get(rand.nextInt(validCandidates.size()));
                    GameObject wallObj = selectedItem.factory.apply(pos[0], pos[1]);

                    String img = wallObj.getImageName();
                    if (img != null) {
                        if (img.contains("WallSearchable/")) {
                            placedSearchables++;
                        } else if (img.contains("WallDecoration/")) {
                            placedDecoratives++;
                        }
                    }

                    map.placeObject(wallObj, pos[0], pos[1]);
                    placedWallObjs++;
                }
            }
        }

        repaint();
    }

    private boolean isFarEnough(int tx, int ty) {
        int[][] dirs = { { 0, 0 }, { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        for (int[] d : dirs) {
            int nx = tx + d[0];
            int ny = ty + d[1];
            if (map.isValidPosition(nx, ny)) {
                GameObject obj = map.getObjectAt(nx, ny);
                if (obj != null && !(obj instanceof domain.models.tile.FloorTile)
                        && !(obj instanceof domain.models.tile.WallTile)) {
                    return false;
                }
            }
        }
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON SERIALIZE / DESERIALIZE (hafif, Gson'suz)
    // ─────────────────────────────────────────────────────────────────────────

    private String mapToJson(String name) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"name\": \"").append(name).append("\",\n");
        sb.append("  \"timestamp\": \"")
                .append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date()))
                .append("\",\n");
        sb.append("  \"width\": ").append(map.getWidth()).append(",\n");
        sb.append("  \"height\": ").append(map.getHeight()).append(",\n");
        sb.append("  \"objects\": [\n");

        boolean first = true;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj == null || obj instanceof FloorTile)
                    continue;

                if (obj instanceof WallTile) {
                    WallTile wall = (WallTile) obj;
                    GameObject deco = wall.getDecoration();
                    if (deco != null) {
                        String type = objectType(deco);
                        if (type != null) {
                            if (!first)
                                sb.append(",\n");
                            first = false;
                            sb.append("    {\"type\":\"").append(type)
                                    .append("\",\"name\":\"").append(escape(deco.getName()))
                                    .append("\",\"x\":").append(x)
                                    .append(",\"y\":").append(y)
                                    .append(",\"isWallMounted\":true")
                                    .append(",\"customScale\":").append(deco.getCustomScale())
                                    .append(",\"imageName\":\"").append(escape(deco.getImageName())).append("\"");

                            if (deco instanceof domain.models.entity.SearchableObject) {
                                domain.models.entity.SearchableObject so = (domain.models.entity.SearchableObject) deco;
                                sb.append(",\"searched\":").append(so.isSearched());
                                if (so.getHiddenItem() != null) {
                                    sb.append(",\"hiddenItemType\":\"")
                                            .append(so.getHiddenItem().getClass().getSimpleName()).append("\"");
                                }
                            }
                            sb.append("}");
                        }
                    }
                    continue;
                }

                String type = objectType(obj);
                if (type == null)
                    continue;
                if (!first)
                    sb.append(",\n");
                first = false;
                sb.append("    {\"type\":\"").append(type)
                        .append("\",\"name\":\"").append(escape(obj.getName()))
                        .append("\",\"x\":").append(x)
                        .append(",\"y\":").append(y)
                        .append(",\"customScale\":").append(obj.getCustomScale());

                if (obj instanceof Chest) {
                    sb.append(",\"isLocked\":").append(((Chest) obj).isLocked());
                }
                if (obj instanceof Door) {
                    sb.append(",\"isLocked\":").append(((Door) obj).isLocked());
                }
                if (obj instanceof Column || obj instanceof Sign || obj instanceof Decoration) {
                    sb.append(",\"imageName\":\"").append(escape(obj.getImageName())).append("\"");
                }
                if (obj instanceof SearchableObject) {
                    sb.append(",\"openImageName\":\"").append(escape(((SearchableObject) obj).getOpenImageName()))
                            .append("\"")
                            .append(",\"imageName\":\"").append(escape(obj.getImageName())).append("\"");
                }
                if (obj instanceof PotionItem || obj instanceof KeyItem) {
                    sb.append(",\"imageName\":\"").append(escape(obj.getImageName())).append("\"");
                }
                sb.append("}");
            }
        }
        sb.append("\n  ]\n}");
        return sb.toString();
    }

    private String objectType(GameObject obj) {
        if (obj instanceof domain.models.staticObjects.LevelDoor)
            return "LevelDoor";
        if (obj instanceof domain.models.staticObjects.WallObject)
            return "WallObject";
        if (obj instanceof PotionItem)
            return "PotionItem";
        if (obj instanceof SwordItem)
            return "SwordItem";
        if (obj instanceof WoodenSwordItem)
            return "WoodenSwordItem";
        if (obj instanceof SamuraiSwordItem)
            return "SamuraiSwordItem";
        if (obj instanceof DiamondSwordItem)
            return "DiamondSwordItem";
        if (obj instanceof AxeItem)
            return "AxeItem";
        if (obj instanceof BowItem)
            return "BowItem";
        if (obj instanceof FireWandItem)
            return "FireWandItem";
        if (obj instanceof ArmorItem)
            return "ArmorItem";
        if (obj instanceof RingItem)
            return "RingItem";
        if (obj instanceof KeyItem)
            return "KeyItem";
        if (obj instanceof Chest)
            return "Chest";
        if (obj instanceof DoubleCrate)
            return "DoubleCrate";
        if (obj instanceof Crate)
            return "Crate";
        if (obj instanceof Column)
            return "Column";
        if (obj instanceof Sign)
            return "Sign";
        if (obj instanceof Door)
            return "Door";
        if (obj instanceof Decoration)
            return "Decoration";
        if (obj instanceof SearchableObject)
            return "SearchableObject";
        return null;
    }

    private void loadMapFromJson(String json) {
        // Önce haritayı temizle (duvarlar korunur, dekorasyonları temizlenir)
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj instanceof WallTile) {
                    ((WallTile) obj).setDecoration(null);
                } else {
                    map.placeObject(new FloorTile(), x, y);
                }
            }
        }

        // Basit JSON ayrıştırıcı — her nesne kaydı satır bazında işlenir
        for (String line : json.split("\n")) {
            line = line.trim();
            if (!line.startsWith("{\"type\""))
                continue;
            String type = jsonStr(line, "type");
            String name = jsonStr(line, "name");
            int x = jsonInt(line, "x");
            int y = jsonInt(line, "y");
            boolean locked = "true".equals(jsonStr(line, "isLocked"));
            String imgName = jsonStr(line, "imageName");
            String openImgName = jsonStr(line, "openImageName");
            boolean isWallMounted = "true".equals(jsonStr(line, "isWallMounted"));
            double scale = jsonDouble(line, "customScale");
            boolean searched = "true".equals(jsonStr(line, "searched"));
            String hiddenItemType = jsonStr(line, "hiddenItemType");

            GameObject obj = switch (type) {
                case "PotionItem" -> {
                    if (name != null && name.toLowerCase().contains("blue")) {
                        yield new PotionItem(new ManaPotion("Blue Potion", 20), x, y,
                                "images/items/potion/blue_potion.png");
                    } else if (name != null && name.toLowerCase().contains("green")) {
                        yield new PotionItem(new EnergyPotion("Green Potion", 30), x, y,
                                "images/items/potion/green_potion.png");
                    } else {
                        yield new PotionItem(new HealthPotion("Red Potion", 5), x, y,
                                "images/items/potion/red_potion.png");
                    }
                }
                case "SwordItem" -> new SwordItem(x, y);
                case "WoodenSwordItem" -> new WoodenSwordItem(x, y);
                case "SamuraiSwordItem" -> new SamuraiSwordItem(x, y);
                case "DiamondSwordItem" -> new DiamondSwordItem(x, y);
                case "AxeItem" -> new AxeItem(x, y);
                case "BowItem" -> new BowItem(x, y);
                case "FireWandItem" -> new FireWandItem(x, y);
                case "ArmorItem" -> new ArmorItem(x, y);
                case "RingItem" -> {
                    if (name != null && name.toLowerCase().contains("blue")) {
                        yield new RingItem(new BlueRing("Blue Ring"), x, y, "images/items/ring/blue_ring.png");
                    } else if (name != null && name.toLowerCase().contains("red")) {
                        yield new RingItem(new RedRing("Red Ring"), x, y, "images/items/ring/red_ring.png");
                    } else {
                        yield new RingItem(new GreenRing("Ring of Might"), x, y, "images/items/ring/green_ring.png");
                    }
                }
                case "KeyItem" -> imgName != null && !imgName.isEmpty()
                        ? new KeyItem(name, x, y, imgName)
                        : new KeyItem(x, y);
                case "Chest" -> imgName != null && !imgName.isEmpty()
                        ? new Chest(name, x, y, locked, imgName)
                        : new Chest(name, x, y, locked);
                case "DoubleCrate" -> new DoubleCrate(name, x, y);
                case "Crate" -> new Crate(name, x, y);
                case "Column" -> imgName != null && !imgName.isEmpty()
                        ? new Column(name, x, y, imgName)
                        : new Column(name, x, y);
                case "Sign" -> imgName != null && !imgName.isEmpty()
                        ? new Sign(name, x, y, imgName)
                        : new Sign(name, x, y);
                case "LevelDoor" -> new domain.models.staticObjects.LevelDoor(name, x, y);
                case "Door" -> new Door(name, x, y, locked);
                case "Decoration" -> imgName != null && !imgName.isEmpty()
                        ? new Decoration(name, x, y, imgName)
                        : new Decoration(name, x, y, "torch/torch_1");
                case "SearchableObject" -> imgName != null && !imgName.isEmpty()
                        ? new SearchableObject(name, x, y, imgName, openImgName)
                        : new SearchableObject(name, x, y);
                case "WallObject" -> {
                    if (imgName != null && imgName.contains("WallSearchable/")) {
                        yield new SearchableObject(name, x, y, imgName, openImgName);
                    } else {
                        yield new domain.models.staticObjects.WallObject(name, x, y, imgName);
                    }
                }
                default -> null;
            };
            if (obj != null) {
                obj.setCustomScale(scale);
                if (obj instanceof SearchableObject) {
                    SearchableObject so = (SearchableObject) obj;
                    so.setSearched(searched);
                    if (hiddenItemType != null && !hiddenItemType.isEmpty()) {
                        if (hiddenItemType.equals("LevelKey")) {
                            so.setHiddenItem(new domain.models.staticObjects.LevelKey(x, y));
                        } else if (hiddenItemType.equals("KeyItem")) {
                            so.setHiddenItem(new KeyItem(x, y));
                        }
                    }
                }
                if (isWallMounted) {
                    GameObject existing = map.getObjectAt(x, y);
                    if (existing instanceof WallTile) {
                        ((WallTile) existing).setDecoration(obj);
                    }
                } else {
                    map.placeObject(obj, x, y);
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PAINT
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        calculateLayout();

        if (mainFrameImg != null) {
            g2.drawImage(mainFrameImg, 0, 0, getWidth(), getHeight(), null);
        } else {
            g2.setColor(new Color(42, 22, 38));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        paintLeftPalette(g2);
        paintMap(g2);
        paintHoverHighlight(g2);
        paintActionButtons(g2);
        paintCursor(g2);
        paintTooltip(g2);
        paintSelectedLabel(g2);

        g2.dispose();
    }

    private void calculateLayout() {
        int leftPad = 20;
        int rightPad = 40;
        int topPad = 30;
        int bottomPad = 80;

        int mapAreaX = LEFT_PANEL_W + leftPad;
        int mapAreaY = topPad;
        int mapAreaW = getWidth() - mapAreaX - rightPad;
        int mapAreaH = getHeight() - topPad - bottomPad;

        if (mapAreaW <= 0 || mapAreaH <= 0)
            return;

        int tw = mapAreaW / map.getWidth();
        int th = mapAreaH / map.getHeight();
        tileSize = Math.max(4, Math.min(tw, th));

        int mapW = tileSize * map.getWidth();
        int mapH = tileSize * map.getHeight();
        mapOffsetX = mapAreaX + (mapAreaW - mapW) / 2;
        mapOffsetY = mapAreaY + (mapAreaH - mapH) / 2;
    }

    private void drawPaletteCategory(Graphics2D g, String categoryLabel, String categoryId, List<PaletteItem> items) {
        int startY = getCategoryStartY(categoryId);

        if (subtitlePanelImg != null) {
            int sw = 288;
            int sh = 46;
            int sx = (LEFT_PANEL_W - sw) / 2;
            g.drawImage(subtitlePanelImg, sx, startY, sw, sh, null);
            g.setColor(new Color(230, 220, 200));
            g.setFont(vt323Font != null ? vt323Font.deriveFont(Font.BOLD, 22f) : new Font("Monospaced", Font.BOLD, 16));
            FontMetrics fm = g.getFontMetrics();
            int textX = sx + 40;
            g.drawString(categoryLabel, textX, startY + sh / 2 + fm.getAscent() / 2 - 2);
        }

        // Draw items
        for (int i = 0; i < items.size(); i++) {
            PaletteItem item = items.get(i);
            Rectangle bounds = getPaletteItemBounds(categoryId, i);

            // Background box for item
            g.setColor(new Color(30, 15, 25, 200));
            g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);

            boolean selected = categoryId.equals(selectedPanel) && (i == selectedPaletteIdx);
            if (selected) {
                g.setColor(new Color(255, 215, 0, 100));
                g.fillRoundRect(bounds.x - 2, bounds.y - 2, bounds.width + 4, bounds.height + 4, 8, 8);
            }

            BufferedImage icon = getIcon(item);
            drawIconFit(g, icon, bounds.x, bounds.y, bounds.width);

            if (selected) {
                g.setColor(new Color(255, 215, 0));
                g.setStroke(new BasicStroke(2.5f));
                g.drawRoundRect(bounds.x - 2, bounds.y - 2, bounds.width + 4, bounds.height + 4, 8, 8);
            } else {
                g.setColor(new Color(255, 255, 255, 50));
                g.setStroke(new BasicStroke(1f));
                g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
            }
        }
    }

    private void paintLeftPalette(Graphics2D g) {
        Shape oldClip = g.getClip();
        g.setClip(0, 0, LEFT_PANEL_W, getHeight() - BOTTOM_BTN_H);

        if (titlePanelImg != null) {
            int tw = 274;
            int th = 53;
            int tx = (LEFT_PANEL_W - tw) / 2;
            int ty = 12 - leftScrollY;
            g.drawImage(titlePanelImg, tx, ty, tw, th, null);
            g.setColor(new Color(255, 240, 220));
            g.setFont(vt323Font != null ? vt323Font.deriveFont(Font.BOLD, 26f) : new Font("Monospaced", Font.BOLD, 18));
            FontMetrics fm = g.getFontMetrics();
            String title = "BUILD MODE";
            g.drawString(title, tx + (tw - fm.stringWidth(title)) / 2, ty + th / 2 + fm.getAscent() / 2 - 2);
        }

        drawPaletteCategory(g, "OBSTACLE", "OBSTACLE", obstaclePalette);
        drawPaletteCategory(g, "ITEM", "ITEM", itemPalette);
        drawPaletteCategory(g, "WALL ITEM", "WALL_ITEM", wallItemPalette);

        g.setClip(oldClip);
    }

    private void paintTooltip(Graphics2D g) {
        if (hoveredPaletteLabel != null) {
            g.setFont(vt323Font != null ? vt323Font.deriveFont(Font.BOLD, 18f) : new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(hoveredPaletteLabel) + 12;
            int th = fm.getHeight() + 6;
            java.awt.Point mp = getMousePosition();
            if (mp != null) {
                int tx = Math.min(getWidth() - tw - 10, Math.max(10, mp.x - tw / 2));
                int ty = mp.y + 15;
                if (ty + th > getHeight() - BOTTOM_BTN_H) {
                    ty = mp.y - th - 5;
                }
                g.setColor(new Color(20, 10, 5, 230));
                g.fillRoundRect(tx, ty, tw, th, 6, 6);
                g.setColor(new Color(255, 220, 100));
                g.drawRoundRect(tx, ty, tw, th, 6, 6);
                g.drawString(hoveredPaletteLabel, tx + 6, ty + fm.getAscent() + 3);
            }
        }
    }

    private void paintSelectedLabel(Graphics2D g) {
        PaletteItem selected = getSelectedPaletteItem();
        if (selected != null) {
            String selLabel = "[ Selected: " + selected.label + " ]";
            g.setFont(vt323Font != null ? vt323Font.deriveFont(Font.BOLD, 18f) : new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g.getFontMetrics();
            int lw = fm.stringWidth(selLabel) + 10;
            int lh = fm.getHeight() + 4;
            int lx = getWidth() - lw - 15;
            int ly = 15;
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRoundRect(lx, ly, lw, lh, 5, 5);
            g.setColor(new Color(255, 215, 0));
            g.drawRoundRect(lx, ly, lw, lh, 5, 5);
            g.drawString(selLabel, lx + 5, ly + fm.getAscent() + 2);
        }
    }

    private void paintMap(Graphics2D g) {
        if (map == null || tileManager == null)
            return;

        // PASS 1: Draw all floors and base walls (WITHOUT decorations)
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                int px = mapOffsetX + x * tileSize;
                int py = mapOffsetY + y * tileSize;
                GameObject obj = map.getObjectAt(x, y);
                if (obj == null)
                    continue;

                // Draw floor background first
                if (obj instanceof domain.models.item.MapItem ||
                        obj instanceof Column || obj instanceof Chest || obj instanceof Crate ||
                        obj instanceof Door || obj instanceof Decoration ||
                        obj instanceof SearchableObject || obj instanceof Sign ||
                        obj instanceof FloorTile) {
                    BufferedImage floor = tileManager.getTile("floor", x, y);
                    if (floor != null)
                        g.drawImage(floor, px, py, tileSize, tileSize, null);
                }

                // Draw base walls
                if (obj instanceof WallTile) {
                    if ("wall/wall_side".equals(obj.getImageName())) {
                        BufferedImage tImg = tileManager.getTile("wall/wall_side");
                        if (tImg != null) {
                            int sw = Math.max(tileSize / 3, 4) + 6;
                            int dx = (x == 0) ? px + tileSize - sw : px;
                            g.drawImage(tImg, dx, py - 6, sw, tileSize + 6, null);
                        }
                    } else if ("wall/wall_1".equals(obj.getImageName())) {
                        BufferedImage tImg = tileManager.getTile(obj.getImageName());
                        if (tImg != null) {
                            int dh = (int) (tileSize * 1.5);
                            int drawY = py + tileSize - dh;
                            g.drawImage(tImg, px, drawY, tileSize, dh, null);
                        }
                    } else if ("wall/wall_2".equals(obj.getImageName())) {
                        BufferedImage tImg = tileManager.getTile(obj.getImageName());
                        if (tImg != null) {
                            int dh = (int) (tileSize * 1.5);
                            int drawY = py + tileSize - dh;
                            g.drawImage(tImg, px, drawY, tileSize, dh, null);
                        }
                    } else {
                        BufferedImage tImg = tileManager.getTile(obj.getImageName());
                        if (tImg != null) {
                            g.drawImage(tImg, px, py - 6, tileSize, tileSize + 6, null);
                        }
                    }
                } else if (y == 0 && obj instanceof domain.models.staticObjects.LevelDoor) {
                    BufferedImage tImg = tileManager.getTile("wall/wall_1");
                    if (tImg != null) {
                        int dh = (int) (tileSize * 1.5);
                        int drawY = py + tileSize - dh;
                        g.drawImage(tImg, px, drawY, tileSize, dh, null);
                    }
                }
            }
        }

        // PASS 2: Draw all interactive entities, items, obstacles, and wall decorations
        // on top
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                int px = mapOffsetX + x * tileSize;
                int py = mapOffsetY + y * tileSize;
                GameObject obj = map.getObjectAt(x, y);
                if (obj == null)
                    continue;

                // 1. Draw Wall Decorations
                if (obj instanceof WallTile) {
                    WallTile wall = (WallTile) obj;
                    if (wall.getDecoration() != null) {
                        GameObject deco = wall.getDecoration();
                        String imgName = deco.getImageName();
                        if (imgName != null && imgName.contains("WallDecoration/torch")) {
                            long now = System.currentTimeMillis();
                            int frame = (int) ((now / 120) % 4) + 1;
                            imgName = "images/WallDecoration/torch" + frame + ".png";
                        }
                        BufferedImage decoImg = tileManager.getTile(imgName);
                        if (decoImg != null) {
                            int iw = decoImg.getWidth();
                            int ih = decoImg.getHeight();

                            int[] dims = getDecorDimensions(deco.getImageName(), deco.getCustomScale(), tileSize, iw,
                                    ih, deco.getName());
                            int dw = dims[0];
                            int dh = dims[1];
                            int drawX = px + (tileSize - dw) / 2;
                            int drawY;
                            if (deco instanceof domain.models.staticObjects.WallObject
                                    || deco instanceof domain.models.entity.SearchableObject) {
                                int wallOffset = "wall/wall_1".equals(obj.getImageName()) ? 8 : 6;
                                drawY = py - wallOffset / 2 + (tileSize - dh) / 2;
                            } else {
                                drawY = py + tileSize - dh;
                            }
                            g.drawImage(decoImg, drawX, drawY, dw, dh, null);
                        }
                    }
                } else if (!(obj instanceof FloorTile)) {
                    // 2. Draw all non-wall objects (Items, Obstacles, etc.)
                    BufferedImage tImg = null;
                    if (obj instanceof domain.models.item.MapItem) {
                        tImg = ((domain.models.item.MapItem) obj).getSprite();
                    } else {
                        String imgName = obj.getImageName();
                        if (obj instanceof Decoration && imgName != null && imgName.startsWith("torch/")) {
                            long now = System.currentTimeMillis();
                            int[] frames = { 1, 2, 3, 4, 6, 7, 8 };
                            int frame = frames[(int) ((now / 120) % frames.length)];
                            imgName = "torch/torch_" + frame;
                        }
                        tImg = tileManager.getTile(imgName);
                    }

                    if (tImg != null) {
                        if (obj instanceof domain.models.item.MapItem) {
                            double scaleMult = 1.30;
                            if (obj instanceof domain.models.item.usables.PotionItem
                                    || obj instanceof domain.models.item.wearables.RingItem) {
                                scaleMult *= 0.7; // Potions & rings render 30% smaller
                            }
                            int maxDim = (int) (tileSize * scaleMult);
                            int iw = tImg.getWidth();
                            int ih = tImg.getHeight();
                            double scale = Math.min((double) maxDim / iw, (double) maxDim / ih);
                            int dw = (int) (iw * scale);
                            int dh = (int) (ih * scale);
                            int drawX = px + (tileSize - dw) / 2;
                            int drawY = py + (tileSize - dh) / 2;
                            g.drawImage(tImg, drawX, drawY, dw, dh, null);
                        } else if (obj instanceof Column || obj instanceof Chest || obj instanceof Crate ||
                                obj instanceof Door || obj instanceof Decoration ||
                                obj instanceof SearchableObject || obj instanceof Sign) {
                            int iw = tImg.getWidth();
                            int ih = tImg.getHeight();
                            if (iw == 447 && ih == 558) {
                                iw = 31;
                                ih = 64;
                            }
                            int dw = tileSize;
                            int dh = tileSize;
                            if (obj instanceof Door) {
                                dw = tileSize * 2;
                                dh = (int) (ih * ((double) dw / iw));
                            } else {
                                int[] dims = getDecorDimensions(obj.getImageName(), obj.getCustomScale(), tileSize, iw,
                                        ih, obj.getName());
                                dw = dims[0];
                                dh = dims[1];
                            }
                            int drawX = px + (tileSize - dw) / 2;
                            int drawY = py + tileSize - dh; // Bottom aligned!
                            g.drawImage(tImg, drawX, drawY, dw, dh, null);
                        } else {
                            g.setColor(new Color(180, 100, 200, 180));
                            g.fillRect(px + 4, py + 4, tileSize - 8, tileSize - 8);
                        }
                    }
                }
            }
        }
    }

    // ── Hover Highlight ───────────────────────────────────────────────────────
    private void paintHoverHighlight(Graphics2D g) {
        if (hoverTileX < 0 || hoverTileY < 0)
            return;

        int px = mapOffsetX + hoverTileX * tileSize;
        int py = mapOffsetY + hoverTileY * tileSize;

        GameObject obj = map.getObjectAt(hoverTileX, hoverTileY);
        boolean isWall = (obj instanceof WallTile);

        PaletteItem pItem = getSelectedPaletteItem();
        GameObject selectedObj = null;
        if (pItem != null && pItem.factory != null) {
            selectedObj = pItem.factory.apply(hoverTileX, hoverTileY);
        }
        boolean isWallMounted = false;
        if (selectedObj != null) {
            isWallMounted = (selectedObj instanceof domain.models.staticObjects.WallObject ||
                    (selectedObj instanceof domain.models.entity.SearchableObject &&
                            selectedObj.getImageName() != null &&
                            selectedObj.getImageName().contains("WallSearchable/")));
        }

        Color fillColor;
        Color borderColor;

        if (isWall) {
            boolean placeable = isWallTilePlaceable(hoverTileX, hoverTileY, selectedObj, isWallMounted);
            if (placeable) {
                fillColor = new Color(60, 220, 60, 100); // Green fill
                borderColor = new Color(80, 255, 80, 200); // Green border
            } else {
                fillColor = new Color(220, 60, 60, 100); // Red fill
                borderColor = new Color(255, 80, 80, 200); // Red border
            }
        } else {
            if (isWallMounted) {
                fillColor = new Color(220, 60, 60, 100); // Red fill for wall items inside the map
                borderColor = new Color(255, 80, 80, 200); // Red border
            } else {
                fillColor = new Color(220, 220, 60, 100); // Yellow/Orange fill
                borderColor = new Color(255, 240, 80, 200); // Yellow/Orange border
            }
        }

        g.setColor(fillColor);
        g.fillRect(px, py, tileSize, tileSize);

        g.setStroke(new BasicStroke(2));
        g.setColor(borderColor);
        g.drawRect(px, py, tileSize, tileSize);
        g.setStroke(new BasicStroke(1));
    }

    // ── Alt Aksiyon Butonları ─────────────────────────────────────────────────
    private void paintActionButtons(Graphics2D g) {
        int btnCount = actionBtns.size();
        int gap = 4; // small gap
        int maxBtnH = BOTTOM_BTN_H - 6; // 42px

        // First compute the aspect ratios of all buttons
        double[] aspects = new double[btnCount];
        double sumAspect = 0;
        for (int i = 0; i < btnCount; i++) {
            aspects[i] = getBtnAspectRatio(actionBtns.get(i));
            sumAspect += aspects[i];
        }

        // Start with the maximum height
        int btnH = maxBtnH;
        int totalWidthNeeded = (int) (btnH * sumAspect) + gap * (btnCount - 1);

        // If it exceeds the available width, scale down based on width
        int availableWidth = getWidth() - 8;
        if (totalWidthNeeded > availableWidth) {
            btnH = (int) ((availableWidth - gap * (btnCount - 1)) / sumAspect);
            totalWidthNeeded = (int) (btnH * sumAspect) + gap * (btnCount - 1);
        }

        // Center the buttons horizontally and vertically
        int startX = (getWidth() - totalWidthNeeded) / 2;
        int startY = getHeight() - BOTTOM_BTN_H + (BOTTOM_BTN_H - btnH) / 2;

        // Alt şerit arka plan — mor
        g.setColor(new Color(42, 22, 38));
        g.fillRect(0, getHeight() - BOTTOM_BTN_H, getWidth(), BOTTOM_BTN_H);
        // İnce ayırıcı çizgi
        g.setColor(new Color(100, 50, 80, 180));
        g.fillRect(0, getHeight() - BOTTOM_BTN_H, getWidth(), 2);

        g.setFont(vt323Font != null ? vt323Font.deriveFont(Font.BOLD, 18f) : new Font("Arial", Font.BOLD, 12));

        int currentX = startX;
        for (int i = 0; i < btnCount; i++) {
            ActionBtn btn = actionBtns.get(i);
            int btnW = (int) (btnH * aspects[i]);
            int bx = currentX;
            int by = startY;

            btn.bounds.setBounds(bx, by, btnW, btnH);

            BufferedImage sprite = getBtnSprite(btn);
            if (sprite != null) {
                g.drawImage(sprite, bx, by, btnW, btnH, null);
            } else {
                // Buton arka plan (Fallback)
                GradientPaint gp = new GradientPaint(bx, by, btn.bgColor.brighter(),
                        bx, by + btnH, btn.bgColor.darker());
                g.setPaint(gp);
                g.fillRoundRect(bx, by, btnW, btnH, 8, 8);

                // Kenar parlaklık efekti
                g.setColor(new Color(255, 255, 255, 40));
                g.fillRoundRect(bx + 1, by + 1, btnW - 2, btnH / 2, 8, 8);

                // Kenar çizgisi
                g.setColor(btn.bgColor.brighter().brighter());
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(bx, by, btnW, btnH, 8, 8);
                g.setStroke(new BasicStroke(1));

                // Label
                g.setColor(Color.WHITE);
                FontMetrics fm = g.getFontMetrics();
                int lx = bx + (btnW - fm.stringWidth(btn.label)) / 2;
                int ly = by + (btnH + fm.getAscent()) / 2 - 2;
                g.drawString(btn.label, lx, ly);
            }

            currentX += btnW + gap;
        }
    }

    // ── Fare imleciyle birlikte seçili item göster ──────────────────────────
    private int lastMouseX = -1, lastMouseY = -1;

    private void paintCursor(Graphics2D g) {
        PaletteItem item = getSelectedPaletteItem();
        if (item == null || item.factory == null)
            return;
        if (hoverTileX < 0 || hoverTileY < 0)
            return;

        BufferedImage icon = getIcon(item);
        if (item.iconPath != null && item.iconPath.startsWith("torch/")) {
            long now = System.currentTimeMillis();
            int[] frames = { 1, 2, 3, 4, 6, 7, 8 };
            int frame = frames[(int) ((now / 120) % frames.length)];
            icon = tileManager.getTile("torch/torch_" + frame);
        }
        if (icon == null)
            return;

        int px = mapOffsetX + hoverTileX * tileSize;
        int py = mapOffsetY + hoverTileY * tileSize;

        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));

        GameObject dummy = item.factory.apply(0, 0);
        if (dummy instanceof domain.models.item.MapItem) {
            double scaleMult = 1.10;
            if (dummy instanceof domain.models.item.usables.PotionItem
                    || dummy instanceof domain.models.item.wearables.RingItem) {
                scaleMult *= 0.7; // Potions & rings render 30% smaller
            }
            int maxDim = (int) (tileSize * scaleMult);
            int iw = icon.getWidth();
            int ih = icon.getHeight();
            double scale = Math.min((double) maxDim / iw, (double) maxDim / ih);
            int dw = (int) (iw * scale);
            int dh = (int) (ih * scale);
            int drawX = px + (tileSize - dw) / 2;
            int drawY = py + (tileSize - dh) / 2;
            g.drawImage(icon, drawX, drawY, dw, dh, null);
        } else if (dummy instanceof Column || dummy instanceof Chest || dummy instanceof Crate ||
                dummy instanceof Door || dummy instanceof Decoration ||
                dummy instanceof SearchableObject || dummy instanceof Sign ||
                dummy instanceof domain.models.staticObjects.WallObject) {
            int iw = icon.getWidth();
            int ih = icon.getHeight();
            if (iw == 447 && ih == 558) {
                iw = 31;
                ih = 64;
            }
            int[] dims = getDecorDimensions(dummy.getImageName(), dummy.getCustomScale(), tileSize, iw, ih,
                    dummy.getName());
            int dw = dims[0];
            int dh = dims[1];
            int drawX = px + (tileSize - dw) / 2;
            int drawY;
            boolean isHoverWall = (map.getObjectAt(hoverTileX, hoverTileY) instanceof WallTile);
            if (isHoverWall
                    && (dummy instanceof domain.models.staticObjects.WallObject || dummy instanceof SearchableObject)) {
                GameObject tileObj = map.getObjectAt(hoverTileX, hoverTileY);
                int wallOffset = (tileObj != null && "wall/wall_1".equals(tileObj.getImageName())) ? 8 : 6;
                drawY = py - wallOffset / 2 + (tileSize - dh) / 2;
            } else {
                drawY = py + tileSize - dh;
            }
            g.drawImage(icon, drawX, drawY, dw, dh, null);
        } else {
            g.drawImage(icon, px, py, tileSize, tileSize, null);
        }

        g.setComposite(old);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private int getLevelDoorX() {
        if (map == null)
            return -1;
        for (int x = 0; x < map.getWidth(); x++) {
            GameObject obj = map.getObjectAt(x, 0);
            if (obj instanceof domain.models.staticObjects.LevelDoor) {
                return x;
            }
        }
        return -1;
    }

    private boolean isWallTilePlaceable(int tx, int ty, GameObject selectedObj, boolean isWallMounted) {
        if (!isWallMounted || selectedObj == null) {
            return false;
        }

        GameObject existing = map.getObjectAt(tx, ty);
        if (!(existing instanceof WallTile)) {
            return false;
        }

        // Must be top or bottom wall, excluding corners
        if ((ty != 0 && ty != map.getHeight() - 1) || tx == 0 || tx == map.getWidth() - 1) {
            return false;
        }

        WallTile wall = (WallTile) existing;

        // Must not be adjacent to LevelDoor (sağ ve sol)
        int doorX = getLevelDoorX();
        if (ty == 0 && doorX != -1 && (tx == doorX - 1 || tx == doorX + 1)) {
            return false;
        }

        // Check decoration limits
        String img = selectedObj.getImageName();
        if (img != null) {
            if (img.contains("WallSearchable/")) {
                boolean replacingSearchable = false;
                GameObject currentDeco = wall.getDecoration();
                if (currentDeco != null && currentDeco.getImageName() != null
                        && currentDeco.getImageName().contains("WallSearchable/")) {
                    replacingSearchable = true;
                }
                if (!replacingSearchable && countWallSearchable() >= MAX_SEARCHABLE_PER_MAP) {
                    return false;
                }
            } else if (img.contains("WallDecoration/")) {
                boolean replacingDecorative = false;
                GameObject currentDeco = wall.getDecoration();
                if (currentDeco != null && currentDeco.getImageName() != null
                        && currentDeco.getImageName().contains("WallDecoration/")) {
                    replacingDecorative = true;
                }
                if (!replacingDecorative && countWallDecorative() >= MAX_DECORATIVE_PER_MAP) {
                    return false;
                }
            }
        }

        return true;
    }

    private int[] getDecorDimensions(String img, double scale, int tileSize, int iw, int ih, String objName) {
        if (img == null) {
            return new int[] { tileSize, tileSize };
        }
        String lower = img.toLowerCase();

        boolean isAbsolute = !lower.contains("wallsearchable/");
        double baseW;
        if (lower.contains("flag") || lower.contains("banner")) {
            baseW = 30;
        } else if (lower.contains("blood_stain")) {
            baseW = 70;
        } else if (lower.contains("statue") || (objName != null && objName.toLowerCase().contains("statue"))) {
            baseW = 70;
        } else if (lower.contains("acid_ooze")) {
            baseW = 70;
        } else if (lower.contains("chain")) {
            baseW = 60;
        } else if (lower.contains("moss")) {
            baseW = 55;
        } else if (lower.contains("crack")) {
            baseW = 70;
        } else if (lower.contains("wall_grill")) {
            baseW = 50;
        } else if (lower.contains("missing_brick")) {
            baseW = 40;
        } else if (lower.contains("gargoyle")) {
            baseW = 75;
        } else if (lower.contains("wall_cavity")) {
            baseW = 40;
        } else if (lower.contains("pipe_hole")) {
            baseW = 40;
        } else if (lower.contains("loose_stone")) {
            baseW = 50;
        } else if (lower.contains("torch") || (objName != null && objName.toLowerCase().contains("torch"))) {
            baseW = 60;
        } else if (lower.contains("skull") || (objName != null && objName.toLowerCase().contains("skull"))) {
            baseW = tileSize * 0.8;
            isAbsolute = false;
        } else {
            isAbsolute = false;
            if (lower.contains("wallsearchable") || lower.contains("walldecoration")) {
                baseW = Math.max(tileSize - 6, 4);
            } else {
                baseW = tileSize;
            }
        }

        double finalW = isAbsolute ? (baseW * ((double) tileSize / 64.0)) : baseW;
        int dw = (int) Math.round(finalW * scale);
        int dh = (int) Math.round(ih * ((double) dw / iw));

        if (lower.contains("torch") || (objName != null && objName.toLowerCase().contains("torch"))) {
            if (dw > tileSize || dh > tileSize) {
                double f = Math.min((double) tileSize / iw, (double) tileSize / ih);
                dw = (int) Math.round(iw * f);
                dh = (int) Math.round(ih * f);
            }
        }

        return new int[] { dw, dh };
    }

    private BufferedImage trimTransparency(BufferedImage img) {
        if (img == null)
            return null;
        int width = img.getWidth();
        int height = img.getHeight();
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int alpha = (img.getRGB(x, y) >> 24) & 0xff;
                if (alpha > 0) {
                    if (x < minX)
                        minX = x;
                    if (x > maxX)
                        maxX = x;
                    if (y < minY)
                        minY = y;
                    if (y > maxY)
                        maxY = y;
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return img;
        }

        return img.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private BufferedImage getBtnSprite(ActionBtn btn) {
        if (btn.sprite != null)
            return btn.sprite;
        if (btn.spritePath == null)
            return null;

        String key = btn.spritePath;
        if (imgCache.containsKey(key)) {
            btn.sprite = imgCache.get(key);
            return btn.sprite;
        }

        BufferedImage img = null;
        String[] paths = {
                "resources/" + btn.spritePath,
                "../resources/" + btn.spritePath
        };
        for (String p : paths) {
            File f = new File(p);
            if (f.exists()) {
                try {
                    img = ImageIO.read(f);
                } catch (Exception ignored) {
                }
                break;
            }
        }
        if (img != null) {
            img = trimTransparency(img);
            imgCache.put(key, img);
            btn.sprite = img;
        }
        return img;
    }

    private double getBtnAspectRatio(ActionBtn btn) {
        BufferedImage img = getBtnSprite(btn);
        if (img != null) {
            return (double) img.getWidth() / img.getHeight();
        }
        return 3.2;
    }

    private BufferedImage getIcon(PaletteItem item) {
        if (item.icon != null)
            return item.icon;
        if (item.iconPath == null)
            return null;

        String key = item.iconPath;
        if (imgCache.containsKey(key)) {
            item.icon = imgCache.get(key);
            return item.icon;
        }

        BufferedImage img = null;
        if (item.isTileIcon) {
            img = tileManager.getTile(item.iconPath);
        } else {
            // Doğrudan dosya
            String[] paths = {
                    "resources/" + item.iconPath,
                    "../resources/" + item.iconPath
            };
            for (String p : paths) {
                File f = new File(p);
                if (f.exists()) {
                    try {
                        img = ImageIO.read(f);
                    } catch (Exception ignored) {
                    }
                    break;
                }
            }
        }
        if (img != null) {
            imgCache.put(key, img);
            item.icon = img;
        }
        return img;
    }

    /**
     * İkonu verilen slota aspect-ratio koruyarak, ortalanmış çizer.
     * Slot boşsa fallback olarak renkli bir daire çizer.
     */
    private void drawIconFit(Graphics2D g, BufferedImage icon, int slotX, int slotY, int slotSize) {
        if (icon == null) {
            g.setColor(new Color(60, 30, 10, 160));
            g.fillRoundRect(slotX, slotY, slotSize, slotSize, 4, 4);
            g.setColor(new Color(180, 100, 50));
            g.fillOval(slotX + slotSize / 4, slotY + slotSize / 4, slotSize / 2, slotSize / 2);
            return;
        }
        int iw = icon.getWidth();
        int ih = icon.getHeight();
        if (iw == 447 && ih == 558) {
            iw = 31;
            ih = 64;
        }
        float scale = Math.min((float) slotSize / iw, (float) slotSize / ih);
        int dw = Math.max(1, (int) (iw * scale));
        int dh = Math.max(1, (int) (ih * scale));
        int dx = slotX + (slotSize - dw) / 2;
        int dy = slotY + (slotSize - dh) / 2;
        g.drawImage(icon, dx, dy, dw, dh, null);
    }

    /** Haritadaki boş (FloorTile) konumların listesi */
    private List<int[]> getFreeTiles() {
        List<int[]> free = new ArrayList<>();
        for (int x = 0; x < map.getWidth(); x++)
            for (int y = 0; y < map.getHeight(); y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj instanceof FloorTile)
                    free.add(new int[] { x, y });
            }
        return free;
    }

    private void showMsg(String msg, String title) {
        JOptionPane.showMessageDialog(this, msg, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void showMaxItemDialog() {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        Frame parentFrame = (parentWindow instanceof Frame) ? (Frame) parentWindow : null;
        MaxItemDialog dialog = new MaxItemDialog(parentFrame);
        dialog.setVisible(true);
    }

    private void showMaxObstacleDialog() {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        Frame parentFrame = (parentWindow instanceof Frame) ? (Frame) parentWindow : null;
        MaxObstacleDialog dialog = new MaxObstacleDialog(parentFrame);
        dialog.setVisible(true);
    }

    private void showMaxWallDialog() {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        Frame parentFrame = (parentWindow instanceof Frame) ? (Frame) parentWindow : null;
        MaxWallDialog dialog = new MaxWallDialog(parentFrame);
        dialog.setVisible(true);
    }

    // ── Basit JSON yardımcıları ───────────────────────────────────────────────
    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }

    private String jsonStr(String line, String key) {
        String k = "\"" + key + "\":\"";
        int i = line.indexOf(k);
        if (i < 0) {
            String k2 = "\"" + key + "\":";
            int i2 = line.indexOf(k2);
            if (i2 < 0)
                return null;
            i2 += k2.length();
            int j2 = i2;
            while (j2 < line.length() && line.charAt(j2) != ',' && line.charAt(j2) != '}') {
                j2++;
            }
            String val = line.substring(i2, j2).trim();
            if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                val = val.substring(1, val.length() - 1);
            }
            return val;
        }
        i += k.length();
        int j = line.indexOf("\"", i);
        return j < 0 ? null : line.substring(i, j);
    }

    private int jsonInt(String line, String key) {
        String k = "\"" + key + "\":";
        int i = line.indexOf(k);
        if (i < 0)
            return 0;
        i += k.length();
        int j = i;
        while (j < line.length() && (Character.isDigit(line.charAt(j)) || line.charAt(j) == '-'))
            j++;
        try {
            return Integer.parseInt(line.substring(i, j));
        } catch (Exception e) {
            return 0;
        }
    }

    private double jsonDouble(String line, String key) {
        String k = "\"" + key + "\":";
        int i = line.indexOf(k);
        if (i < 0)
            return 1.0;
        i += k.length();
        int j = i;
        while (j < line.length()
                && (Character.isDigit(line.charAt(j)) || line.charAt(j) == '-' || line.charAt(j) == '.'))
            j++;
        try {
            return Double.parseDouble(line.substring(i, j));
        } catch (Exception e) {
            return 1.0;
        }
    }

    private boolean isSameWall(GameMap map, int x, int y, String imgName) {
        if (x < 0 || x >= map.getWidth() || y < 0 || y >= map.getHeight())
            return false;
        GameObject obj = map.getObjectAt(x, y);
        return obj instanceof WallTile && imgName != null && imgName.equals(obj.getImageName());
    }
}
