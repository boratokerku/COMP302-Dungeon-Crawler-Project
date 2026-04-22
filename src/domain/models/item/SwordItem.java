package domain.models.item;

import domain.logic.TakeAction;

public class SwordItem extends MapItem {
    public SwordItem(int x, int y) {
        super("Knight Sword", x, y, "images/weapons/knight_sword.png");
        this.addAction(new TakeAction());
    }
}
