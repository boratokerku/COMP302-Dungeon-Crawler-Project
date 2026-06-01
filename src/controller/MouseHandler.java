package controller;

import domain.models.map.GameMap;
import domain.models.entity.Hero;
import domain.models.entity.GameObject;
import view.ActionMenu;

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.SwingUtilities;

public class MouseHandler extends MouseAdapter {

    private final Hero hero;
    private domain.models.map.GameMap gameMap;
    private final view.GameView gameView;
    private final ActionMenu actionMenu;

    private javax.swing.Timer[] logicTimerRef;
    private javax.swing.Timer[] renderTimerRef;
    private ui.SearchPopupDialog activeDialog = null;

    public MouseHandler(Hero hero, domain.models.map.GameMap gameMap, view.GameView gameView, ActionMenu actionMenu) {
        this.hero = hero;
        this.gameMap = gameMap;
        this.gameView = gameView;
        this.actionMenu = actionMenu;
    }

    public void setTimers(javax.swing.Timer[] logicTimer, javax.swing.Timer[] renderTimer) {
        this.logicTimerRef = logicTimer;
        this.renderTimerRef = renderTimer;
    }

    public void setGameMap(domain.models.map.GameMap map) {
        this.gameMap = map;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (actionMenu != null && actionMenu.isVisible()) {
            boolean inside = actionMenu.contains(e.getX(), e.getY());
            if (inside) {
                actionMenu.handleMouseClick(e.getX(), e.getY(), gameView);
                return;
            } else {
                actionMenu.hideMenu();
                gameView.repaint();
                return;
            }
        }

        int tileSize = gameView.getTileSize();
        int offsetX = gameView.getOffsetX();
        int offsetY = gameView.getOffsetY();

        if (tileSize <= 0)
            return;

        // Check if the pause button on the HUD was clicked
        if (gameView.isPauseButtonClicked(e.getX(), e.getY())) {
            gameView.triggerPauseMenu();
            return;
        }

        // Check if an item in the inventory was clicked
        GameObject invObj = gameView.getClickedInventoryItem(e.getX(), e.getY());
        if (invObj != null) {
            actionMenu.show(gameView, invObj, e.getX(), e.getY());
            return;
        }

        // 1. Ekrandaki tıkı Map offsetlerine göre grid koordinatına çevir
        int clickedGridX = (e.getX() - offsetX) / tileSize;
        int clickedGridY = (e.getY() - offsetY) / tileSize;

        // Bounds check
        if (clickedGridX < 0 || clickedGridX >= gameMap.getWidth() || clickedGridY < 0
                || clickedGridY >= gameMap.getHeight()) {
            actionMenu.hideMenu();
            return;
        }

        // 3. 3x3 alan kontrolü (fark her iki eksende de max 1 olmalı)
        boolean isAdjacent = hero.isAdjacentTo(clickedGridX, clickedGridY);

        if (!isAdjacent) {
            actionMenu.hideMenu(); // Uzaktaki tıklamalarda menüyü kapat
            return;
        }

        // 4. O konumda nesne var mı?
        GameObject obj = gameMap.getObjectAt(clickedGridX, clickedGridY);
        
        System.out.println("Clicked tile type: " + (obj != null ? obj.getClass().getName() : "null"));
        if (obj instanceof domain.models.tile.WallTile) {
            domain.models.tile.WallTile wt = (domain.models.tile.WallTile) obj;
            System.out.println("Wall decoration: " + wt.getDecoration());
            if (wt.getDecoration() != null) {
                System.out.println("Decoration class: " + wt.getDecoration().getClass().getName());
                System.out.println("Decoration image: " + wt.getDecoration().getImageName());
            }
        }

        if (obj instanceof domain.models.tile.WallTile) {
            domain.models.tile.WallTile wt = (domain.models.tile.WallTile) obj;
            GameObject deco = wt.getDecoration();
            if (deco != null) {
                if (deco instanceof domain.models.entity.SearchableObject) {
                    obj = deco;
                } else {
                    // Non-searchable item: only display its clean name and close menus
                    String cleanName = getCleanDecorationName(deco);
                    view.GameView.addFloatingText(wt.getX(), wt.getY(), cleanName, new java.awt.Color(255, 215, 0));
                    actionMenu.hideMenu();
                    gameView.repaint();
                    return;
                }
            } else {
                // Empty wall: just dismiss menus, do not show "Unknown Object" popup
                actionMenu.hideMenu();
                gameView.repaint();
                return;
            }
        }

        if (obj == null) {
            actionMenu.hideMenu();
            return;
        }

        if (obj instanceof domain.models.entity.Chest) {
            java.util.List<domain.logic.Action> actions = obj.getActions();
            if (actions != null && !actions.isEmpty()) {
                actions.get(0).execute(hero, obj);
                gameView.repaint();
            }
            actionMenu.hideMenu();
            return;
        }

        if (obj instanceof domain.models.entity.SearchableObject) {
            // Dismiss any active search dialog to prevent duplicates
            if (activeDialog != null) {
                activeDialog.dispose();
                activeDialog = null;
            }

            Window parentWindow = SwingUtilities.getWindowAncestor(gameView);
            Frame parentFrame = (parentWindow instanceof Frame) ? (Frame) parentWindow : null;

            // Calculate target screen location right next to the clicked object
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

            final GameObject targetObj = obj;
            activeDialog = new ui.SearchPopupDialog(parentFrame, obj.getName(), () -> {
                domain.logic.SearchAction sa = new domain.logic.SearchAction(null);
                sa.execute(hero, targetObj);
                gameView.repaint();
            });
            activeDialog.setLocation(targetX, targetY);
            activeDialog.setVisible(true);
            return;
        }

        if (obj == null || obj.getActions() == null || obj.getActions().isEmpty()) {
            actionMenu.hideMenu();
            gameView.repaint();
            return;
        }

        // We show the Action Menu inside GameView, passing e.getComponent() logic
        // actually handled by actionMenu parent,
        // but actionMenu is a child of GameView (we'll add it).
        actionMenu.show(gameView, obj, e.getX(), e.getY());
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int rotation = e.getWheelRotation();
        if (rotation != 0) {
            gameView.scrollHotbar(rotation);
        }
    }

