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
	private static Nation n1;
	private static Region r1;
	private static final double EPSILON = 1E-5;

	private static final String k1 = "k1";

	@Before
	public void createCharacter() throws IOException {
		c = Character.newCharacter(Utils.rules);
		c.kingdom = k1;

		w = mockWorld();
	}

	private World mockWorld() throws IOException {
		World world = mock(World.class);

		n1 = mock(Nation.class);
		when(world.getNation(k1)).thenReturn(n1);

		r1 = Mocks.region(k1, Region.Type.LAND, 1.0, Ideology.SWORD_OF_TRUTH);
		world.regions = Collections.singletonList(r1);
		when(world.getRules()).thenReturn(Utils.rules);

		return world;
	}
}
