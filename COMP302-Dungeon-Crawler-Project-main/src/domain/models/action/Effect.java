package domain.models.action;

import domain.models.entity.GameObject;
import domain.models.entity.Hero;
import domain.models.map.GameMap;

public interface Effect {
    void apply(GameObject target, Hero hero, GameMap map);
}
