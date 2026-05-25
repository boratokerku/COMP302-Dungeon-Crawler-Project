package domain.logic;

import domain.models.entity.Hero;
import domain.models.entity.GameObject;

public class EatAction implements Action {
    @Override
    public String getName() {
        return "Eat";
    }

    @Override
    public boolean isAvailable(Hero hero, GameObject target) {
        return true;
    }

    @Override
    public void execute(Hero hero, GameObject target) {
        int maxHp = 17;
        int maxEnergy = 100;
        hero.setHp(Math.min(maxHp, hero.getHp() + 5));
        hero.setEnergy(Math.min(maxEnergy, hero.getEnergy() + 15));
        util.helpers.SoundManager.playHeal();
        
        if (target != null) {
            if (target.getMap() != null) {
                target.getMap().removeObject(target);
            } else if (hero.getInventory() != null && hero.getInventory().getItems().contains(target)) {
                hero.getInventory().removeItem(target);
            }
        }
        System.out.println("Ate it! Replenished HP and Energy.");
    }
}
