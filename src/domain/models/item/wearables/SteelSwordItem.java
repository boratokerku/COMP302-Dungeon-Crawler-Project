package domain.models.item.wearables;

import domain.logic.TakeAction;
import domain.logic.EquipAction;
import domain.logic.UnequipAction;
import domain.logic.DiscardAction;
import domain.models.item.MapItem;

public class SteelSwordItem extends MapItem {
    public SteelSwordItem(int x, int y) {
        super("Steel Sword", x, y, "images/weapons/steel_sword_1.png");
        this.addAction(new TakeAction());
        this.addAction(new EquipAction(6, 600)); // 6 atk, 0.6s delay
        this.addAction(new UnequipAction());
        this.addAction(new DiscardAction());
    }

    @Override
    public double getWeaponPivotX() {
        return 0.5;
    }

    @Override
    public double getWeaponPivotY() {
        return 0.85;
    }
}
