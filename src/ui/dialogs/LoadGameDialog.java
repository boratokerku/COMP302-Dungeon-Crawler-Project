package ui.dialogs;

import domain.models.GameState;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class LoadGameDialog extends JDialog {
    private BufferedImage bgImage;
    private BufferedImage loadButtonImage;
    private BufferedImage cancelButtonImage;
    private BufferedImage deleteButtonImage;

    private JList<String> saveList;
    private ImageButton loadBtn;
    private ImageButton cancelBtn;
    private ImageButton deleteBtn;

    private boolean loaded = false;
    private GameState selectedState = null;
    private boolean deleteRequested = false;
    private GameState deleteState = null;

    private static final int INSET = 60; // 9-slice boundary insets

    public LoadGameDialog(Frame owner, List<GameState> saves) {
        super(owner, "Load Game", true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        loadImages();

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

        setSize(400, 340);
        setLocationRelativeTo(owner);

        // JList and ScrollPane for save records
        String[] labels = saves.stream()
                .map(s -> s.saveName + "  —  " + s.timestamp)
                .toArray(String[]::new);

        saveList = new JList<>(labels);
        saveList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        if (!saves.isEmpty()) {
            saveList.setSelectedIndex(0);
        }
        saveList.setFont(getRetroFont(Font.PLAIN, 20f));
        saveList.setBackground(new Color(40, 20, 30));
        saveList.setForeground(Color.WHITE);
        saveList.setSelectionBackground(new Color(150, 60, 90));
        saveList.setSelectionForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(saveList);
        scrollPane.setBounds(30, 80, 340, 180);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(150, 100, 120), 1));
        contentPanel.add(scrollPane);

        // Buttons
        BufferedImage trimmedLoad = trimImage(loadButtonImage);
        BufferedImage trimmedCancel = trimImage(cancelButtonImage);
        BufferedImage trimmedDelete = trimImage(deleteButtonImage);

        float scale = 0.45f;
        int btnHeight = Math.round(55 * scale);
        int loadW = getWidthForHeight(trimmedLoad, btnHeight, Math.round(130 * scale));
        int cancelW = getWidthForHeight(trimmedCancel, btnHeight, Math.round(130 * scale));
        int deleteW = getWidthForHeight(trimmedDelete, btnHeight, Math.round(130 * scale));

        loadBtn = new ImageButton(trimmedLoad, "Load");
        cancelBtn = new ImageButton(trimmedCancel, "Cancel");
        deleteBtn = new ImageButton(trimmedDelete, "Delete");

        // Position buttons at the bottom (centered horizontally)
        int totalBtnWidth = loadW + deleteW + cancelW + 30; // 15px gaps
        int startX = (400 - totalBtnWidth) / 2;
        loadBtn.setBounds(startX, 270, loadW, btnHeight);
        deleteBtn.setBounds(startX + loadW + 15, 270, deleteW, btnHeight);
        cancelBtn.setBounds(startX + loadW + 15 + deleteW + 15, 270, cancelW, btnHeight);

        contentPanel.add(loadBtn);
        contentPanel.add(deleteBtn);
        contentPanel.add(cancelBtn);

        // Action listeners
        loadBtn.addActionListener(e -> {
            int idx = saveList.getSelectedIndex();
            if (idx >= 0) {
                loaded = true;
                selectedState = saves.get(idx);
                dispose();
            }
        });

        deleteBtn.addActionListener(e -> {
            int idx = saveList.getSelectedIndex();
            if (idx >= 0) {
                deleteRequested = true;
                deleteState = saves.get(idx);
                dispose();
            }
        });

        cancelBtn.addActionListener(e -> {
            loaded = false;
            dispose();
        });

        // Double click to load
        saveList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int idx = saveList.getSelectedIndex();
                    if (idx >= 0) {
                        loaded = true;
                        selectedState = saves.get(idx);
                        dispose();
                    }
                }
            }
        });

        setContentPane(contentPanel);
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
        bgImage = loadImg("resources/images/PopUpImages/LoadGameBox.png");
        loadButtonImage = loadImg("resources/images/PopUpImages/ConfirmButton_Build.png");
        cancelButtonImage = loadImg("resources/images/PopUpImages/CancelButton.png");
        deleteButtonImage = loadImg("resources/images/PopUpImages/DeleteButton.png");
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

    private void drawNineSlice(Graphics g, BufferedImage img, int x, int y, int width, int height, int top, int right,
            int bottom, int left) {
        int iw = img.getWidth();
        int ih = img.getHeight();

        int[] sx = { 0, left, iw - right, iw };
        int[] sy = { 0, top, ih - bottom, ih };

        int[] dx = { x, x + left, x + width - right, x + width };
        int[] dy = { y, y + top, y + height - bottom, y + height };

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                g.drawImage(img, dx[c], dy[r], dx[c + 1], dy[r + 1], sx[c], sy[r], sx[c + 1], sy[r + 1], null);
            }
        }
    }

    public boolean isLoaded() {
        return loaded;
    }

    public GameState getSelectedState() {
        return selectedState;
    }

    public boolean isDeleteRequested() {
        return deleteRequested;
    }

    public GameState getDeleteState() {
        return deleteState;
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
