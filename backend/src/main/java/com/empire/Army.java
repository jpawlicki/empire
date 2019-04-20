package com.empire;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class Army {
	static enum Type {
		@SerializedName("army")
		ARMY,
		@SerializedName("navy")
		NAVY
	}

	int id = -1;
	Type type;
	double size;
	double gold;
	String kingdom = "";
	int location = -1;
	List<Preparation> preparation = new ArrayList<>();
	List<String> tags = new ArrayList<>();
	Map<String, Double> composition = new HashMap<>();
	String orderhint = "";

	public double calcStrength(World w, Character leader, int inspires, boolean lastStand) {
		double strength = size * (isArmy() ? 1 / 100.0 : 1);
		double mods = 1;
		Region r = w.regions.get(location);
		if (hasTag("Steel")) mods += .15;
		if (hasTag("Seafaring") && r.type.equals("water")) mods += 1.5;
		if (isArmy() && !"Pirate".equals(kingdom) && w.getNation(kingdom).hasTag("Disciplined")) mods += .1;
		if (isArmy() && r.type.equals("land") && NationData.isFriendly(r.kingdom, kingdom, w)) mods += r.calcFortification() - 1;
		if (isArmy() && r.noble != null && r.noble.hasTag("Loyal") && r.kingdom.equals(kingdom)) mods += .25;
		if (Ideology.SWORD_OF_TRUTH == w.getDominantIruhanIdeology()) {
			Ideology sr = NationData.getStateReligion(kingdom, w);
			if (Ideology.SWORD_OF_TRUTH == sr) mods += .25;
			else if (sr.religion == Religion.IRUHAN) mods += .15;
		}
		if (lastStand) mods += 4;
		if (isArmy() && NationData.getStateReligion(kingdom, w).religion == Religion.IRUHAN) mods += inspires * .05;
		if (leader != null && "".equals(leader.captor)) mods += leader.calcLevel(isArmy() ? "general" : "admiral") * .2;
		return strength * mods;
	}

	public boolean hasTag(String tag) {
		return tags.contains(tag);
	}

	public boolean isArmy() {
		return type == Type.ARMY;
	}

	public boolean isNavy() {
		return type == Type.NAVY;
	}

	void addTag(String tag) {
		tags.add(tag);
	}
}

