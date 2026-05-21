package domain.models.entity;

public class DoubleCrate extends Crate {
    public DoubleCrate(String name, int x, int y) {
        super(name, x, y);
        this.imageName = "double_crate";
    }
}
