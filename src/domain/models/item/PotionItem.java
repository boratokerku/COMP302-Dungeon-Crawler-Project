package domain.models.item;

import domain.logic.TakeAction;
import domain.logic.UseAction;
import domain.logic.DiscardAction;

public class PotionItem extends MapItem {
    public PotionItem(int x, int y) {
        super("Red Potion", x, y, "images/items/potion/red_potion.png");
        this.addAction(new TakeAction());
        this.addAction(new UseAction());
        this.addAction(new DiscardAction());
    }
}
