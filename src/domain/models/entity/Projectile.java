package domain.models.entity;

public class Projectile extends Entity {
    private double deltaX, deltaY;
    private double exactX, exactY;
    private int damage;
    private Entity owner;
    private String type = "SPELL"; // Default type for compatibility

    public Projectile(int x, int y, double exactX, double exactY, double deltaX, double deltaY, int damage, Entity owner) {
        super(x, y, 1);
        this.exactX = exactX;
        this.exactY = exactY;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.damage = damage;
        this.owner = owner;
    }

    public Projectile(int x, int y, double exactX, double exactY, double deltaX, double deltaY, int damage, Entity owner, String type) {
        this(x, y, exactX, exactY, deltaX, deltaY, damage, owner);
        this.type = type;
    }

    public String getType() {
        return type;
    }

    /**
     * Her logic tick'te çağrılır.
     * Hareketi ve duvar çarpışmasını yönetir.
     * Duvara çarparsa alive = false olur.
     */
    public void step(domain.models.map.GameMap map) {
        if (!this.alive) return;

        exactX += deltaX;
        exactY += deltaY;

        int newX = (int) Math.round(exactX);
        int newY = (int) Math.round(exactY);

        // Döküman §2.5.2: "cannot pass through walls or grid cells occupied by large objects"
        if (!map.isWalkable(newX, newY)) {
            this.alive = false;
            System.out.println("Projectile hit a wall at (" + newX + ", " + newY + ")");
            return;
        }

        this.x = newX;
        this.y = newY;
    }

    @Override
    public void update() {
        // Hareket mantığı DemoRunner'da step(map) üzerinden yönetilir
    }

    public int getDamage()  { return damage; }
    public Entity getOwner() { return owner; }
    public double getDeltaX()   { return deltaX; }
    public double getDeltaY()   { return deltaY; }
    public double getExactX()   { return exactX; }
    public double getExactY()   { return exactY; }
}