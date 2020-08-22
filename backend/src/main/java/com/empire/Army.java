package com.empire;

import com.empire.util.StringUtil;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class Army extends RulesObject {
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
		@SerializedName("Scheming") SCHEMING,
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

	public double calcStrength(World w, Character leader, int inspires) {
		double strength = size * (isArmy() ? getRules().armyBaseStrength : getRules().navyBaseStrength);

		double mods = 1.0;
		Region r = w.regions.get(location);

		if (hasTag(Tag.STEEL)) mods += getRules().steelMod;
		if (hasTag(Tag.SEAFARING) && r.isSea()) mods += getRules().seafaringMod;
		if (isArmy() && !getRules().pirateKingdom.equals(kingdom) && w.getNation(kingdom).hasTag(Nation.Tag.DISCIPLINED)) mods += getRules().disciplinedArmyStrengthMod;
		if (isArmy() && r.isLand() && Nation.isFriendly(r.getKingdom(), kingdom, w)) mods += r.calcFortificationMod();
		if (Ideology.SWORD_OF_TRUTH == Nation.getStateReligion(kingdom, w)) mods += getRules().swordOfTruthMod;
		if (Ideology.SWORD_OF_TRUTH == w.getDominantIruhanIdeology() && Nation.getStateReligion(kingdom, w).religion == Religion.IRUHAN) mods += getRules().iruhanMod;
		if (Ideology.TAPESTRY_OF_PEOPLE == Nation.getStateReligion(kingdom, w)) mods += getRules().perIdeologyTapestryArmyMod * Region.numUniqueIdeologies(kingdom, w);
		if (Nation.getStateReligion(kingdom, w).religion == Religion.IRUHAN) mods += inspires * getRules().perInspireMod;
		if (leader != getRules().noLeader) {
			mods += leader.calcLeadMod(type);
		}

		return strength * mods;
	}

	public double getEffectiveSize() {
		return size * (isNavy() ? 30 : 1);
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

	double getCost(World w, String order) {
		if (hasTag(Army.Tag.HIGHER_POWER)) return 0;
		double cost = size;
		double mods = 1;
		if (isArmy()) cost *= 1.0 / 100;
		else cost *= 1 / 3.0;
		if (hasTag(Army.Tag.CRAFTS_SOLDIERS) && !order.startsWith("Travel ")) {
			mods -= 0.5;
		}
		if (Ideology.COMPANY == Nation.getStateReligion(kingdom, w)) mods -= 0.5;
		if (w.getNation(kingdom).hasTag(Nation.Tag.REBELLIOUS) && w.getNation(kingdom).coreRegions.contains(location)) {
			mods -= 0.5;
		}
		return Math.max(0, cost * mods);
	}

	void addTag(Army.Tag tag) {
		tags.add(tag);
	}

	/**
	 * Orders the army to attempt to raze something.
	 */
	public void raze(World w, String order, Character leader, int inspires) {
		if (!isArmy()) return;
		Region region = w.regions.get(location);
		int razes = (int) (calcStrength(w, leader, inspires) * getRules().razesPerNormalizedStrength / region.calcMinConquestStrength(w));
		if (razes == 0) {
			w.notifyPlayer(kingdom, "Razing Failed", "Army " + id + " is not powerful enough to raze constructions in " + region.name + ".");
			return;
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
				if (targets == 0) w.notifyPlayer(kingdom, "Razing Failed", "By the time army " + id + " was ready, there was no " + target + " left to raze in " + region.name + ".");
				break;
			}
			targets++;
			region.constructions.remove(bestRaze);
			if (Construction.Type.TEMPLE == bestRaze.type) {
				region.setReligion(null, w);
			}
			gold += bestRaze.originalCost * getRules().razeRefundFactor;
		}
		this.gold += gold;
		w.notifyPlayer(kingdom, "Razing", "Our army in " + region.name + " looted and razed " + StringUtil.quantify(targets, target) + ", carrying off assets worth " + Math.round(gold) + " gold.");
		if (!region.getKingdom().equals(kingdom)) w.notifyPlayer(region.getKingdom(), "Razing", "An army of " + kingdom + " looted then razed " + StringUtil.quantify(targets, target) + " in " + region.name + ".");
	}

	/**
	 * Orders the army to conquer the region they inhabit.
	 * Modifies conqueredRegions.
	 */
	public void conquer(World w, String order, Set<Region> conqueredRegions, Map<String, List<String>> tributes, Map<Army, Character> leaders, int inspires, Set<String> excommunicatedNations) {
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
		double strength = calcStrength(w, leaders.get(this), inspires);
		for (Army a : w.armies) {
			if (a.isArmy() && a.location == location && !a.kingdom.equals(kingdom) && a.calcStrength(w, leaders.get(a), inspires) > strength) {
				stopped = true;
				w.notifyPlayer(kingdom, "Conquest Failed", "Army " + id + " is not the strongest army in " + region.name + " and cannot conquer it.");
				break;
			}
		}
		if (stopped) return;
		// Must be strong enough.
		if (strength < region.calcMinConquestStrength(w)) {
			w.notifyPlayer(kingdom, "Conquest Failed", "Army " + id + " is not strong enough to conquer " + region.name + ".");
			return;
		}
		// Must attack.
		if (w.getNation(kingdom).getRelationship(region.getKingdom()).battle != Relationship.War.ATTACK) {
			w.notifyPlayer(kingdom, "Conquest Failed", "Army " + id + " is not able to conquer " + region.name + " without attacking " + region.getKingdom() + ".");
			return;
		}
		if (target.equals(region.getKingdom())) return;
		String nobleFate = "";
		if (region.noble != null && (region.noble.unrest.get() < .5 || w.getNation(target).hasTag(Nation.Tag.REPUBLICAN))) {
			nobleFate = " " + region.noble.name + " and their family fought courageously in defense of the region but were slain.";
			if (!w.spyRings.stream().anyMatch(r -> r.getLocation() == location && r.getNation() == region.getKingdom())) w.spyRings.add(SpyRing.newSpyRing(getRules(), region.getKingdom(), region.noble.calcPosthumousSpyRingStrength(), location));
			region.noble = null;
		}
		if (region.noble != null) {
			nobleFate = " " + region.noble.name + " swore fealty to their new rulers.";
			region.noble.unrest.set(.15);
		}
		if (w.church.hasDoctrine(Church.Doctrine.DEFENDERS_OF_FAITH) && excommunicatedNations.contains(region.getKingdom())) w.getNation(kingdom).goodwill += getRules().defendersOfFaithConquestOpinion;
		region.constructions.removeIf(c -> c.type == Construction.Type.FORTIFICATIONS);
		w.notifyAllPlayers(region.name + " Conquered", "An army of " + kingdom + " has conquered " + region.name + " (a region of " + region.getKingdom() + ") and installed a government loyal to " + target + "." + nobleFate);
		for (Region r : w.regions) if (r.noble != null && r.getKingdom().equals(kingdom)) if (tributes.getOrDefault(region.getKingdom(), new ArrayList<>()).contains(kingdom) && w.getNation(region.getKingdom()).previousTributes.contains(r.getKingdom())) r.noble.unrest.add(.20);
		region.setKingdom(w, target);
		conqueredRegions.add(region);
		orderhint = "";
	}

	private Army(Rules rules) {
		super(rules);
	}

	static Army newArmy(Rules rules) {
		return new Army(rules);
	}
}

