package domain.logic;

import domain.models.entity.Hero;
import domain.models.entity.GameObject;

public class WearAction implements Action {
    private int defBonus;

    public WearAction(int defBonus) {
        this.defBonus = defBonus;
    }

    @Override
    public String getName() {
        return "Wear";
    }

    @Override
    public boolean isAvailable(Hero hero, GameObject target) {
        return true;
    }

    @Override
    public void execute(Hero hero, GameObject target) {
        hero.setDef(hero.getDef() + defBonus);
        System.out.println("Wore it! Defense increased by " + defBonus + ".");
    }
}
