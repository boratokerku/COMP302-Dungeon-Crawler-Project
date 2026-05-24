package test;

import domain.models.entity.Hero;
import domain.models.entity.Knight;
import domain.models.map.GameMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JUnit 5 tests for Hero.attack(Entity target, GameMap map).
 *
 * Tested method:
 *   public void attack(Entity target, GameMap map)
 *
 * Specification (from Hero.java behavior):
 *   @requires target may be null; map may be null.
 *             If an attack is expected to execute, target must be alive
 *             and hero energy must be at least 10.
 *   @modifies target.hp, target.alive, hero.energy, and possibly map cell at
 *             target coordinates if target dies and map is non-null.
 *   @effects  If target is non-null and alive and hero has enough energy,
 *             target takes damage calculated by (hero.str / 2 + hero.weaponAtk)
 *             and hero energy decreases by 10. If the target is dead or hero
 *             lacks energy, no state change occurs for target HP and no energy
 *             is spent.
 */
public class HeroAttackTest {

    private Hero hero;
    private Knight target;
    private GameMap map;

    @BeforeEach
    public void setUp() {
        map = new GameMap(5, 5);
        hero = new Hero(1, 1);
        target = new Knight(2, 1);

        // Deterministic damage: damage = (STR / 2) + weaponAtk = (12 / 2) + 0 = 6
        hero.setStr(12);
        hero.setEnergy(100);
    }

    @Test
    public void testAttack_WithEnoughEnergy_ShouldReduceTargetHpAndEnergy() {
        hero.attack(target, map);

        assertEquals(14, target.getHp(), "Target HP should be reduced by deterministic damage (6)");
        assertEquals(90, hero.getEnergy(), "Hero should spend 10 energy for a melee attack");
    }

    @Test
    public void testAttack_WithInsufficientEnergy_ShouldNotChangeTargetOrEnergy() {
        hero.setEnergy(5);

        hero.attack(target, map);

        assertEquals(20, target.getHp(), "Target HP should remain unchanged when hero has insufficient energy");
        assertEquals(5, hero.getEnergy(), "Hero energy should remain unchanged when attack cannot be executed");
    }

    @Test
    public void testAttack_OnDeadTarget_ShouldDoNothing() {
        target.takeDamage(999); // dead target

        hero.attack(target, map);

        assertEquals(0, target.getHp(), "Dead target HP should remain 0");
        assertEquals(100, hero.getEnergy(), "Hero should not spend energy when target is already dead");
    }
}
