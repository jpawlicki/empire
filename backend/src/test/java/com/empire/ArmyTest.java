package com.empire;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class ArmyTest {
	private static Army plainArmy;
	private static World world;
	private static final double DELTA = 1E-5;

	private void addFortification() {
		Construction fort = new Construction();
		fort.type = Constants.constFort;
		world.regions.get(0).constructions = Arrays.asList(fort, fort);
	}

	private Character getLeader() {
		Character leader = new Character();
		leader.experience.put(Constants.charDimGeneral, 3.0);
		leader.experience.put(Constants.charDimAdmiral, 3.0);
		return leader;
	}

	private void makeSwordOfTruthDominant() {
		Region r = new Region();
		r.religion = Ideology.SWORD_OF_TRUTH;
		r.population = 1E9;
		world.regions.add(r);
	}

	@Before
	public void setUpPlainArmy() {
		plainArmy = new Army();
		plainArmy.type = Army.Type.ARMY;
		plainArmy.size = 100.0;
		plainArmy.kingdom = "k1";
		plainArmy.location = 0;

		world = WorldTest.armyTestWorld();
	}

	@Test
	public void calcStrengthBasic() {
		assertEquals(1.0, plainArmy.calcStrength(world, null, 0, false), DELTA);
	}

	@Test
	public void calcStrengthNavy() {
		plainArmy.type = Army.Type.NAVY;
		assertEquals(100.0, plainArmy.calcStrength(world, null, 0, false), DELTA);
	}

	@Test
	public void calcStrengthSteel() {
		plainArmy.addTag(Constants.armySteelTag);
		assertEquals(1.15, plainArmy.calcStrength(world, null, 0, false), DELTA);
	}

	@Test
	public void calcStrengthSeafaringOnLand() {
		plainArmy.addTag(Constants.armySeafaringTag);
		assertEquals(1.0, plainArmy.calcStrength(world, null, 0, false), DELTA);
	}

	@Test
	public void calcStrengthSeafaring() {
		plainArmy.addTag(Constants.armySeafaringTag);
		world.regions.get(0).type = Region.Type.WATER;
		assertEquals(2.5, plainArmy.calcStrength(world, null, 0, false), DELTA);
	}

	@Test
	public void calcStrengthDisciplinedNavy() {
		world.getNation("k1").addTag(Constants.nationDisciplinedTag);
		plainArmy.type = Army.Type.NAVY;
		assertEquals(100.0, plainArmy.calcStrength(world, null, 0, false), DELTA);
	}

	@Test
	public void calcStrengthDisciplinedPirate() {
		world.getNation("k1").addTag(Constants.nationDisciplinedTag);
		plainArmy.kingdom = Constants.armyPirateTag;
		assertEquals(1.0, plainArmy.calcStrength(world, null, 0, false), DELTA);
	}

	@Test
	public void calcStrengthDisciplined() {
		world.getNation("k1").addTag(Constants.nationDisciplinedTag);
		assertEquals(1.1, plainArmy.calcStrength(world, null, 0, false), DELTA);
	}

	@Test
	public void calcStrengthFortificationNavy() {
		addFortification();
		plainArmy.type = Army.Type.NAVY;
		assertEquals(100.0, plainArmy.calcStrength(world, null, 0, false), DELTA);
	}

	@Test
	public void calcStrengthFortificationWater() {
		addFortification();
		world.regions.get(0).type = Region.Type.WATER;
		assertEquals(1.0, plainArmy.calcStrength(world, null, 0, false), DELTA);
	}

	@Test
	public void calcStrengthFortificationNotFriendly() {
		addFortification();
		plainArmy.kingdom = "k2";
		assertEquals(1.0, plainArmy.calcStrength(world, null, 0, false), DELTA);
	}

	@Test
	public void calcStrengthFortification() {
		addFortification();
		assertEquals(1.3, plainArmy.calcStrength(world, null, 0, false), DELTA);
	}

	@Test
	public void calcStrengthLoyalNavy() {
		plainArmy.location = 1;
		plainArmy.type = Army.Type.NAVY;
		assertEquals(100.0, plainArmy.calcStrength(world, null, 0, false), DELTA);
	}

	@Test
	public void calcStrengthLoyalNotRegionOwner() {
		plainArmy.location = 1;
		world.regions.get(1).kingdom = "k2";
		assertEquals(1.0, plainArmy.calcStrength(world, null, 0, false), DELTA);
	}

	@Test
	public void calcStrengthLoyal() {
		plainArmy.location = 1;
		assertEquals(1.25, plainArmy.calcStrength(world, null, 0, false), DELTA);
	}

	@Test
	public void calcStrengthSwordOfTruthNonIruhan() {
		makeSwordOfTruthDominant();
		world.regions.get(0).religion = Ideology.ALYRJA;
		world.regions.get(0).population = 1.0;
		assertEquals(1.0, plainArmy.calcStrength(world, null, 0, false), DELTA);
	}

	@Test
	public void calcStrengthSwordOfTruthAligned() {
		makeSwordOfTruthDominant();
		world.regions.get(0).religion = Ideology.SWORD_OF_TRUTH;
		world.regions.get(0).population = 1.0;
		assertEquals(1.25, plainArmy.calcStrength(world, null, 0, false), DELTA);
	}

	@Test
	public void calcStrengthSwordOfTruthNonAligned() {
		makeSwordOfTruthDominant();
		world.regions.get(0).religion = Ideology.CHALICE_OF_COMPASSION;
		world.regions.get(0).population = 1.0;
		assertEquals(1.15, plainArmy.calcStrength(world, null, 0, false), DELTA);
	}

	@Test
	public void calcStrengthLastStand() {
		assertEquals(5.0, plainArmy.calcStrength(world, null, 0, true), DELTA);
	}

	@Test
	public void calcStrengthInspireNavy() {
		world.regions.get(0).religion = Ideology.CHALICE_OF_COMPASSION;
		plainArmy.type = Army.Type.NAVY;
		assertEquals(100.0, plainArmy.calcStrength(world, null, 2, false), DELTA);
	}

	@Test
	public void calcStrengthInspire() {
		world.regions.get(0).religion = Ideology.CHALICE_OF_COMPASSION;
		world.regions.get(0).population = 1.0;
		assertEquals(1.1, plainArmy.calcStrength(world, null, 2, false), DELTA);
	}

	@Test
	public void calcStrengthCaptured() {
		Character leader = getLeader();
		leader.captor = "DONTCARE";
		assertEquals(1.0, plainArmy.calcStrength(world, leader, 0, false), DELTA);
	}

	@Test
	public void calcStrengthGeneral() {
		assertEquals(1.4, plainArmy.calcStrength(world, getLeader(), 0, false), DELTA);
	}

	@Test
	public void calcStrengthAdmiral() {
		plainArmy.type = Army.Type.NAVY;
		assertEquals(140.0, plainArmy.calcStrength(world, getLeader(), 0, false), DELTA);
	}
}
