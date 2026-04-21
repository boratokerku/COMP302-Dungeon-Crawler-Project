package domain.models.entity;

import java.awt.Point;
import java.util.Random;

public class Knight extends Entity {

    private final Random random = new Random();

    public Knight(int x, int y) {
        super(x, y, 20); // Design doc §2.5.1: "Knights start with 20HP"
    }

    /**
     * Knight AI ana metodu.
     *
     * Design doc §2.5.1:
     * "If the hero is more than 5 grid cells away (computed as Euclidean distance
     *  and rounded up to next integer) the knight can not see the hero and walks
     *  randomly. If the distance is 5 or less the knight moves towards the hero."
     *
     * @param hero     Oyuncu karakteri
     * @param map      Harita (yürünebilirlik kontrolü için)
     * @param entities Tüm varlıklar (çakışma kontrolü için)
     */
    public void followHero(Hero hero, domain.models.map.GameMap map, java.util.List<Entity> entities) {
        if (!this.isAlive()) return;

        // Euclidean mesafeyi hesapla, yukarıya yuvarla (design doc §2.5.1)
        double dx = this.x - hero.getX();
        double dy = this.y - hero.getY();
        int distance = (int) Math.ceil(Math.sqrt(dx * dx + dy * dy));

        if (distance > 5) {
            // ROAMING: Hero çok uzakta, Knight göremez → rastgele yürür
            System.out.println("Knight: Roaming");
            roam(map, entities);
        } else {
            // CHASING: Hero algılama mesafesinde → Hero'ya doğru hareket et
            System.out.println("Knight: Chasing Hero");
            chaseHero(hero, map, entities);
        }
    }

    /**
     * Hero'ya doğru bir adım atar (önce yatay ekseni kapatır, sonra dikey).
     */
    private void chaseHero(Hero hero, domain.models.map.GameMap map, java.util.List<Entity> entities) {
        int nextX = this.x;
        int nextY = this.y;

        if (this.x < hero.getX()) {
            nextX++;
        } else if (this.x > hero.getX()) {
            nextX--;
        } else if (this.y < hero.getY()) {
            nextY++;
        } else if (this.y > hero.getY()) {
            nextY--;
        }

        if (canMoveTo(nextX, nextY, map, entities)) {
            this.x = nextX;
            this.y = nextY;
        }
    }

    /**
     * Dört yönden (UP, DOWN, LEFT, RIGHT) rastgele birini seçer ve gidebiliyorsa gider.
     */
    private void roam(domain.models.map.GameMap map, java.util.List<Entity> entities) {
        // 4 yön: {deltaX, deltaY}
        int[][] directions = { {0, -1}, {0, 1}, {-1, 0}, {1, 0} };
        int[] dir = directions[random.nextInt(directions.length)];

        int nextX = this.x + dir[0];
        int nextY = this.y + dir[1];

        if (canMoveTo(nextX, nextY, map, entities)) {
            this.x = nextX;
            this.y = nextY;
        }
    }

    /**
     * Verilen pozisyon yürünebilir mi ve başka bir canlı varlık tarafından işgal edilmemiş mi?
     *
     * @return true ise hareket etmek güvenli
     */
    private boolean canMoveTo(int nextX, int nextY, domain.models.map.GameMap map, java.util.List<Entity> entities) {
        if (map == null || !map.isWalkable(nextX, nextY)) return false;
        if (entities != null) {
            for (Entity e : entities) {
                if (e != this && e.isAlive() && e.getX() == nextX && e.getY() == nextY) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void update() {
        // Takip mantığı followHero(hero, map, entities) ile tetiklenir.
    }

    @Override
    public Point getPosition() {
        return new Point(this.x, this.y);
    }
}