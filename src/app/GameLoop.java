package app;

import domain.logic.EnemySpawner;
import domain.logic.LevelManager;
import domain.logic.RandomItemSpawner;
import domain.logic.ScrollSpawner;
import domain.models.GameMode;
import domain.models.Team;
import domain.models.entity.*;
import domain.models.item.MapItem;
import domain.models.item.usables.PotionItem;
import domain.models.item.VictoryCoin;
import domain.models.map.GameMap;
import domain.models.staticObjects.Door;
import domain.models.staticObjects.KeyItem;
import domain.models.staticObjects.LevelDoor;
import view.GameOverMenu;
import view.GameView;
import controller.InputHandler;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameLoop {
    private Timer logicTimer;
    private Timer renderTimer;

    private Hero hero;
    private List<Entity> entities;
    private GameMap[] mapRef;
    private GameMode mode;
    private InputHandler inputHandler;
    private EnemySpawner spawner;
    private ScrollSpawner scrollSpawner;
    private LevelManager levelManager;
    private GameView gameView;
    private JFrame frame;
    private GameOverMenu gameOverMenu;
    
    private long totalElapsedTimeMs;
    private long lastTickTime;
    private Runnable advanceLevelRunnable;

    public GameLoop(Hero hero, List<Entity> entities, GameMap[] mapRef, GameMode mode,
                    InputHandler inputHandler, EnemySpawner spawner, ScrollSpawner scrollSpawner,
                    LevelManager levelManager, GameView gameView, JFrame frame,
                    GameOverMenu gameOverMenu, long initialElapsedMs, Runnable advanceLevelRunnable) {
        this.hero = hero;
        this.entities = entities;
        this.mapRef = mapRef;
        this.mode = mode;
        this.inputHandler = inputHandler;
        this.spawner = spawner;
        this.scrollSpawner = scrollSpawner;
        this.levelManager = levelManager;
        this.gameView = gameView;
        this.frame = frame;
        this.gameOverMenu = gameOverMenu;
        this.totalElapsedTimeMs = initialElapsedMs;
        this.lastTickTime = System.currentTimeMillis();
        this.advanceLevelRunnable = advanceLevelRunnable;
        
        setupTimers();
    }

    private void setupTimers() {
        logicTimer = new Timer(120, e -> updateLogic());
        renderTimer = new Timer(16, e -> gameView.repaint());
    }

    public void start() {
        lastTickTime = System.currentTimeMillis();
        logicTimer.start();
        renderTimer.start();
    }

    public void stop() {
        if (logicTimer != null) logicTimer.stop();
        if (renderTimer != null) renderTimer.stop();
    }

    public Timer getLogicTimer() { return logicTimer; }
    public Timer getRenderTimer() { return renderTimer; }
    
    public void setLastTickTime(long time) { this.lastTickTime = time; }

    private void updateLogic() {
        long now = System.currentTimeMillis();
        long delta = now - lastTickTime;
        lastTickTime = now;
        if (delta > 0 && delta < 1000) {
            totalElapsedTimeMs += delta;
        }
        gameView.setElapsedSeconds(totalElapsedTimeMs / 1000);

        if (mode == GameMode.TEAM_MATCH) {
            boolean cyanAlive = false;
            boolean orangeAlive = false;
            for (Entity ent : entities) {
                if (ent.isAlive()) {
                    if (ent.getTeam() == Team.CYAN) cyanAlive = true;
                    if (ent.getTeam() == Team.ORANGE) orangeAlive = true;
                }
            }

            if (!cyanAlive || !orangeAlive) {
                stop();
                inputHandler.disableInput();

                boolean isVictory = cyanAlive;
                if (isVictory) {
                    util.helpers.SoundManager.playVictory();
                } else {
                    util.helpers.SoundManager.playGameOver();
                }
                String headingText = isVictory ? "YOU WIN" : "GAME OVER";
                String subHeadingText = isVictory ? "Orange Team has been defeated. You win!"
                        : "Cyan Team has been defeated. Game Over!";

                gameOverMenu.setupGameOverMenu(headingText, subHeadingText, false, isVictory);
                frame.setGlassPane(gameOverMenu);
                gameOverMenu.setVisible(true);
                return;
            }
        } else {
            if (levelManager != null) {
                GameObject standingOn = mapRef[0].getObjectAt(hero.getX(), hero.getY());
                if (standingOn instanceof LevelDoor) {
                    LevelDoor ld = (LevelDoor) standingOn;
                    if (!ld.isLocked() && advanceLevelRunnable != null) {
                        advanceLevelRunnable.run();
                    }
                }
            }

            boolean allEnemiesDefeated = true;
            for (Entity entity : entities) {
                if ((entity instanceof Knight || entity instanceof Sorcerer) && entity.isAlive()) {
                    allEnemiesDefeated = false;
                    break;
                }
            }

            boolean exitReached = false;
            for (int x = 0; x < mapRef[0].getWidth(); x++) {
                for (int y = 0; y < mapRef[0].getHeight(); y++) {
                    GameObject obj = mapRef[0].getObjectAt(x, y);
                    if (obj instanceof Door && "Exit Door".equals(obj.getName())) {
                        Door door = (Door) obj;
                        if (!door.isLocked() && Math.abs(hero.getX() - x) <= 1 && Math.abs(hero.getY() - y) <= 1) {
                            exitReached = true;
                        }
                    }
                }
            }

            if (allEnemiesDefeated && exitReached) {
                stop();
                inputHandler.disableInput();

                util.helpers.SoundManager.playVictory();

                gameOverMenu.setupGameOverMenu("YOU WIN", "You have escaped the COMP302 dungeon!", false, true);
                frame.setGlassPane(gameOverMenu);
                gameOverMenu.setVisible(true);
                return;
            }

            if (!hero.isAlive()) {
                stop();
                inputHandler.disableInput();
                util.helpers.SoundManager.playGameOver();
                gameOverMenu.setupGameOverMenu("GAME OVER", "You have succumbed to your fate.", false, false);
                frame.setGlassPane(gameOverMenu);
                gameOverMenu.setVisible(true);
                return;
            }
        }

        hero.update();

        if (mode == GameMode.TEAM_MATCH) {
            for (Entity ent : entities) {
                if (ent instanceof Knight && ent.isAlive()) {
                    ((Knight) ent).followHero(hero, mapRef[0], entities);
                } else if (ent instanceof Sorcerer && ent.isAlive()) {
                    ((Sorcerer) ent).followHero(hero, mapRef[0], entities);
                }
            }
        } else {
            if (levelManager == null || levelManager.getCurrentLevel() == 1) {
                spawner.trySpawn(entities);
            }

            List<Entity> minionsToSpawn = new ArrayList<>();
            for (Entity ent : entities) {
                if (ent.isAlive()) {
                    if (ent instanceof Knight) {
                        ((Knight) ent).followHero(hero, mapRef[0], entities);
                    } else if (ent instanceof Sorcerer) {
                        ((Sorcerer) ent).followHero(hero, mapRef[0], entities);
                    } else if (ent instanceof FinalBoss) {
                        FinalBoss boss = (FinalBoss) ent;
                        boss.followHero(hero, mapRef[0], entities);
                        minionsToSpawn.addAll(boss.pollPendingMinions());
                        minionsToSpawn.addAll(boss.pollPendingSorcerers());
                    }
                }
            }
            if (!minionsToSpawn.isEmpty()) {
                entities.addAll(minionsToSpawn);
            }
        }

        List<Projectile> newProjectiles = new ArrayList<>();
        if (mode == GameMode.TEAM_MATCH) {
            for (Entity ent : entities) {
                if (ent instanceof Sorcerer) {
                    Projectile p = ((Sorcerer) ent).pollPendingProjectile();
                    if (p != null) newProjectiles.add(p);
                }
            }
        } else {
            for (Entity ent : entities) {
                if (ent instanceof Sorcerer) {
                    Projectile p = ((Sorcerer) ent).pollPendingProjectile();
                    if (p != null) newProjectiles.add(p);
                } else if (ent instanceof FinalBoss) {
                    Projectile p = ((FinalBoss) ent).pollPendingProjectile();
                    if (p != null) newProjectiles.add(p);
                }
            }
        }
        entities.addAll(newProjectiles);

        for (Entity en : entities) {
            if (en instanceof Projectile && en.isAlive()) {
                Projectile proj = (Projectile) en;
                proj.step(mapRef[0]);

                if (mode == GameMode.TEAM_MATCH) {
                    for (Entity target : entities) {
                        if (target.isAlive() && target != proj.getOwner() && !(target instanceof Projectile)) {
                            if (proj.getX() == target.getX() && proj.getY() == target.getY()
                                    && target.getTeam() != Team.NONE
                                    && target.getTeam() != proj.getOwner().getTeam()) {
                                int def = (target instanceof Hero) ? ((Hero) target).getDef() : 0;
                                if (target instanceof Knight) def = 1;
                                int damage = Math.max(1, proj.getDamage() - def);
                                target.takeDamage(damage);
                                GameView.addFloatingText(target.getX(), target.getY(), "-" + damage + " HP", new Color(255, 60, 60));
                                proj.setHp(0);
                                break;
                            }
                        }
                    }
                } else {
                    if (proj.getOwner() == hero) {
                        for (Entity enemy : entities) {
                            if (enemy.isAlive() && enemy != hero && !(enemy instanceof ShadowClone) && !(enemy instanceof Projectile)) {
                                boolean hit = enemy.occupiesTile(proj.getX(), proj.getY());
                                if (hit) {
                                    int def = 0;
                                    if (enemy instanceof Knight) def = 1;
                                    int damage = Math.max(0, proj.getDamage() - def);
                                    enemy.takeDamage(damage);
                                    GameView.addFloatingText(enemy.getX(), enemy.getY(), "-" + damage + " HP", new Color(255, 60, 60));
                                    proj.setHp(0);
                                    System.out.println("Enemy hit by player projectile! Damage: " + damage + " | Enemy HP: " + enemy.getHp());

                                    if (!enemy.isAlive()) {
                                        System.out.println("Enemy defeated by projectile!");
                                        GameObject loot = null;
                                        if (enemy instanceof FinalBoss) {
                                            loot = new VictoryCoin(enemy.getX(), enemy.getY());
                                        } else {
                                            Random rand = new Random();
                                            int dropType = rand.nextInt(3);
                                            if (dropType == 0) {
                                                loot = MapItem.createRandomItem(enemy.getX(), enemy.getY());
                                            } else if (dropType == 1) {
                                                loot = PotionItem.createRandomPotionItem(enemy.getX(), enemy.getY());
                                            } else {
                                                int locked = RandomItemSpawner.countLockedChests(mapRef[0]);
                                                int keys = RandomItemSpawner.countKeys(mapRef[0], hero);
                                                if (keys < locked) {
                                                    loot = new KeyItem(enemy.getX(), enemy.getY());
                                                } else {
                                                    loot = rand.nextBoolean() ? MapItem.createRandomItem(enemy.getX(), enemy.getY()) : PotionItem.createRandomPotionItem(enemy.getX(), enemy.getY());
                                                }
                                            }
                                        }
                                        if (loot != null) {
                                            mapRef[0].placeObject(loot, enemy.getX(), enemy.getY());
                                            System.out.println("Loot dropped: " + loot.getName());
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    } else {
                        if (proj.isAlive() && proj.getX() == hero.getX() && proj.getY() == hero.getY()) {
                            int def = hero.getDef();
                            int damage = Math.max(1, proj.getDamage() - def);
                            hero.takeDamage(damage);
                            GameView.addFloatingText(hero.getX(), hero.getY(), "-" + damage + " HP", new Color(255, 200, 50));
                            proj.setHp(0);
                            System.out.println("Hero hit by projectile! Damage: " + damage + " | Hero HP: " + hero.getHp());
                        }
                    }
                }
            }
        }

        scrollSpawner.trySpawn();
        ShadowClone activeCloneForUpdate = inputHandler.getShadowClone();
        if (activeCloneForUpdate != null) activeCloneForUpdate.update();

        entities.removeIf(ent -> !ent.isAlive());
    }
}
