package domain.models.item;

import domain.logic.TakeAction;
import domain.logic.UseAction;

/**
 * Victory Coin — dropped by the Final Boss on death.
 * Picking it up and using it from the inventory triggers the game's victory sequence.
 */
public class VictoryCoin extends MapItem {

    private static Runnable victoryCallback = null;

    public VictoryCoin(int x, int y) {
        super("Victory Coin", x, y, "images/items/coin/coin_1.png");
        this.addAction(new TakeAction());
        this.addAction(new UseAction());
    }

    public static void setVictoryCallback(Runnable callback) {
        victoryCallback = callback;
    }

    public static void triggerVictory() {
        if (victoryCallback != null) {
            victoryCallback.run();
        }
    }

    /** Victory Coin is not a weapon. GRASP Polymorphism override. */
    @Override
    public boolean isWeapon() {
        return false;
    }
}
