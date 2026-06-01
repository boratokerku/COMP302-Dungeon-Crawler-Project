package domain.logic;

import domain.models.entity.*;
import domain.models.item.usables.PotionItem;
import domain.models.item.wearables.*;
import domain.models.map.GameMap;
import domain.models.staticObjects.*;
import domain.models.tile.FloorTile;
import domain.models.tile.WallTile;

public class MapFileManager {

    public static String mapToJson(GameMap map, String name) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"name\": \"").append(name).append("\",\n");
        sb.append("  \"timestamp\": \"")
                .append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date()))
                .append("\",\n");
        sb.append("  \"width\": ").append(map.getWidth()).append(",\n");
        sb.append("  \"height\": ").append(map.getHeight()).append(",\n");
        sb.append("  \"objects\": [\n");

        boolean first = true;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj == null || obj instanceof FloorTile)
                    continue;

                if (obj instanceof WallTile) {
                    WallTile wall = (WallTile) obj;
                    GameObject deco = wall.getDecoration();
                    if (deco != null) {
                        String type = objectType(deco);
                        if (type != null) {
                            if (!first)
                                sb.append(",\n");
                            first = false;
                            sb.append("    {\"type\":\"").append(type)
                                    .append("\",\"name\":\"").append(escape(deco.getName()))
                                    .append("\",\"x\":").append(x)
                                    .append(",\"y\":").append(y)
                                    .append(",\"isWallMounted\":true")
                                    .append(",\"customScale\":").append(deco.getCustomScale())
                                    .append(",\"imageName\":\"").append(escape(deco.getImageName())).append("\"");

                            if (deco instanceof SearchableObject) {
                                SearchableObject so = (SearchableObject) deco;
                                sb.append(",\"searched\":").append(so.isSearched());
                                if (so.getHiddenItem() != null) {
                                    sb.append(",\"hiddenItemType\":\"")
                                            .append(so.getHiddenItem().getClass().getSimpleName()).append("\"");
                                }
                            }
                            sb.append("}");
                        }
                    }
                    continue;
                }

                String type = objectType(obj);
                if (type == null)
                    continue;
                if (!first)
                    sb.append(",\n");
                first = false;
                sb.append("    {\"type\":\"").append(type)
                        .append("\",\"name\":\"").append(escape(obj.getName()))
                        .append("\",\"x\":").append(x)
                        .append(",\"y\":").append(y)
                        .append(",\"customScale\":").append(obj.getCustomScale());

                if (obj instanceof Chest) {
                    sb.append(",\"isLocked\":").append(((Chest) obj).isLocked());
                }
                if (obj instanceof Door) {
                    sb.append(",\"isLocked\":").append(((Door) obj).isLocked());
                }
                if (obj instanceof Column || obj instanceof Sign || obj instanceof Decoration) {
                    sb.append(",\"imageName\":\"").append(escape(obj.getImageName())).append("\"");
                }
                if (obj instanceof SearchableObject) {
                    sb.append(",\"openImageName\":\"").append(escape(((SearchableObject) obj).getOpenImageName()))
                            .append("\"")
                            .append(",\"imageName\":\"").append(escape(obj.getImageName())).append("\"");
                }
                if (obj instanceof PotionItem || obj instanceof KeyItem) {
                    sb.append(",\"imageName\":\"").append(escape(obj.getImageName())).append("\"");
                }
                sb.append("}");
            }
        }
        sb.append("\n  ]\n}");
        return sb.toString();
    }

    private static String objectType(GameObject obj) {
        if (obj instanceof LevelDoor)
            return "LevelDoor";
        if (obj instanceof WallObject)
            return "WallObject";
        if (obj instanceof PotionItem)
            return "PotionItem";
        if (obj instanceof SwordItem)
            return "SwordItem";
        if (obj instanceof WoodenSwordItem)
            return "WoodenSwordItem";
        if (obj instanceof SamuraiSwordItem)
            return "SamuraiSwordItem";
        if (obj instanceof DiamondSwordItem)
            return "DiamondSwordItem";
        if (obj instanceof AxeItem)
            return "AxeItem";
        if (obj instanceof BowItem)
            return "BowItem";
        if (obj instanceof FireWandItem)
            return "FireWandItem";
        if (obj instanceof ArmorItem)
            return "ArmorItem";
        if (obj instanceof RingItem)
            return "RingItem";
        if (obj instanceof KeyItem)
            return "KeyItem";
        if (obj instanceof Chest)
            return "Chest";
        if (obj instanceof domain.models.entity.DoubleCrate)
            return "DoubleCrate";
        if (obj instanceof domain.models.entity.Crate)
            return "Crate";
        if (obj instanceof Column)
            return "Column";
        if (obj instanceof Sign)
            return "Sign";
        if (obj instanceof Door)
            return "Door";
        if (obj instanceof Decoration)
            return "Decoration";
        if (obj instanceof SearchableObject)
            return "SearchableObject";
        return null;
    }

    public static void loadMapFromJson(GameMap map, String json) {
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj instanceof WallTile) {
                    ((WallTile) obj).setDecoration(null);
                } else {
                    map.placeObject(new FloorTile(), x, y);
                }
            }
        }

        for (String line : json.split("\n")) {
            line = line.trim();
            if (!line.startsWith("{\"type\""))
                continue;
            String type = jsonStr(line, "type");
            String name = jsonStr(line, "name");
            int x = jsonInt(line, "x");
            int y = jsonInt(line, "y");
            boolean locked = "true".equals(jsonStr(line, "isLocked"));
            String imgName = jsonStr(line, "imageName");
            String openImgName = jsonStr(line, "openImageName");
            boolean isWallMounted = "true".equals(jsonStr(line, "isWallMounted"));
            double scale = jsonDouble(line, "customScale");
            boolean searched = "true".equals(jsonStr(line, "searched"));
            String hiddenItemType = jsonStr(line, "hiddenItemType");

            GameObject obj = GameObjectFactory.create(type, name, x, y, locked, imgName);

            if (obj == null && "WallObject".equals(type) && imgName != null && imgName.contains("WallSearchable/")) {
                obj = new SearchableObject(name, x, y, imgName, openImgName);
            } else if (obj instanceof SearchableObject && openImgName != null && !openImgName.isEmpty()) {
                obj = new SearchableObject(name, x, y, imgName, openImgName);
            }
            if (obj != null) {
                obj.setCustomScale(scale);
                if (obj instanceof SearchableObject) {
                    SearchableObject so = (SearchableObject) obj;
                    so.setSearched(searched);
                    if (hiddenItemType != null && !hiddenItemType.isEmpty()) {
                        if (hiddenItemType.equals("LevelKey")) {
                            so.setHiddenItem(new LevelKey(x, y));
                        } else if (hiddenItemType.equals("KeyItem")) {
                            so.setHiddenItem(new KeyItem(x, y));
                        }
                    }
                }
                if (isWallMounted) {
                    GameObject existing = map.getObjectAt(x, y);
                    if (existing instanceof WallTile) {
                        ((WallTile) existing).setDecoration(obj);
                    }
                } else {
                    map.placeObject(obj, x, y);
                }
            }
        }
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }

    private static String jsonStr(String line, String key) {
        String k = "\"" + key + "\":\"";
        int i = line.indexOf(k);
        if (i < 0) {
            String k2 = "\"" + key + "\":";
            int i2 = line.indexOf(k2);
            if (i2 < 0)
                return null;
            i2 += k2.length();
            int j2 = i2;
            while (j2 < line.length() && line.charAt(j2) != ',' && line.charAt(j2) != '}') {
                j2++;
            }
            String val = line.substring(i2, j2).trim();
            if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                val = val.substring(1, val.length() - 1);
            }
            return val;
        }
        i += k.length();
        int j = line.indexOf("\"", i);
        return j < 0 ? null : line.substring(i, j);
    }

    private static int jsonInt(String line, String key) {
        String k = "\"" + key + "\":";
        int i = line.indexOf(k);
        if (i < 0)
            return 0;
        i += k.length();
        int j = i;
        while (j < line.length() && (Character.isDigit(line.charAt(j)) || line.charAt(j) == '-'))
            j++;
        try {
            return Integer.parseInt(line.substring(i, j));
        } catch (Exception e) {
            return 0;
        }
    }

    private static double jsonDouble(String line, String key) {
        String k = "\"" + key + "\":";
        int i = line.indexOf(k);
        if (i < 0)
            return 1.0;
        i += k.length();
        int j = i;
        while (j < line.length()
                && (Character.isDigit(line.charAt(j)) || line.charAt(j) == '-' || line.charAt(j) == '.'))
            j++;
        try {
            return Double.parseDouble(line.substring(i, j));
        } catch (Exception e) {
            return 1.0;
        }
    }
}
