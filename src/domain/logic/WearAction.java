package domain.logic;

import domain.models.entity.Hero;
import domain.models.entity.GameObject;
import domain.models.item.MapItem;

public class WearAction implements Action {

    @Override
    public String getName() {
        return "Wear";
    }

    @Override
    public boolean isAvailable(Hero hero, GameObject target) {
        if (!hero.getInventory().getItems().contains(target)) return false;
        String name = target.getClass().getSimpleName();
        if (name.contains("Armor") || name.contains("Ring")) {
            return hero.getEquippedArmor() != target && hero.getEquippedRing() != target;
        }
        return false;
    }

    @Override
    public void execute(Hero hero, GameObject target) {
        if (target instanceof MapItem) {
            MapItem item = (MapItem) target;
            String name = target.getClass().getSimpleName();
            if (name.contains("Armor")) {
                hero.equipArmor(item);
                System.out.println("Wore Armor: " + item.getName() + " | New DEF: " + hero.getDef());
            } else if (name.contains("Ring")) {
                hero.equipRing(item);
                System.out.println("Wore Ring: " + item.getName() + " | New STR: " + hero.getStr());
            }
        }
    }
}