    private String getCleanDecorationName(GameObject deco) {
        if (deco == null) return "Object";
        String name = deco.getName();
        if ("WallDecoration".equals(name) || "Decoration".equals(name) || "WallObject".equals(name) || name == null || name.isEmpty() || name.equals("Unknown Object")) {
            // Resolve from imageName
            String img = deco.getImageName();
            if (img != null) {
                String lower = img.toLowerCase();
                if (lower.contains("torch")) return "Wall Torch";
                if (lower.contains("chain")) return "Chain";
                if (lower.contains("moss")) return "Moss";
                if (lower.contains("crack")) return "Crack";
                if (lower.contains("cobweb")) return "Cobweb";
                if (lower.contains("red_flag")) return "Red Flag";
                if (lower.contains("green_flag")) return "Green Flag";
                if (lower.contains("blue_flag")) return "Blue Flag";
                if (lower.contains("acid_ooze")) return "Acid Ooze";
                if (lower.contains("blood_stain")) return "Blood Stain";
                if (lower.contains("skull")) return "Skull";
                if (lower.contains("statue")) return "Statue";
                if (lower.contains("missing_brick")) return "Missing Brick";
                if (lower.contains("loose_stone")) return "Loose Stone";
                if (lower.contains("wall_cavity")) return "Wall Cavity";
                if (lower.contains("wall_grill")) return "Wall Grill";
                if (lower.contains("gargoyle")) return "Gargoyle";
                if (lower.contains("pipe_hole")) return "Pipe Hole";
            }
        }
        return name;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (actionMenu != null && actionMenu.isVisible()) {
            actionMenu.handleMouseMove(e.getX(), e.getY());
            gameView.repaint();
        }
    }
}
