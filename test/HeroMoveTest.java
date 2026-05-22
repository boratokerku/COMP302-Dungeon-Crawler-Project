import domain.models.entity.Hero;
import domain.models.map.GameMap;
import domain.models.tile.FloorTile;
import domain.models.tile.WallTile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 tests for Hero.move(int dx, int dy).
 *
 * Tested method:
 *   public boolean move(int dx, int dy)
 *
 * Spec (from Hero.java Javadoc):
 *   @requires (dx >= -1 && dx <= 1) && (dy >= -1 && dy <= 1)
 *   @modifies this.x, this.y
 *   @effects  If the target position (this.x + dx, this.y + dy) is within the map
 *             boundaries and there is no collision with a Static Object (Wall),
 *             updates the hero's coordinates to the target position. If a collision
 *             occurs or the target is out of bounds, the hero's position remains unchanged.
 */
public class HeroMoveTest {

    /**
     * A 5x5 GameMap is used for each test.
     * GameMap's constructor places WallTiles along the border and FloorTiles
     * in the interior, so positions (1,1) through (3,3) are walkable floors.
     *
     * Coordinate scheme (x = row, y = col):
     *   (0,*) and (4,*) → wall rows
     *   (*,0) and (*,4) → wall columns
     *   (1..3, 1..3)    → walkable floor
     */
    private static final int MAP_SIZE = 5;

    private GameMap map;
    private Hero hero;

    /**
     * Builds a fresh 5x5 map and a Hero before every test.
     * The Hero is registered with the map so that move(dx, dy) can query it.
     */
    @BeforeEach
    public void setUp() {
        map  = new GameMap(MAP_SIZE, MAP_SIZE);
        hero = new Hero(1, 1);           // start position; coordinates set by placeObject
        map.placeObject(hero, 1, 1);     // registers the hero on the grid at (1,1)
        hero.setCurrentMap(map);         // hero needs map reference for move(dx,dy)
    }

    // -----------------------------------------------------------------------
    // Test 1 – Normal Case
    // -----------------------------------------------------------------------

    /**
     * EFFECTS: When the target tile (2,1) is an empty floor and within bounds,
     * move(1, 0) must update the hero's position to (2,1).
     *
     * Setup: 5x5 map; (2,1) is a FloorTile (passable = true) by default.
     * Hero starts at (1,1). move(1, 0) targets (2,1).
     */
    @Test
    public void testMove_NormalEmptyTile_ShouldUpdatePosition() {
        // (2,1) is a floor by default – no extra setup needed.
        boolean moved = hero.move(1, 0);

        assertTrue(moved, "move() should return true when moving to an empty floor");
        assertEquals(2, hero.getX(), "Hero's x-coordinate should be 2 after moving right");
        assertEquals(1, hero.getY(), "Hero's y-coordinate should remain 1");
    }

    // -----------------------------------------------------------------------
    // Test 2 – Boundary Case: Wall Collision
    // -----------------------------------------------------------------------

    /**
     * EFFECTS: When a WallTile (non-passable) occupies (2,1), move(1, 0)
     * must leave the hero's position unchanged at (1,1).
     *
     * Setup: Place a WallTile at (2,1) to simulate a solid wall.
     * Hero starts at (1,1). move(1, 0) targets the blocked cell (2,1).
     */
    @Test
    public void testMove_HitWallCollision_ShouldNotUpdatePosition() {
        // Overwrite the floor at (2,1) with a non-passable WallTile.
        map.placeObject(new WallTile(), 2, 1);

        boolean moved = hero.move(1, 0);

        assertFalse(moved, "move() should return false when blocked by a Wall");
        assertEquals(1, hero.getX(), "Hero's x-coordinate must remain 1 after hitting a wall");
        assertEquals(1, hero.getY(), "Hero's y-coordinate must remain 1 after hitting a wall");
    }

    // -----------------------------------------------------------------------
    // Test 3 – Edge Case: Out of Bounds
    // -----------------------------------------------------------------------

    /**
     * EFFECTS: When the target position (-1, 0) is outside the map grid,
     * move(-1, 0) must leave the hero's position unchanged at (0,0).
     *
     * Setup: Manually set hero's position to (0,0) (the border corner).
     * move(-1, 0) would target (-1, 0), which is outside valid bounds.
     */
    @Test
    public void testMove_OutOfBounds_ShouldNotUpdatePosition() {
        // Relocate hero to the top-left corner of the map.
        hero.setPosition(0, 0);

        boolean moved = hero.move(-1, 0);

        assertFalse(moved, "move() should return false when the target is out of bounds");
        assertEquals(0, hero.getX(), "Hero's x-coordinate must remain 0 when move goes out of bounds");
        assertEquals(0, hero.getY(), "Hero's y-coordinate must remain 0 when move goes out of bounds");
    }
}
