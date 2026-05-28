package domain.models.item.wearables;

import domain.logic.TakeAction;
import domain.logic.EquipAction;
import domain.logic.UnequipAction;
import domain.models.item.MapItem;
import domain.logic.DiscardAction;

public class WoodenSwordItem extends MapItem {
    public WoodenSwordItem(int x, int y) {
        super("Wooden Sword", x, y, "images/weapons/wooden_sword.png");
        this.addAction(new TakeAction());
        this.addAction(new EquipAction(2)); // Base tier melee weapon
        this.addAction(new UnequipAction());
        this.addAction(new DiscardAction());
    }

    @Override
    public double getWeaponPivotX() {
        return 0.5;
    }

    @Override
    public double getWeaponPivotY() {
        return 0.85; // Hand grips near the bottom hilt
    }
}
