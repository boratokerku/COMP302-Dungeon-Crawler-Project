package domain.models.entity;

import java.util.Random;

import domain.models.AnimationState;
import domain.models.Direction;
import java.awt.Point;

public class Hero extends Entity {
    private int mana = 80;
    private int def = 2;
    private int str;
    private int energy = 100;
    private Direction currentDirection = Direction.RIGHT;
    private AnimationState currentAnimationState = AnimationState.IDLE;
    private domain.models.inventory.Inventory inventory;
    private int weaponAtk = 0;

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
            return new Random().nextInt(20) < this.str;
        }
        return false;
    }

    public int calculateDamage(int weaponAtk) {
        return (this.str * 2) + (weaponAtk * 3);
    }

    public void consumeEnergyForMove() {
        this.energy = Math.max(0, this.energy - 3);
    }

    public void heal(int amount) {
        this.hp += amount;
        if (this.hp > 17)
            this.hp = 17;
    }

    public void equipWeapon(domain.models.item.SwordItem sword) {
        // Basic sword buff
        this.weaponAtk = 5;
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

    public int getEnergy() {
        return this.energy;
    }

    public int getStr() {
        return str;
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
                this.energy -= attackCost; // Saldırı maliyeti
                System.out.println("Hero attacked target! Damage: " + damage + " Energy: " + this.energy);

                if (!target.isAlive() && map != null) {
                    System.out.println("Enemy defeated!");
                    java.util.Random rand = new java.util.Random();
                    int dropType = rand.nextInt(3);
                    domain.models.entity.GameObject loot = null;
                    if (dropType == 0) {
                        loot = new domain.models.item.SwordItem(target.getX(), target.getY());
                    } else if (dropType == 1) {
                        loot = new domain.models.item.PotionItem(target.getX(), target.getY());
                    } else {
                        loot = new domain.models.staticObjects.KeyItem(target.getX(), target.getY());
                    }
                    map.placeObject(loot, target.getX(), target.getY());
                    System.out.println("Loot dropped: " + loot.getName());
                }
            } else {
                System.out.println("Saldırı için yeterli enerji yok!");
            }
        }
    }

    // Getters
    public Direction getDirection() {
        return currentDirection;
    }

    @Override
    public void update() {
        // Enerji yenilenmesi (Logic Loop her 120ms'de bir çağırdığında azar azar dolar)
        if (this.energy < 100) {
            this.energy += 1;
        }
    }

}