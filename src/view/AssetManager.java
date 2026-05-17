package view;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
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
            for (int i = 1; i <= 9; i++) {
                String path = "resources/images/characters/Hero/hero_walk_" + i + ".png";
                File imgFile = findFile(path);
                if (imgFile != null) {
                    BufferedImage frame = ImageIO.read(imgFile);
                    heroWalkRight.addFrame(frame);
                }
            }
            File idleFile = findFile("resources/images/characters/Hero/hero_walk_1.png");
            if (idleFile != null) {
                BufferedImage idleFrame = ImageIO.read(idleFile);
                heroIdle.addFrame(idleFrame);
            }
        } catch (Exception e) {
            System.err.println("Error loading Hero sprites: " + e.getMessage());
        }
    }

    private void loadEnemyAnimations() {
        try {
            for (int i = 1; i <= 9; i++) {
                File kFile = findFile("resources/images/characters/Enemy/Knight/knight_walk_" + i + ".png");
                if (kFile != null) {
                    BufferedImage frame = ImageIO.read(kFile);
                    knightWalk.addFrame(frame);
                }
            }

            // Load Sorcerer walking frames (1-9)
            for (int i = 1; i <= 9; i++) {
                File sFile = findFile("resources/images/characters/Enemy/Sorcerer/sorcerer_walk_" + i + ".png");
                if (sFile != null) {
                    BufferedImage frame = ImageIO.read(sFile);
                    sorcererWalk.addFrame(frame);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading Enemy sprites: " + e.getMessage());
        }
    }

    private File findFile(String path) {
        File f = new File(path);
        if (f.exists()) return f;
        f = new File("../" + path);
        if (f.exists()) return f;
        return null;
    }

    public BufferedImage getHeroSprite(AnimationState state) {
        if (state == AnimationState.IDLE) {
            return heroIdle.getCurrentFrame();
        }
        return heroWalkRight.getCurrentFrame();
    }

    /**
     * Returns the current sprite frame for a given sprite key.
     * Keys are declared by entities via the Renderable interface.
     */
    public BufferedImage getSprite(String key) {
        if (key == null) return null;
        switch (key) {
            case "knight":   return knightWalk.getCurrentFrame();
            case "sorcerer": return sorcererWalk.getCurrentFrame();
            case "hero":     return heroIdle.getCurrentFrame();
            default:         return null;
        }
    }

    public BufferedImage getKnightSprite() {
        return knightWalk.getCurrentFrame();
    }

    public BufferedImage getSorcererSprite() {
        return sorcererWalk.getCurrentFrame();
    }
}