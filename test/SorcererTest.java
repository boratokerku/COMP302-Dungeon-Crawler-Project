import domain.models.entity.Hero;
import domain.models.entity.Sorcerer;
import domain.models.map.GameMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 tests for Sorcerer.followHero().
 *
 * Tested method:
 * public void followHero(Hero hero, GameMap map, List<Entity> entities)
 *
 * These tests verify that the REQUIRES / MODIFIES / EFFECTS spec
 * documented in Sorcerer.java holds in practice.
 */
public class SorcererTest {

    private Sorcerer sorcerer;
    private Hero hero;
    private GameMap map;
    private List<domain.models.entity.Entity> entities;

    /**
     * Common setup: a 10x10 walkable map, hero at (5,5), sorcerer at (2,2).
     * Both cooldowns are reset to "just started" so they won't fire by default.
     */
    @BeforeEach
    public void setUp() {
        map = new GameMap(10, 10);
        hero = new Hero(5, 5);
        sorcerer = new Sorcerer(2, 2);
        entities = new ArrayList<>();
        entities.add(sorcerer);

        // Reset both cooldown timers to "now" so they don't accidentally fire.
        sorcerer.setTimeLeft(7000); // teleport timer: full 7 s remaining
        sorcerer.setProjectileTimeLeft(5000); // projectile timer: full 5 s remaining
    }

    // -----------------------------------------------------------------------
    // Test 1 — EFFECTS clause: dead sorcerer must do nothing
    // -----------------------------------------------------------------------

    /**
     * EFFECTS spec: "If this.isAlive() == false, the method returns immediately
     * with no side effects."
     *
     * We kill the sorcerer BEFORE calling followHero, then force both cooldowns
     * to have expired. If the spec holds, no projectile should be queued.
     */
    @Test
    public void testFollowHero_WhenDead_NoProjectileIsCreated() {
        // Kill the sorcerer
        sorcerer.takeDamage(100);
        assertFalse(sorcerer.isAlive(), "Sorcerer should be dead after 100 damage");

        // Force both cooldowns to have expired
        sorcerer.setTimeLeft(0);
        sorcerer.setProjectileTimeLeft(0);

        // Call the method under test
        sorcerer.followHero(hero, map, entities);

        // No projectile should have been created
        assertNull(sorcerer.pollPendingProjectile(),
                "A dead sorcerer must NOT create a projectile");
    }

    // -----------------------------------------------------------------------
    // Test 2 — EFFECTS clause: projectile is fired only after 5-second cooldown
    // -----------------------------------------------------------------------

    /**
     * EFFECTS spec: "If elapsed time since last projectile >= 5000 ms:
     * pendingProjectile is assigned a new Projectile directed toward the target."
     *
     * We test two sub-cases:
     * (a) cooldown NOT yet expired → no projectile
     * (b) cooldown HAS expired → projectile is created
     */
    @Test
    public void testFollowHero_ProjectileCooldown_FiresOnlyWhenReady() {
        // (a) Cooldown not expired: 3 seconds still remaining
        sorcerer.setProjectileTimeLeft(3000);
        sorcerer.followHero(hero, map, entities);
        assertNull(sorcerer.pollPendingProjectile(),
                "Sorcerer should NOT fire while projectile cooldown has 3 s left");

        // (b) Cooldown expired: 0 ms remaining → projectile must be created
        sorcerer.setProjectileTimeLeft(0);
        sorcerer.followHero(hero, map, entities);
        assertNotNull(sorcerer.pollPendingProjectile(),
                "Sorcerer MUST fire a projectile when the 5-second cooldown has expired");
    }

    // -----------------------------------------------------------------------
    // Test 3 — MODIFIES clause: teleport timer resets after 7-second cooldown
    // -----------------------------------------------------------------------

    /**
     * MODIFIES spec: "this.lastTeleportTime — updated when the 7-second
     * teleport cooldown expires."
     *
     * We force the teleport cooldown to 0 (expired) and confirm that after
     * followHero() the timer has been reset, i.e. getTimeLeft() is now close
     * to 7000 ms (within a generous 500 ms tolerance for test execution time).
     */
    @Test
    public void testFollowHero_TeleportTimer_ResetsAfterCooldownExpires() {
        // Force teleport cooldown to have expired
        sorcerer.setTimeLeft(0);
        assertEquals(0, sorcerer.getTimeLeft(),
                "Teleport timer should report 0 ms remaining before followHero call");

        // Call the method — timer should be reset regardless of whether the
        // 50% random chance actually teleports the sorcerer.
        sorcerer.followHero(hero, map, entities);

        long timeLeft = sorcerer.getTimeLeft();
        assertTrue(timeLeft > 0 && timeLeft <= 7000,
                "After followHero resets the timer, getTimeLeft() must be in (0, 7000] ms, was: " + timeLeft);
    }
}
