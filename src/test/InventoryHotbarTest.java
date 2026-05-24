package test;

import domain.models.inventory.InventoryHotbar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InventoryHotbarTest {

    private InventoryHotbar hotbar;

    @BeforeEach
    public void setUp() {
        hotbar = new InventoryHotbar();
    }

    @Test
    public void testScrollWithinBoundaries_ShouldUpdateSelectedSlot() {
        hotbar.setSelectedSlot(4);

        hotbar.scroll(2);

        assertEquals(6, hotbar.getSelectedSlot(), "Scrolling within bounds should move to the expected slot");
    }

    @Test
    public void testScrollPastUpperBound_ShouldWrapToFirstSlot() {
        hotbar.setSelectedSlot(8);

        hotbar.scroll(1);

        assertEquals(1, hotbar.getSelectedSlot(), "Scrolling past slot 8 should wrap to slot 1");
    }

    @Test
    public void testScrollPastLowerBound_ShouldWrapToLastSlot() {
        hotbar.setSelectedSlot(1);

        hotbar.scroll(-1);

        assertEquals(8, hotbar.getSelectedSlot(), "Scrolling below slot 1 should wrap to slot 8");
    }
}