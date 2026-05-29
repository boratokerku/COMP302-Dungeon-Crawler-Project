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

    @Test
    public void testPlaceWallMountedDecoration() {
        domain.models.tile.WallTile wall = new domain.models.tile.WallTile("wall/wall_1");
        map.placeObject(wall, 1, 1);
        
        domain.models.staticObjects.Decoration torch = new domain.models.staticObjects.Decoration("Torch", 1, 1, "torch/torch_1");
        boolean success = map.placeObject(torch, 1, 1);
        
        assertTrue(success, "Should succeed in placing decoration on a WallTile");
        assertEquals(torch, wall.getDecoration(), "The wall tile should store the decoration");
        assertEquals(1, torch.getX());
        assertEquals(1, torch.getY());
        assertEquals(map, torch.getCurrentMap());
        assertTrue(map.repOk(), "Map repOk should be true after placing a wall-mounted decoration");
    }

    @Test
    public void testEraseWallMountedDecoration() {
        domain.models.tile.WallTile wall = new domain.models.tile.WallTile("wall/wall_1");
        map.placeObject(wall, 1, 1);
        
        domain.models.staticObjects.Decoration torch = new domain.models.staticObjects.Decoration("Torch", 1, 1, "torch/torch_1");
        map.placeObject(torch, 1, 1);
        
        // Erase decoration by placing null
        boolean success = map.placeObject(null, 1, 1);
        
        assertTrue(success, "Should succeed in erasing decoration on a WallTile");
        assertNull(wall.getDecoration(), "The wall tile decoration should be cleared");
        assertEquals(wall, map.getObjectAt(1, 1), "The base wall tile itself must remain intact at the coordinate");
        assertTrue(map.repOk(), "Map repOk should be true after erasing decoration");
    }

    @Test
    public void testSearchableObjectStateToggle() {
        domain.models.entity.SearchableObject chest = new domain.models.entity.SearchableObject(
            "Gold Chest", 2, 2, "containers/gold_chest_closed", "containers/gold_chest_empty"
        );
        map.placeObject(chest, 2, 2);
        
        assertFalse(chest.isSearched(), "Chest should not be searched initially");
        assertEquals("containers/gold_chest_closed", chest.getImageName());
        
        // Trigger search
        chest.search();
        
        assertTrue(chest.isSearched(), "Chest should be marked as searched");
        assertEquals("containers/gold_chest_empty", chest.getImageName(), "Image name should toggle to the open/empty state image");
        assertTrue(map.repOk(), "Map repOk should remain true");
    }
}
