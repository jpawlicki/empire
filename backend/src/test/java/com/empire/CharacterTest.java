package com.empire;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CharacterTest {
    private Character c;
    private static World w;
    private static NationData n1;
    private static Region r1;
    private static final double DELTA = 1E-5;

    private static final String k1 = "k1";

    @Before
    public void createCharacter() {
        c = new Character();
        c.kingdom = k1;

        w = mockWorld();
    }

    private World mockWorld(){
        World world = mock(World.class);

        n1 = mock(NationData.class);
        when(world.getNation(k1)).thenReturn(n1);

        r1 = Mocks.region(k1, Region.Type.LAND, 1.0, Ideology.SWORD_OF_TRUTH);
        world.regions = Collections.singletonList(r1);

        return world;
    }

    @Test
    public void calcPlotPowerBasic() {
        assertEquals(1.3, c.calcPlotPower(w, false, 0), DELTA);
    }

    @Test
    public void calcPlotPowerSpyLevel() {
        c.addExperienceSpy();
        c.addExperienceSpy();
        c.addExperienceSpy();
        assertEquals(1.6, c.calcPlotPower(w, false, 0), DELTA);
    }

    @Test
    public void calcPlotPowerGuarding() {
        assertEquals(1.8, c.calcPlotPower(w, true, 0), DELTA);
    }

    @Test
    public void calcPlotPowerLyskr() {
        r1.religion = Ideology.LYSKR;
        assertEquals(1.7, c.calcPlotPower(w, false, 0), DELTA);
    }

    @Test
    public void calcPlotPowerCompany() {
        r1.religion = Ideology.COMPANY;
        assertEquals(1.5, c.calcPlotPower(w, false, 0), DELTA);
    }

    @Test
    public void calcPlotPowerInspireNonIruhan() {
        r1.religion = Ideology.ALYRJA;
        assertEquals(1.3, c.calcPlotPower(w, false, 2), DELTA);
    }

    @Test
    public void calcPlotPowerInspireIruhan() {
        r1.religion = Ideology.SWORD_OF_TRUTH;
        assertEquals(1.4, c.calcPlotPower(w, false, 2), DELTA);
    }

    @Test
    public void calcPlotPowerCaptive() {
        c.captor = "DONTCARE";
        assertEquals(0.8, c.calcPlotPower(w, false, 0), DELTA);
    }
}
