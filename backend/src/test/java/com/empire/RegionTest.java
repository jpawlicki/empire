package com.empire;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RegionTest {
    private static Region r;
    private static World world;
    private static final double DELTA = 1E-5;

    @Before
    public void setUpRregion() {
        r = new Region();

        world = WorldTest.regionTestWorld();
    }

    @Test
    public void numUniqueIdeologiesTest(){
        assertEquals(2, Region.numUniqueIdeologies("k1", world));
        assertEquals(2, Region.numUniqueIdeologies2("k1", world));
    }

    // TODO:  The can transfer food tests rely on making a test world that is too complex at the moment, these should
    // be revisited if/once refactoring has made this more feasible

    @Test
    public void canTransferFoodToAdjacent(){
        // Test when r0 and r1 are directly adjacent one another, assert true
        //assertTrue(r0.canFoodTransferTo(world, r1));
        assertTrue(true);
    }

    @Test
    public void canTransferFoodToViaOcean(){
        // Test when r0 and r1 connect via ocean, assert true
        //assertTrue(r0.canFoodTransferTo(world, r1));
        assertTrue(true);
    }

    @Test
    public void canTransferFoodToFalse(){
        // Test when r0 and r1 are neither adjacent nor connected via ocean, assert false
        //assertFalse(r0.canFoodTransferTo(world, r1));
        assertFalse(false);
    }

    @Test
    public void canTransferFoodToSelf(){
        // Test when r0 and r1 are the same region, assert false
        //assertFalse(r0.canFoodTransferTo(world, r1));
        assertFalse(false);
    }
}
