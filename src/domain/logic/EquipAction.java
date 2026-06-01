package domain.logic;

import domain.models.entity.Hero;
import domain.models.GameObject;

public class EquipAction implements Action {

    private final int atkBonus;
    private final int delayMs;

    public EquipAction() {
        this.atkBonus = 10;
        this.delayMs = 800; // default delay
    }

    public EquipAction(int atkBonus) {
        this.atkBonus = atkBonus;
        this.delayMs = 800; // default delay
    }

    public EquipAction(int atkBonus, int delayMs) {
        this.atkBonus = atkBonus;
        this.delayMs = delayMs;
    }

    public int getAtkBonus() {
        return atkBonus;
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
        if (target instanceof domain.models.item.MapItem) {
            hero.equipWeapon((domain.models.item.MapItem) target, atkBonus, delayMs);
            System.out.println("Equipped: " + target.getName() + " | ATK: " + atkBonus + " | Delay: " + delayMs + "ms | STR: " + hero.getStr());
        }
    }
}
