package domain.models;

import java.util.List;
import java.util.ArrayList;

/**
 * Oyunun tam anlık durumunu tutan veri sınıfı.
 * Gson bu sınıfı doğrudan JSON'a çevirir — hiç extra kod gerekmez.
 */
public class GameState {

    // Save metadata
    public String saveName;
    public String timestamp; // "2026-05-04 21:00"

    // Alt veri sınıfları
    public HeroRecord hero;
    public List<String> inventoryItems = new ArrayList<>(); // item tip isimleri
    public List<ItemRecord> mapItems = new ArrayList<>();   // haritadaki eşyalar
    public List<EnemyRecord> enemies = new ArrayList<>();   // düşmanlar

    // ---------------------------------------------------------------

    public static class HeroRecord {
        public int x, y;
        public int hp;
        public int mana;
        public int energy;

        public HeroRecord() {}

        public HeroRecord(int x, int y, int hp, int mana, int energy) {
            this.x = x;
            this.y = y;
            this.hp = hp;
            this.mana = mana;
            this.energy = energy;
        }
    }

    public static class ItemRecord {
        public String type; // "PotionItem", "SwordItem", "ShadowCloneScroll", "KeyItem"
        public int x, y;

        public ItemRecord() {}

        public ItemRecord(String type, int x, int y) {
            this.type = type;
            this.x = x;
            this.y = y;
        }
    }

    public static class EnemyRecord {
        public String type; // "Knight" veya "Sorcerer"
        public int x, y;
        public int hp;
        public boolean alive;

        public EnemyRecord() {}

        public EnemyRecord(String type, int x, int y, int hp, boolean alive) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.hp = hp;
            this.alive = alive;
        }
    }
}
