import domain.models.entity.*;
import domain.models.map.GameMap;
import domain.models.Direction;

public class DemoRunner {

    public static void main(String[] args) {

        GameMap map = new GameMap(10, 10);
        Hero hero = new Hero(1, 1);
        Knight knight = new Knight(1, 1);
        Sorcerer sorcerer = new Sorcerer(8, 8);

        System.out.println("--- Dungeon Crawler Demo Başladı ---");

        hero.move(Direction.RIGHT);
        System.out.println("Hero hareket etti. Yeni pozisyon: " + hero.getPosition());

        knight.update();

        sorcerer.update();

        if (hero.getPosition().equals(knight.getPosition())) {
            System.out.println("Çatışma algılandı!");
            hero.attack(knight);
        }

        else {
            System.out.println("Hero ve Knight farklı yerdeler, savaş yok.");
        }

        System.out.println("--- Demo Sonlandı ---");
    }
}