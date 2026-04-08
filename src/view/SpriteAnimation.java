package view;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class SpriteAnimation {
    private List<BufferedImage> frames;
    private int currentFrame = 0;
    private long lastFrameTime;
    private int frameDelay;

    public SpriteAnimation(int frameDelay) {
        this.frames = new ArrayList<>();
        this.frameDelay = frameDelay;
        this.lastFrameTime = System.currentTimeMillis();
    }

    public void addFrame(BufferedImage img) {
        frames.add(img);
    }

    public BufferedImage getCurrentFrame() {
        if (System.currentTimeMillis() - lastFrameTime > frameDelay) {
            currentFrame = (currentFrame + 1) % frames.size();
            lastFrameTime = System.currentTimeMillis();
        }
        return frames.get(currentFrame);
    }
}