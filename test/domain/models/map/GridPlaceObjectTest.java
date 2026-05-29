package domain.models.map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import domain.models.entity.GameObject;

public class GridPlaceObjectTest {

    private Grid grid;
    private GameMap gameMap;

    @BeforeEach
    public void setUp() {
        grid = new Grid(10, 10);
        gameMap = new GameMap(10, 10);
    }

    private static class TestGameObject extends GameObject {
        public TestGameObject(String name, int x, int y, boolean passable) {
            super(name, x, y, "dummy.png", passable);
        }
    }

    @Test
    public void testPlaceObject_ValidEmptyCell_ReturnsTrue() {
        TestGameObject obj = new TestGameObject("TestObj", 0, 0, true);
        boolean result = grid.placeObject(obj, 5, 5);
        
        assertTrue(result, "Placing on a valid empty cell should return true");
        assertEquals(obj, grid.getObjectAt(5, 5), "Object should be stored at (5, 5)");
        assertEquals(5, obj.getX(), "Object's internal X should be updated");
        assertEquals(5, obj.getY(), "Object's internal Y should be updated");
        assertTrue(grid.repOk(), "Grid representation invariant should hold");
    }

    @Test
    public void testPlaceObject_NegativeX_ReturnsFalse() {
        TestGameObject obj = new TestGameObject("TestObj", 0, 0, true);
        boolean result = grid.placeObject(obj, -1, 5);
        
        assertFalse(result, "Placing with negative X should return false");
        assertNull(grid.getObjectAt(-1, 5), "No object should be placed");
        assertEquals(0, obj.getX(), "Object's internal X should remain unchanged");
        assertTrue(grid.repOk(), "Grid representation invariant should hold");
    }

    @Test
    public void testPlaceObject_ValidCell_SetsMapReference() {
        TestGameObject obj = new TestGameObject("TestObj", 0, 0, true);
        boolean result = gameMap.placeObject(obj, 3, 3);
        
        assertTrue(result, "Placing on a valid GameMap cell should return true");
        assertEquals(obj, gameMap.getObjectAt(3, 3), "Object should be stored at (3, 3)");
        assertNotNull(obj.getMap(), "GameMap reference should be set on the object");
        assertEquals(gameMap, obj.getMap(), "GameMap reference should match the map it was placed on");
        assertTrue(gameMap.repOk(), "GameMap representation invariant should hold");
    }
}
