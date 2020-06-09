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

	public static Noble noble(double unrest) {
		Noble n = mock(Noble.class);
		n.unrest.set(unrest);
		n.name = "DONTCARE";
		return n;
	}

	public static Character character() {
		return mock(Character.class);
	}
}
