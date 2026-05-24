package domain.models.item;

import domain.logic.TakeAction;
import domain.logic.ReadAction;
import domain.models.entity.Entity;
import domain.models.map.GameMap;

import java.util.List;

public class ShadowCloneScroll extends MapItem {

    public ShadowCloneScroll(int x, int y, List<Entity> entities, GameMap map,
                             controller.InputHandler inputHandler) {
        super("Shadow Clone Scroll", x, y, "images/items/readings/book.png");
        this.addAction(new TakeAction());
        this.addAction(new ReadAction(entities, map, inputHandler));
    }
}
