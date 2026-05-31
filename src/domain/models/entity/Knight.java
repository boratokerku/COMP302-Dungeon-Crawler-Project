package domain.models.entity;

import java.awt.Point;
import java.util.Random;
import domain.models.item.MapItem;

public class Knight extends Entity implements Renderable {

    @Override
    public String getSpriteKey() { return "knight"; }

    private final Random random = new Random();
    private int moveCooldown = 0;
    
    private MapItem equippedWeapon = null;
    private int weaponAtk = 0;

    public Knight(int x, int y) {
        super(x, y, 20); // Design doc §2.5.1: "Knights start with 20HP"
    }

    public boolean hasWeapon() {
        if (this.getTeam() == domain.models.Team.NONE || this.getTeam() == null) {
            return true;
        }
        return equippedWeapon != null;
    }

    public MapItem getEquippedWeapon() {
        return equippedWeapon;
    }

    // Knight AI ana metodu — en yakın hedefi (Hero veya ShadowClone) takip eder
    public void followHero(Hero hero, domain.models.map.GameMap map, java.util.List<Entity> entities) {
        if (!this.isAlive()) return;

        moveCooldown++;
        if (moveCooldown < 4) { // Her 4 tick'te bir hareket
            return;
        }
        moveCooldown = 0;

        // In Team Match, pick up weapon if standing on one
        if (this.getTeam() != domain.models.Team.NONE && this.getTeam() != null) {
            domain.models.entity.GameObject obj = map.getObjectAt(this.x, this.y);
            if (obj instanceof MapItem && ((MapItem) obj).isWeapon()) {
                pickUpWeapon((MapItem) obj, map);
            }
        }

        if (!hasWeapon()) {
            MapItem weapon = findNearestWeaponOnMap(map);
            if (weapon != null) {
                chaseWeapon(weapon, map, entities);
                return;
            }
        }

        Entity target;
        if (this.getTeam() != domain.models.Team.NONE && this.getTeam() != null) {
            target = findNearestEnemyTeamMatch(entities);
        } else {
            target = findNearestTarget(hero, entities);
        }
        
        if (target == null) return;

        int distance = (int) Math.ceil(distanceTo(target));

        if (distance <= 1) {
            // ATTACK: Hedefe bitişik — saldır
            attackTarget(target);
        } else if (this.getTeam() != domain.models.Team.NONE || distance <= 5) {
            // CHASING: Hedef algılama mesafesinde veya Team Match modunda (sınırsız görüş) → hedefe doğru hareket et
            chaseTarget(target, map, entities);
        } else {
            // ROAMING: Hedef çok uzakta → rastgele yürür
            roam(map, entities);
        }
    }

    private Entity findNearestEnemyTeamMatch(java.util.List<Entity> entities) {
        Entity nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Entity e : entities) {
            if (e != this && e.isAlive() && e.getTeam() != domain.models.Team.NONE && e.getTeam() != this.getTeam()) {
                double d = distanceTo(e);
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
        int baseDamage = (this.getTeam() != domain.models.Team.NONE && this.getTeam() != null) ? this.weaponAtk : 4;
        int def = 0;
        if (target instanceof Hero) {
            def = ((Hero) target).getDef();
        } else if (target instanceof Knight) {
            def = 1;
        }
        int damage = Math.max(1, baseDamage - def); // Minimum 1 damage to prevent complete invincibility
        target.takeDamage(damage);
        view.GameView.addFloatingText(target.getX(), target.getY(), "-" + damage + " HP", new java.awt.Color(255, 200, 50));
        System.out.println("Knight dealt " + damage + " dmg | Target HP: " + target.getHp());
    }

    private void pickUpWeapon(MapItem weapon, domain.models.map.GameMap map) {
        this.equippedWeapon = weapon;
        int atk = 5; // default fallback
        for (domain.logic.Action action : weapon.getActions()) {
            if (action instanceof domain.logic.EquipAction) {
                atk = ((domain.logic.EquipAction) action).getAtkBonus();
                break;
            }
        }
        this.weaponAtk = atk;
        map.removeObject(weapon);
        System.out.println("Knight (" + this.getTeam() + ") picked up weapon " + weapon.getName() + " with ATK: " + atk);
    }

    private MapItem findNearestWeaponOnMap(domain.models.map.GameMap map) {
        if (map == null) return null;
        MapItem nearest = null;
        double minDist = Double.MAX_VALUE;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                domain.models.entity.GameObject obj = map.getObjectAt(x, y);
                if (obj instanceof MapItem) {
                    MapItem item = (MapItem) obj;
                    if (item.isWeapon()) {
                        double dx = this.x - x;
                        double dy = this.y - y;
                        double d = Math.sqrt(dx * dx + dy * dy);
                        if (d < minDist) {
                            minDist = d;
                            nearest = item;
                        }
                    }
                }
            }
        }
        return nearest;
    }

    private void chaseWeapon(MapItem weapon, domain.models.map.GameMap map, java.util.List<Entity> entities) {
        int nextX = this.x;
        int nextY = this.y;

        if (this.x < weapon.getX()) {
            nextX++;
        } else if (this.x > weapon.getX()) {
            nextX--;
        } else if (this.y < weapon.getY()) {
            nextY++;
        } else if (this.y > weapon.getY()) {
            nextY--;
        }

        if (canMoveTo(nextX, nextY, map, entities)) {
            this.x = nextX;
            this.y = nextY;
        } else {
            // Try alternate axis if blocked
            nextX = this.x;
            nextY = this.y;
            if (this.y < weapon.getY()) {
                nextY++;
            } else if (this.y > weapon.getY()) {
                nextY--;
            } else if (this.x < weapon.getX()) {
                nextX++;
            } else if (this.x > weapon.getX()) {
                nextX--;
            }
            if (canMoveTo(nextX, nextY, map, entities)) {
                this.x = nextX;
                this.y = nextY;
            }
        }
        
        // Pick it up immediately if we stepped on it
        if (this.x == weapon.getX() && this.y == weapon.getY()) {
            pickUpWeapon(weapon, map);
        }
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
                if (e != this && e.isAlive()) {
                    if (e.occupiesTile(nextX, nextY)) return false;
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