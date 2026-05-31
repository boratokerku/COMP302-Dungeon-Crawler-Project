package domain.models.entity;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Sorcerer extends Entity implements Renderable {

    @Override
    public String getSpriteKey() { return "sorcerer"; }

    private long lastTeleportTime;
    private long lastProjectileTime; // Döküman §2.5.2: Her 5 sn'de bir mermi
    private Projectile pendingProjectile = null; // ConcurrentModification önlemek için
    private final Random random = new Random();

    public Sorcerer(int x, int y) {
        super(x, y, 10);
        long now = System.currentTimeMillis();
        this.lastTeleportTime = now;
        this.lastProjectileTime = now;
    }

    public long getTimeLeft() {
        return Math.max(0, 7000 - (System.currentTimeMillis() - lastTeleportTime));
    }

    public void setTimeLeft(long timeLeft) {
        this.lastTeleportTime = System.currentTimeMillis() - (7000 - timeLeft);
    }

    public long getProjectileTimeLeft() {
        return Math.max(0, 5000 - (System.currentTimeMillis() - lastProjectileTime));
    }

    public void setProjectileTimeLeft(long timeLeft) {
        this.lastProjectileTime = System.currentTimeMillis() - (5000 - timeLeft);
    }

    /**
     * Main AI method for the Sorcerer.
     *
     * Design doc §2.5.2:
     * "Sorcerer does not walk and moves only by teleportation."
     * "Every 7 seconds, with 50% probability he teleports himself
     *  to a random empty spot on the map."
     *
     * REQUIRES:
     *   - hero != null (the hero reference must not be null)
     *   - map != null  (the map reference must not be null; used to find empty tiles for teleportation)
     *   - This Sorcerer instance must have a valid (x, y) position within the map boundaries
     *
     * MODIFIES:
     *   - this.lastTeleportTime   (updated when the 7-second teleport cooldown expires)
     *   - this.lastProjectileTime (updated when the 5-second projectile cooldown expires)
     *   - this.pendingProjectile  (set to a new Projectile when the 5-second cooldown expires)
     *   - this.x, this.y          (changed if a teleport occurs: Sorcerer moves to a random
     *                               walkable, unoccupied tile on the map)
     *
     * EFFECTS:
     *   - If this.isAlive() == false, the method returns immediately with no side effects.
     *   - In Team Match mode (this.getTeam() != NONE), the target is the nearest living
     *     entity from the opposing team; in normal mode, the target is whichever of the
     *     hero or any living ShadowClone is closest to the Sorcerer.
     *   - If elapsed time since last projectile >= 5000 ms: pendingProjectile is assigned
     *     a new Projectile directed toward the current target (to be retrieved by
     *     DemoRunner via pollPendingProjectile()).
     *   - If elapsed time since last teleport >= 7000 ms: with 50% probability
     *     (random.nextBoolean()), this.x and this.y are updated to a random walkable
     *     and unoccupied tile; with 50% probability nothing happens but the timer resets.
     *
     * @param hero     the player character (must not be null)
     * @param map      the game map (must not be null; used to search for empty tiles)
     * @param entities the list of all entities (used for occupancy and team checks)
     */
    public void followHero(Hero hero, domain.models.map.GameMap map, java.util.List<Entity> entities) {
        if (!this.isAlive()) return;

        long currentTime = System.currentTimeMillis();

        Entity target;
        if (this.getTeam() != domain.models.Team.NONE && this.getTeam() != null) {
            target = findNearestEnemyTeamMatch(entities);
        } else {
            target = findNearestTarget(hero, entities);
        }
        
        if (target == null) return;

        // Her 7 sn'de teleport (design doc §2.5.2)
        if (currentTime - lastTeleportTime >= 7000) {
            lastTeleportTime = currentTime;
            attemptTeleport(map, entities);
        }

        // Her 5 sn'de mermi fırlat (design doc §2.5.2)
        if (currentTime - lastProjectileTime >= 5000) {
            lastProjectileTime = currentTime;
            pendingProjectile = createProjectile(target);
        }
    }

    private Entity findNearestEnemyTeamMatch(java.util.List<Entity> entities) {
        Entity nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Entity e : entities) {
            if (e != this && e.isAlive() && e.getTeam() != domain.models.Team.NONE && e.getTeam() != this.getTeam()) {
                double d = Math.sqrt(Math.pow(this.x - e.getX(), 2) + Math.pow(this.y - e.getY(), 2));
                if (d < minDist) {
                    minDist = d;
                    nearest = e;
                }
            }
        }
        return nearest;
    }

    // Hero veya hayatta olan ShadowClone'dan hangisi daha yakınsa onu döndür
    private Entity findNearestTarget(Hero hero, java.util.List<Entity> entities) {
        Entity nearest = hero;
        double minDist = Math.sqrt(Math.pow(this.x - hero.getX(), 2) + Math.pow(this.y - hero.getY(), 2));

        if (entities != null) {
            for (Entity e : entities) {
                if (e instanceof ShadowClone && e.isAlive()) {
                    double d = Math.sqrt(Math.pow(this.x - e.getX(), 2) + Math.pow(this.y - e.getY(), 2));
                    if (d < minDist) {
                        minDist = d;
                        nearest = e;
                    }
                }
            }
        }
        return nearest;
    }

    /** DemoRunner tarafından her tick sonunda çağrılır, mermiyi alır ve sıfırlar */
    public Projectile pollPendingProjectile() {
        Projectile p = pendingProjectile;
        pendingProjectile = null;
        return p;
    }

    /** Hero'ya doğru yönelmiş bir Projectile oluşturur */
    private Projectile createProjectile(Entity hero) {
        double diffX = hero.getX() - this.x;
        double diffY = hero.getY() - this.y;
        double dist = Math.sqrt(diffX*diffX + diffY*diffY);

        if (dist == 0) return null; // Aynı konumda ise ates etme

        // Pürüzsüz vektörel yönelim (Tam isabet)
        double speed = 0.5;
        double dx = (diffX / dist) * speed;
        double dy = (diffY / dist) * speed;

        util.helpers.SoundManager.playShoot();
        return new Projectile(this.x, this.y, this.x, this.y, dx, dy, 8, this, "SORCERER_FIREBALL");
    }

    /**
     * %50 ihtimalle haritadaki rastgele boş bir tile'a ışınlanır.
     *
     * Design doc §2.5.2:
     * "with 50% probability he teleports himself to a random empty spot on the map"
     */
    private void attemptTeleport(domain.models.map.GameMap map, java.util.List<Entity> entities) {
        if (!random.nextBoolean()) {
            // %50 ihtimalle ışınlanma gerçekleşmez
            System.out.println("Sorcerer güç topluyor (ışınlanma bu sefer gerçekleşmedi).");
            return;
        }

        // Haritadaki tüm yürünebilir ve boş hücreleri topla
        List<int[]> emptyTiles = new ArrayList<>();
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                if (map.isWalkable(x, y) && !isOccupied(x, y, entities)) {
                    emptyTiles.add(new int[]{x, y});
                }
            }
        }

        if (emptyTiles.isEmpty()) {
            System.out.println("Sorcerer ışınlanmak istedi ama boş yer bulunamadı.");
            return;
        }

        // Boş tile'lardan rastgele birini seç
        int[] target = emptyTiles.get(random.nextInt(emptyTiles.size()));
        this.x = target[0];
        this.y = target[1];
        util.helpers.SoundManager.playTeleport();
        System.out.println("Sorcerer ışınlandı! Yeni konum: (" + this.x + ", " + this.y + ")");
    }

    /**
     * Verilen koordinat, başka bir canlı varlık tarafından işgal edilmiş mi?
     */
    private boolean isOccupied(int x, int y, java.util.List<Entity> entities) {
        if (entities == null) return false;
        for (Entity e : entities) {
            if (e != this && e.isAlive()) {
                if (e.occupiesTile(x, y)) return true;
            }
        }
        return false;
    }

    @Override
    public void update() {
        // Hareket mantığı followHero(hero, map, entities) ile tetiklenir.
    }

    @Override
    public Point getPosition() {
        return new Point(this.x, this.y);
    }
}