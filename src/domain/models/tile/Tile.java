package domain.models.tile;

import domain.models.entity.GameObject;

public abstract class Tile extends GameObject {
    protected String symbol; 

    public Tile(int x, int y, String imageName, boolean passable) {
        super(x, y, imageName, passable);
        this.symbol = imageName;
    }

    public String getSymbol() {
        return symbol;
    }
}