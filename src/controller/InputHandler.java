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

    public void setGameMap(domain.models.map.GameMap map) {
        this.map = map;
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
                java.util.List<domain.models.entity.GameObject> items = hero.getInventory().getItems();
                int itemIndex = hotbarSlot - 1;
                if (itemIndex >= 0 && itemIndex < items.size()) {
                    domain.models.entity.GameObject item = items.get(itemIndex);
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
                java.util.List<domain.models.entity.GameObject> items = hero.getInventory().getItems();
                int itemIndex = selectedSlot - 1;
                if (itemIndex >= 0 && itemIndex < items.size()) {
                    domain.models.entity.GameObject item = items.get(itemIndex);
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
                    util.helpers.SoundManager.playSwing();
                }
            }
        } else if (code == KeyEvent.VK_I) {
            if (gameView != null) {
                gameView.toggleInventory();
                gameView.repaint();
            }
        } else if (code == KeyEvent.VK_E) {
            // Önce kendi bulunduğumuz hücreyle etkileşime girmeyi dene
            domain.models.entity.GameObject selfObj = map.getObjectAt(hero.getX(), hero.getY());
            if (selfObj != null) {
                java.util.List<domain.logic.Action> actions = selfObj.getActions();
                if (actions != null && !actions.isEmpty()) {
                    java.util.List<domain.logic.Action> available = new java.util.ArrayList<>();
                    for (domain.logic.Action action : actions) {
                        if (action.isAvailable(hero, selfObj)) {
                            available.add(action);
                        }
                    }
                    if (!available.isEmpty()) {
                        available.get(0).execute(hero, selfObj);
                        System.out.println(available.get(0).getName() + " executed on " + selfObj.getName() + " on player's tile");
                        if (gameView != null) gameView.repaint();
                        return;
                    }
                }
            }

            // Çevredeki tüm nesnelerle (3x3 çevre karesi) etkileşime gir
            domain.models.entity.GameObject bestObj = null;
            int bestTypePriority = Integer.MAX_VALUE;
            int bestSpatialPriority = Integer.MAX_VALUE;
            int bestNx = -1;
            int bestNy = -1;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue; // Kendini pas geç
                    int nx = hero.getX() + dx;
                    int ny = hero.getY() + dy;
                    if (nx >= 0 && nx < map.getWidth() && ny >= 0 && ny < map.getHeight()) {
                        domain.models.entity.GameObject obj = map.getObjectAt(nx, ny);
                        if (obj instanceof domain.models.tile.WallTile) {
                            domain.models.entity.GameObject deco = ((domain.models.tile.WallTile) obj).getDecoration();
                            if (deco instanceof domain.models.entity.SearchableObject) {
                                obj = deco;
                            }
                        }
                        if (obj != null) {
                            if (obj instanceof domain.models.entity.Chest) {
                                continue;
                            }
                            
                            java.util.List<domain.logic.Action> objActions = obj.getActions();
                            boolean hasAvailable = false;
                            boolean hasTake = false;
                            if (objActions != null) {
                                for (domain.logic.Action action : objActions) {
                                    if (action.isAvailable(hero, obj)) {
                                        hasAvailable = true;
                                        if (action.getName().equals("Take")) {
                                            hasTake = true;
                                        }
                                    }
                                }
                            }
                            
                            if (!hasAvailable && !(obj instanceof domain.models.staticObjects.Door) && !(obj instanceof domain.models.entity.SearchableObject)) {
                                continue;
                            }

                            int typePriority = hasTake ? 1 : 2;
                            int spatialPriority = 3; // Diagonal default
                            domain.models.Direction facing = hero.getDirection();
                            boolean isFront = false;
                            if (facing == domain.models.Direction.UP && dx == 0 && dy == -1) isFront = true;
                            else if (facing == domain.models.Direction.DOWN && dx == 0 && dy == 1) isFront = true;
                            else if (facing == domain.models.Direction.LEFT && dx == -1 && dy == 0) isFront = true;
                            else if (facing == domain.models.Direction.RIGHT && dx == 1 && dy == 0) isFront = true;

                            if (isFront) {
                                spatialPriority = 1;
                            } else if (dx == 0 || dy == 0) {
                                spatialPriority = 2; // Orthogonal
                            }

                            if (typePriority < bestTypePriority || (typePriority == bestTypePriority && spatialPriority < bestSpatialPriority)) {
                                bestTypePriority = typePriority;
                                bestSpatialPriority = spatialPriority;
                                bestObj = obj;
                                bestNx = nx;
                                bestNy = ny;
                            }
                        }
                    }
                }
            }

            if (bestObj != null) {
                int nx = bestNx;
                int ny = bestNy;
                domain.models.entity.GameObject obj = bestObj;
                            if (obj instanceof domain.models.entity.SearchableObject) {
                                java.awt.Window parentWindow = javax.swing.SwingUtilities.getWindowAncestor(gameView);
                                java.awt.Frame parentFrame = (parentWindow instanceof java.awt.Frame) ? (java.awt.Frame) parentWindow : null;

                                float scale = 0.35f;
                                int width = Math.round(612 * scale);
                                int height = Math.round(408 * scale);

                                int objScreenX = gameView.getOffsetX() + obj.getX() * gameView.getTileSize();
                                int objScreenY = gameView.getOffsetY() + obj.getY() * gameView.getTileSize();

                                java.awt.Point screenLoc = gameView.getLocationOnScreen();
                                int targetX = screenLoc.x + objScreenX + gameView.getTileSize() + 5;
                                
                                // If it exceeds the right bounds of the game view, place it to the left of the item
                                if (objScreenX + gameView.getTileSize() + 5 + width > gameView.getWidth()) {
                                    targetX = screenLoc.x + objScreenX - width - 5;
                                }
                                int targetY = screenLoc.y + objScreenY + (gameView.getTileSize() - height) / 2;

                                final domain.models.entity.GameObject targetObj = obj;
                                ui.SearchPopupDialog dialog = new ui.SearchPopupDialog(parentFrame, obj.getName(), () -> {
                                    domain.logic.SearchAction sa = new domain.logic.SearchAction(null);
                                    sa.execute(hero, targetObj);
                                    gameView.repaint();
                                });
                                dialog.setLocation(targetX, targetY);
                                dialog.setVisible(true);
                                return;
                            }

                            if (obj instanceof domain.models.staticObjects.Door) {
                                domain.models.staticObjects.Door door = (domain.models.staticObjects.Door) obj;
                                if (door.isLocked()) {
                                    boolean isLevelDoor = (door instanceof domain.models.staticObjects.LevelDoor);
                                    boolean hasKey = false;
                                    if (isLevelDoor) {
                                        for (domain.models.entity.GameObject item : hero.getInventory().getItems()) {
                                            if (item instanceof domain.models.staticObjects.LevelKey) {
                                                hasKey = true;
                                                break;
                                            }
                                        }
                                    } else {
                                        for (domain.models.entity.GameObject item : hero.getInventory().getItems()) {
                                            if (item instanceof domain.models.staticObjects.KeyItem) {
                                                hasKey = true;
                                                break;
                                            }
                                        }
                                    }

                                    String keyTypeStr = isLevelDoor ? "Skull Key" : "Key";
                                    String[] options = new String[]{hasKey ? "Open (Uses " + keyTypeStr + ")" : "Open (Need " + keyTypeStr + ")", "Cancel"};

                                    int choice = javax.swing.JOptionPane.showOptionDialog(
                                            gameView,
                                            "What would you like to do with " + door.getName() + "?",
                                            "Select Interaction",
                                            javax.swing.JOptionPane.DEFAULT_OPTION,
                                            javax.swing.JOptionPane.QUESTION_MESSAGE,
                                            null,
                                            options,
                                            options[0]
                                    );

                                    if (choice == 0) {
                                        if (isLevelDoor) {
                                            domain.models.staticObjects.LevelDoor levelDoor = (domain.models.staticObjects.LevelDoor) door;
                                            boolean success = levelDoor.tryUnlockWithKey(hero);
                                            if (success) {
                                                util.helpers.SoundManager.playUnlock();
                                                view.GameView.addFloatingText(nx, ny, "UNLOCKED!", java.awt.Color.GREEN);
                                            } else {
                                                javax.swing.JOptionPane.showMessageDialog(
                                                        gameView,
                                                        "This door is locked! You need a Skull Key to open it.",
                                                        "Door Locked",
                                                        javax.swing.JOptionPane.WARNING_MESSAGE
                                                );
                                                view.GameView.addFloatingText(nx, ny, "Skull Key Required", java.awt.Color.RED);
                                            }
                                        } else {
                                            // Normal door
                                            domain.models.staticObjects.KeyItem keyToUse = null;
                                            for (domain.models.entity.GameObject item : hero.getInventory().getItems()) {
                                                if (item instanceof domain.models.staticObjects.KeyItem) {
                                                    keyToUse = (domain.models.staticObjects.KeyItem) item;
                                                    break;
                                                }
                                            }

                                            if (keyToUse != null) {
                                                if (keyToUse.isSingleUse()) {
                                                    hero.getInventory().removeItem(keyToUse);
                                                }
                                                door.unlock();
                                                door.open();
                                                util.helpers.SoundManager.playUnlock();
                                                System.out.println("Unlocked door using key!");
                                                view.GameView.addFloatingText(nx, ny, "UNLOCKED!", java.awt.Color.GREEN);
                                            } else {
                                                javax.swing.JOptionPane.showMessageDialog(
                                                        gameView,
                                                        "This door is locked! You need a Key to open it.",
                                                        "Door Locked",
                                                        javax.swing.JOptionPane.WARNING_MESSAGE
                                                );
                                                view.GameView.addFloatingText(nx, ny, "Key Required", java.awt.Color.RED);
                                            }
                                        }
                                        if (gameView != null) gameView.repaint();
                                    }
                                    return;
                                } else {
                                    // Door is unlocked (open). If it is a LevelDoor, add "New Level" interaction choice
                                    if (door instanceof domain.models.staticObjects.LevelDoor) {
                                        String[] options = new String[]{"New Level", "Cancel"};
                                        int choice = javax.swing.JOptionPane.showOptionDialog(
                                                gameView,
                                                "What would you like to do with " + door.getName() + "?",
                                                "Select Interaction",
                                                javax.swing.JOptionPane.DEFAULT_OPTION,
                                                javax.swing.JOptionPane.QUESTION_MESSAGE,
                                                null,
                                                options,
                                                options[0]
                                        );
                                        if (choice == 0) {
                                            domain.models.staticObjects.LevelDoor.triggerOpenTransition();
                                        }
                                        return;
                                    }
                                }
                            }
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
                                     if (obj instanceof domain.models.entity.Chest) {
                                         domain.models.entity.Chest chest = (domain.models.entity.Chest) obj;
                                         for (domain.models.entity.GameObject item : hero.getInventory().getItems()) {
                                             if (item instanceof domain.models.staticObjects.KeyItem) {
                                                 if (canKeyOpenChest((domain.models.staticObjects.KeyItem) item, chest)) {
                                                     hasKey = true;
                                                     break;
                                                 }
                                             }
                                         }
                                     } else {
                                         for (domain.models.entity.GameObject item : hero.getInventory().getItems()) {
                                             if (item instanceof domain.models.staticObjects.KeyItem) {
                                                 hasKey = true;
                                                 break;
                                             }
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

    private boolean canKeyOpenChest(domain.models.staticObjects.KeyItem key, domain.models.entity.Chest chest) {
        return true;
    }
}