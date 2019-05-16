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

	/**
	 * Orders the army to attempt to raze something.
	 * @return the gold gained from razing.
	 */
	public double raze(World w, String order, Character leader, int inspires, boolean lastStanding) {
		if (!isArmy()) return 0;
		Region region = w.regions.get(location);
		if (calcStrength(w, leader, inspires, lastStanding) < region.calcMinConquestStrength(w) / 2) {
			w.notifications.add(new Notification(kingdom, "Razing Failed", "Army " + id + " is not powerful enough to raze constructions in " + region.name + "."));
			return 0;
		}
		String target = order.replace("Raze ", "");
		Construction bestRaze = null;
		for (Construction c : region.constructions) {
			if (target.contains(c.type) && (!"temple".equals(c.type) || target.contains(c.religion.toString()))) {
				if (bestRaze == null || bestRaze.originalCost < c.originalCost) bestRaze = c;
			}
		}
		if (bestRaze == null) {
			w.notifications.add(new Notification(kingdom, "Razing Failed", "By the time army " + id + " was ready, there was no " + target + " left to raze in " + region.name + "."));
			return 0;
		}
		region.constructions.remove(bestRaze);
		if ("temple".equals(bestRaze.type)) {
			region.setReligion(null, w);
		}
		double gold = bestRaze.originalCost * Constants.razeRefundFactor;
		w.getNation(kingdom).gold += gold;
		w.notifications.add(new Notification(kingdom, "Razing in " + region.name, "Our army looted and razed a " + target + ", carrying off assets worth " + Math.round(gold) + " gold."));
		if (!region.getKingdom().equals(kingdom)) w.notifications.add(new Notification(region.getKingdom(), "Razing in " + region.name, "An army of " + kingdom + " looted a " + target + ", then burned it to the ground."));
		return gold;
	}
}

