package domain.models.item;

import domain.models.entity.Hero;

public class EnergyPotion extends Item {
    private int energyAmount;

    public EnergyPotion(String name, int energyAmount) {
        super(name, 0.5); // İksirler hafiftir
        this.energyAmount = energyAmount;
    }

    @Override
    public void use(Hero hero) {
        hero.setEnergy(Math.min(100, hero.getEnergy() + energyAmount));
        System.out.println(name + " consumed. Restored Energy: " + energyAmount);
    }
}
