package com.empire;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

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
    }
}
