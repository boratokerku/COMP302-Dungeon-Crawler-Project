package view;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class MainMenuView extends JPanel {

    private Runnable onStartGame;
    private Image backgroundImage;
    private BufferedImage titleImage;

    private ScaledImageButton startBtn;
    private ScaledImageButton helpBtn;
    private ScaledImageButton quitBtn;

    public MainMenuView(Runnable onStartGame) {
        this.onStartGame = onStartGame;
        this.setBackground(new Color(20, 18, 28));

        try {
            File bgFile = findFile(
                "resources/images/MainMenuImages/main_menu_bg.png",
                "../resources/images/MainMenuImages/main_menu_bg.png"
            );
            if (bgFile != null) backgroundImage = ImageIO.read(bgFile);

            File titleFile = findFile(
                "resources/images/MainMenuImages/title.png",
                "../resources/images/MainMenuImages/title.png"
            );
            if (titleFile != null) {
                BufferedImage orig = ImageIO.read(titleFile);
                titleImage = trimImage(orig);
            }
        } catch (Exception e) {
            System.err.println("Resim yuklenemedi: " + e.getMessage());
        }

        initUI();
    }

    private File findFile(String... paths) {
        for (String path : paths) {
            File f = new File(path);
            if (f.exists()) return f;
        }
        return null;
    }

    private void initUI() {
        setLayout(null);

        startBtn = new ScaledImageButton("resources/images/MainMenuImages/start_game_button.png");
        helpBtn  = new ScaledImageButton("resources/images/MainMenuImages/help_button.png");
        quitBtn  = new ScaledImageButton("resources/images/MainMenuImages/quit_button.png");

        startBtn.addActionListener(e -> {
            if (onStartGame != null) onStartGame.run();
        });

        helpBtn.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Welcome to Dungeon Crawler!\n\n" +
                "Use Arrow Keys or W, A, S, D to move.\n" +
                "Avoid enemies and survive as long as you can!\n",
                "Help", JOptionPane.INFORMATION_MESSAGE));

        quitBtn.addActionListener(e -> System.exit(0));

        add(startBtn);
        add(helpBtn);
        add(quitBtn);
    }

    @Override
    public void doLayout() {
        super.doLayout();
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        double scaleX = w / 832.0;
        double scaleY = h / 640.0;
        double scale  = Math.max(scaleX, scaleY);

        int logW = (int)(832 * scale);
        int logH = (int)(640 * scale);
        int offX = (w - logW) / 2;
        int offY = (h - logH) / 2;

        double btnScale = Math.min(scale, 1.25);
        int btnW = (int)(320 * btnScale);
        int btnH = (int)(80  * btnScale);
        int gap  = (int)(10  * btnScale);

        int startX = offX + (logW - btnW) / 2;
        int totalH = 3 * btnH + 2 * gap;
        int centerY = offY + (int)(380 * scale);
        int startY  = centerY - totalH / 2;

        if (startBtn != null) startBtn.setBounds(startX, startY,                    btnW, btnH);
        if (helpBtn  != null) helpBtn .setBounds(startX, startY + btnH + gap,       btnW, btnH);
        if (quitBtn  != null) quitBtn .setBounds(startX, startY + 2*(btnH + gap),   btnW, btnH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            double scaleX = getWidth()  / 832.0;
            double scaleY = getHeight() / 640.0;
            double scale  = Math.max(scaleX, scaleY);
            int logW = (int)(832 * scale);
            int logH = (int)(640 * scale);
            int offX = (getWidth()  - logW) / 2;
            int offY = (getHeight() - logH) / 2;
            g.drawImage(backgroundImage, offX, offY, logW, logH, this);

            if (titleImage != null) {
                double tScale = Math.min(scale, 1.5);
                double ratio  = (double) titleImage.getHeight() / titleImage.getWidth();
                int tW = (int)(500 * tScale);
                int tH = (int)(tW  * ratio);
                if (tW > getWidth() - 40) { tW = getWidth() - 40; tH = (int)(tW * ratio); }
                int tX = getWidth() / 2 - tW / 2;
                int tY = offY + (int)(120 * scale) - tH / 2 + (scale > 1 ? (int)((scale-1)*80) : 0);
                if (tY < 20) tY = 20;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(titleImage, tX, tY, tW, tH, null);
                g2.dispose();
            }
        }
    }

    // --- İç sınıf: resim tabanlı buton ---
    private class ScaledImageButton extends JButton {
        private BufferedImage img;

        public ScaledImageButton(String imagePath) {
            try {
                File f = new File(imagePath);
                if (!f.exists()) f = new File("../" + imagePath);
                if (f.exists()) {
                    BufferedImage raw = ImageIO.read(f);
                    if (raw != null) img = trimImage(raw);
                }
            } catch (Exception ignored) {}
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (img != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    // --- Yardımcı: şeffaf kenarlıkları kırp ---
    private BufferedImage trimImage(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int top = 0, bottom = h - 1, left = 0, right = w - 1;
        try {
            outer: for (int y = 0; y < h; y++) for (int x = 0; x < w; x++)
                if (((img.getRGB(x,y) >> 24) & 0xff) > 10) { top = y; break outer; }
            outer: for (int y = h-1; y >= 0; y--) for (int x = 0; x < w; x++)
                if (((img.getRGB(x,y) >> 24) & 0xff) > 10) { bottom = y; break outer; }
            outer: for (int x = 0; x < w; x++) for (int y = 0; y < h; y++)
                if (((img.getRGB(x,y) >> 24) & 0xff) > 10) { left = x; break outer; }
            outer: for (int x = w-1; x >= 0; x--) for (int y = 0; y < h; y++)
                if (((img.getRGB(x,y) >> 24) & 0xff) > 10) { right = x; break outer; }
        } catch (Exception e) { return img; }
        if (right <= left || bottom <= top) return img;
        return img.getSubimage(left, top, right - left + 1, bottom - top + 1);
    }
}
