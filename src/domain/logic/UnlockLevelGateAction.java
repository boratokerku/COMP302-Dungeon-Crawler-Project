package domain.logic;

import domain.models.entity.Hero;
import domain.models.GameObject;
import domain.models.staticObjects.LevelDoor;
import domain.models.item.LevelKey;

public class UnlockLevelGateAction implements Action {
    @Override
    public String getName() {
        return "Unlock Gate";
    }

    @Override
    public boolean isAvailable(Hero hero, GameObject target) {
        if (target instanceof LevelDoor) {
            return ((LevelDoor) target).isLocked();
        }
        return false;
    }

    @Override
    public void execute(Hero hero, GameObject target) {
        if (target instanceof LevelDoor) {
            LevelDoor gate = (LevelDoor) target;
            boolean success = gate.tryUnlockWithKey(hero);
            if (success) {
                util.helpers.SoundManager.playUnlock();
                view.GameView.addFloatingText(gate.getX(), gate.getY(), "UNLOCKED!", java.awt.Color.GREEN);
                System.out.println("Unlocked level gate using Skull Key!");
            } else {
                javax.swing.JOptionPane.showMessageDialog(
                        null,
                        "This door is locked! You need a Skull Key to open it.",
                        "Door Locked",
                        javax.swing.JOptionPane.WARNING_MESSAGE
                );
                view.GameView.addFloatingText(gate.getX(), gate.getY(), "Skull Key Required", java.awt.Color.RED);
            }
        }
    }
}
