package view;

import domain.logic.SaveManager;
import domain.models.GameState;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Game Over overlay menu, drawn over the game.
 * Uses a transparent dark background with styled buttons to restart, load save, or exit to menu.
 */
public class GameOverMenu extends JPanel {

    private final Runnable onRestart;
    private final java.util.function.Consumer<GameState> onLoadGame;
    private final Runnable onMainMenu;

    private JPanel containerPanel;
    private JLabel heading;
    private JLabel subHeading;
    private JButton restartBtn;
    private JButton loadBtn;
    private JButton menuBtn;

    public GameOverMenu(Runnable onRestart, java.util.function.Consumer<GameState> onLoadGame, Runnable onMainMenu) {
        this.onRestart = onRestart;
        this.onLoadGame = onLoadGame;
        this.onMainMenu = onMainMenu;

        setOpaque(false); // Translucent background handled in paintComponent
        setLayout(new GridBagLayout()); // Center components

        initUI();
        setVisible(false); // Hidden by default
    }

    private void initUI() {
        containerPanel = new JPanel();
        containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.Y_AXIS));
        containerPanel.setBackground(new Color(40, 10, 10, 230)); // Deep red-black transparent tint
        containerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 50, 50), 3), // Crimson border
                BorderFactory.createEmptyBorder(30, 50, 30, 50)
        ));

        // Heading: GAME OVER
        heading = new JLabel("GAME OVER");
        heading.setFont(new Font("Arial", Font.BOLD, 48));
        heading.setForeground(new Color(255, 60, 60)); // Bright crimson
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        containerPanel.add(heading);

        containerPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Subheading
        subHeading = new JLabel("You have succumbed to your fate.");
        subHeading.setFont(new Font("Arial", Font.PLAIN, 18));
        subHeading.setForeground(Color.LIGHT_GRAY);
        subHeading.setAlignmentX(Component.CENTER_ALIGNMENT);
        containerPanel.add(subHeading);

        containerPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        // Buttons Panel
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new GridLayout(3, 1, 0, 15));
        btnPanel.setOpaque(false);

        restartBtn = createMenuButton("Restart Game");
        loadBtn    = createMenuButton("Load Save");
        menuBtn    = createMenuButton("Main Menu");

        restartBtn.addActionListener(e -> {
            setVisible(false);
            if (onRestart != null) onRestart.run();
        });

        loadBtn.addActionListener(e -> showLoadDialog());

        menuBtn.addActionListener(e -> {
            setVisible(false);
            if (onMainMenu != null) onMainMenu.run();
        });

        btnPanel.add(restartBtn);
        btnPanel.add(loadBtn);
        btnPanel.add(menuBtn);

        btnPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        containerPanel.add(btnPanel);

        add(containerPanel);
    }

    public void setupGameOverMenu(String headingText, String subHeadingText, boolean showLoad, boolean isVictory) {
        heading.setText(headingText);
        subHeading.setText(subHeadingText);
        loadBtn.setVisible(showLoad);

        if (isVictory) {
            containerPanel.setBackground(new Color(10, 35, 30, 230)); // Deep emerald-black transparent tint
            containerPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(50, 220, 180), 3), // Emerald border
                    BorderFactory.createEmptyBorder(30, 50, 30, 50)
            ));
            heading.setForeground(new Color(255, 215, 0)); // Gold

            // Style buttons for victory
            styleButton(restartBtn, new Color(200, 255, 230), new Color(20, 80, 60), new Color(60, 200, 150));
            styleButton(menuBtn, new Color(200, 255, 230), new Color(20, 80, 60), new Color(60, 200, 150));
        } else {
            containerPanel.setBackground(new Color(40, 10, 10, 230)); // Deep red-black transparent tint
            containerPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 50, 50), 3), // Crimson border
                    BorderFactory.createEmptyBorder(30, 50, 30, 50)
            ));
            heading.setForeground(new Color(255, 60, 60)); // Crimson

            // Style buttons for defeat
            styleButton(restartBtn, new Color(255, 200, 200), new Color(80, 20, 20), new Color(200, 60, 60));
            styleButton(menuBtn, new Color(255, 200, 200), new Color(80, 20, 20), new Color(200, 60, 60));
            styleButton(loadBtn, new Color(255, 200, 200), new Color(80, 20, 20), new Color(200, 60, 60));
        }

        containerPanel.revalidate();
        containerPanel.repaint();
    }

    private void styleButton(JButton btn, Color fg, Color bg, Color border) {
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setBorder(BorderFactory.createLineBorder(border, 1));
    }

    private JButton createMenuButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setForeground(new Color(255, 200, 200)); // Pale rose
        btn.setBackground(new Color(80, 20, 20));    // Dark crimson background
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(new Color(200, 60, 60), 1));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(200, 40));
        return btn;
    }

    private void showLoadDialog() {
        List<GameState> saves = SaveManager.listSaves();
        if (saves.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No saved games found.", "Load Game", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] labels = saves.stream()
                .map(s -> s.saveName + "  —  " + s.timestamp)
                .toArray(String[]::new);

        JDialog dialog = new JDialog((java.awt.Frame) null, "Load Game", true);
        dialog.setLayout(new java.awt.BorderLayout(10, 10));

        JList<String> list = new JList<>(labels);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setFont(new Font("Arial", Font.PLAIN, 14));

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(380, 200));
        dialog.add(scrollPane, java.awt.BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        JButton loadBtn2  = new JButton("Load");
        JButton deleteBtn = new JButton("Delete");
        JButton cancelBtn = new JButton("Cancel");

        loadBtn2.addActionListener(ev -> {
            int idx = list.getSelectedIndex();
            if (idx >= 0 && onLoadGame != null) {
                dialog.dispose();
                setVisible(false); // Hide the GameOverMenu overlay
                onLoadGame.accept(saves.get(idx));
            }
        });

        deleteBtn.addActionListener(ev -> {
            int idx = list.getSelectedIndex();
            if (idx >= 0) {
                int confirm = JOptionPane.showConfirmDialog(dialog,
                        "Are you sure you want to delete " + saves.get(idx).saveName + "?", "Confirm",
                        JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    File f = new File("saves/" + saves.get(idx).saveName + ".json");
                    if (f.delete()) {
                        dialog.dispose();
                        showLoadDialog(); // Refresh list
                    }
                }
            }
        });

        cancelBtn.addActionListener(ev -> dialog.dispose());

        btnPanel.add(loadBtn2);
        btnPanel.add(deleteBtn);
        btnPanel.add(cancelBtn);
        dialog.add(btnPanel, java.awt.BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Overlay a very dark background to gray out the action underneath
        g.setColor(new Color(0, 0, 0, 190));
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }
}
