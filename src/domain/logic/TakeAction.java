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
        return !hero.getInventory().isFull();
    }

    @Override
    public void execute(Hero hero, GameObject target) {
        if (target != null && target.getMap() != null) {
            hero.getInventory().addItem(target);
            target.getMap().removeObject(target);
        }
    }
}
