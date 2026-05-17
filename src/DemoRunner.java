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
import java.awt.Color;
import javax.swing.JPanel;

public class DemoRunner {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("COMP302 Dungeon Crawler");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            CardLayout cardLayout = new CardLayout();
            JPanel mainPanel = new JPanel(cardLayout);

            view.MainMenuView menuView = new view.MainMenuView(
                    () -> startGame(frame, mainPanel, cardLayout),
                    (state) -> loadGame(frame, mainPanel, cardLayout, state)
            );
            menuView.setPreferredSize(new java.awt.Dimension(1250, 1000));

            mainPanel.setBackground(Color.BLACK);
            mainPanel.add(menuView, "Menu");
            cardLayout.show(mainPanel, "Menu"); 
            frame.add(mainPanel);

            frame.setSize(832, 640);
            frame.pack();
            frame.revalidate();
            frame.repaint();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // Yeni oyun — tüm nesneler rastgele oluşturulur
    private static void startGame(JFrame frame, JPanel mainPanel, CardLayout cardLayout) {
        GameMap map = new GameMap(25, 20);
        Hero hero = new Hero(4, 4);
        Knight knight = new Knight(12, 10);
        Sorcerer sorcerer = new Sorcerer(18, 5);

        // Kasalar (Crates) odaların/koridorların köşelerinde
        map.placeObject(new domain.models.entity.Crate("Crate", 2, 2), 2, 2);
        map.placeObject(new domain.models.entity.Crate("Crate", 3, 2), 3, 2);
        map.placeObject(new domain.models.entity.Crate("Crate", 22, 17), 22, 17);
        map.placeObject(new domain.models.entity.Crate("Crate", 2, 17), 2, 17);

        // Sandıklar (Chests) uzak köşelerde
        map.placeObject(new domain.models.entity.Chest("Main Chest", 22, 2), 22, 2);
        map.placeObject(new domain.models.entity.Chest("Hidden Chest", 2, 18), 2, 18);

        // Sütunlar (Columns) geniş alanın ortasında bir ana salon oluşturacak şekilde
        map.placeObject(new domain.models.entity.Column("Column", 8, 8, "colon/gray_colon_whole"), 8, 8);
        map.placeObject(new domain.models.entity.Column("Column", 16, 8, "colon/gray_colon_whole"), 16, 8);
        map.placeObject(new domain.models.entity.Column("Column", 8, 12, "colon/gray_colon_whole"), 8, 12);
        map.placeObject(new domain.models.entity.Column("Column", 16, 12, "colon/gray_colon_whole"), 16, 12);

        // Eşyalar
        map.placeObject(new domain.models.item.PotionItem(10, 10), 10, 10);
        map.placeObject(new domain.models.staticObjects.KeyItem(12, 10), 12, 10);
        map.placeObject(new domain.models.item.SwordItem(14, 10), 14, 10);



        // Dekorasyonlar (Torches)
        map.placeObject(new domain.models.staticObjects.Decoration("Torch", 6, 1, "torch/torch_1"), 6, 1);
        map.placeObject(new domain.models.staticObjects.Decoration("Torch", 18, 1, "torch/torch_1"), 18, 1);
        map.placeObject(new domain.models.staticObjects.Decoration("Torch", 1, 10, "torch/torch_1"), 1, 10);
        map.placeObject(new domain.models.staticObjects.Decoration("Torch", 23, 10, "torch/torch_1"), 23, 10);

        List<Entity> entities = new ArrayList<>();
        entities.add(hero);
        entities.add(knight);
        entities.add(sorcerer);

        // Yeni oyunda haritada ve envantertde scroll yok — boş listeler
        List<GameState.ItemRecord> scrollItems = new ArrayList<>();
        List<String> inventoryScrollTypes = new ArrayList<>();
        setupGameView(frame, mainPanel, cardLayout, hero, entities, map, knight, sorcerer, scrollItems, inventoryScrollTypes, null);
    }

