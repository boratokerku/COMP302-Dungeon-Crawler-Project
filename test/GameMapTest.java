package domain.models.map;

import domain.models.entity.GameObject;
import domain.models.entity.Hero;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GameMapTest {

    private GameMap map;

    @BeforeEach
    public void setUp() {
        // Create a 5x5 map
        map = new GameMap(5, 5);
    }

    @Test
    public void testRepOk_ValidInitialState_ShouldReturnTrue() {
        assertTrue(map.repOk(), "A freshly initialized map should be valid and return true for repOk()");
    }

    @Test
    public void testRepOk_WhenCoordinateDesyncOccurs_ShouldReturnFalse() {
        Hero hero = new Hero(1, 1);
        map.placeObject(hero, 1, 1);
        hero.setCurrentMap(map);

        // Ensure the initial state is valid
        assertTrue(map.repOk(), "Map should be valid initially");

        // Break bidirectional position sync: alter hero's coordinates without updating grid
        hero.setPosition(2, 2);

        assertFalse(map.repOk(), "repOk() should return false when a GameObject's coordinates do not match its grid cell");
    }

    @Test
    public void testRepOk_WhenMapReferenceDesyncOccurs_ShouldReturnFalse() {
        Hero hero = new Hero(1, 1);
        map.placeObject(hero, 1, 1);
        hero.setCurrentMap(map);

        // Ensure the initial state is valid
        assertTrue(map.repOk(), "Map should be valid initially");

        // Break bidirectional map reference sync: set hero's map pointer to null
        hero.setCurrentMap(null);

        assertFalse(map.repOk(), "repOk() should return false when a GameObject's map reference is desynced");
    }

    @Test
    public void testRepOk_WhenEntityIsDuplicated_ShouldReturnFalse() {
        Hero hero = new Hero(1, 1);
        map.placeObject(hero, 1, 1);
        hero.setCurrentMap(map);

        // Ensure the initial state is valid
        assertTrue(map.repOk(), "Map should be valid initially");

        // Manually duplicate the exact same Hero memory reference into another cell
        map.cells[2][2] = hero;

        assertFalse(map.repOk(), "repOk() should return false when the exact same GameObject occupies more than one cell");
    }

    @Test
    public void testRepOk_WhenGridStructureCorrupted_ShouldReturnFalse() {
        // Ensure the initial state is valid
        assertTrue(map.repOk(), "Map should be valid initially");

        // Corrupt internal grid structure: set a row of cells to null
        map.cells[2] = null;

        assertFalse(map.repOk(), "repOk() should return false when the internal grid structure is corrupted");
    }
}
