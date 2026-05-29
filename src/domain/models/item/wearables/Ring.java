package domain.models.item.wearables;

import domain.models.item.Item;

public abstract class Ring extends Item {
    public Ring(String name) {
        super(name, 0.2); // Rings are light weight (0.2)
    }

    @Override
    public void use(domain.models.entity.Hero hero) {
        // No-op: Rings are equipped/worn via WearAction, not consumed via UseAction
    }

    public int getStrBonus() {
        return 0;
    }

    public int getHpBonus() {
        return 0;
    }

    public int getManaCostReduction() {
        return 0;
    }
}
