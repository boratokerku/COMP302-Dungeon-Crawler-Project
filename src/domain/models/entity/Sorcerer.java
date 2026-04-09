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
     * Ayrıca 7 saniyede bir %50 ihtimalle anında yanına ışınlanma özelliğini tetikler.
     */
    public void followHero(Hero hero) {
        long currentTime = System.currentTimeMillis();

        // 1. Önce Teleport kontrolü (7 saniyede bir)
        if (currentTime - lastTeleportTime >= 7000) {
            boolean teleported = attemptTeleport(hero);
            lastTeleportTime = currentTime; // Zamanlayıcıyı sıfırla
            
            if (teleported) {
                return; // Işınlandıysa bu el adım atmasına gerek yok, işlemi bitir
            }
        }

        // 2. Işınlanmadıysa Normal Adım Adım Takip (Knight ile aynı mantık)
        int heroX = hero.getX();
        int heroY = hero.getY();

        if (this.x < heroX) {
            this.x++; // Hero sağda → sağa git
        } else if (this.x > heroX) {
            this.x--; // Hero solda → sola git
        } else if (this.y < heroY) {
            this.y++; // Hero aşağıda → aşağı git
        } else if (this.y > heroY) {
            this.y--; // Hero yukarıda → yukarı git
        }
    }

    private boolean attemptTeleport(Hero hero) {
        // %50 ihtimal kontrolü
        if (random.nextBoolean()) {
            System.out.println("Sorcerer ışınlanma gücünü kullandı!");
            this.x = hero.getX();
            this.y = hero.getY();
            return true; // Işınlanma Başarılı
        } else {
            System.out.println("Sorcerer güç topluyor (Işınlanma başarısız).");
            return false; // Işınlanma Başarısız
        }
    }

    @Override
    public void update() {
        // Parametresiz update şu an kullanılmıyor, yerine 'followHero(hero)' çağrılacak.
    }

    @Override
    public Point getPosition() {
        return new Point(this.x, this.y);
    }
}