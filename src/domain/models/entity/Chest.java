package domain.models.entity;

public class Chest extends GameObject {
    public Chest(String name, int x, int y) {
        super(name, x, y, "chest", false);
        java.util.List<GameObject> contents = new java.util.ArrayList<>();
        contents.add(new domain.models.item.PotionItem(x, y));
        this.addAction(new domain.logic.OpenAction(contents));
        this.addAction(new domain.logic.BreakAction());
    }
}
