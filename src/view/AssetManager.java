package view;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import domain.models.AnimationState;

public class AssetManager {
    private BufferedImage characterSheet;
    private BufferedImage itemsSheet;

    // Her animasyon durumu için bir animatör tutan harita
    private Map<AnimationState, SpriteAnimation> heroAnimations;

    public AssetManager() {
        heroAnimations = new HashMap<>();
        loadSheets();
        setupHeroAnimations();
    }

    private void loadSheets() {
        try {
            // Dosya yollarını projendeki klasör yapısına göre güncelle
            characterSheet = ImageIO.read(new File("src/assets/characters x2.png"));
            itemsSheet = ImageIO.read(new File("src/assets/items x2.png"));
        } catch (IOException e) {
            System.err.println("Assetler yüklenirken hata oluştu: " + e.getMessage());
        }
    }

    private void setupHeroAnimations() {
        // --- HERO WALK RIGHT (Manuel Koordinatlar) ---
        // Not: Resmin x2 olduğunu söyledin, bu yüzden kareler genelde 32x32 veya
        // yakınıdır.
        SpriteAnimation walkRight = new SpriteAnimation(120); // 120ms kare hızı

        // Buradaki koordinatları (x, y, width, height) resmine bakarak manuel ince ayar
        // yapmalısın
        walkRight.addFrame(characterSheet.getSubimage(0, 64, 32, 32));
        walkRight.addFrame(characterSheet.getSubimage(33, 64, 32, 32));
        walkRight.addFrame(characterSheet.getSubimage(65, 64, 32, 32));

        heroAnimations.put(AnimationState.WALK_RIGHT, walkRight);

        // --- HERO IDLE ---
        SpriteAnimation idle = new SpriteAnimation(500);
        idle.addFrame(characterSheet.getSubimage(0, 64, 32, 32));
        heroAnimations.put(AnimationState.IDLE, idle);
    }

    /**
     * Hero'nun o anki durumuna göre doğru kareyi döndürür.
     */
    public BufferedImage getHeroSprite(AnimationState state) {
        SpriteAnimation anim = heroAnimations.getOrDefault(state, heroAnimations.get(AnimationState.IDLE));
        return anim.getCurrentFrame();
    }
}