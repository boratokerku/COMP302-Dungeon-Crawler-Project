package domain.logic;

import domain.models.entity.Hero;
import domain.models.entity.GameObject;

public class TakeAction implements Action {

    @Override
    public String getName() {
        return "Take";
    }

    @Override
    public boolean isAvailable(Hero hero, GameObject target) {
        return !hero.getInventory().isFull() && !hero.getInventory().getItems().contains(target);
    }

    @Override
    public void execute(Hero hero, GameObject target) {
        if (target != null && target.getMap() != null) {
            hero.getInventory().addItem(target);
            int tx = target.getX();
            int ty = target.getY();
            domain.models.map.GameMap map = target.getMap();
            map.removeObject(target);
            // Fallback: If it's still on the map (e.g. coordinates didn't match),
            // forcefully place a FloorTile
            if (map.getObjectAt(tx, ty) == target) {
                map.placeObject(new domain.models.tile.FloorTile(), tx, ty);
            }
            // Check adjacent cells just in case it was somehow displaced
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int nx = tx + dx;
                    int ny = ty + dy;
                    if (nx >= 0 && nx < map.getWidth() && ny >= 0 && ny < map.getHeight()) {
                        if (map.getObjectAt(nx, ny) == target) {
                            map.placeObject(new domain.models.tile.FloorTile(), nx, ny);
                        }
                    }
                }
            }
        }
    }
}
