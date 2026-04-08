package domain.models.entity;

public class Projectile extends Entity {
    private int deltaX, deltaY; // Hareket yönü ve hızı
    private int damage;
    private Entity owner; // Bu mermiyi kim attı? (Sorcerer mı Hero mu?)

    public Projectile(int x, int y, int deltaX, int deltaY, int damage, Entity owner) {
        super(x, y, 1); // Mermilerin genellikle 1 canı olur, bir yere çarpınca yok olurlar
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.damage = damage;
        this.owner = owner;
    }

    /**
     * Merminin her frame'de ilerlemesini sağlar.
     */
    public void update() {
        this.x += deltaX;
        this.y += deltaY;

        // Burada Collision (çarpışma) kontrolü yapılmalı.
        // Eğer duvara veya bir düşmana çarparsa mermi yok edilmeli.
    }

    // Getters
    public int getDamage() {
        return damage;
    }

    public Entity getOwner() {
        return owner;
    }
}