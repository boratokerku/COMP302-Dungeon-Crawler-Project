package domain.models.map;

import domain.models.tile.Tile;
import domain.models.entity.GameObject;

public class GameMap extends Grid {

    public GameMap(int width, int height) {
        super(width, height); // Grid constructor (rows, cols)
        initializeEmptyMap();
    }

    private void initializeEmptyMap() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (i == 0 || i == rows - 1 || j == 0 || j == cols - 1) {
                    placeObject(new domain.models.tile.WallTile(), i, j);
                } else {
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