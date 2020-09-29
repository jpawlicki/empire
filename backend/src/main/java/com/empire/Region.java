package com.empire;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

class Region extends RulesObject {
	enum Type {
		@SerializedName("land") LAND,
		@SerializedName("water") WATER
	}

	int id;
	String name;
	Type type;
	Culture culture;
	double population;
	Ideology religion;
	Percentage unrestPopular;
	Noble noble;
	List<Construction> constructions = new ArrayList<>();
	double food;
	double crops;
	boolean cultAccessed;

	private String kingdom;

	private transient boolean foodPinned = false;

	//TODO: does this method belong with kingdom/nation?
	static int numUniqueIdeologies(String kingdom, World w) {
		return (int) w.regions.stream()
				.filter(r -> kingdom.equals(r.getKingdom()))
				.flatMap(r -> r.constructions.stream())
				.filter(c -> c.type == Construction.Type.TEMPLE)
				.map(c -> c.religion)
				.distinct()
				.count();
	}

	public boolean canFoodTransferTo(World w, Region target) {
		Set<Region> legals = new HashSet<>();
		legals.add(this);
		Deque<Region> stack = new ArrayDeque<>();
		stack.push(this);

		for (Region n : getNeighbors(w)) {
			if (n.isSea()) stack.push(n);
			legals.add(n);
		}

		while (!stack.isEmpty()) {
			Region r = stack.pop();
			for (Region n : r.getNeighbors(w)) {
				if (n.isSea() && !legals.contains(n)) stack.push(n);
				legals.add(n);
			}
		}

		return legals.contains(target);
	}

	public List<Army.Tag> getArmyTags() {
		return culture.getArmyTags();
	}

	public double calcImmigrationWeight(World w) {
		double mod = 1;
		if (religion == Ideology.FLAME_OF_KITH) mod += getRules().flameOfKithImmigrationWeightMod;
		if (w.getNation(kingdom).hasTag(Nation.Tag.WELCOMING)) mod += 1;
		if (Ideology.CHALICE_OF_COMPASSION == w.getDominantIruhanIdeology() && religion.religion == Religion.IRUHAN) mod += 2;
		return (1 - unrestPopular.get()) * mod;
	}

	public double calcRecruitment(World w, double signingBonus, double rationing, Army largestInRegion, Map<String, List<String>> tributes) {
		double base = population * getRules().recruitmentPerPop;
		double unrest = calcUnrest(w);
		if (unrest > getRules().unrestRecruitmentEffectThresh) base *= 1.0 - (unrest - getRules().unrestRecruitmentEffectThresh);

		double mods = 1;
		double flat = 0;
		flat += constructions.stream().filter(c -> c.type == Construction.Type.TEMPLE && (c.religion == Ideology.RJINKU || c.religion == Ideology.SWORD_OF_TRUTH)).count() * 40;
		mods += calcSigningBonusMod(signingBonus);

		if (w.getNation(getKingdom()).hasTag(Nation.Tag.IMPERIALISTIC)) {
			for (String k : tributes.keySet()) if (tributes.get(k).contains(getKingdom())) mods += 0.2;
		}

		if (hasNoble()) mods += noble.calcRecruitMod();

		for (Character c : w.characters) if (c.location == id && Nation.isFriendly(c.kingdom, kingdom, w)) mods += c.calcRecruitMod();

		Nation wKingdom = w.getNation(kingdom);
		if (wKingdom.hasTag(Nation.Tag.PATRIOTIC)) mods += getRules().patrioticMod;
		if (wKingdom.hasTag(Nation.Tag.DISCIPLINED)) mods += 0.2;
		if (wKingdom.hasTag(Nation.Tag.WARLIKE) && wKingdom.coreRegions.contains(w.regions.indexOf(this))) {
			int conquests = 0;
			for (int i = 0; i < w.regions.size(); i++) if (kingdom.equals(w.regions.get(i).kingdom) && !wKingdom.coreRegions.contains(i)) conquests++;
			mods += conquests * getRules().perConquestWarlikeRecruitmentMod;
		}

		if (culture == Culture.ANPILAYN) mods -= .25;
		else if (culture == Culture.EOLSUNG) mods += .25;
		else if (culture == Culture.HANSA) mods -= .35;
		else if (culture == Culture.TAVIAN) mods -= .1;
		else if (culture == Culture.TYRGAETAN) mods += .35;

		if (religion == Ideology.RJINKU) {
			mods += getRules().rjinkuRecruitmentMod;
		} else if (religion == Ideology.SWORD_OF_TRUTH) {
			mods += getRules().swordOfTruthRecruitmentMod;
		} else if (religion == Ideology.RIVER_OF_KUUN && rationing > 1) {
			mods += (rationing - 1) * 3;
		}

		if (largestInRegion != null && !Nation.isFriendly(kingdom, largestInRegion.kingdom, w) && largestInRegion.hasTag(Army.Tag.PILLAGERS)) mods += getRules().armyPillagersRecruitmentMod;

		return Math.max(0, base * mods + flat);
	}

