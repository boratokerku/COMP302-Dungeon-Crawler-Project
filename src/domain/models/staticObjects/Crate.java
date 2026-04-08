package domain.models.staticObjects;

import domain.models.item.Item;
import java.util.ArrayList;
import java.util.List;

public class Crate extends StaticObject {
    private int hp = 1; // Genelde tek vuruşta kırılır ama istersen artırabilirsin
    private List<Item> contents; // Kırıldığında yere düşecek eşyalar

    public Crate(int x, int y) {
        // x, y, obstacle=true (geçilemez), breakable=true (kırılabilir)
        super(x, y, true, true);
        this.contents = new ArrayList<>();
    }

    /**
     * Hero 'BREAK' aksiyonu yaptığında bu metot çağrılır.
     */
    public void takeDamage(int damage) {
        this.hp -= damage;
        if (this.hp <= 0) {
            onDestroyed();
        }
    }

    private void onDestroyed() {
        System.out.println("Crate smashed!");
        // Burada içindeki eşyaları yere (floor) bırakma mantığı tetiklenecek
        this.obstacle = false; // Artık üzerinden geçilebilir
    }

    // Kasaya eşya eklemek için (Map Generator kullanacak)
    public void addItem(Item item) {
        contents.add(item);
    }
}