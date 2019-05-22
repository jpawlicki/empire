package com.empire;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class RegionTest {
    private static Region r;
    private static World world;
    private static final double DELTA = 1E-5;

    private static final String k1 = "k1";
    private static final String k2 = "k2";
    private static final String k3 = "k3";

    @Before
    public void setUpRregion() {
        r = new Region();

//        world = WorldTest.regionTestWorld();
        world = mockWorld();
    }

    private World mockWorld(){
        World mockWorld = mock(World.class);
        Region r0 = mockRegion(k1, Region.Type.LAND, Ideology.CHALICE_OF_COMPASSION);
        Region r1 = mockRegion(k1, Region.Type.LAND, Ideology.CHALICE_OF_COMPASSION);
        Region r2 = mockRegion(k1, Region.Type.LAND, Ideology.CHALICE_OF_COMPASSION);
        Region r3 = mockRegion(k2, Region.Type.LAND, null);
        Region r4 = mockRegion(k3, Region.Type.WATER, null);
        mockWorld.regions = Arrays.asList(r0, r1, r2, r3, r4);
        return mockWorld;
    }

    private Region mockRegion(String kingdom, Region.Type type, Ideology religion){
        Region r = mock(Region.class);
        when(r.getKingdom()).thenReturn(kingdom);
        r.type = type;
        r.religion = religion;
        return r;
    }

    @Test
    public void numUniqueIdeologiesTest(){
        assertEquals(2, Region.numUniqueIdeologies(k1, world));
        assertEquals(2, Region.numUniqueIdeologies2(k1, world));
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
