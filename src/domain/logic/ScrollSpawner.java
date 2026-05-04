package domain.logic;

import domain.models.entity.Entity;
import domain.models.item.ShadowCloneScroll;
import domain.models.map.GameMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ScrollSpawner {

    private static final long SPAWN_INTERVAL_MS = 15_000; // 15 saniyede bir scroll çıkar

    private long lastSpawnTime;
    private final GameMap map;
    private final List<Entity> entities;
    private final controller.InputHandler inputHandler;
    private final Random random = new Random();

    public ScrollSpawner(GameMap map, List<Entity> entities, controller.InputHandler inputHandler) {
        this.map = map;
        this.entities = entities;
        this.inputHandler = inputHandler;
        this.lastSpawnTime = System.currentTimeMillis();
    }

    public long getTimeLeft() {
        return Math.max(0, SPAWN_INTERVAL_MS - (System.currentTimeMillis() - lastSpawnTime));
    }

    public void setTimeLeft(long timeLeft) {
        this.lastSpawnTime = System.currentTimeMillis() - (SPAWN_INTERVAL_MS - timeLeft);
    }

    // Her logic tick'te çağrılır — 15 saniye dolunca scroll çıkarır
    public void trySpawn() {
        long now = System.currentTimeMillis();
        if (now - lastSpawnTime < SPAWN_INTERVAL_MS) return;
        lastSpawnTime = now;

        int[] pos = findRandomEmptyTile();
        if (pos == null) {
            System.out.println("ScrollSpawner: Boş tile bulunamadı.");
            return;
        }

        ShadowCloneScroll scroll = new ShadowCloneScroll(pos[0], pos[1], entities, map, inputHandler);
        map.placeObject(scroll, pos[0], pos[1]);
        System.out.println("ScrollSpawner: Shadow Clone Scroll çıktı! Konum: (" + pos[0] + ", " + pos[1] + ")");
    }

    // Haritada rastgele boş yürünebilir tile bul
    private int[] findRandomEmptyTile() {
        int w = map.getWidth();
        int h = map.getHeight();
        List<int[]> candidates = new ArrayList<>();

        for (int x = 1; x < w - 1; x++) {
            for (int y = 1; y < h - 1; y++) {
                if (!map.isWalkable(x, y)) continue;

                // Başka bir entity üzerine koyma
                boolean occupied = false;
                for (Entity e : entities) {
                    if (e.isAlive() && e.getX() == x && e.getY() == y) {
                        occupied = true;
                        break;
                    }
                }
                if (!occupied) candidates.add(new int[]{x, y});
            }
        }

        if (candidates.isEmpty()) return null;
        return candidates.get(random.nextInt(candidates.size()));
    }
}
