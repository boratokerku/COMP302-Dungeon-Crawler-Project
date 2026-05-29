package ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class HelpDialog extends JDialog {
    private BufferedImage helpBoxImg;
    private BufferedImage objectiveBoxImg;
    private BufferedImage leftArrowImg;
    private BufferedImage rightArrowImg;
    private BufferedImage confirmImg;

    private int page = 1; // 1 = HelpBox, 2 = ObjectiveBox

    public HelpDialog(Frame owner) {
        super(owner, "Help & Objective", true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0)); // Transparent window background

        loadImages();

        int width = 577;
        int height = 433;
        setSize(width, height);
        setLocationRelativeTo(owner);

        JPanel contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                BufferedImage activeBg = (page == 1) ? helpBoxImg : objectiveBoxImg;
                if (activeBg != null) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.drawImage(activeBg, 0, 0, getWidth(), getHeight(), null);
                    g2.dispose();
                }
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setLayout(null);

        // Trim transparency from button assets for precise bounds
        BufferedImage trimmedLeft = trimImage(leftArrowImg);
        BufferedImage trimmedRight = trimImage(rightArrowImg);
        BufferedImage trimmedConfirm = trimImage(confirmImg);

        // Standard scaled sizes
        int arrowH = 45;
        int leftW = getWidthForHeight(trimmedLeft, arrowH, 48);
        int rightW = getWidthForHeight(trimmedRight, arrowH, 48);

        int confirmH = 35;
        int confirmW = getWidthForHeight(trimmedConfirm, confirmH, 125);

        ImageButton leftArrowBtn = new ImageButton(trimmedLeft, "<");
        ImageButton rightArrowBtn = new ImageButton(trimmedRight, ">");
        ImageButton confirmBtn = new ImageButton(trimmedConfirm, "Confirm");

        // Positions
        // below right for rightArrowBtn: page 1
        int marginX = 45;
        int marginY = 40;
        int btnY = height - arrowH - marginY;

        rightArrowBtn.setBounds(width - rightW - marginX, btnY, rightW, arrowH);

        // below left for leftArrowBtn: page 2
        leftArrowBtn.setBounds(marginX, btnY, leftW, arrowH);

        // right below for confirmBtn: page 2 (aligned vertically)
        int confirmY = height - confirmH - marginY;
        confirmBtn.setBounds(width - confirmW - marginX, confirmY, confirmW, confirmH);

        // Action Listeners
        rightArrowBtn.addActionListener(e -> {
            page = 2;
            leftArrowBtn.setVisible(true);
            confirmBtn.setVisible(true);
            rightArrowBtn.setVisible(false);
            contentPanel.repaint();
        });

        leftArrowBtn.addActionListener(e -> {
            page = 1;
            leftArrowBtn.setVisible(false);
            confirmBtn.setVisible(false);
            rightArrowBtn.setVisible(true);
            contentPanel.repaint();
        });

        confirmBtn.addActionListener(e -> {
            dispose();
        });

        // Set initial visibility
        leftArrowBtn.setVisible(false);
        confirmBtn.setVisible(false);
        rightArrowBtn.setVisible(true);

        contentPanel.add(leftArrowBtn);
        contentPanel.add(rightArrowBtn);
        contentPanel.add(confirmBtn);

        setContentPane(contentPanel);
    }

    private void loadImages() {
        helpBoxImg = loadImg("resources/images/HelperMenuImages/HelpBox.png");
        objectiveBoxImg = loadImg("resources/images/HelperMenuImages/ObjectiveBox.png");
        leftArrowImg = loadImg("resources/images/PopUpImages/LeftArrowButton.png");
        rightArrowImg = loadImg("resources/images/PopUpImages/RightArrowButton.png");
        confirmImg = loadImg("resources/images/PopUpImages/ConfirmButton.png");
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
