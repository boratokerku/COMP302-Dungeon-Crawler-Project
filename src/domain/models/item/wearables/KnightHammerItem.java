package domain.models.item.wearables;

import domain.logic.TakeAction;
import domain.logic.EquipAction;
import domain.logic.UnequipAction;
import domain.logic.DiscardAction;
import domain.models.item.MapItem;

public class KnightHammerItem extends MapItem {
    public KnightHammerItem(int x, int y) {
        super("Knight Hammer", x, y, "images/weapons/knight_hammer.png");
        this.addAction(new TakeAction());
        this.addAction(new EquipAction(20, 1200)); // 20 atk, 1.2s delay
        this.addAction(new UnequipAction());
        this.addAction(new DiscardAction());
    }

    @Override
    public double getWeaponPivotX() {
        return 0.5;
    }

    @Override
    public double getWeaponPivotY() {
        return 0.85; // Grip near bottom handle
    }
}
