package ui;

import domain.logic.SaveManager;
import domain.models.GameState;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class SaveGameDialog extends JDialog {
    private BufferedImage bgImage;
    private BufferedImage confirmButtonImage;
    private BufferedImage cancelButtonImage;

    private JTextField nameField;
    private JCheckBox overwriteCheck;
    private JComboBox<String> saveCombo;
    private JLabel comboLabel;
    private ImageButton confirmBtn;
    private ImageButton cancelBtn;

    private boolean saved = false;
    private String resultSaveName = null;

    private static final int INSET = 60; // 9-slice boundary insets

    public SaveGameDialog(Frame owner, List<GameState> existingSaves) {
        super(owner, "Save Game", true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        loadImages();

        // Initial setup
        nameField = new JTextField(15);
        nameField.setFont(new Font("Arial", Font.PLAIN, 14));
        nameField.setBackground(new Color(40, 20, 30));
        nameField.setForeground(Color.WHITE);
        nameField.setCaretColor(Color.WHITE);
        nameField.setBorder(BorderFactory.createLineBorder(new Color(150, 100, 120), 1));

        overwriteCheck = new JCheckBox("Overwrite Existing Save");
        overwriteCheck.setFont(new Font("Arial", Font.BOLD, 12));
        overwriteCheck.setForeground(new Color(255, 215, 0));
        overwriteCheck.setOpaque(false);
        overwriteCheck.setFocusPainted(false);

        comboLabel = new JLabel("Select Save:");
        comboLabel.setFont(new Font("Arial", Font.BOLD, 12));
        comboLabel.setForeground(Color.LIGHT_GRAY);
        comboLabel.setVisible(false);

        saveCombo = new JComboBox<>();
        saveCombo.setFont(new Font("Arial", Font.PLAIN, 12));
        saveCombo.setBackground(new Color(40, 20, 30));
        saveCombo.setForeground(Color.WHITE);
        saveCombo.setBorder(BorderFactory.createLineBorder(new Color(150, 100, 120), 1));
        saveCombo.setVisible(false);

        for (GameState s : existingSaves) {
            saveCombo.addItem(s.saveName);
        }

        // Layout container
        JPanel contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bgImage != null) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    drawNineSlice(g2, bgImage, 0, 0, getWidth(), getHeight(), INSET, INSET, INSET, INSET);
                    g2.dispose();
                }
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setLayout(null);

        // Title
        JLabel titleLabel = new JLabel("SAVE GAME");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(new Color(255, 215, 0));
        titleLabel.setBounds(30, 25, 200, 25);
        contentPanel.add(titleLabel);

        // Name Label & Field
        JLabel nameLabel = new JLabel("Save Name:");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        nameLabel.setForeground(Color.LIGHT_GRAY);
        nameLabel.setBounds(35, 65, 100, 20);
        contentPanel.add(nameLabel);

        nameField.setBounds(135, 65, 190, 24);
        contentPanel.add(nameField);

        // Overwrite checkbox
        overwriteCheck.setBounds(31, 100, 220, 25);
        contentPanel.add(overwriteCheck);

        // Overwrite label & combo box
        comboLabel.setBounds(35, 135, 100, 20);
        contentPanel.add(comboLabel);

        saveCombo.setBounds(135, 135, 190, 24);
        contentPanel.add(saveCombo);

        // Buttons
        BufferedImage trimmedConfirm = trimImage(confirmButtonImage);
        BufferedImage trimmedCancel = trimImage(cancelButtonImage);

        float scale = 0.6f;
        int btnHeight = Math.round(55 * scale);
        int confirmW = getWidthForHeight(trimmedConfirm, btnHeight, Math.round(130 * scale));
        int cancelW = getWidthForHeight(trimmedCancel, btnHeight, Math.round(130 * scale));

        confirmBtn = new ImageButton(trimmedConfirm, "Save");
        cancelBtn = new ImageButton(trimmedCancel, "Cancel");

        confirmBtn.setBounds(80, 185, confirmW, btnHeight);
        cancelBtn.setBounds(85 + confirmW, 185, cancelW, btnHeight);

        contentPanel.add(confirmBtn);
        contentPanel.add(cancelBtn);

        // Listener for expansion
        overwriteCheck.addActionListener(e -> {
            boolean selected = overwriteCheck.isSelected();
            comboLabel.setVisible(selected);
            saveCombo.setVisible(selected);
            if (selected) {
                if (saveCombo.getItemCount() > 0) {
                    nameField.setText((String) saveCombo.getSelectedItem());
                    nameField.setEnabled(false);
                }
                adjustSize(true);
            } else {
                nameField.setEnabled(true);
                nameField.setText("");
                adjustSize(false);
            }
        });

        saveCombo.addActionListener(e -> {
            if (overwriteCheck.isSelected() && saveCombo.getSelectedItem() != null) {
                nameField.setText((String) saveCombo.getSelectedItem());
            }
        });

        confirmBtn.addActionListener(e -> {
            String text = nameField.getText().trim();
            if (text.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a valid save name.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            saved = true;
            resultSaveName = text;
            dispose();
        });

        cancelBtn.addActionListener(e -> {
            saved = false;
            dispose();
        });

        setContentPane(contentPanel);
        adjustSize(false);
        setLocationRelativeTo(owner);
    }

    private void adjustSize(boolean expanded) {
        int w = 370;
        int h = expanded ? 290 : 230;
        setSize(w, h);
        int btnY = expanded ? 225 : 165;
        confirmBtn.setLocation(confirmBtn.getX(), btnY);
        cancelBtn.setLocation(cancelBtn.getX(), btnY);
        revalidate();
        repaint();
    }

    private void loadImages() {
        bgImage = loadImg("resources/images/PopUpImages/SaveGameBox.png");
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

    private void drawNineSlice(Graphics g, BufferedImage img, int x, int y, int width, int height, int top, int right, int bottom, int left) {
        int iw = img.getWidth();
        int ih = img.getHeight();

        int[] sx = {0, left, iw - right, iw};
        int[] sy = {0, top, ih - bottom, ih};

        int[] dx = {x, x + left, x + width - right, x + width};
        int[] dy = {y, y + top, y + height - bottom, y + height};

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                g.drawImage(img, dx[c], dy[r], dx[c+1], dy[r+1], sx[c], sy[r], sx[c+1], sy[r+1], null);
            }
        }
    }

    public boolean isSaved() {
        return saved;
    }

    public String getSaveName() {
        return resultSaveName;
    }

    private class ImageButton extends JButton {
        private BufferedImage img;
        private boolean hovered = false;
        private boolean pressed = false;

        public ImageButton(BufferedImage image, String fallbackText) {
            this.img = image;
            if (this.img == null) {
                setText(fallbackText);
                setFont(new Font("Arial", Font.BOLD, 14));
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
