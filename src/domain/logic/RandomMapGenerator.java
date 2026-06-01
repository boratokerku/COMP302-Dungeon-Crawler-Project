package domain.logic;

import domain.models.staticObjects.Chest;
import domain.models.staticObjects.Column;
import domain.models.staticObjects.Crate;
import domain.models.GameObject;
import domain.models.staticObjects.SearchableObject;
import domain.models.item.MapItem;
import domain.models.item.usables.PotionItem;
import domain.models.map.GameMap;
import domain.models.staticObjects.Decoration;
import domain.models.item.KeyItem;
import domain.models.staticObjects.LevelDoor;
import domain.models.staticObjects.WallObject;
import domain.models.tile.FloorTile;
import domain.models.tile.WallTile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RandomMapGenerator {
    private static final int MAX_SEARCHABLE_PER_MAP = 3;
    private static final int MAX_DECORATIVE_PER_MAP = 10;

    public static void generateRandomMap(GameMap map) {
        Random rand = new Random();
        int w = map.getWidth();
        int h = map.getHeight();

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj instanceof WallTile) {
                    ((WallTile) obj).setDecoration(null);
                }

                if (x == 0 || x == w - 1) {
                    map.placeObject(new WallTile("wall/wall_side"), x, y);
                } else if (y == 0) {
                    map.placeObject(new WallTile("wall/wall_1"), x, y);
                } else if (y == h - 1) {
                    map.placeObject(new WallTile("wall/wall_2"), x, y);
                } else {
                    map.placeObject(new FloorTile(), x, y);
                }
            }
        }

        boolean[][] reserved = new boolean[w][h];
        for (int rx = 3; rx <= 5; rx++) {
            for (int ry = 3; ry <= 5; ry++) {
                reserved[rx][ry] = true;
            }
        }

        int doorX = rand.nextInt(w - 4) + 2;
        map.placeObject(new LevelDoor("Level Gate", doorX, 0), doorX, 0);
        map.placeObject(new FloorTile(), doorX, 1);
        reserved[doorX][1] = true;

        int numColumns = rand.nextInt(3) + 2;
        for (int i = 0; i < numColumns; i++) {
            int cx, cy;
            int attempts = 0;
            do {
                cx = rand.nextInt(w - 4) + 2;
                cy = rand.nextInt(h - 4) + 2;
                attempts++;
            } while ((reserved[cx][cy] || !isFarEnough(map, cx, cy)) && attempts < 100);

            if (attempts < 100) {
                String colImg = rand.nextBoolean() ? "colon/gray_colon_whole" : "colon/purple_colon_whole";
                map.placeObject(new Column("Column", cx, cy, colImg), cx, cy);
                reserved[cx][cy] = true;
            }
        }

        int numChests = rand.nextInt(3) + 2;
        int lockedChestCount = 0;
        String[] chestTypes = { "Brown Chest", "Red Chest", "White Chest", "Gold Chest", "Silver Chest", "Bag" };
        for (int i = 0; i < numChests; i++) {
            int cx, cy;
            int attempts = 0;
            do {
                cx = rand.nextInt(w - 2) + 1;
                cy = rand.nextInt(h - 2) + 1;
                attempts++;
            } while ((reserved[cx][cy] || !isFarEnough(map, cx, cy)) && attempts < 100);

            if (attempts < 100) {
                String chosenType = chestTypes[rand.nextInt(chestTypes.length)];
                boolean locked = (i == 0) && !chosenType.equals("Bag");
                if (locked)
                    lockedChestCount++;
                map.placeObject(new Chest(chosenType, cx, cy, locked), cx, cy);
                reserved[cx][cy] = true;
            }
        }

        int numCrates = rand.nextInt(3) + 4;
        for (int i = 0; i < numCrates; i++) {
            int cx, cy;
            int attempts = 0;
            do {
                cx = rand.nextInt(w - 2) + 1;
                cy = rand.nextInt(h - 2) + 1;
                attempts++;
            } while ((reserved[cx][cy] || !isFarEnough(map, cx, cy)) && attempts < 100);

            if (attempts < 100) {
                map.placeObject(new Crate("Crate", cx, cy), cx, cy);
                reserved[cx][cy] = true;
            }
        }

        for (int i = 0; i < lockedChestCount; i++) {
            int keyX, keyY;
            int attempts = 0;
            do {
                keyX = rand.nextInt(w - 4) + 2;
                keyY = rand.nextInt(h - 4) + 2;
                attempts++;
            } while ((reserved[keyX][keyY] || !isFarEnough(map, keyX, keyY)) && attempts < 100);
            if (attempts < 100) {
                map.placeObject(new KeyItem(keyX, keyY), keyX, keyY);
                reserved[keyX][keyY] = true;
            }
        }

        int numPotions = rand.nextInt(2) + 2;
        for (int i = 0; i < numPotions; i++) {
            int px, py;
            int patts = 0;
            do {
                px = rand.nextInt(w - 2) + 1;
                py = rand.nextInt(h - 2) + 1;
                patts++;
            } while ((reserved[px][py] || !isFarEnough(map, px, py)) && patts < 100);
            if (patts < 100) {
                map.placeObject(PotionItem.createRandomPotionItem(px, py), px, py);
                reserved[px][py] = true;
            }
        }

        int numWeapons = rand.nextInt(3) + 2;
        for (int i = 0; i < numWeapons; i++) {
            int wx, wy;
            int watts = 0;
            do {
                wx = rand.nextInt(w - 2) + 1;
                wy = rand.nextInt(h - 2) + 1;
                watts++;
            } while ((reserved[wx][wy] || !isFarEnough(map, wx, wy)) && watts < 100);
            if (watts < 100) {
                GameObject weapon = MapItem.createRandomItem(wx, wy);
                map.placeObject(weapon, wx, wy);
                reserved[wx][wy] = true;
            }
        }

        int numTorches = rand.nextInt(3) + 3;
        for (int i = 0; i < numTorches; i++) {
            int tx, ty;
            int tatts = 0;
            do {
                tx = rand.nextInt(w - 2) + 1;
                ty = rand.nextInt(h - 2) + 1;
                tatts++;
            } while ((reserved[tx][ty] || !isFarEnough(map, tx, ty)) && tatts < 100);
            if (tatts < 100) {
                map.placeObject(new Decoration("Torch", tx, ty, "torch/torch_1"), tx, ty);
                reserved[tx][ty] = true;
            }
        }

        List<String[]> wallObjects = new ArrayList<>();
        wallObjects.add(new String[] { "WallObject", "Banner (Blue)", "WallDecoration/banner_blue" });
        wallObjects.add(new String[] { "WallObject", "Banner (Red)", "WallDecoration/banner_red" });
        wallObjects.add(new String[] { "WallObject", "Blood Stain", "WallDecoration/blood_stain" });
        wallObjects.add(new String[] { "SearchableObject", "Gargoyle Statue", "WallSearchable/gargoyle" });
        wallObjects.add(new String[] { "WallObject", "Goo Stain", "WallDecoration/goo_stain" });
        wallObjects.add(new String[] { "WallObject", "Moss", "WallDecoration/moss" });

        int levelDoorXVal = -1;
        for (int x = 0; x < w; x++) {
            GameObject obj = map.getObjectAt(x, 0);
            if (obj instanceof LevelDoor) {
                levelDoorXVal = x;
                break;
            }
        }

        List<int[]> wallTiles = new ArrayList<>();
        for (int x = 1; x < w - 1; x++) {
            if (levelDoorXVal != -1 && (x == levelDoorXVal - 1 || x == levelDoorXVal + 1)) {
            } else {
                GameObject topWall = map.getObjectAt(x, 0);
                if (topWall instanceof WallTile && ((WallTile) topWall).getDecoration() == null) {
                    wallTiles.add(new int[] { x, 0 });
                }
            }
            GameObject botWall = map.getObjectAt(x, h - 1);
            if (botWall instanceof WallTile && ((WallTile) botWall).getDecoration() == null) {
                wallTiles.add(new int[] { x, h - 1 });
            }
        }

        if (!wallTiles.isEmpty()) {
            Collections.shuffle(wallTiles);
            int numWallObjects = rand.nextInt(5) + 6;
            int placedWallObjs = 0;
            int placedSearchables = 0;
            int placedDecoratives = 0;
            for (int[] pos : wallTiles) {
                if (placedWallObjs >= numWallObjects)
                    break;
                
                List<String[]> validCandidates = new ArrayList<>();
                for (String[] wo : wallObjects) {
                    if (wo[2].contains("WallSearchable/") && placedSearchables < MAX_SEARCHABLE_PER_MAP) {
                        validCandidates.add(wo);
                    } else if (wo[2].contains("WallDecoration/") && placedDecoratives < MAX_DECORATIVE_PER_MAP) {
                        validCandidates.add(wo);
                    }
                }
                if (validCandidates.isEmpty())
                    break;

                String[] selectedItem = validCandidates.get(rand.nextInt(validCandidates.size()));
                GameObject wallObj = GameObjectFactory.create(selectedItem[0], selectedItem[1], pos[0], pos[1], false, selectedItem[2]);
                if (wallObj == null) {
                    wallObj = new SearchableObject(selectedItem[1], pos[0], pos[1], selectedItem[2], null);
                }

                if (selectedItem[2].contains("WallSearchable/")) {
                    placedSearchables++;
                } else if (selectedItem[2].contains("WallDecoration/")) {
                    placedDecoratives++;
                }

                map.placeObject(wallObj, pos[0], pos[1]);
                placedWallObjs++;
            }
        }
    }

    private static boolean isFarEnough(GameMap map, int tx, int ty) {
        int[][] dirs = { { 0, 0 }, { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        for (int[] d : dirs) {
            int nx = tx + d[0];
            int ny = ty + d[1];
            if (map.isValidPosition(nx, ny)) {
                GameObject obj = map.getObjectAt(nx, ny);
                if (obj != null && !(obj instanceof FloorTile) && !(obj instanceof WallTile)) {
                    return false;
                }
            }
        }
        return true;
    }
}
