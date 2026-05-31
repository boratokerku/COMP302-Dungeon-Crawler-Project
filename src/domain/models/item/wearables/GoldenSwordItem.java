package domain.models.item.wearables;

import domain.logic.TakeAction;
import domain.logic.EquipAction;
import domain.logic.UnequipAction;
import domain.logic.DiscardAction;
import domain.models.item.MapItem;

public class GoldenSwordItem extends MapItem {
    public GoldenSwordItem(int x, int y) {
        super("Golden Sword", x, y, "images/weapons/golden_sword_1.png");
        this.addAction(new TakeAction());
        this.addAction(new EquipAction(8, 500)); // 8 atk, 0.5s delay
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
