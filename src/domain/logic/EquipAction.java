package domain.logic;

import domain.models.entity.Hero;
import domain.models.entity.GameObject;

public class EquipAction implements Action {

    @Override
    public String getName() {
        return "Equip";
    }

    @Override
    public boolean isAvailable(Hero hero, GameObject target) {
        return hero.getInventory().getItems().contains(target);
    }

    @Override
    public void execute(Hero hero, GameObject target) {
        if (target instanceof domain.models.item.SwordItem) {
            hero.equipWeapon((domain.models.item.SwordItem) target);
            System.out.println("Equipped Sword: " + target.getName() + " (Item remains in inventory)");
        }
    }
}
