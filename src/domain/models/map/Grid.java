package domain.models.map;

import domain.models.entity.GameObject;

/**
 * Overview: Grid represents a 2D rectangular map of cells, where each cell
 * can hold at most one GameObject. It provides bounds checking and bidirectional
 * placement (updating both the grid cell and the object's internal position).
 *
 * Abstraction Function:
 *   AF(this) = a 2D grid of size rows x cols where
 *   AF(cells[x][y]) = the GameObject occupying position (x,y), or null if empty.
 *
 * Representation Invariant:
 *   - cells != null
 *   - cells.length == rows
 *   - for all x: cells[x] != null && cells[x].length == cols
 *   - rows > 0 && cols > 0
 */
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

    /**
     * Places a given GameObject onto the grid at the specified coordinates.
     * If another object already exists at the target cell, it will be overwritten.
     *
     * @param obj The GameObject to place on the grid. Can be null to clear the cell.
     * @param x   The x-coordinate (row index) where the object should be placed.
     * @param y   The y-coordinate (column index) where the object should be placed.
     * @return    true if the object was successfully placed;
     *            false if the coordinates are out of bounds.
     *
     * @requires  true (no precondition; all inputs handled defensively)
     * @modifies  this.cells[x][y], obj.position (if obj != null and coords are valid)
     * @effects   If (x, y) is outside grid boundaries, returns false and leaves
     *            this and obj unchanged.
     *            Otherwise, sets cells[x][y] = obj, updates obj's internal
     *            position to (x, y) if obj is non-null, and returns true.
     */
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

    /**
     * Checks whether the representation invariant holds for this Grid.
     * @return true if the rep invariant is satisfied; false otherwise.
     */
    public boolean repOk() {
        if (cells == null) return false;
        if (cells.length != rows) return false;
        for (int i = 0; i < rows; i++) {
            if (cells[i] == null) return false;
            if (cells[i].length != cols) return false;
        }
        return rows > 0 && cols > 0;
    }
}
