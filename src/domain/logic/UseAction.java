package domain.logic;

import domain.models.entity.Hero;
import domain.models.entity.GameObject;

public class UseAction implements Action {

    @Override
    public String getName() {
        return "Use";
    }

    @Override
    public boolean isAvailable(Hero hero, GameObject target) {
        return hero.getInventory().getItems().contains(target);
    }

    @Override
    public void execute(Hero hero, GameObject target) {
        if (target instanceof domain.models.item.PotionItem) {
            hero.heal(5); // Heal 5 HP
            System.out.println("Used Potion. Hero healed for 5 HP. Current HP: " + hero.getHp());
            hero.getInventory().removeItem(target);
        } else if (target instanceof domain.models.staticObjects.KeyItem) {
            domain.models.staticObjects.KeyItem key = (domain.models.staticObjects.KeyItem) target;
            System.out.println("Used Key. (Simulated door opening)");
            if (key.isSingleUse()) {
                hero.getInventory().removeItem(target);
                System.out.println("Key was single use and has been removed from inventory.");
            } else {
                System.out.println("Key is multi-use and remains in inventory.");
            }
        }
    }
}
