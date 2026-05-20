package domain.models.item;

import domain.logic.TakeAction;
import domain.logic.WearAction;
import domain.logic.TakeOffAction;
import domain.logic.DiscardAction;

public class ArmorItem extends MapItem {

    private final int defBonus;

    public ArmorItem(int x, int y) {
        super("Steel Armor", x, y, "images/items/steel_armor.png");
        this.defBonus = 4; // Increases DEF by 4!
        this.addAction(new TakeAction());
        this.addAction(new WearAction());
        this.addAction(new TakeOffAction());
        this.addAction(new DiscardAction());
    }

    @Override
    public int getDefBonus() {
        return this.defBonus;
    }
}
