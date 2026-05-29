package domain.logic;

import domain.models.entity.*;
import domain.models.entity.Crate;
import domain.models.map.GameMap;
import domain.models.staticObjects.*;
import domain.models.item.*;
import domain.models.item.usables.EnergyPotion;
import domain.models.item.usables.HealthPotion;
import domain.models.item.usables.ManaPotion;
import domain.models.item.usables.PotionItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * State machine managing multi-level dungeon progression.
 * 
 * Level 1 (Entry): Uses the designed/default map. Regular enemies.
 * Level 2 (The Depths): Tighter corridors, more columns, harder enemies.
 * Level 3 (Boss Arena): Open arena with the Final Boss.
 */
public class LevelManager {

    public static final int MAX_LEVELS = 3;

    private int currentLevel = 1;
    private final Random random = new Random();

    public LevelManager() {
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int level) {
        this.currentLevel = Math.max(1, Math.min(level, MAX_LEVELS));
    }

    public boolean isLastLevel() {
        return currentLevel >= MAX_LEVELS;
    }

    /**
     * Advances to the next level and generates the new map.
     * 
     * @return The generated GameMap for the new level, or null if already at max
     *         level.
     */
    public GameMap advanceLevel() {
        if (isLastLevel())
            return null;
        currentLevel++;
        return generateLevel(currentLevel);
    }

    /**
     * Generates a map for the specified level with appropriate obstacles and
     * decorations.
     * Enemies are added separately by the caller (DemoRunner).
     */
    public GameMap generateLevel(int level) {
        GameMap map = new GameMap(28, 20);

        switch (level) {
            case 2:
                generateDepths(map);
                break;
            case 3:
                generateBossArena(map);
                break;
            default:
                // Level 1 uses the designed/default map — this shouldn't be called for level 1
                break;
        }

        return map;
    }

    /**
     * Populates the entity list with enemies appropriate for the given level.
     * 
     * @param level    The level number
     * @param map      The game map
     * @param entities The entity list to populate (hero should already be in it)
     * @param hero     The hero reference
     */
    public void populateEnemies(int level, GameMap map, List<Entity> entities, Hero hero) {
        switch (level) {
            case 2:
                populateDepthsEnemies(map, entities, hero);
                break;
            case 3:
                populateBossArenaEnemies(map, entities, hero);
                break;
            default:
                break;
        }
    }

    // ── Level 2: The Depths ──────────────────────────────────────────────────

    private void generateDepths(GameMap map) {
        int w = map.getWidth();
        int h = map.getHeight();

        // Tighter corridors with more columns
        // Column pairs forming corridors
        map.placeObject(new Column("Column", 5, 3, "colon/gray_colon_whole"), 5, 3);
        map.placeObject(new Column("Column", 5, 5, "colon/gray_colon_whole"), 5, 5);
        map.placeObject(new Column("Column", 5, 7, "colon/gray_colon_whole"), 5, 7);
        map.placeObject(new Column("Column", 5, 10, "colon/gray_colon_whole"), 5, 10);
        map.placeObject(new Column("Column", 5, 12, "colon/gray_colon_whole"), 5, 12);

        map.placeObject(new Column("Column", 10, 3, "colon/gray_colon_whole"), 10, 3);
        map.placeObject(new Column("Column", 10, 5, "colon/gray_colon_whole"), 10, 5);
        map.placeObject(new Column("Column", 10, 7, "colon/gray_colon_whole"), 10, 7);
        map.placeObject(new Column("Column", 10, 10, "colon/gray_colon_whole"), 10, 10);
        map.placeObject(new Column("Column", 10, 12, "colon/gray_colon_whole"), 10, 12);

        map.placeObject(new Column("Column", 16, 3, "colon/gray_colon_whole"), 16, 3);
        map.placeObject(new Column("Column", 16, 5, "colon/gray_colon_whole"), 16, 5);
        map.placeObject(new Column("Column", 16, 7, "colon/gray_colon_whole"), 16, 7);
        map.placeObject(new Column("Column", 16, 10, "colon/gray_colon_whole"), 16, 10);
        map.placeObject(new Column("Column", 16, 12, "colon/gray_colon_whole"), 16, 12);

        map.placeObject(new Column("Column", 22, 3, "colon/gray_colon_whole"), 22, 3);
        map.placeObject(new Column("Column", 22, 5, "colon/gray_colon_whole"), 22, 5);
        map.placeObject(new Column("Column", 22, 7, "colon/gray_colon_whole"), 22, 7);
        map.placeObject(new Column("Column", 22, 10, "colon/gray_colon_whole"), 22, 10);
        map.placeObject(new Column("Column", 22, 12, "colon/gray_colon_whole"), 22, 12);

        // Crates as obstacles
        map.placeObject(new Crate("Crate", 3, 4), 3, 4);
        map.placeObject(new Crate("Crate", 8, 8), 8, 8);
        map.placeObject(new Crate("Crate", 13, 6), 13, 6);
        map.placeObject(new Crate("Crate", 18, 11), 18, 11);

        // Chests with loot
        map.placeObject(new Chest("Depths Chest", w - 3, 2, true), w - 3, 2);
        map.placeObject(new Chest("Depths Chest", 2, h - 3, true), 2, h - 3);

        // Keys to match chests
        map.placeObject(new KeyItem(7, 4), 7, 4);
        map.placeObject(new KeyItem(w - 8, h - 5), w - 8, h - 5);

        // Potions scattered
        map.placeObject(new PotionItem(new HealthPotion("Red Potion", 5), 3, 8, "images/items/potion/red_potion.png"),
                3, 8);
        map.placeObject(
                new PotionItem(new HealthPotion("Red Potion", 5), w - 4, 4, "images/items/potion/red_potion.png"),
                w - 4, 4);

        // Torches for atmosphere
        map.placeObject(new Decoration("Torch", 3, 1, "torch/torch_1"), 3, 1);
        map.placeObject(new Decoration("Torch", w / 2, 1, "torch/torch_1"), w / 2, 1);
        map.placeObject(new Decoration("Torch", w - 5, 1, "torch/torch_1"), w - 5, 1);

        placeRandomWallObjects(map, true, true);

        // Level Door to Boss Arena (locked, needs Level Key)
        placeRandomLevelDoor(map, "Boss Gate");

        // Level Key hidden in the level
        hideLevelKey(map);
    }

