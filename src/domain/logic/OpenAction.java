package domain.logic;

import domain.models.entity.Hero;
import domain.models.entity.GameObject;
import java.util.List;
import java.util.ArrayList;

public class OpenAction implements Action {
    private List<GameObject> contents;

    public OpenAction(List<GameObject> contents) {
        if (contents != null) {
            this.contents = new ArrayList<>(contents);
        } else {
            this.contents = new ArrayList<>();
        }
    }

    @Override
    public String getName() {
        return "Open";
    }

    @Override
    public boolean isAvailable(Hero hero, GameObject target) {
        return true;
    }

    @Override
    public void execute(Hero hero, GameObject target) {
        if (target != null && target.getMap() != null) {
            int tx = target.getX();
            int ty = target.getY();
            domain.models.map.GameMap map = target.getMap();
            
            boolean wasLocked = false;
            if (target instanceof domain.models.entity.Chest) {
                domain.models.entity.Chest chest = (domain.models.entity.Chest) target;
                if (chest.isLocked()) {
                    wasLocked = true;
                    domain.models.staticObjects.KeyItem keyToUse = null;
                    for (GameObject item : hero.getInventory().getItems()) {
                        if (item instanceof domain.models.staticObjects.KeyItem) {
                            keyToUse = (domain.models.staticObjects.KeyItem) item;
                            break;
                        }
                    }
                    if (keyToUse != null) {
                        if (keyToUse.isSingleUse()) {
                            hero.getInventory().removeItem(keyToUse);
                        }
                        chest.setLocked(false);
                        util.helpers.SoundManager.playUnlock();
                        System.out.println("Unlocked chest using key!");
                        view.GameView.addFloatingText(tx, ty, "UNLOCKED!", java.awt.Color.GREEN);
                    } else {
                        // Show warning dialog for locked chest without key!
                        javax.swing.JOptionPane.showMessageDialog(
                                null,
                                "This chest is locked! You need a Key to open it.",
                                "Chest Locked",
                                javax.swing.JOptionPane.WARNING_MESSAGE
                        );
                        System.out.println("Chest is locked! Need a key!");
                        view.GameView.addFloatingText(tx, ty, "LOCKED!", java.awt.Color.RED);
                        return; // Stop execution
                    }
                }
            }
            
            String containerType = wasLocked ? "Locked Chest" : "Unlocked Chest";
            
            // Remove the chest from the map
            map.removeObject(target);
            
            boolean dropped = false;
            for (GameObject item : new ArrayList<>(contents)) {
                if (item instanceof domain.models.item.MapItem) {
                    domain.models.item.MapItem mi = (domain.models.item.MapItem) item;
                    mi.setPosition(tx, ty);
                    map.placeObject(mi, tx, ty);
                    System.out.println("Opened " + containerType + "! Dropped " + mi.getName() + " on the floor.");
                    dropped = true;
                    break;
                }
            }
            
            // Fallback: If chest contents are empty, drop a random loot item
            if (!dropped) {
                domain.models.item.MapItem loot = domain.models.item.MapItem.createRandomItem(tx, ty);
                if (loot != null) {
                    map.placeObject(loot, tx, ty);
                    System.out.println("Opened " + containerType + "! Dropped " + loot.getName() + " on the floor.");
                } else {
                    System.out.println("Opened " + containerType + ", but it was empty.");
                }
            }
            
            util.helpers.SoundManager.playUnlock();
            // Show floating text feedback!
            view.GameView.addFloatingText(tx, ty, "OPENED!", java.awt.Color.YELLOW);
            
            contents.clear();
        }
    }
}
