package ui;

import domain.models.entity.Chest;
import domain.models.entity.Column;
import domain.models.entity.Crate;
import domain.models.entity.DoubleCrate;
import domain.models.entity.GameObject;
import domain.models.entity.SearchableObject;
import domain.models.entity.Sign;
import domain.models.item.*;
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
    private int rightScrollY = 0;
    private int topScrollX = 0;

    // ── Palet ────────────────────────────────────────────────────────────────
    // ── Palet ────────────────────────────────────────────────────────────────
    private final List<PaletteItem> leftPalette = new ArrayList<>();
    private final List<PaletteItem> topPalette = new ArrayList<>();
    private final List<PaletteItem> rightPalette = new ArrayList<>();
    private String selectedPanel = null; // "LEFT", "TOP", "RIGHT"
    private int selectedPaletteIdx = -1; // seçili item (-1 = hiçbiri)

    private static final int LEFT_PANEL_W = 64;
    private static final int RIGHT_PANEL_W = 64;
    private static final int TOP_PANEL_H = 70;

    private Rectangle getLeftItemBounds(int idx) {
        int x = (LEFT_PANEL_W - ICON_SIZE) / 2;
        int y = TOP_PANEL_H + 20 + idx * (ICON_SIZE + 10) - leftScrollY;
        return new Rectangle(x, y, ICON_SIZE, ICON_SIZE);
    }

    private Rectangle getRightItemBounds(int idx) {
        int x = getWidth() - RIGHT_PANEL_W + (RIGHT_PANEL_W - ICON_SIZE) / 2;
        int y = TOP_PANEL_H + 20 + idx * (ICON_SIZE + 10) - rightScrollY;
        return new Rectangle(x, y, ICON_SIZE, ICON_SIZE);
    }

    private Rectangle getTopItemBounds(int idx) {
        int totalW = topPalette.size() * (ICON_SIZE + 8) - 8;
        int startX = Math.max(10, (getWidth() - totalW) / 2) - topScrollX;
        int x = startX + idx * (ICON_SIZE + 8);
        int y = (TOP_PANEL_H - ICON_SIZE) / 2;
        return new Rectangle(x, y, ICON_SIZE, ICON_SIZE);
    }

    private PaletteItem getSelectedPaletteItem() {
        if (selectedPanel == null || selectedPaletteIdx < 0)
            return null;
        if ("LEFT".equals(selectedPanel))
            return leftPalette.get(selectedPaletteIdx);
        if ("RIGHT".equals(selectedPanel))
            return rightPalette.get(selectedPaletteIdx);
        if ("TOP".equals(selectedPanel))
            return topPalette.get(selectedPaletteIdx);
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

        buildPalette();
        buildActionButtons();
        setupMouseListeners();

        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int mx = e.getX();
                int my = e.getY();
                int bottomStart = getHeight() - BOTTOM_BTN_H;
                if (mx < LEFT_PANEL_W && my >= TOP_PANEL_H && my < bottomStart) {
                    leftScrollY += e.getWheelRotation() * 25;
                    int viewH = bottomStart - TOP_PANEL_H;
                    int maxScrollY = Math.max(0, leftPalette.size() * (ICON_SIZE + 10) + 40 - viewH);
                    if (leftScrollY < 0)
                        leftScrollY = 0;
                    if (leftScrollY > maxScrollY)
                        leftScrollY = maxScrollY;
                    repaint();
                } else if (mx >= getWidth() - RIGHT_PANEL_W && my >= TOP_PANEL_H && my < bottomStart) {
                    rightScrollY += e.getWheelRotation() * 25;
                    int viewH = bottomStart - TOP_PANEL_H;
                    int maxScrollY = Math.max(0, rightPalette.size() * (ICON_SIZE + 10) + 40 - viewH);
                    if (rightScrollY < 0)
                        rightScrollY = 0;
                    if (rightScrollY > maxScrollY)
                        rightScrollY = maxScrollY;
                    repaint();
                } else if (my < TOP_PANEL_H) {
                    topScrollX += e.getWheelRotation() * 25;
                    int totalW = topPalette.size() * (ICON_SIZE + 8) - 8;
                    int maxScrollX = Math.max(0, totalW + 40 - getWidth());
                    if (topScrollX < 0)
                        topScrollX = 0;
                    if (topScrollX > maxScrollX)
                        topScrollX = maxScrollX;
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
                                currentScale = Math.max(0.15, Math.min(3.0, currentScale));
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
        // LEFT PALETTE (Obstacles & Wall-Mounted Objects)
        leftPalette.add(new PaletteItem("Crate", "crate", true, (x, y) -> new Crate("Crate", x, y)));
        leftPalette
                .add(new PaletteItem("DbCrate", "double_crate", true, (x, y) -> new DoubleCrate("DoubleCrate", x, y)));
        leftPalette.add(new PaletteItem("Column", "colon/gray_colon_whole", true,
                (x, y) -> new Column("Column", x, y, "colon/gray_colon_whole")));
        leftPalette.add(new PaletteItem("PurpleCol", "colon/purple_colon_whole", true,
                (x, y) -> new Column("Column", x, y, "colon/purple_colon_whole")));
        leftPalette.add(
                new PaletteItem("Sign", "sign/sign_brown", true, (x, y) -> new Sign("Sign", x, y, "sign/sign_brown")));
        leftPalette.add(new PaletteItem("SignOrg", "sign/sign_orange", true,
                (x, y) -> new Sign("Sign", x, y, "sign/sign_orange")));
        leftPalette.add(new PaletteItem("Torch", "torch/torch_1", true,
                (x, y) -> new Decoration("Torch", x, y, "torch/torch_1")));

        // DYNAMICALLY SCAN WallObjects DIRECTORY
        File wallDir = new File("resources/images/WallObjects");
        if (!wallDir.exists()) {
            wallDir = new File("../resources/images/WallObjects");
        }
        if (wallDir.exists() && wallDir.isDirectory()) {
            File[] files = wallDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            if (files != null) {
                for (File file : files) {
                    String filename = file.getName();
                    String rawLabel = filename.substring(0, filename.lastIndexOf('.'));
                    // Capitalize label nicely
                    String label = rawLabel;
                    if (rawLabel.length() > 0) {
                        label = Character.toUpperCase(rawLabel.charAt(0)) + rawLabel.substring(1);
                    }
                    final String finalLabel = label;
                    final String relativePath = "images/WallObjects/" + filename;
                    leftPalette.add(new PaletteItem(finalLabel, relativePath, false,
                            (x, y) -> new domain.models.staticObjects.WallObject(finalLabel, x, y, relativePath)));
                }
            }
        }

        // TOP PALETTE (Usables & Collectibles)
        topPalette.add(new PaletteItem("RedPotion", "images/items/potion/red_potion.png", false,
                (x, y) -> new PotionItem("Red Potion", x, y, "images/items/potion/red_potion.png")));
        topPalette.add(new PaletteItem("BluePotion", "images/items/potion/blue_potion.png", false,
                (x, y) -> new PotionItem("Blue Potion", x, y, "images/items/potion/blue_potion.png")));
        topPalette.add(new PaletteItem("GreenPotion", "images/items/potion/green_potion.png", false,
                (x, y) -> new PotionItem("Green Potion", x, y, "images/items/potion/green_potion.png")));
        topPalette.add(new PaletteItem("GoldKey", "images/items/key/golden_key_1.png", false,
                (x, y) -> new KeyItem("Golden Key", x, y, "images/items/key/golden_key_1.png")));
        topPalette
                .add(new PaletteItem("Sword", "images/weapons/knight_sword.png", false, (x, y) -> new SwordItem(x, y)));
        topPalette.add(new PaletteItem("WdSword", "images/weapons/wooden_sword.png", false,
                (x, y) -> new WoodenSwordItem(x, y)));
        topPalette.add(new PaletteItem("Axe", "images/weapons/axe.png", false, (x, y) -> new AxeItem(x, y)));
        topPalette.add(new PaletteItem("Bow", "images/weapons/bow.png", false, (x, y) -> new BowItem(x, y)));
        topPalette.add(
                new PaletteItem("FireWand", "images/weapons/fire_wand.png", false, (x, y) -> new FireWandItem(x, y)));
        topPalette.add(new PaletteItem("Katana", "images/weapons/samurai_sword.png", false,
                (x, y) -> new SamuraiSwordItem(x, y)));
        topPalette.add(new PaletteItem("DiamSword", "images/weapons/diamond_sword_1.png", false,
                (x, y) -> new DiamondSwordItem(x, y)));
        topPalette.add(new PaletteItem("Armor", "images/items/steel_armor.png", false, (x, y) -> new ArmorItem(x, y)));
        topPalette
                .add(new PaletteItem("Ring", "images/items/ring/green_ring.png", false, (x, y) -> new RingItem(x, y)));

        // RIGHT PALETTE (Searchable Containers)
        rightPalette.add(new PaletteItem("BrChest", "containers/chest_brown", true, (x,
                y) -> new SearchableObject("Brown Chest", x, y, "containers/chest_brown", "containers/empty_chest_1")));
        rightPalette.add(new PaletteItem("RedChest", "containers/chest_red", true,
                (x, y) -> new SearchableObject("Red Chest", x, y, "containers/chest_red", "containers/empty_chest_2")));
        rightPalette.add(new PaletteItem("WhChest", "containers/chest_white", true, (x,
                y) -> new SearchableObject("White Chest", x, y, "containers/chest_white", "containers/empty_chest_3")));
        rightPalette.add(new PaletteItem("GoldChest", "containers/gold_chest_closed", true,
                (x, y) -> new SearchableObject("Gold Chest", x, y, "containers/gold_chest_closed",
                        "containers/gold_chest_empty")));
        rightPalette.add(new PaletteItem("SilvChest", "containers/silver_chest_closed", true,
                (x, y) -> new SearchableObject("Silver Chest", x, y, "containers/silver_chest_closed",
                        "containers/silver_chest_empty")));
        rightPalette.add(new PaletteItem("Bag", "containers/bag", true,
                (x, y) -> new SearchableObject("Bag", x, y, "containers/bag", "storage/bag - empty")));
        rightPalette.add(new PaletteItem("MagBag", "containers/magical_bag", true,
                (x, y) -> new SearchableObject("Magical Bag", x, y, "containers/magical_bag", "storage/bag - empty")));
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

        boolean inTopPanel = (my < TOP_PANEL_H);
        boolean inLeftPanel = (mx < LEFT_PANEL_W && my >= TOP_PANEL_H && my < bottomStart);
        boolean inRightPanel = (mx >= getWidth() - RIGHT_PANEL_W && my >= TOP_PANEL_H && my < bottomStart);

        if (inTopPanel || inLeftPanel || inRightPanel) {
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

        boolean inViewport = (mx >= LEFT_PANEL_W && mx < getWidth() - RIGHT_PANEL_W &&
                my >= TOP_PANEL_H && my < bottomStart);

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
        boolean inViewport = (mx >= LEFT_PANEL_W && mx < getWidth() - RIGHT_PANEL_W &&
                my >= TOP_PANEL_H && my < bottomStart);

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
        GameObject obj = item.factory.apply(hoverTileX, hoverTileY);

        boolean isWallMounted = (obj instanceof domain.models.staticObjects.WallObject);

        if (existing instanceof WallTile) {
            if (!isWallMounted) {
                return; // Normal items cannot be placed on wall tiles
            }
        }

        if (obj instanceof domain.models.staticObjects.WallObject) {
            if (!(existing instanceof WallTile) ||
                    (hoverTileY != 0 && hoverTileY != map.getHeight() - 1)
                    ||
                    hoverTileX == 0 || hoverTileX == map.getWidth() - 1) {
                return; // WallObjects can only be placed on top and bottom non-side wall tiles
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
                || obj instanceof domain.models.entity.SearchableObject
                || obj instanceof domain.models.staticObjects.WallObject;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PALETTE SELECTION
    // ─────────────────────────────────────────────────────────────────────────

    private void selectPaletteAt(int mx, int my) {
        int bottomStart = getHeight() - BOTTOM_BTN_H;
        // Check LEFT panel
        if (mx < LEFT_PANEL_W && my >= TOP_PANEL_H && my < bottomStart) {
            for (int i = 0; i < leftPalette.size(); i++) {
                if (getLeftItemBounds(i).contains(mx, my)) {
                    if ("LEFT".equals(selectedPanel) && selectedPaletteIdx == i) {
                        selectedPanel = null;
                        selectedPaletteIdx = -1;
                    } else {
                        selectedPanel = "LEFT";
                        selectedPaletteIdx = i;
                    }
                    return;
                }
            }
        }
        // Check RIGHT panel
        else if (mx >= getWidth() - RIGHT_PANEL_W && my >= TOP_PANEL_H && my < bottomStart) {
            for (int i = 0; i < rightPalette.size(); i++) {
                if (getRightItemBounds(i).contains(mx, my)) {
                    if ("RIGHT".equals(selectedPanel) && selectedPaletteIdx == i) {
                        selectedPanel = null;
                        selectedPaletteIdx = -1;
                    } else {
                        selectedPanel = "RIGHT";
                        selectedPaletteIdx = i;
                    }
                    return;
                }
            }
        }
        // Check TOP panel
        else if (my < TOP_PANEL_H) {
            for (int i = 0; i < topPalette.size(); i++) {
                if (getTopItemBounds(i).contains(mx, my)) {
                    if ("TOP".equals(selectedPanel) && selectedPaletteIdx == i) {
                        selectedPanel = null;
                        selectedPaletteIdx = -1;
                    } else {
                        selectedPanel = "TOP";
                        selectedPaletteIdx = i;
                    }
                    return;
                }
            }
        }
    }

    private PaletteItem getPaletteItemAt(int mx, int my) {
        int bottomStart = getHeight() - BOTTOM_BTN_H;
        if (mx < LEFT_PANEL_W && my >= TOP_PANEL_H && my < bottomStart) {
            for (int i = 0; i < leftPalette.size(); i++) {
                if (getLeftItemBounds(i).contains(mx, my)) {
                    return leftPalette.get(i);
                }
            }
        } else if (mx >= getWidth() - RIGHT_PANEL_W && my >= TOP_PANEL_H && my < bottomStart) {
            for (int i = 0; i < rightPalette.size(); i++) {
                if (getRightItemBounds(i).contains(mx, my)) {
                    return rightPalette.get(i);
                }
            }
        } else if (my < TOP_PANEL_H) {
            for (int i = 0; i < topPalette.size(); i++) {
                if (getTopItemBounds(i).contains(mx, my)) {
                    return topPalette.get(i);
                }
            }
        }
        return null;
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
                if (obj == null || obj instanceof FloorTile)
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

        // 3. Çıkış Kapısı yerleştirme KALDIRILDI (Kapi assla konulmamali)

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
                map.placeObject(new PotionItem(px, py), px, py);
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
        for (PaletteItem item : leftPalette) {
            if (item.factory != null) {
                GameObject dummy = item.factory.apply(0, 0);
                if (dummy instanceof domain.models.staticObjects.WallObject) {
                    wallObjectItems.add(item);
                }
            }
        }
        if (!wallObjectItems.isEmpty()) {
            List<int[]> wallTiles = new ArrayList<>();
            for (int x = 1; x < w - 1; x++) {
                GameObject topWall = map.getObjectAt(x, 0);
                if (topWall instanceof WallTile && ((WallTile) topWall).getDecoration() == null) {
                    wallTiles.add(new int[] { x, 0 });
                }
                GameObject botWall = map.getObjectAt(x, h - 1);
                if (botWall instanceof WallTile && ((WallTile) botWall).getDecoration() == null) {
                    wallTiles.add(new int[] { x, h - 1 });
                }
            }

            if (!wallTiles.isEmpty()) {
                java.util.Collections.shuffle(wallTiles);
                int numWallObjects = rand.nextInt(3) + 2; // Place 2 to 4 random WallObjects
                int placedWallObjs = 0;
                for (int[] pos : wallTiles) {
                    if (placedWallObjs >= numWallObjects)
                        break;
                    PaletteItem selectedItem = wallObjectItems.get(rand.nextInt(wallObjectItems.size()));
                    GameObject wallObj = selectedItem.factory.apply(pos[0], pos[1]);
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
                                    .append(",\"imageName\":\"").append(escape(deco.getImageName())).append("\"}");
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

            GameObject obj = switch (type) {
                case "PotionItem" -> imgName != null && !imgName.isEmpty()
                        ? new PotionItem(name, x, y, imgName)
                        : new PotionItem(x, y);
                case "SwordItem" -> new SwordItem(x, y);
                case "WoodenSwordItem" -> new WoodenSwordItem(x, y);
                case "SamuraiSwordItem" -> new SamuraiSwordItem(x, y);
                case "DiamondSwordItem" -> new DiamondSwordItem(x, y);
                case "AxeItem" -> new AxeItem(x, y);
                case "BowItem" -> new BowItem(x, y);
                case "FireWandItem" -> new FireWandItem(x, y);
                case "ArmorItem" -> new ArmorItem(x, y);
                case "RingItem" -> new RingItem(x, y);
                case "KeyItem" -> imgName != null && !imgName.isEmpty()
                        ? new KeyItem(name, x, y, imgName)
                        : new KeyItem(x, y);
                case "Chest" -> new Chest(name, x, y, locked);
                case "DoubleCrate" -> new DoubleCrate(name, x, y);
                case "Crate" -> new Crate(name, x, y);
                case "Column" -> imgName != null && !imgName.isEmpty()
                        ? new Column(name, x, y, imgName)
                        : new Column(name, x, y);
                case "Sign" -> imgName != null && !imgName.isEmpty()
                        ? new Sign(name, x, y, imgName)
                        : new Sign(name, x, y);
                case "Door" -> new Door(name, x, y, locked);
                case "Decoration" -> imgName != null && !imgName.isEmpty()
                        ? new Decoration(name, x, y, imgName)
                        : new Decoration(name, x, y, "torch/torch_1");
                case "SearchableObject" -> imgName != null && !imgName.isEmpty()
                        ? new SearchableObject(name, x, y, imgName, openImgName)
                        : new SearchableObject(name, x, y);
                case "WallObject" -> new domain.models.staticObjects.WallObject(name, x, y, imgName);
                default -> null;
            };
            if (obj != null) {
                obj.setCustomScale(scale);
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

        paintTopPanel(g2);
        paintLeftPanel(g2);
        paintRightPanel(g2);
        paintMap(g2);
        paintHoverHighlight(g2);
        paintActionButtons(g2);
        paintCursor(g2);
        paintTooltip(g2);
        paintSelectedLabel(g2);

        g2.dispose();
    }

    private void calculateLayout() {
        int mapAreaX = LEFT_PANEL_W;
        int mapAreaY = TOP_PANEL_H;
        int fullW = getWidth() - LEFT_PANEL_W - RIGHT_PANEL_W;
        int fullH = getHeight() - TOP_PANEL_H - BOTTOM_BTN_H;

        int mapAreaW = (int) (fullW * 0.92);
        int mapAreaH = (int) (fullH * 0.92);

        if (mapAreaW <= 0 || mapAreaH <= 0)
            return;

        int tw = mapAreaW / map.getWidth();
        int th = mapAreaH / map.getHeight();
        tileSize = Math.max(4, Math.min(tw, th));

        int mapW = tileSize * map.getWidth();
        int mapH = tileSize * map.getHeight();
        mapOffsetX = mapAreaX + (fullW - mapW) / 2;
        mapOffsetY = mapAreaY + (fullH - mapH) / 2 - 10;
    }

    private void paintTopPanel(Graphics2D g) {
        int W = getWidth();
        int H = TOP_PANEL_H;

        BufferedImage bgImg = tileManager.getTile("carpet/red_carpet_middle");
        BufferedImage botImg = tileManager.getTile("carpet/red_carpet_bottom");
        if (bgImg != null && botImg != null) {
            // Left end cap: rotate 90 degrees counter-clockwise
            java.awt.geom.AffineTransform oldTx = g.getTransform();
            g.translate(H / 2.0, H / 2.0);
            g.rotate(Math.toRadians(-90));
            g.drawImage(botImg, -H / 2, -H / 2, H, H, null);
            g.setTransform(oldTx);

            // Middle body
            g.drawImage(bgImg, H, 0, W - H * 2, H, null);

            // Right end cap: rotate 90 degrees clockwise
            oldTx = g.getTransform();
            g.translate(W - H + H / 2.0, H / 2.0);
            g.rotate(Math.toRadians(90));
            g.drawImage(botImg, -H / 2, -H / 2, H, H, null);
            g.setTransform(oldTx);
        } else if (bgImg != null) {
            g.drawImage(bgImg, 0, 0, W, H, null);
        } else {
            // Retro gradient background fallback (dark red/purple)
            GradientPaint bg = new GradientPaint(
                    0, 0, new Color(50, 20, 40),
                    0, H, new Color(30, 10, 25));
            g.setPaint(bg);
            g.fillRect(0, 0, W, H);
        }

        // Gold border at the bottom
        g.setColor(new Color(212, 175, 55)); // Gold color
        g.setStroke(new BasicStroke(2.5f));
        g.drawLine(0, H - 2, W, H - 2);

        // Items
        Shape oldClip = g.getClip();
        g.setClip(0, 0, W, H - 2);
        for (int i = 0; i < topPalette.size(); i++) {
            PaletteItem item = topPalette.get(i);
            Rectangle bounds = getTopItemBounds(i);

            boolean selected = "TOP".equals(selectedPanel) && (i == selectedPaletteIdx);

            // Set item background to #0A142E (Color(10, 20, 46))
            g.setColor(new Color(10, 20, 46));
            g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);

            if (selected) {
                g.setColor(new Color(255, 215, 0, 100));
                g.fillRoundRect(bounds.x - 2, bounds.y - 2, bounds.width + 4, bounds.height + 4, 8, 8);
            }

            BufferedImage icon = getIcon(item);
            drawIconFit(g, icon, bounds.x, bounds.y, bounds.width);

            if (selected) {
                g.setColor(new Color(255, 215, 0)); // Gold select border
                g.setStroke(new BasicStroke(2.5f));
                g.drawRoundRect(bounds.x - 2, bounds.y - 2, bounds.width + 4, bounds.height + 4, 8, 8);
            } else {
                g.setColor(new Color(255, 255, 255, 40));
                g.setStroke(new BasicStroke(1));
                g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);
            }
        }
        g.setClip(oldClip);

        // Scrollbar
        int viewW = W;
        int contentW = topPalette.size() * (ICON_SIZE + 8) + 12;
        if (contentW > viewW) {
            int sbH = 4;
            int sbW = (int) (((double) viewW / contentW) * viewW);
            int sbX = (int) (((double) topScrollX / (contentW - viewW)) * (viewW - sbW));
            g.setColor(new Color(212, 175, 55, 180));
            g.fillRoundRect(sbX, H - 8, sbW, sbH, 2, 2);
        }
    }

    private void paintLeftPanel(Graphics2D g) {
        int W = LEFT_PANEL_W;
        int H = getHeight() - TOP_PANEL_H - BOTTOM_BTN_H;
        int Y = TOP_PANEL_H;

        BufferedImage bgImg = tileManager.getTile("carpet/red_carpet_middle");
        if (bgImg != null) {
            g.drawImage(bgImg, 0, Y, W, H, null);
        } else {
            // Gradient background fallback
            GradientPaint bg = new GradientPaint(
                    0, Y, new Color(40, 20, 50),
                    W, Y + H, new Color(20, 10, 30));
            g.setPaint(bg);
            g.fillRect(0, Y, W, H);
        }

        // Gold border on the right
        g.setColor(new Color(212, 175, 55));
        g.setStroke(new BasicStroke(2.5f));
        g.drawLine(W - 2, Y, W - 2, Y + H);

        // Items
        Shape oldClip = g.getClip();
        g.setClip(0, Y, W - 2, H);
        for (int i = 0; i < leftPalette.size(); i++) {
            PaletteItem item = leftPalette.get(i);
            Rectangle bounds = getLeftItemBounds(i);

            boolean selected = "LEFT".equals(selectedPanel) && (i == selectedPaletteIdx);

            if (selected) {
                g.setColor(new Color(255, 215, 0, 100));
                g.fillRoundRect(bounds.x - 2, bounds.y - 2, bounds.width + 4, bounds.height + 4, 8, 8);
            }

            BufferedImage icon = getIcon(item);
            if (item.iconPath != null && item.iconPath.startsWith("torch/")) {
                long now = System.currentTimeMillis();
                int[] frames = { 1, 2, 3, 4, 6, 7, 8 };
                int frame = frames[(int) ((now / 120) % frames.length)];
                icon = tileManager.getTile("torch/torch_" + frame);
            }
            drawIconFit(g, icon, bounds.x, bounds.y, bounds.width);

            if (selected) {
                g.setColor(new Color(255, 215, 0));
                g.setStroke(new BasicStroke(2.5f));
                g.drawRoundRect(bounds.x - 2, bounds.y - 2, bounds.width + 4, bounds.height + 4, 8, 8);
            } else {
                g.setColor(new Color(255, 255, 255, 40));
                g.setStroke(new BasicStroke(1));
                g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);
            }
        }
        g.setClip(oldClip);

        // Scrollbar
        int viewH = H;
        int contentH = leftPalette.size() * (ICON_SIZE + 10) + 40;
        if (contentH > viewH) {
            int sbW = 4;
            int sbH = (int) (((double) viewH / contentH) * viewH);
            int sbY = Y + (int) (((double) leftScrollY / (contentH - viewH)) * (viewH - sbH));
            g.setColor(new Color(212, 175, 55, 180)); // gold semi-transparent
            g.fillRoundRect(W - 8, sbY, sbW, sbH, 2, 2);
        }
    }

    private void paintRightPanel(Graphics2D g) {
        int W = RIGHT_PANEL_W;
        int H = getHeight() - TOP_PANEL_H - BOTTOM_BTN_H;
        int X = getWidth() - W;
        int Y = TOP_PANEL_H;

        BufferedImage bgImg = tileManager.getTile("carpet/red_carpet_middle");
        if (bgImg != null) {
            g.drawImage(bgImg, X, Y, W, H, null);
        } else {
            // Gradient background fallback
            GradientPaint bg = new GradientPaint(
                    X, Y, new Color(40, 20, 50),
                    X + W, Y + H, new Color(20, 10, 30));
            g.setPaint(bg);
            g.fillRect(X, Y, W, H);
        }

        // Gold border on the left
        g.setColor(new Color(212, 175, 55));
        g.setStroke(new BasicStroke(2.5f));
        g.drawLine(X + 2, Y, X + 2, Y + H);

        // Items
        Shape oldClip = g.getClip();
        g.setClip(X + 2, Y, W - 2, H);
        for (int i = 0; i < rightPalette.size(); i++) {
            PaletteItem item = rightPalette.get(i);
            Rectangle bounds = getRightItemBounds(i);

            boolean selected = "RIGHT".equals(selectedPanel) && (i == selectedPaletteIdx);

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
                g.setColor(new Color(255, 255, 255, 40));
                g.setStroke(new BasicStroke(1));
                g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);
            }
        }
        g.setClip(oldClip);

        // Scrollbar
        int viewH = H;
        int contentH = rightPalette.size() * (ICON_SIZE + 10) + 40;
        if (contentH > viewH) {
            int sbW = 4;
            int sbH = (int) (((double) viewH / contentH) * viewH);
            int sbY = Y + (int) (((double) rightScrollY / (contentH - viewH)) * (viewH - sbH));
            g.setColor(new Color(212, 175, 55, 180));
            g.fillRoundRect(X + 4, sbY, sbW, sbH, 2, 2);
        }
    }

    private void paintTooltip(Graphics2D g) {
        if (hoveredPaletteLabel != null) {
            g.setFont(new Font("Arial", Font.BOLD, 12));
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
            g.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g.getFontMetrics();
            int lw = fm.stringWidth(selLabel) + 10;
            int lh = fm.getHeight() + 4;
            int lx = getWidth() - lw - 15;
            int ly = TOP_PANEL_H + 10;
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
                    BufferedImage floor = tileManager.getTile("floor");
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
                        BufferedImage decoImg = tileManager.getTile(deco.getImageName());
                        if (decoImg != null) {
                            int iw = decoImg.getWidth();
                            int ih = decoImg.getHeight();

                            int dw = tileSize;
                            if (deco instanceof Decoration) {
                                dw = (int) (tileSize * 0.4);
                            } else if (deco instanceof domain.models.staticObjects.WallObject) {
                                dw = Math.max(tileSize - 6, 4);
                            }
                            dw = (int) (dw * deco.getCustomScale());
                            int dh = (int) (ih * ((double) dw / iw));
                            int drawX = px + (tileSize - dw) / 2;
                            int drawY;
                            if (deco instanceof domain.models.staticObjects.WallObject) {
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
                            if (obj instanceof domain.models.item.PotionItem
                                    || obj instanceof domain.models.item.RingItem) {
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
                            int dw = tileSize;
                            if (obj instanceof Decoration) {
                                dw = (int) (tileSize * 0.4); // torch should be much smaller!
                            } else if (obj instanceof Door) {
                                dw = tileSize * 3;
                            }
                            int dh = (int) (ih * ((double) dw / iw));
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

        // Geçerli: sarı-yeşil; Duvar: kırmızı
        if (isWall) {
            g.setColor(new Color(220, 60, 60, 100));
        } else {
            g.setColor(new Color(220, 220, 60, 100));
        }
        g.fillRect(px, py, tileSize, tileSize);

        g.setStroke(new BasicStroke(2));
        g.setColor(isWall ? new Color(255, 80, 80, 200) : new Color(255, 240, 80, 200));
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

        g.setFont(new Font("Arial", Font.BOLD, 12));

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
            if (dummy instanceof domain.models.item.PotionItem || dummy instanceof domain.models.item.RingItem) {
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
                dummy instanceof SearchableObject || dummy instanceof Sign) {
            int iw = icon.getWidth();
            int ih = icon.getHeight();
            int dw = tileSize;
            if (dummy instanceof Decoration) {
                dw = (int) (tileSize * 0.4); // torch should be much smaller!
            }
            int dh = (int) (ih * ((double) dw / iw));
            int drawX = px + (tileSize - dw) / 2;
            int drawY = py + tileSize - dh; // Bottom aligned!
            g.drawImage(icon, drawX, drawY, dw, dh, null);
        } else {
            g.drawImage(icon, px, py, tileSize, tileSize, null);
        }

        g.setComposite(old);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

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
