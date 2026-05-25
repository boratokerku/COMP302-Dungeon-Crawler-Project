package domain.models.entity;

import domain.models.map.GameMap;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Final Boss entity — a 2x2 footprint hybrid enemy with 100 HP.
 *
 * AI Behavior:
 * - Walks toward the hero like a Knight (slow: every 8 ticks).
 * - Fires AoE projectiles toward the hero every 6 seconds (like Sorcerer).
 * - Teleports to a random empty 2x2 area every 10 seconds with 50% probability.
 * - Phase Mechanics: spawns minion Knights at HP thresholds (80/60/40/20).
 *
 * The boss occupies 4 tiles: (x,y), (x+1,y), (x,y+1), (x+1,y+1).
 */
public class FinalBoss extends Entity implements Renderable {

    @Override
    public String getSpriteKey() { return "finalboss"; }

    private final Random random = new Random();
    private int moveCooldown = 0;
    private long lastProjectileTime;
    private long lastTeleportTime;
    private Projectile pendingProjectile = null;

    // Phase spawn tracking — each threshold fires once
    private boolean phase80Triggered = false;
    private boolean phase60Triggered = false;
    private boolean phase40Triggered = false;
    private boolean phase20Triggered = false;

    // Pending minions to spawn (collected by DemoRunner each tick)
    private final List<Knight> pendingMinions = new ArrayList<>();
    private final List<Sorcerer> pendingSorcerers = new ArrayList<>();

    public FinalBoss(int x, int y) {
        super(x, y, 100); // 100 HP
        long now = System.currentTimeMillis();
        this.lastProjectileTime = now;
        this.lastTeleportTime = now;
    }

    // ── Phase flags for save/load ────────────────────────────────────────────

    public boolean isPhase80Triggered() { return phase80Triggered; }
    public boolean isPhase60Triggered() { return phase60Triggered; }
    public boolean isPhase40Triggered() { return phase40Triggered; }
    public boolean isPhase20Triggered() { return phase20Triggered; }

    public void setPhase80Triggered(boolean v) { phase80Triggered = v; }
    public void setPhase60Triggered(boolean v) { phase60Triggered = v; }
    public void setPhase40Triggered(boolean v) { phase40Triggered = v; }
    public void setPhase20Triggered(boolean v) { phase20Triggered = v; }

    // ── Projectile polling (same pattern as Sorcerer) ────────────────────────

    public Projectile pollPendingProjectile() {
        Projectile p = pendingProjectile;
        pendingProjectile = null;
        return p;
    }

    // ── Minion polling ───────────────────────────────────────────────────────

    public List<Knight> pollPendingMinions() {
        List<Knight> minions = new ArrayList<>(pendingMinions);
        pendingMinions.clear();
        return minions;
    }

    public List<Sorcerer> pollPendingSorcerers() {
        List<Sorcerer> sorcerers = new ArrayList<>(pendingSorcerers);
        pendingSorcerers.clear();
        return sorcerers;
    }

    // ── 2x2 Footprint helpers ────────────────────────────────────────────────

    /**
     * Returns all 4 tile positions occupied by the boss.
     */
    public List<Point> getOccupiedTiles() {
        List<Point> tiles = new ArrayList<>();
        tiles.add(new Point(x, y));
        tiles.add(new Point(x + 1, y));
        tiles.add(new Point(x, y + 1));
        tiles.add(new Point(x + 1, y + 1));
        return tiles;
    }

    /**
     * Returns true if the given position is occupied by this boss's 2x2 footprint.
     */
    public boolean occupiesTile(int tx, int ty) {
        return (tx == x || tx == x + 1) && (ty == y || ty == y + 1);
    }

