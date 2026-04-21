package domain.models.entity;

import domain.models.item.Item;
import java.util.ArrayList;
import java.util.List;

public class Inventory {
    private final int MAX_SIZE = 8; // 2x4 grid size from design doc
    private List<Item> items;

    public Inventory() {
        this.items = new ArrayList<>();
    }

    public boolean addItem(Item item) {
        if (isFull()) {
            return false;
        }
        items.add(item);
        return true;
    }

    public boolean isFull() {
        return items.size() >= MAX_SIZE;
    }

    public List<Item> getItems() {
        return items;
    }
}
