package com.empire;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ArmyTest {
  @Test
  public void calcStrength_basic() {
		Army a = new Army();
		a.type = "army";
		a.size = 100;
		a.kingdom = "k1";
		a.location = 0;

		assertEquals(1.0, a.calcStrength(WorldTest.makeTestWorld(), null, 0, false), 0.00001);
  }
}
