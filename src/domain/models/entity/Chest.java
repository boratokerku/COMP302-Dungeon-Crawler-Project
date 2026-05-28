package domain.models.entity;

public class Chest extends GameObject {
    private boolean isLocked = false;

    public Chest(String name, int x, int y, boolean isLocked, String customImage) {
        super(name, x, y, customImage, false);
        this.isLocked = isLocked;
        java.util.List<GameObject> contents = new java.util.ArrayList<>();
        contents.add(domain.models.item.PotionItem.createRandomPotionItem(x, y));
        this.addAction(new domain.logic.OpenAction(contents));
        this.addAction(new domain.logic.BreakAction());
    }

    public Chest(String name, int x, int y, boolean isLocked) {
        this(name, x, y, isLocked, getSpriteForName(name));
    }

    public Chest(String name, int x, int y) {
        this(name, x, y, false);
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean isLocked) {
        this.isLocked = isLocked;
    }

    private static String getSpriteForName(String name) {
        if (name == null) {
            return "chest";
        }
        String lower = name.toLowerCase();
        if (lower.contains("brown chest") || lower.contains("chest_brown")) {
            return "containers/chest_brown";
        } else if (lower.contains("red chest") || lower.contains("chest_red")) {
            return "containers/chest_red";
        } else if (lower.contains("white chest") || lower.contains("chest_white")) {
            return "containers/chest_white";
        } else if (lower.contains("gold chest") || lower.contains("gold_chest")) {
            return "containers/gold_chest_closed";
        } else if (lower.contains("silver chest") || lower.contains("silver_chest")) {
            return "containers/silver_chest_closed";
        } else if (lower.contains("magical bag") || lower.contains("magical_bag")) {
            return "containers/magical_bag";
        } else if (lower.contains("bag")) {
            return "containers/bag";
        }
        return "chest";
    }
}

