package com.empire;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
		plainArmy.kingdom = Constants.pirateKingdom;
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
		world.regions.get(1).setKingdomNoScore("k2");
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

	@Test
	public void raze() {
		Region r = world.regions.get(0);
		r.population = 1000;
		r.religion = Ideology.SWORD_OF_TRUTH;
		// Nothing to raze.
		assertEquals(0, r.constructions.size());
		assertEquals(0, plainArmy.raze(world, "Raze temple Iruhan (Sword of Truth)", null, 0, false), DELTA);

		// Raze one temple.
		r.constructions.add(Construction.makeTemple(Ideology.SWORD_OF_TRUTH, 100));
		r.setReligion(null, world);
		assertEquals(r.religion, Ideology.SWORD_OF_TRUTH);
		assertEquals(80, plainArmy.raze(world, "Raze temple Iruhan (Sword of Truth)", null, 0, false), DELTA);
		assertEquals(r.religion, Ideology.SWORD_OF_TRUTH);
		assertEquals(0, r.constructions.size());
		assertEquals(0, plainArmy.raze(world, "Raze temple Iruhan (Sword of Truth)", null, 0, false), DELTA);

		// Razing temple changes religion.
		r.constructions.add(Construction.makeTemple(Ideology.SWORD_OF_TRUTH, 100));
		r.constructions.add(Construction.makeTemple(Ideology.SWORD_OF_TRUTH, 100));
		r.constructions.add(Construction.makeTemple(Ideology.VESSEL_OF_FAITH, 100));
		r.setReligion(null, world);
		assertEquals(Ideology.SWORD_OF_TRUTH, r.religion);
		assertEquals(80, plainArmy.raze(world, "Raze temple Iruhan (Sword of Truth)", null, 0, false), DELTA);
		assertEquals(Ideology.SWORD_OF_TRUTH, r.religion);
		assertEquals(80, plainArmy.raze(world, "Raze temple Iruhan (Sword of Truth)", null, 0, false), DELTA);
		assertEquals(1, r.constructions.size());
		assertEquals(Ideology.VESSEL_OF_FAITH, r.religion);
		assertEquals(0, plainArmy.raze(world, "Raze temple Iruhan (Sword of Truth)", null, 0, false), DELTA);
		assertEquals(1, r.constructions.size());

		// Razing requires a minimum strength.
		r.population = 1000000;
		assertEquals(0, plainArmy.raze(world, "Raze temple Iruhan (Vessel of Faith)", null, 0, false), DELTA);

		// Razing can raze multiple buildings in one action.
		r.population = 100;
		r.constructions.add(Construction.makeTemple(Ideology.SWORD_OF_TRUTH, 100));
		r.constructions.add(Construction.makeTemple(Ideology.SWORD_OF_TRUTH, 100));
		assertEquals(160, plainArmy.raze(world, "Raze temple Iruhan (Sword of Truth)", null, 0, false), DELTA);
	}

	@Test
	public void conquer() {
		// Basic conquest.
		Region r = world.regions.get(0);
		r.religion = Ideology.SWORD_OF_TRUTH;
		r.setKingdomNoScore("k2");
		HashSet<Region> conqueredRegions = new HashSet<>();
		plainArmy.conquer(world, "Conquer", conqueredRegions, new HashMap<String, List<String>>(), new HashMap<Army, Character>(), 0, new HashSet<String>());
		assertTrue(conqueredRegions.contains(r));
		assertEquals("k1", r.getKingdom());

		// Conquest requires a minimum size.
		r.population = 100000;
		r.setKingdomNoScore("k2");
		conqueredRegions.clear();
		plainArmy.conquer(world, "Conquer", conqueredRegions, new HashMap<String, List<String>>(), new HashMap<Army, Character>(), 0, new HashSet<String>());
		assertTrue(conqueredRegions.isEmpty());
		assertEquals("k2", r.getKingdom());

		// Conquest destroys fortifications.
		r.population = 100;
		r.constructions.add(Construction.makeFortification(40));
		r.constructions.add(Construction.makeFortification(40));
		r.constructions.add(Construction.makeFortification(40));
		plainArmy.conquer(world, "Conquer", conqueredRegions, new HashMap<String, List<String>>(), new HashMap<Army, Character>(), 0, new HashSet<String>());
		assertTrue(conqueredRegions.contains(r));
		assertEquals("k1", r.getKingdom());
		assertTrue(r.constructions.isEmpty());
	}
}
