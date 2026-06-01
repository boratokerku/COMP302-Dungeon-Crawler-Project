package ui.design;

import domain.models.entity.*;
import domain.models.map.GameMap;
import domain.models.tile.FloorTile;
import domain.models.tile.WallTile;
import ui.DesignModeView;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.SwingUtilities;
import domain.models.staticObjects.WallObject;
import domain.models.entity.SearchableObject;

public class DesignModeInputHandler extends MouseAdapter {
    private final DesignModeView view;
    private final GameMap map;
    private final PaletteManager paletteManager;

    private boolean isDragging = false;
    private int hoverTileX = -1;
    private int hoverTileY = -1;

    public DesignModeInputHandler(DesignModeView view, GameMap map, PaletteManager paletteManager) {
        this.view = view;
        this.map = map;
        this.paletteManager = paletteManager;
    }

    public int getHoverTileX() { return hoverTileX; }
    public int getHoverTileY() { return hoverTileY; }

    @Override
    public void mousePressed(MouseEvent e) {
        isDragging = false;
        int mx = e.getX();
        int my = e.getY();
        
        view.setLastMouseX(mx);
        view.setLastMouseY(my);

        // Call view to handle UI clicks (action buttons, palette selection)
        if (view.handleUiClick(mx, my)) {
            view.repaint();
            return;
        }

        // If not a UI click, it must be a map click
        handleMapClick(e);
        view.repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        isDragging = true;
        int mx = e.getX();
        int my = e.getY();
        
        view.setLastMouseX(mx);
        view.setLastMouseY(my);

        if (view.isInsideLeftPanel(mx)) {
            // Palette scrolling
            view.scrollLeftPanel(e.getY());
            view.repaint();
            return;
        }

        updateHover(mx, my);
        placeOrErase(e);
        view.repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int mx = e.getX();
        int my = e.getY();
        
        view.setLastMouseX(mx);
        view.setLastMouseY(my);

        boolean insideLeft = view.isInsideLeftPanel(mx);
        if (insideLeft) {
            hoverTileX = -1;
            hoverTileY = -1;
            view.updatePaletteHover(mx, my);
        } else {
            view.clearPaletteHover();
            updateHover(mx, my);
        }
        view.repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (view.isInsideLeftPanel(e.getX())) {
            view.scrollLeftPanel(e.getWheelRotation() * -30);
            view.repaint();
        }
    }

    private void handleMapClick(MouseEvent e) {
        updateHover(e.getX(), e.getY());
        if (hoverTileX >= 0 && hoverTileX < map.getWidth() &&
            hoverTileY >= 0 && hoverTileY < map.getHeight()) {
            placeOrErase(e);
        }
    }

    private void updateHover(int mx, int my) {
        int[] tilePos = view.screenToTile(mx, my);
        if (tilePos != null) {
            hoverTileX = tilePos[0];
            hoverTileY = tilePos[1];
        } else {
            hoverTileX = -1;
            hoverTileY = -1;
        }
    }

    private void placeOrErase(MouseEvent e) {
        if (hoverTileX < 0 || hoverTileX >= map.getWidth() ||
            hoverTileY < 0 || hoverTileY >= map.getHeight()) {
            return;
        }

        boolean isRightClick = SwingUtilities.isRightMouseButton(e);
        if (isRightClick) {
            eraseAt(hoverTileX, hoverTileY);
            return;
        }

        PaletteItem selected = paletteManager.getSelectedPaletteItem();
        if (selected == null || selected.factory == null) return;

        GameObject obj = selected.factory.apply(hoverTileX, hoverTileY);
        if (obj == null) return;

        GameObject existing = map.getObjectAt(hoverTileX, hoverTileY);
        if (existing instanceof domain.models.staticObjects.LevelDoor) {
            return; // Cannot overwrite Level Door
        }

        boolean replacingSameCategory = false;
        if (view.isItem(obj)) {
            if (view.isItem(existing)) {
                replacingSameCategory = true;
            }
            if (!replacingSameCategory && view.countItems() >= view.MAX_ITEMS) {
                if (e.getID() != MouseEvent.MOUSE_DRAGGED) {
                    view.showMaxItemDialog();
                }
                return;
            }
        } else if (obj instanceof WallObject || obj instanceof SearchableObject) {
            if (existing instanceof WallTile) {
                WallTile wt = (WallTile) existing;
                GameObject currentDeco = wt.getDecoration();
                
                boolean isDecor = obj.getImageName() != null && obj.getImageName().contains("WallDecoration/");
                boolean isSearch = obj.getImageName() != null && obj.getImageName().contains("WallSearchable/");

                if (isDecor) {
                    if (currentDeco != null && currentDeco.getImageName() != null && currentDeco.getImageName().contains("WallDecoration/")) {
                        replacingSameCategory = true;
                    }
                    if (!replacingSameCategory && view.countWallDecorative() >= view.MAX_DECORATIVE_PER_MAP) {
                        if (e.getID() != MouseEvent.MOUSE_DRAGGED) view.showMaxWallDialog();
                        return;
                    }
                } else if (isSearch) {
                    if (currentDeco != null && currentDeco.getImageName() != null && currentDeco.getImageName().contains("WallSearchable/")) {
                        replacingSameCategory = true;
                    }
                    if (!replacingSameCategory && view.countWallSearchable() >= view.MAX_SEARCHABLE_PER_MAP) {
                        if (e.getID() != MouseEvent.MOUSE_DRAGGED) view.showMaxWallDialog();
                        return;
                    }
                }
                
                if (view.isWallTilePlaceable(hoverTileX, hoverTileY, obj, true)) {
                    wt.setDecoration(obj);
                } else {
                    if (e.getID() != MouseEvent.MOUSE_DRAGGED) view.showInvalidMapDialog();
                }
            }
            return;
        } else if (view.isObstacle(obj)) {
            if (view.isObstacle(existing)) {
                replacingSameCategory = true;
            }
            if (!replacingSameCategory && view.countObstacles() >= view.MAX_OBSTACLES) {
                if (e.getID() != MouseEvent.MOUSE_DRAGGED) {
                    view.showMaxObstacleDialog();
                }
                return;
            }
        }

        map.placeObject(obj, hoverTileX, hoverTileY);
    }

    public void eraseAt(int tx, int ty) {
        GameObject existing = map.getObjectAt(tx, ty);
        if (existing == null) return;
        if (existing instanceof domain.models.staticObjects.LevelDoor) return;

        if (existing instanceof WallTile) {
            map.placeObject(null, tx, ty);
        } else {
            map.placeObject(new FloorTile(), tx, ty);
        }
    }
}