	// TODO: this belongs alongside the game constants, should determine a way to parameterize these function-type rules
	public double calcSigningBonusMod(double signingBonus){
		return signingBonus <= 0 ? signingBonus * 0.5 : (Math.log(signingBonus) / Math.log(2)) * 0.5 + 0.5;
	}

	public double calcTaxIncome(World w, double taxRate, double rationing) {
		double base = population * getRules().taxPerPop;
		double unrest = calcUnrest(w);
		if (unrest > getRules().unrestTaxEffectThresh) base *= 1.0 - (unrest - getRules().unrestTaxEffectThresh);

		double mods = 1;
		double flat = 0;
		flat += constructions.stream().filter(c -> c.type == Construction.Type.TEMPLE && (c.religion == Ideology.TAPESTRY_OF_PEOPLE || c.religion == Ideology.SYRJEN)).count() * 3;
		long numFortifications = constructions.stream().filter(c -> c.type == Construction.Type.FORTIFICATIONS).count();
		flat -= constructions.stream().filter(c -> c.type == Construction.Type.TEMPLE && c.religion == Ideology.CHALICE_OF_COMPASSION).count();

		if (culture == Culture.ANPILAYN) mods += .25;
		else if (culture == Culture.EOLSUNG) mods -= .25;
		else if (culture == Culture.TYRGAETAN) mods -= .25;

		if (hasNoble()) mods += noble.calcTaxMod();
		for (Character c : w.characters) if (c.location == id && Nation.isFriendly(c.kingdom, kingdom, w)) mods += c.calcTaxMod();

		Nation wKingdom = w.getNation(kingdom);
		if (wKingdom.hasTag(Nation.Tag.MERCANTILE)) mods += getRules().mercantileTaxMod;
		if (wKingdom.hasTag(Nation.Tag.WARLIKE) && wKingdom.coreRegions.contains(w.regions.indexOf(this))) {
			int conquests = 0;
			for (int i = 0; i < w.regions.size(); i++) if (kingdom.equals(w.regions.get(i).kingdom) && !wKingdom.coreRegions.contains(i)) conquests++;
			mods += conquests * getRules().perConquestWarlikeTaxMod;
		}

		boolean neighborKuun = false;
		if (religion == Ideology.SYRJEN) {
			mods += getRules().syrjenTaxMod;
		} else if (religion == Ideology.RIVER_OF_KUUN && rationing > 1) {
			mods += (rationing - 1) * 3;
		} else if (religion == Ideology.CHALICE_OF_COMPASSION) {
			mods += getRules().chaliceOfCompassionTaxMod;
		}

		return Math.max(0, base * mods * taxRate + flat);
	}

	public double calcConsumption() {
		return population;
	}

	public double calcPirateThreat(World w) {
		if (isSea()) return 0;

		double unrest = calcUnrest(w);
		double mods = 1;
		mods += Math.pow(2, w.pirate.bribes.getOrDefault(kingdom, 0.0) / getRules().pirateThreatDoubleGold) - 1;
		return Math.max(0, unrest * mods);
	}

	public void setReligion(Ideology bias, World w) {
		if (bias == null) bias = religion;
		HashMap<Ideology, Integer> ideologies = new HashMap<>();

		for (Construction c : constructions) {
			if (c.type == Construction.Type.TEMPLE) ideologies.put(c.religion, ideologies.getOrDefault(c.religion, 0) + 1);
		}
		int maxV = ideologies.getOrDefault(bias, -1);
		Ideology max = bias;
		for (Ideology r : ideologies.keySet()) {
			if (ideologies.get(r) > maxV) {
				maxV = ideologies.get(r);
				max = r;
			}
		}
		if (max != religion) {
			for (String k : w.getNationNames()) {
				Ideology si = Nation.getStateReligion(k, w);
				if (max.religion == si.religion) w.score(k, Nation.ScoreProfile.RELIGION, getRules().scoreReligionPerConverted);
				if (religion.religion == si.religion) w.score(k, Nation.ScoreProfile.RELIGION, -getRules().scoreReligionPerConverted);
				if (max == si) w.score(k, Nation.ScoreProfile.RELIGION, getRules().scoreIdeologyPerConverted);
				if (religion == si) w.score(k, Nation.ScoreProfile.RELIGION, -getRules().scoreIdeologyPerConverted);
			}
		}
		religion = max;
	}

	public String getKingdom() {
		return kingdom;
	}

	public void setKingdomNoScore(String kingdom) {
		this.kingdom = kingdom;
	}

