package view.dialogs;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * WallSearchable nesnelere tıklandığında açılan premium görsel arama popup'ı.
 */
public class SearchPopupDialog extends JDialog {
    private boolean searchTriggered = false;
    private BufferedImage bgImage;
    private BufferedImage searchImage;
    private BufferedImage cancelImage;
    private final String objectName;

    private final Runnable onSearchConfirm;

    public SearchPopupDialog(Frame owner, String objectName, Runnable onSearchConfirm) {
        super(owner, "Search " + objectName, false); // modal = false
        this.objectName = objectName;
        this.onSearchConfirm = onSearchConfirm;
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0)); // Şeffaf arka plan

        loadImages();

        // 0.35x scaling factor for perfect screen fit next to tiles
        float scale = 0.35f;
        int width = Math.round(612 * scale);
        int height = Math.round(408 * scale);
        setSize(width, height);
        if (owner != null) {
            setLocationRelativeTo(owner);
        } else {
            // Fallback for headless/no-owner environments
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation((screenSize.width - width) / 2, (screenSize.height - height) / 2);
        }

        JPanel contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                if (bgImage != null) {
                    g2.drawImage(bgImage, 0, 0, getWidth(), getHeight(), null);
                }

                // Centered object name inside the search box photo
                String displayTitle = objectName.toUpperCase();
                g2.setFont(new Font("Arial", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(displayTitle)) / 2;
                int textY = Math.round(getHeight() * 0.40f);

                // Premium drop shadow
                g2.setColor(new Color(0, 0, 0, 180));
                g2.drawString(displayTitle, textX + 1, textY + 1);

                // Gold text color matching original dungeon crawl aesthetics
                g2.setColor(new Color(255, 215, 0));
                g2.drawString(displayTitle, textX, textY);

                g2.dispose();
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setLayout(null);

        // Trim transparency paddings in button sprites
        BufferedImage trimmedSearch = trimImage(searchImage);
        BufferedImage trimmedCancel = trimImage(cancelImage);

        int btnHeight = Math.round(55 * scale);
        int searchW = getWidthForHeight(trimmedSearch, btnHeight, Math.round(150 * scale));
        int cancelW = getWidthForHeight(trimmedCancel, btnHeight, Math.round(150 * scale));

        ImageButton searchBtn = new ImageButton(trimmedSearch, "Search");
        ImageButton cancelBtn = new ImageButton(trimmedCancel, "Cancel");

        searchBtn.addActionListener(e -> {
            searchTriggered = true;
            dispose();
            if (onSearchConfirm != null) {
                onSearchConfirm.run();
            }
        });

        cancelBtn.addActionListener(e -> {
            searchTriggered = false;
            dispose();
        });

        // Horizontally center and vertically position buttons inside the box
        int spacing = Math.round(30 * scale);
        int totalWidth = searchW + cancelW + spacing;
        int startX = (width - totalWidth) / 2;
        int btnY = Math.round(getHeight() * 0.58f);

        searchBtn.setBounds(startX, btnY, searchW, btnHeight);
        cancelBtn.setBounds(startX + searchW + spacing, btnY, cancelW, btnHeight);

        contentPanel.add(searchBtn);
        contentPanel.add(cancelBtn);

        setContentPane(contentPanel);
    }

    private void loadImages() {
        bgImage = loadImg("resources/images/PopUpImages/SearchBox.png");
        searchImage = loadImg("resources/images/PopUpImages/SearchButton.png");
        cancelImage = loadImg("resources/images/PopUpImages/CancelButton.png");
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

    public boolean isSearchTriggered() {
        return searchTriggered;
    }

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

    private class ImageButton extends JButton {
        private BufferedImage img;
        private boolean hovered = false;
        private boolean pressed = false;

        public ImageButton(BufferedImage image, String fallbackText) {
            this.img = image;
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

                if (pressed) {
                    drawY += 1;
                } else if (hovered) {
                    drawY -= 3;
                }

                g2.drawImage(img, drawX, drawY, drawWidth, drawHeight, null);

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
