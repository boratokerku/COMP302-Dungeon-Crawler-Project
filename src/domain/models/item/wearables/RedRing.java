package domain.models.item.wearables;

public class RedRing extends Ring {
    public RedRing(String name) {
        super(name);
    }

    @Override
    public int getHpBonus() {
        return 5; // Red Ring increases max HP by 5 (raising max HP to 22)
    }
}
