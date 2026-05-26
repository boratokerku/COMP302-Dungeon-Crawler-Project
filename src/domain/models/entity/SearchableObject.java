package domain.models.entity;

public class SearchableObject extends GameObject {
    private boolean isSearched = false;
    private String openImageName;
    private GameObject hiddenItem = null;
    private boolean trapTriggered = false;

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

    public void setSearched(boolean searched) {
        this.isSearched = searched;
    }

    public GameObject getHiddenItem() {
        return hiddenItem;
    }

    public void setHiddenItem(GameObject hiddenItem) {
        this.hiddenItem = hiddenItem;
    }

    public boolean isTrapTriggered() {
        return trapTriggered;
    }

    public void setTrapTriggered(boolean trapTriggered) {
        this.trapTriggered = trapTriggered;
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

