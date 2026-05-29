package domain.models.staticObjects;

import domain.logic.TakeAction;
import domain.logic.DiscardAction;
import domain.models.item.MapItem;

/**
 * A special key that unlocks LevelDoors to transition between dungeon levels.
 * Distinct from the regular KeyItem used for chests.
 */
public class LevelKey extends MapItem {

    public LevelKey(int x, int y) {
        this("Skull Key", x, y, "images/items/key/skull_key.png");
    }

    public LevelKey(String name, int x, int y, String imagePath) {
        super(name, x, y, imagePath);
        this.addAction(new TakeAction());
        this.addAction(new DiscardAction());
    }
}
