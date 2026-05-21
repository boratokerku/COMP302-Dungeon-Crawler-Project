package domain.models.entity;

import java.awt.Point;

public abstract class Entity extends GameObject {
    protected int hp;
    protected boolean alive = true;
    protected domain.models.Team team = domain.models.Team.NONE;

    public Entity(int x, int y, int hp) {
        super(x, y, "entity", true); 
        this.hp = hp;
    }

    public domain.models.Team getTeam() {
        return team;
    }

    public void setTeam(domain.models.Team team) {
        this.team = team;
    }


    public Point getPosition() {
        return new Point(this.x, this.y);
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

    public void setHp(int hp) {
        this.hp = Math.max(0, hp);
        this.alive = this.hp > 0;
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