package domain.logic;

import domain.models.entity.Hero;
import domain.models.GameObject;

public class DiscardAction implements Action {

    @Override
    public String getName() {
        return "Discard";
    }

    @Override
    public boolean isAvailable(Hero hero, GameObject target) {
        return hero.getInventory().getItems().contains(target);
    }

    @Override
    public void execute(Hero hero, GameObject target) {
        hero.getInventory().removeItem(target);
        System.out.println("Item discarded: " + target.getName());
    }
}
