package domain.models.item;

import domain.models.entity.Hero;
import domain.models.action.Action;
import domain.models.action.effects.TakeEffect;
import domain.models.action.effects.UseItemEffect;

public class Potion extends Item {
    private int healAmount;

    public Potion(int x, int y, String imageName, String name, int healAmount) {
        super(x, y, imageName, name, 0.5); // İksirler hafiftir
        this.healAmount = healAmount;
        this.addAction(new Action("Take", new TakeEffect()));
        this.addAction(new Action("Eat", new UseItemEffect()));
    }

    @Override
    public void use(Hero hero) {
        hero.heal(healAmount);
        System.out.println(name + " consumed. Healed: " + healAmount);
    }
}