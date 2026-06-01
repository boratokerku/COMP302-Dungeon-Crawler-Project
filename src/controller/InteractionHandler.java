package controller;
import domain.models.item.LevelKey;
import domain.models.item.KeyItem;

import domain.logic.Action;
import domain.logic.BreakAction;
import domain.logic.OpenAction;
import domain.logic.SearchAction;
import domain.logic.event.GameEventBus;
import domain.logic.event.SoundEvent;
import domain.models.entity.Hero;
import domain.models.GameObject;
import domain.models.map.GameMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles 'E' key object interactions: picking up items, opening doors/chests,
 * searching wall objects, and interacting with general world objects.
 *
 * <p>GRASP Controller: this class receives the interaction intent from InputHandler
 * (which simply delegates when 'E' is pressed) and coordinates the domain logic.
 * It no longer mixes UI code with domain logic at the same level as InputHandler.</p>
 *
 * <p>Low Coupling improvement: UI dialogs (JOptionPane) are still shown here because
 * the 'E' key interaction is inherently synchronous and requires a dialog result.
 * However, all domain feedback (floating text, sounds) goes through GameEventBus.</p>
 */
public class InteractionHandler {

    private final Hero hero;
    private GameMap map;
    private final view.GameView gameView;

    public InteractionHandler(Hero hero, GameMap map, view.GameView gameView) {
        this.hero = hero;
        this.map = map;
        this.gameView = gameView;
    }

    public void setGameMap(GameMap map) {
        this.map = map;
    }

