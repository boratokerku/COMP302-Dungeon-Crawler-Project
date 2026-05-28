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
        if (!hero.getInventory().getItems().contains(target)) {
            return false;
        }
        if (target instanceof domain.models.item.usables.PotionItem) {
            domain.models.item.Item pot = ((domain.models.item.usables.PotionItem) target).getPotion();
            if (pot instanceof domain.models.item.usables.ManaPotion) {
                return hero.getMana() < 80;
            } else if (pot instanceof domain.models.item.usables.EnergyPotion) {
                return hero.getEnergy() < 100;
            } else if (pot instanceof domain.models.item.usables.HealthPotion) {
                return hero.getHp() < hero.getMaxHp();
            }
        }
        return true;
    }

    @Override
    public void execute(Hero hero, GameObject target) {
        if (target instanceof domain.models.item.usables.PotionItem) {
            domain.models.item.usables.PotionItem potionItem = (domain.models.item.usables.PotionItem) target;
            domain.models.item.Item pot = potionItem.getPotion();

            // Execute the domain item logic
            pot.use(hero);

            // Execute the feedback logic (sound, floating text)
            util.helpers.SoundManager.playHeal();
            if (pot instanceof domain.models.item.usables.ManaPotion) {
                view.GameView.addFloatingText(hero.getX(), hero.getY(), "+20 Mana", java.awt.Color.CYAN);
            } else if (pot instanceof domain.models.item.usables.EnergyPotion) {
                view.GameView.addFloatingText(hero.getX(), hero.getY(), "+30 Energy", java.awt.Color.YELLOW);
            } else if (pot instanceof domain.models.item.usables.HealthPotion) {
                view.GameView.addFloatingText(hero.getX(), hero.getY(), "+5 HP", java.awt.Color.GREEN);
            }

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
