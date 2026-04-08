import domain.models.entity.Hero;
import domain.models.entity.Knight;
import domain.models.entity.Sorcerer;
import domain.models.map.GameMap;
import domain.logic.GameEngine; // Oyun döngüsü için
// İsteğe bağlı: Hepsini tek seferde almak için import domain.models.entity.*;
import domain.models.Direction;

public void runDemo() {
    // 1. Haritayı ve Karakterleri Yükle
    GameMap map = new GameMap(10, 10);
    Hero hero = new Hero(1, 1);
    Knight knight = new Knight(5, 5);
    Sorcerer sorcerer = new Sorcerer(8, 8);

    // 2. Hareket Simülasyonu
    hero.move(Direction.RIGHT); // (2, 1) konumuna geçer

    // 3. Knight Devriyesi (Patrol)
    knight.update(); // 1 birim sola kayar

    // 4. Sorcerer Işınlanması
    sorcerer.update(); // 7 saniye geçince %50 ihtimalle teleport olur

    // 5. Çatışma (Battle)
    if (hero.getPosition() == knight.getPosition()) {
        hero.attack(knight);
    }
}