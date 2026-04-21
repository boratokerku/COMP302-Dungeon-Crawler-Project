package view;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import javax.imageio.ImageIO;

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
        
        // Logo / Title
        JLabel titleLabel = new JLabel("Dungeon Crawler");
        titleLabel.setFont(new Font("Serif", Font.BOLD, 64));
        titleLabel.setForeground(Color.RED);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Add some space at the top
        add(Box.createVerticalStrut(100));
        add(titleLabel);
        add(Box.createVerticalStrut(100));
        
        // Buttons
        JButton startBtn = createStyledButton("Start Game");
        JButton helpBtn = createStyledButton("Help");
        JButton quitBtn = createStyledButton("Quit");
        
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
        add(Box.createVerticalStrut(20));
        add(helpBtn);
        add(Box.createVerticalStrut(20));
        add(quitBtn);
        
        // Fill remaining space
        add(Box.createVerticalGlue());
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFont(new Font("SansSerif", Font.BOLD, 24));
        btn.setFocusPainted(false);
        
        // Fix for macOS background and foreground color visibility on JButtons
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        
        Color gulKurusu = new Color(181, 101, 118);
        Color gulKurusuHover = new Color(201, 121, 138);
        
        btn.setBackground(gulKurusu);
        btn.setForeground(Color.RED);
        btn.setMaximumSize(new Dimension(250, 50));
        btn.setPreferredSize(new Dimension(250, 50));
        // Hover effect
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(gulKurusuHover);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(gulKurusu);
            }
        });
        return btn;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}
