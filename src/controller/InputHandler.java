package controller;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import domain.models.entity.Hero;
import domain.models.Direction;
import domain.models.AnimationState;

public class InputHandler implements KeyListener {
    private Hero hero;

    public InputHandler(Hero hero) {
        this.hero = hero;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        // Hareket ve Yön Mantığı
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
            hero.move(Direction.UP);
            hero.setAnimationState(AnimationState.WALK_UP);
        } else if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
            hero.move(Direction.DOWN);
            hero.setAnimationState(AnimationState.WALK_DOWN);
        } else if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
            hero.move(Direction.LEFT);
            hero.setAnimationState(AnimationState.WALK_LEFT);
        } else if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
            hero.move(Direction.RIGHT);
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