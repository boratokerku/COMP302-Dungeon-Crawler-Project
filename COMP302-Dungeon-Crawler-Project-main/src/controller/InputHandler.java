package controller;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import domain.models.entity.Hero;
import domain.models.Direction;
import domain.models.AnimationState;
import view.GameView; // Needed so we can call toggleInventoryMenu() on key press

public class InputHandler implements KeyListener {
    private Hero hero;
    private domain.models.map.GameMap map;
    private java.util.List<domain.models.entity.Entity> entities;
    // Reference to the view so we can open/close UI overlays (e.g. Inventory)
    private GameView gameView;

    public InputHandler(Hero hero, domain.models.map.GameMap map,
            java.util.List<domain.models.entity.Entity> entities) {
        this.hero = hero;
        this.map = map;
        this.entities = entities;
    }

    /**
     * Links this handler to the GameView so that key-driven UI actions
     * (like opening the inventory) can trigger repaints on the view.
     */
    public void setGameView(GameView view) {
        this.gameView = view;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        // Hareket ve Yön Mantığı
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
            if (hero.move(Direction.UP, map, entities)) {
                hero.setAnimationState(AnimationState.WALK_UP);
            }
        } else if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
            if (hero.move(Direction.DOWN, map, entities)) {
                hero.setAnimationState(AnimationState.WALK_DOWN);
            }
        } else if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
            if (hero.move(Direction.LEFT, map, entities)) {
                hero.setAnimationState(AnimationState.WALK_LEFT);
            }
        } else if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
            if (hero.move(Direction.RIGHT, map, entities)) {
                hero.setAnimationState(AnimationState.WALK_RIGHT);
            }
        }

        else if (code == KeyEvent.VK_SPACE) {
            if (hero.getEnergy() >= 10) {
                hero.setAnimationState(AnimationState.ATTACK);
            }
        }

        // E key — toggles the pixel-art Inventory / Action Menu overlay.
        // Pressing again while open will close it (toggle behaviour).
        else if (code == KeyEvent.VK_E) {
            if (gameView != null) {
                gameView.toggleInventoryMenu();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
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