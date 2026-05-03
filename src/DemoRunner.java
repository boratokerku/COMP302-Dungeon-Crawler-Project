import domain.models.entity.*;
import domain.models.map.GameMap;
import domain.models.Direction;
import domain.logic.EnemySpawner;
import view.AssetManager;
import view.GameView;
import view.TileManager;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

import java.awt.CardLayout;
import java.awt.Color;
import javax.swing.JPanel;

public class DemoRunner {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("COMP302 Dungeon Crawler Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            CardLayout cardLayout = new CardLayout();
            JPanel mainPanel = new JPanel(cardLayout);

            view.MainMenuView menuView = new view.MainMenuView(() -> {
                startGame(frame, mainPanel, cardLayout);
            });
            menuView.setPreferredSize(new java.awt.Dimension(832, 640));

            mainPanel.setBackground(Color.BLACK);
            mainPanel.add(menuView, "Menu");
            cardLayout.show(mainPanel, "Menu"); 
            frame.add(mainPanel);

            frame.setSize(832, 640);
            frame.pack();
            frame.revalidate();
            frame.repaint();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void startGame(JFrame frame, JPanel mainPanel, CardLayout cardLayout) {
        // Gerekli yöneticiler (Managers)
        AssetManager assetManager = AssetManager.getInstance();
        TileManager tileManager = new TileManager();

        // Modeller
        GameMap map = new GameMap(13, 10);
        Hero hero = new Hero(1, 2); 
        Knight knight = new Knight(11, 8); 
        Sorcerer sorcerer = new Sorcerer(8, 8); 

        // View (Görünüm)
        GameView gameView = new GameView(hero, assetManager);
        gameView.setPreferredSize(new java.awt.Dimension(832, 640));
        gameView.setGameMap(map);
        gameView.setTileManager(tileManager);
        gameView.setEnemies(knight, sorcerer);

        mainPanel.add(gameView, "Game");
        cardLayout.show(mainPanel, "Game");

        List<Entity> entities = new ArrayList<>();
        entities.add(hero);
        entities.add(knight);
        entities.add(sorcerer);

        gameView.setEntityList(entities);

        controller.InputHandler inputHandler = new controller.InputHandler(hero, map, entities);
        frame.addKeyListener(inputHandler);
        frame.setFocusable(true);
        frame.requestFocusInWindow();

        EnemySpawner spawner = new EnemySpawner(map);

        javax.swing.Timer logicTimer = new javax.swing.Timer(120, (e) -> {
            hero.update();
            knight.followHero(hero, map, entities);
            sorcerer.followHero(hero, map, entities);
            spawner.trySpawn(entities);

            for (Knight k : spawner.getSpawnedKnights()) {
                if (k.isAlive()) k.followHero(hero, map, entities);
            }
            for (Sorcerer s : spawner.getSpawnedSorcerers()) {
                if (s.isAlive()) s.followHero(hero, map, entities);
            }
        });
        logicTimer.start();

        javax.swing.Timer renderTimer = new javax.swing.Timer(16, (e) -> {
            gameView.repaint();
        });
        renderTimer.start();
    }
}