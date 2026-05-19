package domain.logic;

import domain.models.entity.Hero;
import domain.models.entity.GameObject;

public class EquipAction implements Action {

    private final int atkBonus;

    public EquipAction() {
        this.atkBonus = 10;
    }

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
        if (target instanceof domain.models.item.SwordItem) {
            hero.equipWeapon((domain.models.item.SwordItem) target, atkBonus);
            System.out.println("Equipped: " + target.getName() + " | ATK: " + atkBonus + " | STR: " + hero.getStr());
        }
    }
}
