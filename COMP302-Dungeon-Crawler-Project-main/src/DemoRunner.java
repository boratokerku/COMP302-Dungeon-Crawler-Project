import domain.models.entity.*;
import domain.models.map.GameMap;
import domain.models.Direction;
import domain.logic.EnemySpawner;
import domain.models.item.*;
import view.AssetManager;
import view.GameView;
import view.TileManager;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

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
            Knight knight = new Knight(11, 8); // Başlangıç düşmanı — karşı köşe
            Sorcerer sorcerer = new Sorcerer(8, 8); // Başlangıç düşmanı

            // Test için Potion ve Sword ekle
            Potion potion = new Potion(2, 2, "red_potion", "Health Potion", 5);
            map.placeObject(potion, 2, 2);

            Weapon weapon = new Weapon(3, 3, "big_sword", "Sword", 10);
            map.placeObject(weapon, 3, 3);

            // View (Görünüm)
            GameView gameView = new GameView(hero, assetManager);
            // JPanel boyutunu tam haritaya göre ayarla
            gameView.setPreferredSize(new java.awt.Dimension(832, 640));
            gameView.setGameMap(map);
            gameView.setTileManager(tileManager);
            gameView.setEnemies(knight, sorcerer);

            frame.add(gameView);

            // ArrayList kullanıyoruz (Arrays.asList değil) — EnemySpawner yeni düşman ekleyebilsin
            List<Entity> entities = new ArrayList<>();
            entities.add(hero);
            entities.add(knight);
            entities.add(sorcerer);

            // GameView'a entity listesini bağla — yeni spawn olanlar da otomatik çizilsin
            gameView.setEntityList(entities);

            // Klavye girdilerini dinlemek için InputHandler'ı frame'e ekliyoruz
            controller.InputHandler inputHandler = new controller.InputHandler(hero, map, entities);
            // Link the view so that pressing E can open the Inventory overlay
            inputHandler.setGameView(gameView);
            frame.addKeyListener(inputHandler);
            frame.setFocusable(true);
            frame.requestFocusInWindow();

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // EnemySpawner — design doc §2.5: 9 saniyede bir, kenar tile'dan, %60/%30/%10
            EnemySpawner spawner = new EnemySpawner(map);

            // Logic Loop (Düşman hareketleri ve enerji yenilenmesi hızı)
            javax.swing.Timer logicTimer = new javax.swing.Timer(120, (e) -> {
                // Hero enerji yenileme
                hero.update();

                // Başlangıç düşmanlarının AI'sı
                knight.followHero(hero, map, entities);
                sorcerer.followHero(hero, map, entities);

                // Spawn kontrolü (her 9 saniyede bir yeni düşman çıkarmayı dener)
                spawner.trySpawn(entities);

                // Yeni spawn olan düşmanların AI'sını çalıştır
                for (Knight k : spawner.getSpawnedKnights()) {
                    if (k.isAlive()) k.followHero(hero, map, entities);
                }
                for (Sorcerer s : spawner.getSpawnedSorcerers()) {
                    if (s.isAlive()) s.followHero(hero, map, entities);
                }
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