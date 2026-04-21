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

    private final int TILE_SIZE = 32;
    private final int SCALE = 2;
    private final int ACTUAL_SIZE = TILE_SIZE * SCALE;

    public GameView(Hero hero, AssetManager assetManager) {
        this.hero = hero;
        this.assetManager = assetManager;

        this.setPreferredSize(new Dimension(800, 600));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
    }

    private domain.models.map.GameMap gameMap;
    private domain.models.entity.Knight knight;
    private domain.models.entity.Sorcerer sorcerer;
    private TileManager tileManager;

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

    public void setTileManager(TileManager tileManager) {
        this.tileManager = tileManager;
    }

    // Çizimden hemen önce ekran boyutlarını hesaplar
    private void calculateDimensions() {
        if (gameMap != null) {

            int tileW = getWidth() / gameMap.getWidth();
            int tileH = getHeight() / gameMap.getHeight();

            tileSize = Math.min(tileW, tileH);

            offsetX = (getWidth() - (tileSize * gameMap.getWidth())) / 2;
            offsetY = (getHeight() - (tileSize * gameMap.getHeight())) / 2;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        calculateDimensions();

        drawMap(g2d);

        drawHero(g2d);

        drawEnemies(g2d);

        drawHUD(g2d);

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
        g.setColor(new Color(200, 200, 50));
        int energyWidth = (int) ((hero.getEnergy() / 100.0) * barWidth);
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
                g2d.drawImage(frame, x + tileSize, y, -tileSize, tileSize, null);
            } else {
                g2d.drawImage(frame, x, y, tileSize, tileSize, null);
            }
        }
    }

    private void drawEnemies(Graphics2D g2d) {
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
                g2d.drawImage(sFrame, offsetX + (sorcerer.getX() * tileSize), offsetY + (sorcerer.getY() * tileSize),
                        tileSize, tileSize, null);
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
                    BufferedImage tileImage = tileManager.getTile(obj.getImageName());
                    if (tileImage != null) {
                        g2d.drawImage(tileImage, offsetX + (x * tileSize), offsetY + (y * tileSize), tileSize, tileSize,
                                null);
                    }
                }
            }
        }
    }
}