package com.empire;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CharacterTest {
    private Character c;
    private static World w;
    private static Rules rules;
    private static NationData n1;
    private static Region r1;
    private static final double DELTA = 1E-5;

    private static final String k1 = "k1";

    @Before
    public void createCharacter() {
				try { 
					rules = Rules.loadRules(5);
				} catch (IOException e) {
					fail(e.getMessage());
				}
        c = new Character();
        c.kingdom = k1;
        setExperience(0.0);

        w = mockWorld();
    }

    private World mockWorld(){
        World world = mock(World.class);
				world.rules = rules;

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
        c.experience.put(rules.charDimGeneral, general);
        c.experience.put(rules.charDimAdmiral, admiral);
        c.experience.put(rules.charDimGovernor, governor);
        c.experience.put(rules.charDimSpy, spy);
    }

    private void addHeroicTag() {
        when(n1.hasTag(NationData.Tag.HEROIC)).thenReturn(true);
    }

    private void assertDimsExpEqual(double general, double admiral, double governor, double spy) {
        assertEquals(general, c.getExperience(rules.charDimGeneral), DELTA);
        assertEquals(admiral, c.getExperience(rules.charDimAdmiral), DELTA);
        assertEquals(governor, c.getExperience(rules.charDimGovernor), DELTA);
        assertEquals(spy, c.getExperience(rules.charDimSpy), DELTA);
    }

    @Test
    public void calcLevelAll() {
        List<Double> expLevel = Arrays.asList(1.0, 4.0, 9.0, 16.0, 25.0);

        for(int level = 1; level <= 5; level++) {
            setExperience(expLevel.get(level-1));
            assertEquals(level, c.calcLevel(rules.charDimGeneral));
            assertEquals(level, c.calcLevel(rules.charDimAdmiral));
            assertEquals(level, c.calcLevel(rules.charDimGovernor));
            assertEquals(level, c.calcLevel(rules.charDimSpy));
        }
    }

    @Test
    public void addExperienceGeneralRegular() {
        c.addExperience(rules.charDimGeneral, w);
        assertDimsExpEqual(1.0, 0.0, 0.0, 0.0);
    }

    @Test
    public void addExperienceAdmirallRegular() {
        c.addExperience(rules.charDimAdmiral, w);
        assertDimsExpEqual(0.0, 1.0, 0.0, 0.0);
    }

    @Test
    public void addExperienceGovernorRegular() {
        c.addExperience(rules.charDimGovernor, w);
        assertDimsExpEqual(0.0, 0.0, 1.0, 0.0);
    }

    @Test
    public void addExperienceSpyRegular() {
        c.addExperience(rules.charDimSpy, w);
        assertDimsExpEqual(0.0, 0.0, 0.0, 1.0);

    }

    @Test
    public void addExperienceAllRegular() {
        c.addExperience(rules.charDimAll, w);
        assertDimsExpEqual(0.25, 0.25, 0.25, 0.25);
    }

    @Test
    public void addExperienceGeneralHeroic() {
        addHeroicTag();
        c.addExperience(rules.charDimGeneral, w);
        assertDimsExpEqual(2.0, 0.0, 0.0, 0.0);
    }

    @Test
    public void addExperienceAdmiralHeroic() {
        addHeroicTag();
        c.addExperience(rules.charDimAdmiral, w);
        assertDimsExpEqual(0.0, 2.0, 0.0, 0.0);
    }

    @Test
    public void addExperienceGovernorHeroic() {
        addHeroicTag();
        c.addExperience(rules.charDimGovernor, w);
        assertDimsExpEqual(0.0, 0.0, 2.0, 0.0);
    }

    @Test
    public void addExperienceSpyHeroic() {
        addHeroicTag();
        c.addExperience(rules.charDimSpy, w);
        assertDimsExpEqual(0.0, 0.0, 0.0, 2.0);
    }

    @Test
    public void addExperienceAllHeroic() {
        addHeroicTag();
        c.addExperience(rules.charDimAll, w);
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
