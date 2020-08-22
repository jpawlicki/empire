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
	public void addContributionTo_SupportFriendly() {
		SpyRing r = SpyRing.newSpyRing(Utils.rules, "nation", 50, 0);
		World w = mock(World.class);
		Nation n = mock(Nation.class);
		Region r0 = mock(Region.class);
		when(r0.getKingdom()).thenReturn("nation");
		w.regions = Arrays.asList(r0);
		r0.constructions = Arrays.asList();
		w.tivar = new Tivar();
		w.characters = Arrays.asList();
		when(w.getNation("nation")).thenReturn(n);
		r.involve(0, SpyRing.InvolvementDisposition.SUPPORTING);
		Plot.OutcomeWeights outcome = new Plot.OutcomeWeights(Utils.rules);
		r.addContributionTo(0, r0, "defender", w, outcome);
		assertEquals(50 * .9, outcome.getSuccess(), EPSILON);
		assertEquals(0, outcome.getFailure(), EPSILON);
		assertEquals(0, outcome.getCriticalFailure(), EPSILON);
	}

	@Test
	public void addContributionTo_SabotageFriendly() {
		SpyRing r = SpyRing.newSpyRing(Utils.rules, "nation", 50, 0);
		World w = mock(World.class);
		Nation n = mock(Nation.class);
		Region r0 = mock(Region.class);
		when(r0.getKingdom()).thenReturn("nation");
		r0.constructions = Arrays.asList();
		w.tivar = new Tivar();
		w.characters = Arrays.asList();
		w.regions = Arrays.asList(r0);
		when(w.getNation("nation")).thenReturn(n);
		r.involve(0, SpyRing.InvolvementDisposition.SABOTAGING);
		Plot.OutcomeWeights outcome = new Plot.OutcomeWeights(Utils.rules);
		r.addContributionTo(0, r0, "defender", w, outcome);
		assertEquals(0, outcome.getSuccess(), EPSILON);
		assertEquals(0, outcome.getFailure(), EPSILON);
		assertEquals(100 * .9, outcome.getCriticalFailure(), EPSILON);
	}

	@Test
	public void addContributionTo_SupportUnfriendly() {
		SpyRing r = SpyRing.newSpyRing(Utils.rules, "nation", 50, 0);
		World w = mock(World.class);
		Nation nation = mock(Nation.class);
		Nation defender = mock(Nation.class);
		Region r0 = mock(Region.class);
		when(r0.getKingdom()).thenReturn("defender");
		r0.constructions = Arrays.asList();
		w.tivar = new Tivar();
		w.characters = Arrays.asList();
		w.regions = Arrays.asList(r0);
		Relationship enmity = new Relationship();
		enmity.battle = Relationship.War.ATTACK;
		when(w.getNation("nation")).thenReturn(nation);
		when(w.getNation("defender")).thenReturn(defender);
		when(defender.getRelationship("nation")).thenReturn(enmity);
		when(nation.getRelationship("defender")).thenReturn(enmity);
		r.involve(0, SpyRing.InvolvementDisposition.SUPPORTING);
		Plot.OutcomeWeights outcome = new Plot.OutcomeWeights(Utils.rules);
		r.addContributionTo(0, r0, "defender", w, outcome);
		assertEquals(50 * .7, outcome.getSuccess(), EPSILON);
		assertEquals(0, outcome.getFailure(), EPSILON);
		assertEquals(0, outcome.getCriticalFailure(), EPSILON);
	}

	@Test
	public void damage() {
		SpyRing r = SpyRing.newSpyRing(Utils.rules, "nation", 50, 0);
		World w = mock(World.class);
		Nation n = mock(Nation.class);
		Region r0 = mock(Region.class);
		when(r0.getKingdom()).thenReturn("nation");
		w.regions = Arrays.asList(r0);
		r0.constructions = Arrays.asList();
		w.tivar = new Tivar();
		w.characters = Arrays.asList();
		when(w.getNation("nation")).thenReturn(n);
		r.involve(0, SpyRing.InvolvementDisposition.SUPPORTING);
		Plot.OutcomeWeights outcome = new Plot.OutcomeWeights(Utils.rules);
		r.addContributionTo(0, r0, "defender", w, outcome);
		assertEquals(50 * .9, outcome.getSuccess(), EPSILON);
		assertEquals(0, outcome.getFailure(), EPSILON);
		assertEquals(0, outcome.getCriticalFailure(), EPSILON);

		r.damage();
		outcome = new Plot.OutcomeWeights(Utils.rules);
		r.addContributionTo(0, r0, "defender", w, outcome);
		assertEquals(50 * .9 * .5, outcome.getSuccess(), EPSILON);
		assertEquals(0, outcome.getFailure(), EPSILON);
		assertEquals(0, outcome.getCriticalFailure(), EPSILON);
	}

	@Test
	public void grow() {
		SpyRing r = SpyRing.newSpyRing(Utils.rules, "nation", 50, 0);
		World w = mock(World.class);
		Nation n = mock(Nation.class);
		Region r0 = mock(Region.class);
		when(r0.getKingdom()).thenReturn("nation");
		w.regions = Arrays.asList(r0);
		r0.constructions = Arrays.asList();
		w.tivar = new Tivar();
		w.characters = Arrays.asList();
		when(w.getNation("nation")).thenReturn(n);
		r.involve(0, SpyRing.InvolvementDisposition.SUPPORTING);
		Plot.OutcomeWeights outcome = new Plot.OutcomeWeights(Utils.rules);
		r.addContributionTo(0, r0, "defender", w, outcome);
		assertEquals(50 * .9, outcome.getSuccess(), EPSILON);
		assertEquals(0, outcome.getFailure(), EPSILON);
		assertEquals(0, outcome.getCriticalFailure(), EPSILON);

		r.grow();
		outcome = new Plot.OutcomeWeights(Utils.rules);
		r.addContributionTo(0, r0, "defender", w, outcome);
		assertEquals(57.5 * .9, outcome.getSuccess(), EPSILON);
		assertEquals(0, outcome.getFailure(), EPSILON);
		assertEquals(0, outcome.getCriticalFailure(), EPSILON);
	}

	@Test
	public void expose() {
		SpyRing r = SpyRing.newSpyRing(Utils.rules, "nation", 50, 0);
		assertFalse(r.isExposed());
		r.expose();
		assertTrue(r.isExposed());
	}

	@Test
	public void belongsTo() {
		SpyRing r = SpyRing.newSpyRing(Utils.rules, "nation", 50, 0);
		assertFalse(r.belongsTo("defender"));
		assertTrue(r.belongsTo("nation"));
	}

	@Test
	public void involvement() {
		SpyRing r = SpyRing.newSpyRing(Utils.rules, "nation", 50, 0);
		assertFalse(r.getInvolvementIn(1).isPresent());
		assertFalse(r.getInvolvementIn(2).isPresent());

		r.involve(1, SpyRing.InvolvementDisposition.SUPPORTING);
		assertTrue(r.getInvolvementIn(1).isPresent());
		assertEquals(SpyRing.InvolvementDisposition.SUPPORTING, r.getInvolvementIn(1).get());
		assertFalse(r.getInvolvementIn(2).isPresent());

		r.involve(2, SpyRing.InvolvementDisposition.SABOTAGING);
		assertFalse(r.getInvolvementIn(1).isPresent());
		assertTrue(r.getInvolvementIn(2).isPresent());
		assertEquals(SpyRing.InvolvementDisposition.SABOTAGING, r.getInvolvementIn(2).get());

		r.involve(-1, null);
		assertFalse(r.getInvolvementIn(1).isPresent());
		assertFalse(r.getInvolvementIn(2).isPresent());
	}
}
