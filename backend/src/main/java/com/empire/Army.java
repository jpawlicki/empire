package com.empire;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class Army {
	enum Type {
		@SerializedName("army") ARMY,
		@SerializedName("navy") NAVY
	}

	enum Tag {
		@SerializedName("Steel") STEEL,
		@SerializedName("Formations") FORMATIONS,
		@SerializedName("Pillagers") PILLAGERS,
		@SerializedName("Raiders") RAIDERS,
		@SerializedName("Seafaring") SEAFARING,
		@SerializedName("Impressment") IMPRESSMENT,
		@SerializedName("Riders") RIDERS,
		@SerializedName("Crafts-soldiers") CRAFTS_SOLDIERS,
		@SerializedName("Weathered") WEATHERED,
		@SerializedName("Pathfinders") PATHFINDERS,
		@SerializedName("Unpredictable") UNPREDICTABLE,
		@SerializedName("Higher Power") HIGHER_POWER,
		@SerializedName("Undead") UNDEAD
	}

	int id = -1;
	Type type;
	double size;
	double gold;
	String kingdom = "";
	int location = -1;
	List<Preparation> preparation = new ArrayList<>();
	List<Tag> tags = new ArrayList<>();
	Map<String, Double> composition = new HashMap<>();
	String orderhint = "";

	public double calcStrength(World w, Character leader, int inspires, boolean lastStand) {
		double strength = size * (isArmy() ? Constants.armyBaseStrength : Constants.navyBaseStrength);

		double mods = 1.0;
		Region r = w.regions.get(location);

		if (hasTag(Tag.STEEL)) mods += Constants.steelMod;
		if (hasTag(Tag.SEAFARING) && r.isSea()) mods += Constants.seafaringMod;
		if (isArmy() && !Constants.pirateKingdom.equals(kingdom) && w.getNation(kingdom).hasTag(NationData.Tag.DISCIPLINED)) mods += Constants.disciplinedArmyStrengthMod;
		if (isArmy() && r.isLand() && NationData.isFriendly(r.getKingdom(), kingdom, w)) mods += r.calcFortificationMod();
		if (Ideology.SWORD_OF_TRUTH == w.getDominantIruhanIdeology()) {
			Ideology sr = NationData.getStateReligion(kingdom, w);
			if (Ideology.SWORD_OF_TRUTH == sr) mods += Constants.swordOfTruthMod;
			else if (sr.religion == Religion.IRUHAN) mods += Constants.iruhanMod;
		}
		if (lastStand) mods += Constants.lastStandMod;
		if (isArmy() && NationData.getStateReligion(kingdom, w).religion == Religion.IRUHAN) mods += inspires * Constants.perInspireMod;
		if (leader != Constants.noLeader && !leader.isCaptive()) {
			mods += leader.calcLeadMod(type);
		}

		return strength * mods;
	}

	public boolean hasTag(Tag tag) {
		return tags.contains(tag);
	}

	public boolean isArmy() {
		return type == Type.ARMY;
	}

	public boolean isNavy() {
		return type == Type.NAVY;
	}

	void addTag(Army.Tag tag) {
		tags.add(tag);
	}

	/**
	 * Orders the army to attempt to raze something.
	 * @return the gold gained from razing.
	 */
	public double raze(World w, String order, Character leader, int inspires, boolean lastStanding) {
		if (!isArmy()) return 0;
		Region region = w.regions.get(location);
		int razes = (int) (calcStrength(w, leader, inspires, lastStanding) * 2 / region.calcMinConquestStrength(w));
		if (razes == 0) {
			w.notifications.add(new Notification(kingdom, "Razing Failed", "Army " + id + " is not powerful enough to raze constructions in " + region.name + "."));
			return 0;
		}
		String target = order.replace("Raze ", "");
		int targets = 0;
		double gold = 0;
		for (int i = 0; i < razes; i++) {
			Construction bestRaze = null;
			for (Construction c : region.constructions) {
				if (target.contains(c.type.toString().toLowerCase()) && (c.type != Construction.Type.TEMPLE || target.contains(c.religion.toString()))) {
					if (bestRaze == null || bestRaze.originalCost < c.originalCost) bestRaze = c;
				}
			}
			if (bestRaze == null) {
				if (targets == 0) w.notifications.add(new Notification(kingdom, "Razing Failed", "By the time army " + id + " was ready, there was no " + target + " left to raze in " + region.name + "."));
				break;
			}
			targets++;
			region.constructions.remove(bestRaze);
			if (Construction.Type.TEMPLE == bestRaze.type) {
				region.setReligion(null, w);
			}
			gold += bestRaze.originalCost * Constants.razeRefundFactor;
		}
		w.getNation(kingdom).gold += gold;
		w.notifications.add(new Notification(kingdom, "Razing in " + region.name, "Our army looted and razed " + StringUtil.quantify(targets, target) + ", carrying off assets worth " + Math.round(gold) + " gold."));
		if (!region.getKingdom().equals(kingdom)) w.notifications.add(new Notification(region.getKingdom(), "Razing in " + region.name, "An army of " + kingdom + " looted then razed " + StringUtil.quantify(targets, target) + "."));
		return gold;
	}

	/**
	 * Orders the army to conquer the region they inhabit.
	 * Modifies conqueredRegions.
	 */
	public void conquer(World w, String order, Set<Region> conqueredRegions, Map<String, List<String>> tributes, Map<Army, Character> leaders, int inspires, Set<String> lastStands) {
		if (!isArmy()) return;
		String target = order.replace("Conquer for ", "");
		if (target.equals("Conquer")) target = kingdom;
		if (w.getNation(target) == null) return;
		Region region = w.regions.get(location);
		if (!region.isLand()) return;
		if (kingdom.equals(region.getKingdom())) return;
		if (conqueredRegions.contains(region)) return;
		// Must be strongest in region (not counting other armies of the same ruler).
		boolean stopped = false;
		double strength = calcStrength(w, leaders.get(this), inspires, lastStands.contains(kingdom));
		for (Army a : w.armies) {
			if (a.isArmy() && a.location == location && !a.kingdom.equals(kingdom) && a.calcStrength(w, leaders.get(a), inspires, lastStands.contains(a.kingdom)) > strength) {
				stopped = true;
				w.notifications.add(new Notification(kingdom, "Conquest Failed", "Army " + id + " is not the strongest army in " + region.name + " and cannot conquer it."));
				break;
			}
		}
		if (stopped) return;
		// Must be strong enough.
		if (strength < region.calcMinConquestStrength(w)) {
			w.notifications.add(new Notification(kingdom, "Conquest Failed", "Army " + id + " is not strong enough to conquer " + region.name + "."));
			return;
		}
		// Must attack.
		if (w.getNation(kingdom).getRelationship(region.getKingdom()).battle != Relationship.War.ATTACK) {
			w.notifications.add(new Notification(kingdom, "Conquest Failed", "Army " + id + " is not able to conquer " + region.name + " without attacking " + region.getKingdom() + "."));
			return;
		}
		if (target.equals(region.getKingdom())) return;
		String nobleFate = "";
		if (region.noble != null && (region.noble.unrest < .5 || w.getNation(target).hasTag(NationData.Tag.REPUBLICAN))) {
			nobleFate = " " + region.noble.name + " and their family fought courageously in defense of the region but were slain.";
			region.noble = null;
		}
		if (region.noble != null) {
			nobleFate = " " + region.noble.name + " swore fealty to their new rulers.";
			region.noble.unrest = .15;
		}
		if (w.church.hasDoctrine(Church.Doctrine.DEFENDERS_OF_FAITH) && w.getNation(region.getKingdom()).goodwill <= 0) w.getNation(kingdom).goodwill += Constants.defendersOfFaithConquestOpinion;
		region.constructions.removeIf(c -> c.type == Construction.Type.FORTIFICATIONS);
		w.notifyAllPlayers(region.name + " Conquered", "An army of " + kingdom + " has conquered " + region.name + " (a region of " + region.getKingdom() + ") and installed a government loyal to " + target + "." + nobleFate);
		for (Region r : w.regions) if (r.noble != null && r.getKingdom().equals(kingdom)) if (tributes.getOrDefault(region.getKingdom(), new ArrayList<>()).contains(kingdom) && w.getNation(region.getKingdom()).previousTributes.contains(r.getKingdom())) r.noble.unrest = Math.min(1, r.noble.unrest + .06);
		region.setKingdom(w, target);
		conqueredRegions.add(region);
		orderhint = "";
	}
}

