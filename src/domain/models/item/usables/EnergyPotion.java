package domain.models.item.usables;

import domain.models.entity.Hero;
import domain.models.item.Item;

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
