package com.empire;

import java.util.ArrayList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Mocks {
    public static Region region(String kingdom, Region.Type type, double population, Ideology religion) {
        Region r = mock(Region.class);
        when(r.getKingdom()).thenReturn(kingdom);
        r.type = type;
        r.population = population;
        r.religion = religion;
        r.constructions = new ArrayList<>();
        return r;
    }

    public static Noble noble(String nobleTag, double unrest) {
        Noble n = mock(Noble.class);
        n.unrest = unrest;
        n.name = "DONTCARE";
        when(n.hasTag(nobleTag)).thenReturn(true);
        return n;
    }

    public static Character character(double general, double admiral, double governor, double spy) {
        Character c = mock(Character.class);
        c.captor = Constants.noCaptor;
        when(c.getExperience(Constants.charDimGeneral)).thenReturn(general);
        when(c.getExperience(Constants.charDimAdmiral)).thenReturn(admiral);
        when(c.getExperience(Constants.charDimGovernor)).thenReturn(governor);
        when(c.getExperience(Constants.charDimSpy)).thenReturn(spy);
        return c;
    }

    public static Character character(double xp) {
        return Mocks.character(xp, xp, xp, xp);
    }
}
