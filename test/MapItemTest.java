package domain.models.item;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MapItemTest {

    @Test
    public void testCreateRandomWeapon_ReturnsNonNullAndCorrectCoordinates() {
        int x = 4;
        int y = 7;
        MapItem item = MapItem.createRandomWeapon(x, y);
        
        assertNotNull(item, "createRandomWeapon should return a non-null MapItem");
        assertEquals(x, item.getX(), "The item's X coordinate should match the input");
        assertEquals(y, item.getY(), "The item's Y coordinate should match the input");
    }

    @Test
    public void testCreateRandomItem_ReturnsNonNullAndCorrectCoordinates() {
        int x = 2;
        int y = 9;
        MapItem item = MapItem.createRandomItem(x, y);
        
        assertNotNull(item, "createRandomItem should return a non-null MapItem");
        assertEquals(x, item.getX(), "The item's X coordinate should match the input");
        assertEquals(y, item.getY(), "The item's Y coordinate should match the input");
    }

    @Test
    public void testDefaultStatsAndBonuses() {
        WoodenSwordItem weapon = new WoodenSwordItem(0, 0);
        
        assertFalse(weapon.isRanged(), "Wooden sword should not be ranged by default");
        assertEquals(0, weapon.getDefBonus(), "Melee weapon should have 0 defense bonus by default");
        assertEquals(0, weapon.getStrBonus(), "Melee weapon should have 0 strength bonus by default");
        assertEquals(0, weapon.getManaCost(), "Melee weapon should have 0 mana cost");
    }
}
