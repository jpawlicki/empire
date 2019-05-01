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

	public static final double armyBaseStrength = 1E-2;
	public static final double navyBaseStrength = 1.0;

	public static final String armySteelTag = "Steel";
	public static final double steelMod = 0.15;

	public static final String armySeafaringTag = "Seafaring";
	public static final double seafaringMod = 1.5;

	public static final String armyPirateTag = "Pirate";

	public static final String nationDisciplinedTag = "Disciplined";
	public static final double disciplinedMod = 0.1;

	public static final String nobleLoyalTag = "Loyal";
	public static final double loyalMod = 0.25;

	public static final double swordOfTruthMod = 0.25;
	public static final double iruhanMod = 0.15;
	public static final double lastStandMod = 4.0;
	public static final double perInspireMod = 0.05;

	public static final String noCaptor = "";

	public static final String charDimGeneral = "general";
	public static final String charDimAdmiral = "admiral";
	public static final double perLevelLeaderMod = 0.2;

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

		double mods = 1.0;
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
		if (leader != null && noCaptor.equals(leader.captor)) mods += leader.calcLevel(isArmy() ? charDimGeneral : charDimAdmiral) * perLevelLeaderMod;

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

