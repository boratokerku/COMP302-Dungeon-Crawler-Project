package domain.models.entity;

import java.awt.Point;
import java.util.Random;

public class Sorcerer extends Entity {
    private long lastTeleportTime; // Son ışınlanma zamanı (milisaniye)
    private final Random random = new Random();

    public Sorcerer(int x, int y) {
        super(x, y, 8);
        this.lastTeleportTime = System.currentTimeMillis();
    }

    /**
     * Hero'yu adım adım takip eder.
     * Ayrıca 7 saniyede bir %50 ihtimalle anında yanına ışınlanma özelliğini
     * tetikler.
     */
    public void followHero(Hero hero, domain.models.map.GameMap map, java.util.List<Entity> entities) {
        if (!this.isAlive())
            return;

        long currentTime = System.currentTimeMillis();

        if (currentTime - lastTeleportTime >= 7000) {
            boolean teleported = attemptTeleport(hero, map, entities);
            lastTeleportTime = currentTime;

            if (teleported) {
                return;
            }
        }

        int heroX = hero.getX();
        int heroY = hero.getY();

        int nextX = this.x;
        int nextY = this.y;

        if (this.x < heroX) {
            nextX++;
        } else if (this.x > heroX) {
            nextX--;
        } else if (this.y < heroY) {
            nextY++;
        } else if (this.y > heroY) {
            nextY--;
        }

        boolean occupied = false;
        if (entities != null) {
            for (Entity e : entities) {
                if (e != this && e.isAlive() && e.getX() == nextX && e.getY() == nextY) {
                    occupied = true;
                    break;
                }
            }
        }

        if (map != null && map.isWalkable(nextX, nextY) && !occupied) {
            this.x = nextX;
            this.y = nextY;
        }
    }

    private boolean attemptTeleport(Hero hero, domain.models.map.GameMap map, java.util.List<Entity> entities) {
        if (random.nextBoolean()) {
            boolean occupied = false;
            if (entities != null) {
                for (Entity e : entities) {
                    if (e != this && e.isAlive() && e.getX() == hero.getX() && e.getY() == hero.getY()) {
                        occupied = true;
                        break;
                    }
                }
            }

            if (map != null && map.isWalkable(hero.getX(), hero.getY()) && !occupied) {
                System.out.println("Sorcerer ışınlanma gücünü kullandı!");
                this.x = hero.getX();
                this.y = hero.getY();
                return true;
            }
        }
        System.out.println("Sorcerer güç topluyor (Işınlanma başarısız veya dolu/duvar).");
        return false;
    }

    @Override
    public void update() {
        // Parametresiz update şu an kullanılmıyor, yerine 'followHero(hero)'
        // çağrılacak.
    }

    @Override
    public Point getPosition() {
        return new Point(this.x, this.y);
    }
}