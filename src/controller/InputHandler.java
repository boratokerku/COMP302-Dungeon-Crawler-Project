package controller;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import domain.models.entity.Hero;
import domain.models.Direction;
import domain.models.AnimationState;

public class InputHandler implements KeyListener {
    private Hero hero;
    private domain.models.map.GameMap map;
    private java.util.List<domain.models.entity.Entity> entities;

    public InputHandler(Hero hero, domain.models.map.GameMap map, java.util.List<domain.models.entity.Entity> entities) {
        this.hero = hero;
        this.map = map;
        this.entities = entities;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        // Hareket ve Yön Mantığı
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
            hero.move(Direction.UP, map, entities);
            hero.setAnimationState(AnimationState.WALK_UP);
        } else if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
            hero.move(Direction.DOWN, map, entities);
            hero.setAnimationState(AnimationState.WALK_DOWN);
        } else if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
            hero.move(Direction.LEFT, map, entities);
            hero.setAnimationState(AnimationState.WALK_LEFT);
        } else if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
            hero.move(Direction.RIGHT, map, entities);
            hero.setAnimationState(AnimationState.WALK_RIGHT);
        }

        // Saldırı (Örn: Boşluk tuşu)
        else if (code == KeyEvent.VK_SPACE) {
            hero.setAnimationState(AnimationState.ATTACK);
            // hero.attack(target) mantığı buraya bağlanacak
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Tuş bırakıldığında karakteri IDLE (bekleme) durumuna alıyoruz
        int code = e.getKeyCode();
        if (isMovementKey(code)) {
            hero.setAnimationState(AnimationState.IDLE);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    private boolean isMovementKey(int code) {
        return code == KeyEvent.VK_W || code == KeyEvent.VK_S ||
                code == KeyEvent.VK_A || code == KeyEvent.VK_D ||
                code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN ||
                code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT;
    }
}