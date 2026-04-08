package domain.models.entity;

public class Knight extends Entity {
    private String moveDirection = "HORIZONTAL"; // Knight'lar yatay veya dikey hareket eder

    public Knight(int x, int y) {
        super(x, y, 10); // Knight için örnek can, dokümanda belirtilmemişse 10 iyidir
    }

    // Knight'ın kendine has zekası (Sadece ileri-geri gider)
    public void patrol() {
        // Hareket mantığı buraya gelecek
    }
}