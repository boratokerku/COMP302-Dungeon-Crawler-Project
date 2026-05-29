package domain.logic;

import domain.models.entity.Entity;
import domain.models.entity.Knight;
import domain.models.entity.Sorcerer;
import domain.models.map.GameMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Handles enemy spawning according to design doc §2.5:
 *
 * - Enemies appear every 9 seconds at a random empty edge tile (next to a wall).
 * - 60% of the time a Knight spawns, 30% of the time a Sorcerer, 10% nothing spawns.
 * - There can be at most 5 enemies on the floor at the same time.
 */
public class EnemySpawner {

    // Design doc §2.5 sabitleri — performans için magic number kullanmıyoruz
    private static final int SPAWN_INTERVAL_MS = 9000; // Her 9 saniyede bir spawn denemesi
    private static final int MAX_ENEMIES = 5;           // Aynı anda haritada en fazla 5 düşman olabilir
    private static final int KNIGHT_THRESHOLD = 6;      // Zar 0-5 → Knight (%60 ihtimal)
    private static final int SORCERER_THRESHOLD = 9;    // Zar 6-8 → Sorcerer (%30) | Zar 9 → spawn yok (%10)

    private long lastSpawnTime; // Son spawn denemesinin zamanı (milisaniye)
    private final Random random = new Random();
    private GameMap map;

    // Knight ve Sorcerer ayrı listelerde tutulur — DemoRunner instanceof kullanmadan followHero çağırabilsin
    private final List<Knight> spawnedKnights = new ArrayList<>();
    private final List<Sorcerer> spawnedSorcerers = new ArrayList<>();

    public EnemySpawner(GameMap map) {
        this.map = map;
        this.lastSpawnTime = System.currentTimeMillis();
    }

    public void setGameMap(GameMap map) {
        this.map = map;
    }

    public void clearSpawnedEnemies() {
        spawnedKnights.clear();
        spawnedSorcerers.clear();
    }

    public long getTimeLeft() {
        return Math.max(0, SPAWN_INTERVAL_MS - (System.currentTimeMillis() - lastSpawnTime));
    }

    public void setTimeLeft(long timeLeft) {
        this.lastSpawnTime = System.currentTimeMillis() - (SPAWN_INTERVAL_MS - timeLeft);
    }

    /**
     * Called every logic tick. Checks whether it is time to spawn a new enemy
     * and, if so, applies the 60/30/10 probability rule.
     *
     * @param allEntities The shared entity list (hero + all enemies). New enemies
     *                    are added here so collision detection stays up to date.
     */
    public void trySpawn(List<Entity> allEntities) {
        long now = System.currentTimeMillis();

        // Henüz 9 saniye dolmadıysa bekle, bir şey yapma
        if (now - lastSpawnTime < SPAWN_INTERVAL_MS) return;
        lastSpawnTime = now; // Zamanlayıcıyı sıfırla

        // Hâlâ hayatta olan düşman sayısını kontrol et (design doc: max 5)
        int aliveEnemies = countAliveEnemies();
        if (aliveEnemies >= MAX_ENEMIES) {
            System.out.println("EnemySpawner: Max düşman sayısına ulaşıldı ("
                    + MAX_ENEMIES + "), bu seferlik spawn yok.");
            return;
        }

        // 10 yüzlü zar at → 0-5: Knight, 6-8: Sorcerer, 9: hiç spawn yok
        int roll = random.nextInt(10);

        if (roll >= SORCERER_THRESHOLD) {
            // %10 ihtimalle bu turda hiçbir şey spawn etmiyoruz
            System.out.println("EnemySpawner: Spawn yok bu seferlik (%10 ihtimal).");
            return;
        }

        // Find a random empty edge tile for spawning
        int[] spawnPos = findEdgeTile(allEntities);
        if (spawnPos == null) {
            System.out.println("EnemySpawner: Boş kenar tile bulunamadı, spawn ertelendi.");
            return;
        }

        if (roll < KNIGHT_THRESHOLD) {
            // 60% → spawn Knight
            Knight k = new Knight(spawnPos[0], spawnPos[1]);
            spawnedKnights.add(k);
            allEntities.add(k);
            System.out.println("EnemySpawner: Yeni Knight spawn edildi! Konum: ("
                    + spawnPos[0] + ", " + spawnPos[1] + ") | Toplam düşman: " + countAliveEnemies());
        } else {
            // 30% → spawn Sorcerer
            Sorcerer s = new Sorcerer(spawnPos[0], spawnPos[1]);
            spawnedSorcerers.add(s);
            allEntities.add(s);
            System.out.println("EnemySpawner: Yeni Sorcerer spawn edildi! Konum: ("
                    + spawnPos[0] + ", " + spawnPos[1] + ") | Toplam düşman: " + countAliveEnemies());
        }
    }