    private void populateDepthsEnemies(GameMap map, List<Entity> entities, Hero hero) {
        int w = map.getWidth();
        int h = map.getHeight();
        // 2 Knights + 2 Sorcerers for The Depths
        entities.add(new Knight(8, 3));
        entities.add(new Knight(w - 8, h - 6));
        entities.add(new Sorcerer(w - 4, 5));
        entities.add(new Sorcerer(4, h - 5));
    }

    // ── Level 3: Boss Arena ──────────────────────────────────────────────────

    private void generateBossArena(GameMap map) {
        int w = map.getWidth();
        int h = map.getHeight();

        // Open arena with 4 corner columns
        map.placeObject(new Column("Column", 4, 3, "colon/gray_colon_whole"), 4, 3);
        map.placeObject(new Column("Column", w - 5, 3, "colon/gray_colon_whole"), w - 5, 3);
        map.placeObject(new Column("Column", 4, h - 4, "colon/gray_colon_whole"), 4, h - 4);
        map.placeObject(new Column("Column", w - 5, h - 4, "colon/gray_colon_whole"), w - 5, h - 4);

        // Torches on every wall for dramatic boss fight lighting
        map.placeObject(new Decoration("Torch", 4, 1, "torch/torch_1"), 4, 1);
        map.placeObject(new Decoration("Torch", w / 3, 1, "torch/torch_1"), w / 3, 1);
        map.placeObject(new Decoration("Torch", 2 * w / 3, 1, "torch/torch_1"), 2 * w / 3, 1);
        map.placeObject(new Decoration("Torch", w - 5, 1, "torch/torch_1"), w - 5, 1);

        placeRandomWallObjects(map, false, true);

        // Some potions for the boss fight (different varieties)
        map.placeObject(new PotionItem(new HealthPotion("Red Potion", 5), 2, 2, "images/items/potion/red_potion.png"),
                2, 2);
        map.placeObject(
                new PotionItem(new ManaPotion("Blue Potion", 20), w - 3, 2, "images/items/potion/blue_potion.png"),
                w - 3, 2);
        map.placeObject(
                new PotionItem(new EnergyPotion("Green Potion", 30), 2, h - 3, "images/items/potion/green_potion.png"),
                2, h - 3);
        map.placeObject(
                new PotionItem(new HealthPotion("Red Potion", 5), w - 3, h - 3, "images/items/potion/red_potion.png"),
                w - 3, h - 3);
    }

