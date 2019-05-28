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
        setExperience(0.0);

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

    private void setExperience(double exp) {
        setExperience(exp, exp, exp, exp);
    }

    private void setExperience(double general, double admiral, double governor, double spy) {
        c.experience.put(Constants.charDimGeneral, general);
        c.experience.put(Constants.charDimAdmiral, admiral);
        c.experience.put(Constants.charDimGovernor, governor);
        c.experience.put(Constants.charDimSpy, spy);
    }

    private void addHeroicTag() {
        when(n1.hasTag(NationData.Tag.HEROIC)).thenReturn(true);
    }

    private void assertDimsExpEqual(double general, double admiral, double governor, double spy) {
        assertEquals(general, c.getExperience(Constants.charDimGeneral), DELTA);
        assertEquals(admiral, c.getExperience(Constants.charDimAdmiral), DELTA);
        assertEquals(governor, c.getExperience(Constants.charDimGovernor), DELTA);
        assertEquals(spy, c.getExperience(Constants.charDimSpy), DELTA);
    }

    @Test
    public void calcLevelAll() {
        List<Double> expLevel = Arrays.asList(1.0, 4.0, 9.0, 16.0, 25.0);

        for(int level = 1; level <= 5; level++) {
            setExperience(expLevel.get(level-1));
            assertEquals(level, c.calcLevel(Constants.charDimGeneral));
            assertEquals(level, c.calcLevel(Constants.charDimAdmiral));
            assertEquals(level, c.calcLevel(Constants.charDimGovernor));
            assertEquals(level, c.calcLevel(Constants.charDimSpy));
        }
    }

    @Test
    public void addExperienceGeneralRegular() {
        c.addExperience(Constants.charDimGeneral, w);
        assertDimsExpEqual(1.0, 0.0, 0.0, 0.0);
    }

    @Test
    public void addExperienceAdmirallRegular() {
        c.addExperience(Constants.charDimAdmiral, w);
        assertDimsExpEqual(0.0, 1.0, 0.0, 0.0);
    }

    @Test
    public void addExperienceGovernorRegular() {
        c.addExperience(Constants.charDimGovernor, w);
        assertDimsExpEqual(0.0, 0.0, 1.0, 0.0);
    }

    @Test
    public void addExperienceSpyRegular() {
        c.addExperience(Constants.charDimSpy, w);
        assertDimsExpEqual(0.0, 0.0, 0.0, 1.0);

    }

    @Test
    public void addExperienceAllRegular() {
        c.addExperience(Constants.charDimAll, w);
        assertDimsExpEqual(0.25, 0.25, 0.25, 0.25);
    }

    @Test
    public void addExperienceGeneralHeroic() {
        addHeroicTag();
        c.addExperience(Constants.charDimGeneral, w);
        assertDimsExpEqual(2.0, 0.0, 0.0, 0.0);
    }

    @Test
    public void addExperienceAdmiralHeroic() {
        addHeroicTag();
        c.addExperience(Constants.charDimAdmiral, w);
        assertDimsExpEqual(0.0, 2.0, 0.0, 0.0);
    }

    @Test
    public void addExperienceGovernorHeroic() {
        addHeroicTag();
        c.addExperience(Constants.charDimGovernor, w);
        assertDimsExpEqual(0.0, 0.0, 2.0, 0.0);
    }

    @Test
    public void addExperienceSpyHeroic() {
        addHeroicTag();
        c.addExperience(Constants.charDimSpy, w);
        assertDimsExpEqual(0.0, 0.0, 0.0, 2.0);
    }

    @Test
    public void addExperienceAllHeroic() {
        addHeroicTag();
        c.addExperience(Constants.charDimAll, w);
        assertDimsExpEqual(0.5, 0.5, 0.5, 0.5);
    }

    @Test
    public void calcPlotPowerBasic() {
        assertEquals(1.3, c.calcPlotPower(w, false, 0), DELTA);
    }

    @Test
    public void calcPlotPowerSpyLevel() {
        setExperience(4.0);
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
