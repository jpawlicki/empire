package com.empire;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class NobleTest {
	private static final double EPSILON = 1E-6;

	@Test
	public void makeNoble() {
		Noble n = Noble.makeNoble(Culture.ANPILAYN, 0);
		assertEquals(0, n.unrest, EPSILON);
		assertNotEquals("", n.name);
		assertEquals(Crisis.Type.NONE, n.crisis.type);
		assertEquals(6, n.crisis.deadline);
	}

	@Test
	public void calcLevel() {
		Noble n = Noble.makeNoble(Culture.ANPILAYN, 0);
		assertEquals(1, n.calcLevel(), EPSILON);
		n.addExperience();
		assertEquals(Math.sqrt(2), n.calcLevel(), EPSILON);
		n.addExperience();
		assertEquals(Math.sqrt(3), n.calcLevel(), EPSILON);
		n.addExperience();
		assertEquals(2, n.calcLevel(), EPSILON);
	}
}
