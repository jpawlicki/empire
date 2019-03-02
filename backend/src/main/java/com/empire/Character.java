package com.empire;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		if (xp >= 24) return 5;
		else if (xp >= 15) return 4;
		else if (xp >= 8) return 3;
		else if (xp >= 3) return 2;
		else return 1;
	}

	public double calcPlotPower(World w, boolean boosted, int inspires) {
		double power = 1;
		if (boosted) power += 0.5;
		power += calcLevel("spy") * 0.3;
		if (NationData.getStateReligion(kingdom, w).equals("Northern (Lyskr)")) power += .4;
		if (NationData.getStateReligion(kingdom, w).equals("Company")) power += .2;
		if (NationData.getStateReligion(kingdom, w).startsWith("Iruhan")) power += inspires * .05;
		if (!"".equals(captor)) power -= 0.5;
		return power;
	}

	public void addExperience(String dimension, World w) {
		if ("*".equals(dimension)) {
			for (String d : new String[]{"general", "admiral", "spy", "governor"}) experience.put(d, experience.get(d) + (w.getNation(kingdom).hasTag("Heroic") ? .5 : .25));
		} else {
			experience.put(dimension, experience.get(dimension) + (w.getNation(kingdom).hasTag("Heroic") ? 2 : 1));
		}
	}

	public boolean hasTag(String tag) {
		return tags.contains(tag);
	}

	void addTag(String tag) {
		tags.add(tag);
	}
}

