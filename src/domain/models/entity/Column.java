package domain.models.entity;

public class Column extends GameObject {
    public Column(String name, int x, int y) {
        super(name, x, y, "column", false);
    }

    public Column(String name, int x, int y, String imageName) {
        super(name, x, y, imageName, false);
    }
}
