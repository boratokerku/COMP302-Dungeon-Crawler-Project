package domain.models.item;

import domain.models.entity.Hero;

public class Weapon extends Item {
    private int attackPower;

    public Weapon(String name, int attackPower) {
        super(name, 2.0); // Silahlar daha ağırdır
        this.attackPower = attackPower;
    }

    @Override
    public void use(Hero hero) {
        // Silahı kuşanma (Equip) mantığı buraya gelecek
        System.out.println(name + " equipped. ATK: " + attackPower);
    }

    public int getAttackPower() {
        return attackPower;
    }
}