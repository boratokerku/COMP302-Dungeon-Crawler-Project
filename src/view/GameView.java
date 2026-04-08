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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Gelişmiş çizim yetenekleri için Graphics2D'ye dönüştürüyoruz
        Graphics2D g2d = (Graphics2D) g;

        // 1. Zemin veya Haritayı Çiz (Şimdilik boş geçebiliriz)
        drawMap(g2d);

        // 2. Hero'yu Çiz
        drawHero(g2d);

        // Kaynakları temizle
        g2d.dispose();
    }

    private void drawHero(Graphics2D g2d) {
        // Hero'nun o anki animasyon karesini AssetManager'dan al
        BufferedImage frame = assetManager.getHeroSprite(hero.getAnimationState());

        if (frame != null) {
            // Hero'nun koordinatlarını piksel cinsine çevir
            int x = hero.getX() * ACTUAL_SIZE;
            int y = hero.getY() * ACTUAL_SIZE;

            // --- SOLA DÖNME MANTIĞI ---
            if (hero.getDirection() == Direction.LEFT) {
                // Resmi yatayda aynalayarak çiziyoruz
                // x + ACTUAL_SIZE noktasından başlayıp, genişliği -ACTUAL_SIZE veriyoruz
                g2d.drawImage(frame, x + ACTUAL_SIZE, y, -ACTUAL_SIZE, ACTUAL_SIZE, null);
            } else {
                // Sağa, Yukarı veya Aşağı bakarken normal çiz
                g2d.drawImage(frame, x, y, ACTUAL_SIZE, ACTUAL_SIZE, null);
            }
        }
    }

    private void drawMap(Graphics2D g2d) {
        // İleride buraya MapManager'dan gelen duvarları ve yerleri çizeceğiz
        // Örnek: g2d.drawImage(assetManager.getWallSprite(), 0, 0, ACTUAL_SIZE,
        // ACTUAL_SIZE, null);
    }
}