package domain.models.item.wearables;

import domain.logic.TakeAction;
import domain.logic.EquipAction;
import domain.logic.UnequipAction;
import domain.logic.DiscardAction;
import domain.models.item.MapItem;

public class BowItem extends MapItem {
    public BowItem(int x, int y) {
        super("Hunting Bow", x, y, "images/weapons/bow.png");
        this.addAction(new TakeAction());
        this.addAction(new EquipAction(5)); // Rare physical ranged weapon
        this.addAction(new UnequipAction());
        this.addAction(new DiscardAction());
    }

    @Override
    public double getWeaponPivotX() {
        return 0.15; // Gripped at the bow body center-left
    }

    @Override
    public double getWeaponPivotY() {
        return 0.5; // Centered vertically
    }

    @Override
    public boolean isRanged() {
        return true;
    }

    @Override
    public String getProjectileType() {
        return "ARROW";
    }

    @Override
    public double getBaseRotationAngle() {
        return 0.0; // Ranged physical bow is held straight (0 degrees)
    }
}
