package domain.models.item;

import domain.logic.TakeAction;
import domain.logic.WearAction;
import domain.logic.TakeOffAction;
import domain.logic.DiscardAction;

public class RingItem extends MapItem {

    private final int strBonus;

    public RingItem(int x, int y) {
        super("Ring of Might", x, y, "images/items/ring/red_ring.png");
        this.strBonus = 5; // Increases STR stat by 5!
        this.addAction(new TakeAction());
        this.addAction(new WearAction());
        this.addAction(new TakeOffAction());
        this.addAction(new DiscardAction());
    }

    @Override
    public int getStrBonus() {
        return this.strBonus;
    }
}
