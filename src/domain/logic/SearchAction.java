package domain.logic;

import domain.models.entity.Hero;
import domain.models.entity.GameObject;

public class SearchAction implements Action {
    private GameObject hiddenItem;

    public SearchAction(GameObject hiddenItem) {
        this.hiddenItem = hiddenItem;
    }

    @Override
    public String getName() {
        return "Search";
    }

    @Override
    public boolean isAvailable(Hero hero, GameObject target) {
        return true;
    }

    @Override
    public void execute(Hero hero, GameObject target) {
        if (hiddenItem != null && hero.getInventory() != null && !hero.getInventory().isFull()) {
            hero.getInventory().addItem(hiddenItem);
            System.out.println("You found a " + hiddenItem.getName() + "!");
            hiddenItem = null;
        } else {
            System.out.println("Nothing found.");
        }
    }
}
