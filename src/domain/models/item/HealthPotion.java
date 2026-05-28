package domain.models.item;

import domain.models.entity.Hero;

public class HealthPotion extends Item {
    private int healAmount;

    public HealthPotion(String name, int healAmount) {
        super(name, 0.5); // İksirler hafiftir
        this.healAmount = healAmount;
    }

    @Override
    public void use(Hero hero) {
        hero.heal(healAmount);
        System.out.println(name + " consumed. Healed: " + healAmount);
    }
}
