package domain.models;

/**
 * Marks an Entity as having a visual representation in the game world.
 *
 * Implementing classes declare a sprite key (a string the AssetManager
 * can look up) and an optional render alpha for transparency effects.
 *
 * This keeps the domain layer decoupled from view classes — no imports
 * of AssetManager or BufferedImage are needed here.
 */
public interface Renderable {

    /**
     * Returns the sprite key used to look up this entity's image
     * in AssetManager.getSprite(). Example: "knight", "sorcerer".
     */
    String getSpriteKey();

    /**
     * Opacity for rendering (0.0 = invisible, 1.0 = fully opaque).
     * Override in subclasses that need transparency (e.g. ShadowClone).
     */
    default float getRenderAlpha() {
        return 1.0f;
    }
}
