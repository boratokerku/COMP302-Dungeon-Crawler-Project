package view;

import domain.models.entity.GameObject;
import domain.logic.Action;
import domain.models.entity.Hero;

import javax.swing.*;
import java.awt.*;

public class ActionMenu {

    private final Hero hero;
    private JPopupMenu popup;

    public ActionMenu(Hero hero) {
        this.hero = hero;
        this.popup = new JPopupMenu();
    }

    public void show(Component invoker, GameObject obj, int screenX, int screenY) {
        popup.removeAll();

        // Nesnenin adını başlık olarak ekle
        JLabel title = new JLabel(obj.getName(), SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        popup.add(title);
        popup.addSeparator();

        // Her aksiyon için bir buton ekle
        for (Action action : obj.getActions()) {
            JMenuItem item = new JMenuItem(action.getName() + " " + obj.getName());

            if (!action.isAvailable(hero, obj)) {
                item.setEnabled(false); // inventory dolu veya action kullanılamaz
                item.setText(item.getText() + " [Unavailable]");
            } else {
                item.addActionListener(e -> {
                    action.execute(hero, obj);
                    System.out.println(action.getName() + " executed on " + obj.getName());
                    invoker.repaint(); // Ekranı güncelle ki eşya haritadan silinsin
                });
            }
            popup.add(item);
        }

        // Fare konumunda göster
        if (popup.getComponentCount() > 2) { 
            popup.show(invoker, screenX, screenY);
        } else {
            JMenuItem noActionItem = new JMenuItem("No actions available");
            noActionItem.setEnabled(false);
            popup.add(noActionItem);
            popup.show(invoker, screenX, screenY);
        }
    }

    public void hideMenu() {
        popup.setVisible(false);
    }
}
