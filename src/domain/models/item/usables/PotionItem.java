package domain.models.item.usables;

import domain.logic.TakeAction;
import domain.logic.UseAction;
import domain.models.item.Item;
import domain.models.item.MapItem;
import domain.logic.DiscardAction;

public class PotionItem extends MapItem {
    private Item potion;

    public PotionItem(Item potion, int x, int y, String imagePath) {
        super(potion.getName(), x, y, imagePath);
        this.potion = potion;
        this.addAction(new TakeAction());
        this.addAction(new UseAction());
        this.addAction(new DiscardAction());
    }

    // Compatibility constructor for custom-named potion/placeholder items in the
    // palette
    public PotionItem(String name, int x, int y, String imagePath) {
        super(name, x, y, imagePath);
        String lower = name != null ? name.toLowerCase() : "";
        if (lower.contains("blue") || lower.contains("mana")) {
            this.potion = new ManaPotion(name, 20);
        } else if (lower.contains("green") || lower.contains("poison") || lower.contains("energy")) {
            this.potion = new EnergyPotion(name, 30);
        } else {
            this.potion = new HealthPotion(name, 5);
        }
        this.addAction(new TakeAction());
        this.addAction(new UseAction());
        this.addAction(new DiscardAction());
    }

    public PotionItem(int x, int y) {
        this(new HealthPotion("Health Potion", 5), x, y, "images/items/potion/red_potion.png");
    }

    public Item getPotion() {
        return potion;
    }

    public static PotionItem createRandomPotionItem(int x, int y) {
        double roll = Math.random();
        if (roll < 0.4) {
            return new PotionItem(new HealthPotion("Health Potion", 5), x, y, "images/items/potion/red_potion.png");
        } else if (roll < 0.7) {
            return new PotionItem(new ManaPotion("Mana Potion", 20), x, y, "images/items/potion/blue_potion.png");
        } else {
            return new PotionItem(new EnergyPotion("Poison Potion", 30), x, y, "images/items/potion/green_potion.png");
        }
    }
}
