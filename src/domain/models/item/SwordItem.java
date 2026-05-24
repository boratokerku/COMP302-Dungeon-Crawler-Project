package domain.models.item;

import domain.logic.TakeAction;
import domain.logic.EquipAction;
import domain.logic.UnequipAction;
import domain.logic.DiscardAction;

public class SwordItem extends MapItem {
    public SwordItem(int x, int y) {
        super("Knight Sword", x, y, "images/weapons/knight_sword.png");
        this.addAction(new TakeAction());
        this.addAction(new EquipAction(5));
        this.addAction(new UnequipAction());
        this.addAction(new DiscardAction());
    }

    @Override
    public double getWeaponPivotX() {
        return 0.5;
    }

    @Override
    public double getWeaponPivotY() {
        return 0.85; // Hand grips the hilt of the sword near the bottom
    }

    @Override
    public double getWeaponAngleOffset() {
        return 0.0; // Naturally vertical
    }

    @Override
    public int getHandOffsetX() {
        return 0; // standard horizontal grip
    }

    @Override
    public int getHandOffsetY() {
        return 0; // standard vertical grip
    }
}
