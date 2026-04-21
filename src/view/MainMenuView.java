package view;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class MainMenuView extends JPanel {

    private Runnable onStartGame;
    private Image backgroundImage;

    public MainMenuView(Runnable onStartGame) {
        this.onStartGame = onStartGame;
        try {
            File bgFile = new File("resources/images/main_menu_bg.png");
            if (bgFile.exists()) {
                backgroundImage = ImageIO.read(bgFile);
            } else {
                bgFile = new File("resources/images/main_menu_bg.jpg");
                if (bgFile.exists()) {
                    backgroundImage = ImageIO.read(bgFile);
                }
            }
        } catch (Exception e) {
            System.err.println("Arka plan resmi yuklenemedi: " + e.getMessage());
        }
        initUI();
    }

    private void initUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.BLACK);

        // Add some space at the top to push buttons below the background image's title
        add(Box.createVerticalStrut(250));

        // Buttons
        JButton startBtn = createImageButton("resources/images/start_game_button.png");
        JButton helpBtn = createImageButton("resources/images/help_button.png");
        JButton quitBtn = createImageButton("resources/images/quit_button.png");

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
        add(Box.createVerticalStrut(10));
        add(helpBtn);
        add(Box.createVerticalStrut(10));
        add(quitBtn);

        // Fill remaining space
        add(Box.createVerticalGlue());
    }

    private JButton createImageButton(String imagePath) {
        JButton btn = new JButton();
        try {
            BufferedImage originalImg = ImageIO.read(new File(imagePath));
            BufferedImage trimmedImg = trimImage(originalImg);
            Image scaledImg = trimmedImg.getScaledInstance(320, 80, Image.SCALE_SMOOTH);
            btn.setIcon(new ImageIcon(scaledImg));
        } catch (Exception e) {
            System.err.println("Buton resmi yuklenemedi: " + imagePath);
        }
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.setMaximumSize(new Dimension(320, 80));
        btn.setPreferredSize(new Dimension(320, 80));

        return btn;
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
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}