    /**
     * Called when the player presses 'E'. Tries to interact with objects on the
     * hero's tile first, then sweeps the surrounding 3×3 area.
     */
    public void handleInteract() {
        if (hero == null || map == null) return;

        // 1. Check the hero's own tile first
        GameObject selfObj = map.getObjectAt(hero.getX(), hero.getY());
        if (selfObj != null) {
            List<Action> actions = selfObj.getActions();
            if (actions != null && !actions.isEmpty()) {
                List<Action> available = filterAvailable(hero, selfObj, actions);
                if (!available.isEmpty()) {
                    available.get(0).execute(hero, selfObj);
                    System.out.println(available.get(0).getName() + " executed on " + selfObj.getName() + " on player's tile");
                    if (gameView != null) gameView.repaint();
                    return;
                }
            }
        }

        // 2. Find the best interactable object in the 3×3 neighbourhood
        GameObject bestObj = null;
        int bestTypePriority = Integer.MAX_VALUE;
        int bestSpatialPriority = Integer.MAX_VALUE;
        int bestNx = -1, bestNy = -1;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = hero.getX() + dx;
                int ny = hero.getY() + dy;
                if (nx < 0 || nx >= map.getWidth() || ny < 0 || ny >= map.getHeight()) continue;

                GameObject obj = map.getObjectAt(nx, ny);

                // Check wall decorations
                if (obj instanceof domain.models.tile.WallTile) {
                    GameObject deco = ((domain.models.tile.WallTile) obj).getDecoration();
                    if (deco instanceof domain.models.staticObjects.SearchableObject) {
                        obj = deco;
                    }
                }

                if (obj == null) continue;
                if (obj instanceof domain.models.staticObjects.Chest) continue; // Chests handled separately

                List<Action> objActions = obj.getActions();
                boolean hasAvailable = false;
                boolean hasTake = false;
                if (objActions != null) {
                    for (Action action : objActions) {
                        if (action.isAvailable(hero, obj)) {
                            hasAvailable = true;
                            if (action.getName().equals("Take")) hasTake = true;
                        }
                    }
                }

                if (!hasAvailable
                        && !(obj instanceof domain.models.staticObjects.Door)
                        && !(obj instanceof domain.models.staticObjects.SearchableObject)) {
                    continue;
                }

                int typePriority = hasTake ? 1 : 2;
                int spatialPriority = computeSpatialPriority(dx, dy);

                if (typePriority < bestTypePriority
                        || (typePriority == bestTypePriority && spatialPriority < bestSpatialPriority)) {
                    bestTypePriority = typePriority;
                    bestSpatialPriority = spatialPriority;
                    bestObj = obj;
                    bestNx = nx;
                    bestNy = ny;
                }
            }
        }

        if (bestObj != null) {
            handleObjectInteraction(bestObj, bestNx, bestNy);
        }
    }

    // ── Interaction dispatch ───────────────────────────────────────────────────

    private void handleObjectInteraction(GameObject obj, int nx, int ny) {
        // SearchableObject — show mini search popup
        if (obj instanceof domain.models.staticObjects.SearchableObject) {
            showSearchPopup(obj);
            return;
        }

        // Door (locked or unlocked)
        if (obj instanceof domain.models.staticObjects.Door) {
            handleDoorInteraction((domain.models.staticObjects.Door) obj, nx, ny);
            return;
        }

        // Generic actions
        List<Action> actions = obj.getActions();
        if (actions == null || actions.isEmpty()) return;

        List<Action> available = filterAvailable(hero, obj, actions);
        if (available.isEmpty()) return;

        if (available.size() == 1
                && !(obj instanceof domain.models.staticObjects.Crate)
                && !(obj instanceof domain.models.staticObjects.Chest)) {
            available.get(0).execute(hero, obj);
            System.out.println(available.get(0).getName() + " executed on " + obj.getName() + " via E key");
            if (gameView != null) gameView.repaint();
            return;
        }

        // Multiple actions or Crate/Chest — show dialog
        showActionDialog(obj, nx, ny, available);
    }

    // ── Door handling ─────────────────────────────────────────────────────────

    private void handleDoorInteraction(domain.models.staticObjects.Door door, int nx, int ny) {
        if (door instanceof domain.models.staticObjects.LevelDoor) {
            if (door.isLocked()) {
                GameEventBus.fireFloatingText(nx, ny, "Locked", java.awt.Color.RED);
            }
            return;
        }

        if (door.isLocked()) {
            boolean hasKey = hasKeyForDoor(door, false);

            String[] options = {
                hasKey ? "Open (Uses Key)" : "Open (Need Key)",
                "Cancel"
            };

            int choice = javax.swing.JOptionPane.showOptionDialog(
                    gameView,
                    "What would you like to do with " + door.getName() + "?",
                    "Select Interaction",
                    javax.swing.JOptionPane.DEFAULT_OPTION,
                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]);

            if (choice == 0) {
                domain.models.item.KeyItem keyToUse = findKeyInInventory();
                if (keyToUse != null) {
                    if (keyToUse.isSingleUse()) {
                        hero.getInventory().removeItem(keyToUse);
                    }
                    door.unlock();
                    door.open();
                    GameEventBus.fireSound(SoundEvent.SoundType.UNLOCK);
                    GameEventBus.fireFloatingText(nx, ny, "UNLOCKED!", java.awt.Color.GREEN);
                    System.out.println("Unlocked door using key!");
                } else {
                    javax.swing.JOptionPane.showMessageDialog(gameView,
                            "This door is locked! You need a Key to open it.",
                            "Door Locked", javax.swing.JOptionPane.WARNING_MESSAGE);
                    GameEventBus.fireFloatingText(nx, ny, "Key Required", java.awt.Color.RED);
                }
                if (gameView != null) gameView.repaint();
            }
        }
    }

    // ── Searchable popup ──────────────────────────────────────────────────────

    private void showSearchPopup(GameObject obj) {
        java.awt.Window parentWindow = javax.swing.SwingUtilities.getWindowAncestor(gameView);
        java.awt.Frame parentFrame = (parentWindow instanceof java.awt.Frame)
                ? (java.awt.Frame) parentWindow : null;

        float scale = 0.35f;
        int width = Math.round(612 * scale);
        int height = Math.round(408 * scale);

        int objScreenX = gameView.getOffsetX() + obj.getX() * gameView.getTileSize();
        int objScreenY = gameView.getOffsetY() + obj.getY() * gameView.getTileSize();

        java.awt.Point screenLoc = gameView.getLocationOnScreen();
        int targetX = screenLoc.x + objScreenX + gameView.getTileSize() + 5;
        if (objScreenX + gameView.getTileSize() + 5 + width > gameView.getWidth()) {
            targetX = screenLoc.x + objScreenX - width - 5;
        }
        int targetY = screenLoc.y + objScreenY + (gameView.getTileSize() - height) / 2;

        final GameObject targetObj = obj;
        view.dialogs.SearchPopupDialog dialog = new view.dialogs.SearchPopupDialog(parentFrame, obj.getName(), () -> {
            SearchAction sa = new SearchAction(null);
            sa.execute(hero, targetObj);
            if (gameView != null) gameView.repaint();
        });
        dialog.setLocation(targetX, targetY);
        dialog.setVisible(true);
    }

    // ── Generic action dialog ─────────────────────────────────────────────────

    private void showActionDialog(GameObject obj, int nx, int ny, List<Action> available) {
        boolean hasKey = hasRegularKeyInInventory(obj);

        String[] options;
        if (obj instanceof domain.models.staticObjects.Crate) {
            options = new String[]{"Break (-10 Energy)", "Cancel"};
        } else if (obj instanceof domain.models.staticObjects.Chest) {
            domain.models.staticObjects.Chest chest = (domain.models.staticObjects.Chest) obj;
            if (chest.isLocked()) {
                options = new String[available.size() + 1];
                for (int i = 0; i < available.size(); i++) {
                    Action act = available.get(i);
                    if (act instanceof BreakAction) {
                        options[i] = "Break (-10 Energy)";
                    } else if (act instanceof OpenAction) {
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
                null, options, options[0]);

        if (obj instanceof domain.models.staticObjects.Crate) {
            if (choice == 0) {
                available.get(0).execute(hero, obj);
                if (gameView != null) gameView.repaint();
            }
        } else if (obj instanceof domain.models.staticObjects.Chest) {
            domain.models.staticObjects.Chest chest = (domain.models.staticObjects.Chest) obj;
            if (chest.isLocked()) {
                if (choice >= 0 && choice < available.size()) {
                    available.get(choice).execute(hero, obj);
                    if (gameView != null) gameView.repaint();
                }
            } else {
                if (choice == 0) {
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
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Action> filterAvailable(Hero hero, GameObject obj, List<Action> actions) {
        List<Action> available = new ArrayList<>();
        for (Action action : actions) {
            if (action.isAvailable(hero, obj)) {
                available.add(action);
            }
        }
        return available;
    }

    private int computeSpatialPriority(int dx, int dy) {
        domain.models.Direction facing = hero.getDirection();
        boolean isFront = false;
        if (facing == domain.models.Direction.UP && dx == 0 && dy == -1) isFront = true;
        else if (facing == domain.models.Direction.DOWN && dx == 0 && dy == 1) isFront = true;
        else if (facing == domain.models.Direction.LEFT && dx == -1 && dy == 0) isFront = true;
        else if (facing == domain.models.Direction.RIGHT && dx == 1 && dy == 0) isFront = true;

        if (isFront) return 1;
        if (dx == 0 || dy == 0) return 2;
        return 3;
    }

    private boolean hasKeyForDoor(domain.models.staticObjects.Door door, boolean isLevelDoor) {
        if (hero.getInventory() == null) return false;
        for (GameObject item : hero.getInventory().getItems()) {
            if (isLevelDoor && item instanceof domain.models.item.LevelKey) return true;
            if (!isLevelDoor && item instanceof domain.models.item.KeyItem) return true;
        }
        return false;
    }

    private domain.models.item.KeyItem findKeyInInventory() {
        if (hero.getInventory() == null) return null;
        for (GameObject item : hero.getInventory().getItems()) {
            if (item instanceof domain.models.item.KeyItem) {
                return (domain.models.item.KeyItem) item;
            }
        }
        return null;
    }

    private boolean hasRegularKeyInInventory(GameObject obj) {
        if (hero.getInventory() == null) return false;
        if (obj instanceof domain.models.staticObjects.Chest) {
            domain.models.staticObjects.Chest chest = (domain.models.staticObjects.Chest) obj;
            for (GameObject item : hero.getInventory().getItems()) {
                if (item instanceof domain.models.item.KeyItem) return true;
            }
            return false;
        }
        for (GameObject item : hero.getInventory().getItems()) {
            if (item instanceof domain.models.item.KeyItem) return true;
        }
        return false;
    }
}
