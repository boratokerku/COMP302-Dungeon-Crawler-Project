package ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class SaveMapDialog extends JDialog {
    private BufferedImage bgImage;
    private BufferedImage bookIcon;
    private BufferedImage confirmButtonImage;
    private BufferedImage cancelButtonImage;

    private JTextField nameField;
    private JCheckBox overwriteCheck;
    private JComboBox<String> mapCombo;
    private JLabel nameLabel;
    private JLabel comboLabel;
    private ImageButton confirmBtn;
    private ImageButton cancelBtn;

    private boolean saved = false;
    private String resultMapName = null;

    private static final int DIALOG_W = 460;
    private static final int DIALOG_H = 330;

    public SaveMapDialog(Frame owner, List<String> existingMaps) {
        super(owner, "Save Map", true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setSize(DIALOG_W, DIALOG_H);

        loadImages();

        // Initial setup
        nameField = new JTextField(15);
        nameField.setFont(getRetroFont(Font.PLAIN, 18f));
        nameField.setBackground(new Color(30, 15, 22));
        nameField.setForeground(new Color(255, 235, 180));
        nameField.setCaretColor(Color.WHITE);
        nameField.setBorder(BorderFactory.createLineBorder(new Color(180, 140, 60), 1));

        overwriteCheck = new JCheckBox("Overwrite Existing Map");
        overwriteCheck.setFont(getRetroFont(Font.BOLD, 18f));
        overwriteCheck.setForeground(new Color(255, 215, 0));
        overwriteCheck.setOpaque(false);
        overwriteCheck.setFocusPainted(false);

        nameLabel = new JLabel("Map Name:");
        nameLabel.setFont(getRetroFont(Font.BOLD, 18f));
        nameLabel.setForeground(Color.LIGHT_GRAY);

        comboLabel = new JLabel("Select Map:");
        comboLabel.setFont(getRetroFont(Font.BOLD, 18f));
        comboLabel.setForeground(Color.LIGHT_GRAY);
        comboLabel.setVisible(false);

        mapCombo = new JComboBox<>();
        mapCombo.setFont(getRetroFont(Font.PLAIN, 16f));
        mapCombo.setBackground(new Color(30, 15, 22));
        mapCombo.setForeground(new Color(255, 235, 180));
        mapCombo.setBorder(BorderFactory.createLineBorder(new Color(180, 140, 60), 1));
        mapCombo.setVisible(false);

        for (String m : existingMaps) {
            mapCombo.addItem(m);
        }

        // Layout container
        JPanel contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bgImage != null) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    
                    // Draw background frame to fit the window exactly
                    g2.drawImage(bgImage, 0, 0, getWidth(), getHeight(), null);

                    // Draw Book decoration icon on the left
                    if (bookIcon != null) {
                        g2.drawImage(bookIcon, 35, 98, 48, 48, null);
                    }

                    // Render custom retro title with shadow
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    Font titleFont = getRetroFont(Font.BOLD, 30f);
                    g2.setFont(titleFont);
                    String titleText = "SAVE MAP";
                    FontMetrics fm = g2.getFontMetrics();
                    int titleX = (getWidth() - fm.stringWidth(titleText)) / 2;
                    int titleY = 62;

                    // Shadow
                    g2.setColor(Color.BLACK);
                    g2.drawString(titleText, titleX + 2, titleY + 2);

                    // Foreground (gold)
                    g2.setColor(new Color(255, 215, 0));
                    g2.drawString(titleText, titleX, titleY);

                    g2.dispose();
                }
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setLayout(null);

        // Position components
        nameLabel.setBounds(100, 103, 100, 24);
        contentPanel.add(nameLabel);

        nameField.setBounds(210, 103, 210, 24);
        contentPanel.add(nameField);

        overwriteCheck.setBounds(96, 143, 260, 25);
        contentPanel.add(overwriteCheck);

        comboLabel.setBounds(100, 183, 100, 24);
        contentPanel.add(comboLabel);

        mapCombo.setBounds(210, 183, 210, 24);
        contentPanel.add(mapCombo);

        // Buttons
        BufferedImage trimmedConfirm = trimImage(confirmButtonImage);
        BufferedImage trimmedCancel = trimImage(cancelButtonImage);

        float scale = 0.6f;
        int btnHeight = Math.round(55 * scale);
        int confirmW = getWidthForHeight(trimmedConfirm, btnHeight, Math.round(130 * scale));
        int cancelW = getWidthForHeight(trimmedCancel, btnHeight, Math.round(130 * scale));

        confirmBtn = new ImageButton(trimmedConfirm, "Save");
        cancelBtn = new ImageButton(trimmedCancel, "Cancel");

        // Center the buttons horizontally at the bottom (fixed position)
        int totalBtnWidth = confirmW + cancelW + 20;
        int startX = (DIALOG_W - totalBtnWidth) / 2;
        int btnY = 260;

        confirmBtn.setBounds(startX, btnY, confirmW, btnHeight);
        cancelBtn.setBounds(startX + confirmW + 20, btnY, cancelW, btnHeight);

        contentPanel.add(confirmBtn);
        contentPanel.add(cancelBtn);

        // Listener for expansion (fixed dialog size, just toggle visibility of combo box)
        overwriteCheck.addActionListener(e -> {
            boolean selected = overwriteCheck.isSelected();
            comboLabel.setVisible(selected);
            mapCombo.setVisible(selected);
            if (selected) {
                if (mapCombo.getItemCount() > 0) {
                    nameField.setText((String) mapCombo.getSelectedItem());
                    nameField.setEnabled(false);
                }
            } else {
                nameField.setEnabled(true);
                nameField.setText("");
            }
        });

        mapCombo.addActionListener(e -> {
            if (overwriteCheck.isSelected() && mapCombo.getSelectedItem() != null) {
                nameField.setText((String) mapCombo.getSelectedItem());
            }
        });

        confirmBtn.addActionListener(e -> {
            String text = nameField.getText().trim();
            if (text.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a valid map name.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            saved = true;
            resultMapName = text;
            dispose();
        });

        cancelBtn.addActionListener(e -> {
            saved = false;
            dispose();
        });

        setContentPane(contentPanel);
        setLocationRelativeTo(owner);
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
        bgImage = trimImage(loadImg("resources/images/PopUpImages/BlankDialogBox.png"));
        bookIcon = loadImg("resources/images/items/readings/book.png");
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

    public boolean isSaved() {
        return saved;
    }

    public String getMapName() {
        return resultMapName;
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
