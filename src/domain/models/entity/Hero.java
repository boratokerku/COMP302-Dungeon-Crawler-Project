package domain.models.entity;

import java.util.Random;

import domain.models.AnimationState;
import domain.models.Direction;

public class Hero extends Entity {
    private int mana = 80;
    private int def = 2;
    private int str;
    private int energy = 100;
    private Direction currentDirection = Direction.RIGHT;
    private AnimationState currentAnimationState = AnimationState.IDLE;
    private domain.models.inventory.Inventory inventory;
    private int weaponAtk = 0;
    private domain.models.item.MapItem equippedWeapon;
    private domain.models.item.MapItem equippedArmor = null;
    private domain.models.item.MapItem equippedRing = null;
    private domain.models.map.GameMap currentMap = null;

    public void setCurrentMap(domain.models.map.GameMap map) {
        this.currentMap = map;
    }

    public domain.models.map.GameMap getCurrentMap() {
        return this.currentMap;
    }

    public Hero(int x, int y) {
        super(x, y, 17); // Max HP = 17
        this.str = new Random().nextInt(8) + 8;
        this.inventory = new domain.models.inventory.Inventory(8); // 2x4 layout
    }

    public domain.models.inventory.Inventory getInventory() {
        return inventory;
    }

    public AnimationState getAnimationState() {
        return currentAnimationState;
    }

    public void setAnimationState(AnimationState animationState) {
        this.currentAnimationState = animationState;
    }

    public boolean attemptBreak() {
        int energyCost = 10;
        if (this.energy >= energyCost) {
            this.energy -= energyCost;
            return new Random().nextInt(20) < getStr();
        }
        return false;
    }

    public int calculateDamage(int weaponAtk) {
        // Döküman §3: Hasar = f(STR, ATK) — sadece sabit değil, istatistiğe dayalı
        return (getStr() / 2) + weaponAtk;
    }

    public void consumeEnergyForMove() {
        this.energy = Math.max(0, this.energy - 3);
    }

    public void heal(int amount) {
        this.hp += amount;
        if (this.hp > 17)
            this.hp = 17;
    }

    public void equipWeapon(domain.models.item.MapItem weapon) {
        this.equippedWeapon = weapon;
        this.weaponAtk = 5; // Default ATK
    }

    public void equipWeapon(domain.models.item.MapItem weapon, int atk) {
        this.equippedWeapon = weapon;
        this.weaponAtk = atk; // Real weapon ATK
    }

    public void equipWeapon(domain.models.item.SwordItem sword) {
        equipWeapon((domain.models.item.MapItem) sword);
    }

    public void equipWeapon(domain.models.item.SwordItem sword, int atk) {
        equipWeapon((domain.models.item.MapItem) sword, atk);
    }

    public void unequipWeapon() {
        this.equippedWeapon = null;
        this.weaponAtk = 0;
    }

    // We put this in the Hero class using Information Expert
    // because the Hero knows its own coordinates best.
    public boolean isAdjacentTo(int targetX, int targetY) {
        return Math.abs(this.x - targetX) <= 1 && Math.abs(this.y - targetY) <= 1;
    }

    public int getHp() {
        return this.hp;
    }

    public int getMana() {
        return this.mana;
    }

    public void setMana(int mana) {
        this.mana = mana;
    }

