package domain.models.staticObjects;

import domain.models.entity.GameObject;

public class Decoration extends GameObject {
    public Decoration(String name, int x, int y, String imageName) {
        super(name, x, y, imageName, true); // Passable = true
    }
}
