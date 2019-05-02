package com.empire;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class ArmyTest {
	public static Army plainArmy;
	public static World world;
	public static double delta = 1E-5;

	@Before
	public void setUpPlainArmy(){
		plainArmy = new Army();
		plainArmy.type = Army.Type.ARMY;
		plainArmy.size = 100.0;
		plainArmy.kingdom = "k1";
		plainArmy.location = 0;

		world = WorldTest.makeTestWorld();
	}

	@Test
	public void calcStrengthBasic(){
		assertEquals(1.0, plainArmy.calcStrength(world, null, 0, false), delta);
	}

	@Test
	public void calcStrengthNavy(){
		plainArmy.type = Army.Type.NAVY;
		assertEquals(100.0, plainArmy.calcStrength(world, null, 0, false), delta);
	}

	@Test
	public void calcStrengthSteel(){
		plainArmy.addTag(Constants.armySteelTag);
		assertEquals(1.15, plainArmy.calcStrength(world, null, 0, false), delta);
	}

	@Test
	public void calcStrengthSeafaring(){
		plainArmy.addTag(Constants.armySeafaringTag);

		assertEquals(1.0, plainArmy.calcStrength(world, null, 0, false), delta);

		world.regions.get(0).type = Region.Type.WATER;
		assertEquals(2.5, plainArmy.calcStrength(world, null, 0, false), delta);
	}

	@Test
	public void calcStrengthDisciplined(){
		world.getNation("k1").addTag(Constants.nationDisciplinedTag);

		plainArmy.type = Army.Type.NAVY;
		assertEquals(100.0, plainArmy.calcStrength(world, null, 0, false), delta);
		plainArmy.type = Army.Type.ARMY;

		plainArmy.kingdom = Constants.armyPirateTag;
		assertEquals(1.0, plainArmy.calcStrength(world, null, 0, false), delta);
		plainArmy.kingdom = "k1";

		assertEquals(1.1, plainArmy.calcStrength(world, null, 0, false), delta);
	}

	@Test
	public void calcStrengthFortification(){
		Construction fort = new Construction();
		fort.type = Constants.constFort;
		world.regions.get(0).constructions = Collections.singletonList(fort);

		plainArmy.type = Army.Type.NAVY;
		assertEquals(100.0, plainArmy.calcStrength(world, null, 0, false), delta);
		plainArmy.type = Army.Type.ARMY;

		world.regions.get(0).type = Region.Type.WATER;
		assertEquals(1.0, plainArmy.calcStrength(world, null, 0, false), delta);
		world.regions.get(0).type = Region.Type.LAND;

		plainArmy.kingdom = "k2";
		assertEquals(1.0, plainArmy.calcStrength(world, null, 0, false), delta);
		plainArmy.kingdom = "k1";

		assertEquals(1.15, plainArmy.calcStrength(world, null, 0, false), delta);
	}

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