    /**
     * Check if the boss can move to a new top-left corner position.
     * All 4 destination tiles must be valid, walkable, and unoccupied by other entities.
     */
    private boolean canMoveTo(int newX, int newY, GameMap map, List<Entity> entities) {
        // Check all 4 tiles of the 2x2 footprint
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                int checkX = newX + dx;
                int checkY = newY + dy;

                if (map == null || !map.isWalkable(checkX, checkY)) return false;

                // Check entity collision (ignore self)
                if (entities != null) {
                    for (Entity e : entities) {
                        if (e != this && e.isAlive() && e.getX() == checkX && e.getY() == checkY) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    // ── Main AI method ───────────────────────────────────────────────────────

    public void followHero(Hero hero, GameMap map, List<Entity> entities) {
        if (!this.isAlive()) return;

        long currentTime = System.currentTimeMillis();

        // --- Phase Mechanics: spawn minions at HP thresholds ---
        checkPhaseSpawns(map, entities);

        // --- Teleportation: every 10 seconds, 50% chance ---
        if (currentTime - lastTeleportTime >= 10000) {
            lastTeleportTime = currentTime;
            attemptTeleport(map, entities);
        }

        // --- AoE Projectile: every 6 seconds ---
        if (currentTime - lastProjectileTime >= 6000) {
            lastProjectileTime = currentTime;
            pendingProjectile = createProjectile(hero);
        }

        // --- Movement: chase hero (slow, every 8 ticks) ---
        moveCooldown++;
        if (moveCooldown >= 8) {
            moveCooldown = 0;

            // Check adjacency — any of the 4 boss tiles within 1 tile of hero
            boolean adjacent = false;
            for (Point p : getOccupiedTiles()) {
                if (Math.abs(p.x - hero.getX()) <= 1 && Math.abs(p.y - hero.getY()) <= 1) {
                    adjacent = true;
                    break;
                }
            }

            if (adjacent) {
                attackHero(hero);
            } else {
                chaseHero(hero, map, entities);
            }
        }
    }

    // ── Attack ───────────────────────────────────────────────────────────────

    private void attackHero(Hero hero) {
        int baseDamage = 8;
        int def = hero.getDef();
        int damage = Math.max(1, baseDamage - def);
        hero.takeDamage(damage);
        view.GameView.addFloatingText(hero.getX(), hero.getY(), "-" + damage + " HP",
                new java.awt.Color(255, 50, 50));
        System.out.println("FinalBoss dealt " + damage + " dmg | Hero HP: " + hero.getHp());
    }

    // ── Movement ─────────────────────────────────────────────────────────────

    private void chaseHero(Hero hero, GameMap map, List<Entity> entities) {
        // Calculate center of boss for direction
        double bossX = this.x + 0.5;
        double bossY = this.y + 0.5;

        int nextX = this.x;
        int nextY = this.y;

        if (bossX < hero.getX()) {
            nextX++;
        } else if (bossX > hero.getX()) {
            nextX--;
        } else if (bossY < hero.getY()) {
            nextY++;
        } else if (bossY > hero.getY()) {
            nextY--;
        }

        if (canMoveTo(nextX, nextY, map, entities)) {
            this.x = nextX;
            this.y = nextY;
        }
    }

    // ── Teleportation ────────────────────────────────────────────────────────

    private void attemptTeleport(GameMap map, List<Entity> entities) {
        if (!random.nextBoolean()) {
            System.out.println("FinalBoss teleport skipped (50% chance).");
            return;
        }

        // Find all valid 2x2 areas on the map
        List<int[]> candidates = new ArrayList<>();
        for (int tx = 1; tx < map.getWidth() - 2; tx++) {
            for (int ty = 1; ty < map.getHeight() - 2; ty++) {
                if (canMoveTo(tx, ty, map, entities)) {
                    candidates.add(new int[]{tx, ty});
                }
            }
        }

        if (candidates.isEmpty()) {
            System.out.println("FinalBoss: No valid 2x2 teleport destination found.");
            return;
        }

        int[] dest = candidates.get(random.nextInt(candidates.size()));
        this.x = dest[0];
        this.y = dest[1];
        System.out.println("FinalBoss teleported to (" + x + ", " + y + ")");
    }

    // ── Projectile ───────────────────────────────────────────────────────────

    private Projectile createProjectile(Entity target) {
        // Fire from the center of the 2x2 boss
        double centerX = this.x + 0.5;
        double centerY = this.y + 0.5;

        double diffX = target.getX() - centerX;
        double diffY = target.getY() - centerY;
        double dist = Math.sqrt(diffX * diffX + diffY * diffY);

        if (dist == 0) return null;

        double speed = 0.4; // Slightly slower than Sorcerer
        double dx = (diffX / dist) * speed;
        double dy = (diffY / dist) * speed;

        return new Projectile(this.x, this.y, centerX, centerY, dx, dy, 12, this, "BOSS_FIREBALL");
    }

    // ── Phase Mechanics ──────────────────────────────────────────────────────

    private void checkPhaseSpawns(GameMap map, List<Entity> entities) {
        if (!phase80Triggered && hp < 80) {
            phase80Triggered = true;
            spawnMinions(1, 0, map, entities); // 1 Knight
            view.GameView.addFloatingText(x, y, "PHASE 2!", new java.awt.Color(255, 100, 100));
        }
        if (!phase60Triggered && hp < 60) {
            phase60Triggered = true;
            spawnMinions(1, 1, map, entities); // 1 Knight + 1 Sorcerer
            view.GameView.addFloatingText(x, y, "PHASE 3!", new java.awt.Color(255, 80, 80));
        }
        if (!phase40Triggered && hp < 40) {
            phase40Triggered = true;
            spawnMinions(1, 2, map, entities); // 1 Knight + 2 Sorcerers
            view.GameView.addFloatingText(x, y, "PHASE 4!", new java.awt.Color(255, 50, 50));
        }
        if (!phase20Triggered && hp < 20) {
            phase20Triggered = true;
            spawnMinions(2, 2, map, entities); // 2 Knights + 2 Sorcerers
            view.GameView.addFloatingText(x, y, "FINAL PHASE!", new java.awt.Color(255, 0, 0));
        }
    }

    /**
     * Spawns Knights and Sorcerers near the boss.
     * They are stored in pendingMinions for DemoRunner to collect.
     */
    private void spawnMinions(int knightCount, int sorcererCount, GameMap map, List<Entity> entities) {
        List<int[]> candidates = new ArrayList<>();
        // Look for walkable tiles near the boss (within 3 tile radius)
        for (int dx = -3; dx <= 4; dx++) {
            for (int dy = -3; dy <= 4; dy++) {
                int tx = this.x + dx;
                int ty = this.y + dy;
                if (occupiesTile(tx, ty)) continue; // Skip boss tiles
                if (map.isWalkable(tx, ty) && !isOccupied(tx, ty, entities)) {
                    candidates.add(new int[]{tx, ty});
                }
            }
        }

        java.util.Collections.shuffle(candidates);

        int spawned = 0;
        int totalNeeded = knightCount + sorcererCount;
        for (int[] pos : candidates) {
            if (spawned >= totalNeeded) break;
            if (spawned < knightCount) {
                Knight minion = new Knight(pos[0], pos[1]);
                pendingMinions.add(minion);
                System.out.println("FinalBoss spawned minion Knight at (" + pos[0] + ", " + pos[1] + ")");
            } else {
                Sorcerer minion = new Sorcerer(pos[0], pos[1]);
                pendingSorcerers.add(minion);
                System.out.println("FinalBoss spawned minion Sorcerer at (" + pos[0] + ", " + pos[1] + ")");
            }
            spawned++;
        }
    }

    private boolean isOccupied(int tx, int ty, List<Entity> entities) {
        if (entities == null) return false;
        for (Entity e : entities) {
            if (e != this && e.isAlive() && e.getX() == tx && e.getY() == ty) {
                return true;
            }
        }
        return false;
    }

    // ── Timer accessors for save/load ────────────────────────────────────────

    public long getProjectileTimeLeft() {
        return Math.max(0, 6000 - (System.currentTimeMillis() - lastProjectileTime));
    }

    public void setProjectileTimeLeft(long timeLeft) {
        this.lastProjectileTime = System.currentTimeMillis() - (6000 - timeLeft);
    }

    public long getTeleportTimeLeft() {
        return Math.max(0, 10000 - (System.currentTimeMillis() - lastTeleportTime));
    }

    public void setTeleportTimeLeft(long timeLeft) {
        this.lastTeleportTime = System.currentTimeMillis() - (10000 - timeLeft);
    }

    @Override
    public void update() {
        // Movement logic is triggered via followHero()
    }

    @Override
    public Point getPosition() {
        return new Point(this.x, this.y);
    }
}