    /**
     * Finds a random empty tile on the inner perimeter of the map
     * (the floor cells directly adjacent to the outer walls).
     *
     * Design doc §2.5: "random empty cell next to a wall"
     *
     * For a 13-wide × 10-tall map the inner perimeter is:
     *   Top edge   : y = 1,          x = 1 .. width-2
     *   Bottom edge: y = height-2,   x = 1 .. width-2
     *   Left edge  : x = 1,          y = 2 .. height-3  (corners counted above)
     *   Right edge : x = width-2,    y = 2 .. height-3
     *
     * @return {x, y} of the chosen tile, or null if no empty edge tile exists.
     */
    private int[] findEdgeTile(List<Entity> allEntities) {
        List<int[]> candidates = new ArrayList<>();
        int w = map.getWidth();
        int h = map.getHeight();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (isFreeFloor(x, y, allEntities)) {
                    // Check if adjacent to a wall
                    boolean nextToWall = false;
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            if (dx == 0 && dy == 0) continue;
                            int nx = x + dx;
                            int ny = y + dy;
                            if (map.isValidPosition(nx, ny)) {
                                domain.models.entity.GameObject neighbor = map.getObjectAt(nx, ny);
                                if (neighbor instanceof domain.models.tile.WallTile) {
                                    nextToWall = true;
                                    break;
                                }
                            }
                        }
                        if (nextToWall) break;
                    }
                    if (nextToWall) {
                        candidates.add(new int[]{x, y});
                    }
                }
            }
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(random.nextInt(candidates.size()));
    }

    /**
     * Returns true if the tile at (x, y) is a walkable floor tile AND
     * is not currently occupied by any alive entity.
     */
    private boolean isFreeFloor(int x, int y, List<Entity> allEntities) {
        if (!map.isWalkable(x, y)) return false;
        for (Entity e : allEntities) {
            if (e.isAlive()) {
                if (e.occupiesTile(x, y)) return false;
            }
        }
        return true;
    }

    /**
     * Counts how many spawned enemies are still alive.
     */
    private int countAliveEnemies() {
        int count = 0;
        // Ölmüş düşmanları saymıyoruz — sadece hâlâ ayakta olanlar 5 sınırına dahil
        for (Knight k : spawnedKnights) if (k.isAlive()) count++;
        for (Sorcerer s : spawnedSorcerers) if (s.isAlive()) count++;
        return count;
    }

    // ── DemoRunner'ın logic timer'ı tarafından kullanılan getter'lar ──────────

    /** Bu spawner tarafından üretilen tüm Knight'ları döndürür (ölmüş olabilir, isAlive() kontrol et). */
    public List<Knight> getSpawnedKnights() {
        return spawnedKnights;
    }

    /** Bu spawner tarafından üretilen tüm Sorcerer'ları döndürür (ölmüş olabilir, isAlive() kontrol et). */
    public List<Sorcerer> getSpawnedSorcerers() {
        return spawnedSorcerers;
    }
}
