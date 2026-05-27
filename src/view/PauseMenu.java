package view;

import domain.logic.SaveManager;
import domain.logic.LevelManager;
import domain.models.entity.Entity;
import domain.models.entity.Hero;
import domain.models.map.GameMap;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Oyun içi duraklat menüsü.
 * JFrame'in glass pane'i olarak kullanılır — oyunun üstüne yarı saydam overlay olarak çizilir.
 * ESC tuşuyla açılıp kapanır.
 */
public class PauseMenu extends JPanel {

    private final Hero hero;
    private final List<Entity> entities;
    private final GameMap map;
    private final domain.logic.EnemySpawner enemySpawner;
    private final domain.logic.ScrollSpawner scrollSpawner;
    private final LevelManager levelManager;
    private final GameView gameView;
    private final Runnable onResume;
    private final Runnable onRestart;
    private final Runnable onMainMenu;

    // Loaded image assets
    private BufferedImage bgImage;
    private BufferedImage resumeImage;
    private BufferedImage restartImage;
    private BufferedImage saveImage;
    private BufferedImage mainMenuImage;

    public PauseMenu(Hero hero, List<Entity> entities, GameMap map,
                     domain.logic.EnemySpawner enemySpawner, domain.logic.ScrollSpawner scrollSpawner,
                     LevelManager levelManager, GameView gameView,
                     Runnable onResume, Runnable onRestart, Runnable onMainMenu) {
        this.hero = hero;
        this.entities = entities;
        this.map = map;
        this.enemySpawner = enemySpawner;
        this.scrollSpawner = scrollSpawner;
        this.levelManager = levelManager;
        this.gameView = gameView;
        this.onResume = onResume;
        this.onRestart = onRestart;
        this.onMainMenu = onMainMenu;

        loadImages();

        setOpaque(false); // Arka planı saydam tut
        setLayout(new GridBagLayout()); // Butonları ortalar

        initButtons();
        setVisible(false); // Başlangıçta gizli
    }

    private void loadImages() {
        bgImage = loadImg("resources/images/PauseMenuImages/PauseMenuBox.png");
        resumeImage = loadImg("resources/images/PauseMenuImages/PauseResumeGameButton.png");
        restartImage = loadImg("resources/images/PauseMenuImages/PauseRestartGameButton.png");
        saveImage = loadImg("resources/images/PauseMenuImages/PauseSaveGameButton.png");
        mainMenuImage = loadImg("resources/images/PauseMenuImages/PauseMainMenuButton.png");
    }

