package com.empire;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class NobleTest {
	private static final double EPSILON = 1E-6;

	@Test
	public void newNoble() {
		Noble n = Noble.newNoble(Culture.ANPILAYN, 0, Utils.rules);
		assertEquals(0, n.unrest.get(), EPSILON);
		assertNotEquals("", n.name);
		assertEquals(Crisis.Type.NONE, n.crisis.type);
		assertEquals(6, n.crisis.deadline);
	}

	@Test
	public void calcLevel() {
		Noble n = Noble.newNoble(Culture.ANPILAYN, 0, Utils.rules);
		assertEquals(1, n.calcLevel(), EPSILON);
		n.addExperience(false);
		assertEquals(Math.sqrt(2), n.calcLevel(), EPSILON);
		n.addExperience(false);
		assertEquals(Math.sqrt(3), n.calcLevel(), EPSILON);
		n.addExperience(false);
		assertEquals(2, n.calcLevel(), EPSILON);
	}
}