    private void placeRandomWallObjects(GameMap map, boolean includeSearchables, boolean includeDecorations) {
        int w = map.getWidth();
        int h = map.getHeight();
        List<int[]> emptyWallTiles = new ArrayList<>();
        for (int x = 1; x < w - 1; x++) {
            GameObject topWall = map.getObjectAt(x, 0);
            if (topWall instanceof domain.models.tile.WallTile
                    && ((domain.models.tile.WallTile) topWall).getDecoration() == null) {
                emptyWallTiles.add(new int[] { x, 0 });
            }
            GameObject botWall = map.getObjectAt(x, h - 1);
            if (botWall instanceof domain.models.tile.WallTile
                    && ((domain.models.tile.WallTile) botWall).getDecoration() == null) {
                emptyWallTiles.add(new int[] { x, h - 1 });
            }
        }
        Collections.shuffle(emptyWallTiles, random);

        String[] searchables = { "missing_brick.png", "wall_cavity.png", "loose_stone.png", "wall_grill.png",
                "gargoyle.png", "pipe_hole.png" };
        String[] decorations = { "blood_stain.png", "chain.png", "cobweb.png", "crack.png", "moss.png", "skull.png" };

        int numToPlace = Math.min(emptyWallTiles.size(), 12);
        for (int i = 0; i < numToPlace; i++) {
            int[] pos = emptyWallTiles.get(i);
            boolean placeSearchable = includeSearchables && (random.nextBoolean() || !includeDecorations);
            if (!includeSearchables && includeDecorations)
                placeSearchable = false;

            if (placeSearchable) {
                String img = searchables[random.nextInt(searchables.length)];
                String relativePath = "images/WallSearchable/" + img;
                GameObject wallTile = map.getObjectAt(pos[0], pos[1]);
                if (wallTile instanceof domain.models.tile.WallTile) {
                    SearchableObject deco = new SearchableObject("WallSearchable", pos[0], pos[1], relativePath,
                            relativePath);
                    ((domain.models.tile.WallTile) wallTile).setDecoration(deco);
                    deco.setMap(map);
                }
            } else if (includeDecorations) {
                String img = decorations[random.nextInt(decorations.length)];
                String relativePath = "images/WallDecoration/" + img;
                GameObject wallTile = map.getObjectAt(pos[0], pos[1]);
                if (wallTile instanceof domain.models.tile.WallTile) {
                    GameObject wallObj = new WallObject("WallDecoration", pos[0], pos[1], relativePath);
                    ((domain.models.tile.WallTile) wallTile).setDecoration(wallObj);
                    wallObj.setMap(map);
                }
            }
        }
    }

    private void populateBossArenaEnemies(GameMap map, List<Entity> entities, Hero hero) {
        // FinalBoss spawns in the center of the arena
        int w = map.getWidth();
        int h = map.getHeight();
        FinalBoss boss = new FinalBoss(w / 2 - 1, h / 2 - 1);
        entities.add(boss);
    }

