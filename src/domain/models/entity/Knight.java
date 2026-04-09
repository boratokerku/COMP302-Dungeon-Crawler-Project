package domain.models.entity;

import java.awt.Point;

public class Knight extends Entity {
    private String moveDirection = "HORIZONTAL"; // Knight'lar yatay veya dikey hareket eder

    public Knight(int x, int y) {
        super(x, y, 10); // Knight için örnek can, dokümanda belirtilmemişse 10 iyidir
    }

    // Knight'ın kendine has zekası 
    public void followHero(Hero hero) {
        int heroX = hero.getX();
        int heroY = hero.getY();

        if (this.x < heroX) {
            this.x++; 
        } else if (this.x > heroX) {
            this.x--; 
        } else if (this.y < heroY) {
            this.y++; 
        } else if (this.y > heroY) {
            this.y--;
        }
    }

    @Override
    public void update() {
        // Hero parametresi almadığı için update metodunda bir şey yapmıyoruz. 
        // Takip için followHero çağırılmalı.
    }

    @Override // Bu anotasyon hata varsa seni uyarır, kullanman iyidir
    public Point getPosition() {
        return new Point(this.x, this.y);
    }
}