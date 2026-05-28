package domain.models.item.wearables;

import domain.logic.TakeAction;
import domain.logic.WearAction;
import domain.models.item.MapItem;
import domain.logic.TakeOffAction;
import domain.logic.DiscardAction;

public class RingItem extends MapItem {
    private Ring ring;

    public RingItem(Ring ring, int x, int y, String imagePath) {
        super(ring.getName(), x, y, imagePath);
        this.ring = ring;
        this.addAction(new TakeAction());
        this.addAction(new WearAction());
        this.addAction(new TakeOffAction());
        this.addAction(new DiscardAction());
    }

    // Compatibility constructor
    public RingItem(String name, int x, int y, String imagePath) {
        super(name, x, y, imagePath);
        if (name != null && name.toLowerCase().contains("blue")) {
            this.ring = new BlueRing(name);
        } else if (name != null && name.toLowerCase().contains("red")) {
            this.ring = new RedRing(name);
        } else {
            this.ring = new GreenRing(name);
        }
        this.addAction(new TakeAction());
        this.addAction(new WearAction());
        this.addAction(new TakeOffAction());
        this.addAction(new DiscardAction());
    }

    public RingItem(int x, int y) {
        this(new GreenRing("Ring of Might"), x, y, "images/items/ring/green_ring.png");
    }

    public Ring getRing() {
        return ring;
    }

    @Override
    public int getStrBonus() {
        return ring != null ? ring.getStrBonus() : 0;
    }

    @Override
    public int getHpBonus() {
        return ring != null ? ring.getHpBonus() : 0;
    }

    @Override
    public int getManaCostReduction() {
        return ring != null ? ring.getManaCostReduction() : 0;
    }
}
