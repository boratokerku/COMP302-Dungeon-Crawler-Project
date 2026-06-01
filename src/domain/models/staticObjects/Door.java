package domain.models.staticObjects;
import domain.models.item.KeyItem;

import domain.models.GameObject;

public class Door extends GameObject {
    private boolean isLocked;

    public Door(String name, int x, int y, boolean isLocked) {
        super(name, x, y, "door/door_closed", false);
        this.isLocked = isLocked;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void unlock() {
        this.isLocked = false;
    }

    public void open() {
        if (!isLocked) {
            this.passable = true;
            this.imageName = "door/door_open";
        }
    }
}