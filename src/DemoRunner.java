import domain.models.entity.*;
import domain.models.map.GameMap;
import domain.models.Direction;
import domain.logic.EnemySpawner;
import view.AssetManager;
import view.GameView;
import view.TileManager;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

import java.awt.CardLayout;
import javax.swing.JPanel;

public class DemoRunner {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("COMP302 Dungeon Crawler Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            CardLayout cardLayout = new CardLayout();
            JPanel mainPanel = new JPanel(cardLayout);

            view.MainMenuView menuView = new view.MainMenuView(() -> {
                startGame(frame, mainPanel, cardLayout);
            });
            menuView.setPreferredSize(new java.awt.Dimension(832, 640));

            mainPanel.add(menuView, "Menu");
            frame.add(mainPanel);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void startGame(JFrame frame, JPanel mainPanel, CardLayout cardLayout) {
        // Gerekli yöneticiler (Managers)
        AssetManager assetManager = AssetManager.getInstance();
        TileManager tileManager = new TileManager();

        // Modeller
        // ACTUAL_SIZE = 64 px (GameView'da)
        // 13 sütun x 64px = 832px genişlik
        // 10 satır x 64px = 640px yükseklik
        GameMap map = new GameMap(13, 10);
        Hero hero = new Hero(1, 2); // y=1 artık duvar yüzeyi olduğu için 2'den başlıyoruz
        Knight knight = new Knight(11, 8); // Başlangıç düşmanı — karşı köşe
        Sorcerer sorcerer = new Sorcerer(8, 8); // Başlangıç düşmanı

        // Random Spawning logic for Items (2 Potions, 1 Sword, 1 Key)
        java.util.Random rand = new java.util.Random();

        // Spawn 2 Potions
        for (int i = 0; i < 2; i++) {
            placeRandomItem(map, new domain.models.item.PotionItem(0, 0), hero, knight, sorcerer, rand);
        }
        // Spawn 1 Sword
        placeRandomItem(map, new domain.models.item.SwordItem(0, 0), hero, knight, sorcerer, rand);
        // Spawn 1 Key
        placeRandomItem(map, new domain.models.staticObjects.KeyItem(0, 0), hero, knight, sorcerer, rand);

        // Spawn Static Objects
        for (int i = 0; i < 2; i++) {
            placeRandomItem(map, new domain.models.entity.Column("Column " + (i + 1), 0, 0), hero, knight, sorcerer, rand);
            placeRandomItem(map, new domain.models.entity.Crate("Crate " + (i + 1), 0, 0), hero, knight, sorcerer, rand);
        }
        placeRandomItem(map, new domain.models.entity.Chest("Chest", 0, 0), hero, knight, sorcerer, rand);
        placeRandomItem(map, new domain.models.entity.SearchableObject("Searchable", 0, 0), hero, knight, sorcerer, rand);

        // View (Görünüm)
        GameView gameView = new GameView(hero, assetManager);
        // JPanel boyutunu tam haritaya göre ayarla
        gameView.setPreferredSize(new java.awt.Dimension(832, 640));
        gameView.setGameMap(map);
        gameView.setTileManager(tileManager);
        gameView.setEnemies(knight, sorcerer);

        mainPanel.add(gameView, "Game");
        cardLayout.show(mainPanel, "Game");

        // ActionMenu & MouseHandler Initialize
        view.ActionMenu actionMenu = new view.ActionMenu(hero);
        controller.MouseHandler mouseHandler = new controller.MouseHandler(hero, map, gameView, actionMenu);
        gameView.addMouseListener(mouseHandler);

        // ArrayList kullanıyoruz (Arrays.asList değil) — EnemySpawner yeni düşman
        // ekleyebilsin
        List<Entity> entities = new ArrayList<>();
        entities.add(hero);
        entities.add(knight);
        entities.add(sorcerer);

        // GameView'a entity listesini bağla — yeni spawn olanlar da otomatik çizilsin
        gameView.setEntityList(entities);

        // Klavye girdilerini dinlemek için InputHandler'ı frame'e ekliyoruz
        controller.InputHandler inputHandler = new controller.InputHandler(hero, map, entities, gameView);
        frame.addKeyListener(inputHandler);
        frame.setFocusable(true);
        frame.requestFocusInWindow();

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
                if (k.isAlive())
                    k.followHero(hero, map, entities);
            }
            for (Sorcerer s : spawner.getSpawnedSorcerers()) {
                if (s.isAlive())
                    s.followHero(hero, map, entities);
            }
        });
        logicTimer.start();

        // Render Loop (Saniyede 60 Kare - 60 FPS Çizim Motoru)
        javax.swing.Timer renderTimer = new javax.swing.Timer(16, (e) -> {
            gameView.repaint();
        });
        renderTimer.start();
    }

    private static void placeRandomItem(domain.models.map.GameMap map, domain.models.entity.GameObject item,
            domain.models.entity.Hero hero, domain.models.entity.Knight knight,
            domain.models.entity.Sorcerer sorcerer, java.util.Random rand) {
        boolean placed = false;
        while (!placed) {
            int x = rand.nextInt(map.getWidth());
            int y = rand.nextInt(map.getHeight());

            // Avoid hero and enemy starting positions
            if ((x == hero.getX() && y == hero.getY()) ||
                    (x == knight.getX() && y == knight.getY()) ||
                    (x == sorcerer.getX() && y == sorcerer.getY())) {
                continue;
            }

            // Must be entirely empty floor (meaning no wall or existing items)
            domain.models.entity.GameObject existingObj = map.getObjectAt(x, y);
            if (existingObj != null && existingObj.getImageName().equals("floor")
                    && !(existingObj instanceof domain.models.item.MapItem)) {
                item.setPosition(x, y);
                map.placeObject(item, x, y);
                placed = true;
            }
        }
    }
}