package view;

import domain.models.entity.GameObject;
import domain.logic.Action;
import domain.models.entity.Hero;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ActionMenu {

    private final Hero hero;
    private boolean visible;
    private int x, y;
    private int width, height;
    private GameObject targetObject;
    private List<Action> actions = new ArrayList<>();
    private int hoveredIndex = -1;

    private BufferedImage bgImg;
    private BufferedImage smallBgImg;
    private Font vtFont;

    private final int padding = 15;
    private final int lineSpacing = 24;
    private final int titleHeight = 28;

    public ActionMenu(Hero hero) {
        this.hero = hero;
        this.visible = false;

        try {
            File imgFile = new File("resources/images/HUDScreen/action_menu.png");
            if (!imgFile.exists()) {
                imgFile = new File("../resources/images/HUDScreen/action_menu.png");
            }
            if (imgFile.exists()) {
                bgImg = ImageIO.read(imgFile);
            }
        } catch (Exception e) {
            System.err.println("Could not load action_menu.png: " + e.getMessage());
        }

        try {
            File imgFile = new File("resources/images/PopUpImages/PopUpActionMenu.png");
            if (!imgFile.exists()) {
                imgFile = new File("../resources/images/PopUpImages/PopUpActionMenu.png");
            }
            if (imgFile.exists()) {
                smallBgImg = ImageIO.read(imgFile);
            }
        } catch (Exception e) {
            System.err.println("Could not load PopUpActionMenu.png: " + e.getMessage());
        }

        try {
            File fontFile = new File("resources/fonts/VT323-Regular.ttf");
            if (!fontFile.exists()) {
                fontFile = new File("../resources/fonts/VT323-Regular.ttf");
            }
            if (fontFile.exists()) {
                vtFont = Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(Font.PLAIN, 20f);
            }
        } catch (Exception e) {
            vtFont = new Font("Monospaced", Font.BOLD, 16);
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void hideMenu() {
        this.visible = false;
        this.hoveredIndex = -1;
    }

    public void show(Component invoker, GameObject obj, int screenX, int screenY) {
        this.targetObject = obj;
        this.actions = obj.getActions();
        this.hoveredIndex = -1;

        boolean isItem = obj instanceof domain.models.item.MapItem;

        if (isItem) {
            if (bgImg != null) {
                this.width = 350;
                this.height = 200;
            } else {
                // Fallback dynamic calculation
                Graphics g = invoker.getGraphics();
                if (g != null) {
                    Font font = vtFont != null ? vtFont : g.getFont();
                    g.setFont(font);
                    FontMetrics fm = g.getFontMetrics();
                    
                    int maxW = fm.stringWidth(obj.getName()) + 10;
                    if (actions.isEmpty()) {
                        maxW = Math.max(maxW, fm.stringWidth("No actions available"));
                    } else {
                        for (Action action : actions) {
                            String label = action.getName() + " " + obj.getName();
                            if (!action.isAvailable(hero, obj)) {
                                label += " [Unavailable]";
                            }
                            maxW = Math.max(maxW, fm.stringWidth(label));
                        }
                    }
                    this.width = maxW + padding * 2 + 10;
                    int count = actions.isEmpty() ? 1 : actions.size();
                    this.height = titleHeight + 10 + (count * lineSpacing) + padding * 2;
                } else {
                    this.width = 250;
                    this.height = 150;
                }
            }
        } else {
            if (smallBgImg != null) {
                this.width = 271;
                this.height = 140;
            } else {
                this.width = 250;
                this.height = 130;
            }
        }

        // Clamp menu dimensions to fit within invoker bounds
        int maxMenuWidth = invoker.getWidth() - 10;
        int maxMenuHeight = invoker.getHeight() - 10;
        if (this.width > maxMenuWidth) {
            this.width = maxMenuWidth;
        }
        if (this.height > maxMenuHeight) {
            this.height = maxMenuHeight;
        }

        this.x = screenX;
        this.y = screenY;

        // Adjust position so it doesn't render off-screen
        if (this.x + this.width > invoker.getWidth()) {
            this.x = invoker.getWidth() - this.width - 5;
        }
        if (this.y + this.height > invoker.getHeight()) {
            this.y = invoker.getHeight() - this.height - 5;
        }
        this.x = Math.max(5, this.x);
        this.y = Math.max(5, this.y);

        this.visible = true;
        invoker.repaint();
    }

    private int getLineIndexAt(int localX, int localY) {
        boolean isItem = targetObject instanceof domain.models.item.MapItem;
        if (isItem) {
            if (bgImg != null) {
                if (localX < 10 || localX > 340) {
                    return -1;
                }
                if (localY >= 32 && localY <= 57) return 0;
                if (localY >= 66 && localY <= 91) return 1;
                if (localY >= 98 && localY <= 123) return 2;
                if (localY >= 130 && localY <= 154) return 3;
                if (localY >= 161 && localY <= 186) return 4;
                return -1;
            }
        } else {
            if (smallBgImg != null) {
                if (localX < 35 || localX > 240) {
                    return -1;
                }
                if (localY >= 32 && localY <= 75) return 0;
                if (localY >= 87 && localY <= 122) return 1;
                return -1;
            }
        }

        // Fallback vertical division
        int localYStart = padding + titleHeight + 5;
        int localYDiff = localY - localYStart;
        if (localYDiff >= 0) {
            int index = localYDiff / lineSpacing;
            int count = actions.isEmpty() ? 1 : actions.size();
            if (index >= 0 && index < count) {
                return index;
            }
        }
        return -1;
    }

    public boolean contains(int mx, int my) {
        if (!visible)
            return false;
        return mx >= x && mx < x + width && my >= y && my < y + height;
    }

    public void handleMouseMove(int mx, int my) {
        if (!visible)
            return;
        if (contains(mx, my)) {
            int index = getLineIndexAt(mx - x, my - y);
            if (index >= 0 && index < actions.size()) {
                this.hoveredIndex = index;
            } else {
                this.hoveredIndex = -1;
            }
        } else {
            this.hoveredIndex = -1;
        }
    }

    public boolean handleMouseClick(int mx, int my, Component invoker) {
        if (!visible)
            return false;
        if (contains(mx, my)) {
            int index = getLineIndexAt(mx - x, my - y);
            if (index >= 0 && index < actions.size()) {
                Action action = actions.get(index);
                if (action.isAvailable(hero, targetObject)) {
                    action.execute(hero, targetObject);
                    System.out.println(action.getName() + " executed on " + targetObject.getName());
                }
            }
            hideMenu();
            invoker.repaint();
            return true; // Click consumed
        }

        hideMenu();
        invoker.repaint();
        return false; // Clicked outside, not consumed here
    }

    public void draw(Graphics2D g) {
        if (!visible)
            return;

        // Save graphic state
        Color oldColor = g.getColor();
        Font oldFont = g.getFont();
        Stroke oldStroke = g.getStroke();

        boolean isItem = targetObject instanceof domain.models.item.MapItem;

        if (isItem) {
            // Draw large action menu
            if (bgImg != null) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(bgImg, x, y, width, height, null);

                // Draw Actions
                g.setFont(vtFont != null ? vtFont : new Font("Monospaced", Font.PLAIN, 14));
                FontMetrics afm = g.getFontMetrics();

                int[] centersY = {44, 78, 110, 142, 174};
                int[] startsY = {32, 66, 98, 130, 161};
                int[] heights = {25, 25, 25, 25, 25};

                for (int i = 0; i < 5; i++) {
                    if (i < actions.size()) {
                        Action action = actions.get(i);
                        boolean available = action.isAvailable(hero, targetObject);
                        String label = action.getName() + " " + targetObject.getName();
                        if (!available) {
                            label += " [Unavailable]";
                        }

                        // Draw hover highlight box if hovered and available
                        if (available && i == hoveredIndex) {
                            g.setColor(new Color(255, 255, 255, 40));
                            g.fillRect(x + 10, y + startsY[i], 330, heights[i]);
                            g.setColor(new Color(255, 215, 0)); // Bright gold text
                        } else if (!available) {
                            g.setColor(new Color(140, 140, 140)); // Disabled gray text
                        } else {
                            g.setColor(new Color(255, 235, 180)); // Soft gold/yellow text
                        }

                        // Push the normal texts a little to the right (x + 40 instead of x + 20)
                        int textX = x + 40;
                        int textY = y + centersY[i] + afm.getAscent() / 2 - 3;
                        g.drawString(label, textX, textY);
                    }
                }
            } else {
                // Fallback: draw semi-transparent dark container with brown border
                g.setColor(new Color(30, 20, 15, 240));
                g.fillRoundRect(x, y, width, height, 10, 10);
                g.setColor(new Color(130, 90, 60));
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(x, y, width, height, 10, 10);

                // Draw actions line by line
                g.setFont(vtFont != null ? vtFont : new Font("Monospaced", Font.PLAIN, 14));
                FontMetrics afm = g.getFontMetrics();
                int actionStartY = y + padding + 10;

                if (actions.isEmpty()) {
                    g.setColor(new Color(150, 150, 150)); // Grey
                    int textX = x + padding + 5;
                    int textY = actionStartY + afm.getAscent();
                    g.drawString("No actions available", textX, textY);
                } else {
                    for (int i = 0; i < actions.size(); i++) {
                        Action action = actions.get(i);
                        boolean available = action.isAvailable(hero, targetObject);
                        String label = action.getName() + " " + targetObject.getName();
                        if (!available) {
                            label += " [Unavailable]";
                        }

                        int textX = x + padding + 5;
                        int textY = actionStartY + i * lineSpacing + afm.getAscent();

                        // Draw hover highlight box if hovered and available
                        if (available && i == hoveredIndex) {
                            g.setColor(new Color(255, 255, 255, 40));
                            g.fillRect(x + padding - 5, actionStartY + i * lineSpacing, width - (padding * 2) + 10,
                                    lineSpacing - 2);

                            g.setColor(new Color(255, 215, 0)); // Bright gold text
                        } else if (!available) {
                            g.setColor(new Color(140, 140, 140)); // Disabled gray text
                        } else {
                            g.setColor(new Color(255, 235, 180)); // Soft gold/yellow text
                        }

                        g.drawString(label, textX, textY);
                    }
                }
            }
        } else {
            // Draw small action menu
            if (smallBgImg != null) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(smallBgImg, x, y, width, height, null);

                // Draw Actions
                g.setFont(vtFont != null ? vtFont : new Font("Monospaced", Font.PLAIN, 14));
                FontMetrics afm = g.getFontMetrics();

                int[] centersY = {53, 104};
                int[] startsY = {32, 87};
                int[] heights = {43, 35};

                for (int i = 0; i < 2; i++) {
                    if (i < actions.size()) {
                        Action action = actions.get(i);
                        boolean available = action.isAvailable(hero, targetObject);
                        String label = action.getName() + " " + targetObject.getName();
                        if (!available) {
                            label += " [Unavailable]";
                        }

                        // Draw hover highlight box if hovered and available
                        if (available && i == hoveredIndex) {
                            g.setColor(new Color(255, 255, 255, 40));
                            g.fillRect(x + 35, y + startsY[i], 205, heights[i]);
                            g.setColor(new Color(255, 215, 0)); // Bright gold text
                        } else if (!available) {
                            g.setColor(new Color(140, 140, 140)); // Disabled gray text
                        } else {
                            g.setColor(new Color(255, 235, 180)); // Soft gold/yellow text
                        }

                        // Push a little right
                        int textX = x + 45;
                        int textY = y + centersY[i] + afm.getAscent() / 2 - 3;
                        g.drawString(label, textX, textY);
                    }
                }
            } else {
                // Fallback: draw semi-transparent dark container with brown border
                g.setColor(new Color(30, 20, 15, 240));
                g.fillRoundRect(x, y, width, height, 10, 10);
                g.setColor(new Color(130, 90, 60));
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(x, y, width, height, 10, 10);

                // Draw actions line by line
                g.setFont(vtFont != null ? vtFont : new Font("Monospaced", Font.PLAIN, 14));
                FontMetrics afm = g.getFontMetrics();
                int actionStartY = y + padding + 10;

                if (actions.isEmpty()) {
                    g.setColor(new Color(150, 150, 150)); // Grey
                    int textX = x + padding + 5;
                    int textY = actionStartY + afm.getAscent();
                    g.drawString("No actions available", textX, textY);
                } else {
                    for (int i = 0; i < actions.size(); i++) {
                        Action action = actions.get(i);
                        boolean available = action.isAvailable(hero, targetObject);
                        String label = action.getName() + " " + targetObject.getName();
                        if (!available) {
                            label += " [Unavailable]";
                        }

                        int textX = x + padding + 5;
                        int textY = actionStartY + i * lineSpacing + afm.getAscent();

                        // Draw hover highlight box if hovered and available
                        if (available && i == hoveredIndex) {
                            g.setColor(new Color(255, 255, 255, 40));
                            g.fillRect(x + padding - 5, actionStartY + i * lineSpacing, width - (padding * 2) + 10,
                                    lineSpacing - 2);

                            g.setColor(new Color(255, 215, 0)); // Bright gold text
                        } else if (!available) {
                            g.setColor(new Color(140, 140, 140)); // Disabled gray text
                        } else {
                            g.setColor(new Color(255, 235, 180)); // Soft gold/yellow text
                        }

                        g.drawString(label, textX, textY);
                    }
                }
            }
        }

        // Restore graphics state
        g.setColor(oldColor);
        g.setFont(oldFont);
        g.setStroke(oldStroke);
    }
}
