package domain.models.map;

import domain.models.entity.GameObject;

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
                else if (j == 0) {
                    placeObject(new domain.models.tile.WallTile("wall/wall_1"), i, j);
                }
                // --- ALT DUVAR (yan duvarların arasında) ---
                else if (j == cols - 1) {
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
}