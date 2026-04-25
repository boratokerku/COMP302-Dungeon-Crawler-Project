package view;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import domain.models.AnimationState;

public class AssetManager {

    private static AssetManager instance;

    private SpriteAnimation heroWalkRight;
    private SpriteAnimation heroIdle;

    private SpriteAnimation knightWalk;
    private SpriteAnimation sorcererWalk;

    private AssetManager() {
        heroWalkRight = new SpriteAnimation(120);
        heroIdle = new SpriteAnimation(500);
        knightWalk = new SpriteAnimation(150);
        sorcererWalk = new SpriteAnimation(150);

        loadHeroAnimations();
        loadEnemyAnimations();
    }

    // Singleton Pattern implementation
    public static AssetManager getInstance() {
        if (instance == null) {
            instance = new AssetManager();
        }
        return instance;
    }

    private void loadHeroAnimations() {
        try {
            // Load Hero walking frames (1-9)
            for (int i = 1; i <= 9; i++) {
                File imgFile = new File("resources/images/characters/Hero/hero_walk_" + i + ".png");
                if (imgFile.exists()) {
                    BufferedImage frame = ImageIO.read(imgFile);
                    heroWalkRight.addFrame(frame);
                }
            }
            // Use the first walk frame as idle for now
            File idleFile = new File("resources/images/characters/Hero/hero_walk_1.png");
            if (idleFile.exists()) {
                BufferedImage idleFrame = ImageIO.read(idleFile);
                heroIdle.addFrame(idleFrame);
            }
        } catch (Exception e) {
            System.err.println("Error loading Hero sprites: " + e.getMessage());
        }
    }

    private void loadEnemyAnimations() {
        try {
            // Load Knight walking frames (1-9)
            for (int i = 1; i <= 9; i++) {
                File imgFile = new File("resources/images/characters/Enemy/Knight/knight_walk_" + i + ".png");
                if (imgFile.exists()) {
                    BufferedImage frame = ImageIO.read(imgFile);
                    knightWalk.addFrame(frame);
                }
            }

            // Load Sorcerer walking frames (1-9)
            for (int i = 1; i <= 9; i++) {
                File imgFile = new File("resources/images/characters/Enemy/Sorcerer/sorcerer_walk_" + i + ".png");
                if (imgFile.exists()) {
                    BufferedImage frame = ImageIO.read(imgFile);
                    sorcererWalk.addFrame(frame);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading Enemy sprites: " + e.getMessage());
        }
    }

    /**
     * Hero'nun o anki durumuna göre doğru kareyi döndürür.
     */
    public BufferedImage getHeroSprite(AnimationState state) {
        if (state == AnimationState.IDLE) {
            return heroIdle.getCurrentFrame();
        }
        // Tüm hareketler için (şimdilik) aynı walk animasyonunu kullan
        return heroWalkRight.getCurrentFrame();
    }

    public BufferedImage getKnightSprite() {
        return knightWalk.getCurrentFrame();
    }

    public BufferedImage getSorcererSprite() {
        return sorcererWalk.getCurrentFrame();
    }
}