package domain.logic;

import domain.models.entity.Hero;
import domain.models.entity.GameObject;

public class BreakAction implements Action {
    @Override
    public String getName() {
        return "Break";
    }

    @Override
    public boolean isAvailable(Hero hero, GameObject target) {
        return true;
    }

    @Override
    public void execute(Hero hero, GameObject target) {
        hero.setEnergy(Math.max(0, hero.getEnergy() - 10));
        
        double successChance = hero.getStr() / 20.0;
        double roll = Math.random();
        
        if (roll <= successChance) {
            if (target != null && target.getMap() != null) {
                int tx = target.getX();
                int ty = target.getY();
                domain.models.map.GameMap map = target.getMap();
                
                // Remove the container first
                map.removeObject(target);
                
                // Drop a high-quality random loot on the floor!
                domain.models.item.MapItem loot = domain.models.item.MapItem.createRandomItem(tx, ty);
                if (loot != null) {
                    map.placeObject(loot, tx, ty);
                    System.out.println("Broke container open! Dropped " + loot.getName() + " at (" + tx + ", " + ty + ")");
                }
                
                // Show floating text feedback!
                view.GameView.addFloatingText(tx, ty, "BROKEN!", java.awt.Color.GREEN);
            }
        } else {
            if (target != null) {
                view.GameView.addFloatingText(target.getX(), target.getY(), "FAILED!", java.awt.Color.RED);
            }
            System.out.println("Failed to break container! (Need more STR)");
        }
    }
}
