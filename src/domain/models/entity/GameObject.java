package domain.models.entity;

/**
 * Base class for everything that can exists on the Grid.
 * Both moving entities (Hero) and static tiles (Wall) are GameObjects.
 */
public abstract class GameObject {
    protected int x, y;
    protected String imageName;
    protected boolean passable;

    public GameObject(int x, int y, String imageName, boolean passable) {
        this.x = x;
        this.y = y;
        this.imageName = imageName;
        this.passable = passable;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public String getImageName() { return imageName; }
    public boolean isPassable() { return passable; }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
