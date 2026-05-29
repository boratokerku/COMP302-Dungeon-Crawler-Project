package domain.models.entity;

/**
 * Base class for everything that can exists on the Grid.
 * Both moving entities (Hero) and static tiles (Wall) are GameObjects.
 */
public abstract class GameObject {
    protected String name;
    protected int x, y;
    protected String imageName;
    protected boolean passable;
    
    private domain.models.map.GameMap map;
    private java.util.List<domain.logic.Action> actions = new java.util.ArrayList<>();
    protected double customScale = 1.0;

    public double getCustomScale() { return customScale; }
    public void setCustomScale(double scale) { this.customScale = scale; }

    // Keep strict backwards compatibility for original constructor
    public GameObject(int x, int y, String imageName, boolean passable) {
        this("Unknown Object", x, y, imageName, passable);
    }
    
    // New overloaded constructor with name
    public GameObject(String name, int x, int y, String imageName, boolean passable) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.imageName = imageName;
        this.passable = passable;
    }

    public String getName() { return name == null ? "Object" : name; }
    public void setName(String name) { this.name = name; }

    public domain.models.map.GameMap getMap() { return map; }
    public void setMap(domain.models.map.GameMap map) { this.map = map; }
    public domain.models.map.GameMap getCurrentMap() { return map; }

    public java.util.List<domain.logic.Action> getActions() { return actions; }
    public void addAction(domain.logic.Action action) { this.actions.add(action); }

    public int getX() { return x; }
    public int getY() { return y; }
    public String getImageName() { return imageName; }
    public boolean isPassable() { return passable; }

    public boolean occupiesTile(int tx, int ty) {
        return this.x == tx && this.y == ty;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
