package domain.models.tile;

public abstract class Tile {
    protected boolean passable; // Karakterler buradan geçebilir mi?
    protected String symbol; // Konsol çıktısı için (örn: "#", ".")

    public Tile(boolean passable, String symbol) {
        this.passable = passable;
        this.symbol = symbol;
    }

    public boolean isPassable() {
        return passable;
    }

    public String getSymbol() {
        return symbol;
    }
}