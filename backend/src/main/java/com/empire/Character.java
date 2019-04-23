package com.empire;

import java.util.*;

final class Character {
	private static final double basePlotStrength = 1.0;
	private static final double guardAgainstPlotMod = 0.5;
	private static final double perSpyLevelPlotMod = 0.3;
	private static final double lyskrPlotMod = 0.4;
	private static final double companyPlotMod = 0.2;
	private static final double perInspirePlotMod = 0.05;
	private static final double capturedPlotMod = -0.5;

	private static final String charDimAll = "*";
	private static final String charDimGeneral = "general";
	private static final String charDimAdmiral = "admiral";
	private static final String charDimSpy = "spy";
	private static final String charDimGovernor = "governor";
	private static final String[] charDims = new String[]{charDimGeneral, charDimAdmiral, charDimSpy, charDimGovernor};

	private static final int minDimLevel = 1;
	private static final double oneDimExpAdd = 1.0;
	private static final double allDimExpAdd = 0.25;

	private static final String nationHeroicTag = "Heroic";
	private static final double heroicExpMultiplier = 2.0;

	private static final String noCaptor = "";

	// This map is ordered by insertion order, code below depends on the keys being in descending order
	// Perhaps change to a TreeMap which orders keys internally (although in ascending order)
	private static final Map<Double, Integer> expLevels = new LinkedHashMap<>();
	static {
		expLevels.put(24.0, 5);
		expLevels.put(15.0, 4);
		expLevels.put(8.0, 3);
		expLevels.put(3.0, 2);
	}

	String name = "";
	String kingdom = "";
	String captor = "";
	String honorific = "";
	int location = -1;
	boolean hidden = false;
	List<Preparation> preparation = new ArrayList<>();
	private List<String> tags = new ArrayList<>();
	Map<String, Double> experience = new HashMap<>();
	List<String> values = new ArrayList<>();
	int leadingArmy = -1;
	String orderhint = "";

	public int calcLevel(String dimension) {
		double xp = experience.get(dimension);

		return expLevels.entrySet().stream().
				filter(e -> xp >= e.getKey()).
				map(Map.Entry::getValue).
				findFirst().
				orElse(minDimLevel);
	}

	public double calcPlotPower(World w, boolean boosted, int inspires) {
		double power = basePlotStrength;

		power += calcLevel(charDimSpy) * perSpyLevelPlotMod;

		if (boosted) power += guardAgainstPlotMod;
		if (Ideology.LYSKR == NationData.getStateReligion(kingdom, w)) power += lyskrPlotMod;
		if (Ideology.COMPANY == NationData.getStateReligion(kingdom, w)) power += companyPlotMod;
		if (NationData.getStateReligion(kingdom, w).religion == Religion.IRUHAN) power += inspires * perInspirePlotMod;
		if (!noCaptor.equals(captor)) power -= capturedPlotMod;

		return power;
	}

	public void addExperience(String dimension, World w) {
		if (charDimAll.equals(dimension)) {
			for (String d : charDims) {
				double expAdd = w.getNation(kingdom).hasTag(nationHeroicTag) ? heroicExpMultiplier * allDimExpAdd : allDimExpAdd;
				experience.put(d, experience.get(d) + expAdd);
			}
		} else {
			double expAdd = w.getNation(kingdom).hasTag(nationHeroicTag) ? heroicExpMultiplier * oneDimExpAdd : oneDimExpAdd;
			experience.put(dimension, experience.get(dimension) + expAdd);
		}
	}

	public boolean hasTag(String tag) {
		return tags.contains(tag);
	}

	void addTag(String tag) {
		tags.add(tag);
	}

	void removeTag(String tag) {
		tags.remove(tag);
	}
}

