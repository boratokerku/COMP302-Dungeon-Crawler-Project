package ui.dialogs;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class DeleteConfirmDialog extends JDialog {
    private BufferedImage bgImage;
    private BufferedImage confirmButtonImage;
    private BufferedImage cancelButtonImage;
    private boolean confirmed = false;

    public DeleteConfirmDialog(Window owner, String message) {
        super(owner instanceof Frame ? (Frame) owner : null, "Confirm Delete", ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        loadImages();

        int dialogW = 380;
        int dialogH = 220;
        if (bgImage != null) {
            dialogW = bgImage.getWidth();
            dialogH = bgImage.getHeight();
        }
        setSize(dialogW, dialogH);
        setLocationRelativeTo(owner);

        final int finalDialogW = dialogW;
        final int finalDialogH = dialogH;

        JPanel contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bgImage != null) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.drawImage(bgImage, 0, 0, getWidth(), getHeight(), null);
                    g2.dispose();
                }
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setLayout(null);

        // Buttons
        BufferedImage trimmedConfirm = trimImage(confirmButtonImage);
        BufferedImage trimmedCancel = trimImage(cancelButtonImage);

        float scale = 0.55f;
        int btnHeight = Math.round(55 * scale);
        int confirmW = getWidthForHeight(trimmedConfirm, btnHeight, Math.round(130 * scale));
        int cancelW = getWidthForHeight(trimmedCancel, btnHeight, Math.round(130 * scale));

        ImageButton confirmBtn = new ImageButton(trimmedConfirm, "Yes");
        ImageButton cancelBtn = new ImageButton(trimmedCancel, "No");

        int totalBtnWidth = confirmW + cancelW + 20;
        int startX = (dialogW - totalBtnWidth) / 2;
        int btnY = Math.round(dialogH * 0.70f);

        confirmBtn.setBounds(startX, btnY, confirmW, btnHeight);
        cancelBtn.setBounds(startX + confirmW + 20, btnY, cancelW, btnHeight);

        contentPanel.add(confirmBtn);
        contentPanel.add(cancelBtn);

        confirmBtn.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        cancelBtn.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        setContentPane(contentPanel);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private Font getRetroFont(int style, float size) {
        try {
            File fontFile = new File("resources/fonts/VT323-Regular.ttf");
            if (!fontFile.exists()) {
                fontFile = new File("../resources/fonts/VT323-Regular.ttf");
            }
            if (fontFile.exists()) {
                Font f = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                return f.deriveFont(style, size);
            }
        } catch (Exception e) {
            System.err.println("Could not load retro font: " + e.getMessage());
        }
        return new Font("Monospaced", style, (int) size);
    }

    private void loadImages() {
        bgImage = loadImg("resources/images/PopUpImages/DeletePopUp.png");
        confirmButtonImage = loadImg("resources/images/PopUpImages/ConfirmButton_Build.png");
        cancelButtonImage = loadImg("resources/images/PopUpImages/CancelButton.png");
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
                setFont(getRetroFont(Font.BOLD, 18f));
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
