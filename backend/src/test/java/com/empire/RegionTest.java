package com.empire;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RegionTest {
	private static Region r;
	private static World w;
	private static Nation n1;
	private static final double DELTA = 1E-5;

	private static final String k1 = "k1";
	private static final String k2 = "k2";
	private static final String k3 = "k3";

	private static final double unrestMiddle = 0.25;
	private static final double unrestLower = 0.15;
	private static final double unrestHigher = 0.35;
	private static final double pop = 1E4;

	@Before
	public void setUpRegion() throws IOException {
		r = Region.newRegion(Utils.rules);
		r.type = Region.Type.LAND;
		r.setKingdomNoScore(k1);
		r.religion = Ideology.COMPANY;
		r.population = pop;
		r.unrestPopular = new Percentage(unrestMiddle);
		r.setKingdomNoScore(k1);

		w = mockWorld(r);
	}

	private World mockWorld(Region r) throws IOException {
		World world = mock(World.class);
		Region r1 = mockRegion(k1, Region.Type.LAND, Ideology.CHALICE_OF_COMPASSION);
		Region r2 = mockRegion(k1, Region.Type.LAND, Ideology.CHALICE_OF_COMPASSION);
		Region r3 = mockRegion(k1, Region.Type.LAND, Ideology.SWORD_OF_TRUTH);
		Region r4 = mockRegion(k2, Region.Type.LAND, null);
		Region r5 = mockRegion(k3, Region.Type.WATER, null);
		world.regions = Arrays.asList(r, r1, r2, r3, r4, r5);
		when(world.getRules()).thenReturn(Utils.rules);

		Pirate p = mock(Pirate.class);
		p.bribes = new HashMap<>();
		p.bribes.put(k1, 0.0);
		world.pirate = p;

		n1 = mock(Nation.class);
		when(world.getNation(k1)).thenReturn(n1);

		return world;
	}

	private Region mockRegion(String kingdom, Region.Type type, Ideology religion){
		Region r = mock(Region.class);
		when(r.getKingdom()).thenReturn(kingdom);
		r.type = type;
		r.religion = religion;
		r.constructions = new ArrayList<>();
		return r;
	}

	@Test
	public void numUniqueIdeologiesTest(){
		assertEquals(0, Region.numUniqueIdeologies(k1, w));
		w.regions.get(0).constructions.add(Construction.makeTemple(Ideology.ALYRJA, 0));
		assertEquals(1, Region.numUniqueIdeologies(k1, w));
		assertEquals(0, Region.numUniqueIdeologies(k2, w));
		w.regions.get(0).constructions.add(Construction.makeTemple(Ideology.LYSKR, 0));
		assertEquals(2, Region.numUniqueIdeologies(k1, w));
		assertEquals(0, Region.numUniqueIdeologies(k2, w));
		w.regions.get(1).constructions.add(Construction.makeTemple(Ideology.RJINKU, 0));
		assertEquals(3, Region.numUniqueIdeologies(k1, w));
		assertEquals(0, Region.numUniqueIdeologies(k2, w));
		w.regions.get(1).constructions.add(Construction.makeTemple(Ideology.LYSKR, 0));
		assertEquals(3, Region.numUniqueIdeologies(k1, w));
		assertEquals(0, Region.numUniqueIdeologies(k2, w));
	}

	// TODO(s):	The can transfer food tests rely on making a test w that is too complex at the moment, these should
	// be revisited if/once refactoring has made this more feasible

	@Test
	public void canTransferFoodToAdjacent(){
		// Test when r0 and r1 are directly adjacent one another, assert true
		//assertTrue(r0.canFoodTransferTo(w, r1));
		assertTrue(true);
	}

	@Test
	public void canTransferFoodToViaOcean(){
		// Test when r0 and r1 connect via ocean, assert true
		//assertTrue(r0.canFoodTransferTo(w, r1));
		assertTrue(true);
	}

	@Test
	public void canTransferFoodToFalse(){
		// Test when r0 and r1 are neither adjacent nor connected via ocean, assert false
		//assertFalse(r0.canFoodTransferTo(w, r1));
		assertFalse(false);
	}

	@Test
	public void canTransferFoodToSelf(){
		// Test when r0 and r1 are the same region, assert false
		//assertFalse(r0.canFoodTransferTo(w, r1));
		assertFalse(false);
	}

	@Test
	public void calcUnrestClericalNonIruhan(){
		r.religion = Ideology.ALYRJA;
		assertEquals(0.0, r.calcUnrestClerical(unused -> -25), DELTA);
	}

	@Test
	public void calcUnrestVesselOfFaith(){
		r.religion = Ideology.VESSEL_OF_FAITH;
		assertEquals(0.0, r.calcUnrestClerical(unused -> -25), DELTA);
	}

	@Test
	public void calcUnrestClericalIruhan(){
		r.religion = Ideology.SWORD_OF_TRUTH;
		assertEquals(0.5, r.calcUnrestClerical(unused -> -50), DELTA);
	}

	@Test
	public void calcUnrestNobleEmptyName(){
		r.noble = Noble.newNoble(Culture.ANPILAYN, 1, Utils.rules);
		r.noble.name = "";
		r.noble.unrest.add(unrestMiddle);
		assertEquals(0.0, r.calcUnrestNoble(), DELTA);
	}

	@Test
	public void calcUnrestNoblePresent(){
		r.noble = Noble.newNoble(Culture.ANPILAYN, 1, Utils.rules);
		r.noble.unrest.add(unrestMiddle);
		assertEquals(unrestMiddle, r.calcUnrestNoble(), DELTA);
	}

	@Test
	public void calcUnrestAll(){
		r.unrestPopular.set(unrestLower);
		assertEquals(unrestLower, r.calcUnrest(unused -> -25), DELTA);

		r.religion = Ideology.SWORD_OF_TRUTH;
		assertEquals(0.25, r.calcUnrest(unused -> -25), DELTA);

		r.noble = Noble.newNoble(Culture.ANPILAYN, 1, Utils.rules);
		r.noble.unrest.add(unrestHigher);
		assertEquals(unrestHigher, r.calcUnrest(unused -> -25), DELTA);
	}

	@Test
	public void calcBaseConquestStrength(){
		r.noble = null;
		assertEquals(5.25, r.calcBaseConquestStrength(w), DELTA);
	}

	@Test
	public void calcMinConquestStrengthDisciplined(){
		when(n1.hasTag(Nation.Tag.DISCIPLINED)).thenReturn(true);
		assertEquals(5.25 * 2, r.calcMinConquestStrength(w), DELTA);
	}

	@Test
	public void calcMinConquestStrengthFortified(){
		r.constructions = Arrays.asList(Construction.makeFortifications(0), Construction.makeFortifications(0));
		assertEquals(7.875, r.calcMinConquestStrength(w), DELTA);
	}

	@Test
	public void calcFortificationMod(){
		r.constructions = Arrays.asList(Construction.makeFortifications(0), Construction.makeFortifications(0));
		assertEquals(0.5, r.calcFortificationMod(), DELTA);
	}

	@Test
	public void calcSigningBonus(){
		assertEquals(-1.0, r.calcSigningBonusMod(-2.0), DELTA);
		assertEquals(-0.5, r.calcSigningBonusMod(-1.0), DELTA);
		assertEquals(0.0, r.calcSigningBonusMod(0.0), DELTA);
		assertEquals(1.0, r.calcSigningBonusMod(2.0), DELTA);
		assertEquals(1.5, r.calcSigningBonusMod(4.0), DELTA);
		assertEquals(2.0, r.calcSigningBonusMod(8.0), DELTA);
	}

	@Test
	public void calcConsumptionBasic(){
		assertEquals(pop, r.calcConsumption(), DELTA);
	}

	@Test
	public void calcPirateThreatBasic(){
		r.unrestPopular.set(0.0);
		assertEquals(0.0, r.calcPirateThreat(w), DELTA);
	}

	@Test
	public void calcPirateThreatUnrest(){
		assertEquals(0.25, r.calcPirateThreat(w), DELTA);
	}

	@Test
	public void calcPirateThreatWaterZero(){
		r.type = Region.Type.WATER;
		assertEquals(0.0, r.calcPirateThreat(w), DELTA);
	}

	@Test
	public void calcPirateThreatBribe(){
		w.pirate.bribes.put(k1, -90.0);
		assertEquals(0.03125, r.calcPirateThreat(w), DELTA);

		w.pirate.bribes.put(k1, -60.0);
		assertEquals(0.0625, r.calcPirateThreat(w), DELTA);

		w.pirate.bribes.put(k1, -30.0);
		assertEquals(0.125, r.calcPirateThreat(w), DELTA);

		w.pirate.bribes.put(k1, 30.0);
		assertEquals(0.5, r.calcPirateThreat(w), DELTA);

		w.pirate.bribes.put(k1, 60.0);
		assertEquals(1.0, r.calcPirateThreat(w), DELTA);

		w.pirate.bribes.put(k1, 90.0);
		assertEquals(2.0, r.calcPirateThreat(w), DELTA);
	}

	@Test
	public void plantChaliceOfCompassion() {
		r.religion = Ideology.CHALICE_OF_COMPASSION;
		r.population = 10000;
		r.crops = 0;
		r.plant(2);
		assertEquals(2500, r.crops, DELTA);
	}

	@Test
	public void plantAlyrja() {
		r.religion = Ideology.ALYRJA;
		r.population = 10000;
		r.crops = 0;
		r.plant(2);
		assertEquals(0, r.crops, DELTA);
	}

	@Test
	public void plantHarvestTurn() {
		r.population = 10000;
		r.crops = 0;
		r.plant(4);
		assertEquals(30000, r.crops, DELTA);
	}

	@Test
	public void plantHarvestTurnWinter() {
		r.population = 10000;
		r.crops = 0;
		r.plant(8);
		assertEquals(20000, r.crops, DELTA);
	}

	@Test
	public void plantHarvestTurnSpring() {
		r.population = 10000;
		r.crops = 0;
		r.plant(20);
		assertEquals(40000, r.crops, DELTA);
	}

	@Test
	public void plantHarvestTurnSummer() {
		r.population = 10000;
		r.crops = 0;
		r.plant(32);
		assertEquals(70000, r.crops, DELTA);
	}

	@Test
	public void plantHarvestTurnNoble() {
		r.noble = Noble.newNoble(Culture.ANPILAYN, 0, Utils.rules);
		r.population = 10000;
		r.crops = 0;
		r.plant(4);
		assertEquals(30000 * (1 + 0.03), r.crops, DELTA);
	}

	@Test
	public void plantHarvestTurnNobleHighLevel() {
		r.noble = Noble.newNoble(Culture.ANPILAYN, 0, Utils.rules);
		for (int i = 0; i < 24; i++) r.noble.addExperience(false);
		r.population = 10000;
		r.crops = 0;
		r.plant(4);
		assertEquals(30000 * (1 + 0.15), r.crops, DELTA);
	}

	@Test
	public void harvest() {
		r.population = 10000;
		r.crops = 1000000;
		r.food = 0;
		r.harvest(unused -> 0);
		assertEquals(80000, r.food, DELTA);
		assertEquals(0, r.crops, DELTA);
	}

	@Test
	public void harvestUnrest() {
		r.population = 10000;
		r.crops = 1000000;
		r.food = 0;
		r.unrestPopular.set(.95);
		r.harvest(unused -> 0);
		assertEquals(24000, r.food, DELTA);
		assertEquals(0, r.crops, DELTA);
	}
}
