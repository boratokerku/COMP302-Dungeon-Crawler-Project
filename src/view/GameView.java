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

    // Sabitler (TA sunumunda "Magic Numbers" kullanmadığını göstermek için önemli)
    private final int TILE_SIZE = 32; // Her bir kare 32x32 piksel
    private final int SCALE = 2; // Görseli x2 büyütmek istersen (isteğe bağlı)
    private final int ACTUAL_SIZE = TILE_SIZE * SCALE;

    public GameView(Hero hero, AssetManager assetManager) {
        this.hero = hero;
        this.assetManager = assetManager;

        // Panel ayarları
        this.setPreferredSize(new Dimension(800, 600)); // Pencere boyutu
        this.setBackground(Color.BLACK); // Arka plan (Zindan havası)
        this.setDoubleBuffered(true); // Titremeyi önleyen teknik (Double Buffering)
    }

    private domain.models.map.GameMap gameMap;
    private domain.models.entity.Knight knight;
    private domain.models.entity.Sorcerer sorcerer;
    private TileManager tileManager;

    // Spawn edilen dahil TÜM entity'leri tutan liste (EnemySpawner yeni ekledikçe buraya yansır)
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
     * Bu liste güncellendiğinde (yeni düşman spawn olduğunda) GameView otomatik olarak
     * yeni düşmanları da çizer — referans olduğu için her zaman güncel kalır.
     */
    public void setEntityList(java.util.List<domain.models.entity.Entity> list) {
        this.entityList = list;
    }

    public void setTileManager(TileManager tileManager) {
        this.tileManager = tileManager;
    }
    
    // Çizimden hemen önce ekran boyutlarını hesaplar
    private void calculateDimensions() {
        if (gameMap != null) {
            // Ekranın o anki Genişliğini ve Yüksekliğini (tam ekran yapıldığında artar) harita sınırlarına böl
            int tileW = getWidth() / gameMap.getWidth();
            int tileH = getHeight() / gameMap.getHeight();
            // Görüntünün uzayıp bozulmaması (Aspect Ratio korunması) için en küçük olanı baz al
            tileSize = Math.min(tileW, tileH);
            
            // Haritayı ekranın tam ortasına hizalamak için boşluk (offset) hesapla
            offsetX = (getWidth() - (tileSize * gameMap.getWidth())) / 2;
            offsetY = (getHeight() - (tileSize * gameMap.getHeight())) / 2;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        // Her çizim döngüsünde ekranın boyutuna göre karelerin (Tile) büyüklüğünü hesapla
        calculateDimensions();

        // 1. Zemin veya Haritayı Çiz
        drawMap(g2d);

        // 2. Hero'yu Çiz
        drawHero(g2d);

        // 3. Düşmanları Çiz
        drawEnemies(g2d);

        // 4. HUD (Arayüz) Çiz
        drawHUD(g2d);

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
        int hpWidth = (int)((hero.getHp() / 17.0) * barWidth); // 17 max can
        g.fillRect(hudX, hudY, Math.max(0, hpWidth), barHeight);
        g.setColor(Color.WHITE);
        g.drawString("HP: " + hero.getHp(), hudX + barWidth + 5, hudY + 12);

        // 2. Mana Bar
        hudY += 25;
        g.setColor(Color.GRAY);
        g.fillRect(hudX, hudY, barWidth, barHeight);
        g.setColor(new Color(50, 100, 200)); // Mavi
        int manaWidth = (int)((hero.getMana() / 80.0) * barWidth);
        g.fillRect(hudX, hudY, Math.max(0, manaWidth), barHeight);
        g.setColor(Color.WHITE);
        g.drawString("Mana: " + hero.getMana(), hudX + barWidth + 5, hudY + 12);

        // 3. Energy Bar
        hudY += 25;
        g.setColor(Color.GRAY);
        g.fillRect(hudX, hudY, barWidth, barHeight);
        g.setColor(new Color(200, 200, 50)); // Sarı
        int energyWidth = (int)((hero.getEnergy() / 100.0) * barWidth);
        g.fillRect(hudX, hudY, Math.max(0, energyWidth), barHeight);
        g.setColor(Color.WHITE);
        g.drawString("Energy: " + hero.getEnergy(), hudX + barWidth + 5, hudY + 12);
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
                if (!e.isAlive()) continue; // Ölmüş entity'leri çizme
                if (e instanceof domain.models.entity.Hero) continue; // Hero ayrıca çiziliyor

                BufferedImage frame = null;
                if (e instanceof domain.models.entity.Knight) {
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
            // entityList yoksa: sadece başlangıç knight ve sorcerer'ı çiz (geriye dönük uyumluluk)
            if (knight != null && knight.isAlive()) {
                BufferedImage kFrame = assetManager.getKnightSprite();
                if (kFrame != null) {
                    g2d.drawImage(kFrame, offsetX + (knight.getX() * tileSize), offsetY + (knight.getY() * tileSize), tileSize, tileSize, null);
                }
            }
            if (sorcerer != null && sorcerer.isAlive()) {
                BufferedImage sFrame = assetManager.getSorcererSprite();
                if (sFrame != null) {
                    g2d.drawImage(sFrame, offsetX + (sorcerer.getX() * tileSize), offsetY + (sorcerer.getY() * tileSize), tileSize, tileSize, null);
                }
            }
        }
    }

    private void drawMap(Graphics2D g2d) {
        if (gameMap == null || tileManager == null) return;
        
        for (int x = 0; x < gameMap.getWidth(); x++) {
            for (int y = 0; y < gameMap.getHeight(); y++) {
                domain.models.entity.GameObject obj = gameMap.getObjectAt(x, y);
                if (obj != null) {
                    BufferedImage tileImage = tileManager.getTile(obj.getImageName());
                    if (tileImage != null) {
                        g2d.drawImage(tileImage, offsetX + (x * tileSize), offsetY + (y * tileSize), tileSize, tileSize, null);
                    }
                }
            }
        }
    }
}