package domain.logic;

import domain.models.entity.Hero;
import domain.models.GameObject;
import domain.models.item.MapItem;

public class TakeOffAction implements Action {

    @Override
    public String getName() {
        return "Take Off";
    }

    @Override
    public boolean isAvailable(Hero hero, GameObject target) {
        return hero.getEquippedArmor() == target || hero.getEquippedRing() == target;
    }

    @Override
    public void execute(Hero hero, GameObject target) {
        if (target instanceof MapItem) {
            String name = target.getClass().getSimpleName();
            if (name.contains("Armor")) {
                hero.unequipArmor();
                System.out.println("Took off Armor: " + target.getName() + " | New DEF: " + hero.getDef());
            } else if (name.contains("Ring")) {
                hero.unequipRing();
                System.out.println("Took off Ring: " + target.getName() + " | New STR: " + hero.getStr());
            }
        }
    }
}
