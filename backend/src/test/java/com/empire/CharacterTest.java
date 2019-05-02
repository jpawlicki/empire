package com.empire;

import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CharacterTest {
    Character c;

    @Before
    public void createCharacter(){
        c = new Character();
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
    public void CalcLevelUnknownDimThrowsError(){
        // Not a great test but have something in here about unknown keys, could be fixed by using enum
        setExperience(1.0);
        c.calcLevel("DUMMY");
    }
}