    private BufferedImage loadImg(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) {
                f = new File("../" + path);
            }
            if (f.exists()) {
                return ImageIO.read(f);
            }
        } catch (Exception e) {
            System.err.println("Could not load image: " + path + " - " + e.getMessage());
        }
        return null;
    }

    private int getWidthForHeight(BufferedImage img, int targetHeight, int fallbackWidth) {
        if (img == null) return fallbackWidth;
        float aspect = img.getWidth() / (float) img.getHeight();
        return Math.round(targetHeight * aspect);
    }

    private void initButtons() {
        if (bgImage != null) {
            float scale = 1.25f;
            int bgWidth = Math.round(602 * scale);
            int bgHeight = Math.round(414 * scale);

            int innerX = Math.round(204 * scale);
            int innerWidth = Math.round(214 * scale);
            int innerY = Math.round(152 * scale);
            int innerHeight = Math.round(233 * scale);

            int btnHeight = Math.round(52 * scale);

            // Pre-trim images to get accurate bounds and aspect ratio
            BufferedImage trimmedResume  = trimImage(resumeImage);
            BufferedImage trimmedRestart = trimImage(restartImage);
            BufferedImage trimmedSave    = trimImage(saveImage);
            BufferedImage trimmedMenu    = trimImage(mainMenuImage);

            // Calculate precise widths to match aspect ratio
            int resumeW  = getWidthForHeight(trimmedResume, btnHeight, Math.round(160 * scale));
            int restartW = getWidthForHeight(trimmedRestart, btnHeight, Math.round(160 * scale));
            int saveW    = getWidthForHeight(trimmedSave, btnHeight, Math.round(160 * scale));
            int menuW    = getWidthForHeight(trimmedMenu, btnHeight, Math.round(160 * scale));

            JPanel menuBoxPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.drawImage(bgImage, 0, 0, getWidth(), getHeight(), null);
                    g2.dispose();
                }
            };
            menuBoxPanel.setOpaque(false);
            menuBoxPanel.setLayout(null);
            menuBoxPanel.setPreferredSize(new Dimension(bgWidth, bgHeight));
            menuBoxPanel.setMinimumSize(new Dimension(bgWidth, bgHeight));
            menuBoxPanel.setMaximumSize(new Dimension(bgWidth, bgHeight));

            // Create custom image buttons with trimmed images
            JButton resumeBtn  = new ImageButton(trimmedResume, "Resume");
            JButton restartBtn = new ImageButton(trimmedRestart, "Restart Game");
            JButton saveBtn    = new ImageButton(trimmedSave, "Save Game");
            JButton menuBtn    = new ImageButton(trimmedMenu, "Main Menu");

            // Set ActionListeners
            resumeBtn.addActionListener(e -> {
                setVisible(false);
                if (onResume != null) onResume.run();
            });

            restartBtn.addActionListener(e -> {
                setVisible(false);
                if (onRestart != null) onRestart.run();
            });

            saveBtn.addActionListener(e -> handleSave());

            menuBtn.addActionListener(e -> {
                setVisible(false);
                if (onMainMenu != null) onMainMenu.run();
            });

            // Position each button centered horizontally inside the inner purple container bounds
            int spacing = 6;

            resumeBtn.setBounds(innerX + (innerWidth - resumeW) / 2, innerY + 6, resumeW, btnHeight);
            restartBtn.setBounds(innerX + (innerWidth - restartW) / 2, innerY + 6 + (btnHeight + spacing), restartW, btnHeight);
            saveBtn.setBounds(innerX + (innerWidth - saveW) / 2, innerY + 6 + 2 * (btnHeight + spacing), saveW, btnHeight);
            menuBtn.setBounds(innerX + (innerWidth - menuW) / 2, innerY + 6 + 3 * (btnHeight + spacing), menuW, btnHeight);

            menuBoxPanel.add(resumeBtn);
            menuBoxPanel.add(restartBtn);
            menuBoxPanel.add(saveBtn);
            menuBoxPanel.add(menuBtn);

            add(menuBoxPanel);
        } else {
            // Fallback to text buttons if background image is not found
            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(4, 1, 0, 12));
            panel.setBackground(new Color(30, 20, 10, 220));
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(180, 140, 60), 2),
                    BorderFactory.createEmptyBorder(20, 40, 20, 40)
            ));

            JButton resumeBtn  = createFallbackButton("Resume");
            JButton restartBtn = createFallbackButton("Restart Game");
            JButton saveBtn    = createFallbackButton("Save Game");
            JButton menuBtn    = createFallbackButton("Main Menu");

            resumeBtn.addActionListener(e -> {
                setVisible(false);
                if (onResume != null) onResume.run();
            });

            restartBtn.addActionListener(e -> {
                setVisible(false);
                if (onRestart != null) onRestart.run();
            });

            saveBtn.addActionListener(e -> handleSave());

            menuBtn.addActionListener(e -> {
                setVisible(false);
                if (onMainMenu != null) onMainMenu.run();
            });

            panel.add(resumeBtn);
            panel.add(restartBtn);
            panel.add(saveBtn);
            panel.add(menuBtn);

            add(panel);
        }
    }

    private JButton createFallbackButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setForeground(new Color(255, 220, 100)); // Altın sarısı yazı
        btn.setBackground(new Color(60, 40, 20));    // Koyu kahve arka plan
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(new Color(180, 140, 60), 1));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void handleSave() {
        // Mevcut save'leri listele
        java.util.List<domain.models.GameState> existingSaves = domain.logic.SaveManager.listSaves();
        String[] options = new String[existingSaves.size() + 1];
        options[0] = "[ New Save ]";
        for (int i = 0; i < existingSaves.size(); i++) {
            options[i + 1] = existingSaves.get(i).saveName + "  —  " + existingSaves.get(i).timestamp;
        }

        String selected = (String) JOptionPane.showInputDialog(
                this,
                "Create a new save or overwrite an existing one:",
                "Save Game",
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );

        if (selected == null) return;

        String saveName;
        if ("[ New Save ]".equals(selected)) {
            saveName = JOptionPane.showInputDialog(this, "Enter save name:", "New Save", JOptionPane.PLAIN_MESSAGE);
            if (saveName == null || saveName.trim().isEmpty()) return;
            saveName = saveName.trim();
        } else {
            // Seçilen save'in adını çıkar (format: "name  —  date")
            int idx = java.util.Arrays.asList(options).indexOf(selected) - 1;
            saveName = existingSaves.get(idx).saveName;
        }

        SaveManager.save(saveName, hero, entities, map, enemySpawner, scrollSpawner, levelManager.getCurrentLevel(), gameView.getElapsedSeconds());
        JOptionPane.showMessageDialog(this, "Saved successfully: " + saveName, "Save Successful", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Ekranın tamamını koyu yarı saydam renkle kapla
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }

    // --- İç sınıf: resim tabanlı buton ---
    private class ImageButton extends JButton {
        private BufferedImage img;
        private boolean hovered = false;
        private boolean pressed = false;

        public ImageButton(BufferedImage trimmedImage, String fallbackText) {
            this.img = trimmedImage;
            if (this.img == null) {
                setText(fallbackText);
                setFont(new Font("Arial", Font.BOLD, 16));
                setForeground(new Color(255, 220, 100));
                setContentAreaFilled(true);
                setBackground(new Color(60, 40, 20));
                setBorder(BorderFactory.createLineBorder(new Color(180, 140, 60), 1));
            } else {
                setFocusPainted(false);
                setBorderPainted(false);
                setContentAreaFilled(false);
                setOpaque(false);
            }
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    hovered = true;
                    repaint();
                }
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    hovered = false;
                    pressed = false;
                    repaint();
                }
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        pressed = true;
                        repaint();
                    }
                }
                @Override
                public void mouseReleased(java.awt.event.MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        pressed = false;
                        repaint();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (img != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                
                int drawX = 0;
                int drawY = 0;
                int drawWidth = getWidth();
                int drawHeight = getHeight();

                // Premium interactive effects: shift vertically
                if (pressed) {
                    drawY += 1;
                } else if (hovered) {
                    drawY -= 3;
                }

                g2.drawImage(img, drawX, drawY, drawWidth, drawHeight, null);

                // Highlight and press overlays
                if (pressed) {
                    g2.setColor(new Color(0, 0, 0, 45));
                    g2.fillRect(drawX, drawY, drawWidth, drawHeight);
                } else if (hovered) {
                    g2.setColor(new Color(255, 255, 255, 30));
                    g2.fillRect(drawX, drawY, drawWidth, drawHeight);
                }

                g2.dispose();
            } else {
                super.paintComponent(g);
            }
        }
    }

    // --- Yardımcı: şeffaf kenarlıkları kırp ---
    private BufferedImage trimImage(BufferedImage img) {
        if (img == null) return null;
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
