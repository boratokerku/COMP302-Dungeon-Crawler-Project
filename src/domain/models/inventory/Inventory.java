package domain.models.inventory;

import domain.models.entity.GameObject;
import domain.models.entity.Hero;
import java.util.ArrayList;
import java.util.List;

public class Inventory {
    private final int capacity;
    private final List<GameObject> items;
    private Hero owner;

    // 2x4 layout logic essentially means max 8 items
    public Inventory(int capacity) {
        this.capacity = capacity;
        this.items = new ArrayList<>();
    }

    public Inventory(int capacity, Hero owner) {
        this.capacity = capacity;
        this.items = new ArrayList<>();
        this.owner = owner;
    }

    public void setOwner(Hero owner) {
        this.owner = owner;
    }

    public boolean isFull() {
        return items.size() >= capacity;
    }

    public boolean addItem(GameObject item) {
        if (!isFull() && item != null) {
            items.add(item);
            System.out.println("Added " + item.getName() + " to inventory. (Slots: " + items.size() + "/" + capacity + ")");
            if (owner != null) {
                owner.onInventoryChanged();
            }
            return true;
        }
        return false;
    }

    public boolean removeItem(GameObject item) {
        if (item != null && items.remove(item)) {
            System.out.println("Removed " + item.getName() + " from inventory.");
            if (owner != null) {
                owner.onInventoryChanged();
            }
            return true;
        }
        return false;
    }

    public List<GameObject> getItems() {
        return items;
    }
}
