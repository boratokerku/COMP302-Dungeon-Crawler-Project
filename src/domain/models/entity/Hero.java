package domain.models.entity;

import java.util.Random;

public class Hero extends Entity {
    private int hp = 17;
    private int mana = 80;
    private int def = 2;
    private int str;
    private int energy = 100;

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
}