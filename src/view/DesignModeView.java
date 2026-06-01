package view;

import view.dialogs.*;

import domain.models.staticObjects.Chest;
import domain.models.staticObjects.Column;
import domain.models.staticObjects.Crate;
import domain.models.GameObject;
import domain.models.staticObjects.SearchableObject;
import domain.models.staticObjects.Sign;
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
import java.util.ArrayList;
import java.util.List;

public class DesignModeView extends JPanel {
    public final int MAX_OBSTACLES = 50;
    public final int MAX_ITEMS = 25;
    public final int MAX_DECORATIVE_PER_MAP = 30;
    public final int MAX_SEARCHABLE_PER_MAP = 10;

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
    private final view.design.PaletteManager paletteManager;
    private controller.design.DesignModeInputHandler inputHandler;

    private static final int LEFT_PANEL_W = 340;

    private int getCategoryStartY(String category) {
        int y = 80 - leftScrollY; // Title panel height is ~50, margin ~30
        if (category.equals("OBSTACLE"))
            return y;

        int rowsObs = (int) Math.ceil(paletteManager.getObstaclePalette().size() / 4.0);
        y += 46 + rowsObs * 60 + 10; // 46 for subtitle_panel, 60 per row, 10 gap

        if (category.equals("ITEM"))
            return y;

        int rowsItem = (int) Math.ceil(paletteManager.getItemPalette().size() / 4.0);
        y += 46 + rowsItem * 60 + 10;

        return y;
    }

