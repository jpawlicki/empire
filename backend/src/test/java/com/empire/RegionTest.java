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
    private static World w;
    private static NationData n1;
    private static final double DELTA = 1E-5;

    private static final String k1 = "k1";
    private static final String k2 = "k2";
    private static final String k3 = "k3";

    private static final double unrestMiddle = 0.25;
    private static final double unrestLower = 0.15;
    private static final double unrestHigher = 0.35;

    @Before
    public void setUpRegion() {
        r = new Region();
        r.setKingdomNoScore(k1);
        r.religion = Ideology.COMPANY;
        r.unrestPopular = unrestMiddle;

//        w = WorldTest.regionTestWorld();
        w = mockWorld(r);
    }

    private World mockWorld(Region r){
        World world = mock(World.class);
        Region r1 = mockRegion(k1, Region.Type.LAND, Ideology.CHALICE_OF_COMPASSION);
        Region r2 = mockRegion(k1, Region.Type.LAND, Ideology.CHALICE_OF_COMPASSION);
        Region r3 = mockRegion(k1, Region.Type.LAND, Ideology.SWORD_OF_TRUTH);
        Region r4 = mockRegion(k2, Region.Type.LAND, null);
        Region r5 = mockRegion(k3, Region.Type.WATER, null);
        world.regions = Arrays.asList(r, r1, r2, r3, r4, r5);

        n1 = mock(NationData.class);
        when(world.getNation(k1)).thenReturn(n1);

        return world;
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
        assertEquals(2, Region.numUniqueIdeologies(k1, w));
        assertEquals(2, Region.numUniqueIdeologies2(k1, w));
    }

    // TODO:  The can transfer food tests rely on making a test w that is too complex at the moment, these should
    // be revisited if/once refactoring has made this more feasible

    @Test
    public void canTransferFoodToAdjacent(){
        // Test when r0 and r1 are directly adjacent one another, assert true
        //assertTrue(r0.canFoodTransferTo(w, r1));
        assertTrue(true);
    }

    @Test
    public void canTransferFoodToViaOcean(){
        // Test when r0 and r1 connect via ocean, assert true
        //assertTrue(r0.canFoodTransferTo(w, r1));
        assertTrue(true);
    }

    @Test
    public void canTransferFoodToFalse(){
        // Test when r0 and r1 are neither adjacent nor connected via ocean, assert false
        //assertFalse(r0.canFoodTransferTo(w, r1));
        assertFalse(false);
    }

    @Test
    public void canTransferFoodToSelf(){
        // Test when r0 and r1 are the same region, assert false
        //assertFalse(r0.canFoodTransferTo(w, r1));
        assertFalse(false);
    }

    @Test
    public void calcUnrestClericalNonIruhan(){
        r.religion = Ideology.ALYRJA;
        assertEquals(0.0, r.calcUnrestClerical(w), DELTA);
    }

    @Test
    public void calcUnrestVesselOfFaith(){
        r.religion = Ideology.VESSEL_OF_FAITH;
        assertEquals(0.0, r.calcUnrestClerical(w), DELTA);
    }

    @Test
    public void calcUnrestClericalIruhan(){
        r.religion = Ideology.SWORD_OF_TRUTH;
        n1.goodwill = -100.0;
        assertEquals(1.0, r.calcUnrestClerical(w), DELTA);
    }
    
    @Test
    public void calcUnrestBasic(){
        assertEquals(unrestMiddle, r.calcUnrest(w), DELTA);
    }
}
