package domain.models.map;

import domain.models.tile.Tile;

public class GameMap {
    private int width;
    private int height;
    private Tile[][] grid;

    public GameMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new Tile[width][height];
        initializeEmptyMap();
    }

    private void initializeEmptyMap() {
        // Haritayı başlangıçta boş zeminlerle doldur
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // grid[i][j] = new FloorTile(); // Örnek
            }
        }
    }

    /**
     * Belirli bir koordinatın yürünebilir (walkable) olup olmadığını kontrol eder.
     * Logic katmanı (MovementHandler) burayı sorgulayacak.
     */
    public boolean isWalkable(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return false; // Harita dışı
        }
        return grid[x][y].isPassable(); // Tile'ın kendi özelliği
    }

    // Getters
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}