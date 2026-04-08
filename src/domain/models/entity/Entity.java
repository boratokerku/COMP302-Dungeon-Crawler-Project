package domain.models.entity;

public abstract class Entity {
    protected int x, y;
    protected int hp;
    protected boolean alive = true;

    public Entity(int x, int y, int hp) {
        this.x = x;
        this.y = y;
        this.hp = hp;
    }

    // Ortak hareket metodu
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // Getters
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getHp() {
        return hp;
    }

    public boolean isAlive() {
        return alive;
    }

    public void takeDamage(int amount) {
        this.hp -= amount;
        if (this.hp <= 0) {
            this.hp = 0;
            this.alive = false;
        }
    }

    public abstract void update();
}