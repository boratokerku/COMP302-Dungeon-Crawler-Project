package domain.logic;

import domain.logic.event.GameEventBus;
import domain.logic.event.SoundEvent;
import domain.models.entity.Hero;
import domain.models.entity.GameObject;

public class BreakAction implements Action {
    @Override
    public String getName() {
        return "Break";
    }

    @Override
    public boolean isAvailable(Hero hero, GameObject target) {
        if (target instanceof domain.models.entity.Chest) {
            return false;
        }
        return true;
    }

    @Override
    public void execute(Hero hero, GameObject target) {
        if (target instanceof domain.models.entity.Chest) {
            return;
        }
        hero.setEnergy(Math.max(0, hero.getEnergy() - 10));
        
        double successChance = hero.getStr() / 20.0;
        double roll = Math.random();
        
        if (roll <= successChance) {
            if (target != null && target.getMap() != null) {
                int tx = target.getX();
                int ty = target.getY();
                domain.models.map.GameMap map = target.getMap();
                
                String containerType = "Box/Crate";
                if (target instanceof domain.models.entity.Chest) {
                    containerType = "Locked Chest";
                }
                
                // Remove the container first
                map.removeObject(target);
                
                GameObject loot = null;
                if (target instanceof domain.models.entity.Crate) {
                    loot = ((domain.models.entity.Crate) target).getHiddenItem();
                }
                
                boolean isLevelKey = (loot instanceof domain.models.staticObjects.LevelKey);
                
                if (loot == null) {
                    loot = domain.models.item.MapItem.createRandomItem(tx, ty);
                }
                
                if (loot != null) {
                    loot.setPosition(tx, ty);
                    map.placeObject(loot, tx, ty);
                    System.out.println("Broke " + containerType + " open! Dropped " + loot.getName() + " at (" + tx + ", " + ty + ")");
                }
                
                GameEventBus.fireSound(SoundEvent.SoundType.ENEMY_HIT);
                // Show floating text feedback!
                if (isLevelKey) {
                    GameEventBus.fireFloatingText(tx, ty, "Level key found!", new java.awt.Color(255, 215, 0));
                    domain.logic.LevelManager.clearAllOtherSkullKeys(map, tx, ty);
                } else {
                    GameEventBus.fireFloatingText(tx, ty, "BROKEN!", java.awt.Color.GREEN);
                }
            }
        } else {
            if (target != null) {
                GameEventBus.fireSound(SoundEvent.SoundType.WALK);
                GameEventBus.fireFloatingText(target.getX(), target.getY(), "FAILED!", java.awt.Color.RED);
            }
            String containerType = (target instanceof domain.models.entity.Chest) ? "Locked Chest" : "Box/Crate";
            System.out.println("Failed to break " + containerType + "! (Need more STR)");
        }
    }
}
