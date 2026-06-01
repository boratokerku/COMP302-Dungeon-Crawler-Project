package ui.design;

import domain.logic.MapValidator;
import domain.logic.RandomMapGenerator;
import domain.models.entity.Chest;
import domain.models.entity.GameObject;
import domain.models.item.MapItem;
import domain.models.map.GameMap;
import domain.models.tile.FloorTile;
import domain.models.tile.WallTile;
import ui.DesignModeView;
import ui.dialogs.ClearMapDialog;
import ui.dialogs.DeleteConfirmDialog;
import ui.dialogs.LoadMapDialog;
import ui.dialogs.SaveMapDialog;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class DesignModeActionHandler {
    private final DesignModeView view;
    private final GameMap map;
    private final Runnable onBackToMenu;
    private final java.util.function.Consumer<GameMap> onPlayMap;
    private final java.util.function.Consumer<GameMap> onPlayTeamMatchMap;

    private final List<ActionBtn> actionBtns = new ArrayList<>();

    public DesignModeActionHandler(DesignModeView view, GameMap map, Runnable onBackToMenu,
                                   java.util.function.Consumer<GameMap> onPlayMap,
                                   java.util.function.Consumer<GameMap> onPlayTeamMatchMap) {
        this.view = view;
        this.map = map;
        this.onBackToMenu = onBackToMenu;
        this.onPlayMap = onPlayMap;
        this.onPlayTeamMatchMap = onPlayTeamMatchMap;

        buildActionButtons();
    }

    public List<ActionBtn> getActionButtons() {
        return actionBtns;
    }

    private void buildActionButtons() {
        actionBtns.add(new ActionBtn("▶  Play", new Color(60, 140, 60),
                "images/DesignModeImages/DesignModeButtons/PlayButton.png", this::doPlay));
        actionBtns.add(new ActionBtn("▶  Team Match", new Color(60, 100, 160),
                "images/DesignModeImages/DesignModeButtons/PlayTeamMatchButton.png", this::doPlayTeamMatch));
        actionBtns.add(new ActionBtn("⚄  +5 Random", new Color(80, 60, 130),
                "images/DesignModeImages/DesignModeButtons/PlusFiveRandomButton.png", this::doAddRandom));
        actionBtns.add(new ActionBtn("🎲  Gen Map", new Color(110, 50, 130),
                "images/DesignModeImages/DesignModeButtons/GenerateRandomMapButton.png", this::doGenerateRandomMap));
        actionBtns.add(new ActionBtn("💾  Save Map", new Color(50, 90, 150),
                "images/DesignModeImages/DesignModeButtons/SaveMapButton.png", this::doSave));
        actionBtns.add(new ActionBtn("📂  Load Map", new Color(100, 80, 30),
                "images/DesignModeImages/DesignModeButtons/LoadMapButton.png", this::doLoad));
        actionBtns.add(new ActionBtn("🗑  Clear Map", new Color(140, 60, 40),
                "images/DesignModeImages/DesignModeButtons/ClearMapButton.png", this::doClear));
        actionBtns.add(new ActionBtn("✖  Exit Menu", new Color(80, 30, 50),
                "images/DesignModeImages/DesignModeButtons/ExitToMainMenuButton.png", onBackToMenu));
    }

    public boolean fireActionBtn(int mx, int my) {
        for (ActionBtn btn : actionBtns) {
            if (btn.bounds.contains(mx, my)) {
                btn.action.run();
                return true;
            }
        }
        return false;
    }

    private void doPlay() {
        if (!MapValidator.validate(map)) {
            view.showInvalidMapDialog();
            return;
        }
        if (onPlayMap != null)
            onPlayMap.accept(map);
    }

    private void doPlayTeamMatch() {
        if (!MapValidator.validateTeamMatch(map)) {
            view.showInvalidMapDialog();
            return;
        }
        if (onPlayTeamMatchMap != null)
            onPlayTeamMatchMap.accept(map);
    }

    private void doAddRandom() {
        List<int[]> freeTiles = getFreeTiles();
        if (freeTiles.isEmpty()) {
            JOptionPane.showMessageDialog(view, "Boş tile yok! Önce bazı nesneleri silin.", "Uyarı", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int currentItems = view.countItems();
        if (currentItems >= view.MAX_ITEMS) {
            view.showMaxItemDialog();
            return;
        }

        java.util.Random rand = new java.util.Random();
        int placed = 0;
        int remainingSlots = view.MAX_ITEMS - currentItems;
        int toPlace = Math.min(5, remainingSlots);

        while (placed < toPlace && !freeTiles.isEmpty()) {
            int[] pos = freeTiles.remove(rand.nextInt(freeTiles.size()));
            GameObject item = MapItem.createRandomItem(pos[0], pos[1]);
            if (item instanceof Chest) {
                int roll;
                int count = 0;
                do {
                    roll = rand.nextInt(10) + 1;
                    count++;
                } while (roll >= 8 && count < 20);
            }
            map.placeObject(item, pos[0], pos[1]);
            placed++;
        }

        view.repaint();
    }

    private List<int[]> getFreeTiles() {
        List<int[]> free = new ArrayList<>();
        for (int x = 0; x < map.getWidth(); x++)
            for (int y = 0; y < map.getHeight(); y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj instanceof FloorTile)
                    free.add(new int[] { x, y });
            }
        return free;
    }

    private void doGenerateRandomMap() {
        RandomMapGenerator.generateRandomMap(map);
        view.repaint();
    }

    private void doSave() {
        Window parentWindow = SwingUtilities.getWindowAncestor(view);
        Frame parentFrame = (parentWindow instanceof Frame) ? (Frame) parentWindow : null;

        java.util.List<String> existingMaps = new java.util.ArrayList<>();
        File dir = new File("saves/maps");
        if (dir.exists() && dir.listFiles() != null) {
            File[] files = dir.listFiles((d, n) -> n.endsWith(".mapjson"));
            if (files != null) {
                for (File f : files) {
                    existingMaps.add(f.getName().replace(".mapjson", ""));
                }
            }
        }

        SaveMapDialog dialog = new SaveMapDialog(parentFrame, existingMaps);
        dialog.setVisible(true);

        if (!dialog.isSaved())
            return;

        String name = dialog.getMapName();
        if (name == null || name.trim().isEmpty())
            return;
        name = name.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");

        try {
            new File("saves/maps").mkdirs();
            String json = domain.logic.MapFileManager.mapToJson(map, name);
            try (FileWriter fw = new FileWriter("saves/maps/" + name + ".mapjson")) {
                fw.write(json);
            }
            JOptionPane.showMessageDialog(view, "Harita kaydedildi: saves/maps/" + name + ".mapjson", "Kayıt Başarılı", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(view, "Kayıt hatası: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doLoad() {
        Window parentWindow = SwingUtilities.getWindowAncestor(view);
        Frame parentFrame = (parentWindow instanceof Frame) ? (Frame) parentWindow : null;

        java.util.List<String> existingMaps = new java.util.ArrayList<>();
        File dir = new File("saves/maps");
        if (dir.exists() && dir.listFiles() != null) {
            File[] files = dir.listFiles((d, n) -> n.endsWith(".mapjson"));
            if (files != null) {
                for (File f : files) {
                    existingMaps.add(f.getName().replace(".mapjson", ""));
                }
            }
        }

        if (existingMaps.isEmpty()) {
            JOptionPane.showMessageDialog(view, "Kayıtlı harita bulunamadı.", "Yükle", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        LoadMapDialog dialog = new LoadMapDialog(parentFrame, existingMaps);
        dialog.setVisible(true);

        if (dialog.isLoaded()) {
            String chosen = dialog.getSelectedMapName();
            try (FileReader fr = new FileReader("saves/maps/" + chosen + ".mapjson")) {
                StringBuilder sb = new StringBuilder();
                int c;
                while ((c = fr.read()) != -1)
                    sb.append((char) c);
                domain.logic.MapFileManager.loadMapFromJson(map, sb.toString());
                view.repaint();
                JOptionPane.showMessageDialog(view, "Harita yüklendi: " + chosen, "Yükleme Başarılı", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(view, "Yükleme hatası: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
            }
        } else if (dialog.isDeleteRequested()) {
            String deleteTarget = dialog.getDeleteMapName();
            DeleteConfirmDialog confirmDialog = new DeleteConfirmDialog(parentFrame, "Delete map: " + deleteTarget + "?");
            confirmDialog.setVisible(true);
            if (confirmDialog.isConfirmed()) {
                File file = new File("saves/maps/" + deleteTarget + ".mapjson");
                if (file.exists()) {
                    file.delete();
                }
            }
            doLoad();
        }
    }

    private void doClear() {
        Window parentWindow = SwingUtilities.getWindowAncestor(view);
        Frame parentFrame = (parentWindow instanceof Frame) ? (Frame) parentWindow : null;

        ClearMapDialog dialog = new ClearMapDialog(parentFrame);
        dialog.setVisible(true);

        if (!dialog.isConfirmed())
            return;

        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GameObject obj = map.getObjectAt(x, y);
                if (obj instanceof WallTile) {
                    ((WallTile) obj).setDecoration(null);
                    continue;
                }
                if (obj == null || obj instanceof FloorTile || obj instanceof domain.models.staticObjects.LevelDoor)
                    continue;
                map.placeObject(new FloorTile(), x, y);
            }
        }
        view.repaint();
    }
}
