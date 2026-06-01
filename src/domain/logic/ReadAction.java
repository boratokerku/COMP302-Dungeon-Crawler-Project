package domain.logic;

import domain.models.entity.Hero;
import domain.models.entity.Entity;
import domain.models.GameObject;
import domain.models.entity.ShadowClone;
import domain.models.map.GameMap;

import java.util.List;

public class ReadAction implements Action {

    private final List<Entity> entities;
    private final GameMap map;
    private final controller.InputHandler inputHandler;

    public ReadAction(List<Entity> entities, GameMap map, controller.InputHandler inputHandler) {
        this.entities = entities;
        this.map = map;
        this.inputHandler = inputHandler;
    }

    @Override
    public String getName() {
        return "Read";
    }

    @Override
    public boolean isAvailable(Hero hero, GameObject target) {
        // Sadece envanterdeyken okunabilir
        return hero.getInventory().getItems().contains(target);
    }

    @Override
    public void execute(Hero hero, GameObject target) {
        // Hero'nun yanında boş tile bul
        int[] spawnPos = findAdjacentEmpty(hero.getX(), hero.getY());
        if (spawnPos == null) {
            System.out.println("Shadow Clone için boş yer bulunamadı.");
            return;
        }

        // Klonu oluştur, sisteme ekle
        ShadowClone clone = new ShadowClone(spawnPos[0], spawnPos[1]);
        entities.add(clone);
        inputHandler.setShadowClone(clone);

        // Scrollu envanterden kaldır (tek kullanımlık)
        hero.getInventory().removeItem(target);
    }

    // Hero'nun 4 komşu tile'ından boş ve yürünebilir olanı bul
    private int[] findAdjacentEmpty(int heroX, int heroY) {
        int[][] adj = {
            {heroX,     heroY - 1},
            {heroX,     heroY + 1},
            {heroX - 1, heroY},
            {heroX + 1, heroY}
        };

        for (int[] pos : adj) {
            if (!map.isWalkable(pos[0], pos[1])) continue;

            boolean occupied = false;
            for (Entity e : entities) {
                if (e.isAlive()) {
                    if (e.occupiesTile(pos[0], pos[1])) {
                        occupied = true;
                        break;
                    }
                }
            }
            if (!occupied) return pos;
        }
        return null;
    }
}
