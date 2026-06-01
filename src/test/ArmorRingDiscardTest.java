package test;

import domain.models.entity.Hero;
import domain.models.item.wearables.ArmorItem;
import domain.models.item.wearables.RingItem;
import domain.models.item.wearables.RedRing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArmorRingDiscardTest {

    private Hero hero;

    @BeforeEach
    public void setUp() {
        hero = new Hero(1, 1);
        // Reset/init stats
        hero.setDef(2);
        hero.setStr(10);
    }

    @Test
    public void testArmorInstantEffectAndDiscard() {
        // Base defense should be 2
        assertEquals(2, hero.getDef());

        ArmorItem armor = new ArmorItem(0, 0);
        // Add to inventory
        hero.getInventory().addItem(armor);

        // Armor has defBonus of 4. Total def should be 2 + 4 = 6 immediately
        assertEquals(6, hero.getDef());

        // Equipping it (via wear action)
        hero.equipArmor(armor);
        assertEquals(armor, hero.getEquippedArmor());

        // Discarding (removing from inventory)
        hero.getInventory().removeItem(armor);

        // Def should drop back to 2, and the armor should be automatically unequipped
        assertEquals(2, hero.getDef());
        assertNull(hero.getEquippedArmor());
    }

    @Test
    public void testRingInstantEffectAndDiscard() {
        // Base max HP should be 17
        assertEquals(17, hero.getMaxHp());

        RingItem ring = new RingItem(new RedRing("Health Ring"), 0, 0, "images/items/ring/red_ring.png");
        // Add to inventory
        hero.getInventory().addItem(ring);

        // RedRing has hpBonus of 5. Max HP should be 17 + 5 = 22 immediately
        assertEquals(22, hero.getMaxHp());

        // Equipping it (via wear action)
        hero.equipRing(ring);
        assertEquals(ring, hero.getEquippedRing());

        // Discarding (removing from inventory)
        hero.getInventory().removeItem(ring);

        // Max HP should drop back to 17, and the ring should be automatically unequipped
        assertEquals(17, hero.getMaxHp());
        assertNull(hero.getEquippedRing());
    }
}
