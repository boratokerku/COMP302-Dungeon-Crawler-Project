package domain.models.entity;

public class SearchableObject extends GameObject {
    private boolean isSearched = false;
    private String openImageName;

    public SearchableObject(String name, int x, int y, String closedImageName, String openImageName) {
        super(name, x, y, closedImageName, false);
        this.openImageName = openImageName;
        this.addAction(new domain.logic.SearchAction(null));
    }

    public SearchableObject(String name, int x, int y, String closedImageName) {
        this(name, x, y, closedImageName, closedImageName);
    }

    public SearchableObject(String name, int x, int y) {
        this(name, x, y, "containers/chest_brown");
    }

    public boolean isSearched() {
        return isSearched;
    }

    public void search() {
        this.isSearched = true;
        if (openImageName != null && !openImageName.isEmpty()) {
            this.imageName = openImageName;
        }
    }

    public String getOpenImageName() {
        return openImageName;
    }
}

