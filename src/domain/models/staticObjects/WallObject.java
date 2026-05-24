package domain.models.staticObjects;

import domain.models.entity.GameObject;

public class WallObject extends GameObject {
    public WallObject(String name, int x, int y, String imageName) {
        super(name, x, y, imageName, true); // Passable = true
    }
}
