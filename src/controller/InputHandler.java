package controller;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import domain.models.entity.Hero;
import domain.models.entity.ShadowClone;
import domain.models.Direction;
import domain.models.AnimationState;

public class InputHandler implements KeyListener {
    private Hero hero;
    private domain.models.map.GameMap map;
    private java.util.List<domain.models.entity.Entity> entities;
    private view.GameView gameView;

    // Aktif shadow clone (null ise henüz çağrılmamış veya süresi dolmuş)
    private ShadowClone shadowClone;

    public InputHandler(Hero hero, domain.models.map.GameMap map,
            java.util.List<domain.models.entity.Entity> entities, view.GameView gameView) {
        this.hero = hero;
        this.map = map;
        this.entities = entities;
        this.gameView = gameView;
    }

    public void setShadowClone(ShadowClone clone) {
        this.shadowClone = clone;
    }

    public ShadowClone getShadowClone() {
        return shadowClone;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        // Hareket ve Yön Mantığı
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
            if (hero.move(Direction.UP, map, entities)) {
                hero.setAnimationState(AnimationState.WALK_UP);
            }
            moveCloneOpposite(Direction.UP); // Hero hareket edemese bile klon dener
        } else if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
            if (hero.move(Direction.DOWN, map, entities)) {
                hero.setAnimationState(AnimationState.WALK_DOWN);
            }
            moveCloneOpposite(Direction.DOWN);
        } else if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
            if (hero.move(Direction.LEFT, map, entities)) {
                hero.setAnimationState(AnimationState.WALK_LEFT);
            }
            moveCloneOpposite(Direction.LEFT);
        } else if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
            if (hero.move(Direction.RIGHT, map, entities)) {
                hero.setAnimationState(AnimationState.WALK_RIGHT);
            }
            moveCloneOpposite(Direction.RIGHT);
        }
        else if (code == KeyEvent.VK_SPACE) {
            if (hero.getEnergy() >= 10) {
                hero.setAnimationState(AnimationState.ATTACK);
            }
        } else if (code == KeyEvent.VK_I) {
            if (gameView != null) {
                gameView.toggleInventory();
                gameView.repaint();
            }
        } else if (code == KeyEvent.VK_E) {
            // Take action for adjacent items
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int nx = hero.getX() + dx;
                    int ny = hero.getY() + dy;
                    if (nx >= 0 && nx < map.getWidth() && ny >= 0 && ny < map.getHeight()) {
                        domain.models.entity.GameObject obj = map.getObjectAt(nx, ny);
                        if (obj instanceof domain.models.item.MapItem) {
                            for (domain.logic.Action action : obj.getActions()) {
                                if (action.getName().equals("Take") && action.isAvailable(hero, obj)) {
                                    action.execute(hero, obj);
                                    System.out.println("Take executed on " + obj.getName() + " via E key");
                                    if (gameView != null) gameView.repaint();
                                    return; // Sadece bir item al
                                }
                            }
                        }
                    }
                }
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

    // Klon varsa ve hayattaysa ters yönde hareket ettir
    private void moveCloneOpposite(Direction dir) {
        if (shadowClone != null && shadowClone.isAlive()) {
            shadowClone.moveOpposite(dir, map, entities);
        }
    }
}