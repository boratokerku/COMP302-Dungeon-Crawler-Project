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
    private final Runnable onResume;
    private final Runnable onMainMenu;

    public PauseMenu(Hero hero, List<Entity> entities, GameMap map,
                     Runnable onResume, Runnable onMainMenu) {
        this.hero = hero;
        this.entities = entities;
        this.map = map;
        this.onResume = onResume;
        this.onMainMenu = onMainMenu;

        setOpaque(false); // Arka planı saydam tut
        setLayout(new GridBagLayout()); // Butonları ortalar

        initButtons();
        setVisible(false); // Başlangıçta gizli
    }

    private void initButtons() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 1, 0, 12)); // 3 satır, aralıklı
        panel.setBackground(new Color(30, 20, 10, 220)); // Koyu kahve, yarı saydam
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 140, 60), 2),
                BorderFactory.createEmptyBorder(20, 40, 20, 40)
        ));

        JButton resumeBtn = createMenuButton("Devam Et");
        JButton saveBtn   = createMenuButton("Oyunu Kaydet");
        JButton menuBtn   = createMenuButton("Ana Menü");

        resumeBtn.addActionListener(e -> {
            setVisible(false);
            if (onResume != null) onResume.run();
        });

        saveBtn.addActionListener(e -> {
            // Oyuncudan save ismi iste
            String name = JOptionPane.showInputDialog(this, "Save ismi girin:", "Kaydet", JOptionPane.PLAIN_MESSAGE);
            if (name != null && !name.trim().isEmpty()) {
                SaveManager.save(name.trim(), hero, entities, map);
                JOptionPane.showMessageDialog(this, "Oyun kaydedildi: " + name.trim(), "Kayıt Başarılı", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        menuBtn.addActionListener(e -> {
            setVisible(false);
            if (onMainMenu != null) onMainMenu.run();
        });

        panel.add(resumeBtn);
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
