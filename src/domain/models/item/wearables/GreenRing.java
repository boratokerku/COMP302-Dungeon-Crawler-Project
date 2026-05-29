package domain.models.item.wearables;

public class GreenRing extends Ring {
    public GreenRing(String name) {
        super(name);
    }

    @Override
    public int getStrBonus() {
        return 5; // Might Ring increases strength by 5
    }
}
