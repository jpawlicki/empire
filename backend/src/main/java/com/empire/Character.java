package com.empire;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class Character {
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

	// TODO: This function should technically be considered part of the game rules config IMHO
	public int calcLevel(String dimension) {
		return (int) Math.sqrt(experience.getOrDefault(dimension, 0.0) + 1);
	}

	public double calcPlotPower(World w, boolean boosted, int inspires) {
		double power = Constants.basePlotStrength;

		power += calcLevel(Constants.charDimSpy) * Constants.perSpyLevelPlotMod;

		if (boosted) power += Constants.guardAgainstPlotMod;
		if (Ideology.LYSKR == NationData.getStateReligion(kingdom, w)) power += Constants.lyskrPlotMod;
		if (Ideology.COMPANY == NationData.getStateReligion(kingdom, w)) power += Constants.companyPlotMod;
		if (NationData.getStateReligion(kingdom, w).religion == Religion.IRUHAN) power += inspires * Constants.perInspirePlotMod;
		if (!Constants.noCaptor.equals(captor)) power += Constants.capturedPlotMod;

		return power;
	}

	public void addExperience(String dimension, World w) {
		List<String> dims = Constants.charDimAll.equals(dimension) ? Constants.charDims : Collections.singletonList(dimension);
		double expBase = Constants.charDimAll.equals(dimension) ? Constants.allDimExpAdd : Constants.oneDimExpAdd;
		double expMult = w.getNation(kingdom).hasTag(Constants.nationHeroicTag) ? Constants.heroicExpMultiplier : 1.0;

		dims.forEach(d -> experience.put(d, experience.get(d) + expBase * expMult));
	}

	public double getExperience(String dimension){
		return experience.get(dimension);
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

	public String getName() {
		return name;
	}

	public int getLocation() {
		return location;
	}

	public void setLocation(int location) {
		this.location = location;
	}
}

