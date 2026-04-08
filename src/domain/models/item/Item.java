package domain.models.item;

public abstract class Item {
    protected String name;
    protected double weight; // Dokümanda envanter limiti olabilir demiştik

    public Item(String name, double weight) {
        this.name = name;
        this.weight = weight;
    }

    // Her eşyanın kullanımı farklıdır (İksir içilir, kitap okunur)
    public abstract void use(domain.models.entity.Hero hero);

    public String getName() {
        return name;
    }
}