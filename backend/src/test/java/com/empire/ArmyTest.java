package com.empire;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ArmyTest {
  @Test
  public void calcStrength_basic() {
		Army a = new Army();
		a.type = Army.Type.ARMY;
		a.size = 100;
		a.kingdom = "k1";
		a.location = 0;

		assertEquals(1.0, a.calcStrength(WorldTest.makeTestWorld(), null, 0, false), 0.00001);
  }

  @Test
  public void calcStrength_inspires() {
		// For a non-religious nation, inspires don't do anything.
		{
			Army a = new Army();
			a.type = Army.Type.ARMY;
			a.size = 100;
			a.kingdom = "k1";
			a.location = 0;
			assertEquals(1.0, a.calcStrength(WorldTest.makeTestWorld(), null, 10, false), 0.00001);
		}
		// For an Iruhan-aligned nation, inspires do.
		{
			Army a = new Army();
			a.type = Army.Type.ARMY;
			a.size = 100;
			a.kingdom = "k1";
			a.location = 0;
			World w = WorldTest.makeTestWorld();
			w.regions.get(0).religion = Ideology.VESSEL_OF_FAITH;
			w.regions.get(0).kingdom = "k1";
			w.regions.get(0).population = 1;
			assertEquals(Religion.IRUHAN, NationData.getStateReligion("k1", w).religion);
			assertEquals(1.5, a.calcStrength(w, null, 10, false), 0.00001);
		}
  }

  @Test
  public void calcStrength_lastStand() {
		// Last-standing quintuples strength.
		{
			Army a = new Army();
			a.type = Army.Type.ARMY;
			a.size = 100;
			a.kingdom = "k1";
			a.location = 0;
			assertEquals(5.0, a.calcStrength(WorldTest.makeTestWorld(), null, 0, true), 0.00001);
		}
  }
}
