package domain.models.item;

import domain.models.entity.Hero;

public class Weapon extends Item {
    private int attackPower;

    public Weapon(String name, int attackPower) {
        super(name, 2.0);
        this.attackPower = attackPower;
    }

    @Override
    public void use(Hero hero) {
        System.out.println(name + " equipped. ATK: " + attackPower);
    }

    public int getAttackPower() {
        return attackPower;
    }
}