package domain.models.entity;

public class DoubleCrate extends Crate {
    public DoubleCrate(String name, int x, int y) {
        super(name, x, y);
        this.imageName = "double_crate";
    }

    @Override
    public DoubleCrate clone() {
        DoubleCrate cloned = new DoubleCrate(this.name, this.x, this.y);
        cloned.setCustomScale(this.customScale);
        if (this.getHiddenItem() != null) {
            cloned.setHiddenItem(this.getHiddenItem().clone());
        }
        return cloned;
    }
}
