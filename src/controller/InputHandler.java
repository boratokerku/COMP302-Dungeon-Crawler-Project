package controller;
import domain.models.GameObject;

import domain.models.AnimationState;
import domain.models.Direction;
import domain.models.entity.Hero;
import domain.models.entity.ShadowClone;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class InputHandler implements KeyListener {
    private Hero hero;
    private domain.models.map.GameMap map;
    private java.util.List<domain.models.entity.Entity> entities;
    private view.GameView gameView;
    private InteractionHandler interactionHandler;

    // Aktif shadow clone (null ise henüz çağrılmamış veya süresi dolmuş)
    private ShadowClone shadowClone;
    
    private domain.models.GameMode gameMode = domain.models.GameMode.ADVENTURE;
    private boolean inputEnabled = true;

    public void setGameMode(domain.models.GameMode mode) {
        this.gameMode = mode;
    }

    public InputHandler(Hero hero, domain.models.map.GameMap map,
            java.util.List<domain.models.entity.Entity> entities, view.GameView gameView) {
        this.hero = hero;
        this.map = map;
        this.entities = entities;
        this.gameView = gameView;
        this.interactionHandler = new InteractionHandler(hero, map, gameView);
    }

    public void setShadowClone(ShadowClone clone) {
        this.shadowClone = clone;
    }

    public void setGameMap(domain.models.map.GameMap map) {
        this.map = map;
        if (this.interactionHandler != null) {
            this.interactionHandler.setGameMap(map);
        }
    }

    public void disableInput() {
        this.inputEnabled = false;
    }

    public ShadowClone getShadowClone() {
        return shadowClone;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!inputEnabled) return;
        if (!hero.isAlive()) return;
        
        int code = e.getKeyCode();

        int hotbarSlot = mapNumberKeyToSlot(code);
        if (hotbarSlot != -1) {
            if (gameView != null) {
                gameView.setHotbarSlot(hotbarSlot);
            }
            if (hero != null && hero.getInventory() != null) {
                java.util.List<domain.models.GameObject> items = hero.getInventory().getItems();
                int itemIndex = hotbarSlot - 1;
                if (itemIndex >= 0 && itemIndex < items.size()) {
                    domain.models.GameObject item = items.get(itemIndex);
                    if (item != null) {
                        for (domain.logic.Action action : item.getActions()) {
                            String name = action.getName();
                            if (name.equals("Use") || name.equals("Equip") || name.equals("Wear") || name.equals("Read") || name.equals("Eat")) {
                                if (action.isAvailable(hero, item)) {
                                    action.execute(hero, item);
                                    System.out.println("[Hotkey " + hotbarSlot + "] Executed " + name + " on " + item.getName());
                                    if (gameView != null) {
                                        gameView.repaint();
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            return;
        }

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
        } else if (code == KeyEvent.VK_Q) {
            if (hero != null && hero.getInventory() != null && gameView != null) {
                int selectedSlot = gameView.getSelectedSlot();
                java.util.List<domain.models.GameObject> items = hero.getInventory().getItems();
                int itemIndex = selectedSlot - 1;
                if (itemIndex >= 0 && itemIndex < items.size()) {
                    domain.models.GameObject item = items.get(itemIndex);
                    if (item != null) {
                        for (domain.logic.Action action : item.getActions()) {
                            if (action.getName().equals("Discard")) {
                                if (action.isAvailable(hero, item)) {
                                    action.execute(hero, item);
                                    System.out.println("[Hotkey Q] Discarded item " + item.getName() + " from slot " + selectedSlot);
                                    gameView.repaint();
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        else if (code == KeyEvent.VK_SPACE) {
            if (hero.getEquippedWeapon() instanceof domain.models.item.MapItem) {
                domain.models.item.MapItem weapon = (domain.models.item.MapItem) hero.getEquippedWeapon();
                if (weapon.isRanged()) {
                    int cost = Math.max(0, weapon.getManaCost() - hero.getRingManaCostReduction());
                    if (hero.getMana() < cost) {
                        System.out.println("Büyü atmak için yeterli mana yok!");
                        view.GameView.addFloatingText(hero.getX(), hero.getY(), "No Mana!", java.awt.Color.CYAN);
                        return;
                    }
                    if (hero.getEnergy() < 10) {
                        System.out.println("Saldırı için yeterli enerji yok!");
                        return;
                    }
                    
                    // Consume stats
                    hero.setEnergy(Math.max(0, hero.getEnergy() - 10));
                    hero.setMana(Math.max(0, hero.getMana() - cost));
                    hero.setAnimationState(AnimationState.ATTACK);
                    
                    // Firing direction
                    double dx = 0.0, dy = 0.0;
                    switch (hero.getDirection()) {
                        case UP:    dy = -0.5; break;
                        case DOWN:  dy = 0.5;  break;
                        case LEFT:  dx = -0.5; break;
                        case RIGHT: dx = 0.5;  break;
                    }
                    
                    int dmg = hero.calculateDamage(hero.getWeaponAtk());
                    domain.models.entity.Projectile proj = new domain.models.entity.Projectile(
                        hero.getX(), hero.getY(),
                        hero.getX(), hero.getY(),
                        dx, dy,
                        dmg, hero,
                        weapon.getProjectileType()
                    );
                    
                    entities.add(proj);
                    util.helpers.SoundManager.playShoot();
                    System.out.println("Hero fired ranged projectile! Type: " + weapon.getProjectileType() + " | Damage: " + dmg);
                    if (gameView != null) gameView.repaint();
                    return;
                }
            }
            
            // Melee fallback
            if (hero.getEnergy() >= 10) {
                hero.setAnimationState(AnimationState.ATTACK);
                
                // Find target in current facing direction
                int targetX = hero.getX();
                int targetY = hero.getY();
                switch (hero.getDirection()) {
                    case UP:    targetY -= 1; break;
                    case DOWN:  targetY += 1; break;
                    case LEFT:  targetX -= 1; break;
                    case RIGHT: targetX += 1; break;
                }
                
                domain.models.entity.Entity targetEnemy = null;
                for (domain.models.entity.Entity ent : entities) {
                    if (ent != hero && ent.isAlive()) {
                        if (ent.occupiesTile(targetX, targetY)) {
                            targetEnemy = ent;
                            break;
                        }
                    }
                }
                
                if (targetEnemy != null) {
                    hero.attack(targetEnemy, map);
                } else {
                    domain.logic.event.GameEventBus.fireSound(domain.logic.event.SoundEvent.SoundType.SWING);
                }
            }
        } else if (code == KeyEvent.VK_I) {
            if (gameView != null) {
                gameView.toggleInventory();
                gameView.repaint();
            }
        } else if (code == KeyEvent.VK_E) {
            if (interactionHandler != null) {
                interactionHandler.handleInteract();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (!inputEnabled) return;
        if (!hero.isAlive()) return;
        
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

    private int mapNumberKeyToSlot(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_1:
                return 1;
            case KeyEvent.VK_2:
                return 2;
            case KeyEvent.VK_3:
                return 3;
            case KeyEvent.VK_4:
                return 4;
            case KeyEvent.VK_5:
                return 5;
            case KeyEvent.VK_6:
                return 6;
            case KeyEvent.VK_7:
                return 7;
            case KeyEvent.VK_8:
                return 8;
            default:
                return -1;
        }
    }

    // Klon varsa ve hayattaysa ters yönde hareket ettir
    private void moveCloneOpposite(Direction dir) {
        if (shadowClone != null && shadowClone.isAlive()) {
            shadowClone.moveOpposite(dir, map, entities);
        }
    }

    private boolean canKeyOpenChest(domain.models.item.KeyItem key, domain.models.staticObjects.Chest chest) {
        return true;
    }
}