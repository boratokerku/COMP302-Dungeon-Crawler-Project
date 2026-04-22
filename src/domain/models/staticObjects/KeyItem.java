package domain.models.staticObjects;

import domain.logic.TakeAction;
import domain.models.item.MapItem;

public class KeyItem extends MapItem {
    public KeyItem(int x, int y) {
        super("Golden Key", x, y, "images/items/key/golden_key_1.png");
        this.addAction(new TakeAction());
    }
}
