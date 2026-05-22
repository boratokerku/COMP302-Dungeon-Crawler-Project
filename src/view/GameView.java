
package view;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;

import domain.models.entity.Hero;
import domain.models.Direction;

public class GameView extends JPanel {
    private Hero hero;
    private AssetManager assetManager;

    // Inventory rendering is fully delegated to InventoryView
    private InventoryView inventoryView;
    private boolean inventoryVisible = false;

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
    private java.util.List<domain.models.entity.Entity> entityList;
    private domain.models.GameMode gameMode = domain.models.GameMode.ADVENTURE;

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

            int usableWidth = (int) (getWidth() * 0.90);
            int usableHeight = (int) ((getHeight() - hudReserve) * 0.90);

            int tileW = usableWidth / gameMap.getWidth();
            int tileH = usableHeight / gameMap.getHeight();
            tileSize = Math.min(tileW, tileH);

            // Haritayı yatayda ortala, dikeyde HUD'un altına yerleştir
            offsetX = (getWidth() - (tileSize * gameMap.getWidth())) / 2;
            offsetY = hudReserve + (getHeight() - hudReserve - (tileSize * gameMap.getHeight())) / 2;
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

        // Her çizim döngüsünde ekranın boyutuna göre karelerin (Tile) büyüklüğünü
        // hesapla
        calculateDimensions();

        // 0. Arka planı tile ile döşe (düz renk yerine)
        drawBackground(g2d);

        // 1. Zemin veya Haritayı Çiz
        drawMap(g2d);

        // 1.5 Etkileşim Alanı (3x3) Vurgusu
        drawInteractionHighlight(g2d);

        // 1.6 Statik Objeleri Çiz
        drawStaticObjects(g2d);

        // 1.7 Haritadaki Eşyaları Çiz
        drawItems(g2d);

        // 2. Hero'yu Çiz
        drawHero(g2d);

        // 3. Düşmanları Çiz
        drawEnemies(g2d);

        // 4. HUD (Arayüz) Çiz
        drawHUD(g2d);

        // 5. Inventory Çiz
        drawInventory(g2d);

        // 5.5 Süzülen Hasar Efektlerini Çiz (Floating Texts)
        drawFloatingTexts(g2d);