    public static void placeRandomLevelDoor(GameMap map, String name) {
        if (map == null)
            return;

        int w = map.getWidth();
        int h = map.getHeight();

        // Check if map already has a LevelDoor
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj instanceof LevelDoor) {
                    // Already has exactly 1 door, keep it and ensure its front tile is clear!
                    int doorX = x;
                    int doorY = y;
                    // Ensure the tile in front of it (insides) is a FloorTile
                    int frontY = (doorY == 0) ? 1 : (doorY == h - 1 ? h - 2 : doorY);
                    if (frontY != doorY) {
                        GameObject frontObj = map.getObjectAt(doorX, frontY);
                        if (frontObj == null || !(frontObj instanceof domain.models.tile.FloorTile)) {
                            map.placeObject(new domain.models.tile.FloorTile(), doorX, frontY);
                        }
                    }
                    System.out.println("Map already has a LevelDoor at (" + doorX + ", " + doorY
                            + "). Keeping it and clearing front.");
                    return;
                }
            }
        }

        // No LevelDoor found. Place a new random LevelDoor on the top wall (y == 0, en
        // sağ/sol tile'lar hariç)
        Random rand = new Random();
        int doorX = rand.nextInt(w - 4) + 2;
        int doorY = 0; // Top wall only

        map.placeObject(new LevelDoor(name, doorX, doorY), doorX, doorY);

        // Ensure the tile in front of the door (doorX, 1) is a FloorTile (obstacle is
        // removed if any)
        map.placeObject(new domain.models.tile.FloorTile(), doorX, 1);

        System.out.println("Placed static LevelDoor '" + name + "' randomly at (" + doorX + ", " + doorY
                + ") and cleared front tile.");
    }

    public static void hideLevelKey(GameMap map) {
        if (map == null) return;

        // 1. Remove any existing LevelKey from the map floor/cells
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj instanceof domain.models.staticObjects.LevelKey) {
                    map.removeObject(obj);
                }
            }
        }

        // 2. Gather candidates
        List<domain.models.entity.Crate> crateCandidates = new ArrayList<>();
        List<domain.models.entity.SearchableObject> searchableCandidates = new ArrayList<>();
        List<domain.models.tile.WallTile> bannerCandidates = new ArrayList<>();

        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj instanceof domain.models.entity.Crate) {
                    crateCandidates.add((domain.models.entity.Crate) obj);
                } else if (obj instanceof domain.models.entity.SearchableObject) {
                    searchableCandidates.add((domain.models.entity.SearchableObject) obj);
                } else if (obj instanceof domain.models.tile.WallTile) {
                    domain.models.tile.WallTile wt = (domain.models.tile.WallTile) obj;
                    GameObject deco = wt.getDecoration();
                    if (deco instanceof domain.models.entity.SearchableObject) {
                        searchableCandidates.add((domain.models.entity.SearchableObject) deco);
                    } else if (deco instanceof domain.models.staticObjects.WallObject) {
                        String img = deco.getImageName();
                        if (img != null && (img.toLowerCase().contains("flag") || img.toLowerCase().contains("banner"))) {
                            bannerCandidates.add(wt);
                        }
                    }
                }
            }
        }

        int total = crateCandidates.size() + searchableCandidates.size() + bannerCandidates.size();
        Random rand = new Random();

        if (total > 0) {
            int choice = rand.nextInt(total);
            if (choice < crateCandidates.size()) {
                domain.models.entity.Crate crate = crateCandidates.get(choice);
                crate.setHiddenItem(new domain.models.staticObjects.LevelKey(crate.getX(), crate.getY()));
                System.out.println("Hid LevelKey inside Crate at (" + crate.getX() + ", " + crate.getY() + ")");
            } else if (choice < crateCandidates.size() + searchableCandidates.size()) {
                int idx = choice - crateCandidates.size();
                domain.models.entity.SearchableObject so = searchableCandidates.get(idx);
                so.setHiddenItem(new domain.models.staticObjects.LevelKey(so.getX(), so.getY()));
                System.out.println("Hid LevelKey inside SearchableObject at (" + so.getX() + ", " + so.getY() + ")");
            } else {
                int idx = choice - crateCandidates.size() - searchableCandidates.size();
                domain.models.tile.WallTile wt = bannerCandidates.get(idx);
                GameObject banner = wt.getDecoration();
                String img = banner.getImageName();
                int bx = wt.getX();
                int by = wt.getY();
                domain.models.entity.SearchableObject searchable = new domain.models.entity.SearchableObject("WallSearchable", bx, by, img, img);
                searchable.setHiddenItem(new domain.models.staticObjects.LevelKey(bx, by));
                wt.setDecoration(searchable);
                searchable.setMap(map);
                System.out.println("Converted banner to SearchableObject and hid LevelKey at (" + bx + ", " + by + ")");
            }
        } else {
            // Fallback: Place on floor at 2, 3 if possible
            map.placeObject(new domain.models.staticObjects.LevelKey(2, 3), 2, 3);
            System.out.println("No hidden candidates found. Placed LevelKey on floor at (2, 3)");
        }
    }

    public static void clearAllOtherSkullKeys(GameMap map, int excludeX, int excludeY) {
        if (map == null) return;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GameObject obj = map.getObjectAt(x, y);
                // 1. Remove from floor
                if (obj instanceof domain.models.staticObjects.LevelKey) {
                    if (x != excludeX || y != excludeY) {
                        map.removeObject(obj);
                    }
                }
                // 2. Remove hidden items from Crates
                if (obj instanceof domain.models.entity.Crate) {
                    domain.models.entity.Crate crate = (domain.models.entity.Crate) obj;
                    if (crate.getHiddenItem() instanceof domain.models.staticObjects.LevelKey) {
                        crate.setHiddenItem(null);
                    }
                }
                // 3. Remove hidden items from SearchableObjects (on floor or walls)
                if (obj instanceof domain.models.entity.SearchableObject) {
                    domain.models.entity.SearchableObject so = (domain.models.entity.SearchableObject) obj;
                    if (so.getHiddenItem() instanceof domain.models.staticObjects.LevelKey) {
                        so.setHiddenItem(null);
                    }
                }
                if (obj instanceof domain.models.tile.WallTile) {
                    domain.models.tile.WallTile wt = (domain.models.tile.WallTile) obj;
                    GameObject deco = wt.getDecoration();
                    if (deco instanceof domain.models.entity.SearchableObject) {
                        domain.models.entity.SearchableObject so = (domain.models.entity.SearchableObject) deco;
                        if (so.getHiddenItem() instanceof domain.models.staticObjects.LevelKey) {
                            so.setHiddenItem(null);
                        }
                    }
                }
            }
        }
    }
}
