package com.empire;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class Army {
	enum Type {
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
		double strength = size * (isArmy() ? Constants.armyBaseStrength : Constants.navyBaseStrength);

		double mods = 1.0;
		Region r = w.regions.get(location);

		if (hasTag(Constants.armySteelTag)) mods += Constants.steelMod;
		if (hasTag(Constants.armySeafaringTag) && r.isSea()) mods += Constants.seafaringMod;
		if (isArmy() && !Constants.pirateKingdom.equals(kingdom) && w.getNation(kingdom).hasTag(Constants.nationDisciplinedTag)) mods += Constants.disciplinedMod;
		if (isArmy() && r.isLand() && NationData.isFriendly(r.getKingdom(), kingdom, w)) mods += r.calcFortification() - 1;
		if (isArmy() && r.noble != Constants.noNoble && r.noble.hasTag(Constants.nobleLoyalTag) && r.getKingdom().equals(kingdom)) mods += Constants.loyalMod;
		if (Ideology.SWORD_OF_TRUTH == w.getDominantIruhanIdeology()) {
			Ideology sr = NationData.getStateReligion(kingdom, w);
			if (Ideology.SWORD_OF_TRUTH == sr) mods += Constants.swordOfTruthMod;
			else if (sr.religion == Religion.IRUHAN) mods += Constants.iruhanMod;
		}
		if (lastStand) mods += Constants.lastStandMod;
		if (isArmy() && NationData.getStateReligion(kingdom, w).religion == Religion.IRUHAN) mods += inspires * Constants.perInspireMod;
		if (leader != Constants.noLeader && Constants.noCaptor.equals(leader.captor)) {
			mods += leader.calcLevel(isArmy() ? Constants.charDimGeneral : Constants.charDimAdmiral) * Constants.perLevelLeaderMod;
		}

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

