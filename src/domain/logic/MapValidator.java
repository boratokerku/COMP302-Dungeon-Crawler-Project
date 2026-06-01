package domain.logic;

import domain.models.entity.Chest;
import domain.models.entity.GameObject;
import domain.models.map.GameMap;
import domain.models.staticObjects.KeyItem;

public class MapValidator {
    public static boolean validate(GameMap map) {
        return validateChestKeyCounts(map);
    }

    public static boolean validateTeamMatch(GameMap map) {
        return validateChestKeyCounts(map);
    }

    private static boolean validateChestKeyCounts(GameMap map) {
        int lockedChests = 0;
        int keys = 0;

        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj instanceof Chest && ((Chest) obj).isLocked()) {
                    lockedChests++;
                } else if (obj instanceof KeyItem) {
                    keys++;
                }
            }
        }

        return keys == lockedChests;
    }
}