        // Kaynakları temizle
        g2d.dispose();
    }

    private void drawFloatingTexts(Graphics2D g2d) {
        synchronized (floatingTexts) {
            java.util.Iterator<FloatingText> it = floatingTexts.iterator();
            while (it.hasNext()) {
                FloatingText ft = it.next();
                
                // Fade out effect using alpha channel
                int alphaVal = (int) (ft.alpha * 255);
                if (alphaVal < 0) alphaVal = 0;
                if (alphaVal > 255) alphaVal = 255;
                
                g2d.setColor(new Color(ft.color.getRed(), ft.color.getGreen(), ft.color.getBlue(), alphaVal));
                g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 22));
                
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
    private transient java.awt.Font hudFont;
    private transient boolean hudLoaded = false;

    private void loadHUDAssets() {
        if (hudLoaded) return;
        try {
            hpExtImg = javax.imageio.ImageIO.read(new java.io.File("resources/images/HUDScreen/health_exterior.png"));
            hpIntImg = javax.imageio.ImageIO.read(new java.io.File("resources/images/HUDScreen/health_interior.png"));
            
            energyExtImg = javax.imageio.ImageIO.read(new java.io.File("resources/images/HUDScreen/energy_exterior.png"));
            energyIntImg = javax.imageio.ImageIO.read(new java.io.File("resources/images/HUDScreen/energy_interior.png"));
            
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
            
            try {
                java.awt.Font customFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, new java.io.File("resources/fonts/VT323-Regular.ttf"));
                java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(customFont);
                hudFont = customFont.deriveFont(java.awt.Font.PLAIN, 16f); // Kullanıcı talebi: 16 punto
            } catch (Exception fe) {
                System.err.println("VT323 fontu yüklenemedi: " + fe.getMessage());
                hudFont = new java.awt.Font("SansSerif", java.awt.Font.BOLD, 18);
            }
        } catch (Exception e) {
            System.err.println("HUD assetleri bulunamadı!");
        }
        hudLoaded = true;
    }

    private void drawSingleBar(Graphics2D g, String label, int current, int max, BufferedImage interiorImg, BufferedImage exteriorImg, BufferedImage iconImg, int x, int y, int w, int h) {
        // 1. Önce Dış Çerçeveyi (Exterior) tam boyutta çiz (w, h)
        if (exteriorImg != null) {
            g.drawImage(exteriorImg, x, y, w, h, null);
        }

        // 2. Sonra İç Dolguyu (Interior) hesaplayarak çiz
        if (interiorImg != null && exteriorImg != null) {
            // Pixel-art settings
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

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
                
                // Interior, hesaplanan daha küçük boyutlarla (intW, intH) TAM ÇERÇEVENİN İÇİNE çizilir (ÜZERİNE)
                g.drawImage(interiorImg, intX, intY, intW, intH, null);
                
                g.setClip(oldClip);
            }
        }
        
        // Etiketi çerçevenin ÜSTÜNE ortalayarak yaz
        g.setColor(Color.WHITE);
        g.setFont(hudFont != null ? hudFont : new java.awt.Font("SansSerif", java.awt.Font.BOLD, 16));
        // Kullanıcı talebi: ENG yazısındaki sayıyı yaklaştırmak için ": " yerine ":" kullanıyoruz
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
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            // Yazı baseline (alt çizgi) y-5. İkonu yazının hizasına oturtmak için yukarı çekiyoruz
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
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
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
        
        // Y noktası: main frame'in içine dikeyde tam merkeze oturt ve 7 piksel aşağı kaydır
        int y = frameY + (frameH - barH) / 2 + 7;
        
        // 1. Health
        drawSingleBar(g, "HP", hero.getHp(), 17, hpIntImg, hpExtImg, hpIconImg, startX, y, barW, barH);
        
        // 2. Energy
        drawSingleBar(g, "ENG", hero.getEnergy(), 100, energyIntImg, energyExtImg, energyIconImg, startX + (barW + gap), y, barW, barH);
        
        // 3. Mana
        drawSingleBar(g, "MP", hero.getMana(), 80, manaIntImg, manaExtImg, manaIconImg, startX + 2*(barW + gap), y, barW, barH);
        
        // 4. STR
        drawSingleBar(g, "STR", hero.getStr(), 20, strIntImg, strExtImg, strIconImg, startX + 3*(barW + gap), y, barW, barH);
        
        // 5. DEF
        drawSingleBar(g, "DEF", hero.getDef(), 6, defIntImg, defExtImg, defIconImg, startX + 4*(barW + gap), y, barW, barH);
    }

    /** Delegates inventory drawing to InventoryView. */
    private void drawInventory(Graphics2D g) {
        if (!inventoryVisible)
            return;
        inventoryView.draw(g, getWidth(), getHeight());
    }

    /** Returns the inventory item clicked at the given screen position. */
    public domain.models.entity.GameObject getClickedInventoryItem(int screenX, int screenY) {
        if (!inventoryVisible)
            return null;
        return inventoryView.getClickedItem(screenX, screenY);
    }

    private final java.util.Map<String, BufferedImage> weaponImageCache = new java.util.HashMap<>();

    private BufferedImage getWeaponImage(String imageName) {
        if (imageName == null) return null;
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
            System.out.println("[DEBUG] drawEquippedWeapon: Equipped weapon changed to: " + hero.getEquippedWeapon().getName() + " (path: " + path + ")");
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
        
        // 3. Convert absolute standard hand joint coordinates (23, 8) to standard-relative ratios (based on 64px standard size)
        double ratioX = 23.0 / 64.0;
        double ratioY = 8.0 / 64.0;
        double customRatioX = (double) customHandX / 64.0;
        double customRatioY = (double) customHandY / 64.0;
        
        // Scale offsets dynamically with the current rendered dimensions
        int handX = (int) (ratioX * dw) + (int) (customRatioX * dw);
        int handY = (int) (ratioY * dh) + (int) (customRatioY * dh);
        
        g2d.translate(handX, handY);
        
        // 4. Calculate dynamic rotation angle (melee swing angle defaults to 45 deg, or customized per weapon)
        double swingAngle = Math.toRadians(45);
        if (equipped instanceof domain.models.item.MapItem) {
            swingAngle = ((domain.models.item.MapItem) equipped).getBaseRotationAngle();
        }
        double totalAngle = swingAngle + angleOffset;
        g2d.rotate(totalAngle);
        
        // 5. Calculate offset based on weapon pivot points
        int px = (int) (pivotX * wSize);
        int py = (int) (pivotY * wSize);
        
        // 6. Draw the weapon icon such that the pivot (px, py) aligns exactly with hand (0,0)
        g2d.drawImage(weaponImg, -px, -py, wSize, wSize, null);
        
        g2d.setTransform(oldTransform);
    }

    private void drawHero(Graphics2D g2d) {
        BufferedImage frame = assetManager.getHeroSprite(hero.getAnimationState());

        if (frame != null) {
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

    private void drawEnemies(Graphics2D g2d) {
        if (entityList != null && !entityList.isEmpty()) {
            // entityList varsa: tüm entity'leri çiz (başlangıç + spawn edilenler)
            for (domain.models.entity.Entity e : entityList) {
                if (!e.isAlive())
                    continue; // Ölmüş entity'leri çizme
                if (e instanceof domain.models.entity.Hero)
                    continue; // Hero ayrıca çiziliyor

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
                            int px = (int)(offsetX + (proj.getExactX() * tileSize) + tileSize / 4);
                            int py = (int)(offsetY + (proj.getExactY() * tileSize) + tileSize / 4);
                            int size = tileSize / 2;
                            g2d.setColor(new java.awt.Color(200, 150, 100, 200));
                            g2d.fillOval(px, py, size, size);
                        }
                    } else if ("FIREBALL".equalsIgnoreCase(proj.getType())) {
                        // Blazing Fireball (Orange-Red glowing orb)
                        int px = (int)(offsetX + (proj.getExactX() * tileSize) + tileSize / 4);
                        int py = (int)(offsetY + (proj.getExactY() * tileSize) + tileSize / 4);
                        int size = tileSize / 2;
                        g2d.setColor(new java.awt.Color(255, 69, 0, 220)); // Deep orange-red
                        g2d.fillOval(px, py, size, size);
                        g2d.setColor(new java.awt.Color(255, 215, 0, 180)); // Gold aura
                        g2d.drawOval(px - 2, py - 2, size + 4, size + 4);
                    } else {
                        // Purple Mage Spell
                        int px = (int)(offsetX + (proj.getExactX() * tileSize) + tileSize / 4);
                        int py = (int)(offsetY + (proj.getExactY() * tileSize) + tileSize / 4);
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
                } else if (e instanceof domain.models.entity.Knight) {
                    frame = assetManager.getKnightSprite();
                } else if (e instanceof domain.models.entity.Sorcerer) {
                    frame = assetManager.getSorcererSprite();
                }

                if (frame != null) {
                    int iw = frame.getWidth();
                    int ih = frame.getHeight();
                    int dw = tileSize;
                    int dh = (int) (ih * ((double) tileSize / iw));
                    int dx = offsetX + (e.getX() * tileSize);
                    int dy = offsetY + (e.getY() * tileSize) + tileSize - dh;
                    
                    drawTeamAura(g2d, e, dx + dw / 2, dy + dh - 5, tileSize);
                    
                    g2d.drawImage(frame, dx, dy, dw, dh, null);
                }
            }

        } else {
            // entityList yoksa: sadece başlangıç knight ve sorcerer'ı çiz (geriye dönük
            // uyumluluk)
            if (knight != null && knight.isAlive()) {
                BufferedImage kFrame = assetManager.getKnightSprite();
                if (kFrame != null) {
                    int iw = kFrame.getWidth();
                    int ih = kFrame.getHeight();
                    int dw = tileSize;
                    int dh = (int) (ih * ((double) tileSize / iw));
                    int dx = offsetX + (knight.getX() * tileSize);
                    int dy = offsetY + (knight.getY() * tileSize) + tileSize - dh;
                    
                    drawTeamAura(g2d, knight, dx + dw / 2, dy + dh - 5, tileSize);
                    
                    g2d.drawImage(kFrame, dx, dy, dw, dh, null);
                }
            }
            if (sorcerer != null && sorcerer.isAlive()) {
                BufferedImage sFrame = assetManager.getSorcererSprite();
                if (sFrame != null) {
                    int iw = sFrame.getWidth();
                    int ih = sFrame.getHeight();
                    int dw = tileSize;
                    int dh = (int) (ih * ((double) tileSize / iw));
                    int dx = offsetX + (sorcerer.getX() * tileSize);
                    int dy = offsetY + (sorcerer.getY() * tileSize) + tileSize - dh;
                    
                    drawTeamAura(g2d, sorcerer, dx + dw / 2, dy + dh - 5, tileSize);
                    
                    g2d.drawImage(sFrame, dx, dy, dw, dh, null);
                }
            }
        }
    }

    private void drawTeamAura(Graphics2D g2d, domain.models.entity.Entity entity, int cx, int cy, int size) {
        if (gameMode != domain.models.GameMode.TEAM_MATCH) return;
        domain.models.Team team = entity.getTeam();
        if (team == domain.models.Team.NONE || team == null) return;
        
        Color centerColor = (team == domain.models.Team.CYAN) ? new Color(0, 255, 255, 150) : new Color(255, 140, 0, 150);
        Color edgeColor = new Color(0, 0, 0, 0);
        
        int radius = size / 2 + 15;
        
        java.awt.geom.Point2D center = new java.awt.geom.Point2D.Float(cx, cy);
        float[] dist = {0.0f, 1.0f};
        Color[] colors = {centerColor, edgeColor};
        
        java.awt.RadialGradientPaint p = new java.awt.RadialGradientPaint(center, radius, dist, colors);
        java.awt.Paint oldPaint = g2d.getPaint();
        g2d.setPaint(p);
        
        g2d.fillOval(cx - radius, cy - radius / 2, radius * 2, radius);
        g2d.setPaint(oldPaint);
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
                            obj instanceof domain.models.staticObjects.Door ||
                            obj instanceof domain.models.staticObjects.Decoration ||
                            obj instanceof domain.models.entity.SearchableObject ||
                            obj instanceof domain.models.entity.Sign) {
                        // Eğer hücrede bir eşya veya statik obje varsa, altını delik bırakmamak için
                        // Zemin (FloorTile)
                        // çiziyoruz
                        BufferedImage floor = tileManager.getTile("floor");
                        if (floor != null) {
                            g2d.drawImage(floor, offsetX + (x * tileSize), offsetY + (y * tileSize), tileSize, tileSize,
                                    null);
                        }
                    } else if (obj instanceof domain.models.tile.WallTile &&
                            "wall/wall_side".equals(obj.getImageName())) {
                        // Yan duvarlar: ince çiz (tile genişliğinin 1/3'ü)
                        BufferedImage tileImage = tileManager.getTile(obj.getImageName());
                        if (tileImage != null) {
                            int sideWidth = Math.max(tileSize / 3, 4);
                            int drawX;
                            if (x == 0) {
                                // Sol duvar: hücrenin sağ kenarında
                                drawX = offsetX + (x * tileSize) + tileSize - sideWidth;
                            } else {
                                // Sağ duvar: hücrenin sol kenarında
                                drawX = offsetX + (x * tileSize);
                            }
                            g2d.drawImage(tileImage, drawX, offsetY + (y * tileSize),
                                    sideWidth, tileSize, null);
                        }
                    } else {
                        // Normal harita objesi (Duvar vs.)
                        BufferedImage tileImage = tileManager.getTile(obj.getImageName());
                        if (tileImage != null) {
                            g2d.drawImage(tileImage, offsetX + (x * tileSize), offsetY + (y * tileSize), tileSize,
                                    tileSize, null);
                        }
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

    private void drawItems(Graphics2D g2d) {
        if (gameMap == null)
            return;

        for (int x = 0; x < gameMap.getWidth(); x++) {
            for (int y = 0; y < gameMap.getHeight(); y++) {
                domain.models.entity.GameObject obj = gameMap.getObjectAt(x, y);
                if (obj instanceof domain.models.item.MapItem) {
                    domain.models.item.MapItem item = (domain.models.item.MapItem) obj;

                    boolean inZone = hero != null && Math.abs(x - hero.getX()) <= 1
                            && Math.abs(y - hero.getY()) <= 1;

                    if (!inZone) {
                        java.awt.AlphaComposite ac = java.awt.AlphaComposite
                                .getInstance(java.awt.AlphaComposite.SRC_OVER, 0.4f);
                        g2d.setComposite(ac);
                    }

                    if (item.getSprite() != null) {
                        int maxDim = (int) (tileSize * 0.65);
                        int iw = item.getSprite().getWidth();
                        int ih = item.getSprite().getHeight();
                        double scale = Math.min((double) maxDim / iw, (double) maxDim / ih);
                        int dw = (int) (iw * scale);
                        int dh = (int) (ih * scale);
                        int drawX = offsetX + (x * tileSize) + (tileSize - dw) / 2;
                        int drawY = offsetY + (y * tileSize) + (tileSize - dh) / 2;
                        g2d.drawImage(item.getSprite(), drawX, drawY, dw, dh, null);
                    } else {
                        // Sprite yok — renk placeholder (scroll için mor, diğerleri sarı)
                        int spriteSize = (int) (tileSize * 0.65);
                        int posOffset = (tileSize - spriteSize) / 2;
                        int drawX = offsetX + (x * tileSize) + posOffset;
                        int drawY = offsetY + (y * tileSize) + posOffset;
                        if (item instanceof domain.models.item.ShadowCloneScroll) {
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
    }

    private void drawStaticObjects(Graphics2D g2d) {
        if (gameMap == null)
            return;

        for (int x = 0; x < gameMap.getWidth(); x++) {
            for (int y = 0; y < gameMap.getHeight(); y++) {
                domain.models.entity.GameObject obj = gameMap.getObjectAt(x, y);
                if (obj instanceof domain.models.entity.Column ||
                        obj instanceof domain.models.entity.Chest ||
                        obj instanceof domain.models.entity.Crate ||
                        obj instanceof domain.models.staticObjects.Door ||
                        obj instanceof domain.models.staticObjects.Decoration ||
                        obj instanceof domain.models.entity.SearchableObject ||
                        obj instanceof domain.models.entity.Sign) {

                    // Kullanıcı talebi: Obstacle'lar yarı saydam değil opak olmalı
                    // (AlphaComposite kaldırıldı)

                    BufferedImage sprite = null;
                    if (tileManager != null) {
                        String imgName = obj.getImageName();
                        if (obj instanceof domain.models.staticObjects.Decoration && imgName != null && imgName.startsWith("torch/")) {
                            long now = System.currentTimeMillis();
                            int[] frames = {1, 2, 3, 4, 6, 7, 8};
                            int frame = frames[(int) ((now / 120) % frames.length)];
                            imgName = "torch/torch_" + frame;
                        }
                        sprite = tileManager.getTile(imgName);
                    }

                    if (sprite != null) {
                        int iw = sprite.getWidth();
                        int ih = sprite.getHeight();
                        int dw = tileSize;
                        if (obj instanceof domain.models.staticObjects.Decoration) {
                            dw = (int) (tileSize * 0.4);
                        }
                        int dh = (int) (ih * ((double) dw / iw));
                        int drawX = offsetX + (x * tileSize) + (tileSize - dw) / 2;
                        int drawY = offsetY + (y * tileSize) + tileSize - dh;
                        g2d.drawImage(sprite, drawX, drawY, dw, dh, null);
                    } else {
                        // Placeholder fallback
                        int drawX = offsetX + (x * tileSize);
                        int drawY = offsetY + (y * tileSize);
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

                    // (Composite sıfırlamaya gerek yok, çünkü AlphaComposite kullanılmıyor)
                }
            }
        }
    }

    public void toggleInventory() {
        this.inventoryVisible = !this.inventoryVisible;
    }

    public boolean isInventoryVisible() {
        return inventoryVisible;
    }
}