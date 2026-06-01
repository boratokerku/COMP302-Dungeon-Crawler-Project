package domain.models.staticObjects;

import domain.models.GameObject;

public class WallObject extends GameObject {
    public WallObject(String name, int x, int y, String imageName) {
        super(name, x, y, imageName, true); // Passable = true
        this.customScale = getDefaultScale(imageName);
    }

    private double getDefaultScale(String img) {
        if (img == null) return 1.0;
        String lower = img.toLowerCase();
        
        // Banners / flags (very large, draping down, highly visible)
        if (lower.contains("flag") || lower.contains("banner")) {
            return 1.6;
        }
        
        // Large statues / fountains
        if (lower.contains("statue")) {
            return 2.0;
        }
        
        // Green hanging moss/vine
        if (lower.contains("moss")) {
            return 2.0;
        }
        
        // Dripping acid or blood
        if (lower.contains("acid_ooze") || lower.contains("blood_stain")) {
            return 1.8;
        }
        
        // Gargoyle head/fountain
        if (lower.contains("gargoyle")) {
            return 1.5;
        }
        
        // Spider web
        if (lower.contains("cobweb")) {
            return 1.5;
        }
        
        // Chains hanging down
        if (lower.contains("chain")) {
            return 1.4;
        }
        
        // Torch decorations
        if (lower.contains("torch")) {
            return 1.3;
        }
        
        // Drainage pipe, sewer grill, windows/grates
        if (lower.contains("grill") || lower.contains("pipe") || lower.contains("window")) {
            return 1.4;
        }
        
        // Wall cracks, cavities, missing bricks, loose stones
        if (lower.contains("crack") || lower.contains("cavity") || lower.contains("brick") || lower.contains("stone")) {
            return 1.3;
        }
        
        // Skull
        if (lower.contains("skull")) {
            return 1.1;
        }
        
        // Columns / purple columns
        if (lower.contains("colon") || lower.contains("column")) {
            return 1.6;
        }
        
        // Default prominent scale for any other WallObjects
        return 1.5;
    }
}
