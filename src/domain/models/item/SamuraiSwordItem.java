package domain.models.item;

import domain.logic.TakeAction;
import domain.logic.EquipAction;
import domain.logic.UnequipAction;
import domain.logic.DiscardAction;

public class SamuraiSwordItem extends MapItem {
    public SamuraiSwordItem(int x, int y) {
        super("Samurai Katana", x, y, "images/weapons/samurai_sword.png");
        this.addAction(new TakeAction());
        this.addAction(new EquipAction(12)); // Epic tier melee weapon
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
