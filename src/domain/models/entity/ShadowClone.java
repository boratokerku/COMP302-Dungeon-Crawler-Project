package domain.models.entity;

import domain.models.Direction;
import domain.models.map.GameMap;

import java.util.List;

public class ShadowClone extends Entity {

    private static final long LIFETIME_MS = 7_000; // 7 saniye sonra yok olur
    private long birthTime;

    public ShadowClone(int x, int y) {
        super(x, y, Integer.MAX_VALUE);
        this.birthTime = System.currentTimeMillis();
        System.out.println("ShadowClone doğdu! Konum: (" + x + ", " + y + ")");
    }

    public long getTimeLeft() {
        return Math.max(0, LIFETIME_MS - (System.currentTimeMillis() - birthTime));
    }

    public void setTimeLeft(long timeLeft) {
        this.birthTime = System.currentTimeMillis() - (LIFETIME_MS - timeLeft);
    }

    @Override
    public void update() {
        // 7 saniye dolunca yok ol
        if (System.currentTimeMillis() - birthTime >= LIFETIME_MS) {
            this.alive = false;
            System.out.println("ShadowClone yok oldu.");
        }
    }

    @Override
    public void takeDamage(int amount) {
        // Klon hasar almaz
    }

    // Hero'nun yönünün tersine hareket et
    public void moveOpposite(Direction heroDir, GameMap map, List<Entity> entities) {
        if (!alive) return;

        Direction opposite = flipDirection(heroDir);
        int newX = this.x;
        int newY = this.y;

        switch (opposite) {
            case UP:    newY -= 1; break;
            case DOWN:  newY += 1; break;
            case LEFT:  newX -= 1; break;
            case RIGHT: newX += 1; break;
        }

        if (canMoveTo(newX, newY, map, entities)) {
            this.x = newX;
            this.y = newY;
        }
    }

    private Direction flipDirection(Direction dir) {
        switch (dir) {
            case UP:    return Direction.DOWN;
            case DOWN:  return Direction.UP;
            case LEFT:  return Direction.RIGHT;
            case RIGHT: return Direction.LEFT;
            default:    return dir;
        }
    }

    private boolean canMoveTo(int nx, int ny, GameMap map, List<Entity> entities) {
        if (!map.isWalkable(nx, ny)) return false;
        for (Entity e : entities) {
            if (e == this || !e.isAlive()) continue;
            if (e.getX() == nx && e.getY() == ny) return false;
        }
        return true;
    }
}
