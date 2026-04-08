package domain.models.staticObjects;

public abstract class StaticObject {
    protected int x, y;
    protected boolean obstacle; // Üzerinden yürünebilir mi?
    protected boolean breakable; // Kırılabiliyor mu? (Crate, Vase gibi)

    public StaticObject(int x, int y, boolean obstacle, boolean breakable) {
        this.x = x;
        this.y = y;
        this.obstacle = obstacle;
        this.breakable = breakable;
    }

    // Getters
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isObstacle() {
        return obstacle;
    }
}