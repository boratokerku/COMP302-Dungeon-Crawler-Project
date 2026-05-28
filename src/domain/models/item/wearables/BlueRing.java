package domain.models.item.wearables;

public class BlueRing extends Ring {
    public BlueRing(String name) {
        super(name);
    }

    @Override
    public int getManaCostReduction() {
        return 1; // Blue Ring decreases mana spent by 1
    }
}
