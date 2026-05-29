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
        if (name != null && name.toLowerCase().contains("blue")) {
            this.potion = new ManaPotion(name, 20);
        } else if (name != null && name.toLowerCase().contains("green")) {
            this.potion = new EnergyPotion(name, 30);
        } else {
            this.potion = new HealthPotion(name, 5);
        }
        this.addAction(new TakeAction());
        this.addAction(new UseAction());
        this.addAction(new DiscardAction());
    }

    public PotionItem(int x, int y) {
        this(new HealthPotion("Red Potion", 5), x, y, "images/items/potion/red_potion.png");
    }

    public Item getPotion() {
        return potion;
    }

    public static PotionItem createRandomPotionItem(int x, int y) {
        double roll = Math.random();
        if (roll < 0.4) {
            return new PotionItem(new HealthPotion("Red Potion", 5), x, y, "images/items/potion/red_potion.png");
        } else if (roll < 0.7) {
            return new PotionItem(new ManaPotion("Blue Potion", 20), x, y, "images/items/potion/blue_potion.png");
        } else {
            return new PotionItem(new EnergyPotion("Green Potion", 30), x, y, "images/items/potion/green_potion.png");
        }
    }
}
