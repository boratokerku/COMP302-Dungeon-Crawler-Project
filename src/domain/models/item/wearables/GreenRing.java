package domain.models.item.wearables;

public class GreenRing extends Ring {
    public GreenRing(String name) {
        super(name);
    }

    @Override
    public int getEnergyBonus() {
        return 10; // Energy Ring increases max energy by 10
    }
}
