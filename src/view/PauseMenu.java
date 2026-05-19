package view;

import domain.logic.SaveManager;
import domain.models.entity.Entity;
import domain.models.entity.Hero;
import domain.models.map.GameMap;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Oyun içi duraklat menüsü.
 * JFrame'in glass pane'i olarak kullanılır — oyunun üstüne yarı saydam overlay olarak çizilir.
 * ESC tuşuyla açılıp kapanır.
 */
public class PauseMenu extends JPanel {

    private final Hero hero;
    private final List<Entity> entities;
    private final GameMap map;
    private final domain.logic.EnemySpawner enemySpawner;
    private final domain.logic.ScrollSpawner scrollSpawner;
    private final Runnable onResume;
    private final Runnable onRestart;
    private final Runnable onMainMenu;

    public PauseMenu(Hero hero, List<Entity> entities, GameMap map,
                     domain.logic.EnemySpawner enemySpawner, domain.logic.ScrollSpawner scrollSpawner,
                     Runnable onResume, Runnable onRestart, Runnable onMainMenu) {
        this.hero = hero;
        this.entities = entities;
        this.map = map;
        this.enemySpawner = enemySpawner;
        this.scrollSpawner = scrollSpawner;
        this.onResume = onResume;
        this.onRestart = onRestart;
        this.onMainMenu = onMainMenu;

        setOpaque(false); // Arka planı saydam tut
        setLayout(new GridBagLayout()); // Butonları ortalar

        initButtons();
        setVisible(false); // Başlangıçta gizli
    }

    private void initButtons() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 1, 0, 12)); // 4 satır, aralıklı
        panel.setBackground(new Color(30, 20, 10, 220)); // Koyu kahve, yarı saydam
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 140, 60), 2),
                BorderFactory.createEmptyBorder(20, 40, 20, 40)
        ));

        JButton resumeBtn  = createMenuButton("Resume");
        JButton restartBtn = createMenuButton("Restart Game");
        JButton saveBtn    = createMenuButton("Save Game");
        JButton menuBtn    = createMenuButton("Main Menu");

        resumeBtn.addActionListener(e -> {
            setVisible(false);
            if (onResume != null) onResume.run();
        });

        restartBtn.addActionListener(e -> {
            setVisible(false);
            if (onRestart != null) onRestart.run();
        });

        saveBtn.addActionListener(e -> {
            // Mevcut save'leri listele
            java.util.List<domain.models.GameState> existingSaves = domain.logic.SaveManager.listSaves();
            String[] options = new String[existingSaves.size() + 1];
            options[0] = "[ New Save ]";
            for (int i = 0; i < existingSaves.size(); i++) {
                options[i + 1] = existingSaves.get(i).saveName + "  —  " + existingSaves.get(i).timestamp;
            }

            String selected = (String) JOptionPane.showInputDialog(
                    this,
                    "Create a new save or overwrite an existing one:",
                    "Save Game",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            if (selected == null) return;

            String saveName;
            if ("[ New Save ]".equals(selected)) {
                saveName = JOptionPane.showInputDialog(this, "Enter save name:", "New Save", JOptionPane.PLAIN_MESSAGE);
                if (saveName == null || saveName.trim().isEmpty()) return;
                saveName = saveName.trim();
            } else {
                // Seçilen save'in adını çıkar (format: "name  —  date")
                int idx = java.util.Arrays.asList(options).indexOf(selected) - 1;
                saveName = existingSaves.get(idx).saveName;
            }

            SaveManager.save(saveName, hero, entities, map, enemySpawner, scrollSpawner);
            JOptionPane.showMessageDialog(this, "Saved successfully: " + saveName, "Save Successful", JOptionPane.INFORMATION_MESSAGE);
        });

        menuBtn.addActionListener(e -> {
            setVisible(false);
            if (onMainMenu != null) onMainMenu.run();
        });

        panel.add(resumeBtn);
        panel.add(restartBtn);
        panel.add(saveBtn);
        panel.add(menuBtn);

        add(panel);
    }

    private JButton createMenuButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setForeground(new Color(255, 220, 100)); // Altın sarısı yazı
        btn.setBackground(new Color(60, 40, 20));    // Koyu kahve arka plan
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(new Color(180, 140, 60), 1));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Ekranın tamamını koyu yarı saydam renkle kapla
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }
}
