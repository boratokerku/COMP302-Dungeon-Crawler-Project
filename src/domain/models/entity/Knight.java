package domain.models.entity;

import java.awt.Point;

public class Knight extends Entity {
    private String moveDirection = "HORIZONTAL"; // Knight'lar yatay veya dikey hareket eder

    public Knight(int x, int y) {
        super(x, y, 10); // Knight için örnek can, dokümanda belirtilmemişse 10 iyidir
    }

    // Knight'ın kendine has zekası
    public void followHero(Hero hero, domain.models.map.GameMap map, java.util.List<Entity> entities) {
        if (!this.isAlive()) return;
        
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
                    // TODO: e == hero ise saldır! Şimdilik sadece duralım (iç içe geçmeyi engeller)
                    break;
                }
            }
        }

        if (map != null && map.isWalkable(nextX, nextY) && !occupied) {
            this.x = nextX;
            this.y = nextY;
        }
    }

    @Override
    public void update() {
        // Hero parametresi almadığı için update metodunda bir şey yapmıyoruz.
        // Takip için followHero çağırılmalı.
    }

    @Override
    public Point getPosition() {
        return new Point(this.x, this.y);
    }
}