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
        if (target instanceof domain.models.entity.SearchableObject) {
            domain.models.entity.SearchableObject so = (domain.models.entity.SearchableObject) target;
            if (!so.isSearched()) {
                so.search();
                System.out.println("You searched " + so.getName() + ". Nothing found.");
                view.GameView.addFloatingText(so.getX(), so.getY(), "Searched!", java.awt.Color.YELLOW);
            } else {
                System.out.println("Already searched.");
                view.GameView.addFloatingText(so.getX(), so.getY(), "Already searched", java.awt.Color.LIGHT_GRAY);
            }
            return;
        }

        if (hiddenItem != null && hero.getInventory() != null && !hero.getInventory().isFull()) {
            hero.getInventory().addItem(hiddenItem);
            System.out.println("You found a " + hiddenItem.getName() + "!");
            hiddenItem = null;
        } else {
            System.out.println("Nothing found.");
        }
    }
}