    public int getEnergy() {
        return this.energy;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    public int getStr() {
        int bonus = (equippedRing != null) ? equippedRing.getStrBonus() : 0;
        return this.str + bonus;
    }

    public void setStr(int str) {
        this.str = str;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getDef() {
        int bonus = (equippedArmor != null) ? equippedArmor.getDefBonus() : 0;
        return this.def + bonus;
    }

    public void setDef(int def) {
        this.def = def;
    }

    public domain.models.item.MapItem getEquippedArmor() {
        return this.equippedArmor;
    }

    public void equipArmor(domain.models.item.MapItem armor) {
        this.equippedArmor = armor;
    }

    public void unequipArmor() {
        this.equippedArmor = null;
    }

    public domain.models.item.MapItem getEquippedRing() {
        return this.equippedRing;
    }

    public void equipRing(domain.models.item.MapItem ring) {
        this.equippedRing = ring;
    }

    public void unequipRing() {
        this.equippedRing = null;
    }

    public GameObject getEquippedWeapon() {
        return this.equippedWeapon;
    }

    /**
     * Moves the hero on the grid map based on directional offsets.
     *
     * @requires (dx >= -1 && dx <= 1) && (dy >= -1 && dy <= 1)
     * @modifies this.x, this.y
     * @effects If the target position (this.x + dx, this.y + dy) is within the map
     *          boundaries
     *          and there is no collision with a Static Object (Wall), updates the
     *          hero's coordinates
     *          to the target position. If a collision occurs or the target is out
     *          of bounds,
     *          the hero's position remains unchanged.
     */
    public boolean move(int dx, int dy) {
        if (this.currentMap == null)
            return false;
        int nextX = this.x + dx;
        int nextY = this.y + dy;
        if (this.currentMap.isWalkable(nextX, nextY)) {
            this.x = nextX;
            this.y = nextY;
            return true;
        }
        return false;
    }

    public boolean move(Direction dir, domain.models.map.GameMap map, java.util.List<Entity> entities) {
        if (this.energy < 3) {
            System.out.println("No energy to move!");
            return false;
        }
        this.currentDirection = dir;

        int nextX = this.x;
        int nextY = this.y;

        switch (dir) {
            case UP:
                nextY -= 1;
                break;
            case DOWN:
                nextY += 1;
                break;
            case LEFT:
                nextX -= 1;
                break;
            case RIGHT:
                nextX += 1;
                break;
        }

        boolean occupied = false;
        if (entities != null) {
            for (Entity e : entities) {
                if (e != this && e.isAlive() && e.getX() == nextX && e.getY() == nextY) {
                    occupied = true;
                    this.attack(e, map); // Çarptığı düşmana saldır
                    break;
                }
            }
        }

        if (map != null && map.isWalkable(nextX, nextY) && !occupied) {
            this.x = nextX;
            this.y = nextY;
            consumeEnergyForMove();
            System.out.println("Hero moving " + dir + " to (" + this.x + ", " + this.y + ") Energy: " + this.energy);
            return true;
        } else if (!occupied) {
            System.out.println("Hero blocked at (" + nextX + ", " + nextY + ")");
            return false;
        }
        return true; // Occupied covers attack, which is an action
    }

    /**
     * Hero attacks the entity in the current facing direction.
     */
    public void attack(Entity target, domain.models.map.GameMap map) {
        int attackCost = 10;
        if (target != null && target.isAlive()) {
            if (this.energy >= attackCost) {
                int damage = calculateDamage(this.weaponAtk);
                target.takeDamage(damage);
                view.GameView.addFloatingText(target.getX(), target.getY(), "-" + damage + " HP",
                        new java.awt.Color(255, 60, 60));
                this.energy -= attackCost; // Saldırı maliyeti
                System.out.println("Hero attacked target! Damage: " + damage + " Energy: " + this.energy);

                if (!target.isAlive() && map != null) {
                    System.out.println("Enemy defeated!");
                    domain.models.entity.GameObject loot = null;
                    if (target instanceof domain.models.entity.FinalBoss) {
                        loot = new domain.models.item.VictoryCoin(target.getX(), target.getY());
                    } else {
                        java.util.Random rand = new java.util.Random();
                        int dropType = rand.nextInt(3);
                        if (dropType == 0) {
                            loot = domain.models.item.MapItem.createRandomItem(target.getX(), target.getY());
                        } else if (dropType == 1) {
                            loot = new domain.models.item.PotionItem(target.getX(), target.getY());
                        } else {
                            int locked = countLockedChests(map);
                            int keys = countKeys(map, this);
                            if (keys < locked) {
                                loot = new domain.models.staticObjects.KeyItem(target.getX(), target.getY());
                            } else {
                                loot = rand.nextBoolean()
                                    ? domain.models.item.MapItem.createRandomItem(target.getX(), target.getY())
                                    : new domain.models.item.PotionItem(target.getX(), target.getY());
                            }
                        }
                    }
                    map.placeObject(loot, target.getX(), target.getY());
                    System.out.println("Loot dropped: " + loot.getName());
                }
            } else {
                System.out.println("Saldırı için yeterli enerji yok!");
            }
        }
    }

    public int getWeaponAtk() {
        return this.weaponAtk;
    }

    // Getters
    public Direction getDirection() {
        return currentDirection;
    }

    private int countLockedChests(domain.models.map.GameMap map) {
        int count = 0;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                domain.models.entity.GameObject obj = map.getObjectAt(x, y);
                if (obj instanceof domain.models.entity.Chest && ((domain.models.entity.Chest) obj).isLocked()) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countKeys(domain.models.map.GameMap map, Hero hero) {
        int count = 0;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                domain.models.entity.GameObject obj = map.getObjectAt(x, y);
                if (obj instanceof domain.models.staticObjects.KeyItem) {
                    count++;
                }
            }
        }
        if (hero != null && hero.getInventory() != null) {
            for (domain.models.entity.GameObject item : hero.getInventory().getItems()) {
                if (item instanceof domain.models.staticObjects.KeyItem) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public void update() {
        // Enerji yenilenmesi (Logic Loop her 120ms'de bir çağırdığında azar azar dolar)
        if (this.energy < 100) {
            this.energy += 1;
        }
    }

}