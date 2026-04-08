package domain.models.entity;

import java.util.Random;
import domain.models.Direction;
import domain.models.AnimationState;

public class Hero extends Entity {
    private int hp = 17;
    private int mana = 80;
    private int def = 2;
    private int str;
    private int energy = 100;
    private Direction currentDirection = Direction.RIGHT;
    private AnimationState currentAnimationState = AnimationState.IDLE;

    public Hero(int x, int y) {
        super(x, y, 17);
        this.str = new Random().nextInt(8) + 8;
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
        this.energy -= 1;
    }

    public void heal(int amount) {
        this.hp += amount;
    }

    public int getHp() {
        return hp;
    }

    public int getStr() {
        return str;
    }

    public void move(Direction dir) {
        this.currentDirection = dir;
        // Note: actual x/y update will be done after MovementHandler validation
        System.out.println("Hero moving " + dir);
    }

    /**
     * Hero attacks the entity in the current facing direction.
     */
    public void attack(Entity target) {
        if (target != null && target.isAlive()) {
            int damage = 5; // Will scale with Weapon later
            target.takeDamage(damage);
            System.out.println("Hero attacked target! Damage: " + damage);
        }
    }

    // Getters
    public Direction getDirection() {
        return currentDirection;
    }

    public int[] getPosition() {
        return new int[]{ this.x, this.y };
    }

    public void setAnimationState(AnimationState state) {
        this.currentAnimationState = state;
    }

    public AnimationState getAnimationState() {
        return currentAnimationState;
    }

    @Override
    public void update() {
        // Enerji yenilenmesi veya pasif etkiler buraya
    }
}