package domain.models.action.effects;

import domain.models.action.Effect;
import domain.models.entity.GameObject;
import domain.models.entity.Hero;
import domain.models.map.GameMap;

public class TakeEffect implements Effect {

    public TakeEffect() {
    }

    @Override
    public void apply(GameObject target, Hero hero, GameMap map) {
        if (target instanceof domain.models.item.Item && hero.getInventory().isFull()) {
            System.out.println("Inventory is full! Cannot take " + target.getClass().getSimpleName());
            return;
        }

        System.out.println("Taking item: " + target.getClass().getSimpleName() + " at " + target.getX() + ", " + target.getY());
        
        if (map != null) {
            map.placeObject(new domain.models.tile.FloorTile(), target.getX(), target.getY());
            
            if (target instanceof domain.models.item.Item) {
                hero.getInventory().addItem((domain.models.item.Item) target);
                System.out.println("Item added to inventory! Total items: " + hero.getInventory().getItems().size());
            }
        }
    }
}
