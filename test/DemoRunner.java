import domain.models.entity.*;
import domain.models.map.GameMap;
import domain.models.Direction;
import view.AssetManager;
import view.GameView;
import view.TileManager;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class DemoRunner {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("COMP302 Dungeon Crawler Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Gerekli yöneticiler (Managers)
            AssetManager assetManager = new AssetManager();
            TileManager tileManager = new TileManager();

            // Modeller
            // ACTUAL_SIZE = 64 px (GameView'da)
            // 13 sütun x 64px = 832px genişlik
            // 10 satır x 64px = 640px yükseklik
            GameMap map = new GameMap(13, 10); 
            Hero hero = new Hero(1, 2); // y=1 artık duvar yüzeyi olduğu için 2'den başlıyoruz
            Knight knight = new Knight(11, 8); // Karşı köşeye atalım
            Sorcerer sorcerer = new Sorcerer(8, 8);
            
            // View (Görünüm)
            GameView gameView = new GameView(hero, assetManager);
            // JPanel boyutunu tam haritaya göre ayarla
            gameView.setPreferredSize(new java.awt.Dimension(832, 640));
            gameView.setGameMap(map);
            gameView.setTileManager(tileManager);
            gameView.setEnemies(knight, sorcerer);

            frame.add(gameView);
            
            // Çarpışma (Collision) hesaplamaları için tüm varlıkları bir listeye koyuyoruz
            java.util.List<Entity> entities = java.util.Arrays.asList(hero, knight, sorcerer);

            // Klavye girdilerini dinlemek için InputHandler'ı frame'e ekliyoruz
            controller.InputHandler inputHandler = new controller.InputHandler(hero, map, entities);
            frame.addKeyListener(inputHandler);
            frame.setFocusable(true);
            frame.requestFocusInWindow();

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Logic Loop (Düşman hareketleri ve enerji yenilenmesi hızı)
            javax.swing.Timer logicTimer = new javax.swing.Timer(120, (e) -> {
                hero.update();
                knight.followHero(hero, map, entities);
                sorcerer.followHero(hero, map, entities);
            });
            logicTimer.start();

            // Render Loop (Saniyede 60 Kare - 60 FPS Çizim Motoru)
            javax.swing.Timer renderTimer = new javax.swing.Timer(16, (e) -> {
                gameView.repaint();
            });
            renderTimer.start();
        });
    }
}