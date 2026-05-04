import domain.models.entity.*;
import domain.models.map.GameMap;
import domain.logic.EnemySpawner;
import domain.logic.ScrollSpawner;
import domain.models.GameState;
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

            view.MainMenuView menuView = new view.MainMenuView(
                    () -> startGame(frame, mainPanel, cardLayout),
                    (state) -> loadGame(frame, mainPanel, cardLayout, state)
            );
            menuView.setPreferredSize(new java.awt.Dimension(832, 640));

            mainPanel.add(menuView, "Menu");
            frame.add(mainPanel);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // Yeni oyun — tüm nesneler rastgele oluşturulur
    private static void startGame(JFrame frame, JPanel mainPanel, CardLayout cardLayout) {
        GameMap map = new GameMap(13, 10);
        Hero hero = new Hero(1, 2);
        Knight knight = new Knight(11, 8);
        Sorcerer sorcerer = new Sorcerer(8, 8);

        java.util.Random rand = new java.util.Random();

        for (int i = 0; i < 2; i++) {
            placeRandomItem(map, new domain.models.item.PotionItem(0, 0), hero, knight, sorcerer, rand);
        }
        placeRandomItem(map, new domain.models.item.SwordItem(0, 0), hero, knight, sorcerer, rand);
        placeRandomItem(map, new domain.models.staticObjects.KeyItem(0, 0), hero, knight, sorcerer, rand);

        for (int i = 0; i < 2; i++) {
            placeRandomItem(map, new domain.models.entity.Column("Column " + (i + 1), 0, 0), hero, knight, sorcerer, rand);
            placeRandomItem(map, new domain.models.entity.Crate("Crate " + (i + 1), 0, 0), hero, knight, sorcerer, rand);
        }
        placeRandomItem(map, new domain.models.entity.Chest("Chest", 0, 0), hero, knight, sorcerer, rand);
        placeRandomItem(map, new domain.models.entity.SearchableObject("Searchable", 0, 0), hero, knight, sorcerer, rand);

        List<Entity> entities = new ArrayList<>();
        entities.add(hero);
        entities.add(knight);
        entities.add(sorcerer);

        // Yeni oyunda haritada scroll yok — boş liste
        List<GameState.ItemRecord> scrollItems = new ArrayList<>();
        setupGameView(frame, mainPanel, cardLayout, hero, entities, map, knight, sorcerer, scrollItems);
    }

    // Kaydedilmiş oyunu yükle — GameState'ten tüm nesneler yeniden oluşturulur
    private static void loadGame(JFrame frame, JPanel mainPanel, CardLayout cardLayout, GameState state) {
        GameMap map = new GameMap(13, 10);

        // Hero yeniden oluştur
        Hero hero = new Hero(state.hero.x, state.hero.y);
        hero.setHp(state.hero.hp);
        hero.setMana(state.hero.mana);
        hero.setEnergy(state.hero.energy);

        // Düşmanları yeniden oluştur
        Knight knight = null;
        Sorcerer sorcerer = null;
        List<Entity> entities = new ArrayList<>();
        entities.add(hero);

        for (GameState.EnemyRecord rec : state.enemies) {
            if ("Knight".equals(rec.type)) {
                Knight k = new Knight(rec.x, rec.y);
                k.setHp(rec.hp);
                entities.add(k);
                if (knight == null) knight = k;
            } else if ("Sorcerer".equals(rec.type)) {
                Sorcerer s = new Sorcerer(rec.x, rec.y);
                s.setHp(rec.hp);
                entities.add(s);
                if (sorcerer == null) sorcerer = s;
            }
        }

        // Fallback: kayıtta düşman yoksa default pozisyon
        if (knight == null)   { knight   = new Knight(11, 8);  entities.add(knight); }
        if (sorcerer == null) { sorcerer = new Sorcerer(8, 8); entities.add(sorcerer); }

        // Harita itemlarını ayır: scroll'lar ayrı tutulur (inputHandler gerektirir)
        List<GameState.ItemRecord> scrollItems = new ArrayList<>();

        for (GameState.ItemRecord rec : state.mapItems) {
            if ("ShadowCloneScroll".equals(rec.type)) {
                // Scroll'lar setupGameView içinde inputHandler ile birlikte oluşturulur
                scrollItems.add(rec);
            } else {
                domain.models.entity.GameObject item = createItem(rec.type, rec.x, rec.y);
                if (item != null) map.placeObject(item, rec.x, rec.y);
            }
        }

        // Envanter itemlarını yeniden oluştur
        for (String type : state.inventoryItems) {
            domain.models.entity.GameObject item = createItem(type, 0, 0);
            if (item != null) hero.getInventory().addItem(item);
        }

        setupGameView(frame, mainPanel, cardLayout, hero, entities, map, knight, sorcerer, scrollItems);
    }

    // Item tip ismine göre nesne oluşturur — scroll hariç (scroll setupGameView'da oluşur)
    private static domain.models.entity.GameObject createItem(String type, int x, int y) {
        switch (type) {
            case "PotionItem": return new domain.models.item.PotionItem(x, y);
            case "SwordItem":  return new domain.models.item.SwordItem(x, y);
            case "KeyItem":    return new domain.models.staticObjects.KeyItem(x, y);
            default:
                System.err.println("Bilinmeyen item tipi: " + type);
                return null;
        }
    }

    // startGame ve loadGame tarafından ortak kullanılan view/timer/input kurulum
    // scrollItems: haritaya yerleştirilecek scroll kayıtları — inputHandler gerektirdiği için burada oluşturulur
    private static void setupGameView(JFrame frame, JPanel mainPanel, CardLayout cardLayout,
                                      Hero hero, List<Entity> entities, GameMap map,
                                      Knight knight, Sorcerer sorcerer,
                                      List<GameState.ItemRecord> scrollItems) {
        AssetManager assetManager = AssetManager.getInstance();
        TileManager tileManager = new TileManager();

        GameView gameView = new GameView(hero, assetManager);
        gameView.setPreferredSize(new java.awt.Dimension(832, 640));
        gameView.setGameMap(map);
        gameView.setTileManager(tileManager);
        gameView.setEnemies(knight, sorcerer);
        gameView.setEntityList(entities);

        mainPanel.add(gameView, "Game");
        cardLayout.show(mainPanel, "Game");

        view.ActionMenu actionMenu = new view.ActionMenu(hero);
        controller.MouseHandler mouseHandler = new controller.MouseHandler(hero, map, gameView, actionMenu);
        gameView.addMouseListener(mouseHandler);

        controller.InputHandler inputHandler = new controller.InputHandler(hero, map, entities, gameView);
        gameView.setFocusable(true);
        gameView.addKeyListener(inputHandler);
        gameView.requestFocusInWindow();

        // Scroll'ları şimdi yerleştir — inputHandler hazır olduğu için tam işlevsel oluşturulur
        for (GameState.ItemRecord rec : scrollItems) {
            domain.models.item.ShadowCloneScroll scroll =
                    new domain.models.item.ShadowCloneScroll(rec.x, rec.y, entities, map, inputHandler);
            map.placeObject(scroll, rec.x, rec.y);
        }

        // Timer referans tutucular — lambda içinden timer'a erişmek için (pause/resume)
        final javax.swing.Timer[] logicRef  = new javax.swing.Timer[1];
        final javax.swing.Timer[] renderRef = new javax.swing.Timer[1];

        // PauseMenu — JFrame glass pane olarak oyunun üstüne bindiriliyor
        view.PauseMenu pauseMenu = new view.PauseMenu(
                hero, entities, map,
                () -> {
                    if (logicRef[0] != null)  logicRef[0].start();
                    if (renderRef[0] != null) renderRef[0].start();
                    gameView.requestFocusInWindow();
                },
                () -> {
                    if (logicRef[0] != null)  logicRef[0].stop();
                    if (renderRef[0] != null) renderRef[0].stop();
                    cardLayout.show(mainPanel, "Menu");
                }
        );
        frame.setGlassPane(pauseMenu);

        // ESC tuşu — pause/resume toggle
        gameView.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke("ESCAPE"), "togglePause");
        gameView.getActionMap().put("togglePause", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                boolean paused = pauseMenu.isVisible();
                if (paused) {
                    pauseMenu.setVisible(false);
                    if (logicRef[0] != null)  logicRef[0].start();
                    if (renderRef[0] != null) renderRef[0].start();
                    gameView.requestFocusInWindow();
                } else {
                    if (logicRef[0] != null)  logicRef[0].stop();
                    if (renderRef[0] != null) renderRef[0].stop();
                    pauseMenu.setVisible(true);
                }
            }
        });

        // EnemySpawner — design doc §2.5: 9 saniyede bir, kenar tile'dan, %60/%30/%10
        EnemySpawner spawner = new EnemySpawner(map);

        // ScrollSpawner — design doc §Phase2: 15 saniyede bir rastgele tile'a scroll çıkar
        ScrollSpawner scrollSpawner = new ScrollSpawner(map, entities, inputHandler);

        // Logic Loop (Düşman hareketleri ve enerji yenilenmesi hızı)
        logicRef[0] = new javax.swing.Timer(120, (e) -> {
            hero.update();

            knight.followHero(hero, map, entities);
            sorcerer.followHero(hero, map, entities);

            spawner.trySpawn(entities);

            for (Knight k : spawner.getSpawnedKnights()) {
                if (k.isAlive()) k.followHero(hero, map, entities);
            }
            for (Sorcerer s : spawner.getSpawnedSorcerers()) {
                if (s.isAlive()) s.followHero(hero, map, entities);
            }

            scrollSpawner.trySpawn();

            ShadowClone activeClone = inputHandler.getShadowClone();
            if (activeClone != null) activeClone.update();
        });
        logicRef[0].start();

        // Render Loop (Saniyede 60 Kare - 60 FPS Çizim Motoru)
        renderRef[0] = new javax.swing.Timer(16, (e) -> {
            gameView.repaint();
        });
        renderRef[0].start();
    }

    private static void placeRandomItem(domain.models.map.GameMap map, domain.models.entity.GameObject item,
            domain.models.entity.Hero hero, domain.models.entity.Knight knight,
            domain.models.entity.Sorcerer sorcerer, java.util.Random rand) {
        boolean placed = false;
        while (!placed) {
            int x = rand.nextInt(map.getWidth());
            int y = rand.nextInt(map.getHeight());

            if ((x == hero.getX() && y == hero.getY()) ||
                    (x == knight.getX() && y == knight.getY()) ||
                    (x == sorcerer.getX() && y == sorcerer.getY())) {
                continue;
            }

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