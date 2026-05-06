package domain.models.entity;

public class SearchableObject extends GameObject {
    public SearchableObject(String name, int x, int y) {
        super(name, x, y, "searchable", false);
    }
}
