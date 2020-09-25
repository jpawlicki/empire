package com.empire;

import java.io.IOException;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpyRingTest {
	private static final double EPSILON = 1E-5;

	@Test
	public void belongsTo() {
		SpyRing r = SpyRing.newSpyRing("nation", 0);
		assertFalse(r.belongsTo("defender"));
		assertTrue(r.belongsTo("nation"));
	}
}
