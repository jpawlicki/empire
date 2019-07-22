package com.empire;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlotTest {
	private static final double DELTA = 1E-5;

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
/*
	@Test
	public void plotType?OnSuccess() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetTargetRegion() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetDefender() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?OnSuccess() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetTargetRegion() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetDefender() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?OnSuccess() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetTargetRegion() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetDefender() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?OnSuccess() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetTargetRegion() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetDefender() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?OnSuccess() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetTargetRegion() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetDefender() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?OnSuccess() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetTargetRegion() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetDefender() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?OnSuccess() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetTargetRegion() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetDefender() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?OnSuccess() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetTargetRegion() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetDefender() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?OnSuccess() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetTargetRegion() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetDefender() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?OnSuccess() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetTargetRegion() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetDefender() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?OnSuccess() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetTargetRegion() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetDefender() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?OnSuccess() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetTargetRegion() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetDefender() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?OnSuccess() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetTargetRegion() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetDefender() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?OnSuccess() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetTargetRegion() {
		PlotType type = PlotType.;
	}

	@Test
	public void plotType?GetDefender() {
		PlotType type = PlotType.;
	}
	*/
}
