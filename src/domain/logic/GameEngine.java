package domain.logic;

import domain.models.entity.Entity;
import domain.models.entity.Hero;
import java.util.ArrayList;
import java.util.List;

public class GameEngine {
    private List<Entity> entities;
    private boolean isRunning;

    public GameEngine(Hero player) {
        this.entities = new ArrayList<>();
        this.entities.add(player);
        this.isRunning = true;
    }

    public void addEntity(Entity entity) {
        entities.add(entity);
    }

    /**
     * Oyunun her karesinde (frame) çağrılan ana döngü metodu.
     */
    public void update() {
        if (!isRunning)
            return;

        for (Entity entity : entities) {
            // Sorcerer'ın 7 saniye kuralı veya Knight'ın devriyesi burada tetiklenir
            entity.update();

            // Çarpışma kontrolü (Collision)
            checkCollisions(entity);
        }
    }

    private void checkCollisions(Entity entity) {
        // Burada her entity'nin diğerleriyle veya duvarlarla
        // çakışıp çakışmadığını kontrol eden mantık yer alacak.
        // Örn: if (entity.getX() == knight.getX() ...)
    }

    public void setRunning(boolean running) {
        this.isRunning = running;
    }
}