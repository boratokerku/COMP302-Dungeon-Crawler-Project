package domain.models.item.wearables;

import domain.logic.TakeAction;
import domain.logic.EquipAction;
import domain.logic.UnequipAction;
import domain.logic.DiscardAction;
import domain.models.item.MapItem;

public class AxeItem extends MapItem {
    public AxeItem(int x, int y) {
        super("Battle Axe", x, y, "images/weapons/axe.png");
        this.addAction(new TakeAction());
        this.addAction(new EquipAction(8)); // High physical attack bonus
        this.addAction(new UnequipAction());
        this.addAction(new DiscardAction());
    }

    @Override
    public double getWeaponPivotX() {
        return 0.5;
    }

    @Override
    public double getWeaponPivotY() {
        return 0.85; // Grips from the bottom of the axe shaft
    }

    @Override
    public double getWeaponAngleOffset() {
        return 0.0; // Naturally vertical
    }

    @Override
    public int getHandOffsetX() {
        return 0;
    }

    @Override
    public int getHandOffsetY() {
        return 0;
    }
}
