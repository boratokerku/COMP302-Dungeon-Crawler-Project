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
import javax.swing.SwingUtilities;
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

    @Test
    public void testHoverHighlight_WallItemOnFloorTile_ShouldSetColorToRed() throws Exception {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(100, 100, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2d = img.createGraphics();

        // Hover over a floor tile (e.g. 3, 3)
        java.lang.reflect.Field hoverTileXField = DesignModeView.class.getDeclaredField("hoverTileX");
        java.lang.reflect.Field hoverTileYField = DesignModeView.class.getDeclaredField("hoverTileY");
        hoverTileXField.setAccessible(true);
        hoverTileYField.setAccessible(true);
        hoverTileXField.set(designModeView, 3);
        hoverTileYField.set(designModeView, 3);

        // Select a Wall item in the palette
        java.lang.reflect.Field selectedPanelField = DesignModeView.class.getDeclaredField("selectedPanel");
        java.lang.reflect.Field selectedPaletteIdxField = DesignModeView.class.getDeclaredField("selectedPaletteIdx");
        selectedPanelField.setAccessible(true);
        selectedPaletteIdxField.setAccessible(true);
        selectedPanelField.set(designModeView, "WALL_ITEM");
        selectedPaletteIdxField.set(designModeView, 0);

        // Call paintHoverHighlight
        Method paintMethod = DesignModeView.class.getDeclaredMethod("paintHoverHighlight", java.awt.Graphics2D.class);
        paintMethod.setAccessible(true);
        paintMethod.invoke(designModeView, g2d);

        // Check color of g2d - the border color is set to red: Color(255, 80, 80, 200)
        java.awt.Color lastColor = g2d.getColor();
        assertEquals(new java.awt.Color(255, 80, 80, 200), lastColor, "Hovering a wall item over a floor tile must display a red border indicator");
        
        g2d.dispose();
    }

    @Test
    public void testIsWallTilePlaceable_WhenWallHasDecoration_ShouldReturnTrue() throws Exception {
        // Place LevelDoor at (5, 0)
        LevelDoor door = new LevelDoor("Level Gate", 5, 0);
        map.placeObject(door, 5, 0);

        Method method = DesignModeView.class.getDeclaredMethod("isWallTilePlaceable", int.class, int.class, GameObject.class, boolean.class);
        method.setAccessible(true);

        Decoration torch1 = new Decoration("Torch 1", 3, 0, "WallDecoration/torch_1");
        Decoration torch2 = new Decoration("Torch 2", 3, 0, "WallDecoration/torch_1");

        // Place torch1 at (3, 0) first
        GameObject wallObj = map.getObjectAt(3, 0);
        assertTrue(wallObj instanceof WallTile);
        map.placeObject(torch1, 3, 0);

        // Try to place torch2 at (3, 0) - should return true because we can replace/overwrite it
        boolean placeable = (boolean) method.invoke(designModeView, 3, 0, torch2, true);
        assertTrue(placeable, "Should return true for placement even when wall tile already has decoration, as it is replaceable");
    }

    @Test
    public void testGetDecorDimensions_SpecificSizesAndTorchClamping() throws Exception {
        Method method = DesignModeView.class.getDeclaredMethod("getDecorDimensions", 
            String.class, double.class, int.class, int.class, int.class, String.class);
        method.setAccessible(true);

        // Native sizing (tileSize = 64)
        int[] chainDims = (int[]) method.invoke(designModeView, "images/WallDecoration/chain.png", 1.0, 64, 64, 64, "Chain");
        assertEquals(60, chainDims[0], "Chain should render at exactly 60 px wide at native scale");

        int[] mossDims = (int[]) method.invoke(designModeView, "images/WallDecoration/moss.png", 1.0, 64, 64, 64, "Moss");
        assertEquals(55, mossDims[0], "Moss should render at exactly 55 px wide at native scale");

        int[] crackDims = (int[]) method.invoke(designModeView, "images/WallDecoration/crack.png", 1.0, 64, 64, 64, "Crack");
        assertEquals(70, crackDims[0], "Crack should render at exactly 70 px wide at native scale");

        // Scaled sizing inside editor (tileSize = 32)
        int[] chainDims32 = (int[]) method.invoke(designModeView, "images/WallDecoration/chain.png", 1.0, 32, 64, 64, "Chain");
        assertEquals(30, chainDims32[0], "Chain should scale down to 30 px wide at 32px tile size");

        int[] mossDims32 = (int[]) method.invoke(designModeView, "images/WallDecoration/moss.png", 1.0, 32, 64, 64, "Moss");
        assertEquals(28, mossDims32[0], "Moss should scale down to 28 px wide at 32px tile size");

        // WallSearchable items (e.g. wall_grill) should NOT scale down at tileSize=32
        int[] grillDims64 = (int[]) method.invoke(designModeView, "images/WallSearchable/wall_grill.png", 1.0, 64, 64, 64, "WallGrill");
        assertEquals(50, grillDims64[0], "Wall Grill should render at exactly 50 px wide at native scale");

        int[] grillDims32 = (int[]) method.invoke(designModeView, "images/WallSearchable/wall_grill.png", 1.0, 32, 64, 64, "WallGrill");
        assertEquals(50, grillDims32[0], "Wall Grill should still render at exactly 50 px wide at 32px tile size (no scaling)");

        // Wall Torch (60 px)
        int[] torchDefaultDims = (int[]) method.invoke(designModeView, "torch/torch_1", 1.0, 64, 64, 64, "Torch");
        assertEquals(60, torchDefaultDims[0], "Wall Torch should render at exactly 60 px wide by default inside 64px tile size");

        // Torch clamping - raw dimensions iw=100, ih=200, scale=10.0 (huge scale)
        // With tileSize=32, torch should be clamped to NOT exceed 32
        int[] torchDims = (int[]) method.invoke(designModeView, "torch/torch_1", 10.0, 32, 100, 200, "Torch");
        assertTrue(torchDims[0] <= 32 && torchDims[1] <= 32, 
            "Torch must be clamped to not exceed the wall tile size (32px), but was width: " + torchDims[0] + ", height: " + torchDims[1]);
    }

    @Test
    public void testCloneMap_RestartVsResumeState() throws Exception {
        GameMap original = new GameMap(5, 5);
        
        // 1. Get pre-existing WallTile at (1, 0) and place a SearchableObject decoration
        WallTile wall = (WallTile) original.getObjectAt(1, 0);
        assertNotNull(wall);
        SearchableObject wallSo = new SearchableObject("WallSearchable", 1, 0, "images/WallSearchable/gargoyle.png", "images/WallSearchable/gargoyle.png");
        wallSo.setSearched(true);
        wallSo.setTrapTriggered(true);
        domain.models.staticObjects.LevelKey key1 = new domain.models.staticObjects.LevelKey(1, 0);
        wallSo.setHiddenItem(key1);
        original.placeObject(wallSo, 1, 0);

        // 2. Create a floor-mounted SearchableObject
        SearchableObject floorSo = new SearchableObject("Chest", 2, 2, "containers/chest_brown", "containers/chest_brown");
        floorSo.setSearched(true);
        floorSo.setTrapTriggered(true);
        domain.models.staticObjects.KeyItem key2 = new domain.models.staticObjects.KeyItem(2, 2);
        floorSo.setHiddenItem(key2);
        original.placeObject(floorSo, 2, 2);

        // Access private cloneMap method via reflection
        Method cloneMethod = DemoRunner.class.getDeclaredMethod("cloneMap", GameMap.class, boolean.class);
        cloneMethod.setAccessible(true);

        // 3. Test Resume Mode (isRestart = false)
        GameMap resumeClone = (GameMap) cloneMethod.invoke(null, original, false);
        assertNotNull(resumeClone);
        
        // Verify wall decoration on resume clone
        WallTile clonedWall = (WallTile) resumeClone.getObjectAt(1, 0);
        assertNotNull(clonedWall);
        SearchableObject clonedWallSo = (SearchableObject) clonedWall.getDecoration();
        assertNotNull(clonedWallSo);
        assertTrue(clonedWallSo.isSearched(), "Wall searchable should remain searched on resume");
        assertTrue(clonedWallSo.isTrapTriggered(), "Wall searchable trap status should be preserved on resume");
        assertNotNull(clonedWallSo.getHiddenItem(), "Wall searchable hidden item should be preserved on resume");
        assertTrue(clonedWallSo.getHiddenItem() instanceof domain.models.staticObjects.LevelKey, "Hidden item type should be preserved");

        // Verify floor searchable on resume clone
        SearchableObject clonedFloorSo = (SearchableObject) resumeClone.getObjectAt(2, 2);
        assertNotNull(clonedFloorSo);
        assertTrue(clonedFloorSo.isSearched(), "Floor searchable should remain searched on resume");
        assertTrue(clonedFloorSo.isTrapTriggered(), "Floor searchable trap status should be preserved on resume");
        assertNotNull(clonedFloorSo.getHiddenItem(), "Floor searchable hidden item should be preserved on resume");

        // 4. Test Restart Mode (isRestart = true)
        GameMap restartClone = (GameMap) cloneMethod.invoke(null, original, true);
        assertNotNull(restartClone);

        // Verify wall decoration on restart clone
        WallTile restartedWall = (WallTile) restartClone.getObjectAt(1, 0);
        assertNotNull(restartedWall);
        SearchableObject restartedWallSo = (SearchableObject) restartedWall.getDecoration();
        assertNotNull(restartedWallSo);
        assertFalse(restartedWallSo.isSearched(), "Wall searchable should be reset to unsearched on restart");
        assertFalse(restartedWallSo.isTrapTriggered(), "Wall searchable trap status should be reset to false on restart");
        assertNull(restartedWallSo.getHiddenItem(), "Wall searchable hidden item should be reset to null on restart");

        // Verify floor searchable on restart clone
        SearchableObject restartedFloorSo = (SearchableObject) restartClone.getObjectAt(2, 2);
        assertNotNull(restartedFloorSo);
        assertFalse(restartedFloorSo.isSearched(), "Floor searchable should be reset to unsearched on restart");
        assertFalse(restartedFloorSo.isTrapTriggered(), "Floor searchable trap status should be reset to false on restart");
        assertNull(restartedFloorSo.getHiddenItem(), "Floor searchable hidden item should be reset to null on restart");
    }

    @Test
    public void testSearchPopupDialog_InstantiationAndState() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ui.SearchPopupDialog dialog = new ui.SearchPopupDialog(null, "Gargoyle", () -> {});
            assertNotNull(dialog, "Dialog should be successfully instantiated");
            assertFalse(dialog.isSearchTriggered(), "Initial searchTriggered should be false");
            dialog.dispose();
        });
    }

    @Test
    public void testSaveAndLoadInventoryAndChestPersistence() throws Exception {
        domain.models.entity.Hero hero = new domain.models.entity.Hero(1, 1);
        GameMap gameMap = new GameMap(5, 5);
        hero.setCurrentMap(gameMap);
        
        domain.models.staticObjects.KeyItem goldKey2 = new domain.models.staticObjects.KeyItem("Golden Key 2", 1, 1, "images/items/key/golden_key_2.png");
        hero.getInventory().addItem(goldKey2);
        
        domain.models.entity.Chest goldChest = new domain.models.entity.Chest("Gold Chest", 2, 2, true, "containers/gold_chest_closed");
        gameMap.placeObject(goldChest, 2, 2);
        
        // Use SaveManager to save game progress
        domain.logic.SaveManager.save("test_persistence_save", hero, new ArrayList<>(), gameMap, null, null, 1, 0);
        
        // Load the saved progress state
        domain.models.GameState loadedState = domain.logic.SaveManager.load("test_persistence_save");
        assertNotNull(loadedState, "Loaded state should not be null");
        
        // Clean up save file
        new java.io.File("saves/test_persistence_save.json").delete();
        
        // Verify inventory items
        assertEquals(1, loadedState.inventoryItems.size(), "Inventory size should be 1");
        domain.models.GameState.ItemRecord invRec = loadedState.inventoryItems.get(0);
        assertEquals("KeyItem", invRec.type);
        assertEquals("Golden Key 2", invRec.name);
        assertEquals("images/items/key/golden_key_2.png", invRec.imageName);
        
        // Verify map items
        boolean foundChest = false;
        for (domain.models.GameState.ItemRecord mapRec : loadedState.mapItems) {
            if ("Chest".equals(mapRec.type)) {
                assertEquals("Gold Chest", mapRec.name);
                assertEquals("containers/gold_chest_closed", mapRec.imageName);
                assertTrue(mapRec.isLocked);
                foundChest = true;
            }
        }
        assertTrue(foundChest, "Should have saved and loaded the Gold Chest");
    }
}
