package domain.models.item;

import domain.models.entity.Hero;

public class ManaPotion extends Item {
    private int manaAmount;

    public ManaPotion(String name, int manaAmount) {
        super(name, 0.5); // İksirler hafiftir
        this.manaAmount = manaAmount;
    }

    @Override
    public void use(Hero hero) {
        hero.setMana(Math.min(80, hero.getMana() + manaAmount));
        System.out.println(name + " consumed. Restored Mana: " + manaAmount);
    }
}
