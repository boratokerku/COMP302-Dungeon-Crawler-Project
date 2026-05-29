package domain.models.item.wearables;

import domain.logic.TakeAction;
import domain.logic.EquipAction;
import domain.logic.UnequipAction;
import domain.logic.DiscardAction;
import domain.models.item.MapItem;

public class DiamondSwordItem extends MapItem {
    public DiamondSwordItem(int x, int y) {
        super("Diamond Sword", x, y, "images/weapons/diamond_sword_1.png");
        this.addAction(new TakeAction());
        this.addAction(new EquipAction(16)); // Legendary tier melee weapon
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