    // Kaydedilmiş oyunu yükle — GameState'ten tüm nesneler yeniden oluşturulur
    private static void loadGame(JFrame frame, JPanel mainPanel, CardLayout cardLayout, GameState state) {
        GameMap map = new GameMap(25, 20);

        // Hero oluştur ve durumunu yükle
        Hero hero = new Hero(state.hero.x, state.hero.y);
        hero.setHp(state.hero.hp);
        hero.setMana(state.hero.mana);
        hero.setEnergy(state.hero.energy);
        if (state.hero.str > 0) hero.setStr(state.hero.str); // str bilgisini yükle

        // Kılıcı takılıysa ayarla
        if ("SwordItem".equals(state.hero.equippedWeaponType)) {
            hero.equipWeapon(new domain.models.item.SwordItem(0, 0));
        }

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
                s.setTimeLeft(rec.timeLeft); // Sorcerer ışınlanma timer'ı yükle
                if (!rec.alive) s.takeDamage(999);
                entities.add(s);
                sorcerer = s;
            } else if ("ShadowClone".equals(rec.type)) {
                domain.models.entity.ShadowClone clone = new domain.models.entity.ShadowClone(rec.x, rec.y);
                clone.setTimeLeft(rec.timeLeft);
                if (!rec.alive) clone.takeDamage(999);
                entities.add(clone);
            }
        }

        // Fallback: kayıtta düşman yoksa default pozisyon
        if (knight == null)   { knight   = new Knight(12, 10);  entities.add(knight); }
        if (sorcerer == null) { sorcerer = new Sorcerer(18, 5); entities.add(sorcerer); }

        // Harita itemlarını ayır: scroll'lar ayrı tutulur (inputHandler gerektirir)
        List<GameState.ItemRecord> scrollItems = new ArrayList<>();

        for (GameState.ItemRecord rec : state.mapItems) {
            if ("ShadowCloneScroll".equals(rec.type)) {
                // Scroll'lar setupGameView içinde inputHandler ile birlikte oluşturulur
                scrollItems.add(rec);
            } else {
                domain.models.entity.GameObject item = createItem(rec.type, rec.name, rec.x, rec.y);
                if (item != null) map.placeObject(item, rec.x, rec.y);
            }
        }

        // Envanter itemlarını yeniden oluştur — scroll hariç
        List<String> inventoryScrollTypes = new ArrayList<>();
        for (String type : state.inventoryItems) {
            if ("ShadowCloneScroll".equals(type)) {
                // Scroll inputHandler gerektirir — setupGameView'da oluşturulacak
                inventoryScrollTypes.add(type);
            } else {
                domain.models.entity.GameObject item = createItem(type, 0, 0);
                if (item != null) hero.getInventory().addItem(item);
            }
        }

        setupGameView(frame, mainPanel, cardLayout, hero, entities, map, knight, sorcerer, scrollItems, inventoryScrollTypes, state);
    }

    // Item tip ismine göre nesne oluşturur — scroll hariç (scroll setupGameView'da oluşur)
    private static domain.models.entity.GameObject createItem(String type, String name, int x, int y) {
        String displayName = (name != null && !name.isEmpty()) ? name : type;
        switch (type) {
            case "PotionItem":        return new domain.models.item.PotionItem(x, y);
            case "SwordItem":         return new domain.models.item.SwordItem(x, y);
            case "KeyItem":           return new domain.models.staticObjects.KeyItem(x, y);
            case "Column":            return new domain.models.entity.Column(displayName, x, y);
            case "Crate":             return new domain.models.entity.Crate(displayName, x, y);
            case "Chest":             return new domain.models.entity.Chest(displayName, x, y);
            case "SearchableObject":  return new domain.models.entity.SearchableObject(displayName, x, y);
            default:
                System.err.println("Bilinmeyen item tipi: " + type);
                return null;
        }
    }

    // Eski imza — envanter için (name yok)
    private static domain.models.entity.GameObject createItem(String type, int x, int y) {
        return createItem(type, null, x, y);
    }

    // startGame ve loadGame tarafından ortak kullanılan view/timer/input kurulum
    // scrollItems: haritaya yerleştirilecek scroll kayıtları — inputHandler gerektirdiği için burada oluşturulur
    private static void setupGameView(JFrame frame, JPanel mainPanel, CardLayout cardLayout,
                                      Hero hero, List<Entity> entities, GameMap map,
                                      Knight knight, Sorcerer sorcerer,
                                      List<GameState.ItemRecord> scrollItems,
                                      List<String> inventoryScrollTypes,
                                      GameState state) {
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

        // Eğer save'den yüklenen aktif bir ShadowClone varsa, InputHandler'a kaydet
        for (Entity e : entities) {
            if (e instanceof domain.models.entity.ShadowClone && e.isAlive()) {
                inputHandler.setShadowClone((domain.models.entity.ShadowClone) e);
                break;
            }
        }

        // Scroll'ları şimdi yerleştir — inputHandler hazır, tam işlevsel oluşturulur
        for (GameState.ItemRecord rec : scrollItems) {
            domain.models.item.ShadowCloneScroll scroll =
                    new domain.models.item.ShadowCloneScroll(rec.x, rec.y, entities, map, inputHandler);
            map.placeObject(scroll, rec.x, rec.y);
        }

        // Envanterdeki scroll'ları da şimdi oluştur — inputHandler hazır
        for (String type : inventoryScrollTypes) {
            if ("ShadowCloneScroll".equals(type)) {
                hero.getInventory().addItem(
                        new domain.models.item.ShadowCloneScroll(0, 0, entities, map, inputHandler)
                );
            }
        }

        // EnemySpawner ve ScrollSpawner — timer'lar state'ten yüklenir (load game durumuysa)
        EnemySpawner spawner = new EnemySpawner(map);
        ScrollSpawner scrollSpawner = new ScrollSpawner(map, entities, inputHandler);

        if (state != null) {
            spawner.setTimeLeft(state.enemySpawnTimeLeft);
            scrollSpawner.setTimeLeft(state.scrollSpawnTimeLeft);
        }

        // Timer referans tutucular — lambda içinden timer'a erişmek için (pause/resume)
        final javax.swing.Timer[] logicRef  = new javax.swing.Timer[1];
        final javax.swing.Timer[] renderRef = new javax.swing.Timer[1];

        // PauseMenu — JFrame glass pane olarak oyunun üstüne bindiriliyor
        view.PauseMenu pauseMenu = new view.PauseMenu(
                hero, entities, map, spawner, scrollSpawner,
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