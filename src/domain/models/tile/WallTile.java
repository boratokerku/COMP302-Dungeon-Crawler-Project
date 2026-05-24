package domain.models.tile;

import domain.models.entity.GameObject;

public class WallTile extends Tile {
    private GameObject decoration;

    public WallTile() {
        super(0, 0, "wall", false);
    }

    public WallTile(String imageName) {
        super(0, 0, imageName, false);
    }

    public GameObject getDecoration() {
        return decoration;
    }

    public void setDecoration(GameObject decoration) {
        this.decoration = decoration;
        if (decoration != null) {
            decoration.setPosition(this.getX(), this.getY());
        }
    }
}