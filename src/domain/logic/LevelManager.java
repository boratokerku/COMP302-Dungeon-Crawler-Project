package domain.logic;

import domain.models.entity.*;
import domain.models.entity.Crate;
import domain.models.map.GameMap;
import domain.models.staticObjects.*;
import domain.models.item.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * State machine managing multi-level dungeon progression.
 * 
 * Level 1 (Entry):      Uses the designed/default map. Regular enemies.
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
     * @return The generated GameMap for the new level, or null if already at max level.
     */
    public GameMap advanceLevel() {
        if (isLastLevel()) return null;
        currentLevel++;
        return generateLevel(currentLevel);
    }

    /**
     * Generates a map for the specified level with appropriate obstacles and decorations.
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
     * @param level The level number
     * @param map The game map
     * @param entities The entity list to populate (hero should already be in it)
     * @param hero The hero reference
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
        map.placeObject(new PotionItem(3, 8), 3, 8);
        map.placeObject(new PotionItem(w - 4, 4), w - 4, 4);

        // Torches for atmosphere
        map.placeObject(new Decoration("Torch", 3, 1, "torch/torch_1"), 3, 1);
        map.placeObject(new Decoration("Torch", w / 2, 1, "torch/torch_1"), w / 2, 1);
        map.placeObject(new Decoration("Torch", w - 5, 1, "torch/torch_1"), w - 5, 1);

        // Level Door to Boss Arena (locked, needs Level Key)
        placeRandomLevelDoor(map, "Boss Gate");

        // Level Key hidden in the level
        map.placeObject(new LevelKey(2, 3), 2, 3);
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

        // Some potions for the boss fight
        map.placeObject(new PotionItem(2, 2), 2, 2);
        map.placeObject(new PotionItem(w - 3, 2), w - 3, 2);
        map.placeObject(new PotionItem(2, h - 3), 2, h - 3);
        map.placeObject(new PotionItem(w - 3, h - 3), w - 3, h - 3);
    }

    private void populateBossArenaEnemies(GameMap map, List<Entity> entities, Hero hero) {
        // FinalBoss spawns in the center of the arena
        int w = map.getWidth();
        int h = map.getHeight();
        FinalBoss boss = new FinalBoss(w / 2 - 1, h / 2 - 1);
        entities.add(boss);
    }

    public static void placeRandomLevelDoor(GameMap map, String name) {
        int w = map.getWidth();
        int middleX = w / 2;
        int doorY = 0; // Top wall
        map.placeObject(new LevelDoor(name, middleX, doorY), middleX, doorY);
        System.out.println("Placed static LevelDoor '" + name + "' at (" + middleX + ", " + doorY + ")");
    }
}
