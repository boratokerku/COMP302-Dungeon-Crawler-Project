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
    private BufferedImage selectorImage;

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
            File imgFile = new File("resources/images/storage/inventory.png");
            if (imgFile.exists()) {
                bgImage = ImageIO.read(imgFile);
            }
            File selFile = new File("resources/images/storage/slot_selector.png");
            if (selFile.exists()) {
                selectorImage = ImageIO.read(selFile);
            }
        } catch (java.io.IOException e) {
            System.err.println("InventoryView: Could not load assets: " + e.getMessage());
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
        if (hero == null || hero.getInventory() == null)
            return;

        syncHotbarItems();

        // ── Layout ────────────────────────────────────────────────────────────
        if (bgImage != null) {
            double aspect = (double) bgImage.getHeight() / bgImage.getWidth();
            int naturalWidth = (int) Math.round(bgImage.getWidth() * SCALE_FACTOR);
            
            int maxWidth = Math.min(panelWidth - 40, 448);
            int maxHeight = (int) Math.round(maxWidth * aspect);

            double scale = SCALE_FACTOR;
            if (naturalWidth > maxWidth) {
                scale = (double) maxWidth / bgImage.getWidth();
            }
            if (bgImage.getHeight() * scale > maxHeight) {
                scale = (double) maxHeight / bgImage.getHeight();
            }

            barWidth = (int) Math.round(bgImage.getWidth() * scale);
            barHeight = (int) Math.round(bgImage.getHeight() * scale);
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

        double scale = (bgImage != null) ? (double) barWidth / bgImage.getWidth() : 1.0;

        for (int col = 0; col < slotsX; col++) {
            int slotIndex = col + 1;
            
            int slotX, slotY, slotWidthScaled, slotHeightScaled;
            if (bgImage != null) {
                int slotLeft = 178 + col * 281;
                slotX = startX + (int) Math.round(slotLeft * scale);
                slotWidthScaled = (int) Math.round(192 * scale);
                slotY = startY + (int) Math.round(15 * scale);
                slotHeightScaled = (int) Math.round(400 * scale);
            } else {
                slotX = startX + (col * slotWidth);
                slotY = startY;
                slotWidthScaled = slotWidth;
                slotHeightScaled = slotHeight;
            }

            if (hotbar.getSelectedSlot() == slotIndex) {
                if (selectorImage != null) {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    g.drawImage(selectorImage, slotX, slotY, slotWidthScaled, slotHeightScaled, null);
                } else {
                    g.setColor(new Color(255, 255, 255, 170));
                    g.drawRect(slotX + 1, slotY + 1, slotWidthScaled - 3, slotHeightScaled - 3);
                }
            }

            domain.models.entity.GameObject item = hotbar.getSlot(slotIndex);
            if (item != null) {
                drawItemInSlot(g, item, slotX, slotY, slotWidthScaled, slotHeightScaled);
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

    /**
     * Draws the sprite (or a colour placeholder) of a game object inside a slot.
     */
    private void drawItemInSlot(Graphics2D g, domain.models.entity.GameObject item,
            int slotX, int slotY, int slotW, int slotH) {
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

        int currentItemSize = Math.max(12, Math.min(slotW, slotH) - 8);

        if (sprite != null) {
            int renderSize = currentItemSize;
            if (item instanceof domain.models.item.usables.PotionItem
                    || item instanceof domain.models.item.wearables.RingItem) {
                renderSize = (int) (currentItemSize * 0.7); // Potions & rings render 30% smaller
            }
            int iw = sprite.getWidth();
            int ih = sprite.getHeight();
            double scale = Math.min((double) renderSize / iw, (double) renderSize / ih);
            int dw = (int) (iw * scale);
            int dh = (int) (ih * scale);
            int drawX = slotX + (slotW - dw) / 2;
            int drawY = slotY + (slotH - dh) / 2;
            g.drawImage(sprite, drawX, drawY, dw, dh, null);
        } else {
            // Colour placeholder
            if (item instanceof domain.models.item.usables.ShadowCloneScroll) {
                g.setColor(new Color(150, 50, 255)); // purple — scroll
            } else {
                g.setColor(new Color(255, 220, 50)); // yellow — unknown
            }
            int drawX = slotX + (slotW - currentItemSize) / 2;
            int drawY = slotY + (slotH - currentItemSize) / 2;
            g.fillOval(drawX, drawY, currentItemSize, currentItemSize);
        }
    }

    // ── Hit-testing ───────────────────────────────────────────────────────────

    /** Returns the hotbar item that was clicked, or null if the slot is empty. */
    public domain.models.entity.GameObject getClickedItem(int screenX, int screenY) {
        if (hero == null || hero.getInventory() == null)
            return null;
        syncHotbarItems();

        if (bgImage != null) {
            double scale = (double) barWidth / bgImage.getWidth();
            for (int col = 0; col < slotsX; col++) {
                int slotIndex = col + 1;
                int slotLeft = 178 + col * 281;
                int slotX = startX + (int) Math.round(slotLeft * scale);
                int slotWidthScaled = (int) Math.round(192 * scale);
                int slotY = startY + (int) Math.round(15 * scale);
                int slotHeightScaled = (int) Math.round(400 * scale);

                if (screenX >= slotX && screenX <= slotX + slotWidthScaled &&
                        screenY >= slotY && screenY <= slotY + slotHeightScaled) {
                    hotbar.setSelectedSlot(slotIndex);
                    return hotbar.getSlot(slotIndex);
                }
            }
        } else {
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
