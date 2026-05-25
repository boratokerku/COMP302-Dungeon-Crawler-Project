package view;

import domain.models.entity.Hero;
import domain.models.inventory.InventoryHotbar;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Handles always-visible hotbar rendering (8 slots) and slot interactions.
 */
public class InventoryView {

    private static final double SCALE_FACTOR = 2.0;

    // ── Dependencies ─────────────────────────────────────────────────────────
    private final Hero hero;
    private TileManager tileManager; // set after construction via setter

    // ── Layout constants ──────────────────────────────────────────────────────
    private final int slotsX = InventoryHotbar.SLOT_COUNT;

    // ── Computed positions (needed for click hit-testing) ─────────────────────
    private int startX;
    private int startY;

    private final InventoryHotbar hotbar;

    // ── Assets ────────────────────────────────────────────────────────────────
    private BufferedImage bgImage;

    // ── Runtime layout cache ──────────────────────────────────────────────────
    private int barWidth;
    private int barHeight;
    private int slotWidth;
    private int slotHeight;
    private int itemSize;

    // ── Constructor ───────────────────────────────────────────────────────────
    public InventoryView(Hero hero) {
        this.hero = hero;
        this.hotbar = new InventoryHotbar();
        loadBackgroundImage();
    }

    private void loadBackgroundImage() {
        try {
            File imgFile = new File("resources/images/storage/toolbar.png");
            if (imgFile.exists()) {
                bgImage = ImageIO.read(imgFile);
            }
        } catch (java.io.IOException e) {
            System.err.println("InventoryView: Could not load toolbar.png: " + e.getMessage());
        }
    }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setTileManager(TileManager tileManager) {
        this.tileManager = tileManager;
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    /**
    * Draws the hotbar at the bottom of the screen.
     *
     * @param g           the Graphics2D context
     * @param panelWidth  current width of the game panel
     * @param panelHeight current height of the game panel
     */
    public void draw(Graphics2D g, int panelWidth, int panelHeight) {
        if (hero == null || hero.getInventory() == null) return;

        syncHotbarItems();

        // ── Layout ────────────────────────────────────────────────────────────
        if (bgImage != null) {
            barWidth = (int) Math.round(bgImage.getWidth() * SCALE_FACTOR);
            barHeight = (int) Math.round(bgImage.getHeight() * SCALE_FACTOR);
        } else {
            barWidth = Math.min(panelWidth - 40, 320) * 2;
            barHeight = 32 * 2;
        }

        slotWidth = Math.max(1, barWidth / slotsX);
        slotHeight = barHeight;
        itemSize = Math.max(12, Math.min(slotWidth, slotHeight) - 8);

        startX = (panelWidth - barWidth) / 2;
        startY = panelHeight - barHeight - 14;

        // ── Background ───────────────────────────────────────────────────────
        if (bgImage != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(bgImage, startX, startY, barWidth, barHeight, null);
        } else {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(startX, startY, barWidth, barHeight);
        }

        for (int col = 0; col < slotsX; col++) {
            int slotIndex = col + 1;
            int slotX = startX + (col * slotWidth);
            int slotY = startY;

            if (hotbar.getSelectedSlot() == slotIndex) {
                g.setColor(new Color(255, 255, 255, 170));
                g.drawRect(slotX + 1, slotY + 1, slotWidth - 3, slotHeight - 3);
            }

            domain.models.entity.GameObject item = hotbar.getSlot(slotIndex);
            if (item != null) {
                drawItemInSlot(g, item, slotX, slotY);
            }
        }
    }

    private void syncHotbarItems() {
        java.util.List<domain.models.entity.GameObject> items = hero.getInventory().getItems();
        for (int i = 0; i < InventoryHotbar.SLOT_COUNT; i++) {
            domain.models.entity.GameObject item = i < items.size() ? items.get(i) : null;
            hotbar.setItemInSlot(i + 1, item);
        }
    }

    /** Draws the sprite (or a colour placeholder) of a game object inside a slot. */
    private void drawItemInSlot(Graphics2D g, domain.models.entity.GameObject item,
                                int slotX, int slotY) {
        if (item == null) {
            return;
        }

        BufferedImage sprite = null;

        if (item instanceof domain.models.item.MapItem mapItem) {
            sprite = mapItem.getSprite();
        }
        if (sprite == null && item.getImageName() != null && tileManager != null) {
            sprite = tileManager.getTile(item.getImageName());
        }

        if (sprite != null) {
            int renderSize = itemSize;
            if (item instanceof domain.models.item.PotionItem || item instanceof domain.models.item.RingItem) {
                renderSize = (int) (itemSize * 0.7); // Potions & rings render 30% smaller
            }
            int drawX = slotX + (slotWidth - renderSize) / 2;
            int drawY = slotY + (slotHeight - renderSize) / 2;
            g.drawImage(sprite, drawX, drawY, renderSize, renderSize, null);
        } else {
            // Colour placeholder
            if (item instanceof domain.models.item.ShadowCloneScroll) {
                g.setColor(new Color(150, 50, 255)); // purple — scroll
            } else {
                g.setColor(new Color(255, 220, 50)); // yellow — unknown
            }
            int drawX = slotX + (slotWidth - itemSize) / 2;
            int drawY = slotY + (slotHeight - itemSize) / 2;
            g.fillOval(drawX, drawY, itemSize, itemSize);
        }
    }

    // ── Hit-testing ───────────────────────────────────────────────────────────

    /** Returns the hotbar item that was clicked, or null if the slot is empty. */
    public domain.models.entity.GameObject getClickedItem(int screenX, int screenY) {
        if (hero == null || hero.getInventory() == null) return null;
        syncHotbarItems();

        for (int col = 0; col < slotsX; col++) {
            int slotIndex = col + 1;
            int slotX = startX + (col * slotWidth);
            int slotY = startY;

            if (screenX >= slotX && screenX <= slotX + slotWidth &&
                    screenY >= slotY && screenY <= slotY + slotHeight) {
                hotbar.setSelectedSlot(slotIndex);
                return hotbar.getSlot(slotIndex);
            }
        }
        return null;
    }

    public void scrollSelection(int offset) {
        hotbar.scroll(offset);
    }

    public void selectSlot(int slot) {
        if (slot >= 1 && slot <= InventoryHotbar.SLOT_COUNT) {
            hotbar.setSelectedSlot(slot);
        }
    }

    public int getSelectedSlot() {
        return hotbar.getSelectedSlot();
    }
}
