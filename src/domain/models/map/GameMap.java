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

    // Getters
    public int getWidth() {
        return rows;
    }

    public int getHeight() {
        return cols;
    }
}