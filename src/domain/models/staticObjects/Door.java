package domain.models.staticObjects;

public class Door extends StaticObject {
    private boolean isOpen;
    private boolean isLocked;

    public Door(int x, int y, boolean isLocked) {
        super(x, y, true, false);
        this.isOpen = false;
        this.isLocked = isLocked;
    }

    public void open() {
        if (!isLocked) {
            this.isOpen = true;
            this.obstacle = false;
        }
    }
}