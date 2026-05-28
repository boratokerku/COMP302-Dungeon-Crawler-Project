
package view;

import domain.models.Direction;
import domain.models.entity.Hero;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

public class GameView extends JPanel {
    private Hero hero;
    private AssetManager assetManager;

    // Hotbar rendering is delegated to InventoryView (legacy class name)
    private InventoryView inventoryView;

    // Floating text structure for high-quality damage visual effects
    public static class FloatingText {
        public double x, y;
        public String text;
        public Color color;
        public int duration = 30; // 30 frames duration
        public double dy = -0.04; // slowly moves up
        public float alpha = 1.0f;
    }

    private static final java.util.List<FloatingText> floatingTexts = new java.util.ArrayList<>();

    public static java.awt.Font vt323Font;

    static {
        try {
            java.io.File fontFile = new java.io.File("resources/fonts/VT323-Regular.ttf");
            if (!fontFile.exists()) {
                fontFile = new java.io.File("../resources/fonts/VT323-Regular.ttf");
            }
            if (fontFile.exists()) {
                vt323Font = java.awt.Font.createFont(
                        java.awt.Font.TRUETYPE_FONT,
                        fontFile).deriveFont(24f);
                java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(vt323Font);
            } else {
                System.err.println("Font file not found, falling back to Monospaced: " + fontFile.getAbsolutePath());
                vt323Font = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 24);
            }
        } catch (Exception e) {
            e.printStackTrace();
            vt323Font = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 24);
        }
    }

    public static void addFloatingText(double x, double y, String text, Color color) {
        FloatingText ft = new FloatingText();
        ft.x = x;
        ft.y = y;
        ft.text = text;
        ft.color = color;
        synchronized (floatingTexts) {
            floatingTexts.add(ft);
        }
    }

    public GameView(Hero hero, AssetManager assetManager) {
        this.hero = hero;
        this.assetManager = assetManager;

        // Panel ayarları
        this.setPreferredSize(new Dimension(800, 600)); // Pencere boyutu
        this.setBackground(new Color(91, 48, 80)); // Arka plan (Koyu mor — referans görsel)
        this.setDoubleBuffered(true); // Titremeyi önleyen teknik (Double Buffering)

        this.inventoryView = new InventoryView(hero);
    }

    private domain.models.map.GameMap gameMap;
    private domain.models.entity.Knight knight;
    private domain.models.entity.Sorcerer sorcerer;
    private TileManager tileManager;
    private ActionMenu actionMenu;
    private java.util.List<domain.models.entity.Entity> entityList;
    private domain.models.GameMode gameMode = domain.models.GameMode.ADVENTURE;

    private long elapsedSeconds = 0;

    public void setElapsedSeconds(long seconds) {
        this.elapsedSeconds = seconds;
    }

    public long getElapsedSeconds() {
        return this.elapsedSeconds;
    }

    public void setGameMode(domain.models.GameMode mode) {
        this.gameMode = mode;
    }

    // Dinamik hesaplanan ekran değişkenleri
    private int tileSize = 64;
    private int offsetX = 0;
    private int offsetY = 0;

    public void setGameMap(domain.models.map.GameMap map) {
        this.gameMap = map;
    }

    public void setEnemies(domain.models.entity.Knight knight, domain.models.entity.Sorcerer sorcerer) {
        this.knight = knight;
        this.sorcerer = sorcerer;
    }

    /**
     * EnemySpawner ile paylaşılan entity listesini bağlar.
     * Bu liste güncellendiğinde (yeni düşman spawn olduğunda) GameView otomatik
     * olarak
     * yeni düşmanları da çizer — referans olduğu için her zaman güncel kalır.
     */
    public void setEntityList(java.util.List<domain.models.entity.Entity> list) {
        this.entityList = list;
    }

    public void setTileManager(TileManager tileManager) {
        this.tileManager = tileManager;
        this.inventoryView.setTileManager(tileManager);
    }

    public void setActionMenu(ActionMenu actionMenu) {
        this.actionMenu = actionMenu;
    }

    public int getTileSize() {
        return tileSize;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    // Çizimden hemen önce ekran boyutlarını hesaplar
    // Üstte HUD alanı için ekstra boşluk bırakır
    private void calculateDimensions() {
        if (gameMap != null) {
            int hudReserve = 80; // Üstteki HUD barı için ayrılan piksel

            int usableWidth = (int) (getWidth() * 0.82);
            int usableHeight = (int) ((getHeight() - hudReserve) * 0.82);

            int tileW = usableWidth / gameMap.getWidth();
            int tileH = usableHeight / gameMap.getHeight();
            tileSize = Math.min(tileW, tileH);

            offsetX = (getWidth() - (tileSize * gameMap.getWidth())) / 2;
            offsetY = hudReserve + (getHeight() - hudReserve - (tileSize * gameMap.getHeight())) / 2 - 10;
        }
    }

    // Haritanın dışındaki boş alanı wall_1 tile'ı ile döşer (düz renk yerine)
    private void drawBackground(Graphics2D g2d) {
        if (tileManager == null)
            return;
        BufferedImage bgTile = tileManager.getTile("floor");
        if (bgTile == null)
            return;

        // Tüm ekranı tileSize'lık karelerle doldur (harita altında kalsa da sorun
        // değil, üzerine çizilecek)
        for (int px = 0; px < getWidth(); px += tileSize) {
            for (int py = 0; py < getHeight(); py += tileSize) {
                g2d.drawImage(bgTile, px, py, tileSize, tileSize, null);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Her çizim döngüsünde ekranın boyutuna göre karelerin (Tile) büyüklüğünü
        // hesapla
        calculateDimensions();

        // 0. Arka planı tile ile döşe (düz renk yerine)
        drawBackground(g2d);

        // 1. Zemin veya Haritayı Çiz
        drawMap(g2d);

        // 1.5 Etkileşim Alanı (3x3) Vurgusu
        drawInteractionHighlight(g2d);

        // 2. Row-by-row rendering for proper 2.5D perspective (Y-Sorting)
        if (gameMap != null) {
            for (int y = 0; y < gameMap.getHeight(); y++) {
                drawWalls(g2d, y);
                drawStaticObjects(g2d, y);
                drawItems(g2d, y);
                drawHero(g2d, y);
                drawEnemies(g2d, y);
            }
        }

        // 2.5. Uçan mermileri çiz (her zaman en üstte kalacak şekilde)
        drawProjectiles(g2d);

        // 4. HUD (Arayüz) Çiz
        drawHUD(g2d);

        // 5. Inventory Çiz
        drawInventory(g2d);

        // 5.5 Süzülen Hasar Efektlerini Çiz (Floating Texts)
        drawFloatingTexts(g2d);

        if (trapFlashFrames > 0) {
            g2d.setColor(new Color(255, 0, 0, 80));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            trapFlashFrames--;
        }

        if (actionMenu != null && actionMenu.isVisible()) {
            actionMenu.draw(g2d);
        }

        // Kaynakları temizle
        g2d.dispose();
    }

    public static int trapFlashFrames = 0;

    private void drawFloatingTexts(Graphics2D g2d) {
        synchronized (floatingTexts) {
            java.util.Iterator<FloatingText> it = floatingTexts.iterator();
            while (it.hasNext()) {
                FloatingText ft = it.next();

                // Fade out effect using alpha channel
                int alphaVal = (int) (ft.alpha * 255);
                if (alphaVal < 0)
                    alphaVal = 0;
                if (alphaVal > 255)
                    alphaVal = 255;

                g2d.setColor(new Color(ft.color.getRed(), ft.color.getGreen(), ft.color.getBlue(), alphaVal));
                g2d.setFont(vt323Font.deriveFont(java.awt.Font.BOLD, 22f));

                int px = offsetX + (int) (ft.x * tileSize) + tileSize / 4;
                int py = offsetY + (int) (ft.y * tileSize) - 10;

                g2d.drawString(ft.text, px, py);

                // Advance animation states
                ft.y += ft.dy;
                ft.alpha = Math.max(0.0f, ft.alpha - 0.035f);
                ft.duration--;

                if (ft.duration <= 0) {
                    it.remove();
                }
            }
        }
    }

    // YENİ HUD ASSETLERİ
    private transient BufferedImage hpExtImg, hpIntImg;
    private transient BufferedImage energyExtImg, energyIntImg;
    private transient BufferedImage manaExtImg, manaIntImg;
    private transient BufferedImage strExtImg, strIntImg;
    private transient BufferedImage defExtImg, defIntImg;
    private transient BufferedImage hpIconImg, energyIconImg, manaIconImg, strIconImg, defIconImg;
    private transient BufferedImage mainFrameImg;
    private transient BufferedImage pauseButtonImg;
    private transient BufferedImage timerBgImg;
    private transient java.awt.Font hudFont;
    private transient boolean hudLoaded = false;

    private int pauseBtnX, pauseBtnY, pauseBtnW, pauseBtnH;

    public boolean isPauseButtonClicked(int mx, int my) {
        if (pauseButtonImg == null)
            return false;
        return mx >= pauseBtnX && mx <= pauseBtnX + pauseBtnW &&
                my >= pauseBtnY && my <= pauseBtnY + pauseBtnH;
    }

    public void triggerPauseMenu() {
        javax.swing.Action togglePauseAction = this.getActionMap().get("togglePause");
        if (togglePauseAction != null) {
            togglePauseAction.actionPerformed(
                    new java.awt.event.ActionEvent(this, java.awt.event.ActionEvent.ACTION_PERFORMED, "togglePause"));
        }
    }

    private void loadHUDAssets() {
        if (hudLoaded)
            return;
        try {
            hpExtImg = javax.imageio.ImageIO.read(new java.io.File("resources/images/HUDScreen/health_exterior.png"));
            hpIntImg = javax.imageio.ImageIO.read(new java.io.File("resources/images/HUDScreen/health_interior.png"));

            energyExtImg = javax.imageio.ImageIO
                    .read(new java.io.File("resources/images/HUDScreen/energy_exterior.png"));
            energyIntImg = javax.imageio.ImageIO
                    .read(new java.io.File("resources/images/HUDScreen/energy_interior.png"));

            manaExtImg = javax.imageio.ImageIO.read(new java.io.File("resources/images/HUDScreen/mana_exterior.png"));
            manaIntImg = javax.imageio.ImageIO.read(new java.io.File("resources/images/HUDScreen/mana_interior.png"));

            strExtImg = javax.imageio.ImageIO.read(new java.io.File("resources/images/HUDScreen/str_exterior.png"));
            strIntImg = javax.imageio.ImageIO.read(new java.io.File("resources/images/HUDScreen/str_interior.png"));

            defExtImg = javax.imageio.ImageIO.read(new java.io.File("resources/images/HUDScreen/def_exterior.png"));
            defIntImg = javax.imageio.ImageIO.read(new java.io.File("resources/images/HUDScreen/def_interior.png"));

            hpIconImg = javax.imageio.ImageIO.read(new java.io.File("resources/images/HUDScreen/health_icon.png"));
            energyIconImg = javax.imageio.ImageIO.read(new java.io.File("resources/images/HUDScreen/energy_icon.png"));
            manaIconImg = javax.imageio.ImageIO.read(new java.io.File("resources/images/HUDScreen/mana_icon.png"));
            strIconImg = javax.imageio.ImageIO.read(new java.io.File("resources/images/HUDScreen/str_icon.png"));
            defIconImg = javax.imageio.ImageIO.read(new java.io.File("resources/images/HUDScreen/def_icon.png"));

            mainFrameImg = javax.imageio.ImageIO.read(new java.io.File("resources/images/HUDScreen/main_frame.png"));

            java.io.File f = new java.io.File("resources/images/PopUpImages/PauseButton.png");
            if (!f.exists()) {
                f = new java.io.File("../resources/images/PopUpImages/PauseButton.png");
            }
            if (f.exists()) {
                pauseButtonImg = javax.imageio.ImageIO.read(f);
            }

            java.io.File timerBgFile = new java.io.File("resources/images/HUDScreen/Timer.png");
            if (!timerBgFile.exists()) {
                timerBgFile = new java.io.File("../resources/images/HUDScreen/Timer.png");
            }
            if (timerBgFile.exists()) {
                timerBgImg = javax.imageio.ImageIO.read(timerBgFile);
            }

            hudFont = vt323Font.deriveFont(java.awt.Font.PLAIN, 16f); // Kullanıcı talebi: 16 punto
        } catch (Exception e) {
            System.err.println("HUD assetleri bulunamadı!");
        }
        hudLoaded = true;
    }

    private void drawSingleBar(Graphics2D g, String label, int current, int max, BufferedImage interiorImg,
            BufferedImage exteriorImg, BufferedImage iconImg, int x, int y, int w, int h) {
        // 1. Önce Dış Çerçeveyi (Exterior) tam boyutta çiz (w, h)
        if (exteriorImg != null) {
            g.drawImage(exteriorImg, x, y, w, h, null);
        }

        // 2. Sonra İç Dolguyu (Interior) hesaplayarak çiz
        if (interiorImg != null && exteriorImg != null) {
            // Pixel-art settings
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            double ratio = Math.max(0, Math.min(1, current / (double) max));

            // Exterior'ın orijinal boyutlarına (848x244) göre ekrandaki ölçek oranını bul
            double scaleW = (double) w / exteriorImg.getWidth();
            double scaleH = (double) h / exteriorImg.getHeight();

            // Tüm interior'lar (HP dahil) 740x140 boyutunda kabul edilecek
            int targetIntW = 740;
            int targetIntH = 140;

            // Interior'ı Exterior'ın içine orijinal boşluklara göre ortala
            // Kullanıcı talebi: Çok az sola kaydır (+15'ten +10'a düşürüldü)
            int intOrigX = ((exteriorImg.getWidth() - targetIntW) / 2) + 10;
            int intOrigY = ((exteriorImg.getHeight() - targetIntH) / 2) + 14;

            // Ekrandaki (scaled) koordinatlar ve boyutlar
            int shrinkX = 1; // Genişlik birazcık artırıldı (sağdan ve soldan 1'er piksel daraltma)
            int intX = x + (int) (intOrigX * scaleW) + shrinkX;
            int intY = y + (int) (intOrigY * scaleH);
            int intW = (int) (targetIntW * scaleW) - (shrinkX * 2);
            int intH = (int) (targetIntH * scaleH);

            int visibleW = (int) (intW * ratio);

            if (visibleW > 0) {
                java.awt.Shape oldClip = g.getClip();
                // Kırpma işlemi sadece interior alanında yapılır
                g.clipRect(intX, intY, visibleW, intH);

                // Interior, hesaplanan daha küçük boyutlarla (intW, intH) TAM ÇERÇEVENİN İÇİNE
                // çizilir (ÜZERİNE)
                g.drawImage(interiorImg, intX, intY, intW, intH, null);

                g.setClip(oldClip);
            }
        }

        // Etiketi çerçevenin ÜSTÜNE ortalayarak yaz
        g.setColor(Color.WHITE);
        g.setFont(hudFont != null ? hudFont : new java.awt.Font("SansSerif", java.awt.Font.BOLD, 16));
        // Kullanıcı talebi: ENG yazısındaki sayıyı yaklaştırmak için ": " yerine ":"
        // kullanıyoruz
        String text = label + ":" + current + "/" + max;
        java.awt.FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);

        int iconSize = 20; // Yazıların yanında ufak bir kare
        int iconSpacing = 3; // Kullanıcı talebi: Yazıları ikonlara yaklaştır (6'dan 3'e düşürüldü)

        int totalContentWidth = textWidth;
        if (iconImg != null) {
            totalContentWidth += iconSize + iconSpacing;
        }

        int contentStartX = x + (w - totalContentWidth) / 2;

        // Kullanıcı talebi: HP'nin ikonunu ve yazısını birazcık daha sağa al
        if ("HP".equals(label)) {
            contentStartX -= 8;
        }

        if (iconImg != null) {
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            // Yazı baseline (alt çizgi) y-5. İkonu yazının hizasına oturtmak için yukarı
            // çekiyoruz
            int iconY = (y - 5) - iconSize + 3; // +3 piksel ufak bir estetik kaydırma
            g.drawImage(iconImg, contentStartX, iconY, iconSize, iconSize, null);
            g.drawString(text, contentStartX + iconSize + iconSpacing, y - 5);
        } else {
            g.drawString(text, contentStartX, y - 5);
        }
    }

    private void drawHUD(Graphics2D g) {
        loadHUDAssets();

        if (hpExtImg == null) {
            // Fallback
            g.setColor(Color.WHITE);
            g.drawString("HP: " + hero.getHp(), 20, 20);
            return;
        }

        // Haritanın piksel genişliği (sol ve sağ duvar arası mesafe)
        int mapPixelWidth = gameMap != null ? gameMap.getCols() * tileSize : getWidth();

        int frameW = mapPixelWidth;
        int frameH = 100; // Fallback
        int frameX = (getWidth() - frameW) / 2;
        int frameY = 0; // Oyun ekranının tam üstüyle (Y=0) çakışacak biçimde

        // 1. MAIN FRAME Çizimi (En altta kalacak arka plan)
        if (mainFrameImg != null) {
            // Main frame'i harita genişliğine (veya uygun bir orana) göre ölçekle
            frameH = (int) (mainFrameImg.getHeight() * ((double) frameW / mainFrameImg.getWidth()));

            // Pixel-art netliğini korumak için
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(mainFrameImg, frameX, frameY, frameW, frameH, null);
        } else {
            frameY = offsetY - frameH; // Fallback position above map
        }

        // 2. HUD Barları (HP, ENG, vs.)
        // Barları main frame'in İÇİNE ortalamak için hesaplama
        int numBars = 5;
        int gap = 15; // Barlar arası estetik boşluk
        int padding = 60; // Sol ve sağ duvardan boşluk (main frame içine sığması için)

        int availableWidth = frameW - (padding * 2);
        int barW = (availableWidth - (gap * (numBars - 1))) / numBars;
        // Çerçevenin orijinal en/boy oranını bozmadan yüksekliği hesapla
        int barH = (int) (hpExtImg.getHeight() * ((double) barW / hpExtImg.getWidth()));

        // 5 barın ve aralarındaki boşlukların kapladığı GERÇEK toplam genişlik
        int totalWidth = (barW * numBars) + (gap * (numBars - 1));

        // Başlangıç noktası: main frame'in içine yatayda tam merkeze oturt
        int startX = frameX + (frameW - totalWidth) / 2;

        // Y noktası: main frame'in içine dikeyde tam merkeze oturt ve 7 piksel aşağı
        // kaydır
        int y = frameY + (frameH - barH) / 2 + 7;

        // Draw PauseButton to the left of the HUD bar (immediately outside mainFrame) -
        // Much Larger
        if (pauseButtonImg != null) {
            pauseBtnH = (int) (frameH * 0.70);
            pauseBtnW = (int) (pauseButtonImg.getWidth() * ((double) pauseBtnH / pauseButtonImg.getHeight()));
            // Positioned outside mainFrame (with a 10px gap)
            pauseBtnX = frameX - pauseBtnW - 10;
            // Clamp so it doesn't go off-screen if window is narrow
            if (pauseBtnX < 5) {
                pauseBtnX = 5;
            }
            pauseBtnY = frameY + (frameH - pauseBtnH) / 2;
            g.drawImage(pauseButtonImg, pauseBtnX, pauseBtnY, pauseBtnW, pauseBtnH, null);
        }

        // 1. Health
        drawSingleBar(g, "HP", hero.getHp(), hero.getMaxHp(), hpIntImg, hpExtImg, hpIconImg, startX, y, barW, barH);

        // 2. Energy
        drawSingleBar(g, "ENG", hero.getEnergy(), 100, energyIntImg, energyExtImg, energyIconImg, startX + (barW + gap),
                y, barW, barH);

        // 3. Mana
        drawSingleBar(g, "MP", hero.getMana(), 80, manaIntImg, manaExtImg, manaIconImg, startX + 2 * (barW + gap), y,
                barW, barH);

        // 4. STR
        drawSingleBar(g, "STR", hero.getStr(), 20, strIntImg, strExtImg, strIconImg, startX + 3 * (barW + gap), y, barW,
                barH);

        // 5. DEF
        drawSingleBar(g, "DEF", hero.getDef(), 6, defIntImg, defExtImg, defIconImg, startX + 4 * (barW + gap), y, barW,
                barH);

        // 6. Timer
        long minutes = elapsedSeconds / 60;
        long secs = elapsedSeconds % 60;
        String timeStr = String.format("%02d:%02d", minutes, secs);

        if (timerBgImg != null) {
            int timerBgH = (int) (frameH * 0.70);
            int timerBgW = (int) (timerBgImg.getWidth() * ((double) timerBgH / timerBgImg.getHeight()));
            int timerBgX = frameX + frameW + 10;
            // Clamp to avoid going off screen on the right
            if (timerBgX + timerBgW > getWidth() - 5) {
                timerBgX = getWidth() - timerBgW - 5;
            }
            int timerBgY = frameY + (frameH - timerBgH) / 2;
            g.drawImage(timerBgImg, timerBgX, timerBgY, timerBgW, timerBgH, null);

            g.setColor(new Color(255, 220, 100));
            g.setFont(vt323Font.deriveFont(java.awt.Font.BOLD, 26f));
            java.awt.FontMetrics tfm = g.getFontMetrics();
            int timeStrW = tfm.stringWidth(timeStr);
            if (timeStrW > timerBgW * 0.8) {
                g.setFont(vt323Font.deriveFont(java.awt.Font.BOLD, 20f));
                tfm = g.getFontMetrics();
                timeStrW = tfm.stringWidth(timeStr);
            }
            int textX = timerBgX + (timerBgW - timeStrW) / 2;
            int textY = timerBgY + (timerBgH + tfm.getAscent()) / 2 - 3;
            g.drawString(timeStr, textX, textY);
        } else {
            // Fallback: draw plain text
            g.setColor(new Color(255, 220, 100));
            g.setFont(vt323Font.deriveFont(java.awt.Font.BOLD, 26f));
            int timeStrW = g.getFontMetrics().stringWidth(timeStr);
            int timerX = frameX + frameW + 15;
            if (timerX + timeStrW > getWidth() - 5) {
                timerX = getWidth() - timeStrW - 5;
            }
            int timerY = frameY + (frameH + g.getFontMetrics().getAscent()) / 2 - 5;
            g.drawString(timeStr, timerX, timerY);
        }
    }

    /** Delegates always-visible hotbar drawing to InventoryView. */
    private void drawInventory(Graphics2D g) {
        inventoryView.draw(g, getWidth(), getHeight());
    }

    /** Returns the hotbar item clicked at the given screen position. */
    public domain.models.entity.GameObject getClickedInventoryItem(int screenX, int screenY) {
        return inventoryView.getClickedItem(screenX, screenY);
    }

    public void scrollHotbar(int offset) {
        inventoryView.scrollSelection(offset);
        repaint();
    }

    public void setHotbarSlot(int slot) {
        inventoryView.selectSlot(slot);
        repaint();
    }

    private final java.util.Map<String, BufferedImage> weaponImageCache = new java.util.HashMap<>();

    private BufferedImage getWeaponImage(String imageName) {
        if (imageName == null)
            return null;
        if (weaponImageCache.containsKey(imageName)) {
            return weaponImageCache.get(imageName);
        }
        try {
            // Robust multi-path lookup
            String[] pathsToTry = {
                    "resources/" + imageName,
                    "../resources/" + imageName,
                    imageName,
                    "../" + imageName,
                    "src/resources/" + imageName
            };
            java.io.File f = null;
            for (String p : pathsToTry) {
                java.io.File test = new java.io.File(p);
                if (test.exists()) {
                    f = test;
                    break;
                }
            }
            if (f != null) {
                System.out.println("[DEBUG] Weapon image loaded successfully from: " + f.getPath());
                BufferedImage img = javax.imageio.ImageIO.read(f);
                weaponImageCache.put(imageName, img);
                return img;
            } else {
                System.out.println("[DEBUG] WEAPON IMAGE NOT FOUND: " + imageName);
            }
        } catch (Exception e) {
            System.err.println("[DEBUG] Error loading weapon image: " + e.getMessage());
        }
        return null;
    }

    private String lastLoggedWeapon = null;

    private void drawEquippedWeapon(Graphics2D g2d, int x, int y, Direction dir, int dw, int dh) {
        if (hero.getEquippedWeapon() == null) {
            if (lastLoggedWeapon != null) {
                System.out.println("[DEBUG] drawEquippedWeapon: Equipped weapon is now NULL");
                lastLoggedWeapon = null;
            }
            return;
        }

        String path = hero.getEquippedWeapon().getImageName();
        if (!path.equals(lastLoggedWeapon)) {
            System.out.println("[DEBUG] drawEquippedWeapon: Equipped weapon changed to: "
                    + hero.getEquippedWeapon().getName() + " (path: " + path + ")");
            lastLoggedWeapon = path;
        }

        BufferedImage weaponImg = getWeaponImage(path);
        if (weaponImg == null) {
            System.out.println("[DEBUG] drawEquippedWeapon: weaponImg is null for path: " + path);
            return;
        }

        int wSize = dw / 2; // weapon size scales with the hero's width

        // Dynamically fetch weapon metadata (or use defaults)
        double pivotX = 0.5;
        double pivotY = 0.5;
        double angleOffset = 0.0;
        int customHandX = 0;
        int customHandY = 0;

        domain.models.entity.GameObject equipped = hero.getEquippedWeapon();
        if (equipped instanceof domain.models.item.MapItem) {
            domain.models.item.MapItem item = (domain.models.item.MapItem) equipped;
            pivotX = item.getWeaponPivotX();
            pivotY = item.getWeaponPivotY();
            angleOffset = item.getWeaponAngleOffset();
            customHandX = item.getHandOffsetX();
            customHandY = item.getHandOffsetY();
        }

        java.awt.geom.AffineTransform oldTransform = g2d.getTransform();

        // 1. Translate to the center of the rendered hero frame
        g2d.translate(x + dw / 2, y + dh / 2);

        // 2. If facing LEFT, mirror the entire horizontal transformation context!
        if (dir == Direction.LEFT) {
            g2d.scale(-1, 1);
        }

        // 3. Convert absolute standard hand joint coordinates (23, 8) to
        // standard-relative ratios (based on 64px standard size)
        double ratioX = 23.0 / 64.0;
        double ratioY = 8.0 / 64.0;
        double customRatioX = (double) customHandX / 64.0;
        double customRatioY = (double) customHandY / 64.0;

        // Scale offsets dynamically with the current rendered dimensions
        int handX = (int) (ratioX * dw) + (int) (customRatioX * dw);
        int handY = (int) (ratioY * dh) + (int) (customRatioY * dh);

        g2d.translate(handX, handY);

        // 4. Calculate dynamic rotation angle (melee swing angle defaults to 45 deg, or
        // customized per weapon)
        double swingAngle = Math.toRadians(45);
        if (equipped instanceof domain.models.item.MapItem) {
            swingAngle = ((domain.models.item.MapItem) equipped).getBaseRotationAngle();
        }
        double totalAngle = swingAngle + angleOffset;
        g2d.rotate(totalAngle);

        // 5. Calculate offset based on weapon pivot points
        int px = (int) (pivotX * wSize);
        int py = (int) (pivotY * wSize);

        // 6. Draw the weapon icon such that the pivot (px, py) aligns exactly with hand
        // (0,0)
        g2d.drawImage(weaponImg, -px, -py, wSize, wSize, null);

        g2d.setTransform(oldTransform);
    }

    private final java.util.Map<BufferedImage, BufferedImage> redTintCache = new java.util.WeakHashMap<>();

    private BufferedImage getRedTintedImage(BufferedImage src) {
        if (src == null)
            return null;
        synchronized (redTintCache) {
            if (redTintCache.containsKey(src)) {
                return redTintCache.get(src);
            }
            BufferedImage tinted = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < src.getHeight(); y++) {
                for (int x = 0; x < src.getWidth(); x++) {
                    int argb = src.getRGB(x, y);
                    int a = (argb >> 24) & 0xff;
                    if (a > 0) {
                        int g = (argb >> 8) & 0xff;
                        int b = argb & 0xff;
                        int nr = 255;
                        int ng = (int) (g * 0.2);
                        int nb = (int) (b * 0.2);
                        int newArgb = (a << 24) | (nr << 16) | (ng << 8) | nb;
                        tinted.setRGB(x, y, newArgb);
                    } else {
                        tinted.setRGB(x, y, 0);
                    }
                }
            }
            redTintCache.put(src, tinted);
            return tinted;
        }
    }

    private void drawHero(Graphics2D g2d, int yVal) {
        if (hero == null || hero.getY() != yVal)
            return;
        BufferedImage frame = assetManager.getHeroSprite(hero.getAnimationState());

        if (frame != null) {
            boolean isHitFlash = (System.currentTimeMillis() - hero.getLastHitTime() < 250);
            if (isHitFlash) {
                frame = getRedTintedImage(frame);
            }
            int iw = frame.getWidth();
            int ih = frame.getHeight();
            int dw = tileSize;
            int dh = (int) (ih * ((double) tileSize / iw));
            int x = offsetX + (hero.getX() * tileSize);
            int y = offsetY + (hero.getY() * tileSize) + tileSize - dh;

            drawTeamAura(g2d, hero, x + dw / 2, y + dh - 5, tileSize);

            if (hero.getDirection() == Direction.LEFT) {
                // Resmi yatayda aynalayarak çiziyoruz
                g2d.drawImage(frame, x + dw, y, -dw, dh, null);
                drawEquippedWeapon(g2d, x, y, Direction.LEFT, dw, dh);
            } else {
                g2d.drawImage(frame, x, y, dw, dh, null);
                drawEquippedWeapon(g2d, x, y, Direction.RIGHT, dw, dh);
            }
        }
    }

    private void drawEnemies(Graphics2D g2d, int yVal) {
        if (entityList != null && !entityList.isEmpty()) {
            // entityList varsa: tüm entity'leri çiz (başlangıç + spawn edilenler)
            for (domain.models.entity.Entity e : entityList) {
                if (!e.isAlive())
                    continue; // Ölmüş entity'leri çizme
                if (e instanceof domain.models.entity.Hero)
                    continue; // Hero ayrıca çiziliyor
                if (e instanceof domain.models.entity.Projectile)
                    continue; // Projectiles are drawn separately
                if (e.getY() != yVal)
                    continue;

                BufferedImage frame = null;

                if (e instanceof domain.models.entity.Projectile) {
                    domain.models.entity.Projectile proj = (domain.models.entity.Projectile) e;

                    if ("ARROW".equalsIgnoreCase(proj.getType())) {
                        BufferedImage arrowImg = assetManager.getProjectileArrow();
                        if (arrowImg != null) {
                            double px = offsetX + (proj.getExactX() * tileSize);
                            double py = offsetY + (proj.getExactY() * tileSize);

                            // atan2 0 açısını Sağa (Right) doğru kabul eder.
                            // Ok görseli orijinalinde YUKARI (Up) bakıyor.
                            // Bu yüzden +90 derece (Math.PI / 2) offset ekliyoruz.
                            double angle = Math.atan2(proj.getDeltaY(), proj.getDeltaX()) + (Math.PI / 2.0);

                            java.awt.geom.AffineTransform old = g2d.getTransform();
                            g2d.translate(px + tileSize / 2.0, py + tileSize / 2.0);
                            g2d.rotate(angle);
                            g2d.drawImage(arrowImg, -tileSize / 2, -tileSize / 2, tileSize, tileSize, null);
                            g2d.setTransform(old);
                        } else {
                            // Fallback if arrow image fails
                            int px = (int) (offsetX + (proj.getExactX() * tileSize) + tileSize / 4);
                            int py = (int) (offsetY + (proj.getExactY() * tileSize) + tileSize / 4);
                            int size = tileSize / 2;
                            g2d.setColor(new java.awt.Color(200, 150, 100, 200));
                            g2d.fillOval(px, py, size, size);
                        }
                    } else if ("FIREBALL".equalsIgnoreCase(proj.getType())) {
                        // Blazing Fireball (Orange-Red glowing orb)
                        int px = (int) (offsetX + (proj.getExactX() * tileSize) + tileSize / 4);
                        int py = (int) (offsetY + (proj.getExactY() * tileSize) + tileSize / 4);
                        int size = tileSize / 2;
                        g2d.setColor(new java.awt.Color(255, 69, 0, 220)); // Deep orange-red
                        g2d.fillOval(px, py, size, size);
                        g2d.setColor(new java.awt.Color(255, 215, 0, 180)); // Gold aura
                        g2d.drawOval(px - 2, py - 2, size + 4, size + 4);
                    } else {
                        // Purple Mage Spell
                        int px = (int) (offsetX + (proj.getExactX() * tileSize) + tileSize / 4);
                        int py = (int) (offsetY + (proj.getExactY() * tileSize) + tileSize / 4);
                        int size = tileSize / 2;
                        g2d.setColor(new java.awt.Color(186, 85, 211, 220)); // Magical Orchid
                        g2d.fillOval(px, py, size, size);
                        g2d.setColor(new java.awt.Color(255, 200, 255, 180)); // Light aura
                        g2d.drawOval(px - 2, py - 2, size + 4, size + 4);
                    }
                    continue;
                } else if (e instanceof domain.models.entity.ShadowClone) {
                    // Klon: hero sprite'ı %50 saydamlıkla (görsel ayrım)
                    frame = assetManager.getHeroSprite(domain.models.AnimationState.IDLE);
                    if (frame != null) {
                        int iw = frame.getWidth();
                        int ih = frame.getHeight();
                        int dw = tileSize;
                        int dh = (int) (ih * ((double) tileSize / iw));
                        int dx = offsetX + (e.getX() * tileSize);
                        int dy = offsetY + (e.getY() * tileSize) + tileSize - dh;

                        java.awt.AlphaComposite ac = java.awt.AlphaComposite
                                .getInstance(java.awt.AlphaComposite.SRC_OVER, 0.5f);
                        g2d.setComposite(ac);
                        g2d.drawImage(frame, dx, dy, dw, dh, null);
                        g2d.setComposite(java.awt.AlphaComposite
                                .getInstance(java.awt.AlphaComposite.SRC_OVER, 1.0f));
                    }
                    continue;
                } else if (e instanceof domain.models.entity.FinalBoss) {
                    // FinalBoss: 2x2 sprite with HP bar
                    domain.models.entity.FinalBoss boss = (domain.models.entity.FinalBoss) e;
                    frame = assetManager.getBossSprite();
                    if (frame != null) {
                        boolean isHitFlash = (System.currentTimeMillis() - boss.getLastHitTime() < 250);
                        if (isHitFlash) {
                            frame = getRedTintedImage(frame);
                        }
                        int bossSize = tileSize * 2; // 2x2 tile footprint
                        int iw = frame.getWidth();
                        int ih = frame.getHeight();
                        double bossScale = Math.min((double) bossSize / iw, (double) bossSize / ih);
                        int dw = (int) (iw * bossScale);
                        int dh = (int) (ih * bossScale);
                        int dx = offsetX + (boss.getX() * tileSize) + (bossSize - dw) / 2;
                        int dy = offsetY + (boss.getY() * tileSize) + bossSize - dh;
                        g2d.drawImage(frame, dx, dy, dw, dh, null);

                        // HP Bar above boss
                        int barW = bossSize;
                        int barH = 6;
                        int barX = offsetX + (boss.getX() * tileSize);
                        int barY = dy - 10;
                        double hpRatio = Math.max(0, (double) boss.getHp() / 100.0);

                        // Background (dark red)
                        g2d.setColor(new Color(80, 0, 0));
                        g2d.fillRect(barX, barY, barW, barH);
                        // Foreground (gradient red → yellow based on HP)
                        int r = 255;
                        int gr = (int) (255 * hpRatio);
                        g2d.setColor(new Color(r, gr, 0));
                        g2d.fillRect(barX, barY, (int) (barW * hpRatio), barH);
                        // Border
                        g2d.setColor(Color.BLACK);
                        g2d.drawRect(barX, barY, barW, barH);
                        // HP Text
                        g2d.setColor(Color.WHITE);
                        g2d.setFont(vt323Font.deriveFont(java.awt.Font.BOLD, 11f));
                        String hpText = boss.getHp() + "/100";
                        int textW = g2d.getFontMetrics().stringWidth(hpText);
                        g2d.drawString(hpText, barX + (barW - textW) / 2, barY - 2);
                    }
                    continue;
                } else if (e instanceof domain.models.entity.Knight) {
                    frame = assetManager.getKnightSprite();
                } else if (e instanceof domain.models.entity.Sorcerer) {
                    frame = assetManager.getSorcererSprite();
                }

                if (frame != null) {
                    boolean isHitFlash = (System.currentTimeMillis() - e.getLastHitTime() < 250);
                    if (isHitFlash) {
                        frame = getRedTintedImage(frame);
                    }
                    int iw = frame.getWidth();
                    int ih = frame.getHeight();
                    int dw = tileSize;
                    int dh = (int) (ih * ((double) tileSize / iw));
                    int dx = offsetX + (e.getX() * tileSize);
                    int dy = offsetY + (e.getY() * tileSize) + tileSize - dh;

                    drawTeamAura(g2d, e, dx + dw / 2, dy + dh - 5, tileSize);

                    g2d.drawImage(frame, dx, dy, dw, dh, null);

                    // Draw thin health bar above regular enemies
                    int barW = (int) (dw * 0.8);
                    int barH = 4;
                    int barX = dx + (dw - barW) / 2;
                    int barY = dy - 6;
                    double ratio = Math.max(0.0, Math.min(1.0, (double) e.getHp() / e.getMaxHp()));

                    g2d.setColor(Color.BLACK);
                    g2d.fillRect(barX - 1, barY - 1, barW + 2, barH + 2);

                    g2d.setColor(new Color(150, 0, 0));
                    g2d.fillRect(barX, barY, barW, barH);

                    g2d.setColor(new Color(0, 220, 0));
                    g2d.fillRect(barX, barY, (int) (barW * ratio), barH);
                }
            }

        } else {
            // entityList yoksa: sadece başlangıç knight ve sorcerer'ı çiz (geriye dönük
            // uyumluluk)
            if (knight != null && knight.isAlive() && knight.getY() == yVal) {
                BufferedImage kFrame = assetManager.getKnightSprite();
                if (kFrame != null) {
                    boolean isHitFlash = (System.currentTimeMillis() - knight.getLastHitTime() < 250);
                    if (isHitFlash) {
                        kFrame = getRedTintedImage(kFrame);
                    }
                    int iw = kFrame.getWidth();
                    int ih = kFrame.getHeight();
                    int dw = tileSize;
                    int dh = (int) (ih * ((double) tileSize / iw));
                    int dx = offsetX + (knight.getX() * tileSize);
                    int dy = offsetY + (knight.getY() * tileSize) + tileSize - dh;

                    drawTeamAura(g2d, knight, dx + dw / 2, dy + dh - 5, tileSize);

                    g2d.drawImage(kFrame, dx, dy, dw, dh, null);

                    // Draw thin health bar
                    int barW = (int) (dw * 0.8);
                    int barH = 4;
                    int barX = dx + (dw - barW) / 2;
                    int barY = dy - 6;
                    double ratio = Math.max(0.0, Math.min(1.0, (double) knight.getHp() / knight.getMaxHp()));

                    g2d.setColor(Color.BLACK);
                    g2d.fillRect(barX - 1, barY - 1, barW + 2, barH + 2);

                    g2d.setColor(new Color(150, 0, 0));
                    g2d.fillRect(barX, barY, barW, barH);

                    g2d.setColor(new Color(0, 220, 0));
                    g2d.fillRect(barX, barY, (int) (barW * ratio), barH);
                }
            }
            if (sorcerer != null && sorcerer.isAlive() && sorcerer.getY() == yVal) {
                BufferedImage sFrame = assetManager.getSorcererSprite();
                if (sFrame != null) {
                    boolean isHitFlash = (System.currentTimeMillis() - sorcerer.getLastHitTime() < 250);
                    if (isHitFlash) {
                        sFrame = getRedTintedImage(sFrame);
                    }
                    int iw = sFrame.getWidth();
                    int ih = sFrame.getHeight();
                    int dw = tileSize;
                    int dh = (int) (ih * ((double) tileSize / iw));
                    int dx = offsetX + (sorcerer.getX() * tileSize);
                    int dy = offsetY + (sorcerer.getY() * tileSize) + tileSize - dh;

                    drawTeamAura(g2d, sorcerer, dx + dw / 2, dy + dh - 5, tileSize);

                    g2d.drawImage(sFrame, dx, dy, dw, dh, null);

                    // Draw thin health bar
                    int barW = (int) (dw * 0.8);
                    int barH = 4;
                    int barX = dx + (dw - barW) / 2;
                    int barY = dy - 6;
                    double ratio = Math.max(0.0, Math.min(1.0, (double) sorcerer.getHp() / sorcerer.getMaxHp()));

                    g2d.setColor(Color.BLACK);
                    g2d.fillRect(barX - 1, barY - 1, barW + 2, barH + 2);

                    g2d.setColor(new Color(150, 0, 0));
                    g2d.fillRect(barX, barY, barW, barH);

                    g2d.setColor(new Color(0, 220, 0));
                    g2d.fillRect(barX, barY, (int) (barW * ratio), barH);
                }
            }
        }
    }

    private void drawProjectiles(Graphics2D g2d) {
        if (entityList == null)
            return;
        for (domain.models.entity.Entity e : entityList) {
            if (e.isAlive() && e instanceof domain.models.entity.Projectile) {
                domain.models.entity.Projectile proj = (domain.models.entity.Projectile) e;

                if ("ARROW".equalsIgnoreCase(proj.getType())) {
                    BufferedImage arrowImg = assetManager.getProjectileArrow();
                    if (arrowImg != null) {
                        double px = offsetX + (proj.getExactX() * tileSize);
                        double py = offsetY + (proj.getExactY() * tileSize);

                        // atan2 0 açısını Sağa (Right) doğru kabul eder.
                        // Ok görseli orijinalinde YUKARI (Up) bakıyor.
                        // Bu yüzden +90 derece (Math.PI / 2) offset ekliyoruz.
                        double angle = Math.atan2(proj.getDeltaY(), proj.getDeltaX()) + (Math.PI / 2.0);

                        java.awt.geom.AffineTransform old = g2d.getTransform();
                        g2d.translate(px + tileSize / 2.0, py + tileSize / 2.0);
                        g2d.rotate(angle);
                        g2d.drawImage(arrowImg, -tileSize / 2, -tileSize / 2, tileSize, tileSize, null);
                        g2d.setTransform(old);
                    } else {
                        // Fallback if arrow image fails
                        int px = (int) (offsetX + (proj.getExactX() * tileSize) + tileSize / 4);
                        int py = (int) (offsetY + (proj.getExactY() * tileSize) + tileSize / 4);
                        int size = tileSize / 2;
                        g2d.setColor(new java.awt.Color(200, 150, 100, 200));
                        g2d.fillOval(px, py, size, size);
                    }
                } else if ("FIREBALL".equalsIgnoreCase(proj.getType())) {
                    // Blazing Fireball (Orange-Red glowing orb)
                    int px = (int) (offsetX + (proj.getExactX() * tileSize) + tileSize / 4);
                    int py = (int) (offsetY + (proj.getExactY() * tileSize) + tileSize / 4);
                    int size = tileSize / 2;
                    g2d.setColor(new java.awt.Color(255, 69, 0, 220)); // Deep orange-red
                    g2d.fillOval(px, py, size, size);
                    g2d.setColor(new java.awt.Color(255, 215, 0, 180)); // Gold aura
                    g2d.drawOval(px - 2, py - 2, size + 4, size + 4);
                } else if ("BOSS_FIREBALL".equalsIgnoreCase(proj.getType())) {
                    // Boss Fireball — larger, darker, more menacing
                    int px = (int) (offsetX + (proj.getExactX() * tileSize) + tileSize / 6);
                    int py = (int) (offsetY + (proj.getExactY() * tileSize) + tileSize / 6);
                    int size = (int) (tileSize * 0.67);
                    g2d.setColor(new java.awt.Color(200, 0, 0, 230)); // Crimson core
                    g2d.fillOval(px, py, size, size);
                    g2d.setColor(new java.awt.Color(255, 50, 0, 180)); // Red aura
                    g2d.drawOval(px - 3, py - 3, size + 6, size + 6);
                    g2d.setColor(new java.awt.Color(255, 150, 0, 120)); // Outer glow
                    g2d.drawOval(px - 5, py - 5, size + 10, size + 10);
                } else {
                    // Purple Mage Spell
                    int px = (int) (offsetX + (proj.getExactX() * tileSize) + tileSize / 4);
                    int py = (int) (offsetY + (proj.getExactY() * tileSize) + tileSize / 4);
                    int size = tileSize / 2;
                    g2d.setColor(new java.awt.Color(186, 85, 211, 220)); // Magical Orchid
                    g2d.fillOval(px, py, size, size);
                    g2d.setColor(new java.awt.Color(255, 200, 255, 180)); // Light aura
                    g2d.drawOval(px - 2, py - 2, size + 4, size + 4);
                }
            }
        }
    }

    private void drawTeamAura(Graphics2D g2d, domain.models.entity.Entity entity, int cx, int cy, int size) {
        if (gameMode != domain.models.GameMode.TEAM_MATCH)
            return;
        domain.models.Team team = entity.getTeam();
        if (team == domain.models.Team.NONE || team == null)
            return;

        Color centerColor = (team == domain.models.Team.CYAN) ? new Color(0, 255, 255, 150)
                : new Color(255, 140, 0, 150);
        Color edgeColor = new Color(0, 0, 0, 0);

        int radius = size / 2 + 15;

        java.awt.geom.Point2D center = new java.awt.geom.Point2D.Float(cx, cy);
        float[] dist = { 0.0f, 1.0f };
        Color[] colors = { centerColor, edgeColor };

        java.awt.RadialGradientPaint p = new java.awt.RadialGradientPaint(center, radius, dist, colors);
        java.awt.Paint oldPaint = g2d.getPaint();
        g2d.setPaint(p);

        g2d.fillOval(cx - radius, cy - radius / 2, radius * 2, radius);
        g2d.setPaint(oldPaint);
    }

    private boolean isSameWall(domain.models.map.GameMap map, int x, int y, String imgName) {
        if (x < 0 || x >= map.getWidth() || y < 0 || y >= map.getHeight())
            return false;
        domain.models.entity.GameObject obj = map.getObjectAt(x, y);
        return obj instanceof domain.models.tile.WallTile && imgName != null && imgName.equals(obj.getImageName());
    }

    private void drawMap(Graphics2D g2d) {
        if (gameMap == null || tileManager == null)
            return;

        for (int x = 0; x < gameMap.getWidth(); x++) {
            for (int y = 0; y < gameMap.getHeight(); y++) {
                domain.models.entity.GameObject obj = gameMap.getObjectAt(x, y);
                if (obj != null) {
                    if (obj instanceof domain.models.item.MapItem ||
                            obj instanceof domain.models.entity.Column ||
                            obj instanceof domain.models.entity.Chest ||
                            obj instanceof domain.models.entity.Crate ||
                            (obj instanceof domain.models.staticObjects.Door
                                    && !(obj instanceof domain.models.staticObjects.LevelDoor))
                            ||
                            obj instanceof domain.models.staticObjects.Decoration ||
                            obj instanceof domain.models.entity.SearchableObject ||
                            obj instanceof domain.models.entity.Sign ||
                            obj instanceof domain.models.tile.FloorTile) {
                        // Eğer hücrede bir eşya, statik obje veya zemin varsa zemin (FloorTile)
                        // çiziyoruz
                        BufferedImage floor = tileManager.getTile("floor", x, y);
                        if (floor != null) {
                            g2d.drawImage(floor, offsetX + (x * tileSize), offsetY + (y * tileSize), tileSize, tileSize,
                                    null);
                        }
                    }
                }
            }
        }
    }

    private void drawWalls(Graphics2D g2d, int yVal) {
        if (gameMap == null || tileManager == null)
            return;

        // PASS 1: Draw all base walls for the current row (yVal)
        for (int x = 0; x < gameMap.getWidth(); x++) {
            domain.models.entity.GameObject obj = gameMap.getObjectAt(x, yVal);
            if (obj instanceof domain.models.tile.WallTile) {
                if ("wall/wall_side".equals(obj.getImageName())) {
                    // Yan duvarlar: ince çiz (tile genişliğinin 1/3'ü)
                    BufferedImage tileImage = tileManager.getTile(obj.getImageName());
                    if (tileImage != null) {
                        int sideWidth = Math.max(tileSize / 3, 4) + 6; // 6 pixels wider
                        int drawX;
                        if (x == 0) {
                            // Sol duvar: hücrenin sağ kenarında
                            drawX = offsetX + (x * tileSize) + tileSize - sideWidth;
                        } else {
                            // Sağ duvar: hücrenin sol kenarında
                            drawX = offsetX + (x * tileSize);
                        }
                        g2d.drawImage(tileImage, drawX, offsetY + (yVal * tileSize) - 6,
                                sideWidth, tileSize + 6, null); // 6 pixels taller
                    }

                    domain.models.tile.WallTile wall = (domain.models.tile.WallTile) obj;
                    if (wall.getDecoration() != null) {
                        domain.models.entity.GameObject deco = wall.getDecoration();
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
                            int dw = tileSize;
                            if (deco instanceof domain.models.staticObjects.Decoration) {
                                dw = (int) (tileSize * 0.4);
                            } else if (deco instanceof domain.models.staticObjects.WallObject
                                    || deco instanceof domain.models.entity.SearchableObject) {
                                dw = Math.max(tileSize - 6, 4);
                            }
                            int dh = (int) (ih * ((double) dw / iw));
                            int drawX = offsetX + (x * tileSize) + (tileSize - dw) / 2;
                            int drawY;
                            if (deco instanceof domain.models.staticObjects.WallObject
                                    || deco instanceof domain.models.entity.SearchableObject) {
                                drawY = offsetY + (yVal * tileSize) - 3 + (tileSize - dh) / 2; // wallOffset=6
                            } else {
                                drawY = offsetY + (yVal * tileSize) + tileSize - dh;
                            }
                            g2d.drawImage(decoImg, drawX, drawY, dw, dh, null);
                        }
                    }
                } else if ("wall/wall_1".equals(obj.getImageName())) {
                    BufferedImage tileImage = tileManager.getTile(obj.getImageName());
                    if (tileImage != null) {
                        int dh = (int) (tileSize * 1.5);
                        int drawY = offsetY + (yVal * tileSize) + tileSize - dh;
                        g2d.drawImage(tileImage, offsetX + (x * tileSize), drawY, tileSize, dh, null);
                    }
                } else if ("wall/wall_2".equals(obj.getImageName())) {
                    BufferedImage tileImage = tileManager.getTile(obj.getImageName());
                    if (tileImage != null) {
                        int dh = (int) (tileSize * 1.5);
                        int drawY = offsetY + (yVal * tileSize) + tileSize - dh;
                        g2d.drawImage(tileImage, offsetX + (x * tileSize), drawY, tileSize, dh, null);
                    }
                } else if (yVal == 0 && obj instanceof domain.models.staticObjects.LevelDoor) {
                    BufferedImage tileImage = tileManager.getTile("wall/wall_1");
                    if (tileImage != null) {
                        int dh = (int) (tileSize * 1.5);
                        int drawY = offsetY + (yVal * tileSize) + tileSize - dh;
                        g2d.drawImage(tileImage, offsetX + (x * tileSize), drawY, tileSize, dh, null);
                    }
                } else {
                    // Normal harita objesi (Duvar vs.)
                    BufferedImage tileImage = tileManager.getTile(obj.getImageName());
                    if (tileImage != null) {
                        int offset = (obj instanceof domain.models.tile.WallTile) ? 6 : 0;
                        if (offset > 0) {
                            g2d.drawImage(tileImage, offsetX + (x * tileSize), offsetY + (yVal * tileSize) - offset,
                                    tileSize,
                                    tileSize + offset, null);
                        } else {
                            g2d.drawImage(tileImage, offsetX + (x * tileSize), offsetY + (yVal * tileSize), tileSize,
                                    tileSize, null);
                        }
                    }
                }
            }
        }

        // PASS 2: Draw all wall-mounted decorations on top of all base walls and floor
        // tiles for this row (yVal)
        for (int x = 0; x < gameMap.getWidth(); x++) {
            domain.models.entity.GameObject obj = gameMap.getObjectAt(x, yVal);
            if (obj instanceof domain.models.tile.WallTile) {
                domain.models.tile.WallTile wall = (domain.models.tile.WallTile) obj;
                if (wall.getDecoration() != null) {
                    domain.models.entity.GameObject deco = wall.getDecoration();
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

                        int[] dims = getDecorDimensions(deco.getImageName(), deco.getCustomScale(), tileSize, iw, ih,
                                deco.getName());
                        int dw = dims[0];
                        int dh = dims[1];
                        int drawX = offsetX + (x * tileSize) + (tileSize - dw) / 2;
                        int drawY;
                        if (deco instanceof domain.models.staticObjects.WallObject
                                || deco instanceof domain.models.entity.SearchableObject) {
                            int wallOffset = "wall/wall_1".equals(obj.getImageName()) ? 8 : 6;
                            drawY = offsetY + (yVal * tileSize) - wallOffset / 2 + (tileSize - dh) / 2;
                        } else {
                            drawY = offsetY + (yVal * tileSize) + tileSize - dh;
                        }
                        g2d.drawImage(decoImg, drawX, drawY, dw, dh, null);
                    }
                }
            }
        }
    }

    private void drawInteractionHighlight(Graphics2D g2d) {
        if (hero == null || gameMap == null)
            return;

        int heroGridX = hero.getX();
        int heroGridY = hero.getY();

        for (int x = 0; x < gameMap.getWidth(); x++) {
            for (int y = 0; y < gameMap.getHeight(); y++) {
                // Check if this tile is outside the 3x3 zone
                // Sadece etrafındaki 3x3 kareyi gölgelendir
                boolean inZone = Math.abs(x - heroGridX) <= 1
                        && Math.abs(y - heroGridY) <= 1;

                if (inZone) { // dim tiles INSIDE the zone
                    domain.models.entity.GameObject obj = gameMap.getObjectAt(x, y);
                    // Eğer hücredeki obje 'wall' ise (geçilemezse) üzerine karanlık efekti çizme
                    if (obj != null && obj.isPassable()) {
                        g2d.setColor(new Color(0, 0, 0, 60));
                        g2d.fillRect(offsetX + (x * tileSize), offsetY + (y * tileSize), tileSize, tileSize);
                    }
                }
            }
        }
    }

    private void drawItems(Graphics2D g2d, int yVal) {
        if (gameMap == null)
            return;

        for (int x = 0; x < gameMap.getWidth(); x++) {
            domain.models.entity.GameObject obj = gameMap.getObjectAt(x, yVal);
            if (obj instanceof domain.models.item.MapItem) {
                domain.models.item.MapItem item = (domain.models.item.MapItem) obj;

                boolean inZone = hero != null && Math.abs(x - hero.getX()) <= 1
                        && Math.abs(yVal - hero.getY()) <= 1;

                if (!inZone) {
                    java.awt.AlphaComposite ac = java.awt.AlphaComposite
                            .getInstance(java.awt.AlphaComposite.SRC_OVER, 0.4f);
                    g2d.setComposite(ac);
                }

                if (item.getSprite() != null) {
                    double scaleMult = 1.30;
                    if (item instanceof domain.models.item.usables.PotionItem || item instanceof domain.models.item.wearables.RingItem) {
                        scaleMult *= 0.7; // Potions & rings render 30% smaller
                    }
                    int maxDim = (int) (tileSize * scaleMult);
                    int iw = item.getSprite().getWidth();
                    int ih = item.getSprite().getHeight();
                    double scale = Math.min((double) maxDim / iw, (double) maxDim / ih);
                    int dw = (int) (iw * scale);
                    int dh = (int) (ih * scale);
                    int drawX = offsetX + (x * tileSize) + (tileSize - dw) / 2;
                    int drawY = offsetY + (yVal * tileSize) + (tileSize - dh) / 2;
                    g2d.drawImage(item.getSprite(), drawX, drawY, dw, dh, null);
                } else {
                    // Sprite yok — renk placeholder (scroll için mor, diğerleri sarı)
                    int spriteSize = (int) (tileSize * 1.30);
                    int posOffset = (tileSize - spriteSize) / 2;
                    int drawX = offsetX + (x * tileSize) + posOffset;
                    int drawY = offsetY + (yVal * tileSize) + posOffset;
                    if (item instanceof domain.models.item.usables.ShadowCloneScroll) {
                        g2d.setColor(new Color(150, 50, 255)); // Mor — scroll
                    } else {
                        g2d.setColor(new Color(255, 220, 50)); // Sarı — bilinmeyen item
                    }
                    g2d.fillOval(drawX, drawY, spriteSize, spriteSize);
                }

                // Composite sıfırla
                g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1.0f));
            }
        }
    }

    private void drawStaticObjects(Graphics2D g2d, int yVal) {
        if (gameMap == null)
            return;

        for (int x = 0; x < gameMap.getWidth(); x++) {
            domain.models.entity.GameObject obj = gameMap.getObjectAt(x, yVal);
            if (obj instanceof domain.models.entity.Column ||
                    obj instanceof domain.models.entity.Chest ||
                    obj instanceof domain.models.entity.Crate ||
                    obj instanceof domain.models.staticObjects.Door ||
                    obj instanceof domain.models.staticObjects.Decoration ||
                    obj instanceof domain.models.entity.SearchableObject ||
                    obj instanceof domain.models.entity.Sign) {

                if (obj instanceof domain.models.staticObjects.LevelDoor) {
                    int tx = offsetX + (x * tileSize);
                    int ty = offsetY + (yVal * tileSize);
                    java.awt.Paint oldPaint = g2d.getPaint();
                    java.awt.Stroke oldStroke = g2d.getStroke();

                    g2d.setColor(new Color(0, 255, 200, 30));
                    g2d.fillOval(tx + 6, ty + 6, tileSize - 12, tileSize - 12);

                    g2d.setStroke(new java.awt.BasicStroke(3f));
                    g2d.setColor(new Color(0, 255, 200, 180));
                    g2d.drawOval(tx + 6, ty + 6, tileSize - 12, tileSize - 12);

                    g2d.setColor(new Color(255, 255, 255, 200));
                    g2d.setFont(vt323Font.deriveFont(java.awt.Font.BOLD, 11f));
                    String markerText = "LVL";
                    int textW = g2d.getFontMetrics().stringWidth(markerText);
                    g2d.drawString(markerText, tx + (tileSize - textW) / 2, ty + tileSize / 2 + 4);

                    g2d.setStroke(oldStroke);
                    g2d.setPaint(oldPaint);
                }

                // Kullanıcı talebi: Obstacle'lar yarı saydam değil opak olmalı
                // (AlphaComposite kaldırıldı)

                BufferedImage sprite = null;
                if (tileManager != null) {
                    String imgName = obj.getImageName();
                    if (obj instanceof domain.models.staticObjects.Decoration && imgName != null
                            && imgName.startsWith("torch/")) {
                        long now = System.currentTimeMillis();
                        int[] frames = { 1, 2, 3, 4, 6, 7, 8 };
                        int frame = frames[(int) ((now / 120) % frames.length)];
                        imgName = "torch/torch_" + frame;
                    }
                    sprite = tileManager.getTile(imgName);
                }

                if (sprite != null) {
                    int iw = sprite.getWidth();
                    int ih = sprite.getHeight();
                    if (iw == 447 && ih == 558) {
                        iw = 31;
                        ih = 64;
                    }
                    int dw = tileSize;
                    int dh = tileSize;
                    if (obj instanceof domain.models.staticObjects.Door) {
                        dw = tileSize * 2;
                        dh = (int) (ih * ((double) dw / iw));
                    } else {
                        int[] dims = getDecorDimensions(obj.getImageName(), obj.getCustomScale(), tileSize, iw, ih,
                                obj.getName());
                        dw = dims[0];
                        dh = dims[1];
                    }
                    int drawX = offsetX + (x * tileSize) + (tileSize - dw) / 2;
                    int drawY = offsetY + (yVal * tileSize) + tileSize - dh; // Bottom aligned!
                    g2d.drawImage(sprite, drawX, drawY, dw, dh, null);
                } else {
                    // Placeholder fallback
                    int drawX = offsetX + (x * tileSize);
                    int drawY = offsetY + (yVal * tileSize);
                    Color color = Color.MAGENTA;
                    if (obj instanceof domain.models.entity.Column)
                        color = Color.GRAY;
                    else if (obj instanceof domain.models.entity.Chest)
                        color = new Color(139, 69, 19);
                    else if (obj instanceof domain.models.entity.Crate)
                        color = new Color(101, 67, 33);
                    else if (obj instanceof domain.models.entity.SearchableObject)
                        color = Color.DARK_GRAY;
                    else if (obj instanceof domain.models.entity.Sign)
                        color = new Color(180, 115, 60);

                    g2d.setColor(color);
                    g2d.fillRect(drawX, drawY, tileSize, tileSize);
                }
            }
        }
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

    public void toggleInventory() {
        // Hotbar is always visible; keep this as a no-op for compatibility.
        repaint();
    }

    public boolean isInventoryVisible() {
        return true;
    }
}