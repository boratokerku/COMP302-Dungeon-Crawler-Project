import domain.models.Direction;
import domain.models.entity.Entity;
import domain.models.entity.ShadowClone;
import domain.models.map.GameMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test class for ShadowClone.moveOpposite().
 *
 * This test suite verifies:
 * - Correct opposite-direction movement
 * - Collision prevention with other entities
 * - Prevention of movement into blocked or non-walkable tiles
 *
 * Test Method:
 * moveOpposite(Direction heroDir, GameMap map, List<Entity> entities)
 */

public class ShadowCloneTest {

    private ShadowClone clone;
    private GameMap map;
    private List<Entity> entities;

    @BeforeEach
    public void setUp() {
        map = new GameMap(5, 5);
        clone = new ShadowClone(2, 2);

        entities = new ArrayList<>();
        entities.add(clone);
    }

    /**
     * Tests whether ShadowClone correctly moves
     * in the opposite direction of the hero.
     *
     * Expected:
     * Hero UP -> Clone DOWN
     */
    @Test
    public void testMoveOpposite_ValidDirection_ShouldMoveClone() {
        clone.moveOpposite(Direction.UP, map, entities);

        assertEquals(2, clone.getX());
        assertEquals(3, clone.getY());
    }

    /**
     * Tests whether ShadowClone avoids movement
     * into an occupied tile.
     *
     * Expected:
     * Clone position should remain unchanged.
     */
    @Test
    public void testMoveOpposite_BlockedByAnotherClone_ShouldNotMove() {
        ShadowClone blocker = new ShadowClone(3, 2);
        entities.add(blocker);

        clone.moveOpposite(Direction.LEFT, map, entities);

        assertEquals(2, clone.getX());
        assertEquals(2, clone.getY());
    }

    /**
     * Tests whether ShadowClone respects
     * map walkability restrictions.
     *
     * Expected:
     * Clone should not move into blocked tiles.
     */
    @Test
    public void testMoveOpposite_WallTile_ShouldNotMove() {
        clone.setPosition(1, 1);

        clone.moveOpposite(Direction.DOWN, map, entities);

        assertEquals(1, clone.getX());
        assertEquals(1, clone.getY());
    }
}