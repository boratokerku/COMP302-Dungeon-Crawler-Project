package domain.models.entity;

public class Projectile extends Entity {
    private int deltaX, deltaY;
    private int damage;
    private Entity owner;
    private int moveCooldown = 0;
    private static final int MOVE_INTERVAL = 2; // Her 2 tick'te bir hareket (240ms)

    public Projectile(int x, int y, int deltaX, int deltaY, int damage, Entity owner) {
        super(x, y, 1);
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.damage = damage;
        this.owner = owner;
    }

    /**
     * Her logic tick'te çağrılır.
     * Hareketi ve duvar çarpışmasını yönetir.
     * Duvara çarparsa alive = false olur.
     */
    public void step(domain.models.map.GameMap map) {
        if (!this.alive) return;

        moveCooldown++;
        if (moveCooldown < MOVE_INTERVAL) return;
        moveCooldown = 0;

        int newX = this.x + deltaX;
        int newY = this.y + deltaY;

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
    public int getDeltaX()   { return deltaX; }
    public int getDeltaY()   { return deltaY; }
}