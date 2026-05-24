package domain.models.inventory;

import domain.models.entity.GameObject;

public class InventoryHotbar {
    public static final int SLOT_COUNT = 8;

    private final GameObject[] slots;
    private int selectedSlot;

    public InventoryHotbar() {
        this.slots = new GameObject[SLOT_COUNT];
        this.selectedSlot = 1;
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public void setSelectedSlot(int selectedSlot) {
        if (selectedSlot < 1 || selectedSlot > SLOT_COUNT) {
            throw new IllegalArgumentException("selectedSlot must be between 1 and 8 inclusive");
        }
        this.selectedSlot = selectedSlot;
    }

    public GameObject getItemInSelectedSlot() {
        return slots[selectedSlot - 1];
    }

    public GameObject getSlot(int slotIndex) {
        validateSlotIndex(slotIndex);
        return slots[slotIndex - 1];
    }

    public void setItemInSlot(int slotIndex, GameObject item) {
        validateSlotIndex(slotIndex);
        slots[slotIndex - 1] = item;
    }

    private void validateSlotIndex(int slotIndex) {
        if (slotIndex < 1 || slotIndex > SLOT_COUNT) {
            throw new IllegalArgumentException("slotIndex must be between 1 and 8 inclusive");
        }
    }

    /**
     * @requires offset is any integer value
     * @modifies selectedSlot
     * @effects Advances the selected hotbar slot by offset positions and wraps
     *          the result within the inclusive range 1..8. Positive offsets move
     *          forward toward slot 8; negative offsets move backward toward slot 1.
     */
    public void scroll(int offset) {
        selectedSlot = Math.floorMod((selectedSlot - 1) + offset, SLOT_COUNT) + 1;
    }
}