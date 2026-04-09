package domain.models.map;

import domain.models.entity.GameObject;

public class Grid {
    protected int rows;
    protected int cols;
    protected GameObject[][] cells;

    public Grid(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.cells = new GameObject[rows][cols];
    }

    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < rows && y >= 0 && y < cols;
    }

    public boolean isCellEmpty(int x, int y) {
        return isValidPosition(x, y) && cells[x][y] == null;
    }

    public boolean placeObject(GameObject obj, int x, int y) {
        if (!isValidPosition(x, y)) return false;
        
        // Always place on the grid, even if not empty (it replaces the old one)
        cells[x][y] = obj;
        if (obj != null) {
            obj.setPosition(x, y);
        }
        return true;
    }

    public GameObject getObjectAt(int x, int y) {
        if (isValidPosition(x, y)) {
            return cells[x][y];
        }
        return null;
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
}
