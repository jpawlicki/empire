package com.empire;

import java.util.*;

final class Character {
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

		return Constants.expLevels.entrySet().stream().
				filter(e -> xp >= e.getKey()).
				map(Map.Entry::getValue).
				findFirst().
				orElse(Constants.minDimLevel);
	}

	public double calcPlotPower(World w, boolean boosted, int inspires) {
		double power = Constants.basePlotStrength;

		power += calcLevel(Constants.charDimSpy) * Constants.perSpyLevelPlotMod;

		if (boosted) power += Constants.guardAgainstPlotMod;
		if (Ideology.LYSKR == NationData.getStateReligion(kingdom, w)) power += Constants.lyskrPlotMod;
		if (Ideology.COMPANY == NationData.getStateReligion(kingdom, w)) power += Constants.companyPlotMod;
		if (NationData.getStateReligion(kingdom, w).religion == Religion.IRUHAN) power += inspires * Constants.perInspirePlotMod;
		if (!Constants.noCaptor.equals(captor)) power -= Constants.capturedPlotMod;

		return power;
	}

	public void addExperience(String dimension, World w) {
		if (Constants.charDimAll.equals(dimension)) {
			for (String d : Constants.charDims) {
				double expAdd = w.getNation(kingdom).hasTag(Constants.nationHeroicTag) ? Constants.heroicExpMultiplier * Constants.allDimExpAdd : Constants.allDimExpAdd;
				experience.put(d, experience.get(d) + expAdd);
			}
		} else {
			double expAdd = w.getNation(kingdom).hasTag(Constants.nationHeroicTag) ? Constants.heroicExpMultiplier * Constants.oneDimExpAdd : Constants.oneDimExpAdd;
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

