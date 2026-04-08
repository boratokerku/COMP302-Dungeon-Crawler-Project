package domain.models.staticObjects;

public class StaticObject {
    protected int x;
    protected int y;
    protected boolean obstacle;
    protected boolean interactable;

    public StaticObject() {
    }

    public StaticObject(int x, int y, boolean obstacle, boolean interactable) {
        this.x = x;
        this.y = y;
        this.obstacle = obstacle;
        this.interactable = interactable;
    }
}