	public void setKingdom(World w, String kingdom) {
		w.score(kingdom, Nation.ScoreProfile.TERRITORY, getRules().scorePerConqueredTerritory);
		w.score(this.kingdom, Nation.ScoreProfile.TERRITORY, -getRules().scorePerConqueredTerritory);
		this.kingdom = kingdom;
	}

	public Set<Region> getNeighbors(World w) {
		int id = w.regions.indexOf(this);
		Set<Region> neighbors = new HashSet<>();
		for (Geography.Border b : w.getGeography().borders) {
			if (b.b == null) continue;
			if (b.a == id) neighbors.add(w.regions.get(b.b));
			else if (b.b == id) neighbors.add(w.regions.get(b.a));
		}
		return neighbors;
	}

	public Set<Integer> getNeighborsIds(World w) {
		int id = w.regions.indexOf(this);
		Set<Integer> neighbors = new HashSet<>();
		for (Geography.Border b : w.getGeography().borders) {
			if (b.b == null) continue;
			if (b.a == id) neighbors.add(b.b);
			else if (b.b == id) neighbors.add(b.a);
		}
		return neighbors;
	}

	public Map<Region, Integer> getRegionsByDistance(World w) {
		class Node {
			final Region r;
			final int v;
			Node(Region r, int v) {
				this.r = r;
				this.v = v;
			}
		}

		HashMap<Region, Integer> ret = new HashMap<>();
		LinkedList<Node> queue = new LinkedList<>();
		queue.add(new Node(this, 0));
		while (!queue.isEmpty()) {
			Node n = queue.poll();
			if (ret.containsKey(n.r)) continue;
			ret.put(n.r, n.v);
			for (Region r : n.r.getNeighbors(w)) queue.add(new Node(r, n.v + 1));
		}
		return ret;
	}

	public boolean isCoastal(World w) {
		if (isSea()) return false;
		for (Region r : getNeighbors(w)) {
			if (r.isSea()) return true;
		}
		return false;
	}

	public double calcUnrest(GoodwillProvider w) {
		return Math.min(1.0, Math.max(getUnrestPopular(), Math.max(calcUnrestClerical(w), calcUnrestNoble())));
	}

	public double getUnrestPopular(){
		return unrestPopular.get();
	}

	public double calcUnrestClerical(GoodwillProvider w) {
		return Math.min(1, Math.max(0, religion.religion == Religion.IRUHAN && religion != Ideology.VESSEL_OF_FAITH ?
				-w.getGoodwill(kingdom) * getRules().clericalUnrestGoodwillFactor : 0.0));
	}

	// TODO: Some or all of the condition checking into Noble?
	public double calcUnrestNoble() {
		return hasNoble() ? noble.unrest.get() : 0.0;
	}

	// TODO: Enfore min/max, add testing
	public double calcMinConquestStrength(World w) {
		double base = calcBaseConquestStrength(w);
		double mods = 1;
		if (w.getNation(kingdom).hasTag(Nation.Tag.DISCIPLINED)) mods += getRules().disciplinedConqStrengthMod;
		mods += calcFortificationMod();
		return Math.max(0, base * mods);
	}

	// TODO: This is a game rule/equation
	public double calcBaseConquestStrength(GoodwillProvider w) {
		return Math.sqrt(population) * 6 / 100 * (1 - calcUnrest(w) * 0.75);
	}

	public double calcFortificationPct() {
		double fort = 1;
		for (Construction c : constructions) if (c.type == Construction.Type.FORTIFICATIONS) fort += getRules().perFortMod;
		return Math.min(getRules().maxFortMod, fort);
	}

	public double calcFortificationMod() {
		return calcFortificationPct() - 1;
	}

	public boolean isLand() {
		return type == Type.LAND;
	}

	public boolean isSea() {
		return type == Type.WATER;
	}

	public boolean hasNoble() {
		return noble != null && !"".equals(noble.name);
	}

	/**
	 * Returns a set of region ids that are within {@code limit} edges of this region.
	 */
	Set<Integer> getCloseRegionIds(World w, int limit) {
		Set<Integer> closeRegions = new HashSet<>();
		class Node {
			final int r;
			final int dist;
			Node(int r, int dist) { this.r = r; this.dist = dist; }
		}
		PriorityQueue<Node> queue = new PriorityQueue<>(100, Comparator.comparingInt(n -> n.dist));
		queue.add(new Node(w.regions.indexOf(this), 0));
		while (!queue.isEmpty()) {
			Node n = queue.poll();
			if (closeRegions.contains(n.r)) continue;
			closeRegions.add(n.r);
			for (Geography.Border b : w.getGeography().borders) {
				if (b.b != null && b.a == n.r && b.w + n.dist < limit) {
					queue.add(new Node(b.b, b.w + n.dist));
				} else if (b.b != null && b.b == n.r && b.w + n.dist < limit) {
					queue.add(new Node(b.a, b.w + n.dist));
				}
			}
		}
		return closeRegions;
	}

