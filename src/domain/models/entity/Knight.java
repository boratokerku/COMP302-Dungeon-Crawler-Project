package domain.models.entity;

import java.awt.Point;
import java.util.Random;

public class Knight extends Entity implements Renderable {

    @Override
    public String getSpriteKey() { return "knight"; }

    private final Random random = new Random();
    private int moveCooldown = 0;

    public Knight(int x, int y) {
        super(x, y, 20); // Design doc §2.5.1: "Knights start with 20HP"
    }

    // Knight AI ana metodu — en yakın hedefi (Hero veya ShadowClone) takip eder
    public void followHero(Hero hero, domain.models.map.GameMap map, java.util.List<Entity> entities) {
        if (!this.isAlive()) return;

        moveCooldown++;
        if (moveCooldown < 4) { // Her 4 tick'te bir hareket
            return;
        }
        moveCooldown = 0;

        // En yakın hedefi bul (Hero veya ShadowClone)
        Entity target = findNearestTarget(hero, entities);

        int distance = (int) Math.ceil(distanceTo(target));

        if (distance <= 1) {
            // ATTACK: Hedefe bitişik — saldır (design doc §2.5.1)
            String label = (target instanceof Hero) ? "Hero" : "Shadow Clone";
            System.out.println("Knight: Attacking " + label);
            attackTarget(target);
        } else if (distance <= 5) {
            // CHASING: Hedef algılama mesafesinde → hedefe doğru hareket et
            String label = (target instanceof Hero) ? "Hero" : "Shadow Clone";
            System.out.println("Knight: Chasing " + label);
            chaseTarget(target, map, entities);
        } else {
            // ROAMING: Hedef çok uzakta → rastgele yürür
            System.out.println("Knight: Roaming");
            roam(map, entities);
        }
    }

    // Hero veya hayatta olan ShadowClone'dan hangisi daha yakınsa onu döndür
    private Entity findNearestTarget(Hero hero, java.util.List<Entity> entities) {
        Entity nearest = hero;
        double minDist = distanceTo(hero);

        for (Entity e : entities) {
            if (e instanceof ShadowClone && e.isAlive()) {
                double d = distanceTo(e);
                if (d < minDist) {
                    minDist = d;
                    nearest = e;
                }
            }
        }
        return nearest;
    }

    private double distanceTo(Entity target) {
        double dx = this.x - target.getX();
        double dy = this.y - target.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    // Design doc §2.5.1: "4HP damage, some absorbed by DEF stat of hero"
    private void attackTarget(Entity target) {
        int baseDamage = 4;
        int def = (target instanceof Hero) ? ((Hero) target).getDef() : 0;
        int damage = Math.max(1, baseDamage - def); // Minimum 1 damage to prevent complete invincibility
        target.takeDamage(damage);
        view.GameView.addFloatingText(target.getX(), target.getY(), "-" + damage + " HP", new java.awt.Color(255, 200, 50));
        System.out.println("Knight dealt " + damage + " dmg | Target HP: " + target.getHp());
    }

    // Herhangi bir Entity hedefine doğru bir adım atar (Hero veya ShadowClone)
    private void chaseTarget(Entity target, domain.models.map.GameMap map, java.util.List<Entity> entities) {
        int nextX = this.x;
        int nextY = this.y;

        if (this.x < target.getX()) {
            nextX++;
        } else if (this.x > target.getX()) {
            nextX--;
        } else if (this.y < target.getY()) {
            nextY++;
        } else if (this.y > target.getY()) {
            nextY--;
        }

        if (canMoveTo(nextX, nextY, map, entities)) {
            this.x = nextX;
            this.y = nextY;
        }
    }

    private void roam(domain.models.map.GameMap map, java.util.List<Entity> entities) {
        int[][] directions = { {0, -1}, {0, 1}, {-1, 0}, {1, 0} };
        int[] dir = directions[random.nextInt(directions.length)];

        int nextX = this.x + dir[0];
        int nextY = this.y + dir[1];

        if (canMoveTo(nextX, nextY, map, entities)) {
            this.x = nextX;
            this.y = nextY;
        }
    }

    private boolean canMoveTo(int nextX, int nextY, domain.models.map.GameMap map, java.util.List<Entity> entities) {
        if (map == null || !map.isWalkable(nextX, nextY)) return false;
        if (entities != null) {
            for (Entity e : entities) {
                if (e != this && e.isAlive() && e.getX() == nextX && e.getY() == nextY) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void update() {
        // Takip mantığı followHero(hero, map, entities) ile tetiklenir.
    }

    @Override
    public Point getPosition() {
        return new Point(this.x, this.y);
    }
}