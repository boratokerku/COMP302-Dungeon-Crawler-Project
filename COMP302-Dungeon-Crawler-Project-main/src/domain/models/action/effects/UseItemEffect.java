package domain.models.action.effects;

import domain.models.action.Effect;
import domain.models.entity.GameObject;
import domain.models.entity.Hero;
import domain.models.map.GameMap;

public class UseItemEffect implements Effect {

    public UseItemEffect() {
    }

    @Override
    public void apply(GameObject target, Hero hero, GameMap map) {
        if (target instanceof domain.models.item.Item) {
            // Leverage the specific item's predefined use() logic
            domain.models.item.Item item = (domain.models.item.Item) target;
            item.use(hero);

            // Remove it from the map if it was on the ground
            if (map != null && map.getObjectAt(target.getX(), target.getY()) == target) {
                map.placeObject(new domain.models.tile.FloorTile(), target.getX(), target.getY());
                System.out.println(item.getName() + " used from ground and removed.");
            } else {
                // If the item was used from the inventory, it would be removed from there
                hero.getInventory().getItems().remove(item);
                System.out.println(item.getName() + " used from inventory and consumed.");
            }
        }
    }
}
