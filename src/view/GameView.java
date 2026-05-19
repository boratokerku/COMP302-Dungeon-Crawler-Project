
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

    // Spawn edilen dahil TÜM entity'leri tutan liste (EnemySpawner yeni ekledikçe
    // buraya yansır)
    private java.util.List<domain.models.entity.Entity> entityList;

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

        // Kaynakları temizle
        g2d.dispose();
    }

    private void drawHUD(Graphics2D g) {
        // HUD Ayarları
        int hudX = 20;
        int hudY = 20;
        int barWidth = 150;
        int barHeight = 15;

        // Arşaplan (Gölge efekti)
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(hudX - 10, hudY - 10, barWidth + 80, 80);

        // 1. HP Bar (Can)
        g.setColor(Color.GRAY);
        g.fillRect(hudX, hudY, barWidth, barHeight);
        g.setColor(new Color(200, 50, 50)); // Kırmızı
        int hpWidth = (int) ((hero.getHp() / 17.0) * barWidth); // 17 max can
        g.fillRect(hudX, hudY, Math.max(0, hpWidth), barHeight);
        g.setColor(Color.WHITE);
        g.drawString("HP: " + hero.getHp(), hudX + barWidth + 5, hudY + 12);

        // 2. Mana Bar
        hudY += 25;
        g.setColor(Color.GRAY);
        g.fillRect(hudX, hudY, barWidth, barHeight);
        g.setColor(new Color(50, 100, 200)); // Mavi
        int manaWidth = (int) ((hero.getMana() / 80.0) * barWidth);
        g.fillRect(hudX, hudY, Math.max(0, manaWidth), barHeight);
        g.setColor(Color.WHITE);
        g.drawString("Mana: " + hero.getMana(), hudX + barWidth + 5, hudY + 12);

        // 3. Energy Bar
        hudY += 25;
        g.setColor(Color.GRAY);
        g.fillRect(hudX, hudY, barWidth, barHeight);
        g.setColor(new Color(200, 200, 50)); // Sarı
        int energyWidth = (int) ((hero.getEnergy() / 100.0) * barWidth);
        g.fillRect(hudX, hudY, Math.max(0, energyWidth), barHeight);
        g.setColor(Color.WHITE);
        g.drawString("Energy: " + hero.getEnergy(), hudX + barWidth + 5, hudY + 12);
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

    private void drawHero(Graphics2D g2d) {
        BufferedImage frame = assetManager.getHeroSprite(hero.getAnimationState());

        if (frame != null) {
            int x = offsetX + (hero.getX() * tileSize);
            int y = offsetY + (hero.getY() * tileSize);

            if (hero.getDirection() == Direction.LEFT) {
                // Resmi yatayda aynalayarak çiziyoruz
                g2d.drawImage(frame, x + tileSize, y, -tileSize, tileSize, null);
            } else {
                g2d.drawImage(frame, x, y, tileSize, tileSize, null);
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
                    // Mermi: parlayan mor daire olarak çiz
                    int px = offsetX + (e.getX() * tileSize) + tileSize / 4;
                    int py = offsetY + (e.getY() * tileSize) + tileSize / 4;
                    int size = tileSize / 2;
                    g2d.setColor(new java.awt.Color(180, 50, 255, 200)); // Mor (büyücü mermisi)
                    g2d.fillOval(px, py, size, size);
                    g2d.setColor(new java.awt.Color(255, 200, 255, 150)); // Parlak kenar
                    g2d.drawOval(px - 2, py - 2, size + 4, size + 4);
                    continue;
                } else if (e instanceof domain.models.entity.ShadowClone) {
                    // Klon: hero sprite'ı %50 saydamlıkla (görsel ayrım)
                    frame = assetManager.getHeroSprite(domain.models.AnimationState.IDLE);
                    if (frame != null) {
                        java.awt.AlphaComposite ac = java.awt.AlphaComposite
                                .getInstance(java.awt.AlphaComposite.SRC_OVER, 0.5f);
                        g2d.setComposite(ac);
                        g2d.drawImage(frame,
                                offsetX + (e.getX() * tileSize),
                                offsetY + (e.getY() * tileSize),
                                tileSize, tileSize, null);
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
                    g2d.drawImage(frame,
                            offsetX + (e.getX() * tileSize),
                            offsetY + (e.getY() * tileSize),
                            tileSize, tileSize, null);
                }
            }

        } else {
            // entityList yoksa: sadece başlangıç knight ve sorcerer'ı çiz (geriye dönük
            // uyumluluk)
            if (knight != null && knight.isAlive()) {
                BufferedImage kFrame = assetManager.getKnightSprite();
                if (kFrame != null) {
                    g2d.drawImage(kFrame, offsetX + (knight.getX() * tileSize), offsetY + (knight.getY() * tileSize),
                            tileSize, tileSize, null);
                }
            }
            if (sorcerer != null && sorcerer.isAlive()) {
                BufferedImage sFrame = assetManager.getSorcererSprite();
                if (sFrame != null) {
                    g2d.drawImage(sFrame, offsetX + (sorcerer.getX() * tileSize),
                            offsetY + (sorcerer.getY() * tileSize), tileSize, tileSize, null);
                }
            }
        }
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
                            obj instanceof domain.models.entity.SearchableObject) {
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

                    int spriteSize = (int) (tileSize * 0.65);
                    int posOffset = (tileSize - spriteSize) / 2;
                    int drawX = offsetX + (x * tileSize) + posOffset;
                    int drawY = offsetY + (y * tileSize) + posOffset;

                    if (item.getSprite() != null) {
                        g2d.drawImage(item.getSprite(), drawX, drawY, spriteSize, spriteSize, null);
                    } else {
                        // Sprite yok — renk placeholder (scroll için mor, diğerleri sarı)
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
                        obj instanceof domain.models.entity.SearchableObject) {

                    boolean inZone = hero != null && Math.abs(x - hero.getX()) <= 1
                            && Math.abs(y - hero.getY()) <= 1;

                    if (!inZone) {
                        java.awt.AlphaComposite ac = java.awt.AlphaComposite
                                .getInstance(java.awt.AlphaComposite.SRC_OVER, 0.4f);
                        g2d.setComposite(ac);
                    }

                    BufferedImage sprite = null;
                    if (tileManager != null) {
                        sprite = tileManager.getTile(obj.getImageName());
                    }

                    int drawX = offsetX + (x * tileSize);
                    int drawY = offsetY + (y * tileSize);

                    if (sprite != null) {
                        g2d.drawImage(sprite, drawX, drawY, tileSize, tileSize, null);
                    } else {
                        // Placeholder fallback
                        Color color = Color.MAGENTA;
                        if (obj instanceof domain.models.entity.Column)
                            color = Color.GRAY;
                        else if (obj instanceof domain.models.entity.Chest)
                            color = new Color(139, 69, 19);
                        else if (obj instanceof domain.models.entity.Crate)
                            color = new Color(101, 67, 33);
                        else if (obj instanceof domain.models.entity.SearchableObject)
                            color = Color.DARK_GRAY;

                        g2d.setColor(color);
                        g2d.fillRect(drawX, drawY, tileSize, tileSize);
                    }

                    // Reset composite
                    g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1.0f));
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