	void eat(double targetRations, String coreRegionOf, World w) {
		if (isSea()) return;
		double actualEat = Math.min(food, targetRations * population);
		food -= actualEat;
		double actualRations = actualEat / population;
		w.score(getKingdom(), Nation.ScoreProfile.PROSPERITY, actualEat * getRules().foodFedPointFactor);
		if (!coreRegionOf.equals(getKingdom())) w.score(coreRegionOf, Nation.ScoreProfile.PROSPERITY, actualEat * getRules().foodFedPointFactor);
		if (actualRations > 1) {
			w.score(getKingdom(), Nation.ScoreProfile.PROSPERITY, (actualEat - population) * getRules().foodFedPlentifulPointFactor);
			if (!coreRegionOf.equals(getKingdom())) w.score(coreRegionOf, Nation.ScoreProfile.PROSPERITY, (actualEat - population) * getRules().foodFedPlentifulPointFactor);
			unrestPopular.add(1.0 - actualRations);
		} else if (actualRations >= 0.75) {
			if (Nation.getStateReligion(kingdom, w) == Ideology.ALYRJA) {
				unrestPopular.add(1.0 - actualRations / 2);
			} else {
				unrestPopular.add(1.0 - actualRations);
			}
		} else {
			double dead = (0.75 - actualRations) * 0.1 * population;
			unrestPopular.add(Math.min(0.35, 1.0 - actualRations));
			population -= dead;
			w.score(coreRegionOf, Nation.ScoreProfile.PROSPERITY, -1 / 8000.0 * dead);
			w.notifyPlayer(getKingdom(), "Starvation", Math.round(dead) + " have starved to death in " + name + ".");
		}
	}

	void plant(int date) {
		if (religion == Ideology.CHALICE_OF_COMPASSION) crops += population * getRules().chaliceOfCompassionPlantPerCitizen;
		crops += constructions.stream().filter(c -> c.type == Construction.Type.TEMPLE && c.religion == Ideology.CHALICE_OF_COMPASSION).count() * 7500;
		double mod = 1;
		if (hasNoble()) mod += noble.calcPlantMod();
		if (culture == Culture.TYRGAETAN) mod -= .15;
		if (culture == Culture.TAVIAN) mod += .15;
		if (Season.isHarvest(date)) {
			crops += population * Season.get(date).getCrops() * mod;
		}
	}

	void harvest(GoodwillProvider goodwills) {
		if (!isLand()) return;
		double maxHarvest = population * getRules().harvestPerCitizen;
		double unrest = calcUnrest(goodwills);
		if (unrest > 0.25) maxHarvest *= 1.25 - unrest;
		maxHarvest = Math.min(crops, maxHarvest);
		food += maxHarvest;
		crops = 0;
	}

	void grow() {
		population += constructions.stream().filter(c -> c.type == Construction.Type.TEMPLE && c.religion == Ideology.FLAME_OF_KITH).count() * 2000;
		population *= 1.001;
	}

	void cultAccess(Collection<Nation> nations, boolean isCriticalCultRegion) {
		if (cultAccessed || !isLand()) return;
		cultAccessed = true;
		for (Nation n : nations) n.score(Nation.ScoreProfile.CULTIST, getRules().scoreCultistRegionAccess);
		if (isCriticalCultRegion) for (Nation n : nations) n.score(Nation.ScoreProfile.CULTIST, getRules().scoreCultistCriticalRegionAccess);
	}

	boolean hasBeenCultAccessed() {
		return cultAccessed;
	}

	void pinFood() {
		foodPinned = true;
	}

	boolean getFoodPinned() {
		return foodPinned;
	}

	private Region(Rules rules) {
		super(rules);
	}

	static Region newRegion(Rules rules) {
		return new Region(rules);
	}
}

class Construction {
	enum Type {
		@SerializedName("fortifications") FORTIFICATIONS,
		@SerializedName("temple") TEMPLE,
		@SerializedName("shipyard") SHIPYARD;
	}
	Type type;
	Ideology religion; // Only for temples.
	double originalCost;

	static Construction makeTemple(Ideology religion, double cost) {
		Construction c = new Construction();
		c.type = Type.TEMPLE;
		c.religion = religion;
		c.originalCost = cost;
		return c;
	}

	static Construction makeFortifications(double cost) {
		Construction c = new Construction();
		c.type = Type.FORTIFICATIONS;
		c.originalCost = cost;
		return c;
	}

	static Construction makeShipyard(double cost) {
		Construction c = new Construction();
		c.type = Type.SHIPYARD;
		c.originalCost = cost;
		return c;
	}

	/** A no-args constructor is needed for GSON. */
	private Construction() {}
}
