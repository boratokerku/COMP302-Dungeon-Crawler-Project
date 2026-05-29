package test;

import domain.models.Direction;
import domain.models.entity.*;
import domain.models.map.GameMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FinalBossCollisionTest {

    private GameMap map;
    private Hero hero;
    private FinalBoss boss;
    private List<Entity> entities;

    @BeforeEach
    public void setUp() {
        map = new GameMap(12, 12);
        
        hero = new Hero(2, 2);
        hero.setCurrentMap(map);
        hero.setEnergy(100);
        hero.setStr(12);

        // Spawn FinalBoss at (4, 4) -> footprint covers:
        // (4,4) - top-left
        // (5,4) - top-right
        // (4,5) - bottom-left
        // (5,5) - bottom-right
        boss = new FinalBoss(4, 4);

        entities = new ArrayList<>();
        entities.add(hero);
        entities.add(boss);
    }

    @Test
    public void testHeroBumpAttack_TopLeftTile_FromLeft() {
        hero.setPosition(3, 4); // left of (4,4)
        boolean result = hero.move(Direction.RIGHT, map, entities);
        assertTrue(result);
        assertEquals(3, hero.getX());
        assertEquals(4, hero.getY());
        assertTrue(boss.getHp() < 100);
    }

    @Test
    public void testHeroBumpAttack_TopLeftTile_FromAbove() {
        hero.setPosition(4, 3); // above (4,4)
        boolean result = hero.move(Direction.DOWN, map, entities);
        assertTrue(result);
        assertEquals(4, hero.getX());
        assertEquals(3, hero.getY());
        assertTrue(boss.getHp() < 100);
    }

    @Test
    public void testHeroBumpAttack_TopRightTile_FromRight() {
        hero.setPosition(6, 4); // right of (5,4)
        boolean result = hero.move(Direction.LEFT, map, entities);
        assertTrue(result);
        assertEquals(6, hero.getX());
        assertEquals(4, hero.getY());
        assertTrue(boss.getHp() < 100);
    }

    @Test
    public void testHeroBumpAttack_TopRightTile_FromAbove() {
        hero.setPosition(5, 3); // above (5,4)
        boolean result = hero.move(Direction.DOWN, map, entities);
        assertTrue(result);
        assertEquals(5, hero.getX());
        assertEquals(3, hero.getY());
        assertTrue(boss.getHp() < 100);
    }

    @Test
    public void testHeroBumpAttack_BottomLeftTile_FromLeft() {
        hero.setPosition(3, 5); // left of (4,5)
        boolean result = hero.move(Direction.RIGHT, map, entities);
        assertTrue(result);
        assertEquals(3, hero.getX());
        assertEquals(5, hero.getY());
        assertTrue(boss.getHp() < 100);
    }

    @Test
    public void testHeroBumpAttack_BottomLeftTile_FromBelow() {
        hero.setPosition(4, 6); // below (4,5)
        boolean result = hero.move(Direction.UP, map, entities);
        assertTrue(result);
        assertEquals(4, hero.getX());
        assertEquals(6, hero.getY());
        assertTrue(boss.getHp() < 100);
    }

    @Test
    public void testHeroBumpAttack_BottomRightTile_FromRight() {
        hero.setPosition(6, 5); // right of (5,5)
        boolean result = hero.move(Direction.LEFT, map, entities);
        assertTrue(result);
        assertEquals(6, hero.getX());
        assertEquals(5, hero.getY());
        assertTrue(boss.getHp() < 100);
    }

    @Test
    public void testHeroBumpAttack_BottomRightTile_FromBelow() {
        hero.setPosition(5, 6); // below (5,5)
        boolean result = hero.move(Direction.UP, map, entities);
        assertTrue(result);
        assertEquals(5, hero.getX());
        assertEquals(6, hero.getY());
        assertTrue(boss.getHp() < 100);
    }

    @Test
    public void testKnightCollision_CannotMoveIntoBossTiles() {
        Knight knight = new Knight(3, 4);
        entities.add(knight);
        hero.setPosition(4, 4); 
        for (int i = 0; i < 4; i++) {
            knight.followHero(hero, map, entities);
        }
        assertNotEquals(4, knight.getX());
    }

    @Test
    public void testShadowCloneCollision_CannotMoveIntoBossTiles() {
        ShadowClone clone = new ShadowClone(3, 4);
        entities.add(clone);
        clone.moveOpposite(Direction.LEFT, map, entities); 
        assertEquals(3, clone.getX());

        clone.setPosition(6, 4);
        clone.moveOpposite(Direction.RIGHT, map, entities); 
        assertEquals(6, clone.getX());
    }
}
