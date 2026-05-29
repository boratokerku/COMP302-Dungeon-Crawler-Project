package domain.models.staticObjects;

import domain.models.entity.GameObject;

/**
 * A special door that transitions between dungeon levels.
 * Requires a LevelKey in the hero's inventory to unlock.
 */
public class LevelDoor extends Door {

    private static Runnable openCallback = null;

    public static void setOpenCallback(Runnable callback) {
        openCallback = callback;
    }

    public static void triggerOpenTransition() {
        if (openCallback != null) {
            openCallback.run();
        }
    }

    public LevelDoor(String name, int x, int y) {
        super(name, x, y, true); // Always starts locked
        this.addAction(new domain.logic.UnlockLevelGateAction());
    }

    /**
     * Attempts to unlock this door using a LevelKey from the hero's inventory.
     * @param hero The hero attempting to open the door
     * @return true if the door was unlocked successfully
     */
    public boolean tryUnlockWithKey(domain.models.entity.Hero hero) {
        if (!isLocked()) return true; // Already unlocked

        if (hero == null || hero.getInventory() == null) return false;

        // Search for a LevelKey in inventory
        LevelKey keyToUse = null;
        for (GameObject item : hero.getInventory().getItems()) {
            if (item instanceof LevelKey) {
                keyToUse = (LevelKey) item;
                break;
            }
        }

        if (keyToUse != null) {
            hero.getInventory().removeItem(keyToUse);
            unlock();
            open();
            return true;
        }

        return false;
    }
}
