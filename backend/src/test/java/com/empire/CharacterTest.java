package com.empire;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CharacterTest {
    private Character c;
    private static World world;
    private static double delta = 1E-5;

    @Before
    public void createCharacter(){
        c = new Character();
        c.kingdom = "k1";
        setExperience(0.0);

        world = WorldTest.makeTestWorld();
    }

    private void setExperience(double exp){
        setExperience(exp, exp, exp, exp);
    }

    private void setExperience(double general, double admiral, double governor, double spy){
        c.experience.put(Constants.charDimGeneral, general);
        c.experience.put(Constants.charDimAdmiral, admiral);
        c.experience.put(Constants.charDimGovernor, governor);
        c.experience.put(Constants.charDimSpy, spy);
    }

    @Test
    public void calcLevelAll(){
        List<Double> expLevel = Arrays.asList(1.0, 4.0, 9.0, 16.0, 25.0);

        for(int level = 1; level <= 5; level++) {
            setExperience(expLevel.get(level-1));
            assertEquals(level, c.calcLevel(Constants.charDimGeneral));
            assertEquals(level, c.calcLevel(Constants.charDimAdmiral));
            assertEquals(level, c.calcLevel(Constants.charDimGovernor));
            assertEquals(level, c.calcLevel(Constants.charDimSpy));
        }
    }

    @Test(expected=NullPointerException.class)
    public void calcLevelUnknownDimThrowsError(){
        // Not a great test but have something in here about unknown keys, could be fixed by using enum
        c.calcLevel("DUMMY");
    }

    @Test
    public void addExperienceGeneralRegular(){
        c.addExperience(Constants.charDimGeneral, world);
        assertEquals(1.0, c.getExperience(Constants.charDimGeneral), delta);
        assertEquals(0.0, c.getExperience(Constants.charDimAdmiral), delta);
        assertEquals(0.0, c.getExperience(Constants.charDimGovernor), delta);
        assertEquals(0.0, c.getExperience(Constants.charDimSpy), delta);
    }

    @Test
    public void addExperienceAdmirallRegular(){
        c.addExperience(Constants.charDimAdmiral, world);
        assertEquals(0.0, c.getExperience(Constants.charDimGeneral), delta);
        assertEquals(1.0, c.getExperience(Constants.charDimAdmiral), delta);
        assertEquals(0.0, c.getExperience(Constants.charDimGovernor), delta);
        assertEquals(0.0, c.getExperience(Constants.charDimSpy), delta);
    }

    @Test
    public void addExperienceGovernorRegular(){
        c.addExperience(Constants.charDimGovernor, world);
        assertEquals(0.0, c.getExperience(Constants.charDimGeneral), delta);
        assertEquals(0.0, c.getExperience(Constants.charDimAdmiral), delta);
        assertEquals(1.0, c.getExperience(Constants.charDimGovernor), delta);
        assertEquals(0.0, c.getExperience(Constants.charDimSpy), delta);
    }

    @Test
    public void addExperienceSpyRegular(){
        c.addExperience(Constants.charDimSpy, world);
        assertEquals(0.0, c.getExperience(Constants.charDimGeneral), delta);
        assertEquals(0.0, c.getExperience(Constants.charDimAdmiral), delta);
        assertEquals(0.0, c.getExperience(Constants.charDimGovernor), delta);
        assertEquals(1.0, c.getExperience(Constants.charDimSpy), delta);
    }

    @Test
    public void addExperienceAllRegular(){
        c.addExperience(Constants.charDimAll, world);
        assertEquals(0.25, c.getExperience(Constants.charDimGeneral), delta);
        assertEquals(0.25, c.getExperience(Constants.charDimAdmiral), delta);
        assertEquals(0.25, c.getExperience(Constants.charDimGovernor), delta);
        assertEquals(0.25, c.getExperience(Constants.charDimSpy), delta);
    }
}
