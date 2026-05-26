import domain.models.entity.GameObject;
import domain.models.map.GameMap;
import domain.models.staticObjects.LevelDoor;
import domain.models.tile.WallTile;
import domain.models.tile.FloorTile;
import domain.models.staticObjects.Decoration;
import domain.models.entity.SearchableObject;
import ui.DesignModeView;
import view.TileManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class DesignModeViewConstraintTest {

    private GameMap map;
    private TileManager tileManager;
    private DesignModeView designModeView;

    @BeforeEach
    public void setUp() {
        // Create a 10x10 map
        map = new GameMap(10, 10);
        // Fill top/bottom rows with wall tiles, sides with wall tiles, inside with floor tiles
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                if (x == 0 || x == 9 || y == 0 || y == 9) {
                    map.placeObject(new WallTile("wall/wall_1"), x, y);
                } else {
                    map.placeObject(new FloorTile(), x, y);
                }
            }
        }
        tileManager = new TileManager();
        designModeView = new DesignModeView(
            map,
            tileManager,
            () -> {},
            (m) -> {},
            (m) -> {}
        );
    }

    @Test
    public void testGetLevelDoorX_WhenNoDoor_ShouldReturnMinusOne() throws Exception {
        Method method = DesignModeView.class.getDeclaredMethod("getLevelDoorX");
        method.setAccessible(true);
        int doorX = (int) method.invoke(designModeView);
        assertEquals(-1, doorX, "With no door on top row, getLevelDoorX should return -1");
    }

    @Test
    public void testGetLevelDoorX_WithDoor_ShouldReturnCorrectColumn() throws Exception {
        // Place LevelDoor at (5, 0)
        LevelDoor door = new LevelDoor("Level Gate", 5, 0);
        map.placeObject(door, 5, 0);

        Method method = DesignModeView.class.getDeclaredMethod("getLevelDoorX");
        method.setAccessible(true);
        int doorX = (int) method.invoke(designModeView);
        assertEquals(5, doorX, "getLevelDoorX should return the column where the LevelDoor is placed");
    }

    @Test
    public void testIsWallTilePlaceable_AdjacentToDoor_ShouldReturnFalse() throws Exception {
        // Place LevelDoor at (5, 0)
        LevelDoor door = new LevelDoor("Level Gate", 5, 0);
        map.placeObject(door, 5, 0);

        Method method = DesignModeView.class.getDeclaredMethod("isWallTilePlaceable", int.class, int.class, GameObject.class, boolean.class);
        method.setAccessible(true);

        Decoration torch = new Decoration("Torch", 4, 0, "WallDecoration/torch_1");
        
        // (4, 0) is adjacent to door (left)
        boolean leftPlaceable = (boolean) method.invoke(designModeView, 4, 0, torch, true);
        assertFalse(leftPlaceable, "Wall item should NOT be placeable on left adjacent tile of the door");

        // (6, 0) is adjacent to door (right)
        boolean rightPlaceable = (boolean) method.invoke(designModeView, 6, 0, torch, true);
        assertFalse(rightPlaceable, "Wall item should NOT be placeable on right adjacent tile of the door");

        // (3, 0) is non-adjacent top wall
        boolean okPlaceable = (boolean) method.invoke(designModeView, 3, 0, torch, true);
        assertTrue(okPlaceable, "Wall item should be placeable on non-adjacent wall tile");
    }

    @Test
    public void testRandomMapGeneration_SkipsAdjacentTiles() throws Exception {
        // Trigger random map generation
        Method method = DesignModeView.class.getDeclaredMethod("doGenerateRandomMap");
        method.setAccessible(true);
        method.invoke(designModeView);

        // Find the door column
        int doorX = -1;
        for (int x = 0; x < map.getWidth(); x++) {
            if (map.getObjectAt(x, 0) instanceof LevelDoor) {
                doorX = x;
                break;
            }
        }
        
        assertNotEquals(-1, doorX, "Random map must generate an exit door");

        // Verify that x = doorX - 1 and x = doorX + 1 do NOT have any decorations or searchables on the top wall (y = 0)
        if (doorX > 0) {
            GameObject leftObj = map.getObjectAt(doorX - 1, 0);
            assertTrue(leftObj instanceof WallTile, "Left adjacent object on top row must be a WallTile");
            assertNull(((WallTile) leftObj).getDecoration(), "Left adjacent WallTile must NOT have any decoration");
        }
        if (doorX < map.getWidth() - 1) {
            GameObject rightObj = map.getObjectAt(doorX + 1, 0);
            assertTrue(rightObj instanceof WallTile, "Right adjacent object on top row must be a WallTile");
            assertNull(((WallTile) rightObj).getDecoration(), "Right adjacent WallTile must NOT have any decoration");
        }
    }
}
