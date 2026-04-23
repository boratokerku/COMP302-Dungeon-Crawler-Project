package domain.models.staticObjects;

import domain.logic.TakeAction;
import domain.logic.UseAction;
import domain.logic.DiscardAction;
import domain.models.item.MapItem;

public class KeyItem extends MapItem {
    private boolean isSingleUse = true;

    public KeyItem(int x, int y) {
        super("Golden Key", x, y, "images/items/key/golden_key_1.png");
        this.addAction(new TakeAction());
        this.addAction(new UseAction());
        this.addAction(new DiscardAction());
    }

    public boolean isSingleUse() {
        return isSingleUse;
    }

    public void setSingleUse(boolean singleUse) {
        this.isSingleUse = singleUse;
    }
}
