import domain.logic.EnemySpawner;
import domain.logic.ScrollSpawner;
import domain.models.GameState;
import domain.models.entity.*;
import domain.models.map.GameMap;
import java.awt.CardLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import ui.DesignModeView;
import view.AssetManager;
import view.GameView;
import view.TileManager;

public class DemoRunner {
    private static GameMap initialDesignedMap = null;
    private static domain.models.GameMode activeGameMode = domain.models.GameMode.ADVENTURE;

    private static GameMap cloneMap(GameMap original) {
        if (original == null) return null;
        int w = original.getWidth();
        int h = original.getHeight();
        GameMap copy = new GameMap(w, h);
        
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                domain.models.entity.GameObject obj = original.getObjectAt(x, y);
                if (obj == null) continue;
                if (obj instanceof domain.models.tile.WallTile) {
                    domain.models.tile.WallTile origWall = (domain.models.tile.WallTile) obj;
                    domain.models.tile.WallTile newWall = new domain.models.tile.WallTile(origWall.getImageName());
                    copy.placeObject(newWall, x, y);
                    if (origWall.getDecoration() != null) {
                        domain.models.entity.GameObject origDeco = origWall.getDecoration();
                        domain.models.entity.GameObject newDeco = null;
                        if (origDeco instanceof domain.models.entity.Column) {
                            newDeco = new domain.models.entity.Column(origDeco.getName(), x, y, origDeco.getImageName());
                        } else if (origDeco instanceof domain.models.entity.Sign) {
                            newDeco = new domain.models.entity.Sign(origDeco.getName(), x, y, origDeco.getImageName());
                        } else if (origDeco instanceof domain.models.staticObjects.Decoration) {
                            newDeco = new domain.models.staticObjects.Decoration(origDeco.getName(), x, y, origDeco.getImageName());
                        }
                        if (newDeco != null) {
                            newWall.setDecoration(newDeco);
                            newDeco.setMap(copy);
                        }
                    }
                } else if (obj instanceof domain.models.tile.FloorTile) {
                    copy.placeObject(new domain.models.tile.FloorTile(), x, y);
                } else if (obj instanceof domain.models.entity.Chest) {
                    domain.models.entity.Chest chest = (domain.models.entity.Chest) obj;
                    copy.placeObject(new domain.models.entity.Chest(chest.getName(), x, y, chest.isLocked()), x, y);
                } else if (obj instanceof domain.models.entity.DoubleCrate) {
                    copy.placeObject(new domain.models.entity.DoubleCrate(obj.getName(), x, y), x, y);
                } else if (obj instanceof domain.models.entity.Crate) {
                    copy.placeObject(new domain.models.entity.Crate(obj.getName(), x, y), x, y);
                } else if (obj instanceof domain.models.entity.Column) {
                    copy.placeObject(new domain.models.entity.Column(obj.getName(), x, y, obj.getImageName()), x, y);
                } else if (obj instanceof domain.models.entity.Sign) {
                    copy.placeObject(new domain.models.entity.Sign(obj.getName(), x, y, obj.getImageName()), x, y);
                } else if (obj instanceof domain.models.staticObjects.Door) {
                    domain.models.staticObjects.Door door = (domain.models.staticObjects.Door) obj;
                    copy.placeObject(new domain.models.staticObjects.Door(door.getName(), x, y, door.isLocked()), x, y);
                } else if (obj instanceof domain.models.staticObjects.Decoration) {
                    copy.placeObject(new domain.models.staticObjects.Decoration(obj.getName(), x, y, obj.getImageName()), x, y);
                } else if (obj instanceof domain.models.staticObjects.KeyItem) {
                    domain.models.staticObjects.KeyItem key = (domain.models.staticObjects.KeyItem) obj;
                    copy.placeObject(new domain.models.staticObjects.KeyItem(key.getName(), x, y, key.getImageName()), x, y);
                } else if (obj instanceof domain.models.item.PotionItem) {
                    domain.models.item.PotionItem pot = (domain.models.item.PotionItem) obj;
                    copy.placeObject(new domain.models.item.PotionItem(pot.getName(), x, y, pot.getImageName()), x, y);
                } else if (obj instanceof domain.models.item.SwordItem) {
                    copy.placeObject(new domain.models.item.SwordItem(x, y), x, y);
                } else if (obj instanceof domain.models.item.WoodenSwordItem) {
                    copy.placeObject(new domain.models.item.WoodenSwordItem(x, y), x, y);
                } else if (obj instanceof domain.models.item.AxeItem) {
                    copy.placeObject(new domain.models.item.AxeItem(x, y), x, y);
                } else if (obj instanceof domain.models.item.BowItem) {
                    copy.placeObject(new domain.models.item.BowItem(x, y), x, y);
                } else if (obj instanceof domain.models.item.FireWandItem) {
                    copy.placeObject(new domain.models.item.FireWandItem(x, y), x, y);
                } else if (obj instanceof domain.models.item.SamuraiSwordItem) {
                    copy.placeObject(new domain.models.item.SamuraiSwordItem(x, y), x, y);
                } else if (obj instanceof domain.models.item.DiamondSwordItem) {
                    copy.placeObject(new domain.models.item.DiamondSwordItem(x, y), x, y);
                } else if (obj instanceof domain.models.item.ArmorItem) {
                    copy.placeObject(new domain.models.item.ArmorItem(x, y), x, y);
                } else if (obj instanceof domain.models.item.RingItem) {
                    copy.placeObject(new domain.models.item.RingItem(x, y), x, y);
                } else if (obj instanceof domain.models.entity.SearchableObject) {
                    domain.models.entity.SearchableObject so = (domain.models.entity.SearchableObject) obj;
                    copy.placeObject(new domain.models.entity.SearchableObject(so.getName(), x, y, so.getImageName(), so.getOpenImageName()), x, y);
                } else {
                    copy.placeObject(obj, x, y);
                }
            }
        }
        return copy;
    }

    private static void restartGame(JFrame frame, JPanel mainPanel, CardLayout cardLayout) {
        if (initialDesignedMap != null) {
            GameMap cleanMap = cloneMap(initialDesignedMap);
            startGameWithMap(frame, mainPanel, cardLayout, cleanMap, activeGameMode);
        } else {
            startGame(frame, mainPanel, cardLayout);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("COMP302 Dungeon Crawler");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Set window icon
            try {
                java.io.File iconFile = new java.io.File("resources/images/icon.png");
                if (iconFile.exists()) {
                    java.awt.image.BufferedImage rawIcon = javax.imageio.ImageIO.read(iconFile);
                    java.awt.image.BufferedImage processedIcon = processIcon(rawIcon);
                    frame.setIconImage(processedIcon);
                    
                    // Set macOS Dock icon if supported
                    try {
                        if (java.awt.Taskbar.isTaskbarSupported()) {
                            java.awt.Taskbar taskbar = java.awt.Taskbar.getTaskbar();
                            if (taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                                taskbar.setIconImage(processedIcon);
                            }
                        }
                    } catch (Exception e) {
                        // Ignore if Taskbar is not supported or throws exceptions in some environments
                    }
                }
            } catch (Exception e) {
                System.err.println("Icon could not be loaded: " + e.getMessage());
            }

            CardLayout cardLayout = new CardLayout();
            JPanel mainPanel = new JPanel(cardLayout);

            view.MainMenuView menuView = new view.MainMenuView(
                    () -> startDesignMode(frame, mainPanel, cardLayout),
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

    // ── Design Mode ──────────────────────────────────────────────────────────
    private static void startDesignMode(JFrame frame, JPanel mainPanel, CardLayout cardLayout) {
        GameMap map = new GameMap(25, 20);
        TileManager tileManager = new TileManager();

        DesignModeView designView = new DesignModeView(
                map,
                tileManager,
                // onBackToMenu
                () -> cardLayout.show(mainPanel, "Menu"),
                // onPlayMap — tasarımlanan map ile oyunu başlat (Adventure)
                (designedMap) -> {
                    mainPanel.remove(mainPanel.getComponentCount() > 0
                            ? getComponentByName(mainPanel, "Design") : null);
                    startGameWithMap(frame, mainPanel, cardLayout, designedMap, domain.models.GameMode.ADVENTURE);
                },
                // onPlayTeamMatchMap - Team Match başlat
                (designedMap) -> {
                    mainPanel.remove(mainPanel.getComponentCount() > 0
                            ? getComponentByName(mainPanel, "Design") : null);
                    startGameWithMap(frame, mainPanel, cardLayout, designedMap, domain.models.GameMode.TEAM_MATCH);
                }
        );
        designView.setPreferredSize(new java.awt.Dimension(832, 700));

        // Önceki design panelini temizle (varsa)
        for (java.awt.Component c : mainPanel.getComponents()) {
            if ("Design".equals(c.getName())) { mainPanel.remove(c); break; }
        }
        designView.setName("Design");
        mainPanel.add(designView, "Design");
        cardLayout.show(mainPanel, "Design");
        frame.pack();
        designView.requestFocusInWindow();
    }

    /** Tasarımlanan map ile hero + enemies ekleyerek oyunu başlatır */
    private static void startGameWithMap(JFrame frame, JPanel mainPanel, CardLayout cardLayout, GameMap map, domain.models.GameMode mode) {
        initialDesignedMap = cloneMap(map);
        activeGameMode = mode;

        if (mode == domain.models.GameMode.TEAM_MATCH) {
            startTeamMatchWithMap(frame, mainPanel, cardLayout, map);
            return;
        }
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
                        leftTiles.add(new int[]{x, y});
                    } else {
                        rightTiles.add(new int[]{x, y});
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
        for(int i = 0; i < 3; i++) {
            Knight k = new Knight(leftTiles.get(i+1)[0], leftTiles.get(i+1)[1]);
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
        
        for(int i = 0; i < 2; i++) {
            Knight k = new Knight(rightTiles.get(i+2)[0], rightTiles.get(i+2)[1]);
            k.setTeam(domain.models.Team.CYAN);
            entities.add(k);
        }
        
        // Spawn 6 Random Weapons
        java.util.List<int[]> allTiles = new ArrayList<>(leftTiles);
        allTiles.addAll(rightTiles);
        java.util.Collections.shuffle(allTiles);
        for (int i = 0; i < 6; i++) {
            domain.models.item.MapItem weapon = domain.models.item.MapItem.createRandomWeapon(allTiles.get(i)[0], allTiles.get(i)[1]);
            map.placeObject(weapon, allTiles.get(i)[0], allTiles.get(i)[1]);
        }
        
        List<GameState.ItemRecord> scrollItems = new ArrayList<>();
        List<String> inventoryScrollTypes = new ArrayList<>();
        setupGameView(frame, mainPanel, cardLayout, hero, entities, map, null, null,
                scrollItems, inventoryScrollTypes, null, domain.models.GameMode.TEAM_MATCH);
    }

    /** Adına göre panel bul (null-safe) */
    private static java.awt.Component getComponentByName(java.awt.Container container, String name) {
        if (name == null) return null;
        for (java.awt.Component c : container.getComponents()) {
            if (name.equals(c.getName())) return c;
        }
        return null;
    }

    // Yeni oyun — tüm nesneler rastgele oluşturulur
    private static void startGame(JFrame frame, JPanel mainPanel, CardLayout cardLayout) {
        initialDesignedMap = null;
        activeGameMode = domain.models.GameMode.ADVENTURE;
        GameMap map = new GameMap(25, 20);
        Hero hero = new Hero(4, 4);
        hero.setCurrentMap(map);
        Knight knight = new Knight(12, 10);
        Sorcerer sorcerer = new Sorcerer(18, 5);

        // Kasalar (Crates) odaların/koridorların köşelerinde
        map.placeObject(new domain.models.entity.Crate("Crate", 2, 2), 2, 2);
        map.placeObject(new domain.models.entity.Crate("Crate", 3, 2), 3, 2);
        map.placeObject(new domain.models.entity.Crate("Crate", 22, 17), 22, 17);
        map.placeObject(new domain.models.entity.Crate("Crate", 2, 17), 2, 17);

        // Sandıklar (Chests) uzak köşelerde
        map.placeObject(new domain.models.entity.Chest("Main Chest", 22, 2, true), 22, 2);
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

        // =====================================================================
        // TODO: TEMPORARY DEVELOPMENT TEST DROPS - GEÇİCİ GELİŞTİRİCİ TEST SİLAHLARI VE EŞYALARI
        // Bu blok sadece test aşamasında tüm yeni silahları ve giyilebilir eşyaları kolayca denemek için eklenmiştir.
        // Yay, Balta, Asa, Katana, Elmas Kılıç, Çelik Zırh ve Güç Yüzüğü kahramanın etrafında yer alır.
        map.placeObject(new domain.models.item.WoodenSwordItem(4, 5), 4, 5);
        map.placeObject(new domain.models.item.AxeItem(5, 4), 5, 4);
        map.placeObject(new domain.models.item.BowItem(5, 5), 5, 5);
        map.placeObject(new domain.models.item.FireWandItem(3, 4), 3, 4);
        map.placeObject(new domain.models.item.SamuraiSwordItem(3, 5), 3, 5);
        map.placeObject(new domain.models.item.DiamondSwordItem(5, 3), 5, 3);
        map.placeObject(new domain.models.item.ArmorItem(4, 3), 4, 3);
        map.placeObject(new domain.models.item.RingItem(3, 3), 3, 3);
        // =====================================================================



        // Dekorasyonlar (Torches)
        map.placeObject(new domain.models.staticObjects.Decoration("Torch", 6, 1, "torch/torch_1"), 6, 1);
        map.placeObject(new domain.models.staticObjects.Decoration("Torch", 18, 1, "torch/torch_1"), 18, 1);

        // Çıkış Kapısı (Exit Door) - Kilitli olarak yerleştirildi!
        map.placeObject(new domain.models.staticObjects.Door("Exit Door", 22, 10, true), 22, 10);
        map.placeObject(new domain.models.staticObjects.Decoration("Torch", 1, 10, "torch/torch_1"), 1, 10);
        map.placeObject(new domain.models.staticObjects.Decoration("Torch", 23, 10, "torch/torch_1"), 23, 10);

        List<Entity> entities = new ArrayList<>();
        entities.add(hero);
        entities.add(knight);
        entities.add(sorcerer);

        // Yeni oyunda haritada ve envantertde scroll yok — boş listeler
        List<GameState.ItemRecord> scrollItems = new ArrayList<>();
        List<String> inventoryScrollTypes = new ArrayList<>();
        setupGameView(frame, mainPanel, cardLayout, hero, entities, map, knight, sorcerer, scrollItems, inventoryScrollTypes, null, domain.models.GameMode.ADVENTURE);
    }

    // Kaydedilmiş oyunu yükle — GameState'ten tüm nesneler yeniden oluşturulur
    private static void loadGame(JFrame frame, JPanel mainPanel, CardLayout cardLayout, GameState state) {
        initialDesignedMap = null;
        activeGameMode = domain.models.GameMode.ADVENTURE;
        GameMap map = new GameMap(25, 20);

        // Hero oluştur ve durumunu yükle
        Hero hero = new Hero(state.hero.x, state.hero.y);
        hero.setCurrentMap(map);
        hero.setHp(state.hero.hp);
        hero.setMana(state.hero.mana);
        hero.setEnergy(state.hero.energy);
        if (state.hero.str > 0) hero.setStr(state.hero.str); // str bilgisini yükle

        // Kılıcı veya kuşanılmış silahı takılıysa ayarla
        if (state.hero.equippedWeaponType != null && !state.hero.equippedWeaponType.isEmpty()) {
            domain.models.entity.GameObject weapon = createItem(state.hero.equippedWeaponType, 0, 0);
            if (weapon instanceof domain.models.item.MapItem) {
                hero.equipWeapon((domain.models.item.MapItem) weapon);
            }
        }
        // Kuşanılmış zırhı takılıysa ayarla
        if (state.hero.equippedArmorType != null && !state.hero.equippedArmorType.isEmpty()) {
            domain.models.entity.GameObject armor = createItem(state.hero.equippedArmorType, 0, 0);
            if (armor instanceof domain.models.item.MapItem) {
                hero.equipArmor((domain.models.item.MapItem) armor);
            }
        }
        // Kuşanılmış yüzüğü takılıysa ayarla
        if (state.hero.equippedRingType != null && !state.hero.equippedRingType.isEmpty()) {
            domain.models.entity.GameObject ring = createItem(state.hero.equippedRingType, 0, 0);
            if (ring instanceof domain.models.item.MapItem) {
                hero.equipRing((domain.models.item.MapItem) ring);
            }
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
                s.setTimeLeft(rec.timeLeft);                    // Işınlanma timer'ı
                s.setProjectileTimeLeft(rec.projectileTimeLeft); // Mermi timer'ı
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

        // Kaydedilmiş uçan mermileri yeniden oluştur
        if (state.projectiles != null) {
            for (GameState.ProjectileRecord pr : state.projectiles) {
                Entity owner = pr.heroOwned ? hero : sorcerer;
                entities.add(new domain.models.entity.Projectile(
                        pr.x, pr.y, pr.exactX, pr.exactY, pr.deltaX, pr.deltaY, pr.damage, owner, pr.type
                ));
            }
        }

        // Harita itemlarını ayır: scroll'lar ayrı tutulur (inputHandler gerektirir)
        List<GameState.ItemRecord> scrollItems = new ArrayList<>();

        for (GameState.ItemRecord rec : state.mapItems) {
            if ("ShadowCloneScroll".equals(rec.type)) {
                // Scroll'lar setupGameView içinde inputHandler ile birlikte oluşturulur
                scrollItems.add(rec);
            } else {
                domain.models.entity.GameObject item = createItem(rec.type, rec.name, rec.x, rec.y, rec.isLocked);
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

        setupGameView(frame, mainPanel, cardLayout, hero, entities, map, knight, sorcerer, scrollItems, inventoryScrollTypes, state, domain.models.GameMode.ADVENTURE);
    }

    // Item tip ismine göre nesne oluşturur — scroll hariç (scroll setupGameView'da oluşur)
    private static domain.models.entity.GameObject createItem(String type, String name, int x, int y, boolean isLocked) {
        String displayName = (name != null && !name.isEmpty()) ? name : type;
        switch (type) {
            case "PotionItem":        return new domain.models.item.PotionItem(x, y);
            case "SwordItem":         return new domain.models.item.SwordItem(x, y);
            case "AxeItem":           return new domain.models.item.AxeItem(x, y);
            case "WoodenSwordItem":   return new domain.models.item.WoodenSwordItem(x, y);
            case "SamuraiSwordItem":  return new domain.models.item.SamuraiSwordItem(x, y);
            case "DiamondSwordItem":  return new domain.models.item.DiamondSwordItem(x, y);
            case "BowItem":           return new domain.models.item.BowItem(x, y);
            case "FireWandItem":      return new domain.models.item.FireWandItem(x, y);
            case "ArmorItem":         return new domain.models.item.ArmorItem(x, y);
            case "RingItem":          return new domain.models.item.RingItem(x, y);
            case "KeyItem":           return new domain.models.staticObjects.KeyItem(x, y);
            case "Column":            return new domain.models.entity.Column(displayName, x, y);
            case "Sign":              return new domain.models.entity.Sign(displayName, x, y, "sign/sign_brown");
            case "DoubleCrate":       return new domain.models.entity.DoubleCrate(displayName, x, y);
            case "Crate":             return new domain.models.entity.Crate(displayName, x, y);
            case "Chest":             return new domain.models.entity.Chest(displayName, x, y, isLocked);
            case "SearchableObject":  return new domain.models.entity.SearchableObject(displayName, x, y);
            case "Decoration":        return new domain.models.staticObjects.Decoration(displayName, x, y, "torch/torch_1");
            case "Door":
                domain.models.staticObjects.Door door = new domain.models.staticObjects.Door(displayName, x, y, isLocked);
                if (!isLocked) door.open(); // Eğer kilitli değilse açık görseline geç
                return door;
            default:
                System.err.println("Bilinmeyen item tipi: " + type);
                return null;
        }
    }

    private static domain.models.entity.GameObject createItem(String type, String name, int x, int y) {
        return createItem(type, name, x, y, false);
    }

    // Eski imza — envanter için (name yok)
    private static domain.models.entity.GameObject createItem(String type, int x, int y) {
        return createItem(type, null, x, y, false);
    }

    // startGame ve loadGame tarafından ortak kullanılan view/timer/input kurulum
    // scrollItems: haritaya yerleştirilecek scroll kayıtları — inputHandler gerektirdiği için burada oluşturulur
    private static void setupGameView(JFrame frame, JPanel mainPanel, CardLayout cardLayout,
                                      Hero hero, List<Entity> entities, GameMap map,
                                      Knight knight, Sorcerer sorcerer,
                                      List<GameState.ItemRecord> scrollItems,
                                      List<String> inventoryScrollTypes,
                                      GameState state,
                                      domain.models.GameMode mode) {
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

        mainPanel.add(gameView, "Game");
        cardLayout.show(mainPanel, "Game");

        view.ActionMenu actionMenu = new view.ActionMenu(hero);
        controller.MouseHandler mouseHandler = new controller.MouseHandler(hero, map, gameView, actionMenu);
        gameView.addMouseListener(mouseHandler);
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
                    restartGame(frame, mainPanel, cardLayout);
                },
                () -> {
                    if (logicRef[0] != null)  logicRef[0].stop();
                    if (renderRef[0] != null) renderRef[0].stop();
                    cardLayout.show(mainPanel, "Menu");
                }
        );
        frame.setGlassPane(pauseMenu);

        // GameOverMenu — JFrame glass pane olarak Hero ölünce bindirilecek
        view.GameOverMenu gameOverMenu = new view.GameOverMenu(
                () -> {
                    if (logicRef[0] != null)  logicRef[0].stop();
                    if (renderRef[0] != null) renderRef[0].stop();
                    restartGame(frame, mainPanel, cardLayout);
                },
                (loadedState) -> {
                    if (logicRef[0] != null)  logicRef[0].stop();
                    if (renderRef[0] != null) renderRef[0].stop();
                    loadGame(frame, mainPanel, cardLayout, loadedState);
                },
                () -> {
                    if (logicRef[0] != null)  logicRef[0].stop();
                    if (renderRef[0] != null) renderRef[0].stop();
                    cardLayout.show(mainPanel, "Menu");
                }
        );

        // ESC tuşu — pause/resume toggle
        gameView.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke("ESCAPE"), "togglePause");
        gameView.getActionMap().put("togglePause", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (gameOverMenu.isVisible()) return; // Game over aktifken ESC basılamaz
                
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
            
            if (mode == domain.models.GameMode.TEAM_MATCH) {
                // TEAM MATCH WIN/LOSS CONDITION
                boolean cyanAlive = false;
                boolean orangeAlive = false;
                for (domain.models.entity.Entity ent : entities) {
                    if (ent.isAlive()) {
                        if (ent.getTeam() == domain.models.Team.CYAN) cyanAlive = true;
                        if (ent.getTeam() == domain.models.Team.ORANGE) orangeAlive = true;
                    }
                }
                
                if (!cyanAlive || !orangeAlive) {
                    if (logicRef[0] != null)  logicRef[0].stop();
                    if (renderRef[0] != null) renderRef[0].stop();
                    inputHandler.disableInput();
                    
                    boolean isVictory = cyanAlive;
                    String headingText = isVictory ? "YOU WIN" : "GAME OVER";
                    String subHeadingText = isVictory ? "Orange Team has been defeated. You win!" : "Cyan Team has been defeated. Game Over!";
                    
                    gameOverMenu.setupGameOverMenu(headingText, subHeadingText, false, isVictory);
                    
                    frame.setGlassPane(gameOverMenu);
                    gameOverMenu.setVisible(true);
                    return;
                }
            } else {
                // ADVENTURE WIN KONTROLÜ
                boolean allEnemiesDefeated = true;
                for (domain.models.entity.Entity entity : entities) {
                    if ((entity instanceof domain.models.entity.Knight || entity instanceof domain.models.entity.Sorcerer) && entity.isAlive()) {
                        allEnemiesDefeated = false;
                        break;
                    }
                }
                
                boolean exitReached = false;
                for (int x = 0; x < map.getWidth(); x++) {
                    for (int y = 0; y < map.getHeight(); y++) {
                        domain.models.entity.GameObject obj = map.getObjectAt(x, y);
                        if (obj instanceof domain.models.staticObjects.Door && "Exit Door".equals(obj.getName())) {
                            domain.models.staticObjects.Door door = (domain.models.staticObjects.Door) obj;
                            if (!door.isLocked() && Math.abs(hero.getX() - x) <= 1 && Math.abs(hero.getY() - y) <= 1) {
                                exitReached = true;
                            }
                        }
                    }
                }

                if (allEnemiesDefeated && exitReached) {
                    if (logicRef[0] != null)  logicRef[0].stop();
                    if (renderRef[0] != null) renderRef[0].stop();
                    inputHandler.disableInput();
                    
                    javax.swing.JOptionPane.showMessageDialog(frame,
                            "🌟 TEBRİKLER! 🌟\n\nTüm düşmanları yendin ve Çıkış Kapısı'nı açarak COMP302 Zindanından başarıyla kaçtın!\nPhase I başarıyla tamamlandı!",
                            "Zafer!",
                            javax.swing.JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                // GAME OVER KONTROLÜ (Sadece Adventure)
                if (!hero.isAlive()) {
                    if (logicRef[0] != null)  logicRef[0].stop(); // Oyun motorunu (hareketleri) durdur
                    if (renderRef[0] != null) renderRef[0].stop(); // FPS motorunu durdur
                    inputHandler.disableInput(); // Oyuncunun tuş basmalarını engelle
                    
                    // Reset to default Adventure mode style
                    gameOverMenu.setupGameOverMenu("GAME OVER", "You have succumbed to your fate.", true, false);
                    
                    // Game Over ekranını JFrame'in en üst katmanına (GlassPane) bas
                    frame.setGlassPane(gameOverMenu);
                    gameOverMenu.setVisible(true);
                    return;
                }
            }

            hero.update();

            if (mode == domain.models.GameMode.TEAM_MATCH) {
                // Team match update logic (everyone in entities)
                for (domain.models.entity.Entity ent : entities) {
                    if (ent instanceof domain.models.entity.Knight && ent.isAlive()) {
                        ((domain.models.entity.Knight) ent).followHero(hero, map, entities);
                    } else if (ent instanceof domain.models.entity.Sorcerer && ent.isAlive()) {
                        ((domain.models.entity.Sorcerer) ent).followHero(hero, map, entities);
                    }
                }
            } else {
                if (knight != null) knight.followHero(hero, map, entities);
                if (sorcerer != null) sorcerer.followHero(hero, map, entities);

                spawner.trySpawn(entities);

                for (domain.models.entity.Knight k : spawner.getSpawnedKnights()) {
                    if (k.isAlive()) k.followHero(hero, map, entities);
                }
                for (domain.models.entity.Sorcerer s : spawner.getSpawnedSorcerers()) {
                    if (s.isAlive()) s.followHero(hero, map, entities);
                }
            }

            // Tüm Sorcerer'lardan bekleyen mermileri topla
            java.util.List<domain.models.entity.Projectile> newProjectiles = new java.util.ArrayList<>();
            if (mode == domain.models.GameMode.TEAM_MATCH) {
                for (domain.models.entity.Entity ent : entities) {
                    if (ent instanceof domain.models.entity.Sorcerer) {
                        domain.models.entity.Projectile p = ((domain.models.entity.Sorcerer) ent).pollPendingProjectile();
                        if (p != null) newProjectiles.add(p);
                    }
                }
            } else {
                if (sorcerer != null) {
                    domain.models.entity.Projectile sp = sorcerer.pollPendingProjectile();
                    if (sp != null) newProjectiles.add(sp);
                }
                for (domain.models.entity.Sorcerer s : spawner.getSpawnedSorcerers()) {
                    domain.models.entity.Projectile p = s.pollPendingProjectile();
                    if (p != null) newProjectiles.add(p);
                }
            }
            entities.addAll(newProjectiles);

            // Tüm aktif mermileri ilerlet ve Hero/Clone/Enemy çarpışmasını kontrol et
            for (domain.models.entity.Entity en : entities) {
                if (en instanceof domain.models.entity.Projectile && en.isAlive()) {
                    domain.models.entity.Projectile proj = (domain.models.entity.Projectile) en;
                    proj.step(map);
                    
                    if (mode == domain.models.GameMode.TEAM_MATCH) {
                        for (domain.models.entity.Entity target : entities) {
                            if (target.isAlive() && target != proj.getOwner() && !(target instanceof domain.models.entity.Projectile)) {
                                if (proj.getX() == target.getX() && proj.getY() == target.getY() && target.getTeam() != domain.models.Team.NONE && target.getTeam() != proj.getOwner().getTeam()) {
                                    int def = (target instanceof domain.models.entity.Hero) ? ((domain.models.entity.Hero) target).getDef() : 0;
                                    if (target instanceof domain.models.entity.Knight) def = 1;
                                    int damage = Math.max(1, proj.getDamage() - def);
                                    target.takeDamage(damage);
                                    view.GameView.addFloatingText(target.getX(), target.getY(), "-" + damage + " HP", new java.awt.Color(255, 60, 60));
                                    proj.setHp(0);
                                    break;
                                }
                            }
                        }
                    } else {
                        if (proj.getOwner() == hero) {
                            // Hero fırlattığı mermilerin düşmanlara çarpma kontrolü
                            for (domain.models.entity.Entity enemy : entities) {
                                if (enemy.isAlive() && enemy != hero && !(enemy instanceof ShadowClone) && !(enemy instanceof domain.models.entity.Projectile)) {
                                    if (proj.getX() == enemy.getX() && proj.getY() == enemy.getY()) {
                                        int def = 0;
                                        if (enemy instanceof domain.models.entity.Knight) {
                                            def = 1; // Knight armor reduces damage by 1
                                        }
                                        int damage = Math.max(0, proj.getDamage() - def);
                                        enemy.takeDamage(damage);
                                        view.GameView.addFloatingText(enemy.getX(), enemy.getY(), "-" + damage + " HP", new java.awt.Color(255, 60, 60));
                                        proj.setHp(0); // destroy projectile
                                        System.out.println("Enemy hit by player projectile! Damage: " + damage + " | Enemy HP: " + enemy.getHp());
                                        
                                        // Handle enemy defeat & loot drop
                                        if (!enemy.isAlive()) {
                                            System.out.println("Enemy defeated by projectile!");
                                            java.util.Random rand = new java.util.Random();
                                            int dropType = rand.nextInt(3);
                                            domain.models.entity.GameObject loot = null;
                                            if (dropType == 0) {
                                                loot = domain.models.item.MapItem.createRandomItem(enemy.getX(), enemy.getY());
                                            } else if (dropType == 1) {
                                                loot = new domain.models.item.PotionItem(enemy.getX(), enemy.getY());
                                            } else {
                                                loot = new domain.models.staticObjects.KeyItem(enemy.getX(), enemy.getY());
                                            }
                                            map.placeObject(loot, enemy.getX(), enemy.getY());
                                            System.out.println("Loot dropped: " + loot.getName());
                                        }
                                        break;
                                    }
                                }
                            }
                        } else {
                            // Düşman mermisinin Hero veya Clone'a çarpma kontrolü
                            ShadowClone activeClone = inputHandler.getShadowClone();
                            Entity target = (activeClone != null && activeClone.isAlive()) ? activeClone : hero;
                            
                            if (proj.isAlive() && proj.getX() == target.getX() && proj.getY() == target.getY()) {
                                int def = (target instanceof domain.models.entity.Hero) ? ((domain.models.entity.Hero) target).getDef() : 0;
                                int damage = Math.max(1, proj.getDamage() - def); // Minimum 1 damage to prevent complete invincibility
                                target.takeDamage(damage);
                                view.GameView.addFloatingText(target.getX(), target.getY(), "-" + damage + " HP", new java.awt.Color(255, 200, 50));
                                proj.setHp(0); // Mermi yok ol
                                System.out.println("Target hit by projectile! Damage: " + damage + " | Target HP: " + target.getHp());
                            }
                        }
                    }
                }
            }

            scrollSpawner.trySpawn();

            ShadowClone activeCloneForUpdate = inputHandler.getShadowClone();
            if (activeCloneForUpdate != null) activeCloneForUpdate.update();
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

    private static java.awt.image.BufferedImage processIcon(java.awt.image.BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int top = 0, bottom = h - 1, left = 0, right = w - 1;
        try {
            // Top border
            outer: for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int rgb = img.getRGB(x, y);
                    int alpha = (rgb >> 24) & 0xff;
                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;
                    if (alpha > 10 && !(r > 240 && g > 240 && b > 240)) {
                        top = y;
                        break outer;
                    }
                }
            }
            // Bottom border
            outer: for (int y = h - 1; y >= 0; y--) {
                for (int x = 0; x < w; x++) {
                    int rgb = img.getRGB(x, y);
                    int alpha = (rgb >> 24) & 0xff;
                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;
                    if (alpha > 10 && !(r > 240 && g > 240 && b > 240)) {
                        bottom = y;
                        break outer;
                    }
                }
            }
            // Left border
            outer: for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    int rgb = img.getRGB(x, y);
                    int alpha = (rgb >> 24) & 0xff;
                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;
                    if (alpha > 10 && !(r > 240 && g > 240 && b > 240)) {
                        left = x;
                        break outer;
                    }
                }
            }
            // Right border
            outer: for (int x = w - 1; x >= 0; x--) {
                for (int y = 0; y < h; y++) {
                    int rgb = img.getRGB(x, y);
                    int alpha = (rgb >> 24) & 0xff;
                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;
                    if (alpha > 10 && !(r > 240 && g > 240 && b > 240)) {
                        right = x;
                        break outer;
                    }
                }
            }
        } catch (Exception e) {
            return img;
        }

        if (right <= left || bottom <= top) return img;

        // Extract the subimage and make white background transparent
        int subW = right - left + 1;
        int subH = bottom - top + 1;
        java.awt.image.BufferedImage processed = new java.awt.image.BufferedImage(subW, subH, java.awt.image.BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < subH; y++) {
            for (int x = 0; x < subW; x++) {
                int rgb = img.getRGB(left + x, top + y);
                int alpha = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                if (alpha < 10 || (r > 240 && g > 240 && b > 240)) {
                    processed.setRGB(x, y, 0x00000000);
                } else {
                    processed.setRGB(x, y, rgb);
                }
            }
        }
        return processed;
    }
}