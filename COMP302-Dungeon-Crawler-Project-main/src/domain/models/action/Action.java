package domain.models.action;

import domain.models.entity.GameObject;
import domain.models.entity.Hero;
import domain.models.map.GameMap;

public class Action {
    private String name;
    private Effect effect;

    public Action(String name, Effect effect) {
        this.name = name;
        this.effect = effect;
    }

    public String getName() {
        return name;
    }

    public void execute(GameObject target, Hero hero, GameMap map) {
        if (effect != null) {
            effect.apply(target, hero, map);
        }
    }
}
