package domain.models.entity;

import java.util.Random;

public class Sorcerer extends Entity {
    private long lastTeleportTime; // Son ışınlanma zamanı (milisaniye)
    private final Random random = new Random();

    public Sorcerer(int x, int y) {
        super(x, y, 8);
        this.lastTeleportTime = System.currentTimeMillis();
    }

    /**
     * Bu metot GameLoop (Game Clock) tarafından her karede (frame) çağrılır.
     */
    public void update() {
        long currentTime = System.currentTimeMillis();

        // 7 saniye (7000 ms) geçti mi kontrol et
        if (currentTime - lastTeleportTime >= 7000) {
            attemptTeleport();
            lastTeleportTime = currentTime; // Zamanlayıcıyı sıfırla
        }
    }

    private void attemptTeleport() {
        // %50 ihtimal kontrolü
        if (random.nextBoolean()) {
            // Işınlanma mantığı (Yeni koordinatlar domain.logic tarafından doğrulanmalı)
            System.out.println("Sorcerer decided to teleport!");
            // teleport(newX, newY) metodu burada veya logic katmanında tetiklenir.
        } else {
            System.out.println("Sorcerer stayed put (50% chance failed).");
        }
    }
}