package domain.logic;

import domain.models.entity.Hero;
import domain.models.entity.GameObject;
import java.util.List;
import java.util.ArrayList;

public class OpenAction implements Action {
    private List<GameObject> contents;

    public OpenAction(List<GameObject> contents) {
        if (contents != null) {
            this.contents = new ArrayList<>(contents);
        } else {
            this.contents = new ArrayList<>();
        }
    }

    @Override
    public String getName() {
        return "Open";
    }

    @Override
    public boolean isAvailable(Hero hero, GameObject target) {
        return true;
    }

    @Override
    public void execute(Hero hero, GameObject target) {
        List<String> itemNames = new ArrayList<>();
        
        for (GameObject item : new ArrayList<>(contents)) {
            if (hero.getInventory() != null && !hero.getInventory().isFull()) {
                hero.getInventory().addItem(item);
                itemNames.add(item.getName());
            }
        }
        contents.clear();
        
        if (!itemNames.isEmpty()) {
            System.out.println("Opened! Got: " + String.join(", ", itemNames));
        } else {
            System.out.println("Opened! Got: nothing (or inventory full)");
        }
    }
}
