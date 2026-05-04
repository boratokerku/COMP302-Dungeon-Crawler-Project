package domain.models.entity;

import domain.models.Direction;
import domain.models.map.GameMap;

import java.util.List;

public class ShadowClone extends Entity {

    private static final long SURE_MS = 7_000; // 7 saniye sonra yok olur
    private final long dogumZamani;

    public ShadowClone(int x, int y) {
        super(x, y, Integer.MAX_VALUE);
        this.dogumZamani = System.currentTimeMillis();
        System.out.println("ShadowClone doğdu! Konum: (" + x + ", " + y + ")");
    }

    @Override
    public void update() {
        // 7 saniye dolunca yok ol
        if (System.currentTimeMillis() - dogumZamani >= SURE_MS) {
            this.alive = false;
            System.out.println("ShadowClone yok oldu.");
        }
    }

    @Override
    public void takeDamage(int amount) {
        // Klon hasar almaz
    }

    // Hero'nun yönünün tersine hareket et
    public void moveOpposite(Direction heroYon, GameMap map, List<Entity> entities) {
        if (!alive) return;

        Direction ters = tersYon(heroYon);
        int yeniX = this.x;
        int yeniY = this.y;

        switch (ters) {
            case UP:    yeniY -= 1; break;
            case DOWN:  yeniY += 1; break;
            case LEFT:  yeniX -= 1; break;
            case RIGHT: yeniX += 1; break;
        }

        if (gidebilirMi(yeniX, yeniY, map, entities)) {
            this.x = yeniX;
            this.y = yeniY;
        }
    }

    private Direction tersYon(Direction yon) {
        switch (yon) {
            case UP:    return Direction.DOWN;
            case DOWN:  return Direction.UP;
            case LEFT:  return Direction.RIGHT;
            case RIGHT: return Direction.LEFT;
            default:    return yon;
        }
    }

    private boolean gidebilirMi(int nx, int ny, GameMap map, List<Entity> entities) {
        if (!map.isWalkable(nx, ny)) return false;
        for (Entity e : entities) {
            if (e == this || !e.isAlive()) continue;
            if (e.getX() == nx && e.getY() == ny) return false;
        }
        return true;
    }
}
