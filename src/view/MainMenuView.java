package view;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.List;
import domain.models.GameState;
import domain.logic.SaveManager;
import ui.dialogs.HelpDialog;
import ui.dialogs.LoadGameDialog;
import ui.dialogs.DeleteConfirmDialog;

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
        this.onLoadGame  = onLoadGame;
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
            System.err.println("Resim yuklenemedi: " + e.getMessage());
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
    private JButton loadBtn;    // Load Game

    private void initUI() {
        setLayout(null);

        startBtn = new ScaledImageButton("resources/images/MainMenuImages/start_game_button.png");
        helpBtn  = new ScaledImageButton("resources/images/MainMenuImages/help_button.png");
        quitBtn  = new ScaledImageButton("resources/images/MainMenuImages/quit_button.png");
        loadBtn = new ScaledImageButton("resources/images/MainMenuImages/load_game_button.png");



        startBtn.addActionListener(e -> {
            if (onStartGame != null) onStartGame.run();
        });

        helpBtn.addActionListener(e -> {
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            Frame parentFrame = (parentWindow instanceof Frame) ? (Frame) parentWindow : null;
            HelpDialog helpDialog = new HelpDialog(parentFrame);
            helpDialog.setVisible(true);
        });

        quitBtn.addActionListener(e -> System.exit(0));

        loadBtn.addActionListener(e -> showLoadDialog());

        add(startBtn);
        add(helpBtn);
        add(quitBtn);
        add(loadBtn);
    }

    // Save listesini gösterir — Load ve Delete seçenekleriyle
    private void showLoadDialog() {
        List<GameState> saves = SaveManager.listSaves();

        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        Frame parentFrame = (parentWindow instanceof Frame) ? (Frame) parentWindow : null;

        LoadGameDialog dialog = new LoadGameDialog(parentFrame, saves);
        dialog.setVisible(true);

        if (dialog.isLoaded()) {
            if (onLoadGame != null) {
                onLoadGame.accept(dialog.getSelectedState());
            }
        } else if (dialog.isDeleteRequested()) {
            GameState toDelete = dialog.getDeleteState();
            DeleteConfirmDialog confirmDialog = new DeleteConfirmDialog(parentFrame, "Delete " + toDelete.saveName + "?");
            confirmDialog.setVisible(true);
            if (confirmDialog.isConfirmed()) {
                java.io.File f = new java.io.File("saves/" + toDelete.saveName + ".json");
                f.delete();
            }
            showLoadDialog(); // Refresh list
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

        if (startBtn != null) startBtn.setBounds(startX, startY,                   btnW, btnH);
        if (loadBtn  != null) loadBtn.setBounds(startX,  startY + (btnH + gap),    btnW, btnH);
        if (helpBtn  != null) helpBtn.setBounds(startX,  startY + 2 * (btnH + gap), btnW, btnH);
        if (quitBtn  != null) quitBtn.setBounds(startX,  startY + 3 * (btnH + gap), btnW, btnH);
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
