package domain.logic;

import domain.models.entity.Hero;
import domain.models.entity.GameObject;

public class SearchAction implements Action {
    private GameObject hiddenItem;

    public SearchAction(GameObject hiddenItem) {
        this.hiddenItem = hiddenItem;
    }

    @Override
    public String getName() {
        return "Search";
    }

    @Override
    public boolean isAvailable(Hero hero, GameObject target) {
        return true;
    }

    @Override
    public void execute(Hero hero, GameObject target) {
        if (!(target instanceof domain.models.entity.SearchableObject)) {
            return;
        }

        domain.models.entity.SearchableObject so = (domain.models.entity.SearchableObject) target;

        if (so.isSearched()) {
            System.out.println("Already searched.");
            view.GameView.addFloatingText(so.getX(), so.getY(), "Already searched", java.awt.Color.LIGHT_GRAY);
            return;
        }

        so.search();

        if (so.getHiddenItem() != null) {
            domain.models.map.GameMap map = so.getMap();
            GameObject existingWall = map.getObjectAt(so.getX(), so.getY());
            if (existingWall instanceof domain.models.tile.WallTile) {
                ((domain.models.tile.WallTile) existingWall).setDecoration(null);
            }

            int dropY = (so.getY() == 0) ? 1 : so.getY() - 1;
            map.placeObject(so.getHiddenItem(), so.getX(), dropY);

            view.GameView.addFloatingText(so.getX(), so.getY(), "Found Key!", java.awt.Color.YELLOW);
            System.out.println("You found something! A key was hidden here. It fell to the ground.");
            return;
        }

        java.util.Random rand = new java.util.Random();
        int roll = rand.nextInt(100);

        if (roll < 40) {
            // 40% empty -> atmospheric message
            String msg = "Nothing found.";
            String imgName = so.getImageName();
            if (imgName != null) {
                if (imgName.contains("missing_brick"))
                    msg = "Just cold stone behind it.";
                else if (imgName.contains("wall_grill"))
                    msg = "Stale air. Nothing else.";
                else if (imgName.contains("gargoyle"))
                    msg = "The water is still. Nothing stirs.";
                else if (imgName.contains("pipe_hole"))
                    msg = "Too dark to see anything inside.";
                else if (imgName.contains("wall_cavity"))
                    msg = "Your hand finds nothing but dust.";
                else if (imgName.contains("loose_stone"))
                    msg = "The stone is solid. Nothing here.";
            }
            view.GameView.addFloatingText(so.getX(), so.getY(), "Empty", java.awt.Color.LIGHT_GRAY);
            System.out.println(msg);

        } else if (roll < 75) {
            // 35% small item -> random Potion or GoldCoin
            domain.models.item.MapItem item = new domain.models.item.usables.PotionItem(so.getX(), so.getY());

            // Searchable object duvardan kalksın
            domain.models.map.GameMap map = so.getMap();
            GameObject existingWall = map.getObjectAt(so.getX(), so.getY());
            if (existingWall instanceof domain.models.tile.WallTile) {
                ((domain.models.tile.WallTile) existingWall).setDecoration(null);
            }

            int dropY = (so.getY() == 0) ? 1 : so.getY() - 1;
            map.placeObject(item, so.getX(), dropY);

            view.GameView.addFloatingText(so.getX(), so.getY(), "Found Item!", java.awt.Color.GREEN);
            System.out.println("You found a " + item.getName() + "! It fell to the ground.");

        } else if (roll < 95) {
            // 20% key item -> KeyItem
            domain.models.staticObjects.KeyItem key = new domain.models.staticObjects.KeyItem(so.getX(), so.getY());

            domain.models.map.GameMap map = so.getMap();
            GameObject existingWall = map.getObjectAt(so.getX(), so.getY());
            if (existingWall instanceof domain.models.tile.WallTile) {
                ((domain.models.tile.WallTile) existingWall).setDecoration(null);
            }

            int dropY = (so.getY() == 0) ? 1 : so.getY() - 1;
            map.placeObject(key, so.getX(), dropY);

            view.GameView.addFloatingText(so.getX(), so.getY(), "Found Key!", java.awt.Color.ORANGE);
            System.out.println("A key was hidden here! It fell to the ground.");

        } else {
            // 5% trap
            if (!so.isTrapTriggered()) {
                so.setTrapTriggered(true);
                hero.takeDamage(2);
                view.GameView.trapFlashFrames = 15; // 15 frames of red flash
                view.GameView.addFloatingText(so.getX(), so.getY(), "-2 HP", java.awt.Color.RED);
                System.out.println("It was a trap! You lost 2 HP.");
            } else {
                // If already triggered, act as empty
                view.GameView.addFloatingText(so.getX(), so.getY(), "Empty", java.awt.Color.LIGHT_GRAY);
                System.out.println("Nothing found.");
            }
        }
    }
}
