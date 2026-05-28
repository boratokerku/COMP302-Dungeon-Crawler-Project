package domain.models.item.wearables;

import domain.logic.TakeAction;
import domain.logic.EquipAction;
import domain.logic.UnequipAction;
import domain.models.item.MapItem;
import domain.logic.DiscardAction;

public class FireWandItem extends MapItem {
    public FireWandItem(int x, int y) {
        super("Fire Wand", x, y, "images/weapons/fire_wand.png");
        this.addAction(new TakeAction());
        this.addAction(new EquipAction(9)); // Epic magical ranged weapon
        this.addAction(new UnequipAction());
        this.addAction(new DiscardAction());
    }

    @Override
    public double getWeaponPivotX() {
        return 0.5; // Held in the middle
    }

    @Override
    public double getWeaponPivotY() {
        return 0.5;
    }

    @Override
    public boolean isRanged() {
        return true;
    }

    @Override
    public int getManaCost() {
        return 5; // Consumes 5 mana per shot
    }

    @Override
    public String getProjectileType() {
        return "FIREBALL";
    }

    @Override
    public double getBaseRotationAngle() {
        return 0.0; // Magic wand is held straight (0 degrees)
    }
}
