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

	private static final double armyBaseStrength = 1E-2;
	private static final double navyBaseStrength = 1.0;

	private static final String armySteelTag = "Steel";
	private static final double steelMod = 0.15;

	private static final String armySeafaringTag = "Seafaring";
	private static final double seafaringMod = 1.5;

	private static final String armyPirateTag = "Pirate";

	private static final String nationDisciplinedTag = "Disciplined";
	private static final double disciplinedMod = 0.1;

	private static final String nobleLoyalTag = "Loyal";
	private static final double loyalMod = 0.25;

	private static final double swordOfTruthMod = 0.25;
	private static final double iruhanMod = 0.15;
	private static final double lastStandMod = 4.0;
	private static final double perInspireMod = 0.05;

	private static final String leaderGeneral = "general";
	private static final String leaderAdmiral = "admiral";
	private static final double perLevelLeaderMod = 0.2;

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
		double strength = size * (isArmy() ? armyBaseStrength : navyBaseStrength);

		double mods = 1;
		Region r = w.regions.get(location);

		if (hasTag(armySteelTag)) mods += steelMod;
		if (hasTag(armySeafaringTag) && r.isSea()) mods += seafaringMod;
		if (isArmy() && !armyPirateTag.equals(kingdom) && w.getNation(kingdom).hasTag(nationDisciplinedTag)) mods += disciplinedMod;
		if (isArmy() && r.isLand() && NationData.isFriendly(r.kingdom, kingdom, w)) mods += r.calcFortification() - 1;
		if (isArmy() && r.noble != null && r.noble.hasTag(nobleLoyalTag) && r.kingdom.equals(kingdom)) mods += loyalMod;
		if (Ideology.SWORD_OF_TRUTH == w.getDominantIruhanIdeology()) {
			Ideology sr = NationData.getStateReligion(kingdom, w);
			if (Ideology.SWORD_OF_TRUTH == sr) mods += swordOfTruthMod;
			else if (sr.religion == Religion.IRUHAN) mods += iruhanMod;
		}
		if (lastStand) mods += lastStandMod;
		if (isArmy() && NationData.getStateReligion(kingdom, w).religion == Religion.IRUHAN) mods += inspires * perInspireMod;
		if (leader != null && "".equals(leader.captor)) mods += leader.calcLevel(isArmy() ? leaderGeneral : leaderAdmiral) * perLevelLeaderMod;

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

