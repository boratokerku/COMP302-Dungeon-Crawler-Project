package view;

import domain.logic.SaveManager;
import domain.models.GameState;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Game Over overlay menu, drawn over the game.
 * Uses a transparent dark background with styled buttons to restart, load save,
 * or exit to menu.
 */
public class GameOverMenu extends JPanel {

    private final Runnable onRestart;
    private final java.util.function.Consumer<GameState> onLoadGame;
    private final Runnable onMainMenu;

    private JPanel containerPanel;
    private JLabel heading;
    private JLabel subHeading;
    private JButton restartBtn;
    private JButton loadBtn;
    private JButton menuBtn;

    // Custom pixel-art image assets
    private BufferedImage bgImage;
    private BufferedImage restartImage;
    private BufferedImage saveImage;
    private BufferedImage mainMenuImage;

    public GameOverMenu(Runnable onRestart, java.util.function.Consumer<GameState> onLoadGame, Runnable onMainMenu) {
        this.onRestart = onRestart;
        this.onLoadGame = onLoadGame;
        this.onMainMenu = onMainMenu;

        setOpaque(false); // Translucent background handled in paintComponent
        setLayout(new GridBagLayout()); // Center components

        loadImages();
        initUI();
        setVisible(false); // Hidden by default
    }

    private void loadImages() {
        bgImage = loadImg("resources/images/PopUpImages/GameOverBox.png");
        restartImage = loadImg("resources/images/PopUpImages/GameOverRestartButton.png");
        saveImage = loadImg("resources/images/PopUpImages/GameOverSaveGameButton.png");
        mainMenuImage = loadImg("resources/images/PopUpImages/GameOverMainMenuButton.png");
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

    private void initUI() {
        containerPanel = new JPanel();
        containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.Y_AXIS));
        containerPanel.setBackground(new Color(40, 10, 10, 230)); // Deep red-black transparent tint
        containerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 50, 50), 3), // Crimson border
                BorderFactory.createEmptyBorder(30, 50, 30, 50)));

        // Heading: GAME OVER
        heading = new JLabel("GAME OVER");
        heading.setFont(new Font("Arial", Font.BOLD, 48));
        heading.setForeground(new Color(255, 60, 60)); // Bright crimson
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        containerPanel.add(heading);

        containerPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Subheading
        subHeading = new JLabel("You have succumbed to your fate.");
        subHeading.setFont(new Font("Arial", Font.PLAIN, 18));
        subHeading.setForeground(Color.LIGHT_GRAY);
        subHeading.setAlignmentX(Component.CENTER_ALIGNMENT);
        containerPanel.add(subHeading);

        containerPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        // Buttons Panel
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new GridLayout(3, 1, 0, 15));
        btnPanel.setOpaque(false);

        restartBtn = createMenuButton("Restart Game");
        loadBtn = createMenuButton("Load Save");
        menuBtn = createMenuButton("Main Menu");

        restartBtn.addActionListener(e -> {
            setVisible(false);
            if (onRestart != null)
                onRestart.run();
        });

        loadBtn.addActionListener(e -> showLoadDialog());

        menuBtn.addActionListener(e -> {
            setVisible(false);
            if (onMainMenu != null)
                onMainMenu.run();
        });

        btnPanel.add(restartBtn);
        btnPanel.add(loadBtn);
        btnPanel.add(menuBtn);

        btnPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        containerPanel.add(btnPanel);
    }

    public void setupGameOverMenu(String headingText, String subHeadingText, boolean showLoad, boolean isVictory) {
        removeAll(); // Clear previous components

        if (!isVictory && bgImage != null) {
            setupImagePanel(showLoad);
        } else {
            setupFallbackPanel(headingText, subHeadingText, showLoad, isVictory);
        }

        revalidate();
        repaint();
    }

    private void setupFallbackPanel(String headingText, String subHeadingText, boolean showLoad, boolean isVictory) {
        heading.setText(headingText);
        subHeading.setText(subHeadingText);
        loadBtn.setVisible(showLoad);

        if (isVictory) {
            containerPanel.setBackground(new Color(10, 35, 30, 230)); // Deep emerald-black transparent tint
            containerPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(50, 220, 180), 3), // Emerald border
                    BorderFactory.createEmptyBorder(30, 50, 30, 50)));
            heading.setForeground(new Color(255, 215, 0)); // Gold

            // Style buttons for victory
            styleButton(restartBtn, new Color(200, 255, 230), new Color(20, 80, 60), new Color(60, 200, 150));
            styleButton(menuBtn, new Color(200, 255, 230), new Color(20, 80, 60), new Color(60, 200, 150));
        } else {
            containerPanel.setBackground(new Color(40, 10, 10, 230)); // Deep red-black transparent tint
            containerPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 50, 50), 3), // Crimson border
                    BorderFactory.createEmptyBorder(30, 50, 30, 50)));
            heading.setForeground(new Color(255, 60, 60)); // Crimson

            // Style buttons for defeat
            styleButton(restartBtn, new Color(255, 200, 200), new Color(80, 20, 20), new Color(200, 60, 60));
            styleButton(menuBtn, new Color(255, 200, 200), new Color(80, 20, 20), new Color(200, 60, 60));
            styleButton(loadBtn, new Color(255, 200, 200), new Color(80, 20, 20), new Color(200, 60, 60));
        }

        add(containerPanel);
    }

    private void setupImagePanel(boolean showLoad) {
        float scale = 1.25f;
        int bgWidth = Math.round(602 * scale);
        int bgHeight = Math.round(414 * scale);

        int innerX = Math.round(204 * scale);
        int innerWidth = Math.round(214 * scale);
        int innerY = Math.round(152 * scale);
        int innerHeight = Math.round(233 * scale);

        int btnHeight = Math.round(52 * scale * 0.7f);

        // Pre-trim images to get accurate bounds and aspect ratio
        BufferedImage trimmedRestart = trimImage(restartImage);
        BufferedImage trimmedSave = trimImage(saveImage);
        BufferedImage trimmedMenu = trimImage(mainMenuImage);

        // Calculate precise widths to match aspect ratio
        int restartW = getWidthForHeight(trimmedRestart, btnHeight, Math.round(160 * scale));
        int saveW = getWidthForHeight(trimmedSave, btnHeight, Math.round(160 * scale));
        int menuW = getWidthForHeight(trimmedMenu, btnHeight, Math.round(160 * scale));

        // Use the maximum width to ensure all buttons are exactly the same size, scaled
        // down slightly to narrow them
        int btnWidth = Math.round(Math.max(restartW, Math.max(saveW, menuW)) * 0.9f);

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

        // Create custom image buttons
        ImageButton restartBtnImg = new ImageButton(trimmedRestart, "Restart Game");
        ImageButton loadBtnImg = new ImageButton(trimmedSave, "Load Save");
        ImageButton menuBtnImg = new ImageButton(trimmedMenu, "Main Menu");

        restartBtnImg.addActionListener(e -> {
            setVisible(false);
            if (onRestart != null)
                onRestart.run();
        });

        loadBtnImg.addActionListener(e -> showLoadDialog());

        menuBtnImg.addActionListener(e -> {
            setVisible(false);
            if (onMainMenu != null)
                onMainMenu.run();
        });

        if (showLoad) {
            int spacing = Math.round(18 * scale);
            int y0 = innerY + Math.round(2 * scale);
            int y1 = y0 + btnHeight + spacing;
            int y2 = y1 + btnHeight + spacing;

            restartBtnImg.setBounds(innerX + (innerWidth - btnWidth) / 2, y0, btnWidth, btnHeight);
            loadBtnImg.setBounds(innerX + (innerWidth - btnWidth) / 2, y1, btnWidth, btnHeight);
            menuBtnImg.setBounds(innerX + (innerWidth - btnWidth) / 2, y2, btnWidth, btnHeight);

            menuBoxPanel.add(restartBtnImg);
            menuBoxPanel.add(loadBtnImg);
            menuBoxPanel.add(menuBtnImg);
        } else {
            int spacing = Math.round(30 * scale);
            int y0 = innerY + Math.round(22 * scale);
            int y1 = y0 + btnHeight + spacing;

            restartBtnImg.setBounds(innerX + (innerWidth - btnWidth) / 2, y0, btnWidth, btnHeight);
            menuBtnImg.setBounds(innerX + (innerWidth - btnWidth) / 2, y1, btnWidth, btnHeight);

            menuBoxPanel.add(restartBtnImg);
            menuBoxPanel.add(menuBtnImg);
        }

        add(menuBoxPanel);
    }

    private void styleButton(JButton btn, Color fg, Color bg, Color border) {
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setBorder(BorderFactory.createLineBorder(border, 1));
    }

    private JButton createMenuButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setForeground(new Color(255, 200, 200)); // Pale rose
        btn.setBackground(new Color(80, 20, 20)); // Dark crimson background
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(new Color(200, 60, 60), 1));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(200, 40));
        return btn;
    }

    private void showLoadDialog() {
        List<GameState> saves = SaveManager.listSaves();
        if (saves.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No saved games found.", "Load Game", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        Frame parentFrame = (parentWindow instanceof Frame) ? (Frame) parentWindow : null;

        ui.LoadGameDialog dialog = new ui.LoadGameDialog(parentFrame, saves);
        dialog.setVisible(true);

        if (dialog.isLoaded()) {
            setVisible(false); // Hide the GameOverMenu overlay
            if (onLoadGame != null) {
                onLoadGame.accept(dialog.getSelectedState());
            }
        } else if (dialog.isDeleteRequested()) {
            GameState toDelete = dialog.getDeleteState();
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete " + toDelete.saveName + "?", "Confirm Delete",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                File f = new File("saves/" + toDelete.saveName + ".json");
                f.delete();
            }
            showLoadDialog(); // Refresh list
        }
    }

    private int getWidthForHeight(BufferedImage img, int targetHeight, int fallbackWidth) {
        if (img == null)
            return fallbackWidth;
        float aspect = img.getWidth() / (float) img.getHeight();
        return Math.round(targetHeight * aspect);
    }

    private BufferedImage trimImage(BufferedImage img) {
        if (img == null)
            return null;
        int w = img.getWidth(), h = img.getHeight();
        int top = 0, bottom = h - 1, left = 0, right = w - 1;
        try {
            outer: for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++)
                    if (((img.getRGB(x, y) >> 24) & 0xff) > 10) {
                        top = y;
                        break outer;
                    }
            outer: for (int y = h - 1; y >= 0; y--)
                for (int x = 0; x < w; x++)
                    if (((img.getRGB(x, y) >> 24) & 0xff) > 10) {
                        bottom = y;
                        break outer;
                    }
            outer: for (int x = 0; x < w; x++)
                for (int y = 0; y < h; y++)
                    if (((img.getRGB(x, y) >> 24) & 0xff) > 10) {
                        left = x;
                        break outer;
                    }
            outer: for (int x = w - 1; x >= 0; x--)
                for (int y = 0; y < h; y++)
                    if (((img.getRGB(x, y) >> 24) & 0xff) > 10) {
                        right = x;
                        break outer;
                    }
        } catch (Exception e) {
            return img;
        }
        if (right <= left || bottom <= top)
            return img;
        return img.getSubimage(left, top, right - left + 1, bottom - top + 1);
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Overlay a very dark background to gray out the action underneath
        g.setColor(new Color(0, 0, 0, 190));
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }

    // --- Inner class: Image Button ---
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

                // Interactive shift
                if (pressed) {
                    drawY += 1;
                } else if (hovered) {
                    drawY -= 3;
                }

                g2.drawImage(img, drawX, drawY, drawWidth, drawHeight, null);

                // Tint overlays
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
}
