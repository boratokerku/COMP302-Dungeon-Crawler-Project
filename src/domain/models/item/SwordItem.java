package domain.models.item;

import domain.logic.TakeAction;
import domain.logic.EquipAction;
import domain.logic.DiscardAction;

public class SwordItem extends MapItem {
    public SwordItem(int x, int y) {
        super("Knight Sword", x, y, "images/weapons/knight_sword.png");
        this.addAction(new TakeAction());
        this.addAction(new EquipAction(5));
        this.addAction(new DiscardAction());
    }
}
