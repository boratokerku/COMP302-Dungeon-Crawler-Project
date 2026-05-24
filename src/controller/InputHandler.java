package controller;

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
    }

    public void setShadowClone(ShadowClone clone) {
        this.shadowClone = clone;
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
        }
        else if (code == KeyEvent.VK_SPACE) {
            if (hero.getEquippedWeapon() instanceof domain.models.item.MapItem) {
                domain.models.item.MapItem weapon = (domain.models.item.MapItem) hero.getEquippedWeapon();
                if (weapon.isRanged()) {
                    if (hero.getMana() < weapon.getManaCost()) {
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
                    hero.setMana(Math.max(0, hero.getMana() - weapon.getManaCost()));
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
                    System.out.println("Hero fired ranged projectile! Type: " + weapon.getProjectileType() + " | Damage: " + dmg);
                    if (gameView != null) gameView.repaint();
                    return;
                }
            }
            
            // Melee fallback
            if (hero.getEnergy() >= 10) {
                hero.setAnimationState(AnimationState.ATTACK);
            }
        } else if (code == KeyEvent.VK_I) {
            if (gameView != null) {
                gameView.toggleInventory();
                gameView.repaint();
            }
        } else if (code == KeyEvent.VK_E) {
            // Çevredeki tüm nesnelerle (3x3 çevre karesi) etkileşime gir
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue; // Kendini pas geç
                    int nx = hero.getX() + dx;
                    int ny = hero.getY() + dy;
                    if (nx >= 0 && nx < map.getWidth() && ny >= 0 && ny < map.getHeight()) {
                        domain.models.entity.GameObject obj = map.getObjectAt(nx, ny);
                        if (obj != null) {
                            java.util.List<domain.logic.Action> actions = obj.getActions();
                            if (actions != null && !actions.isEmpty()) {
                                // Kullanılabilir eylemleri filtrele
                                java.util.List<domain.logic.Action> available = new java.util.ArrayList<>();
                                for (domain.logic.Action action : actions) {
                                    if (action.isAvailable(hero, obj)) {
                                        available.add(action);
                                    }
                                }
                                
                                if (available.size() == 1 && !(obj instanceof domain.models.entity.Crate) && !(obj instanceof domain.models.entity.Chest)) {
                                    available.get(0).execute(hero, obj);
                                    System.out.println(available.get(0).getName() + " executed on " + obj.getName() + " via E key");
                                    if (gameView != null) gameView.repaint();
                                    return; // Bir etkileşim yetti, döngüden çık
                                } else if (available.size() >= 1) {
                                    // Oyuncuya butonlu dialog penceresi sun (maliyetler dahil)
                                    boolean hasKey = false;
                                    for (domain.models.entity.GameObject item : hero.getInventory().getItems()) {
                                        if (item instanceof domain.models.staticObjects.KeyItem) {
                                            hasKey = true;
                                            break;
                                        }
                                    }
                                    
                                    String[] options;
                                    if (obj instanceof domain.models.entity.Crate) {
                                        options = new String[]{"Break (-10 Energy)", "Cancel"};
                                    } else if (obj instanceof domain.models.entity.Chest) {
                                        domain.models.entity.Chest chest = (domain.models.entity.Chest) obj;
                                        if (chest.isLocked()) {
                                            options = new String[available.size() + 1];
                                            for (int i = 0; i < available.size(); i++) {
                                                domain.logic.Action act = available.get(i);
                                                if (act instanceof domain.logic.BreakAction) {
                                                    options[i] = "Break (-10 Energy)";
                                                } else if (act instanceof domain.logic.OpenAction) {
                                                    options[i] = hasKey ? "Open (Uses Key)" : "Open (Need Key)";
                                                } else {
                                                    options[i] = act.getName();
                                                }
                                            }
                                            options[available.size()] = "Cancel";
                                        } else {
                                            options = new String[]{"Open (Unlocked)", "Cancel"};
                                        }
                                    } else {
                                        options = new String[available.size()];
                                        for (int i = 0; i < available.size(); i++) {
                                            options[i] = available.get(i).getName();
                                        }
                                    }
                                    
                                    int choice = javax.swing.JOptionPane.showOptionDialog(
                                            gameView,
                                            "What would you like to do with " + obj.getName() + "?",
                                            "Select Interaction",
                                            javax.swing.JOptionPane.DEFAULT_OPTION,
                                            javax.swing.JOptionPane.QUESTION_MESSAGE,
                                            null,
                                            options,
                                            options[0]
                                    );
                                    
                                    if (obj instanceof domain.models.entity.Crate) {
                                        if (choice == 0) { // Break (-10 Energy) selected
                                            available.get(0).execute(hero, obj);
                                            if (gameView != null) gameView.repaint();
                                        }
                                    } else if (obj instanceof domain.models.entity.Chest) {
                                        domain.models.entity.Chest chest = (domain.models.entity.Chest) obj;
                                        if (chest.isLocked()) {
                                            if (choice >= 0 && choice < available.size()) {
                                                available.get(choice).execute(hero, obj);
                                                if (gameView != null) gameView.repaint();
                                            }
                                        } else {
                                            if (choice == 0) { // Open (Unlocked) selected
                                                available.get(0).execute(hero, obj);
                                                if (gameView != null) gameView.repaint();
                                            }
                                        }
                                    } else {
                                        if (choice >= 0 && choice < available.size()) {
                                            available.get(choice).execute(hero, obj);
                                            System.out.println(available.get(choice).getName() + " executed on " + obj.getName() + " via E dialog choice");
                                            if (gameView != null) gameView.repaint();
                                        }
                                    }
                                    return; // Etkileşim tamamlandı, çık
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
}