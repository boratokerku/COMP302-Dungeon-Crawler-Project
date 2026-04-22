package domain.models.inventory;

import domain.models.entity.GameObject;
import java.util.ArrayList;
import java.util.List;

public class Inventory {
    private final int capacity;
    private final List<GameObject> items;

    // 2x4 layout logic essentially means max 8 items
    public Inventory(int capacity) {
        this.capacity = capacity;
        this.items = new ArrayList<>();
    }

    public boolean isFull() {
        return items.size() >= capacity;
    }

    public boolean addItem(GameObject item) {
        if (!isFull() && item != null) {
            items.add(item);
            System.out.println("Added " + item.getName() + " to inventory. (Slots: " + items.size() + "/" + capacity + ")");
            return true;
        }
        return false;
    }

    public List<GameObject> getItems() {
        return items;
    }
}
