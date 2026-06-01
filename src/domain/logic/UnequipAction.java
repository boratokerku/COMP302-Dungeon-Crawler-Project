package domain.logic;

import domain.models.entity.Hero;
import domain.models.GameObject;

public class UnequipAction implements Action {

    @Override
    public String getName() {
        return "Unequip";
    }

    @Override
    public boolean isAvailable(Hero hero, GameObject target) {
        return hero.getEquippedWeapon() == target;
    }

    @Override
    public void execute(Hero hero, GameObject target) {
        if (target instanceof domain.models.item.MapItem) {
            hero.unequipWeapon();
            System.out.println("Unequipped Weapon: " + target.getName());
        }
    }
}
