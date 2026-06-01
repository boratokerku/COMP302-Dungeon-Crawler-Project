package domain.models.staticObjects;
import domain.models.GameObject;

public class Sign extends GameObject {
    public Sign(String name, int x, int y) {
        super(name, x, y, "sign/sign_brown", false);
    }

    public Sign(String name, int x, int y, String imageName) {
        super(name, x, y, imageName, false);
    }
}
