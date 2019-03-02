package com.empire;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class Army {
	int id = -1;
	String type = "";
	double size;
	double gold;
	String kingdom = "";
	int location = -1;
	List<Preparation> preparation = new ArrayList<>();
	List<String> tags = new ArrayList<>();
	Map<String, Double> composition = new HashMap<>();
	String orderhint = "";

	public double calcStrength(World w, Character leader, int inspires, boolean lastStand) {
		double strength = size * (type.equals("army") ? 1 / 100.0 : 1);
		double mods = 1;
		Region r = w.regions.get(location);
		if (tags.contains("Steel")) mods += .15;
		if (tags.contains("Seafaring") && r.type.equals("water")) mods += 1.5;
		if (type.equals("army") && !"Pirate".equals(kingdom) && w.kingdoms.get(kingdom).tags.contains("Disciplined")) mods += .1;
		if (type.equals("army") && r.type.equals("land") && NationData.isFriendly(r.kingdom, kingdom, w)) mods += r.calcFortification() - 1;
		if (type.equals("army") && r.noble != null && r.noble.tags.contains("Loyal") && r.kingdom.equals(kingdom)) mods += .25;
		if (type.equals("army") && "Iruhan (Sword of Truth)".equals(w.getDominantIruhanIdeology())) {
			String sr = NationData.getStateReligion(kingdom, w);
			if ("Iruhan (Sword of Truth)".equals(sr)) mods += .25;
			else mods += .15;
		}
		if (lastStand) mods += 4;
		if (type.equals("army") && NationData.getStateReligion(kingdom, w).startsWith("Iruhan")) mods += inspires * .05;
		if (leader != null && "".equals(leader.captor)) mods += leader.calcLevel(type.equals("army") ? "general" : "admiral") * .2;
		return strength * mods;
	}
}

