package view;

import domain.models.entity.Hero;
import domain.models.inventory.InventoryHotbar;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Handles always-visible hotbar rendering (8 slots) and slot interactions.
 */
public class InventoryView {

    // ── Dependencies ─────────────────────────────────────────────────────────
    private final Hero hero;
    private TileManager tileManager; // set after construction via setter

    // ── Layout constants ──────────────────────────────────────────────────────
    private final int slotSize = 44;
    private final int padding = 6;
    private final int slotsX = InventoryHotbar.SLOT_COUNT;

    // ── Computed positions (needed for click hit-testing) ─────────────────────
    private int startX;
    private int startY;

    private final InventoryHotbar hotbar;

    // ── Assets ────────────────────────────────────────────────────────────────
    private BufferedImage bgImage;

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
                bgImage = trimTransparentMargins(ImageIO.read(imgFile));
            }
        } catch (Exception e) {
            System.err.println("InventoryView: Could not load toolbar.png: " + e.getMessage());
        }
    }

    private BufferedImage trimTransparentMargins(BufferedImage src) {
        if (src == null) return null;

        int minX = src.getWidth();
        int minY = src.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int alpha = (src.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha > 0) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return src;
        }

        return src.getSubimage(minX, minY, (maxX - minX) + 1, (maxY - minY) + 1);
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
        int barWidth = slotsX * slotSize + (slotsX + 1) * padding;
        int barHeight = slotSize + (2 * padding);

        startX = (panelWidth - barWidth) / 2;
        startY = panelHeight - barHeight - 14;

        // ── Background ───────────────────────────────────────────────────────
        if (bgImage != null) {
            g.drawImage(bgImage, startX, startY, barWidth, barHeight, null);
        } else {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRoundRect(startX, startY, barWidth, barHeight, 10, 10);
            g.setColor(new Color(200, 200, 200));
            g.drawRoundRect(startX, startY, barWidth, barHeight, 10, 10);
        }

        for (int col = 0; col < slotsX; col++) {
            int slotX = startX + padding + col * (slotSize + padding);
            int slotY = startY + padding;
            int slotIndex = col + 1;

            drawSlot(g, slotX, slotY, hotbar.getSelectedSlot() == slotIndex);

            domain.models.entity.GameObject item = hotbar.getSlot(slotIndex);
            if (item != null) {
                drawItemInSlot(g, item, slotX, slotY);
            }

            g.setColor(new Color(255, 255, 255, 180));
            g.setFont(g.getFont().deriveFont(11f));
            g.drawString(String.valueOf(slotIndex), slotX + 3, slotY + 12);
        }
    }

    private void syncHotbarItems() {
        java.util.List<domain.models.entity.GameObject> items = hero.getInventory().getItems();
        for (int i = 0; i < InventoryHotbar.SLOT_COUNT; i++) {
            domain.models.entity.GameObject item = i < items.size() ? items.get(i) : null;
            hotbar.setItemInSlot(i + 1, item);
        }
    }

    /** Draws a single slot rectangle. Gold border for selected slot, grey for normal. */
    private void drawSlot(Graphics2D g, int x, int y, boolean selected) {
        g.setColor(selected ? new Color(70, 55, 20, 230) : new Color(45, 45, 45, 210));
        g.fillRect(x, y, slotSize, slotSize);
        g.setColor(selected ? new Color(255, 220, 90) : new Color(110, 110, 110));
        g.drawRect(x, y, slotSize, slotSize);
    }

    /** Draws the sprite (or a colour placeholder) of a game object inside a slot. */
    private void drawItemInSlot(Graphics2D g, domain.models.entity.GameObject item,
                                int slotX, int slotY) {
        if (item == null) {
            return;
        }

        BufferedImage sprite = null;

        if (item instanceof domain.models.item.MapItem) {
            sprite = ((domain.models.item.MapItem) item).getSprite();
        }
        if (sprite == null && item.getImageName() != null && tileManager != null) {
            sprite = tileManager.getTile(item.getImageName());
        }

        if (sprite != null) {
            g.drawImage(sprite, slotX + 2, slotY + 2, slotSize - 4, slotSize - 4, null);
        } else {
            // Colour placeholder
            if (item instanceof domain.models.item.ShadowCloneScroll) {
                g.setColor(new Color(150, 50, 255)); // purple — scroll
            } else {
                g.setColor(new Color(255, 220, 50)); // yellow — unknown
            }
            g.fillOval(slotX + 5, slotY + 5, slotSize - 10, slotSize - 10);
        }
    }

    // ── Hit-testing ───────────────────────────────────────────────────────────

    /** Returns the hotbar item that was clicked, or null if the slot is empty. */
    public domain.models.entity.GameObject getClickedItem(int screenX, int screenY) {
        if (hero == null || hero.getInventory() == null) return null;
        syncHotbarItems();

        for (int col = 0; col < slotsX; col++) {
            int slotX = startX + padding + col * (slotSize + padding);
            int slotY = startY + padding;
            int slotIndex = col + 1;

            if (screenX >= slotX && screenX <= slotX + slotSize &&
                    screenY >= slotY && screenY <= slotY + slotSize) {
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
