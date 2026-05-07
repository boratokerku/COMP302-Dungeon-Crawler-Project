package view;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import domain.models.entity.Hero;

/**
 * Handles all inventory rendering: the Inventory.png background,
 * the equipped-weapon slot, and the item grid.
 *
 * GameView delegates to this class so it stays focused on world rendering.
 */
public class InventoryView {

    // ── Dependencies ─────────────────────────────────────────────────────────
    private final Hero hero;
    private TileManager tileManager; // set after construction via setter

    // ── Layout constants ──────────────────────────────────────────────────────
    private final int slotSize  = 40;
    private final int padding   = 5;
    private final int slotsX    = 4;
    private final int slotsY    = 2;

    // ── Computed positions (needed for click hit-testing) ─────────────────────
    private int startX;
    private int gridStartY; // Y where the item grid rows begin

    // ── Assets ────────────────────────────────────────────────────────────────
    private BufferedImage bgImage;

    // ── Constructor ───────────────────────────────────────────────────────────
    public InventoryView(Hero hero) {
        this.hero = hero;
        loadBackgroundImage();
    }

    private void loadBackgroundImage() {
        try {
            File imgFile = new File("resources/images/storage/Inventory.png");
            if (imgFile.exists()) {
                bgImage = ImageIO.read(imgFile);
            }
        } catch (Exception e) {
            System.err.println("InventoryView: Could not load Inventory.png: " + e.getMessage());
        }
    }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setTileManager(TileManager tileManager) {
        this.tileManager = tileManager;
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    /**
     * Draws the full inventory panel (background + equipped slot + item grid).
     * Call this from GameView.paintComponent() when the inventory is visible.
     *
     * @param g           the Graphics2D context
     * @param panelWidth  current width of the game panel
     * @param panelHeight current height of the game panel
     */
    public void draw(Graphics2D g, int panelWidth, int panelHeight) {
        if (hero == null || hero.getInventory() == null) return;

        // ── Layout ────────────────────────────────────────────────────────────
        int invWidth  = slotsX * slotSize + (slotsX + 1) * padding;
        int invHeight = slotsY * slotSize + (slotsY + 1) * padding;

        // Space above the grid: "Equipped" label + one slot + gap
        int equippedAreaHeight = 14 + slotSize + padding;
        int totalHeight = equippedAreaHeight + invHeight;

        startX = panelWidth  - invWidth  - 20;
        int startY = panelHeight - totalHeight - 20;

        // ── Background ───────────────────────────────────────────────────────
        if (bgImage != null) {
            g.drawImage(bgImage, startX, startY, invWidth, totalHeight, null);
        } else {
            // Fallback dark panel
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRoundRect(startX, startY, invWidth, totalHeight, 10, 10);
            g.setColor(new Color(200, 200, 200));
            g.drawRoundRect(startX, startY, invWidth, totalHeight, 10, 10);
        }

        // ── Equipped slot ─────────────────────────────────────────────────────
        int equippedLabelY = startY + 12;
        g.setColor(Color.WHITE);
        g.drawString("Equipped", startX + padding, equippedLabelY);

        int equippedSlotX = startX + padding;
        int equippedSlotY = equippedLabelY + 2;

        drawSlot(g, equippedSlotX, equippedSlotY, true);

        domain.models.entity.GameObject equipped = hero.getEquippedWeapon();
        if (equipped != null) {
            drawItemInSlot(g, equipped, equippedSlotX, equippedSlotY);
        }

        // ── Item grid ────────────────────────────────────────────────────────
        gridStartY = equippedSlotY + slotSize + padding;

        java.util.List<domain.models.entity.GameObject> items =
                hero.getInventory().getItems();

        int itemIndex = 0;
        for (int row = 0; row < slotsY; row++) {
            for (int col = 0; col < slotsX; col++) {
                int slotX = startX   + padding + col * (slotSize + padding);
                int slotY = gridStartY + padding + row * (slotSize + padding);

                drawSlot(g, slotX, slotY, false);

                if (itemIndex < items.size()) {
                    drawItemInSlot(g, items.get(itemIndex), slotX, slotY);
                }
                itemIndex++;
            }
        }
    }

    /** Draws a single slot rectangle. Gold border for equipped, grey for normal. */
    private void drawSlot(Graphics2D g, int x, int y, boolean isEquippedSlot) {
        g.setColor(isEquippedSlot ? new Color(40, 30, 10, 220) : new Color(50, 50, 50, 200));
        g.fillRect(x, y, slotSize, slotSize);
        g.setColor(isEquippedSlot ? new Color(212, 175, 55) : new Color(100, 100, 100));
        g.drawRect(x, y, slotSize, slotSize);
    }

    /** Draws the sprite (or a colour placeholder) of a game object inside a slot. */
    private void drawItemInSlot(Graphics2D g, domain.models.entity.GameObject item,
                                int slotX, int slotY) {
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

    /**
     * Returns the item in the inventory slot that was clicked, or null if none.
     * Uses the last computed layout positions from draw().
     */
    public domain.models.entity.GameObject getClickedItem(int screenX, int screenY) {
        if (hero == null || hero.getInventory() == null) return null;

        java.util.List<domain.models.entity.GameObject> items =
                hero.getInventory().getItems();

        int itemIndex = 0;
        for (int row = 0; row < slotsY; row++) {
            for (int col = 0; col < slotsX; col++) {
                int slotX = startX    + padding + col * (slotSize + padding);
                int slotY = gridStartY + padding + row * (slotSize + padding);

                if (screenX >= slotX && screenX <= slotX + slotSize &&
                    screenY >= slotY && screenY <= slotY + slotSize) {
                    if (itemIndex < items.size()) {
                        return items.get(itemIndex);
                    }
                }
                itemIndex++;
            }
        }
        return null;
    }
}
