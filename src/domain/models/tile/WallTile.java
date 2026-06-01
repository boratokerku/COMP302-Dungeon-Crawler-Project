package domain.models.tile;

import domain.models.GameObject;

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

    @Override
    public WallTile clone() {
        WallTile cloned = (WallTile) super.clone();
        if (this.decoration != null) {
            cloned.decoration = this.decoration.clone();
        }
        return cloned;
    }
}