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

    // Global timers (kalan milisaniye)
    public long enemySpawnTimeLeft = 9000;
    public long scrollSpawnTimeLeft = 15000;

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
        public int str;
        public String equippedWeaponType; // null veya "SwordItem"

        public HeroRecord() {}

        public HeroRecord(int x, int y, int hp, int mana, int energy, int str, String equippedWeaponType) {
            this.x = x;
            this.y = y;
            this.hp = hp;
            this.mana = mana;
            this.energy = energy;
            this.str = str;
            this.equippedWeaponType = equippedWeaponType;
        }
    }

    public static class ItemRecord {
        public String type; // "PotionItem", "Column", "Crate", "Chest", vb.
        public String name; // Column/Crate/Chest gibi isimli nesneler için (diğerleri için null)
        public int x, y;

        public ItemRecord() {}

        public ItemRecord(String type, int x, int y) {
            this.type = type;
            this.x = x;
            this.y = y;
        }

        public ItemRecord(String type, String name, int x, int y) {
            this.type = type;
            this.name = name;
            this.x = x;
            this.y = y;
        }
    }

    public static class EnemyRecord {
        public String type; // "Knight" veya "Sorcerer"
        public int x, y;
        public int hp;
        public boolean alive;
        public long timeLeft; // Sorcerer için kalan ışınlanma süresi (milisaniye)

        public EnemyRecord() {}

        public EnemyRecord(String type, int x, int y, int hp, boolean alive, long timeLeft) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.hp = hp;
            this.alive = alive;
            this.timeLeft = timeLeft;
        }
    }
}
