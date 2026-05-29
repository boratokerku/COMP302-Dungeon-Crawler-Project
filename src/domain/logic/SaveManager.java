package domain.logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import domain.models.GameState;
import domain.models.entity.*;
import domain.models.item.MapItem;
import domain.models.map.GameMap;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SaveManager {

    private static final String SAVES_DIR = "saves";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Oyunu kaydet — saves/<saveName>.json dosyasına yazar
    public static void save(String saveName, Hero hero, List<Entity> entities, GameMap map,
                            domain.logic.EnemySpawner enemySpawner, domain.logic.ScrollSpawner scrollSpawner,
                            int currentLevel, long elapsedSeconds) {
        GameState state = new GameState();
        state.saveName = saveName;
        state.currentLevel = currentLevel;
        state.timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date());
        state.elapsedSeconds = elapsedSeconds;

        // Global Timerlar
        state.enemySpawnTimeLeft = enemySpawner != null ? enemySpawner.getTimeLeft() : 9000;
        state.scrollSpawnTimeLeft = scrollSpawner != null ? scrollSpawner.getTimeLeft() : 15000;

        // Hero verisi
        String equippedWeaponType = hero.getEquippedWeapon() != null ? hero.getEquippedWeapon().getClass().getSimpleName() : null;
        String equippedArmorType  = hero.getEquippedArmor()  != null ? hero.getEquippedArmor().getClass().getSimpleName() : null;
        String equippedRingType   = hero.getEquippedRing()   != null ? hero.getEquippedRing().getClass().getSimpleName() : null;
        
        state.hero = new GameState.HeroRecord(
                hero.getX(), hero.getY(),
                hero.getHp(), hero.getMana(), hero.getEnergy(),
                hero.getStr(), equippedWeaponType, equippedArmorType, equippedRingType
        );

        // Envanter (detaylı nesne kaydı)
        for (GameObject item : hero.getInventory().getItems()) {
            GameState.ItemRecord rec = new GameState.ItemRecord(
                    item.getClass().getSimpleName(), item.getName(), item.getX(), item.getY()
            );
            rec.imageName = item.getImageName();
            state.inventoryItems.add(rec);
        }

        // Haritadaki eşyalar — alınabilir itemlar ve static nesneler
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj == null) continue;
                
                GameState.ItemRecord rec = null;
                if (obj instanceof MapItem) {
                    rec = new GameState.ItemRecord(
                            obj.getClass().getSimpleName(), obj.getName(), x, y
                    );
                    rec.imageName = obj.getImageName();
                } else if (obj instanceof domain.models.entity.Chest) {
                    domain.models.entity.Chest chest = (domain.models.entity.Chest) obj;
                    rec = new GameState.ItemRecord(
                            "Chest", chest.getName(), x, y, chest.isLocked()
                    );
                    rec.imageName = chest.getImageName();
                } else if (obj instanceof domain.models.tile.WallTile) {
                    domain.models.entity.GameObject deco = ((domain.models.tile.WallTile) obj).getDecoration();
                    if (deco instanceof domain.models.entity.SearchableObject) {
                        domain.models.entity.SearchableObject so = (domain.models.entity.SearchableObject) deco;
                        String hiddenItemType = so.getHiddenItem() != null ? so.getHiddenItem().getClass().getSimpleName() : null;
                        rec = new GameState.ItemRecord(
                                "SearchableObject", so.getName(), x, y, so.isSearched(), hiddenItemType
                        );
                        rec.imageName = so.getImageName();
                    }
                } else if (obj instanceof domain.models.entity.Crate) {
                    domain.models.entity.Crate crate = (domain.models.entity.Crate) obj;
                    String hiddenItemType = crate.getHiddenItem() != null ? crate.getHiddenItem().getClass().getSimpleName() : null;
                    rec = new GameState.ItemRecord(
                            "Crate", crate.getName(), x, y, false, hiddenItemType
                    );
                    rec.imageName = crate.getImageName();
                } else if (obj instanceof domain.models.entity.Column
                        || obj instanceof domain.models.entity.SearchableObject
                        || obj instanceof domain.models.staticObjects.Decoration
                        || obj instanceof domain.models.entity.Sign) {
                    rec = new GameState.ItemRecord(
                            obj.getClass().getSimpleName(), obj.getName(), x, y
                    );
                    rec.imageName = obj.getImageName();
                } else if (obj instanceof domain.models.staticObjects.Door) {
                    domain.models.staticObjects.Door door = (domain.models.staticObjects.Door) obj;
                    rec = new GameState.ItemRecord(
                            "Door", door.getName(), x, y, door.isLocked()
                    );
                    rec.imageName = door.getImageName();
                }

                if (rec != null) {
                    state.mapItems.add(rec);
                }
            }
        }

        // Düşmanlar ve ShadowClone
        for (Entity e : entities) {
            if (e instanceof Knight) {
                state.enemies.add(new GameState.EnemyRecord(
                        "Knight", e.getX(), e.getY(), e.getHp(), e.isAlive(), 0
                ));
            } else if (e instanceof Sorcerer) {
                long teleportLeft    = ((Sorcerer) e).getTimeLeft();
                long projectileLeft  = ((Sorcerer) e).getProjectileTimeLeft();
                state.enemies.add(new GameState.EnemyRecord(
                        "Sorcerer", e.getX(), e.getY(), e.getHp(), e.isAlive(), teleportLeft, projectileLeft
                ));
            } else if (e instanceof domain.models.entity.FinalBoss) {
                domain.models.entity.FinalBoss fb = (domain.models.entity.FinalBoss) e;
                state.enemies.add(new GameState.EnemyRecord(
                        "FinalBoss", e.getX(), e.getY(), e.getHp(), e.isAlive(),
                        fb.getTeleportTimeLeft(), fb.getProjectileTimeLeft()
                ));
            } else if (e instanceof domain.models.entity.ShadowClone) {
                if (e.isAlive()) {
                    long timeLeft = ((domain.models.entity.ShadowClone) e).getTimeLeft();
                    state.enemies.add(new GameState.EnemyRecord(
                            "ShadowClone", e.getX(), e.getY(), e.getHp(), e.isAlive(), timeLeft
                    ));
                }
            } else if (e instanceof domain.models.entity.Projectile) {
                if (e.isAlive()) {
                    domain.models.entity.Projectile p = (domain.models.entity.Projectile) e;
                    boolean isHeroOwned = (p.getOwner() instanceof domain.models.entity.Hero);
                    state.projectiles.add(new GameState.ProjectileRecord(
                            p.getX(), p.getY(), p.getExactX(), p.getExactY(),
                            p.getDeltaX(), p.getDeltaY(), p.getDamage(),
                            p.getType(), isHeroOwned
                    ));
                }
            }
        }

        // saves/ klasörünü oluştur (yoksa)
        File dir = new File(SAVES_DIR);
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, saveName + ".json");
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(state, writer);
            System.out.println("Oyun kaydedildi: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Kayıt hatası: " + e.getMessage());
        }
    }

    // Kaydı yükle — GameState döndürür, DemoRunner reconstruct eder
    public static GameState load(String saveName) {
        File file = new File(SAVES_DIR + "/" + saveName + ".json");
        if (!file.exists()) {
            System.err.println("Save bulunamadı: " + file.getAbsolutePath());
            return null;
        }
        try (Reader reader = new FileReader(file)) {
            GameState state = gson.fromJson(reader, GameState.class);
            System.out.println("Oyun yüklendi: " + saveName);
            return state;
        } catch (IOException e) {
            System.err.println("Yükleme hatası: " + e.getMessage());
            return null;
        }
    }

    // Ana menüde göstermek için tüm save'leri listele
    public static List<GameState> listSaves() {
        List<GameState> saves = new ArrayList<>();
        File dir = new File(SAVES_DIR);
        if (!dir.exists()) return saves;

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return saves;

        for (File f : files) {
            try (Reader reader = new FileReader(f)) {
                GameState state = gson.fromJson(reader, GameState.class);
                if (state != null) saves.add(state);
            } catch (IOException e) {
                System.err.println("Save okunamadı: " + f.getName());
            }
        }
        return saves;
    }
}
