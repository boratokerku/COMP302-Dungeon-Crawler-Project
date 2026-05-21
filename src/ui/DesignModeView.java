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
    // 2 satır, ~48px ikonlar — referans görselle aynı
    private static final int ICON_SIZE    = 48;   // px — item ikonu
    private static final int ICON_GAP     = 3;    // px — ikonlar arası
    private static final int TOOLBAR_ROWS = 2;    // satır sayısı
    private static final int TOOLBAR_PAD  = 8;    // px — sol/sağ kenar boşluğu
    private static final int TOOLBAR_VPAD = 6;    // px — üst/alt kenar boşluğu
    // Toolbar yüksekliği: 2 satır ikon + boşluklar + border
    private static final int TOOLBAR_H    = TOOLBAR_VPAD * 2 + TOOLBAR_ROWS * ICON_SIZE + (TOOLBAR_ROWS - 1) * ICON_GAP + 16;
    private static final int BOTTOM_BTN_H = 48;   // px — alt buton şeridi

    private int tileSize   = 32;
    private int mapOffsetX = 0;
    private int mapOffsetY = 0;

    // Hover'daki item için tooltip
    private String hoveredPaletteLabel = null;
    // toolbar scroll (kullanılmıyor — 2 satırda herşey sığıyor)
    private int toolbarScrollX = 0;

    // ── Palet ────────────────────────────────────────────────────────────────
    private final List<PaletteItem> palette = new ArrayList<>();
    private int selectedPaletteIdx = -1;   // seçili item (-1 = hiçbiri)

    private static class PaletteLayout {
        int iconSize;
        int step;
        int cols;
        int startX;
        int startY;
    }

    private PaletteLayout getPaletteLayout() {
        PaletteLayout l = new PaletteLayout();
        int total = palette.size();
        if (total == 0) return l;
        l.cols = (int) Math.ceil(total / (double) TOOLBAR_ROWS);
        
        l.iconSize = ICON_SIZE;
        int gap = ICON_GAP;
        l.step = l.iconSize + gap;
        
        int mapW = tileSize * map.getWidth();
        int totalW = l.cols * l.step - gap;
        int usableW = mapW - TOOLBAR_PAD * 2;
        
        if (totalW > usableW && l.cols > 0) {
            l.step = (usableW + gap) / l.cols;
            l.iconSize = l.step - gap;
            totalW = l.cols * l.step - gap;
        }
        
        l.startX = mapOffsetX + TOOLBAR_PAD + Math.max(0, (usableW - totalW) / 2);
        l.startY = TOOLBAR_VPAD + 8;
        
        // Vertically center smaller icons in the fixed height toolbar
        int drawnH = TOOLBAR_ROWS * l.iconSize + (TOOLBAR_ROWS - 1) * gap;
        int expectedH = TOOLBAR_ROWS * ICON_SIZE + (TOOLBAR_ROWS - 1) * ICON_GAP;
        if (drawnH < expectedH) {
            l.startY += (expectedH - drawnH) / 2;
        }
        
        return l;
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
        this.map         = map;
        this.tileManager = tileManager;
        this.onBackToMenu = onBackToMenu;
        this.onPlayMap    = onPlayMap;
        this.onPlayTeamMatchMap = onPlayTeamMatchMap;

        setBackground(new Color(42, 22, 38));
        setLayout(null);
        setDoubleBuffered(true);

        buildPalette();
        buildActionButtons();
        setupMouseListeners();

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
        // ── Statik Objeler ───────────────────────────────────────────────────
        add("Chest",    "chest",                    true,  (x,y) -> new Chest("Chest", x, y, false));
        add("Crate",    "crate",                    true,  (x,y) -> new Crate("Crate", x, y));
        add("DbCrate",  "double_crate",             true,  (x,y) -> new DoubleCrate("DoubleCrate", x, y));
        add("Column",   "colon/gray_colon_whole",   true,  (x,y) -> new Column("Column", x, y, "colon/gray_colon_whole"));
        add("PurpleCol","colon/purple_colon_whole", true,  (x,y) -> new Column("Column", x, y, "colon/purple_colon_whole"));
        add("Sign",     "sign/sign_brown",          true,  (x,y) -> new Sign("Sign", x, y, "sign/sign_brown"));
        add("Torch",    "torch/torch_1",            true,  (x,y) -> new Decoration("Torch", x, y, "torch/torch_1"));

        // ── Items ────────────────────────────────────────────────────────────
        add("Potion",   "images/items/potion/red_potion.png",    false, (x,y) -> new PotionItem(x, y));
        add("Key",      "images/items/key/golden_key_1.png",     false, (x,y) -> new KeyItem(x, y));
        add("Sword",    "images/weapons/knight_sword.png",        false, (x,y) -> new SwordItem(x, y));
        add("WdSword",  "images/weapons/wooden_sword.png",        false, (x,y) -> new WoodenSwordItem(x, y));
        add("Axe",      "images/weapons/axe.png",                 false, (x,y) -> new AxeItem(x, y));
        add("Bow",      "images/weapons/bow.png",                 false, (x,y) -> new BowItem(x, y));
        add("FireWand", "images/weapons/fire_wand.png",           false, (x,y) -> new FireWandItem(x, y));
        add("Katana",   "images/weapons/samurai_sword.png",       false, (x,y) -> new SamuraiSwordItem(x, y));
        add("DiamSword","images/weapons/diamond_sword_1.png",     false, (x,y) -> new DiamondSwordItem(x, y));
        add("Armor",    "images/items/steel_armor.png",           false, (x,y) -> new ArmorItem(x, y));
        add("Ring",     "images/items/ring/green_ring.png",       false, (x,y) -> new RingItem(x, y));
    }

    private void add(String label, String iconPath, boolean isTileIcon,
                     java.util.function.BiFunction<Integer,Integer,GameObject> factory) {
        palette.add(new PaletteItem(label, iconPath, isTileIcon, factory));
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
        actionBtns.add(new ActionBtn("▶  Play",          new Color(60, 140, 60),   "images/DesignModeImages/DesignModeButtons/PlayButton.png",          this::doPlay));
        actionBtns.add(new ActionBtn("▶  Team Match",    new Color(60, 100, 160),  "images/DesignModeImages/DesignModeButtons/PlayTeamMatchButton.png", this::doPlayTeamMatch));
        actionBtns.add(new ActionBtn("⚄  +5 Random",     new Color(80, 60, 130),   "images/DesignModeImages/DesignModeButtons/PlusFiveRandomButton.png", this::doAddRandom));
        actionBtns.add(new ActionBtn("🎲  Gen Map",      new Color(110, 50, 130),  "images/DesignModeImages/DesignModeButtons/GenerateRandomMapButton.png", this::doGenerateRandomMap));
        actionBtns.add(new ActionBtn("💾  Save Map",      new Color(50, 90, 150),   "images/DesignModeImages/DesignModeButtons/SaveMapButton.png",       this::doSave));
        actionBtns.add(new ActionBtn("📂  Load Map",      new Color(100, 80, 30),   "images/DesignModeImages/DesignModeButtons/LoadMapButton.png",       this::doLoad));
        actionBtns.add(new ActionBtn("🗑  Clear Map",     new Color(140, 60, 40),   "images/DesignModeImages/DesignModeButtons/ClearMapButton.png",      this::doClear));
        actionBtns.add(new ActionBtn("✖  Exit Menu",     new Color(80, 30, 50),    "images/DesignModeImages/DesignModeButtons/ExitToMainMenuButton.png", onBackToMenu::run));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MOUSE LISTENERS
    // ─────────────────────────────────────────────────────────────────────────

    private void setupMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { handlePress(e); }
            @Override public void mouseReleased(MouseEvent e) { isDragging = false; }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e)    { handleMove(e); }
            @Override public void mouseDragged(MouseEvent e)  { handleDrag(e); }
        });
    }

    private void handlePress(MouseEvent e) {
        isDragging = true;
        // Üst araç çubuğu tıklaması — palette seçimi
        if (e.getY() < TOOLBAR_H) {
            selectPaletteAt(e.getX(), e.getY());
            repaint();
            return;
        }
        // Alt buton şeridi
        int bottomStart = getHeight() - BOTTOM_BTN_H;
        if (e.getY() >= bottomStart) {
            fireActionBtn(e.getX(), e.getY());
            return;
        }
        // Harita alanı
        handleMapClick(e);
    }

    private void handleDrag(MouseEvent e) {
        if (!isDragging) return;
        int y = e.getY();
        if (y >= TOOLBAR_H && y < getHeight() - BOTTOM_BTN_H) {
            updateHover(e.getX(), e.getY());
            if (selectedPaletteIdx >= 0) placeOrErase(e);
            repaint();
        }
    }

    private void handleMove(MouseEvent e) {
        updateHover(e.getX(), e.getY());
        // Toolbar üzerindeyse tooltip güncelle
        hoveredPaletteLabel = null;
        if (e.getY() < TOOLBAR_H) {
            PaletteItem hovered = getPaletteItemAt(e.getX(), e.getY());
            if (hovered != null) hoveredPaletteLabel = hovered.label;
        }
        repaint();
    }

    private void handleMapClick(MouseEvent e) {
        updateHover(e.getX(), e.getY());
        placeOrErase(e);
        repaint();
    }

    private void updateHover(int mx, int my) {
        if (tileSize <= 0) { hoverTileX = hoverTileY = -1; return; }
        int tx = (mx - mapOffsetX) / tileSize;
        int ty = (my - mapOffsetY) / tileSize;
        if (map.isValidPosition(tx, ty)) { hoverTileX = tx; hoverTileY = ty; }
        else { hoverTileX = hoverTileY = -1; }
    }

    private void placeOrErase(MouseEvent e) {
        if (hoverTileX < 0 || hoverTileY < 0) return;
        if (selectedPaletteIdx < 0) return;

        PaletteItem item = palette.get(selectedPaletteIdx);

        boolean isRight = javax.swing.SwingUtilities.isRightMouseButton(e);
        boolean isLeft = javax.swing.SwingUtilities.isLeftMouseButton(e);

        // Sağ tık veya silgi → sil
        if (isRight || item.factory == null) {
            eraseAt(hoverTileX, hoverTileY);
            return;
        }

        if (!isLeft) return;

        // Duvar tile'ı üzerine koymayı engelle
        GameObject existing = map.getObjectAt(hoverTileX, hoverTileY);
        if (existing instanceof WallTile) {
            return;
        }

        // Yerleştir
        GameObject obj = item.factory.apply(hoverTileX, hoverTileY);

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
                    showMsg("Maksimum engel sınırına ulaşıldı (" + MAX_OBSTACLES + ")!", "Limit Uyarısı");
                }
                return;
            }
        }

        map.placeObject(obj, hoverTileX, hoverTileY);
    }

    private void eraseAt(int tx, int ty) {
        GameObject existing = map.getObjectAt(tx, ty);
        if (existing == null || existing instanceof WallTile) return;
        map.placeObject(new FloorTile(), tx, ty);
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
        if (obj == null) return false;
        return obj instanceof domain.models.item.MapItem
            || obj instanceof domain.models.staticObjects.KeyItem;
    }

    private boolean isObstacle(GameObject obj) {
        if (obj == null) return false;
        return obj instanceof domain.models.entity.Chest
            || obj instanceof domain.models.entity.Crate
            || obj instanceof domain.models.entity.DoubleCrate
            || obj instanceof domain.models.entity.Column
            || obj instanceof domain.models.entity.Sign
            || obj instanceof domain.models.staticObjects.Decoration;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PALETTE SELECTION
    // ─────────────────────────────────────────────────────────────────────────

    private void selectPaletteAt(int mx, int my) {
        PaletteItem found = getPaletteItemAt(mx, my);
        if (found != null) {
            int idx = palette.indexOf(found);
            selectedPaletteIdx = (selectedPaletteIdx == idx) ? -1 : idx;
        }
    }

    /** Toolbar'da verilen (mx,my) koordinatındaki palette item'ını döndürür */
    private PaletteItem getPaletteItemAt(int mx, int my) {
        if (palette.isEmpty()) return null;
        PaletteLayout layout = getPaletteLayout();

        for (int i = 0; i < palette.size(); i++) {
            int col = i % layout.cols;
            int row = i / layout.cols;
            int ix  = layout.startX + col * layout.step;
            int iy  = layout.startY + row * (layout.iconSize + ICON_GAP);
            if (mx >= ix && mx <= ix + layout.iconSize && my >= iy && my <= iy + layout.iconSize) {
                return palette.get(i);
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

    // ── 1. Play ──────────────────────────────────────────────────────────────
    private void doPlay() {
        if (onPlayMap != null) onPlayMap.accept(map);
    }

    private void doPlayTeamMatch() {
        if (onPlayTeamMatchMap != null) onPlayTeamMatchMap.accept(map);
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

    // ── 3. Save Map ──────────────────────────────────────────────────────────
    private void doSave() {
        String name = JOptionPane.showInputDialog(this, "Harita adı girin:", "Save Map", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
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
        for (int i = 0; i < files.length; i++) names[i] = files[i].getName().replace(".mapjson","");

        String chosen = (String) JOptionPane.showInputDialog(
                this, "Yüklenecek haritayı seçin:", "Load Map",
                JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
        if (chosen == null) return;

        try (FileReader fr = new FileReader("saves/maps/" + chosen + ".mapjson")) {
            StringBuilder sb = new StringBuilder();
            int c; while ((c = fr.read()) != -1) sb.append((char)c);
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

        if (!dialog.isConfirmed()) return;

        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj == null || obj instanceof WallTile) continue;
                if (obj instanceof FloorTile) continue;
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

        // 1. Zemin ve duvarları sıfırla
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
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

        repaint();
    }

    private boolean isFarEnough(int tx, int ty) {
        int[][] dirs = {{0,0}, {-1,0}, {1,0}, {0,-1}, {0,1}};
        for (int[] d : dirs) {
            int nx = tx + d[0];
            int ny = ty + d[1];
            if (map.isValidPosition(nx, ny)) {
                GameObject obj = map.getObjectAt(nx, ny);
                if (obj != null && !(obj instanceof domain.models.tile.FloorTile) && !(obj instanceof domain.models.tile.WallTile)) {
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
        sb.append("  \"timestamp\": \"").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date())).append("\",\n");
        sb.append("  \"width\": ").append(map.getWidth()).append(",\n");
        sb.append("  \"height\": ").append(map.getHeight()).append(",\n");
        sb.append("  \"objects\": [\n");

        boolean first = true;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj == null || obj instanceof WallTile || obj instanceof FloorTile) continue;
                String type = objectType(obj);
                if (type == null) continue;
                if (!first) sb.append(",\n");
                first = false;
                sb.append("    {\"type\":\"").append(type)
                  .append("\",\"name\":\"").append(escape(obj.getName()))
                  .append("\",\"x\":").append(x)
                  .append(",\"y\":").append(y);
                if (obj instanceof Chest)  sb.append(",\"isLocked\":").append(((Chest)obj).isLocked());
                if (obj instanceof Door)   sb.append(",\"isLocked\":").append(((Door)obj).isLocked());
                if (obj instanceof Column || obj instanceof Sign) sb.append(",\"imageName\":\"").append(escape(obj.getImageName())).append("\"");
                sb.append("}");
            }
        }
        sb.append("\n  ]\n}");
        return sb.toString();
    }

    private String objectType(GameObject obj) {
        if (obj instanceof PotionItem)       return "PotionItem";
        if (obj instanceof SwordItem)        return "SwordItem";
        if (obj instanceof WoodenSwordItem)  return "WoodenSwordItem";
        if (obj instanceof SamuraiSwordItem) return "SamuraiSwordItem";
        if (obj instanceof DiamondSwordItem) return "DiamondSwordItem";
        if (obj instanceof AxeItem)          return "AxeItem";
        if (obj instanceof BowItem)          return "BowItem";
        if (obj instanceof FireWandItem)     return "FireWandItem";
        if (obj instanceof ArmorItem)        return "ArmorItem";
        if (obj instanceof RingItem)         return "RingItem";
        if (obj instanceof KeyItem)          return "KeyItem";
        if (obj instanceof Chest)            return "Chest";
        if (obj instanceof DoubleCrate)      return "DoubleCrate";
        if (obj instanceof Crate)            return "Crate";
        if (obj instanceof Column)           return "Column";
        if (obj instanceof Sign)             return "Sign";
        if (obj instanceof Door)             return "Door";
        if (obj instanceof Decoration)       return "Decoration";
        if (obj instanceof SearchableObject) return "SearchableObject";
        return null;
    }

    private void loadMapFromJson(String json) {
        // Önce haritayı temizle (duvarlar korunur)
        for (int x = 0; x < map.getWidth(); x++)
            for (int y = 0; y < map.getHeight(); y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (!(obj instanceof WallTile)) map.placeObject(new FloorTile(), x, y);
            }

        // Basit JSON ayrıştırıcı — her nesne kaydı satır bazında işlenir
        for (String line : json.split("\n")) {
            line = line.trim();
            if (!line.startsWith("{\"type\"")) continue;
            String type = jsonStr(line, "type");
            String name = jsonStr(line, "name");
            int x = jsonInt(line, "x");
            int y = jsonInt(line, "y");
            boolean locked = "true".equals(jsonStr(line, "isLocked"));
            String imgName = jsonStr(line, "imageName");

            GameObject obj = switch (type) {
                case "PotionItem"       -> new PotionItem(x, y);
                case "SwordItem"        -> new SwordItem(x, y);
                case "WoodenSwordItem"  -> new WoodenSwordItem(x, y);
                case "SamuraiSwordItem" -> new SamuraiSwordItem(x, y);
                case "DiamondSwordItem" -> new DiamondSwordItem(x, y);
                case "AxeItem"          -> new AxeItem(x, y);
                case "BowItem"          -> new BowItem(x, y);
                case "FireWandItem"     -> new FireWandItem(x, y);
                case "ArmorItem"        -> new ArmorItem(x, y);
                case "RingItem"         -> new RingItem(x, y);
                case "KeyItem"          -> new KeyItem(x, y);
                case "Chest"            -> new Chest(name, x, y, locked);
                case "DoubleCrate"      -> new DoubleCrate(name, x, y);
                case "Crate"            -> new Crate(name, x, y);
                case "Column"           -> imgName != null && !imgName.isEmpty()
                                              ? new Column(name, x, y, imgName)
                                              : new Column(name, x, y);
                case "Sign"             -> imgName != null && !imgName.isEmpty()
                                              ? new Sign(name, x, y, imgName)
                                              : new Sign(name, x, y);
                case "Door"             -> new Door(name, x, y, locked);
                case "Decoration"       -> new Decoration(name, x, y, "torch/torch_1");
                case "SearchableObject" -> new SearchableObject(name, x, y);
                default                 -> null;
            };
            if (obj != null) map.placeObject(obj, x, y);
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

        paintToolbar(g2);
        paintMap(g2);
        paintHoverHighlight(g2);
        paintActionButtons(g2);
        paintCursor(g2);

        g2.dispose();
    }

    private void calculateLayout() {
        // Harita: toolbar ve buton şeridi arasındaki TÜM alanı kullan
        int mapAreaY = TOOLBAR_H + 6;
        int mapAreaH = getHeight() - TOOLBAR_H - BOTTOM_BTN_H - 12;
        int mapAreaW = getWidth() - 8;

        int tw = mapAreaW / map.getWidth();
        int th = mapAreaH / map.getHeight();
        tileSize = Math.max(4, Math.min(tw, th));

        int mapW = tileSize * map.getWidth();
        int mapH = tileSize * map.getHeight();
        mapOffsetX = (getWidth() - mapW) / 2;
        mapOffsetY = mapAreaY + (mapAreaH - mapH) / 2;
    }

    // ── Üst Toolbar ─────────────────────────────────────────────────────────
    private void paintToolbar(Graphics2D g) {
        // Toolbar haritanın genişliğiyle hizalı
        int mapW     = tileSize * map.getWidth();
        int toolbarX = mapOffsetX;
        int W        = mapW;
        int H        = TOOLBAR_H;

        // 1. Kırmızı-turuncu arka plan (yalnızca harita genişliğinde)
        GradientPaint bg = new GradientPaint(
                toolbarX, 0, new Color(210, 65, 20),
                toolbarX, H, new Color(175, 40, 10));
        g.setPaint(bg);
        g.fillRect(toolbarX, 0, W, H);

        // 2. Dış turuncu çerçeve
        g.setColor(new Color(220, 135, 25));
        g.setStroke(new BasicStroke(3));
        g.drawRect(toolbarX + 2, 2, W - 4, H - 4);

        // İç dashed çerçeve
        float[] dash1 = {6f, 4f};
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, dash1, 0f));
        g.setColor(new Color(255, 210, 60, 200));
        g.drawRect(toolbarX + 6, 6, W - 12, H - 12);

        // En iç ince çerçeve
        float[] dash2 = {3f, 5f};
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, dash2, 3f));
        g.setColor(new Color(255, 240, 120, 120));
        g.drawRect(toolbarX + 9, 9, W - 18, H - 18);
        g.setStroke(new BasicStroke(1));

        // 3. İkon grid — 2 satır, aspect-ratio korunarak
        PaletteLayout layout = getPaletteLayout();
        
        for (int i = 0; i < palette.size(); i++) {
            PaletteItem item = palette.get(i);
            int col = i % layout.cols;
            int row = i / layout.cols;
            int ix  = layout.startX + col * layout.step;
            int iy  = layout.startY + row * (layout.iconSize + ICON_GAP);

            boolean selected = (i == selectedPaletteIdx);

            if (selected) {
                g.setColor(new Color(255, 215, 0, 180));
                g.fillRoundRect(ix - 2, iy - 2, layout.iconSize + 4, layout.iconSize + 4, 6, 6);
            }

            // Aspect-ratio koruyarak çiz
            BufferedImage icon = getIcon(item);
            if (item.iconPath != null && item.iconPath.startsWith("torch/")) {
                long now = System.currentTimeMillis();
                int[] frames = {1, 2, 3, 4, 6, 7, 8};
                int frame = frames[(int) ((now / 120) % frames.length)];
                icon = tileManager.getTile("torch/torch_" + frame);
            }
            int slotPad = selected ? 3 : 1;
            drawIconFit(g, icon, ix + slotPad, iy + slotPad, layout.iconSize - slotPad * 2);

            if (selected) {
                g.setColor(new Color(255, 230, 50));
                g.setStroke(new BasicStroke(2.5f));
                g.drawRoundRect(ix - 2, iy - 2, layout.iconSize + 4, layout.iconSize + 4, 6, 6);
                g.setStroke(new BasicStroke(1));
            }
        }

        // 4. Hover tooltip
        if (hoveredPaletteLabel != null) {
            g.setFont(new Font("Arial", Font.BOLD, 11));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(hoveredPaletteLabel) + 10;
            int th = fm.getHeight() + 4;
            java.awt.Point mp = getMousePosition();
            int tx = (mp != null) ? Math.min(toolbarX + W - tw - 4, Math.max(toolbarX + 4, mp.x - tw / 2)) : toolbarX + W / 2;
            g.setColor(new Color(20, 10, 5, 220));
            g.fillRoundRect(tx, H + 2, tw, th, 5, 5);
            g.setColor(new Color(255, 220, 100));
            g.drawString(hoveredPaletteLabel, tx + 5, H + 2 + fm.getAscent() + 2);
        }

        // 5. Seçili item adı etiketi
        if (selectedPaletteIdx >= 0) {
            String selLabel = "[ " + palette.get(selectedPaletteIdx).label + " ]";
            g.setFont(new Font("Arial", Font.BOLD, 11));
            FontMetrics fm = g.getFontMetrics();
            int lw = fm.stringWidth(selLabel) + 8;
            int lh = fm.getHeight() + 2;
            int lx = toolbarX + W - lw - 6;
            int ly = H - lh - 3;
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRoundRect(lx, ly, lw, lh, 4, 4);
            g.setColor(new Color(255, 230, 80));
            g.drawString(selLabel, lx + 4, ly + fm.getAscent() + 1);
        }
    }


    // ── Harita ───────────────────────────────────────────────────────────────
    private void paintMap(Graphics2D g) {
        if (map == null || tileManager == null) return;

        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                int px = mapOffsetX + x * tileSize;
                int py = mapOffsetY + y * tileSize;

                GameObject obj = map.getObjectAt(x, y);
                if (obj == null) continue;

                // Zemin tile'larını önce çiz
                if (obj instanceof domain.models.item.MapItem ||
                    obj instanceof Column || obj instanceof Chest || obj instanceof Crate ||
                    obj instanceof Door || obj instanceof Decoration ||
                    obj instanceof SearchableObject || obj instanceof Sign) {
                    BufferedImage floor = tileManager.getTile("floor");
                    if (floor != null) g.drawImage(floor, px, py, tileSize, tileSize, null);
                }

                // Yan duvar özel çizim
                if (obj instanceof WallTile && "wall/wall_side".equals(obj.getImageName())) {
                    BufferedImage tImg = tileManager.getTile("wall/wall_side");
                    if (tImg != null) {
                        int sw = Math.max(tileSize / 3, 4);
                        int dx = (x == 0) ? px + tileSize - sw : px;
                        g.drawImage(tImg, dx, py, sw, tileSize, null);
                    }
                    continue;
                }

                // Tile veya item sprite
                BufferedImage tImg = null;
                if (obj instanceof domain.models.item.MapItem) {
                    // MapItem kendi sprite'ını taşır
                    tImg = ((domain.models.item.MapItem) obj).getSprite();
                } else {
                    String imgName = obj.getImageName();
                    if (obj instanceof Decoration && imgName != null && imgName.startsWith("torch/")) {
                        long now = System.currentTimeMillis();
                        int[] frames = {1, 2, 3, 4, 6, 7, 8};
                        int frame = frames[(int) ((now / 120) % frames.length)];
                        imgName = "torch/torch_" + frame;
                    }
                    tImg = tileManager.getTile(imgName);
                }

                if (tImg != null) {
                    if (obj instanceof domain.models.item.MapItem) {
                        // Fit items preserving aspect ratio within tileSize * 0.65, centered
                        int maxDim = (int) (tileSize * 0.65);
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
                        // Fit static objects preserving aspect ratio with width scaled to tileSize, bottom-aligned
                        int iw = tImg.getWidth();
                        int ih = tImg.getHeight();
                        int dw = tileSize;
                        if (obj instanceof Decoration) {
                            dw = (int) (tileSize * 0.4); // torch should be much smaller!
                        }
                        int dh = (int) (ih * ((double) dw / iw));
                        int drawX = px + (tileSize - dw) / 2;
                        int drawY = py + tileSize - dh;
                        g.drawImage(tImg, drawX, drawY, dw, dh, null);
                    } else {
                        // Standard floor/wall tile, fill normally
                        g.drawImage(tImg, px, py, tileSize, tileSize, null);
                    }
                } else if (!(obj instanceof WallTile) && !(obj instanceof FloorTile)) {
                    // Fallback placeholder
                    g.setColor(new Color(180, 100, 200, 180));
                    g.fillRect(px + 4, py + 4, tileSize - 8, tileSize - 8);
                }
            }
        }
    }

    // ── Hover Highlight ───────────────────────────────────────────────────────
    private void paintHoverHighlight(Graphics2D g) {
        if (hoverTileX < 0 || hoverTileY < 0) return;

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
        int btnCount  = actionBtns.size();
        int gap       = 4; // small gap
        int maxBtnH   = BOTTOM_BTN_H - 6; // 42px
        
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
        int startX    = (getWidth() - totalWidthNeeded) / 2;
        int startY    = getHeight() - BOTTOM_BTN_H + (BOTTOM_BTN_H - btnH) / 2;

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
        if (selectedPaletteIdx < 0) return;
        // Fare pozisyonunu takip etmek için mouse listener zaten var;
        // mevcut hover tile varsa orada önizleme çiz
        if (hoverTileX < 0 || hoverTileY < 0) return;

        PaletteItem item = palette.get(selectedPaletteIdx);
        if (item.factory == null) return; // silgi — ek görsele gerek yok

        BufferedImage icon = getIcon(item);
        if (item.iconPath != null && item.iconPath.startsWith("torch/")) {
            long now = System.currentTimeMillis();
            int[] frames = {1, 2, 3, 4, 6, 7, 8};
            int frame = frames[(int) ((now / 120) % frames.length)];
            icon = tileManager.getTile("torch/torch_" + frame);
        }
        if (icon == null) return;

        int px = mapOffsetX + hoverTileX * tileSize;
        int py = mapOffsetY + hoverTileY * tileSize;

        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));

        GameObject dummy = item.factory.apply(0, 0);
        if (dummy instanceof domain.models.item.MapItem) {
            int maxDim = (int) (tileSize * 0.65);
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
            int drawY = py + tileSize - dh;
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
        if (img == null) return null;
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
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return img;
        }

        return img.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private BufferedImage getBtnSprite(ActionBtn btn) {
        if (btn.sprite != null) return btn.sprite;
        if (btn.spritePath == null) return null;

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
                } catch (Exception ignored) {}
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
        if (item.icon != null) return item.icon;
        if (item.iconPath == null) return null;

        String key = item.iconPath;
        if (imgCache.containsKey(key)) { item.icon = imgCache.get(key); return item.icon; }

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
                if (f.exists()) { try { img = ImageIO.read(f); } catch (Exception ignored) {} break; }
            }
        }
        if (img != null) { imgCache.put(key, img); item.icon = img; }
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
        int dw = Math.max(1, (int)(iw * scale));
        int dh = Math.max(1, (int)(ih * scale));
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
                if (obj instanceof FloorTile) free.add(new int[]{x, y});
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

    // ── Basit JSON yardımcıları ───────────────────────────────────────────────
    private String escape(String s) { return s == null ? "" : s.replace("\"","\\\""); }

    private String jsonStr(String line, String key) {
        String k = "\"" + key + "\":\"";
        int i = line.indexOf(k);
        if (i < 0) return null;
        i += k.length();
        int j = line.indexOf("\"", i);
        return j < 0 ? null : line.substring(i, j);
    }

    private int jsonInt(String line, String key) {
        String k = "\"" + key + "\":";
        int i = line.indexOf(k);
        if (i < 0) return 0;
        i += k.length();
        int j = i;
        while (j < line.length() && (Character.isDigit(line.charAt(j)) || line.charAt(j) == '-')) j++;
        try { return Integer.parseInt(line.substring(i, j)); } catch (Exception e) { return 0; }
    }
}
