package view;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.List;
import domain.models.GameState;
import domain.logic.SaveManager;

public class MainMenuView extends JPanel {

    private Runnable onStartGame;
    private java.util.function.Consumer<GameState> onLoadGame;
    private Image backgroundImage;
    private BufferedImage titleImage;

    // Eski constructor — DemoRunner hemen bozulmasın diye
    public MainMenuView(Runnable onStartGame) {
        this(onStartGame, null);
    }

    public MainMenuView(Runnable onStartGame, java.util.function.Consumer<GameState> onLoadGame) {
        this.onStartGame = onStartGame;
        this.onLoadGame = onLoadGame;
        try {
            File bgFile = new File("resources/images/MainMenuImages/main_menu_bg.png");
            if (bgFile.exists()) {
                backgroundImage = ImageIO.read(bgFile);
            } else {
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
    private JButton loadBtn; // Load Game — styled JButton (görselsiz)

    private void initUI() {
        setLayout(null);

        startBtn = new ScaledImageButton("resources/images/MainMenuImages/start_game_button.png");
        helpBtn  = new ScaledImageButton("resources/images/MainMenuImages/help_button.png");
        quitBtn  = new ScaledImageButton("resources/images/MainMenuImages/quit_button.png");

        // Load Game butonu (resim yok, text-based)
        loadBtn = new JButton("Load Game");
        loadBtn.setFont(new Font("Arial", Font.BOLD, 16));
        loadBtn.setForeground(new Color(255, 220, 100));
        loadBtn.setBackground(new Color(60, 40, 20));
        loadBtn.setFocusPainted(false);
        loadBtn.setBorder(BorderFactory.createLineBorder(new Color(180, 140, 60), 2));
        loadBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        startBtn.addActionListener(e -> {
            if (onStartGame != null) onStartGame.run();
        });

        helpBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Welcome to Dungeon Crawler!\n\n" +
                    "Use Arrow Keys or W, A, S, D to move.\n" +
                    "Avoid enemies and survive as long as you can!\n",
                    "Help", JOptionPane.INFORMATION_MESSAGE);
        });

        quitBtn.addActionListener(e -> System.exit(0));

        loadBtn.addActionListener(e -> showLoadDialog());

        add(startBtn);
        add(helpBtn);
        add(quitBtn);
        add(loadBtn);
    }

    // Save listesini gösterir, seçilen save'i callback ile iletir
    private void showLoadDialog() {
        List<GameState> saves = SaveManager.listSaves();
        if (saves.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kayıtlı oyun bulunamadı.", "Yükleme", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Liste: "SaveName — 2026-05-04 21:00"
        String[] labels = saves.stream()
                .map(s -> s.saveName + "  —  " + s.timestamp)
                .toArray(String[]::new);

        String selected = (String) JOptionPane.showInputDialog(
                this,
                "Yüklenecek kaydı seçin:",
                "Oyun Yükle",
                JOptionPane.PLAIN_MESSAGE,
                null,
                labels,
                labels[0]
        );

        if (selected != null && onLoadGame != null) {
            int idx = java.util.Arrays.asList(labels).indexOf(selected);
            if (idx >= 0) onLoadGame.accept(saves.get(idx));
        }
    }

    @Override
    public void doLayout() {
        super.doLayout();
        int w = getWidth();
        int h = getHeight();

        double scaleX = w / 832.0;
        double scaleY = h / 640.0;
        double scale = Math.max(scaleX, scaleY);

        int logicalW = (int) (832 * scale);
        int logicalH = (int) (640 * scale);
        int offsetX = (w - logicalW) / 2;
        int offsetY = (h - logicalH) / 2;

        double btnScale = Math.min(scale, 1.25);
        int btnW = (int) (320 * btnScale);
        int btnH = (int) (80 * btnScale);
        int gap = (int) (10 * btnScale);

        int startX = offsetX + (logicalW - btnW) / 2;

        // 4 buton için toplam yükseklik
        int totalBtnHeight = 4 * btnH + 3 * gap;
        int centerY = offsetY + (int) (400 * scale);
        int startY = centerY - totalBtnHeight / 2;

        if (startBtn != null) startBtn.setBounds(startX, startY, btnW, btnH);
        if (loadBtn  != null) loadBtn.setBounds(startX, startY + btnH + gap, btnW, btnH);
        if (helpBtn  != null) helpBtn.setBounds(startX, startY + 2 * (btnH + gap), btnW, btnH);
        if (quitBtn  != null) quitBtn.setBounds(startX, startY + 3 * (btnH + gap), btnW, btnH);
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
