package domain.models.tile;

public class WallTile extends Tile {
    public WallTile() {
        super(0, 0, "wall", false);
    }

    public WallTile(String imageName) {
        super(0, 0, imageName, false);
    }
}