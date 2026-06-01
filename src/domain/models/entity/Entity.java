package domain.models.entity;
import domain.models.GameObject;

import java.awt.Point;

public abstract class Entity extends GameObject {
    protected int hp;
    protected int maxHp;
    protected boolean alive = true;
    protected domain.models.Team team = domain.models.Team.NONE;
    protected long lastHitTime = 0;

    public Entity(int x, int y, int hp) {
        super(x, y, "entity", true); 
        this.hp = hp;
        this.maxHp = hp;
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

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }

    public long getLastHitTime() {
        return lastHitTime;
    }

    public void setLastHitTime(long lastHitTime) {
        this.lastHitTime = lastHitTime;
    }

    public boolean isAlive() {
        return alive;
    }

    public void takeDamage(int amount) {
        this.hp -= amount;
        this.lastHitTime = System.currentTimeMillis();
        if (this.hp <= 0) {
            this.hp = 0;
            this.alive = false;
        }

        if (this instanceof Hero) {
            util.helpers.SoundManager.playPlayerHit();
        } else if (this instanceof Knight || this instanceof Sorcerer || this instanceof FinalBoss) {
            util.helpers.SoundManager.playEnemyHit();
        }
    }

    public abstract void update();
}