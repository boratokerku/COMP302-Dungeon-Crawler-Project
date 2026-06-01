package domain.logic;

import domain.models.entity.Hero;
import domain.models.GameObject;

public interface Action {
    String getName();
    boolean isAvailable(Hero hero, GameObject target);
    void execute(Hero hero, GameObject target);
}
