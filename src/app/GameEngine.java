package app;
import domain.models.entity.Hero;
import domain.models.staticObjects.Column;
import domain.models.staticObjects.Chest;
import domain.models.staticObjects.Crate;
import domain.models.staticObjects.SearchableObject;
import domain.models.GameObject;

import domain.logic.EnemySpawner;
import domain.logic.GameObjectFactory;
import domain.logic.LevelManager;
import domain.logic.ScrollSpawner;
import domain.logic.event.FloatingTextEvent;
import domain.logic.event.GameEventBus;
import domain.logic.event.GameEventListener;
import domain.logic.event.SoundEvent;
import domain.logic.event.TrapFlashEvent;
import domain.models.GameState;
import domain.models.entity.*;
import domain.models.map.GameMap;
import domain.models.item.*;
import domain.models.item.usables.*;
import domain.models.item.usables.HealthPotion;
import domain.models.item.usables.ManaPotion;
import domain.models.item.usables.EnergyPotion;
import domain.models.item.wearables.*;
import java.awt.CardLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import view.DesignModeView;
import view.AssetManager;
import view.GameView;
import view.TileManager;

public class GameEngine {
    private static GameMap initialDesignedMap = null;
    private static domain.models.GameMode activeGameMode = domain.models.GameMode.ADVENTURE;
    private static LevelManager levelManager = null;

