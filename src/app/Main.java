package app;

import view.DesignModeView;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.image.BufferedImage;
import view.MainMenuView;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("COMP302 Dungeon Crawler");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Set window icon
            try {
                java.io.File iconFile = new java.io.File("resources/images/icon.png");
                if (iconFile.exists()) {
                    BufferedImage rawIcon = javax.imageio.ImageIO.read(iconFile);
                    BufferedImage processedIcon = processIcon(rawIcon);
                    frame.setIconImage(processedIcon);

                    // Set macOS Dock icon if supported
                    try {
                        if (java.awt.Taskbar.isTaskbarSupported()) {
                            java.awt.Taskbar taskbar = java.awt.Taskbar.getTaskbar();
                            if (taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                                taskbar.setIconImage(processedIcon);
                            }
                        }
                    } catch (Exception e) {
                        // Ignore if Taskbar is not supported
                    }
                }
            } catch (Exception e) {
                System.err.println("Icon could not be loaded: " + e.getMessage());
            }

            CardLayout cardLayout = new CardLayout();
            JPanel mainPanel = new JPanel(cardLayout);

            MainMenuView menuView = new MainMenuView(
                    () -> GameEngine.startDesignMode(frame, mainPanel, cardLayout),
                    (state) -> GameEngine.loadGame(frame, mainPanel, cardLayout, state));
            menuView.setPreferredSize(new java.awt.Dimension(1250, 1000));

            mainPanel.setBackground(Color.BLACK);
            mainPanel.add(menuView, "Menu");
            cardLayout.show(mainPanel, "Menu");
            frame.add(mainPanel);

            frame.setSize(1250, 800);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static BufferedImage processIcon(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int top = 0, bottom = h - 1, left = 0, right = w - 1;
        try {
            // Top border
            outer: for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int rgb = img.getRGB(x, y);
                    int alpha = (rgb >> 24) & 0xff;
                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;
                    if (alpha > 10 && !(r > 240 && g > 240 && b > 240)) {
                        top = y;
                        break outer;
                    }
                }
            }
            // Bottom border
            outer: for (int y = h - 1; y >= 0; y--) {
                for (int x = 0; x < w; x++) {
                    int rgb = img.getRGB(x, y);
                    int alpha = (rgb >> 24) & 0xff;
                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;
                    if (alpha > 10 && !(r > 240 && g > 240 && b > 240)) {
                        bottom = y;
                        break outer;
                    }
                }
            }
            // Left border
            outer: for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    int rgb = img.getRGB(x, y);
                    int alpha = (rgb >> 24) & 0xff;
                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;
                    if (alpha > 10 && !(r > 240 && g > 240 && b > 240)) {
                        left = x;
                        break outer;
                    }
                }
            }
            // Right border
            outer: for (int x = w - 1; x >= 0; x--) {
                for (int y = 0; y < h; y++) {
                    int rgb = img.getRGB(x, y);
                    int alpha = (rgb >> 24) & 0xff;
                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;
                    if (alpha > 10 && !(r > 240 && g > 240 && b > 240)) {
                        right = x;
                        break outer;
                    }
                }
            }
        } catch (Exception e) {
            return img;
        }

        if (right <= left || bottom <= top)
            return img;

        // Extract the subimage and make white background transparent
        int subW = right - left + 1;
        int subH = bottom - top + 1;
        BufferedImage processed = new BufferedImage(subW, subH, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < subH; y++) {
            for (int x = 0; x < subW; x++) {
                int rgb = img.getRGB(left + x, top + y);
                int alpha = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                if (alpha < 10 || (r > 240 && g > 240 && b > 240)) {
                    processed.setRGB(x, y, 0x00000000);
                } else {
                    processed.setRGB(x, y, rgb);
                }
            }
        }
        return processed;
    }
}
