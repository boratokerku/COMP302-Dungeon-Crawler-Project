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
                target.getMap().removeObject(target);
            }
            System.out.println("Broke it open!");
        } else {
            System.out.println("Failed to break. (Need more STR)");
        }
    }
}
