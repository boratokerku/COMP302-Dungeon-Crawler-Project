package controller;

import domain.models.map.GameMap;
import domain.models.entity.Hero;
import domain.models.entity.GameObject;
import view.ActionMenu;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MouseHandler extends MouseAdapter {

    private final Hero hero;
    private final GameMap gameMap;
    private final view.GameView gameView;
    private final ActionMenu actionMenu;

    public MouseHandler(Hero hero, GameMap gameMap, view.GameView gameView, ActionMenu actionMenu) {
        this.hero = hero;
        this.gameMap = gameMap;
        this.gameView = gameView;
        this.actionMenu = actionMenu;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int tileSize = gameView.getTileSize();
        int offsetX = gameView.getOffsetX();
        int offsetY = gameView.getOffsetY();

        if (tileSize <= 0) return;

        // 1. Ekrandaki tıkı Map offsetlerine göre grid koordinatına çevir
        int clickedGridX = (e.getX() - offsetX) / tileSize;
        int clickedGridY = (e.getY() - offsetY) / tileSize;

        // Bounds check
        if (clickedGridX < 0 || clickedGridX >= gameMap.getWidth() || clickedGridY < 0 || clickedGridY >= gameMap.getHeight()) {
            actionMenu.hideMenu();
            return;
        }

        // 2. Hero'nun grid konumunu al
        int heroX = hero.getX();
        int heroY = hero.getY();

        // 3. 3x3 alan kontrolü (fark her iki eksende de max 1 olmalı)
        boolean isAdjacent = Math.abs(heroX - clickedGridX) <= 1
                          && Math.abs(heroY - clickedGridY) <= 1;

        if (!isAdjacent) {
            actionMenu.hideMenu(); // Uzaktaki tıklamalarda menüyü kapat
            return;
        }

        // 4. O konumda nesne var mı?
        GameObject obj = gameMap.getObjectAt(clickedGridX, clickedGridY);

        if (obj == null) {
            actionMenu.hideMenu();
            return;
        }

        // We show the Action Menu inside GameView, passing e.getComponent() logic actually handled by actionMenu parent,
        // but actionMenu is a child of GameView (we'll add it). 
        actionMenu.show(gameView, obj, e.getX(), e.getY());
    }
}