    private int getLeftPanelContentHeight() {
        int rowsObs = (int) Math.ceil(paletteManager.getObstaclePalette().size() / 4.0);
        int rowsItem = (int) Math.ceil(paletteManager.getItemPalette().size() / 4.0);
        int rowsWall = (int) Math.ceil(paletteManager.getWallItemPalette().size() / 4.0);

        int y = 80;
        y += 46 + rowsObs * 60 + 10;
        y += 46 + rowsItem * 60 + 10;
        y += 46 + rowsWall * 60 + 30; // 30 is extra bottom margin
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

    public view.design.PaletteItem getSelectedPaletteItem() {
        return paletteManager.getSelectedPaletteItem();
    }

    // ── Hover ────────────────────────────────────────────────────────────────

    // ── Drag ─────────────────────────────────────────────────────────────────
    private boolean isDragging = false;
    private int lastMouseX = 0;
    private int lastMouseY = 0;

    private controller.design.DesignModeActionHandler actionHandler;

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

        this.paletteManager = new view.design.PaletteManager(tileManager);
        this.inputHandler = new controller.design.DesignModeInputHandler(this, map, paletteManager);
        this.actionHandler = new controller.design.DesignModeActionHandler(this, map, onBackToMenu, onPlayMap,
                onPlayTeamMatchMap);

        loadUIImages();

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

        addMouseListener(inputHandler);
        addMouseMotionListener(inputHandler);
        addMouseWheelListener(inputHandler);

        // Bind Keyboard DELETE and BACKSPACE keys to erase the hovered tile
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
                "eraseHovered");
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0),
                "eraseHovered");
        this.getActionMap().put("eraseHovered", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int hx = inputHandler.getHoverTileX();
                int hy = inputHandler.getHoverTileY();
                if (hx >= 0 && hy >= 0) {
                    inputHandler.eraseAt(hx, hy);
                    repaint();
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

    private void loadUIImages() {
        try {
            mainFrameImg = ImageIO.read(new File("resources/images/BuildMode/mainframe.png"));
            titlePanelImg = ImageIO.read(new File("resources/images/BuildMode/title_panel.png"));
            subtitlePanelImg = ImageIO.read(new File("resources/images/BuildMode/subtitle_panel.png"));
            buildModeBoxImg = ImageIO.read(new File("resources/images/BuildMode/buildmodebox.png"));
        } catch (Exception e) {
            System.err.println("Failed to load BuildMode images: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTION BUTTONS (alt şerit)
    // ─────────────────────────────────────────────────────────────────────────

    public void setLastMouseX(int x) {
        this.lastMouseX = x;
    }

    public void setLastMouseY(int y) {
        this.lastMouseY = y;
    }

    public boolean isInsideLeftPanel(int mx) {
        return mx <= LEFT_PANEL_W;
    }

    public void scrollLeftPanel(int delta) {
        leftScrollY += delta;
        int contentH = getLeftPanelContentHeight();
        int maxScroll = Math.max(0, contentH - getHeight());
        if (leftScrollY > maxScroll)
            leftScrollY = maxScroll;
        if (leftScrollY < 0)
            leftScrollY = 0;
    }

    public boolean handleUiClick(int mx, int my) {
        if (my >= getHeight() - BOTTOM_BTN_H - 10) {
            actionHandler.fireActionBtn(mx, my);
            return true;
        }
        if (mx <= LEFT_PANEL_W) {
            selectPaletteAt(mx, my);
            return true;
        }
        return false;
    }

    public void updatePaletteHover(int mx, int my) {
        view.design.PaletteItem hovered = getPaletteItemAt(mx, my);
        if (hovered != null) {
            hoveredPaletteLabel = hovered.label;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            hoveredPaletteLabel = null;
            setCursor(Cursor.getDefaultCursor());
        }
    }

    public void clearPaletteHover() {
        hoveredPaletteLabel = null;
        setCursor(Cursor.getDefaultCursor());
    }

    public int[] screenToTile(int mx, int my) {
        int contentW = getWidth() - LEFT_PANEL_W;
        int contentH = getHeight() - (BOTTOM_BTN_H + 20);

        int mapPixelW = map.getWidth() * tileSize;
        int mapPixelH = map.getHeight() * tileSize;

        mapOffsetX = LEFT_PANEL_W + (contentW - mapPixelW) / 2;
        mapOffsetY = (contentH - mapPixelH) / 2;

        if (mapOffsetX < LEFT_PANEL_W)
            mapOffsetX = LEFT_PANEL_W + 10;
        if (mapOffsetY < 10)
            mapOffsetY = 10;

        if (mx < mapOffsetX || mx >= mapOffsetX + mapPixelW)
            return null;
        if (my < mapOffsetY || my >= mapOffsetY + mapPixelH)
            return null;

        int tx = (mx - mapOffsetX) / tileSize;
        int ty = (my - mapOffsetY) / tileSize;
        return new int[] { tx, ty };
    }

    public int countItems() {
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

    public int countObstacles() {
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

    public int countWallSearchable() {
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

    public int countWallDecorative() {
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

    public boolean isItem(GameObject obj) {
        if (obj == null)
            return false;
        return obj instanceof domain.models.item.MapItem
                || obj instanceof domain.models.item.KeyItem;
    }

    public boolean isObstacle(GameObject obj) {
        if (obj == null)
            return false;
        return obj instanceof domain.models.staticObjects.Chest
                || obj instanceof domain.models.staticObjects.Crate
                || obj instanceof domain.models.staticObjects.DoubleCrate
                || obj instanceof domain.models.staticObjects.Column
                || obj instanceof domain.models.staticObjects.Sign
                || obj instanceof domain.models.staticObjects.Decoration
                || obj instanceof domain.models.staticObjects.SearchableObject;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PALETTE SELECTION
    // ─────────────────────────────────────────────────────────────────────────

    private view.design.PaletteItem getPaletteItemAt(int mx, int my) {
        if (mx >= LEFT_PANEL_W)
            return null;

        for (int i = 0; i < paletteManager.getObstaclePalette().size(); i++) {
            Rectangle b = getPaletteItemBounds("OBSTACLE", i);
            if (b.contains(mx, my))
                return paletteManager.getObstaclePalette().get(i);
        }
        for (int i = 0; i < paletteManager.getItemPalette().size(); i++) {
            Rectangle b = getPaletteItemBounds("ITEM", i);
            if (b.contains(mx, my))
                return paletteManager.getItemPalette().get(i);
        }
        for (int i = 0; i < paletteManager.getWallItemPalette().size(); i++) {
            Rectangle b = getPaletteItemBounds("WALL_ITEM", i);
            if (b.contains(mx, my))
                return paletteManager.getWallItemPalette().get(i);
        }

        return null;
    }

    private void selectPaletteAt(int mx, int my) {
        if (mx >= LEFT_PANEL_W)
            return;

        for (int i = 0; i < paletteManager.getObstaclePalette().size(); i++) {
            if (getPaletteItemBounds("OBSTACLE", i).contains(mx, my)) {
                if ("OBSTACLE".equals(paletteManager.getSelectedPanel())
                        && paletteManager.getSelectedPaletteIdx() == i) {
                    paletteManager.clearSelection();
                } else {
                    paletteManager.selectItem("OBSTACLE", i);
                }
                return;
            }
        }
        for (int i = 0; i < paletteManager.getItemPalette().size(); i++) {
            if (getPaletteItemBounds("ITEM", i).contains(mx, my)) {
                if ("ITEM".equals(paletteManager.getSelectedPanel()) && paletteManager.getSelectedPaletteIdx() == i) {
                    paletteManager.clearSelection();
                } else {
                    paletteManager.selectItem("ITEM", i);
                }
                return;
            }
        }
        for (int i = 0; i < paletteManager.getWallItemPalette().size(); i++) {
            if (getPaletteItemBounds("WALL_ITEM", i).contains(mx, my)) {
                if ("WALL_ITEM".equals(paletteManager.getSelectedPanel())
                        && paletteManager.getSelectedPaletteIdx() == i) {
                    paletteManager.clearSelection();
                } else {
                    paletteManager.selectItem("WALL_ITEM", i);
                }
                return;
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTION BUTTON HANDLING
    // ─────────────────────────────────────────────────────────────────────────

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

    private void drawPaletteCategory(Graphics2D g, String categoryLabel, String categoryId,
            List<view.design.PaletteItem> items) {
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
            view.design.PaletteItem item = items.get(i);
            Rectangle bounds = getPaletteItemBounds(categoryId, i);

            // Background box for item
            g.setColor(new Color(30, 15, 25, 200));
            g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);

            boolean selected = categoryId.equals(paletteManager.getSelectedPanel())
                    && (i == paletteManager.getSelectedPaletteIdx());
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

        drawPaletteCategory(g, "OBSTACLE", "OBSTACLE", paletteManager.getObstaclePalette());
        drawPaletteCategory(g, "ITEM", "ITEM", paletteManager.getItemPalette());
        drawPaletteCategory(g, "WALL ITEM", "WALL_ITEM", paletteManager.getWallItemPalette());

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
        view.design.PaletteItem selected = getSelectedPaletteItem();
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
                                    || deco instanceof domain.models.staticObjects.SearchableObject) {
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
        int hoverTileX = inputHandler.getHoverTileX();
        int hoverTileY = inputHandler.getHoverTileY();
        if (hoverTileX < 0 || hoverTileY < 0)
            return;

        int px = mapOffsetX + hoverTileX * tileSize;
        int py = mapOffsetY + hoverTileY * tileSize;

        GameObject obj = map.getObjectAt(hoverTileX, hoverTileY);
        view.design.PaletteItem pItem = getSelectedPaletteItem();
        GameObject selectedObj = null;

        if (pItem != null && pItem.factory != null) {
            selectedObj = pItem.factory.apply(hoverTileX, hoverTileY);
        }
        boolean isWallMounted = false;
        if (selectedObj != null) {
            isWallMounted = (selectedObj instanceof domain.models.staticObjects.WallObject ||
                    (selectedObj instanceof domain.models.staticObjects.SearchableObject &&
                            selectedObj.getImageName() != null &&
                            selectedObj.getImageName().contains("WallSearchable/")));
        }

        Color fillColor;
        Color borderColor;

        if (obj instanceof WallTile) {
            if (isWallTilePlaceable(hoverTileX, hoverTileY, selectedObj, isWallMounted)) {
                fillColor = new Color(0, 255, 0, 80);
                borderColor = new Color(0, 255, 0);
            } else {
                fillColor = new Color(220, 60, 60, 100);
                borderColor = new Color(255, 80, 80, 200);
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
        java.util.List<view.design.ActionBtn> btns = actionHandler.getActionButtons();
        int totalBtns = btns.size();
        int gap = 4; // small gap
        int maxBtnH = BOTTOM_BTN_H - 6; // 42px

        // First compute the aspect ratios of all buttons
        double[] aspects = new double[totalBtns];
        double sumAspect = 0;
        for (int i = 0; i < totalBtns; i++) {
            aspects[i] = getBtnAspectRatio(btns.get(i));
            sumAspect += aspects[i];
        }

        // Start with the maximum height
        int btnH = maxBtnH;
        int totalWidthNeeded = (int) (btnH * sumAspect) + gap * (totalBtns - 1);

        // If it exceeds the available width, scale down based on width
        int availableWidth = getWidth() - 8;
        if (totalWidthNeeded > availableWidth) {
            btnH = (int) ((availableWidth - gap * (totalBtns - 1)) / sumAspect);
            totalWidthNeeded = (int) (btnH * sumAspect) + gap * (totalBtns - 1);
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
        for (int i = 0; i < totalBtns; i++) {
            view.design.ActionBtn btn = btns.get(i);
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

    private void paintCursor(Graphics2D g) {
        view.design.PaletteItem item = getSelectedPaletteItem();
        if (item == null || item.factory == null)
            return;

        int hoverTileX = inputHandler.getHoverTileX();
        int hoverTileY = inputHandler.getHoverTileY();
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

    public boolean isWallTilePlaceable(int tx, int ty, GameObject selectedObj, boolean isWallMounted) {
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

    private BufferedImage getBtnSprite(view.design.ActionBtn btn) {
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

    private double getBtnAspectRatio(view.design.ActionBtn btn) {
        BufferedImage img = getBtnSprite(btn);
        if (img != null) {
            return (double) img.getWidth() / img.getHeight();
        }
        return 3.2;
    }

    private BufferedImage getIcon(view.design.PaletteItem item) {
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

    public void showMaxItemDialog() {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        Frame parentFrame = (parentWindow instanceof Frame) ? (Frame) parentWindow : null;
        MaxItemDialog dialog = new MaxItemDialog(parentFrame);
        dialog.setVisible(true);
    }

    public void showMaxObstacleDialog() {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        Frame parentFrame = (parentWindow instanceof Frame) ? (Frame) parentWindow : null;
        MaxObstacleDialog dialog = new MaxObstacleDialog(parentFrame);
        dialog.setVisible(true);
    }

    public void showMaxWallDialog() {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        Frame parentFrame = (parentWindow instanceof Frame) ? (Frame) parentWindow : null;
        MaxWallDialog dialog = new MaxWallDialog(parentFrame);
        dialog.setVisible(true);
    }

    public void showInvalidMapDialog() {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        Frame parentFrame = (parentWindow instanceof Frame) ? (Frame) parentWindow : null;
        InvalidMapDialog dialog = new InvalidMapDialog(parentFrame);
        dialog.setVisible(true);
    }

    private boolean isSameWall(GameMap map, int x, int y, String imgName) {
        if (x < 0 || x >= map.getWidth() || y < 0 || y >= map.getHeight())
            return false;
        GameObject obj = map.getObjectAt(x, y);
        return obj instanceof WallTile && imgName != null && imgName.equals(obj.getImageName());
    }
}
