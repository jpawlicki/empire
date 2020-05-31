package com.empire;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlotTest {
	private static final double EPSILON = 1E-5;

	@Test
	public void plotTypeAssassinateOnSuccess() {
		Character c1 = mock(Character.class);
		Character c2 = mock(Character.class);
		c2.name = "Lucky Guy";
		World w = mock(World.class);
		w.characters = new ArrayList<>(Arrays.asList(c1, c2));
		when(w.getCharacterByName("Unfortunate Guy")).thenReturn(Optional.of(c1));
		Plot.PlotType.ASSASSINATE.onSuccess("Unfortunate Guy", w, null);
		assertEquals(1, w.characters.size());
		assertEquals("Lucky Guy", w.characters.get(0).name);
	}

	@Test
	public void plotTypeAssassinateGetDefender() {
		World w = mock(World.class);
		Character c1 = mock(Character.class);
		c1.kingdom = "Foo";
		w.characters = Arrays.asList(c1);
		when(w.getCharacterByName("Unfortunate Guy")).thenReturn(Optional.of(c1));
		assertEquals("Foo", Plot.PlotType.ASSASSINATE.getDefender("Unfortunate Guy", w).get());
	}

	@Test
	public void plotTypeBurnShipyardOnSuccess() {
		World w = mock(World.class);
		Region r0 = mock(Region.class);
		r0.constructions = new ArrayList<>(Arrays.asList(Construction.makeFortifications(10), Construction.makeShipyard(10)));
		r0.name = "Foo";
		w.regions = Arrays.asList(r0);
		Plot.PlotType.BURN_SHIPYARD.onSuccess("Foo", w, null);
		assertEquals(1, r0.constructions.size());
		assertEquals(Construction.Type.FORTIFICATIONS, r0.constructions.get(0).type);
	}

	@Test
	public void plotTypeRegionalGetTargetRegion() {
		World w = mock(World.class);
		Region r0 = mock(Region.class);
		Region r1 = mock(Region.class);
		Region r2 = mock(Region.class);
		r0.name = "Foo";
		r1.name = "Foo1";
		r2.name = "Foo2";
		w.regions = Arrays.asList(r1, r0, r2);
		for (Plot.PlotType p : new Plot.PlotType[] {
			Plot.PlotType.BURN_SHIPYARD,
			Plot.PlotType.SABOTAGE_FORTIFICATIONS,
			Plot.PlotType.SPOIL_FOOD,
			Plot.PlotType.SPOIL_CROPS,
			Plot.PlotType.INCITE_UNREST,
			Plot.PlotType.PIN_FOOD,
			Plot.PlotType.MURDER_NOBLE,
			Plot.PlotType.POISON_RELATIONS}) {
			assertEquals("Plot type " + p, r0, p.getTargetRegion("Foo", w).get());
		}
	}

	@Test
	public void plotTypeRegionalGetDefender() {
		World w = mock(World.class);
		Region r0 = mock(Region.class);
		Region r1 = mock(Region.class);
		Region r2 = mock(Region.class);
		r0.name = "Foo";
		r1.name = "Foo1";
		r2.name = "Foo2";
		when(r0.getKingdom()).thenReturn("nation");
		w.regions = Arrays.asList(r1, r0, r2);
		for (Plot.PlotType p : new Plot.PlotType[] {
			Plot.PlotType.BURN_SHIPYARD,
			Plot.PlotType.SABOTAGE_FORTIFICATIONS,
			Plot.PlotType.SPOIL_FOOD,
			Plot.PlotType.SPOIL_CROPS,
			Plot.PlotType.INCITE_UNREST,
			Plot.PlotType.PIN_FOOD,
			Plot.PlotType.MURDER_NOBLE,
			Plot.PlotType.POISON_RELATIONS}) {
			assertEquals("Plot type " + p, "nation", p.getDefender("Foo", w).get());
		}
	}

	@Test
	public void plotTypeSabotageFortificationsOnSuccess() {
		World w = mock(World.class);
		Region r0 = mock(Region.class);
		r0.constructions = new ArrayList<>(Arrays.asList(Construction.makeFortifications(10), Construction.makeShipyard(10)));
		r0.name = "Foo";
		w.regions = Arrays.asList(r0);
		Plot.PlotType.SABOTAGE_FORTIFICATIONS.onSuccess("Foo", w, null);
		assertEquals(1, r0.constructions.size());
		assertEquals(Construction.Type.SHIPYARD, r0.constructions.get(0).type);
	}

	@Test
	public void plotTypeSpoilFoodOnSuccess() {
		World w = mock(World.class);
		Region r0 = mock(Region.class);
		r0.food = 100;
		r0.name = "Foo";
		w.regions = Arrays.asList(r0);
		Plot.PlotType.SPOIL_FOOD.onSuccess("Foo", w, null);
		assertEquals(50, r0.food, EPSILON);
	}

	@Test
	public void plotTypeSpoilCropsOnSuccess() {
		World w = mock(World.class);
		Region r0 = mock(Region.class);
		r0.crops = 100;
		r0.name = "Foo";
		w.regions = Arrays.asList(r0);
		Plot.PlotType.SPOIL_CROPS.onSuccess("Foo", w, null);
		assertEquals(50, r0.crops, EPSILON);
	}

	@Test
	public void plotTypeInciteUnrestOnSuccess() {
		World w = mock(World.class);
		Region r0 = mock(Region.class);
		r0.unrestPopular = 0;
		r0.name = "Foo";
		w.regions = Arrays.asList(r0);
		Plot.PlotType.INCITE_UNREST.onSuccess("Foo", w, null);
		assertEquals(.4, r0.unrestPopular, EPSILON);
	}

	@Test
	public void plotTypePinFoodOnSuccess() {
		World w = mock(World.class);
		Region r0 = Region.newRegion(Utils.rules);
		r0.name = "Foo";
		w.regions = Arrays.asList(r0);
		Plot.PlotType.PIN_FOOD.onSuccess("Foo", w, null);
		assertTrue(r0.getFoodPinned());
	}

	@Test
	public void murderNobleOnSuccess() {
		World w = mock(World.class);
		Region r0 = mock(Region.class);
		r0.name = "Foo";
		r0.culture = Culture.ANPILAYN;
		r0.noble = Noble.newNoble(Utils.rules);
		r0.noble.name = "Bob";
		r0.noble.addExperience();
		when(r0.hasNoble()).thenReturn(true);
		when(w.getRules()).thenReturn(Utils.rules);
		w.regions = Arrays.asList(r0);
		Plot.PlotType.MURDER_NOBLE.onSuccess("Foo", w, null);
		assertEquals(1, r0.noble.calcLevel(), EPSILON);
		assertFalse("Bob".equals(r0.noble.name));
	}

	@Test
	public void poisonRelationsOnSuccess() {
		World w = mock(World.class);
		Region r0 = mock(Region.class);
		r0.name = "Foo";
		r0.culture = Culture.ANPILAYN;
		r0.noble = Noble.newNoble(Culture.ANPILAYN, 0, Utils.rules);
		w.regions = Arrays.asList(r0);
		assertEquals(0, r0.noble.unrest, EPSILON);
		Plot.PlotType.POISON_RELATIONS.onSuccess("Foo", w, null);
		assertEquals(.15, r0.noble.unrest, EPSILON);
	}

	@Test
	public void plotTypeChurchGetTargetRegion() throws Exception {
		World w = mock(World.class);
		Region r0 = mock(Region.class);
		Region r1 = mock(Region.class);
		Region r2 = mock(Region.class);
		r0.name = "Foo";
		r1.name = "Foo1";
		r2.name = "Foo2";
		w.regions = Arrays.asList(r1, r0, r2);
		Geography geo = Geography.loadGeography(5, 26);
		geo.holycity = 1;
		when(w.getGeography()).thenReturn(geo);
		for (Plot.PlotType p : new Plot.PlotType[] {
			Plot.PlotType.PRAISE,
			Plot.PlotType.DENOUNCE}) {
			assertEquals("Plot type " + p, r0, p.getTargetRegion("Nation", w).get());
		}
	}

	@Test
	public void plotTypePraiseOnSuccess() {
		World w = mock(World.class);
		Nation n = new Nation();
		n.goodwill = 0;
		when(w.getNation("Nation")).thenReturn(n);
		Plot.PlotType.PRAISE.onSuccess("Nation", w, null);
		assertEquals(20, n.goodwill, EPSILON);
	}

	@Test
	public void plotTypePraiseGetDefender() {
		assertEquals("Nation", Plot.PlotType.DENOUNCE.getDefender("Nation", null).get());
	}

	@Test
	public void plotTypeDenounceOnSuccess() {
		World w = mock(World.class);
		Nation n = new Nation();
		n.goodwill = 100;
		when(w.getNation("Nation")).thenReturn(n);
		Plot.PlotType.DENOUNCE.onSuccess("Nation", w, null);
		assertEquals(80, n.goodwill, EPSILON);
	}

	@Test
	public void plotTypeDenounceGetDefender() {
		assertEquals("Nation", Plot.PlotType.DENOUNCE.getDefender("Nation", null).get());
	}

	@Test
	public void plotGetTargetRegionNation() {
		World w = mock(World.class);
		Character c1 = mock(Character.class);
		when(w.getRuler("Nation")).thenReturn(Optional.of(c1));
		Region r1 = mock(Region.class);
		when(c1.getLocationRegion(w)).thenReturn(r1);
		for (Plot.PlotType p : new Plot.PlotType[] { Plot.PlotType.INTERCEPT_COMMUNICATIONS, Plot.PlotType.SURVEY_NATION }) assertEquals("Plot type: " + p, r1, p.getTargetRegion("Nation", w).get());
	}

	@Test
	public void plotGetDefenderNation() {
		for (Plot.PlotType p : new Plot.PlotType[] { Plot.PlotType.INTERCEPT_COMMUNICATIONS, Plot.PlotType.SURVEY_NATION }) assertEquals("Plot type: " + p, "Nation", p.getDefender("Nation", null).get());
	}

	@Test
	public void plotTypeInterceptCommunicationsOnSuccess() {
		World w = mock(World.class);
		w.date = 2;
		Communication c1 = new Communication();
		Communication c2 = new Communication();
		Communication c3 = new Communication();
		Communication c4 = new Communication();
		c1.from = "Nation";
		c1.to = Arrays.asList("Other Nation");
		c1.postDate = 2;
		c2.from = "Other Nation";
		c2.to = Arrays.asList("Nation");
		c2.postDate = 2;
		c3.from = "Other Nation";
		c3.to = Arrays.asList("Other Nation 2");
		c3.postDate = 2;
		c4.from = "Nation";
		c4.to = Arrays.asList("Other Nation");
		c4.postDate = 1;
		w.communications = Arrays.asList(c1, c2, c3, c4);
		Plot.PlotType.INTERCEPT_COMMUNICATIONS.onSuccess("Nation", w, Arrays.asList("Other Nation 2"));
		assertTrue(c1.intercepted.contains("Other Nation 2"));
		assertTrue(c2.intercepted.contains("Other Nation 2"));
		assertFalse(c3.intercepted.contains("Other Nation 2"));
		assertFalse(c4.intercepted.contains("Other Nation 2"));
	}

	@Test
	public void plotTypeSurveyNationOnSuccess() {
		World w = mock(World.class);
		w.armies = new ArrayList<>();
		w.notifications = new ArrayList<>();
		Nation n = mock(Nation.class);
		when(w.getNation("Nation")).thenReturn(mock(Nation.class));
		Plot.PlotType.SURVEY_NATION.onSuccess("Nation", w, Arrays.asList("Other Nation 2"));
		verify(w).notifyPlayer(eq("Other Nation 2"), eq("Report on Nation"), anyString());
	}
}
