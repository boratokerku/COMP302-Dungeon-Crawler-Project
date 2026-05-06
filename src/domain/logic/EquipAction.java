package domain.logic;

import domain.models.entity.Hero;
import domain.models.entity.GameObject;

public class EquipAction implements Action {
    private int atkBonus;

    public EquipAction(int atkBonus) {
        this.atkBonus = atkBonus;
    }

    @Override
    public String getName() {
        return "Equip";
    }

    @Override
    public boolean isAvailable(Hero hero, GameObject target) {
        return hero.getInventory().getItems().contains(target) && hero.getEquippedWeapon() != target;
    }

    @Override
    public void execute(Hero hero, GameObject target) {
        hero.setEquippedWeapon(target);
        hero.setStr(hero.getStr() + atkBonus);
        System.out.println("Equipped weapon! Strength increased by " + atkBonus + ".");
    }
}
