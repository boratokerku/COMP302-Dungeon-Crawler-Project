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
     * Sorcerer AI ana metodu.
     *
     * Design doc §2.5.2:
     * "Sorcerer does not walk and moves only by teleportation."
     * "Every 7 seconds, with 50% probability he teleports himself
     *  to a random empty spot on the map."
     *
     * @param hero     Oyuncu karakteri
     * @param map      Harita (boş tile bulmak için)
     * @param entities Tüm varlıklar (çakışma kontrolü için)
     */
    public void followHero(Hero hero, domain.models.map.GameMap map, java.util.List<Entity> entities) {
        if (!this.isAlive()) return;

        long currentTime = System.currentTimeMillis();

        // Her 7 sn'de teleport (design doc §2.5.2)
        if (currentTime - lastTeleportTime >= 7000) {
            lastTeleportTime = currentTime;
            attemptTeleport(map, entities);
        }

        // Her 5 sn'de mermi fırlat (design doc §2.5.2)
        if (currentTime - lastProjectileTime >= 5000) {
            lastProjectileTime = currentTime;
            pendingProjectile = createProjectile(hero);
        }
    }

    /** DemoRunner tarafından her tick sonunda çağrılır, mermiyi alır ve sıfırlar */
    public Projectile pollPendingProjectile() {
        Projectile p = pendingProjectile;
        pendingProjectile = null;
        return p;
    }

    /** Hero'ya doğru yönelmiş bir Projectile oluşturur */
    private Projectile createProjectile(Hero hero) {
        int dx = Integer.compare(hero.getX(), this.x);
        int dy = Integer.compare(hero.getY(), this.y);

        // Diyagonal ise büyük eksen seçilir
        if (dx != 0 && dy != 0) {
            if (Math.abs(hero.getX() - this.x) >= Math.abs(hero.getY() - this.y)) dy = 0;
            else dx = 0;
        }

        if (dx == 0 && dy == 0) return null; // Aynı konumda ise ates etme

        System.out.println("Sorcerer fired projectile! Direction: (" + dx + ", " + dy + ")");
        // Design doc §2.5.2: 8 HP damage
        return new Projectile(this.x, this.y, dx, dy, 8, this);
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
        System.out.println("Sorcerer ışınlandı! Yeni konum: (" + this.x + ", " + this.y + ")");
    }

    /**
     * Verilen koordinat, başka bir canlı varlık tarafından işgal edilmiş mi?
     */
    private boolean isOccupied(int x, int y, java.util.List<Entity> entities) {
        if (entities == null) return false;
        for (Entity e : entities) {
            if (e != this && e.isAlive() && e.getX() == x && e.getY() == y) {
                return true;
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