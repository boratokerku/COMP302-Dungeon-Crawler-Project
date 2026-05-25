package domain.models.map;

import domain.models.entity.GameObject;

/**
 * OVERVIEW: GameMap represents a 2D spatial grid layout of the game world, 
 * managing the locations and relationships of various GameObjects.
 *
 * ABSTRACTION FUNCTION (AF):
 * AF(this) = A 2D grid dungeon of dimensions this.rows x this.cols, where each 
 * coordinate index contains the registered GameObject(s) occupying that tile.
 *
 * REPRESENTATION INVARIANT (RI):
 * 1. cells != null, rows > 0, cols > 0
 * 2. cells.length == rows and all row arrays have a length equal to cols.
 * 3. Bidirectional Position Sync: For every GameObject obj at cells[r][c], 
 * obj.getX() == r and obj.getY() == c.
 * 4. Bidirectional Map Sync: For every GameObject obj at cells[r][c], 
 * obj.getCurrentMap() == this.
 * 5. No Duplication (Aliasing): The exact same physical memory reference of a 
 * GameObject cannot occupy more than one cell simultaneously.
 */
public class GameMap extends Grid {

    public GameMap(int width, int height) {
        super(width, height); // Grid constructor (rows, cols)
        initializeEmptyMap();
    }

    private void initializeEmptyMap() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // --- YAN DUVARLAR (öncelik: köşeler dahil tüm sütunlar) ---
                if (i == 0 || i == rows - 1) {
                    placeObject(new domain.models.tile.WallTile("wall/wall_side"), i, j);
                }
                // --- ÜST DUVAR (yan duvarların arasında) ---
                else if (j <= 1) {
                    placeObject(new domain.models.tile.WallTile("wall/wall_1"), i, j);
                }
                // --- ALT DUVAR (yan duvarların arasında) ---
                else if (j >= cols - 2) {
                    placeObject(new domain.models.tile.WallTile("wall/wall_2"), i, j);
                }
                // --- ZEMİN ---
                else {
                    placeObject(new domain.models.tile.FloorTile(), i, j);
                }
            }
        }
    }

    /**
     * Logic katmanı (MovementHandler) burayı sorgulayacak.
     */
    public boolean isWalkable(int x, int y) {
        GameObject obj = getObjectAt(x, y);
        return obj != null && obj.isPassable();
    }

    /**
     * Extends Grid.placeObject() by additionally linking the placed object
     * to this GameMap instance.
     *
     * @param obj The GameObject to place. Can be null to clear the cell.
     * @param x   The x-coordinate where the object should be placed.
     * @param y   The y-coordinate where the object should be placed.
     * @return    true if placement succeeded; false if coordinates are out of bounds.
     *
     * @requires  true (no precondition; all inputs handled defensively)
     * @modifies  this.cells[x][y], obj.position, obj.map (if obj != null and valid)
     * @effects   Calls super.placeObject(obj, x, y). If that returns true and obj
     *            is non-null, additionally calls obj.setMap(this) to register the
     *            map reference on the object. Returns the result of the super call.
     */
    @Override
    public boolean placeObject(GameObject obj, int x, int y) {
        if (!isValidPosition(x, y)) return false;

        GameObject existing = getObjectAt(x, y);

        // Rule: WallObject can only be placed on a WallTile on the top or bottom wall (excluding side walls)
        if (obj instanceof domain.models.staticObjects.WallObject) {
            if (!(existing instanceof domain.models.tile.WallTile) ||
                (y != 0 && y != 1 && y != cols - 2 && y != cols - 1) ||
                x == 0 || x == rows - 1) {
                return false;
            }
        }

        if (existing instanceof domain.models.tile.WallTile) {
            if (obj == null) {
                ((domain.models.tile.WallTile) existing).setDecoration(null);
                return true;
            }
            // Rule: WallObjects and Decorations can be placed on wall tiles
            if (obj instanceof domain.models.staticObjects.WallObject || obj instanceof domain.models.staticObjects.Decoration) {
                ((domain.models.tile.WallTile) existing).setDecoration(obj);
                obj.setPosition(x, y);
                obj.setMap(this);
                return true;
            }
            return false;
        }

        boolean success = super.placeObject(obj, x, y);
        if (success && obj != null) {
            obj.setMap(this);
        }
        return success;
    }

    public void removeObject(GameObject obj) {
        if (obj != null && obj.getX() >= 0 && obj.getX() < getWidth() && obj.getY() >= 0 && obj.getY() < getHeight()) {
            if (getObjectAt(obj.getX(), obj.getY()) == obj) {
                // Remove it by placing a basic FloorTile over it.
                placeObject(new domain.models.tile.FloorTile(), obj.getX(), obj.getY());
            }
        }
    }

    // Getters
    public int getWidth() {
        return rows;
    }

    public int getHeight() {
        return cols;
    }

    @Override
    public boolean repOk() {
        if (!super.repOk()) {
            return false;
        }

        java.util.Set<GameObject> seenObjects = new java.util.HashSet<>();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                GameObject obj = cells[r][c];
                if (obj != null) {
                    // 3. Bidirectional Position Sync
                    if (obj.getX() != r || obj.getY() != c) {
                        return false;
                    }

                    // 4. Bidirectional Map Sync
                    if (obj.getCurrentMap() != this) {
                        return false;
                    }

                    // 5. No Duplication (Aliasing)
                    if (seenObjects.contains(obj)) {
                        return false;
                    }
                    seenObjects.add(obj);
                }
            }
        }

        return true;
    }
}