package view;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;

import domain.models.entity.Hero;
import domain.models.Direction;
import domain.models.action.Interactable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class GameView extends JPanel {
    private Hero hero;
    private AssetManager assetManager;
    // --- ACTION MENU STATE ---
    // activeInteractable holds the object whose menu is currently open. 
    // menuX and menuY store the physical screen coordinates to draw the menu box.
    private Interactable activeInteractable = null;
    private int menuX = -1, menuY = -1;

    // --- INVENTORY MENU STATE ---
    // When true, the full-screen inventory panel is rendered on top of the game.
    private boolean inventoryMenuOpen = false;

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

        /* 
         * ACTION MENU MOUSE LOGIC:
         * This listener does two things:
         * 1. If the Action Menu is open, it checks if the user clicked inside the menu box to run an Action.
         * 2. If the Action Menu is closed, it converts the mouse click to a Grid coordinate (x,y), 
         *    checks if it's right next to the Hero, and opens the Action Menu for any Interactable object there.
         */
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (activeInteractable != null) {
                    List<domain.models.action.Action> actions = activeInteractable.getActions();
                    int menuWidth = 140;
                    int menuHeight = actions.size() * 30 + 10;
                    if (e.getX() >= menuX && e.getX() <= menuX + menuWidth &&
                        e.getY() >= menuY && e.getY() <= menuY + menuHeight) {
                        int index = (e.getY() - menuY - 5) / 30;
                        if (index >= 0 && index < actions.size()) {
                            actions.get(index).execute((domain.models.entity.GameObject)activeInteractable, GameView.this.hero, GameView.this.gameMap);
                        }
                    }
                    activeInteractable = null;
                    repaint();
                    return;
                }

                if (tileSize == 0) return;
                int gridX = (e.getX() - offsetX) / tileSize;
                int gridY = (e.getY() - offsetY) / tileSize;

                if (hero != null && Math.abs(hero.getX() - gridX) <= 1 && Math.abs(hero.getY() - gridY) <= 1) {
                    if (gameMap != null) {
                        domain.models.entity.GameObject obj = gameMap.getObjectAt(gridX, gridY);
                        if (obj instanceof Interactable) {
                            List<domain.models.action.Action> actions = ((Interactable)obj).getActions();
                            if (actions != null && !actions.isEmpty()) {
                                activeInteractable = (Interactable)obj;
                                menuX = e.getX();
                                menuY = e.getY();
                                repaint();
                            }
                        }
                    }
                }
            }
        });
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

        // 5. Action Menu Çiz (item interaction pop-up from mouse click)
        drawActionMenu(g2d);

        // 6. Inventory Menu Çiz (full overlay opened with E key)
        drawInventoryMenu(g2d);

        // Kaynakları temizle
        g2d.dispose();
    }

    /**
     * Toggles the inventory overlay open/closed.
     * Called by InputHandler when the player presses E.
     * Also closes any open interactable context menu to avoid overlap.
     */
    public void toggleInventoryMenu() {
        inventoryMenuOpen = !inventoryMenuOpen;
        if (inventoryMenuOpen) {
            // Close any open action-context menu when opening inventory
            activeInteractable = null;
        }
        repaint(); // Immediately refresh the screen to show/hide the panel
    }

    /*
     * ACTION MENU RENDERER:
     * If there is an activeInteractable, this dynamically calculates a UI box size
     * based on how many actions the item has. It draws a dark semi-transparent rectangle
     * and lists the action names using a Monospaced font.
     */
    private void drawActionMenu(Graphics2D g) {
        if (activeInteractable != null) {
            List<domain.models.action.Action> actions = activeInteractable.getActions();
            if (actions == null || actions.isEmpty()) return;

            int menuWidth = 140;
            int menuHeight = actions.size() * 30 + 10;

            // Programmable Action menu background
            // We can add a png file for this as well
            g.setColor(new Color(40, 40, 40, 230)); // dark gray
            g.fillRect(menuX, menuY, menuWidth, menuHeight);
            g.setColor(Color.WHITE); // white border
            g.drawRect(menuX, menuY, menuWidth, menuHeight);            
            g.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 14));
            int textY = menuY + 25;
            for (domain.models.action.Action action : actions) {
                // Determine target name
                String objectName = "";
                if (activeInteractable instanceof domain.models.item.Item) {
                   objectName = ((domain.models.item.Item)activeInteractable).getName();
                } else if (activeInteractable instanceof domain.models.staticObjects.StaticObject) {
                   objectName = activeInteractable.getClass().getSimpleName();
                }
                
                // Draw drop shadow for text to make it readable on complex backgrounds
                g.setColor(Color.BLACK);
                g.drawString(action.getName() + " " + objectName, menuX + 11, textY + 1);
                
                g.setColor(Color.WHITE);
                g.drawString(action.getName() + " " + objectName, menuX + 10, textY);
                textY += 30;
            }
        }
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

        // Keybind hint at the bottom of the HUD so the player knows how to open inventory
        g.setColor(new Color(255, 255, 180));
        g.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 11));
        g.drawString("[E] Inventory", hudX, hudY + 30);
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

    /**
     * INVENTORY MENU RENDERER
     *   - A title bar:  "[ INVENTORY ]"
     *     Each filled slot shows the item name and a type tag (POTION / WEAPON / ITEM)
     *   - A footer hint reminding the player to press E to close
     *
     * Called from paintComponent() only when inventoryMenuOpen == true.
     */
        private void drawInventoryMenu(Graphics2D g) {
        if (!inventoryMenuOpen) return; // Only render when inventory is open

        // 1. Full-screen dim layer
        g.setColor(new Color(0, 0, 0, 170)); 
        g.fillRect(0, 0, getWidth(), getHeight());

        // 2. Panel geometry — centred on screen
        int panelW = 340;
        int panelH = 340;
        int panelX = (getWidth() - panelW) / 2;
        int panelY = (getHeight() - panelH) / 2;

        java.awt.image.BufferedImage invBg = tileManager.getTile("bag - empty");
        if (invBg != null) {
            g.drawImage(invBg, panelX, panelY, panelW, panelH, null);
        }

        // 3. Slot grid
        java.util.List<domain.models.item.Item> items = hero.getInventory().getItems();
        
        // Grid exact metrics (adjust if your PNG differs!)
        int cols = 4;             
        int rows = 4;             
        int slotW = 50;          
        int slotH = 50;           
        int slotPadX = 14;        
        int slotPadY = 14;         
        
        int gridOriginX = panelX + (panelW - (cols * slotW + (cols - 1) * slotPadX)) / 2;
        int gridOriginY = panelY + 60; // Offset below the top edge of bag

        int slotIndex = 0; 
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int sx = gridOriginX + col * (slotW + slotPadX);
                int sy = gridOriginY + row * (slotH + slotPadY);

                if (slotIndex < items.size()) {
                    domain.models.item.Item item = items.get(slotIndex);

                    // Coloured type tag badge
                    String typeTag;
                    Color tagColor;
                    if (item instanceof domain.models.item.Potion) {
                        typeTag = "POTION";
                        tagColor = new Color(200, 80, 80);  // red
                    } else if (item instanceof domain.models.item.Weapon) {
                        typeTag = "WEAPON";
                        tagColor = new Color(80, 160, 220); // blue
                    } else {
                        typeTag = "ITEM";
                        tagColor = new Color(120, 200, 120); // green
                    }
                    g.setColor(tagColor);
                    g.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 9));
                    g.drawString(typeTag, sx + 4, sy + 12);

                    // Item name
                    g.setColor(Color.WHITE);
                    g.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
                    g.drawString(item.getName(), sx + 6, sy + 32);

                    // Slot number
                    g.setColor(new Color(160, 140, 200));
                    g.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 9));
                    g.drawString("#" + (slotIndex + 1), sx + slotW - 20, sy + slotH - 4);
                }
                slotIndex++;
            }
        }

        // 4. Footer: item count summary and close hint
        int footerY = panelY + panelH - 8;
        
        g.setColor(new Color(160, 130, 200));
        g.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 11));
        String summary = "Items: " + items.size() + " / " + (rows*cols);
        g.drawString(summary, panelX + 12, footerY);

        g.setColor(new Color(255, 255, 180));
        String closeHint = "[E] Close";
        int closeW = g.getFontMetrics().stringWidth(closeHint);
        g.drawString(closeHint, panelX + panelW - closeW - 12, footerY);
    }
}