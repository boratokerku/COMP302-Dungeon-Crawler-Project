import domain.models.item.ArmorItem;
public class testArmor {
    public static void main(String[] args) {
        try {
            ArmorItem armor = new ArmorItem(0, 0);
            System.out.println("Success! Sprite: " + armor.getSprite());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
