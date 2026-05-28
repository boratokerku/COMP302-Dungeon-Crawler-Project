package domain.models.item;

import org.junit.jupiter.api.Test;

import domain.models.item.usables.EnergyPotion;
import domain.models.item.usables.HealthPotion;
import domain.models.item.usables.ManaPotion;
import domain.models.item.usables.PotionItem;
import domain.models.item.wearables.WoodenSwordItem;

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

    @Test
    public void testHealthPotionItemProperties() {
        PotionItem item = new PotionItem(new HealthPotion("Red Potion", 5), 3, 5, "images/items/potion/red_potion.png");
        assertEquals("Red Potion", item.getName());
        assertEquals("images/items/potion/red_potion.png", item.getImageName());
        assertEquals(3, item.getX());
        assertEquals(5, item.getY());
        assertTrue(item.getPotion() instanceof HealthPotion);
    }

    @Test
    public void testManaPotionItemProperties() {
        PotionItem item = new PotionItem(new ManaPotion("Blue Potion", 20), 1, 2,
                "images/items/potion/blue_potion.png");
        assertEquals("Blue Potion", item.getName());
        assertEquals("images/items/potion/blue_potion.png", item.getImageName());
        assertTrue(item.getPotion() instanceof ManaPotion);
    }

    @Test
    public void testEnergyPotionItemProperties() {
        PotionItem item = new PotionItem(new EnergyPotion("Green Potion", 30), 9, 9,
                "images/items/potion/green_potion.png");
        assertEquals("Green Potion", item.getName());
        assertEquals("images/items/potion/green_potion.png", item.getImageName());
        assertTrue(item.getPotion() instanceof EnergyPotion);
    }

    @Test
    public void testCreateRandomPotionItem() {
        PotionItem pot = PotionItem.createRandomPotionItem(2, 2);
        assertNotNull(pot);
        assertNotNull(pot.getPotion());
        assertTrue(pot.getPotion() instanceof HealthPotion || pot.getPotion() instanceof ManaPotion
                || pot.getPotion() instanceof EnergyPotion);
    }
}
