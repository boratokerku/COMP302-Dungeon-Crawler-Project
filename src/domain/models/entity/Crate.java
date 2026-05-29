package domain.models.entity;

public class Crate extends GameObject {
    public Crate(String name, int x, int y, String customImage) {
        super(name, x, y, customImage, false);
        this.addAction(new domain.logic.BreakAction());
    }

    public Crate(String name, int x, int y) {
        this(name, x, y, getSpriteForName(name));
    }

    private GameObject hiddenItem = null;

    public GameObject getHiddenItem() {
        return this.hiddenItem;
    }

    public void setHiddenItem(GameObject hiddenItem) {
        this.hiddenItem = hiddenItem;
    }

    private static String getSpriteForName(String name) {
        if (name != null && name.toLowerCase().contains("brown")) {
            return "containers/crate_brown";
        }
        return "crate";
    }
}
