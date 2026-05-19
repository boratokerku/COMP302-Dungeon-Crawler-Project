package domain.models.entity;

public class Chest extends GameObject {
    private boolean isLocked = false;

    public Chest(String name, int x, int y, boolean isLocked) {
        super(name, x, y, "chest", false);
        this.isLocked = isLocked;
        java.util.List<GameObject> contents = new java.util.ArrayList<>();
        contents.add(new domain.models.item.PotionItem(x, y));
        this.addAction(new domain.logic.OpenAction(contents));
        this.addAction(new domain.logic.BreakAction());
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
}
