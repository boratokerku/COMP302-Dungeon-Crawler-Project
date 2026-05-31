package domain.models.item;

import org.junit.jupiter.api.Test;

import domain.models.item.usables.EnergyPotion;
import domain.models.item.wearables.RingItem;
import domain.models.item.wearables.GreenRing;
import domain.models.item.wearables.RedRing;
import domain.models.item.wearables.BlueRing;
import domain.models.entity.Hero;
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

    @Test
    public void testGreenRingProperties() {
        RingItem item = new RingItem(new GreenRing("Ring of Might"), 1, 1, "images/items/ring/green_ring.png");
        assertEquals("Ring of Might", item.getName());
        assertEquals(0, item.getStrBonus());
        assertEquals(0, item.getHpBonus());
        assertEquals(10, item.getEnergyBonus());
        assertEquals(0, item.getManaCostReduction());
    }

    @Test
    public void testHeroEquipGreenRing_MaxEnergyIncreasesTo110() {
        Hero hero = new Hero(0, 0);
        assertEquals(100, hero.getMaxEnergy());
        assertEquals(100, hero.getEnergy());

        RingItem greenRing = new RingItem(new GreenRing("Energy Ring"), 0, 0, "images/items/ring/green_ring.png");
        hero.equipRing(greenRing);
        assertEquals(110, hero.getMaxEnergy());
        assertEquals(110, hero.getEnergy()); // Immediate effect

        // Consume energy first
        hero.setEnergy(90);
        PotionItem energyPotion = new PotionItem(new EnergyPotion("Energy Potion", 30), 0, 0, "images/items/potion/green_potion.png");
        energyPotion.getPotion().use(hero);
        assertEquals(110, hero.getEnergy());

        // Unequip ring subtracts Energy back to 100
        hero.unequipRing();
        assertEquals(100, hero.getMaxEnergy());
        assertEquals(100, hero.getEnergy());
    }

    @Test
    public void testRedRingProperties() {
        RingItem item = new RingItem(new RedRing("Red Ring"), 2, 2, "images/items/ring/red_ring.png");
        assertEquals("Red Ring", item.getName());
        assertEquals(0, item.getStrBonus());
        assertEquals(5, item.getHpBonus());
        assertEquals(0, item.getManaCostReduction());
    }

    @Test
    public void testBlueRingProperties() {
        RingItem item = new RingItem(new BlueRing("Blue Ring"), 3, 3, "images/items/ring/blue_ring.png");
        assertEquals("Blue Ring", item.getName());
        assertEquals(0, item.getStrBonus());
        assertEquals(0, item.getHpBonus());
        assertEquals(1, item.getManaCostReduction());
    }

    @Test
    public void testHeroEquipRedRing_MaxHealthIncreasesTo22() {
        Hero hero = new Hero(0, 0);
        assertEquals(17, hero.getMaxHp());
        assertEquals(17, hero.getHp());

        RingItem redRing = new RingItem(new RedRing("Red Ring"), 0, 0, "images/items/ring/red_ring.png");
        hero.equipRing(redRing);
        assertEquals(22, hero.getMaxHp());
        assertEquals(22, hero.getHp()); // Immediate effect

        // Unequip ring subtracts HP back to 17
        hero.unequipRing();
        assertEquals(17, hero.getMaxHp());
        assertEquals(17, hero.getHp());
    }

    @Test
    public void testPassiveRingsInInventory() {
        Hero hero = new Hero(0, 0);
        assertEquals(17, hero.getMaxHp());
        assertEquals(17, hero.getHp());
        assertEquals(100, hero.getMaxEnergy());
        assertEquals(100, hero.getEnergy());
        assertEquals(0, hero.getRingManaCostReduction());

        // Add Red Ring to inventory
        RingItem redRing = new RingItem(new RedRing("Red Ring"), 0, 0, "images/items/ring/red_ring.png");
        hero.getInventory().addItem(redRing);
        assertEquals(22, hero.getMaxHp());
        assertEquals(22, hero.getHp());

        // Add Green Ring to inventory
        RingItem greenRing = new RingItem(new GreenRing("Energy Ring"), 0, 0, "images/items/ring/green_ring.png");
        hero.getInventory().addItem(greenRing);
        assertEquals(110, hero.getMaxEnergy());
        assertEquals(110, hero.getEnergy());

        // Add Blue Ring to inventory
        RingItem blueRing = new RingItem(new BlueRing("Blue Ring"), 0, 0, "images/items/ring/blue_ring.png");
        hero.getInventory().addItem(blueRing);
        assertEquals(1, hero.getRingManaCostReduction());

        // Remove Red Ring from inventory
        hero.getInventory().removeItem(redRing);
        assertEquals(17, hero.getMaxHp());
        assertEquals(17, hero.getHp());

        // Remove Green Ring from inventory
        hero.getInventory().removeItem(greenRing);
        assertEquals(100, hero.getMaxEnergy());
        assertEquals(100, hero.getEnergy());
    }
}