    private static GameMap cloneMap(GameMap original, boolean isRestart) {
        if (original == null)
            return null;
        int w = original.getWidth();
        int h = original.getHeight();
        GameMap copy = new GameMap(w, h);

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                domain.models.GameObject obj = original.getObjectAt(x, y);
                if (obj == null)
                    continue;

                // Prototype Pattern in action!
                domain.models.GameObject clonedObj = obj.clone();
                clonedObj.setMap(copy); // Ensure map reference points to new map

                // If it's a restart, we might need to reset certain states like searchable traps
                if (isRestart && clonedObj instanceof domain.models.staticObjects.SearchableObject) {
                    domain.models.staticObjects.SearchableObject so = (domain.models.staticObjects.SearchableObject) clonedObj;
                    so.setSearched(false);
                    so.setTrapTriggered(false);
                    so.setHiddenItem(null);
                } else if (isRestart && clonedObj instanceof domain.models.staticObjects.Crate) {
                    domain.models.staticObjects.Crate crate = (domain.models.staticObjects.Crate) clonedObj;
                    crate.setHiddenItem(null);
                }
                
                // For WallTiles with SearchableObject decorations, also reset
                if (isRestart && clonedObj instanceof domain.models.tile.WallTile) {
                    domain.models.tile.WallTile wall = (domain.models.tile.WallTile) clonedObj;
                    if (wall.getDecoration() instanceof domain.models.staticObjects.SearchableObject) {
                        domain.models.staticObjects.SearchableObject so = (domain.models.staticObjects.SearchableObject) wall.getDecoration();
                        so.setSearched(false);
                        so.setTrapTriggered(false);
                        so.setHiddenItem(null);
                    }
                }

                copy.placeObject(clonedObj, x, y);
            }
        }
        return copy;
    }

    private static void restartGame(JFrame frame, JPanel mainPanel, CardLayout cardLayout) {
        if (initialDesignedMap != null) {
            GameMap cleanMap = cloneMap(initialDesignedMap, true);
            startGameWithMap(frame, mainPanel, cardLayout, cleanMap, activeGameMode);
        } else {
            startGame(frame, mainPanel, cardLayout);
        }
    }

    // ── Design Mode ──────────────────────────────────────────────────────────
    public static void startDesignMode(JFrame frame, JPanel mainPanel, CardLayout cardLayout) {
        GameMap map = new GameMap(30, 20);

        // Kapının yeri oyunda build mode açıldığı anda direkt atanmış olarak gelsin
        // (Rastgele 1 kapı yerleştirilir, en sağ/sol tile'lar hariç)
        java.util.Random rand = new java.util.Random();
        int doorX = rand.nextInt(map.getWidth() - 4) + 2;
        map.placeObject(new domain.models.staticObjects.LevelDoor("Level Gate", doorX, 0), doorX, 0);
        map.placeObject(new domain.models.tile.FloorTile(), doorX, 1); // Kapı önü boş olacak

        TileManager tileManager = new TileManager();

        DesignModeView designView = new DesignModeView(
                map,
                tileManager,
                // onBackToMenu
                () -> cardLayout.show(mainPanel, "Menu"),
                // onPlayMap — tasarımlanan map ile oyunu başlat (Adventure)
                (designedMap) -> {
                    mainPanel.remove(mainPanel.getComponentCount() > 0
                            ? getComponentByName(mainPanel, "Design")
                            : null);
                    startGameWithMap(frame, mainPanel, cardLayout, designedMap, domain.models.GameMode.ADVENTURE);
                },
                // onPlayTeamMatchMap - Team Match başlat
                (designedMap) -> {
                    mainPanel.remove(mainPanel.getComponentCount() > 0
                            ? getComponentByName(mainPanel, "Design")
                            : null);
                    startGameWithMap(frame, mainPanel, cardLayout, designedMap, domain.models.GameMode.TEAM_MATCH);
                });
        designView.setPreferredSize(new java.awt.Dimension(1250, 800));

        // Önceki design panelini temizle (varsa)
        for (java.awt.Component c : mainPanel.getComponents()) {
            if ("Design".equals(c.getName())) {
                mainPanel.remove(c);
                break;
            }
        }
        designView.setName("Design");
        mainPanel.add(designView, "Design");
        cardLayout.show(mainPanel, "Design");
        frame.pack();
        designView.requestFocusInWindow();
    }

    /** Tasarımlanan map ile hero + enemies ekleyerek oyunu başlatır */
    public static void startGameWithMap(JFrame frame, JPanel mainPanel, CardLayout cardLayout, GameMap map,
            domain.models.GameMode mode) {
        activeGameMode = mode;

        if (mode == domain.models.GameMode.TEAM_MATCH) {
            initialDesignedMap = cloneMap(map, false);
            startTeamMatchWithMap(frame, mainPanel, cardLayout, map);
            return;
        }

        // Place static Level Door and Level Key for Adventure mode progression
        LevelManager.placeRandomLevelDoor(map, "Level Gate");

        // ITEM HIDING SYSTEM: Hide the LevelKey
        LevelManager.hideLevelKey(map);

        // Save the map clone AFTER door and hidden key setups are established so that
        // restarting yields the exact same layout.
        initialDesignedMap = cloneMap(map, false);

        // Initialize level manager for level progression
        levelManager = new LevelManager();

        Hero hero = new Hero(4, 4);
        hero.setCurrentMap(map);
        Knight knight = new Knight(12, 10);
        Sorcerer sorcerer = new Sorcerer(18, 5);

        List<Entity> entities = new ArrayList<>();
        entities.add(hero);
        entities.add(knight);
        entities.add(sorcerer);

        List<GameState.ItemRecord> scrollItems = new ArrayList<>();
        List<String> inventoryScrollTypes = new ArrayList<>();
        setupGameView(frame, mainPanel, cardLayout, hero, entities, map, knight, sorcerer,
                scrollItems, inventoryScrollTypes, null, mode);
    }

    private static void startTeamMatchWithMap(JFrame frame, JPanel mainPanel, CardLayout cardLayout, GameMap map) {
        // Initialize level manager for Team Match (no level progression, but needed for save/load)
        levelManager = new LevelManager();

        List<Entity> entities = new ArrayList<>();

        // Split map into left and right halves (ignore walls on borders)
        int w = map.getWidth();
        int h = map.getHeight();

        java.util.List<int[]> leftTiles = new ArrayList<>();
        java.util.List<int[]> rightTiles = new ArrayList<>();

        for (int x = 1; x < w - 1; x++) {
            for (int y = 1; y < h - 1; y++) {
                if (map.isWalkable(x, y)) {
                    if (x < w / 2) {
                        leftTiles.add(new int[] { x, y });
                    } else {
                        rightTiles.add(new int[] { x, y });
                    }
                }
            }
        }

        java.util.Collections.shuffle(leftTiles);
        java.util.Collections.shuffle(rightTiles);

        // Spawn Orange Team (Team 1) on Left Half: 1 Sorcerer, 3 Knights
        Sorcerer s1 = new Sorcerer(leftTiles.get(0)[0], leftTiles.get(0)[1]);
        s1.setTeam(domain.models.Team.ORANGE);
        entities.add(s1);
        for (int i = 0; i < 3; i++) {
            Knight k = new Knight(leftTiles.get(i + 1)[0], leftTiles.get(i + 1)[1]);
            k.setTeam(domain.models.Team.ORANGE);
            entities.add(k);
        }

        // Spawn Cyan Team (Team 2) on Right Half: 1 Sorcerer, 2 Knights, 1 Hero
        Hero hero = new Hero(rightTiles.get(0)[0], rightTiles.get(0)[1]);
        hero.setCurrentMap(map);
        hero.setTeam(domain.models.Team.CYAN);
        entities.add(hero);

        Sorcerer s2 = new Sorcerer(rightTiles.get(1)[0], rightTiles.get(1)[1]);
        s2.setTeam(domain.models.Team.CYAN);
        entities.add(s2);

        for (int i = 0; i < 2; i++) {
            Knight k = new Knight(rightTiles.get(i + 2)[0], rightTiles.get(i + 2)[1]);
            k.setTeam(domain.models.Team.CYAN);
            entities.add(k);
        }

        // Spawn 6 Random Weapons
        java.util.List<int[]> allTiles = new ArrayList<>(leftTiles);
        allTiles.addAll(rightTiles);
        java.util.Collections.shuffle(allTiles);
        for (int i = 0; i < 6; i++) {
            MapItem weapon = MapItem.createRandomWeapon(allTiles.get(i)[0],
                    allTiles.get(i)[1]);
            map.placeObject(weapon, allTiles.get(i)[0], allTiles.get(i)[1]);
        }

        List<GameState.ItemRecord> scrollItems = new ArrayList<>();
        List<String> inventoryScrollTypes = new ArrayList<>();
        setupGameView(frame, mainPanel, cardLayout, hero, entities, map, null, null,
                scrollItems, inventoryScrollTypes, null, domain.models.GameMode.TEAM_MATCH);
    }

    /** Adına göre panel bul (null-safe) */
    private static java.awt.Component getComponentByName(java.awt.Container container, String name) {
        if (name == null)
            return null;
        for (java.awt.Component c : container.getComponents()) {
            if (name.equals(c.getName()))
                return c;
        }
        return null;
    }

    // Yeni oyun — tüm nesneler rastgele oluşturulur
    public static void startGame(JFrame frame, JPanel mainPanel, CardLayout cardLayout) {
        initialDesignedMap = null;
        activeGameMode = domain.models.GameMode.ADVENTURE;
        GameMap map = new GameMap(22, 16);
        Hero hero = new Hero(4, 4);
        hero.setCurrentMap(map);
        Knight knight = new Knight(12, 8);
        Sorcerer sorcerer = new Sorcerer(18, 5);

        // Kasalar (Crates) odaların/koridorların köşelerinde
        map.placeObject(new domain.models.staticObjects.Crate("Crate", 2, 2), 2, 2);
        map.placeObject(new domain.models.staticObjects.Crate("Crate", 3, 2), 3, 2);
        map.placeObject(new domain.models.staticObjects.Crate("Crate", 19, 13), 19, 13);
        map.placeObject(new domain.models.staticObjects.Crate("Crate", 2, 13), 2, 13);

        // Sandıklar (Chests) uzak köşelerde
        map.placeObject(new domain.models.staticObjects.Chest("Main Chest", 19, 2, true), 19, 2);
        map.placeObject(new domain.models.staticObjects.Chest("Hidden Chest", 2, 12, true), 2, 12);

        // Sütunlar (Columns) geniş alanın ortasında bir ana salon oluşturacak şekilde
        map.placeObject(new domain.models.staticObjects.Column("Column", 8, 5, "colon/gray_colon_whole"), 8, 5);
        map.placeObject(new domain.models.staticObjects.Column("Column", 14, 5, "colon/gray_colon_whole"), 14, 5);
        map.placeObject(new domain.models.staticObjects.Column("Column", 8, 10, "colon/gray_colon_whole"), 8, 10);
        map.placeObject(new domain.models.staticObjects.Column("Column", 14, 10, "colon/gray_colon_whole"), 14, 10);

        // Eşyalar
        map.placeObject(
                new PotionItem(new HealthPotion("Red Potion", 5),
                        9, 5, "images/items/potion/red_potion.png"),
                9, 5);
        map.placeObject(new domain.models.item.KeyItem(10, 8), 10, 8);
        map.placeObject(new domain.models.item.KeyItem(10, 9), 10, 9);
        map.placeObject(new SwordItem(12, 8), 12, 8);

        // =====================================================================
        // TODO: TEMPORARY DEVELOPMENT TEST DROPS - GEÇİCİ GELİŞTİRİCİ TEST SİLAHLARI VE
        // EŞYALARI
        // Bu blok sadece test aşamasında tüm yeni silahları ve giyilebilir eşyaları
        // kolayca denemek için eklenmiştir.
        // Yay, Balta, Asa, Katana, Elmas Kılıç, Çelik Zırh ve Güç Yüzüğü kahramanın
        // etrafında yer alır.
        map.placeObject(new WoodenSwordItem(4, 5), 4, 5);
        map.placeObject(new AxeItem(5, 4), 5, 4);
        map.placeObject(new BowItem(5, 5), 5, 5);
        map.placeObject(new FireWandItem(3, 4), 3, 4);
        map.placeObject(new SamuraiSwordItem(3, 5), 3, 5);
        map.placeObject(new DiamondSwordItem(5, 3), 5, 3);
        map.placeObject(new ArmorItem(4, 3), 4, 3);
        map.placeObject(new RingItem(3, 3), 3, 3);
        // =====================================================================

        // Dekorasyonlar (Torches)
        map.placeObject(new domain.models.staticObjects.Decoration("Torch", 6, 1, "torch/torch_1"), 6, 1);
        map.placeObject(new domain.models.staticObjects.Decoration("Torch", 16, 1, "torch/torch_1"), 16, 1);

        // Level Door (to Level 2: The Depths) - Kilitli, Level Key gerektirir
        LevelManager.placeRandomLevelDoor(map, "Level Gate");

        // WallObjects (Chains on top wall)
        map.placeObject(new domain.models.staticObjects.WallObject("Chain 1", 10, 1, "images/WallObjects/chain1.png"),
                10, 1);
        map.placeObject(new domain.models.staticObjects.WallObject("Chain 2", 18, 1, "images/WallObjects/chain2.png"),
                18, 1);
        map.placeObject(new domain.models.staticObjects.WallObject("Chain 3", 26, 1, "images/WallObjects/chain3.png"),
                26, 1);

        // Çıkış Kapısı (Exit Door) - Kilitli olarak yerleştirildi!
        map.placeObject(new domain.models.staticObjects.Door("Exit Door", 35, 8, true), 35, 8);
        map.placeObject(new domain.models.staticObjects.Decoration("Torch", 1, 8, "torch/torch_1"), 1, 8);
        map.placeObject(new domain.models.staticObjects.Decoration("Torch", 36, 8, "torch/torch_1"), 36, 8);

        List<Entity> entities = new ArrayList<>();
        entities.add(hero);
        entities.add(knight);
        entities.add(sorcerer);

        // Yeni oyunda haritada ve envantertde scroll yok — boş listeler
        List<GameState.ItemRecord> scrollItems = new ArrayList<>();
        List<String> inventoryScrollTypes = new ArrayList<>();
        setupGameView(frame, mainPanel, cardLayout, hero, entities, map, knight, sorcerer, scrollItems,
                inventoryScrollTypes, null, domain.models.GameMode.ADVENTURE);
    }

    // Kaydedilmiş oyunu yükle — GameState'ten tüm nesneler yeniden oluşturulur
    public static void loadGame(JFrame frame, JPanel mainPanel, CardLayout cardLayout, GameState state) {
        initialDesignedMap = null;
        // Restore the game mode from save (default ADVENTURE for old saves)
        domain.models.GameMode loadedMode = domain.models.GameMode.ADVENTURE;
        if ("TEAM_MATCH".equals(state.gameMode)) {
            loadedMode = domain.models.GameMode.TEAM_MATCH;
        }
        activeGameMode = loadedMode;

        // Initialize level manager with loaded level
        levelManager = new LevelManager();
        levelManager.setCurrentLevel(state.currentLevel);

        GameMap map = new GameMap(
                state.mapWidth > 0 ? state.mapWidth : 22,
                state.mapHeight > 0 ? state.mapHeight : 16);

        // Hero oluştur ve durumunu yükle
        Hero hero = new Hero(state.hero.x, state.hero.y);
        hero.setCurrentMap(map);
        hero.setHp(state.hero.hp);
        hero.setMana(state.hero.mana);
        hero.setEnergy(state.hero.energy);
        if (state.hero.str > 0)
            hero.setStr(state.hero.str);
        // Hero team bilgisini restore et
        if (state.hero.team != null && !state.hero.team.isEmpty() && !"NONE".equals(state.hero.team)) {
            try { hero.setTeam(domain.models.Team.valueOf(state.hero.team)); } catch (Exception ignored) {}
        } else if (loadedMode == domain.models.GameMode.TEAM_MATCH) {
            // Eski format save: Team Match'te hero varsayılan olarak CYAN
            hero.setTeam(domain.models.Team.CYAN);
        }

        // Kılıcı veya kuşanılmış silahı takılıysa ayarla
        if (state.hero.equippedWeaponType != null && !state.hero.equippedWeaponType.isEmpty()) {
            domain.models.GameObject weapon = GameObjectFactory.create(state.hero.equippedWeaponType, null, 0, 0);
            if (weapon instanceof MapItem) {
                hero.equipWeapon((MapItem) weapon);
            }
        }
        // Kuşanılmış zırhı takılıysa ayarla
        if (state.hero.equippedArmorType != null && !state.hero.equippedArmorType.isEmpty()) {
            domain.models.GameObject armor = GameObjectFactory.create(state.hero.equippedArmorType, null, 0, 0);
            if (armor instanceof MapItem) {
                hero.equipArmor((MapItem) armor);
            }
        }
        // Kuşanılmış yüzüğü takılıysa ayarla
        if (state.hero.equippedRingType != null && !state.hero.equippedRingType.isEmpty()) {
            domain.models.GameObject ring = GameObjectFactory.create(state.hero.equippedRingType, null, 0, 0);
            if (ring instanceof MapItem) {
                hero.equipRing((MapItem) ring);
            }
        }

        // Düşmanları yeniden oluştur
        Knight knight = null;
        Sorcerer sorcerer = null;
        List<Entity> entities = new ArrayList<>();
        entities.add(hero);

        int cyanKnightsAssigned = 0;
        int cyanSorcerersAssigned = 0;

        for (GameState.EnemyRecord rec : state.enemies) {
            if ("Knight".equals(rec.type)) {
                Knight k = new Knight(rec.x, rec.y);
                k.setHp(rec.hp);
                if (!rec.alive) k.takeDamage(999);
                // Team bilgisini restore et
                if (rec.team != null && !"NONE".equals(rec.team)) {
                    try { k.setTeam(domain.models.Team.valueOf(rec.team)); } catch (Exception ignored) {}
                } else if (loadedMode == domain.models.GameMode.TEAM_MATCH) {
                    if (cyanKnightsAssigned < 2) {
                        k.setTeam(domain.models.Team.CYAN);
                        cyanKnightsAssigned++;
                    } else {
                        k.setTeam(domain.models.Team.ORANGE);
                    }
                }
                entities.add(k);
                if (knight == null) knight = k;
            } else if ("Sorcerer".equals(rec.type)) {
                Sorcerer s = new Sorcerer(rec.x, rec.y);
                s.setHp(rec.hp);
                s.setTimeLeft(rec.timeLeft);
                s.setProjectileTimeLeft(rec.projectileTimeLeft);
                if (!rec.alive) s.takeDamage(999);
                // Team bilgisini restore et
                if (rec.team != null && !"NONE".equals(rec.team)) {
                    try { s.setTeam(domain.models.Team.valueOf(rec.team)); } catch (Exception ignored) {}
                } else if (loadedMode == domain.models.GameMode.TEAM_MATCH) {
                    if (cyanSorcerersAssigned < 1) {
                        s.setTeam(domain.models.Team.CYAN);
                        cyanSorcerersAssigned++;
                    } else {
                        s.setTeam(domain.models.Team.ORANGE);
                    }
                }
                entities.add(s);
                if (sorcerer == null) sorcerer = s;
            } else if ("ShadowClone".equals(rec.type)) {
                domain.models.entity.ShadowClone clone = new domain.models.entity.ShadowClone(rec.x, rec.y);
                clone.setTimeLeft(rec.timeLeft);
                if (!rec.alive) clone.takeDamage(999);
                entities.add(clone);
            } else if ("FinalBoss".equals(rec.type)) {
                domain.models.entity.FinalBoss fb = new domain.models.entity.FinalBoss(rec.x, rec.y);
                fb.setHp(rec.hp);
                fb.setTeleportTimeLeft(rec.timeLeft);
                fb.setProjectileTimeLeft(rec.projectileTimeLeft);
                fb.setPhase80Triggered(rec.hp < 80);
                fb.setPhase60Triggered(rec.hp < 60);
                fb.setPhase40Triggered(rec.hp < 40);
                fb.setPhase20Triggered(rec.hp < 20);
                if (!rec.alive) fb.takeDamage(999);
                entities.add(fb);
            }
        }

        // Fallback: Adventure modunda kayıtta düşman yoksa default pozisyon
        // Team Match modunda fallback eklenmez — takımlar save'de mükemmel yüklenir
        if (loadedMode == domain.models.GameMode.ADVENTURE) {
            if (knight == null) {
                knight = new Knight(12, 10);
                entities.add(knight);
            }
            if (sorcerer == null) {
                sorcerer = new Sorcerer(18, 5);
                entities.add(sorcerer);
            }
        }

        // Kaydedilmiş uçan mermileri yeniden oluştur
        if (state.projectiles != null) {
            for (GameState.ProjectileRecord pr : state.projectiles) {
                Entity owner = pr.heroOwned ? hero : sorcerer;
                entities.add(new domain.models.entity.Projectile(
                        pr.x, pr.y, pr.exactX, pr.exactY, pr.deltaX, pr.deltaY, pr.damage, owner, pr.type));
            }
        }

        // Harita itemlarını ayır: scroll'lar ayrı tutulur (inputHandler gerektirir)
        List<GameState.ItemRecord> scrollItems = new ArrayList<>();

        for (GameState.ItemRecord rec : state.mapItems) {
            if ("ShadowCloneScroll".equals(rec.type)) {
                // Scroll'lar setupGameView içinde inputHandler ile birlikte oluşturulur
                scrollItems.add(rec);
            } else if ("SearchableObject".equals(rec.type)) {
                domain.models.GameObject item = GameObjectFactory.create(rec.type, rec.name, rec.x, rec.y, rec.isLocked, rec.imageName);
                if (item instanceof domain.models.staticObjects.SearchableObject) {
                    domain.models.staticObjects.SearchableObject so = (domain.models.staticObjects.SearchableObject) item;
                    so.setSearched(rec.searched);
                    if (rec.hiddenItemType != null) {
                        if (rec.hiddenItemType.equals("LevelKey"))
                            so.setHiddenItem(new domain.models.item.LevelKey(rec.x, rec.y));
                        else if (rec.hiddenItemType.equals("KeyItem"))
                            so.setHiddenItem(new domain.models.item.KeyItem(rec.x, rec.y));
                    }
                }
                domain.models.GameObject existing = map.getObjectAt(rec.x, rec.y);
                if (existing instanceof domain.models.tile.WallTile) {
                    ((domain.models.tile.WallTile) existing).setDecoration(item);
                } else {
                    map.placeObject(item, rec.x, rec.y);
                }
            } else {
                domain.models.GameObject item = GameObjectFactory.create(rec.type, rec.name, rec.x, rec.y, rec.isLocked, rec.imageName);
                if (item instanceof domain.models.staticObjects.Crate) {
                    domain.models.staticObjects.Crate crate = (domain.models.staticObjects.Crate) item;
                    if (rec.hiddenItemType != null) {
                        if (rec.hiddenItemType.equals("LevelKey"))
                            crate.setHiddenItem(new domain.models.item.LevelKey(rec.x, rec.y));
                        else if (rec.hiddenItemType.equals("KeyItem"))
                            crate.setHiddenItem(new domain.models.item.KeyItem(rec.x, rec.y));
                    }
                }
                if (item != null)
                    map.placeObject(item, rec.x, rec.y);
            }
        }

        // Envanter itemlarını yeniden oluştur — scroll hariç
        List<String> inventoryScrollTypes = new ArrayList<>();
        for (GameState.ItemRecord rec : state.inventoryItems) {
            String type = rec.type;
            if ("ShadowCloneScroll".equals(type)) {
                // Scroll inputHandler gerektirir — setupGameView'da oluşturulacak
                inventoryScrollTypes.add(type);
            } else {
                domain.models.GameObject item = GameObjectFactory.create(type, rec.name, 0, 0, rec.isLocked, rec.imageName);
                if (item != null)
                    hero.getInventory().addItem(item);
            }
        }

        setupGameView(frame, mainPanel, cardLayout, hero, entities, map, knight, sorcerer, scrollItems,
                inventoryScrollTypes, state, loadedMode);
    }



    // startGame ve loadGame tarafından ortak kullanılan view/timer/input kurulum
    // scrollItems: haritaya yerleştirilecek scroll kayıtları — inputHandler
    // gerektirdiği için burada oluşturulur
    public static void setupGameView(JFrame frame, JPanel mainPanel, CardLayout cardLayout,
            Hero hero, List<Entity> entities, GameMap map,
            Knight knight, Sorcerer sorcerer,
            List<GameState.ItemRecord> scrollItems,
            List<String> inventoryScrollTypes,
            GameState state,
            domain.models.GameMode mode) {

        // ── GameEventBus wiring ───────────────────────────────────────────────
        // Clear any listeners from a previous game session to avoid duplicates.
        GameEventBus.clearListeners();

        // This listener will be replaced with the real GameView instance below,
        // but we register a temporary no-op so compile won't complain.
        // The real listener is registered after gameView is created (see below).

        if (state == null && mode == domain.models.GameMode.ADVENTURE) {

            boolean hasLevelDoor = false;
            for (int x = 0; x < map.getWidth(); x++) {
                for (int y = 0; y < map.getHeight(); y++) {
                    if (map.getObjectAt(x, y) instanceof domain.models.staticObjects.LevelDoor) {
                        hasLevelDoor = true;
                        break;
                    }
                }
                if (hasLevelDoor)
                    break;
            }
            if (hasLevelDoor) {
                LevelManager.hideLevelKey(map);
            }
        }

        final GameMap[] mapRef = new GameMap[] { map };
        AssetManager assetManager = AssetManager.getInstance();
        TileManager tileManager = new TileManager();

        GameView gameView = new GameView(hero, assetManager);
        gameView.setGameMode(mode);
        gameView.setPreferredSize(new java.awt.Dimension(832, 640));
        gameView.setGameMap(map);
        gameView.setTileManager(tileManager);
        // Only set single enemies for backwards compatibility in adventure mode
        if (mode == domain.models.GameMode.ADVENTURE && knight != null && sorcerer != null) {
            gameView.setEnemies(knight, sorcerer);
        }
        gameView.setEntityList(entities);

        // ── GameEventBus listeners ────────────────────────────────────────────
        // Now that gameView is created, register listeners that bridge domain
        // events to the view and sound layers (Model-View Separation / Observer).
        final GameView gvRef = gameView;
        GameEventBus.addListener(event -> {
            if (event instanceof FloatingTextEvent) {
                FloatingTextEvent fte = (FloatingTextEvent) event;
                GameView.addFloatingText((int) fte.getX(), (int) fte.getY(), fte.getText(), fte.getColor());
            } else if (event instanceof SoundEvent) {
                SoundEvent se = (SoundEvent) event;
                switch (se.getSoundType()) {
                    case WALK:      util.helpers.SoundManager.playWalk(); break;
                    case SWING:     util.helpers.SoundManager.playSwing(); break;
                    case HEAL:      util.helpers.SoundManager.playHeal(); break;
                    case SHOOT:     util.helpers.SoundManager.playShoot(); break;
                    case UNLOCK:    util.helpers.SoundManager.playUnlock(); break;
                    case ENEMY_HIT: util.helpers.SoundManager.playEnemyHit(); break;
                    case VICTORY:   util.helpers.SoundManager.playVictory(); break;
                }
            } else if (event instanceof TrapFlashEvent) {
                TrapFlashEvent tfe = (TrapFlashEvent) event;
                GameView.trapFlashFrames = tfe.getFrames();
            }
        });

        mainPanel.add(gameView, "Game");
        cardLayout.show(mainPanel, "Game");

        view.ActionMenu actionMenu = new view.ActionMenu(hero);
        gameView.setActionMenu(actionMenu);
        controller.MouseHandler mouseHandler = new controller.MouseHandler(hero, map, gameView, actionMenu);
        gameView.addMouseListener(mouseHandler);
        gameView.addMouseMotionListener(mouseHandler);
        gameView.addMouseWheelListener(mouseHandler);

        controller.InputHandler inputHandler = new controller.InputHandler(hero, map, entities, gameView);
        inputHandler.setGameMode(mode);
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

        // Design mode'dan gelen "Shadow Clone" PotionItem'larını gerçek
        // ShadowCloneScroll'a dönüştür
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                domain.models.GameObject obj = map.getObjectAt(x, y);
                if (obj instanceof domain.models.item.usables.PotionItem
                        && "Shadow Clone".equals(obj.getName())) {
                    ShadowCloneScroll scroll = new ShadowCloneScroll(x, y, entities, map, inputHandler);
                    map.placeObject(scroll, x, y);
                }
            }
        }

        // Scroll'ları şimdi yerleştir — inputHandler hazır, tam işlevsel oluşturulur
        for (GameState.ItemRecord rec : scrollItems) {
            ShadowCloneScroll scroll = new ShadowCloneScroll(
                    rec.x, rec.y,
                    entities, map, inputHandler);
            map.placeObject(scroll, rec.x, rec.y);
        }

        // Envanterdeki scroll'ları da şimdi oluştur — inputHandler hazır
        for (String type : inventoryScrollTypes) {
            if ("ShadowCloneScroll".equals(type)) {
                hero.getInventory().addItem(
                        new ShadowCloneScroll(0, 0, entities, map, inputHandler));
            }
        }

        // EnemySpawner ve ScrollSpawner — timer'lar state'ten yüklenir (load game
        // durumuysa)
        EnemySpawner spawner = new EnemySpawner(map);
        ScrollSpawner scrollSpawner = new ScrollSpawner(map, entities, inputHandler);

        if (state != null) {
            spawner.setTimeLeft(state.enemySpawnTimeLeft);
            scrollSpawner.setTimeLeft(state.scrollSpawnTimeLeft);
        }

        // Timer referans tutucular — lambda içinden timer'a erişmek için (pause/resume)
        final javax.swing.Timer[] logicRef = new javax.swing.Timer[1];
        final javax.swing.Timer[] renderRef = new javax.swing.Timer[1];

        // GameOverMenu — JFrame glass pane olarak Hero ölünce veya kazanılınca bindirilecek
        final view.GameOverMenu gameOverMenu = new view.GameOverMenu(
                () -> {
                    if (logicRef[0] != null)
                        logicRef[0].stop();
                    if (renderRef[0] != null)
                        renderRef[0].stop();
                    restartGame(frame, mainPanel, cardLayout);
                },
                (loadedState) -> {
                    if (logicRef[0] != null)
                        logicRef[0].stop();
                    if (renderRef[0] != null)
                        renderRef[0].stop();
                    loadGame(frame, mainPanel, cardLayout, loadedState);
                },
                () -> {
                    if (logicRef[0] != null)
                        logicRef[0].stop();
                    if (renderRef[0] != null)
                        renderRef[0].stop();
                    cardLayout.show(mainPanel, "Menu");
                });

        final long[] totalElapsedTimeMs = new long[] { state != null ? state.elapsedSeconds * 1000 : 0 };
        final long[] lastTickTime = new long[] { System.currentTimeMillis() };

        final Runnable advanceLevelRunnable = () -> {
            if (levelManager != null) {
                // Freeze the game first by stopping the timers
                if (logicRef[0] != null)
                    logicRef[0].stop();
                if (renderRef[0] != null)
                    renderRef[0].stop();

                GameMap newMap = levelManager.advanceLevel();
                if (newMap != null) {
                    mapRef[0] = newMap;
                    hero.setCurrentMap(newMap);

                    hero.setPosition(2, 2);
                    newMap.placeObject(hero, 2, 2);

                    gameView.setGameMap(newMap);
                    inputHandler.setGameMap(newMap);
                    mouseHandler.setGameMap(newMap);
                    spawner.setGameMap(newMap);
                    scrollSpawner.setGameMap(newMap);

                    entities.clear();
                    entities.add(hero);

                    // Remove the Skull Key (LevelKey) from the hero's inventory upon level completion
                    if (hero.getInventory() != null) {
                        java.util.List<domain.models.GameObject> toRemove = new java.util.ArrayList<>();
                        for (domain.models.GameObject item : hero.getInventory().getItems()) {
                            if (item instanceof domain.models.item.LevelKey) {
                                toRemove.add(item);
                            }
                        }
                        for (domain.models.GameObject key : toRemove) {
                            hero.getInventory().removeItem(key);
                            System.out.println("Removed Skull Key from inventory on level transition.");
                        }
                    }

                    levelManager.populateEnemies(levelManager.getCurrentLevel(), newMap, entities, hero);
                    spawner.clearSpawnedEnemies();
                    inputHandler.setShadowClone(null);

                    view.GameView.addFloatingText(2, 2, "Level " + levelManager.getCurrentLevel(),
                            java.awt.Color.CYAN);
                    System.out.println("Transitioned to Level " + levelManager.getCurrentLevel());
                    gameView.repaint();
                }

                // Resume the timers
                if (logicRef[0] != null) {
                    lastTickTime[0] = System.currentTimeMillis();
                    logicRef[0].start();
                }
                if (renderRef[0] != null)
                    renderRef[0].start();
            }
        };
        domain.models.staticObjects.LevelDoor.setOpenCallback(advanceLevelRunnable);

        // Victory Coin callback to trigger victory sequence
        VictoryCoin.setVictoryCallback(() -> {
            if (logicRef[0] != null)
                logicRef[0].stop();
            if (renderRef[0] != null)
                renderRef[0].stop();
            inputHandler.disableInput();

            util.helpers.SoundManager.playVictory();

            gameOverMenu.setupGameOverMenu("YOU WIN", "You have conquered the Dungeon!", false, true);
            frame.setGlassPane(gameOverMenu);
            gameOverMenu.setVisible(true);
        });

        // PauseMenu — JFrame glass pane olarak oyunun üstüne bindiriliyor
        view.PauseMenu pauseMenu = new view.PauseMenu(
                hero, entities, map, spawner, scrollSpawner, levelManager, gameView,
                mode,
                () -> {
                    lastTickTime[0] = System.currentTimeMillis();
                    if (logicRef[0] != null)
                        logicRef[0].start();
                    if (renderRef[0] != null)
                        renderRef[0].start();
                    gameView.requestFocusInWindow();
                },
                () -> {
                    if (logicRef[0] != null)
                        logicRef[0].stop();
                    if (renderRef[0] != null)
                        renderRef[0].stop();
                    restartGame(frame, mainPanel, cardLayout);
                },
                () -> {
                    if (logicRef[0] != null)
                        logicRef[0].stop();
                    if (renderRef[0] != null)
                        renderRef[0].stop();
                    cardLayout.show(mainPanel, "Menu");
                });
        frame.setGlassPane(pauseMenu);

        // GameOverMenu glass pane reference is setup earlier

        // ESC tuşu — pause/resume toggle
        gameView.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke("ESCAPE"), "togglePause");
        gameView.getActionMap().put("togglePause", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (gameOverMenu.isVisible())
                    return; // Game over aktifken ESC basılamaz

                boolean paused = pauseMenu.isVisible();
                if (paused) {
                    pauseMenu.setVisible(false);
                    lastTickTime[0] = System.currentTimeMillis();
                    if (logicRef[0] != null)
                        logicRef[0].start();
                    if (renderRef[0] != null)
                        renderRef[0].start();
                    gameView.requestFocusInWindow();
                } else {
                    if (logicRef[0] != null)
                        logicRef[0].stop();
                    if (renderRef[0] != null)
                        renderRef[0].stop();
                    pauseMenu.setVisible(true);
                }
            }
        });

        GameLoop gameLoop = new GameLoop(hero, entities, mapRef, mode, inputHandler, spawner,
                scrollSpawner, levelManager, gameView, frame, gameOverMenu,
                totalElapsedTimeMs[0], advanceLevelRunnable);

        logicRef[0] = gameLoop.getLogicTimer();
        renderRef[0] = gameLoop.getRenderTimer();

        mouseHandler.setTimers(logicRef, renderRef);
        gameLoop.start();
    }


}