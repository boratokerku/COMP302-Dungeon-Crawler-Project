package util.helpers;

import javax.sound.sampled.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SoundManager {
    private static final int SAMPLE_RATE = 16000;
    private static SourceDataLine line;
    private static final BlockingQueue<byte[]> soundQueue = new LinkedBlockingQueue<>();

    static {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, true);
            // Open the audio line ONCE at startup to prevent macOS CoreAudio / Rosetta crashes (exit code 132 / illegal instruction)
            line = AudioSystem.getSourceDataLine(format);
            line.open(format, SAMPLE_RATE * 2); // 2-second buffer size
            line.start();

            // Background worker to consume and play sound bytes sequentially
            Thread worker = new Thread(() -> {
                while (true) {
                    try {
                        byte[] data = soundQueue.take();
                        line.write(data, 0, data.length);
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        System.err.println("SoundManager queue playback error: " + e.getMessage());
                    }
                }
            });
            worker.setDaemon(true);
            worker.start();
        } catch (Exception e) {
            System.err.println("SoundManager initialization failed: " + e.getMessage());
        }
    }

    private static void playSound(byte[] audioData) {
        if (line != null && line.isOpen()) {
            soundQueue.offer(audioData);
        }
    }

    // Event 1: Walk (Short low-pitch dull thump)
    public static void playWalk() {
        int durationMs = 50;
        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        byte[] data = new byte[numSamples];
        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double freq = 120.0 - 60.0 * ((double) i / numSamples);
            double volume = 1.0 - ((double) i / numSamples);
            double val = Math.asin(Math.sin(2 * Math.PI * freq * t)) * (2.0 / Math.PI); // Triangle wave
            data[i] = (byte) (val * 30 * volume);
        }
        playSound(data);
    }

    // Event 2: Melee Swing (Air whoosh wave)
    public static void playSwing() {
        int durationMs = 120;
        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        byte[] data = new byte[numSamples];
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < numSamples; i++) {
            double volume = Math.sin(Math.PI * ((double) i / numSamples));
            double noise = rand.nextDouble() * 2.0 - 1.0;
            double t = (double) i / SAMPLE_RATE;
            double freq = 400.0 - 200.0 * ((double) i / numSamples);
            double tone = Math.sin(2 * Math.PI * freq * t);
            double val = 0.7 * noise + 0.3 * tone;
            data[i] = (byte) (val * 35 * volume);
        }
        playSound(data);
    }

    // Event 3: Ranged Shoot (Fast sliding pitch laser beam)
    public static void playShoot() {
        int durationMs = 150;
        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        byte[] data = new byte[numSamples];
        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double freq = 1500.0 - 1200.0 * ((double) i / numSamples);
            double volume = 1.0 - ((double) i / numSamples);
            double val = Math.sin(2 * Math.PI * freq * t);
            data[i] = (byte) (val * 40 * volume);
        }
        playSound(data);
    }

    // Event 4: Enemy Hit (Metallic impact sound)
    public static void playEnemyHit() {
        int durationMs = 100;
        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        byte[] data = new byte[numSamples];
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double volume = 1.0 - ((double) i / numSamples);
            double noise = rand.nextDouble() * 2.0 - 1.0;
            double freq = 800.0 + 300.0 * Math.sin(50.0 * t);
            double tone = Math.sin(2 * Math.PI * freq * t);
            double val = 0.5 * noise + 0.5 * tone;
            data[i] = (byte) (val * 45 * volume);
        }
        playSound(data);
    }

    // Event 5: Player Hit (Deeper crunch / pain grunt simulation)
    public static void playPlayerHit() {
        int durationMs = 250;
        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        byte[] data = new byte[numSamples];
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double volume = 1.0 - ((double) i / numSamples);
            double noise = rand.nextDouble() * 2.0 - 1.0;
            double freq = 180.0 - 100.0 * ((double) i / numSamples);
            double tone = Math.sin(2 * Math.PI * freq * t);
            double val = 0.7 * noise + 0.3 * tone;
            data[i] = (byte) (val * 55 * volume);
        }
        playSound(data);
    }

    // Event 6: Door/Chest Unlock/Open (Two-tone chime)
    public static void playUnlock() {
        int durationMs = 300;
        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        byte[] data = new byte[numSamples];
        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = (double) i / numSamples;
            double volume = 1.0 - progress;
            double freq = (progress < 0.4) ? 523.25 : 783.99; // C5 to G5
            double val = Math.sin(2 * Math.PI * freq * t);
            val += Math.signum(Math.sin(2 * Math.PI * (freq + 2) * t)) * 0.2;
            data[i] = (byte) ((val / 1.2) * 45 * volume);
        }
        playSound(data);
    }

    // Event 7: Potion / Heal (Sparkly rapid arpeggio)
    public static void playHeal() {
        int durationMs = 400;
        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        byte[] data = new byte[numSamples];
        double[] scale = {523.25, 659.25, 783.99, 1046.50};
        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = (double) i / numSamples;
            double volume = 1.0 - progress;
            int noteIndex = (int) (progress * scale.length * 2) % scale.length;
            double freq = scale[noteIndex];
            double val = Math.sin(2 * Math.PI * freq * t);
            data[i] = (byte) (val * 40 * volume);
        }
        playSound(data);
    }

    // Event 8: Teleport (Sci-fi magic sweep up and down)
    public static void playTeleport() {
        int durationMs = 300;
        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        byte[] data = new byte[numSamples];
        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = (double) i / numSamples;
            double volume = Math.sin(progress * Math.PI);
            double freq = 600.0 + 400.0 * Math.sin(progress * Math.PI * 3.0);
            double val = Math.sin(2 * Math.PI * freq * t);
            data[i] = (byte) (val * 40 * volume);
        }
        playSound(data);
    }

    // Event 9: Victory (Upbeat fanfare notes)
    public static void playVictory() {
        new Thread(() -> {
            double[] notes = {523.25, 659.25, 783.99, 1046.50};
            int[] durations = {150, 150, 150, 400};
            for (int k = 0; k < notes.length; k++) {
                double freq = notes[k];
                int duration = durations[k];
                int numSamples = (SAMPLE_RATE * duration) / 1000;
                byte[] data = new byte[numSamples];
                for (int i = 0; i < numSamples; i++) {
                    double t = (double) i / SAMPLE_RATE;
                    double volume = 1.0 - ((double) i / numSamples) * 0.4;
                    double val = Math.sin(2 * Math.PI * freq * t);
                    val += 0.3 * Math.sin(2 * Math.PI * freq * 2.0 * t);
                    data[i] = (byte) ((val / 1.3) * 50 * volume);
                }
                playSound(data);
                try {
                    Thread.sleep(duration);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    // Event 10: Game Over (Sad falling sequence)
    public static void playGameOver() {
        new Thread(() -> {
            double[] notes = {392.00, 349.23, 311.13, 261.63};
            int[] durations = {200, 200, 200, 500};
            for (int k = 0; k < notes.length; k++) {
                double freq = notes[k];
                int duration = durations[k];
                int numSamples = (SAMPLE_RATE * duration) / 1000;
                byte[] data = new byte[numSamples];
                for (int i = 0; i < numSamples; i++) {
                    double t = (double) i / SAMPLE_RATE;
                    double volume = 1.0 - ((double) i / numSamples);
                    double val = Math.sin(2 * Math.PI * freq * t);
                    data[i] = (byte) (val * 45 * volume);
                }
                playSound(data);
                try {
                    Thread.sleep(duration);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }
}
