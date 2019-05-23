package com.empire;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Mocks {
    public static Region region(String kingdom, Region.Type type, double population, Ideology religion){
        Region r = mock(Region.class);
        when(r.getKingdom()).thenReturn(kingdom);
        r.type = type;
        r.population = population;
        r.religion = religion;
        return r;
    }

    public static Noble noble(String nobleTag, double unrest) {
        Noble n = mock(Noble.class);
        n.unrest = unrest;
        n.name = "DONTCARE";
        when(n.hasTag(nobleTag)).thenReturn(true);
        return n;
    }
}
