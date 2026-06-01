package domain.logic;

import domain.models.entity.GameObject;
import domain.models.entity.Hero;
import domain.models.entity.Knight;
import domain.models.entity.Sorcerer;
import domain.models.map.GameMap;
import domain.models.item.MapItem;
import domain.models.item.usables.PotionItem;
import java.util.Random;

public class RandomItemSpawner {
    
    public static void placeRandomItem(GameMap map, GameObject item, Hero hero, Knight knight, Sorcerer sorcerer, Random rand) {
        boolean placed = false;
        while (!placed) {
            int x = rand.nextInt(map.getWidth());
            int y = rand.nextInt(map.getHeight());

            if ((x == hero.getX() && y == hero.getY()) ||
                    (x == knight.getX() && y == knight.getY()) ||
                    (x == sorcerer.getX() && y == sorcerer.getY())) {
                continue;
            }

            GameObject existingObj = map.getObjectAt(x, y);
            if (existingObj != null && existingObj.getImageName().equals("floor")
                    && !(existingObj instanceof MapItem)) {
                item.setPosition(x, y);
                map.placeObject(item, x, y);
                placed = true;
            }
        }
    }

    public static int countLockedChests(GameMap map) {
        int count = 0;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj instanceof domain.models.entity.Chest && ((domain.models.entity.Chest) obj).isLocked()) {
                    count++;
                }
            }
        }
        return count;
    }

    public static int countKeys(GameMap map, Hero hero) {
        int count = 0;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj instanceof domain.models.staticObjects.KeyItem) {
                    count++;
                }
            }
        }
        if (hero != null && hero.getInventory() != null) {
            for (GameObject item : hero.getInventory().getItems()) {
                if (item instanceof domain.models.staticObjects.KeyItem) {
                    count++;
                }
            }
        }
        return count;
    }
}
