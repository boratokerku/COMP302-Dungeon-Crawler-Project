package domain.logic;

import domain.models.entity.Hero;
import domain.models.entity.GameObject;

public class UseAction implements Action {

    @Override
    public String getName() {
        return "Use";
    }

    @Override
    public boolean isAvailable(Hero hero, GameObject target) {
        return hero.getInventory().getItems().contains(target);
    }

    @Override
    public void execute(Hero hero, GameObject target) {
        if (target instanceof domain.models.item.PotionItem) {
            hero.heal(5); // Heal 5 HP
            System.out.println("Used Potion. Hero healed for 5 HP. Current HP: " + hero.getHp());
            hero.getInventory().removeItem(target);
        } else if (target instanceof domain.models.staticObjects.KeyItem) {
            domain.models.staticObjects.KeyItem key = (domain.models.staticObjects.KeyItem) target;
            domain.models.map.GameMap map = hero.getCurrentMap();
            boolean unlockedAny = false;
            
            if (map != null) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        int nx = hero.getX() + dx;
                        int ny = hero.getY() + dy;
                        if (nx >= 0 && nx < map.getWidth() && ny >= 0 && ny < map.getHeight()) {
                            domain.models.entity.GameObject obj = map.getObjectAt(nx, ny);
                            if (obj instanceof domain.models.staticObjects.Door) {
                                domain.models.staticObjects.Door door = (domain.models.staticObjects.Door) obj;
                                if (door.isLocked()) {
                                    door.unlock();
                                    door.open();
                                    unlockedAny = true;
                                    System.out.println("Door unlocked and opened at (" + nx + ", " + ny + ")!");
                                    
                                    // Show floating text feedback!
                                    view.GameView.addFloatingText(nx, ny, "UNLOCKED!", java.awt.Color.GREEN);
                                }
                            }
                        }
                    }
                }
            }
            
            if (unlockedAny) {
                if (key.isSingleUse()) {
                    hero.getInventory().removeItem(target);
                    System.out.println("Key was single use and has been removed from inventory.");
                } else {
                    System.out.println("Key is multi-use and remains in inventory.");
                }
            } else {
                System.out.println("No adjacent locked doors to unlock!");
            }
        } else if (target instanceof domain.models.item.VictoryCoin) {
            domain.models.item.VictoryCoin.triggerVictory();
        }
    }
}
