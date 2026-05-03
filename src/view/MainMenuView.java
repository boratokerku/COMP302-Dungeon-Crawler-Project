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

    public MainMenuView(Runnable onStartGame) {
        this.onStartGame = onStartGame;
        try {
            File bgFile = new File("resources/images/MainMenuImages/main_menu_bg.png");
            if (bgFile.exists()) {
                backgroundImage = ImageIO.read(bgFile);
            } else {
                // Fallback check if needed, but primary path is now correct
                bgFile = new File("resources/images/main_menu_bg.png");
                if (bgFile.exists()) {
                    backgroundImage = ImageIO.read(bgFile);
                }
            }
        } catch (Exception e) {
            System.err.println("Arka plan resmi yuklenemedi: " + e.getMessage());
        }

        try {
            File titleFile = new File("resources/images/MainMenuImages/title.png");
            if (titleFile.exists()) {
                BufferedImage orig = ImageIO.read(titleFile);
                titleImage = trimImage(orig);
            }
        } catch (Exception e) {
            System.err.println("Baslik resmi yuklenemedi: " + e.getMessage());
        }

        initUI();
    }

    private ScaledImageButton startBtn;
    private ScaledImageButton helpBtn;
    private ScaledImageButton quitBtn;

    private void initUI() {
        setLayout(null); // Mutlak konumlandırma ile butonların esnek ölçeklenmesini sağlayacağız

        startBtn = new ScaledImageButton("resources/images/MainMenuImages/start_game_button.png");
        helpBtn = new ScaledImageButton("resources/images/MainMenuImages/help_button.png");
        quitBtn = new ScaledImageButton("resources/images/MainMenuImages/quit_button.png");

        startBtn.addActionListener(e -> {
            if (onStartGame != null) {
                onStartGame.run();
            }
        });

        helpBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Welcome to Dungeon Crawler!\n\n" +
                            "Use Arrow Keys or W, A, S, D to move.\n" +
                            "Avoid enemies and survive as long as you can!\n",
                    "Help",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        quitBtn.addActionListener(e -> {
            System.exit(0);
        });

        add(startBtn);
        add(helpBtn);
        add(quitBtn);
    }

    @Override
    public void doLayout() {
        super.doLayout();
        int w = getWidth();
        int h = getHeight();

        // Orijinal oyun penceresi olan 832x640'a göre ölçek katsayısını hesaplıyoruz
        double scaleX = w / 832.0;
        double scaleY = h / 640.0;
        // Boşlukları doldurmak için resmi (ve butonları) kırparak/zoomlayarak büyütüyoruz (Math.max)
        double scale = Math.max(scaleX, scaleY);

        int logicalW = (int) (832 * scale);
        int logicalH = (int) (640 * scale);
        int offsetX = (w - logicalW) / 2;
        int offsetY = (h - logicalH) / 2;

        // Butonların aşırı büyümesini engellemek için maksimum bir ölçek (örn. 1.25) belirliyoruz
        double btnScale = Math.min(scale, 1.25);
        int btnW = (int) (320 * btnScale);
        int btnH = (int) (80 * btnScale);
        int gap = (int) (10 * btnScale);
        
        int startX = offsetX + (logicalW - btnW) / 2;
        
        // Butonların kapının üzerinde her zaman ortalı durması için merkez koordinatını hesaplıyoruz
        int totalBtnHeight = 3 * btnH + 2 * gap;
        int centerY = offsetY + (int) (380 * scale); // 250 (eski başlangıç) + 130 (buton bloklarının yarı boyu) = 380
        int startY = centerY - totalBtnHeight / 2;
        
        if (startBtn != null) {
            startBtn.setBounds(startX, startY, btnW, btnH);
        }
        if (helpBtn != null) {
            helpBtn.setBounds(startX, startY + btnH + gap, btnW, btnH);
        }
        if (quitBtn != null) {
            quitBtn.setBounds(startX, startY + 2 * btnH + 2 * gap, btnW, btnH);
        }
    }

    private class ScaledImageButton extends JButton {
        private BufferedImage img;

        public ScaledImageButton(String imagePath) {
            try {
                BufferedImage originalImg = ImageIO.read(new File(imagePath));
                img = trimImage(originalImg);
            } catch (Exception e) {
                System.err.println("Buton resmi yuklenemedi: " + imagePath);
            }
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
                // Resmin pürüzsüz ve orantılı bir şekilde butonun güncel boyutuna çizilmesi
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    private BufferedImage trimImage(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        int top = height / 2, bottom = height / 2, left = width / 2, right = width / 2;
        boolean found = false;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (((img.getRGB(x, y) >> 24) & 0xff) > 10) {
                    top = y; found = true; break;
                }
            }
            if (found) break;
        }
        found = false;
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                if (((img.getRGB(x, y) >> 24) & 0xff) > 10) {
                    bottom = y; found = true; break;
                }
            }
            if (found) break;
        }
        found = false;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (((img.getRGB(x, y) >> 24) & 0xff) > 10) {
                    left = x; found = true; break;
                }
            }
            if (found) break;
        }
        found = false;
        for (int x = width - 1; x >= 0; x--) {
            for (int y = 0; y < height; y++) {
                if (((img.getRGB(x, y) >> 24) & 0xff) > 10) {
                    right = x; found = true; break;
                }
            }
            if (found) break;
        }
        if (right <= left || bottom <= top) return img;
        return img.getSubimage(left, top, right - left + 1, bottom - top + 1);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            double scaleX = getWidth() / 832.0;
            double scaleY = getHeight() / 640.0;
            // Boşlukları doldurmak için zoom yapıyoruz (Math.max)
            double scale = Math.max(scaleX, scaleY);

            int logicalW = (int) (832 * scale);
            int logicalH = (int) (640 * scale);
            int offsetX = (getWidth() - logicalW) / 2;
            int offsetY = (getHeight() - logicalH) / 2;

            g.drawImage(backgroundImage, offsetX, offsetY, logicalW, logicalH, this);

            if (titleImage != null) {
                // Başlık resminin büyüme limitini butonlara kıyasla bir tık daha fazla yapıyoruz (örn: 1.5)
                double titleScale = Math.min(scale, 1.5);
                double imgRatio = (double) titleImage.getHeight() / titleImage.getWidth();
                
                // Başlığın referans genişliği 500 piksel olarak belirleyelim
                int tW = (int) (500 * titleScale);
                int tH = (int) (tW * imgRatio);
                
                // Başlığın ekran dışına (sağ/sol) taşmasını engelliyoruz (minimum 20px boşluk)
                if (tW > getWidth() - 40) {
                    tW = getWidth() - 40;
                    tH = (int) (tW * imgRatio);
                }
                
                // Başlığı her zaman ekranın tam ortasına hizalıyoruz
                int tX = getWidth() / 2 - tW / 2;
                
                // Ekran büyütüldüğünde (scale > 1.0) başlığın butonlara fazla uzak kalmaması için aşağı doğru hafif kaydırma (shift) ekliyoruz
                int extraDrop = 0;
                if (scale > 1.0) {
                    extraDrop = (int) ((scale - 1.0) * 80);
                }
                
                // Başlığın merkez koordinatını hesaplıyor ve ekstra kaydırmayı da ekliyoruz
                int tY = offsetY + (int) (120 * scale) - tH / 2 + extraDrop;

                // Başlığın üstten ekran dışına taşmasını engelliyoruz
                if (tY < 20) {
                    tY = 20;
                }

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(titleImage, tX, tY, tW, tH, null);
                g2.dispose();
            }
        }
    }
}
