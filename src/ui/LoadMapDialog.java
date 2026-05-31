package ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class LoadMapDialog extends JDialog {
    private BufferedImage bgImage;
    private BufferedImage loadButtonImage;
    private BufferedImage cancelButtonImage;

    private JList<String> mapList;
    private ImageButton loadBtn;
    private ImageButton cancelBtn;
    private RetroTextButton deleteBtn;

    private boolean loaded = false;
    private String selectedMapName = null;
    private boolean deleteRequested = false;
    private String deleteMapName = null;

    private static final int DIALOG_W = 460;
    private static final int DIALOG_H = 330;

    public LoadMapDialog(Frame owner, List<String> mapNames) {
        super(owner, "Load Map", true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setSize(DIALOG_W, DIALOG_H);

        loadImages();

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

                    // Render custom retro title with shadow
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    Font titleFont = getRetroFont(Font.BOLD, 30f);
                    g2.setFont(titleFont);
                    String titleText = "LOAD MAP";
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

        // JList and ScrollPane for map names
        String[] labels = mapNames.toArray(new String[0]);

        mapList = new JList<>(labels);
        mapList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mapList.setSelectedIndex(0);
        mapList.setFont(getRetroFont(Font.PLAIN, 20f));
        mapList.setBackground(new Color(30, 15, 22));
        mapList.setForeground(new Color(255, 235, 180));
        mapList.setSelectionBackground(new Color(150, 50, 80));
        mapList.setSelectionForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(mapList);
        scrollPane.setBounds(35, 85, 390, 165);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(180, 140, 60), 1));
        contentPanel.add(scrollPane);

        // Buttons
        BufferedImage trimmedLoad = trimImage(loadButtonImage);
        BufferedImage trimmedCancel = trimImage(cancelButtonImage);

        float scale = 0.6f;
        int btnHeight = Math.round(55 * scale);
        int loadW = getWidthForHeight(trimmedLoad, btnHeight, Math.round(130 * scale));
        int cancelW = getWidthForHeight(trimmedCancel, btnHeight, Math.round(130 * scale));

        loadBtn = new ImageButton(trimmedLoad, "Load");
        cancelBtn = new ImageButton(trimmedCancel, "Cancel");

        // Custom stylized Delete button
        deleteBtn = new RetroTextButton("Delete");

        // Position buttons at the bottom (centered horizontally)
        int totalBtnWidth = loadW + 90 + cancelW + 30; // 15px gaps
        int startX = (DIALOG_W - totalBtnWidth) / 2;
        int btnY = 260;

        loadBtn.setBounds(startX, btnY, loadW, btnHeight);
        deleteBtn.setBounds(startX + loadW + 15, btnY, 90, btnHeight);
        cancelBtn.setBounds(startX + loadW + 15 + 90 + 15, btnY, cancelW, btnHeight);

        contentPanel.add(loadBtn);
        contentPanel.add(deleteBtn);
        contentPanel.add(cancelBtn);

        // Action listeners
        loadBtn.addActionListener(e -> {
            int idx = mapList.getSelectedIndex();
            if (idx >= 0) {
                loaded = true;
                selectedMapName = mapNames.get(idx);
                dispose();
            }
        });

        deleteBtn.addActionListener(e -> {
            int idx = mapList.getSelectedIndex();
            if (idx >= 0) {
                deleteRequested = true;
                deleteMapName = mapNames.get(idx);
                dispose();
            }
        });

        cancelBtn.addActionListener(e -> {
            loaded = false;
            dispose();
        });

        // Double click to load
        mapList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int idx = mapList.getSelectedIndex();
                    if (idx >= 0) {
                        loaded = true;
                        selectedMapName = mapNames.get(idx);
                        dispose();
                    }
                }
            }
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
        loadButtonImage = loadImg("resources/images/PopUpImages/ConfirmButton_Build.png");
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

    // A custom button with premium retro styling
    private class RetroTextButton extends JButton {
        private boolean hovered = false;
        private boolean pressed = false;

        public RetroTextButton(String text) {
            super(text);
            setFont(getRetroFont(Font.BOLD, 18f));
            setForeground(new Color(255, 120, 120)); // Reddish pink
            setBackground(new Color(60, 20, 20));
            setBorder(BorderFactory.createLineBorder(new Color(180, 140, 60), 2));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
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
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int drawY = 0;
            if (pressed) {
                drawY += 1;
                g2.setColor(new Color(40, 10, 10));
            } else if (hovered) {
                drawY -= 2;
                g2.setColor(new Color(80, 25, 25));
            } else {
                g2.setColor(new Color(60, 20, 20));
            }

            g2.fillRect(0, 0, getWidth(), getHeight());

            // Draw border
            g2.setColor(new Color(180, 140, 60));
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

            // Draw text
            g2.setFont(getFont());
            g2.setColor(getForeground());
            FontMetrics fm = g2.getFontMetrics();
            int tx = (getWidth() - fm.stringWidth(getText())) / 2;
            int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent() + drawY;
            g2.drawString(getText(), tx, ty);

            g2.dispose();
        }
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

    public boolean isLoaded() {
        return loaded;
    }

    public String getSelectedMapName() {
        return selectedMapName;
    }

    public boolean isDeleteRequested() {
        return deleteRequested;
    }

    public String getDeleteMapName() {
        return deleteMapName;
    }
}
