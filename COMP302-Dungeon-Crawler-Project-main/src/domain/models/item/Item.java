package domain.models.item;

import domain.models.entity.GameObject;
import domain.models.action.Interactable;
import domain.models.action.Action;
import java.util.List;
import java.util.ArrayList;

public abstract class Item extends GameObject implements Interactable {
    protected String name;
    protected double weight; // Dokümanda envanter limiti olabilir demiştik
    protected List<Action> actions;

    public Item(int x, int y, String imageName, String name, double weight) {
        super(x, y, imageName, true); // true = passable
        this.name = name;
        this.weight = weight;
        this.actions = new ArrayList<>();
    }

    // Her eşyanın kullanımı farklıdır (İksir içilir, kitap okunur)
    public abstract void use(domain.models.entity.Hero hero);

    public String getName() {
        return name;
    }

    @Override
    public List<Action> getActions() {
        return actions;
    }

    public void addAction(Action action) {
        this.actions.add(action);
    }
}