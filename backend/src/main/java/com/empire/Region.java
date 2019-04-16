package com.empire;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;

final class Region {
	String name;
	String type; // "land" or "water" // TODO: can make this into an enum with @SerializedName
	String culture;
	String climate;
	double population;
	String kingdom;
	Ideology religion;
	double unrestPopular;
	Noble noble;
	List<Construction> constructions = new ArrayList<>();
	double food;
	double harvest;
	boolean gotCultFood;

	public boolean canFoodTransferTo(World w, Region target) {
		HashSet<Region> legals = new HashSet<>();
		ArrayList<Region> stack = new ArrayList<>();
		stack.add(this);
		legals.add(this);
		for (Region n : getNeighbors(w)) {
			if (n.type.equals("water")) stack.add(n);
			legals.add(n);
		}
		while (!stack.isEmpty()) {
			Region r = stack.remove(stack.size() - 1);
			for (Region n : r.getNeighbors(w)) {
				if (n.type.equals("water") && !legals.contains(n)) stack.add(n);
				legals.add(n);
			}
		}
		return legals.contains(target);
	}

	public ArrayList<String> getArmyTags() {
		ArrayList<String> t = new ArrayList<>();
		switch (culture) {
			case "anpilayn":
				t.add("Steel");
				t.add("Formations");
				break;
			case "eolsung":
				t.add("Pillagers");
				t.add("Raiders");
				break;
			case "hansa":
				t.add("Seafaring");
				t.add("Impressment");
				break;
			case "tyrgaetan":
				t.add("Weathered");
				t.add("Pathfinders");
				break;
			case "tavian":
				t.add("Riders");
				t.add("Crafts-soldiers");
				break;
		}
		return t;
	}

	private static int numUniqueIdeologies(String kingdom, World w) {
		HashSet<Ideology> ideologies = new HashSet<>();
		for (Region r : w.regions) if (kingdom.equals(r.kingdom)) ideologies.add(r.religion);
		return ideologies.size();
	}

	public double calcRecruitment(World w, ArrayList<Character> governors, double signingBonus, boolean rulerBattled, double rationing, Army largestInRegion) {
		double base = population / 2000.0;
		double mods = 1;
		NationData wKingdom = w.getNation(kingdom);
		if (signingBonus == -1) mods -= 0.5;
		else if (signingBonus == -2) mods -= 1;
		else if (signingBonus >= 1) mods += (Math.log(signingBonus) / Math.log(2)) * .5 + .5;
		if (governors != null) {
			for (Character c : governors) {
				mods += c.calcLevel("governor") * .5 + 1;
			}
		}
		double unrest = calcUnrest(w);
		if (unrest > .25) base *= 1.25 - unrest;
		if (noble != null && noble.hasTag("Inspiring")) mods += .5;
		if (noble != null && noble.hasTag("Untrusting")) mods -= .35;
		if (noble != null && noble.hasTag("Tyrannical")) mods -= .5;
		if (religion == Ideology.RJINKU) {
			mods += 1;
		} else if (religion == Ideology.SWORD_OF_TRUTH) {
			mods += 1;
		} else if (religion == Ideology.TAPESTRY_OF_PEOPLE) {
			boolean getTapestryBonus = false;
			for (Region r : getNeighbors(w)) if (r.type.equals("land") && (!r.culture.equals(culture) || !r.religion == religion)) getTapestryBonus = true;
			if (getTapestryBonus) mods += .5;
		} else if (religion == Ideology.RIVER_OF_KUUN && rationing >= 1.25) {
			mods += .5;
		}
		if (largestInRegion != null && !NationData.isFriendly(kingdom, largestInRegion.kingdom, w) && largestInRegion.hasTag("Pillagers")) mods -= .75;
		if (wKingdom.hasTag("Coast-Dwelling") && isCoastal(w)) mods += .12;
		if (wKingdom.hasTag("Patriotic")) mods += .15;
		if (wKingdom.hasTag("War-like") && wKingdom.coreRegions.contains(w.regions.indexOf(this))) {
			int conquests = 0;
			for (int i = 0; i < w.regions.size(); i++) if (kingdom.equals(w.regions.get(i).kingdom) && !wKingdom.coreRegions.contains(i)) conquests++;
			mods += conquests * .05;
		}
		if ("Northern (Rjinku)".equals(NationData.getStateReligion(kingdom, w)) && rulerBattled) mods += .5;
		if ("Iruhan (Tapestry of People)".equals(NationData.getStateReligion(kingdom, w))) mods += .03 * numUniqueIdeologies(kingdom, w);
		if (NationData.getStateReligion(kingdom, w).startsWith("Iruhan") && "Iruhan (Tapestry of People)".equals(w.getDominantIruhanIdeology()) && NationData.getStateReligion(kingdom, w).startsWith("Iruhan")) mods += .03 * numUniqueIdeologies(kingdom, w);
		return Math.max(0, base * mods);
	}


	public double calcTaxIncome(World w, ArrayList<Character> governors, double taxRate, double rationing) {
		double base = population / 10000.0;
		double mods = taxRate;
		double unrest = calcUnrest(w);
		NationData wKingdom = w.getNation(kingdom);
		if (unrest > .25) base *= 1.25 - unrest;
		if (noble != null && noble.hasTag("Frugal")) mods += .5;
		if (noble != null && noble.hasTag("Hoarding")) mods -= .35;
		if (governors != null) {
			for (Character c : governors) {
				mods += c.calcLevel("governor") * .5 + 1;
			}
		}
		if (wKingdom.hasTag("Coast-Dwelling") && isCoastal(w)) mods += .12;
		if (wKingdom.hasTag("Mercantile")) mods += .15;
		boolean neighborKuun = false;
		for (Region r : getNeighbors(w)) {
			if (r.kingdom != null && !r.kingdom.equals(kingdom) && "Tavian (River of Kuun)".equals(NationData.getStateReligion(r.kingdom, w))) neighborKuun = true;
		}
		if (neighborKuun) mods += 0.5;
		if (religion == Ideology.SYRJEN) {
			mods += 1.25;
		} else if (religion == Ideology.TAPESTRY_OF_PEOPLE) {
			boolean getTapestryBonus = false;
			for (Region r : getNeighbors(w)) if (r.type.equals("land") && (!r.culture.equals(culture) || !r.religion == religion)) getTapestryBonus = true;
			if (getTapestryBonus) mods += .5;
		} else if (religion == Ideology.RIVER_OF_KUUN && rationing == 1.25) {
			mods += .5;
		} else if (religion == Ideology.CHALICE_OF_COMPASSION) {
			mods -= .3;
		}
		if (wKingdom.hasTag("War-like") && wKingdom.coreRegions.contains(w.regions.indexOf(this))) {
			int conquests = 0;
			for (int i = 0; i < w.regions.size(); i++) if (kingdom.equals(w.regions.get(i).kingdom) && !wKingdom.coreRegions.contains(i)) conquests++;
			mods += conquests * .05;
		}
		if ("Iruhan (Tapestry of People)".equals(NationData.getStateReligion(kingdom, w))) mods += .03 * numUniqueIdeologies(kingdom, w);
		if (NationData.getStateReligion(kingdom, w).startsWith("Iruhan") && "Iruhan (Tapestry of People)".equals(w.getDominantIruhanIdeology()) && NationData.getStateReligion(kingdom, w).startsWith("Iruhan")) mods += .03 * numUniqueIdeologies(kingdom, w);
		return Math.max(0, base * mods);
	}

	public double calcConsumption(World w, double foodMod) {
		double base = population;
		double mods = foodMod;
		if (noble != null && noble.hasTag("Rationing")) mods -= .2;
		if (noble != null && noble.hasTag("Wasteful")) mods += .1;
		if (NationData.getStateReligion(kingdom, w).equals("Iruhan (Chalice of Compassion)")) mods -= .15;
		if (mods < 0) mods = 0;
		return base * mods;
	}

	public double calcPirateThreat(World w) {
		if ("water".equals(type)) return 0;
		if (religion == Ideology.ALYRJA) return 0;
		if (noble != null && noble.hasTag("Policing")) return 0;
		double unrest = calcUnrest(w);
		double mods = 1;
		if (noble != null && noble.hasTag("Shady Connections")) mods += 2;
		if (noble != null) mods -= 0.5;
		mods += Math.pow(2, w.pirate.bribes.getOrDefault(kingdom, 0.0) / 30) - 1;
		return Math.max(0, unrest * mods);
	}

	public void setReligion(Ideology bias, World w) {
		HashMap<Ideology, Integer> ideologies = new HashMap<>();
		for (Construction c : constructions) {
			if (c.type.equals("temple")) ideologies.put(c.religion, ideologies.getOrDefault(c.religion, 0) + 1);
		}
		int maxV = ideologies.getOrDefault(bias, -1);
		Ideology max = bias == null ? religion : bias;
		for (String r : ideologies.keySet()) {
			if (ideologies.get(r) > maxV) {
				maxV = ideologies.get(r);
				max = r;
			}
		}
		if (Ideology.VESSEL_OF_FAITH == max && religion != max) {
			for (String k : w.getNationNames()) {
				if (Ideology.VESSEL_OF_FAITH != NationData.getStateReligion(k, w)) continue;
				for (Region r : w.regions) if (k.equals(r.kingdom)) r.unrestPopular = Math.max(0, r.unrestPopular - .05);
			}
		}
		if (max != religion) {
			for (String k : w.getNationNames()) {
				Ideology si = NationData.getStateReligion(k, w);
				if (max.religion == si.religion) w.score(k, "religion", 2);
				if (religion.religion == si.religion) w.score(k, "religion", -2);
				if (max == si) w.score(k, "ideology", 3);
				if (religion == si) w.score(k, "ideology", -3);
			}
		}
		religion = max;
	}

	public Set<Region> getNeighbors(World w) {
		int id = w.regions.indexOf(this);
		Set<Region> neighbors = new HashSet<>();
		for (WorldConstantData.Border b : WorldConstantData.borders) {
			if (b.a == id) neighbors.add(w.regions.get(b.b));
			else if (b.b == id) neighbors.add(w.regions.get(b.a));
		}
		return neighbors;
	}

	public boolean isCoastal(World w) {
		if (type == "water") return false;
		for (Region r : getNeighbors(w)) {
			if (r.type == "water") return true;
		}
		return false;
	}

	public Map<String, Double> calcPlotPowers(World w, List<String> boosts, int inspires) {
		HashMap<String, Double> powers = new HashMap<>();
		for (String kingdom : w.getNationNames()) {
			powers.put(kingdom, 0.0);
		}
		final class Node {
			public final double power;
			public final Region location;
			public Node(double power, Region location) {
				this.power = power;
				this.location = location;
			}
		}

		for (final Character c : w.characters) {
			Function<Node, Node> getPower = (Node n) -> {
				Region r = n.location;
				if (r.type.equals("water")) return new Node(n.power * .9, n.location);
				if (NationData.isFriendly(c.kingdom, r.kingdom, w)) {
					if (r.religion == Ideology.LYSKR) return new Node(n.power, n.location);
					return new Node(n.power * (.9 - r.calcUnrest(w) / 10), n.location);
				}
				return new Node(n.power * (.8 + r.calcUnrest(w) / 10), n.location);
			};
			PriorityQueue<Node> pq = new PriorityQueue<>(100, new Comparator<Node>() {
				@Override
				public int compare(Node a, Node b) {
					return a.power > b.power ? -1 : a.power < b.power ? 1 : 0;
				}
			});
			HashSet<Region> visited = new HashSet<>();
			pq.add(getPower.apply(new Node(c.calcPlotPower(w, boosts.contains(c.kingdom), inspires), w.regions.get(c.location))));
			while (!pq.isEmpty()) {
				Node n = pq.poll();
				if (visited.contains(n.location)) continue;
				visited.add(n.location);
				if (n.location == this) {
					powers.put(c.kingdom, Math.max(powers.get(c.kingdom), n.power));
					break;
				}
				for (Region r : n.location.getNeighbors(w)) {
					if (!visited.contains(r)) pq.add(getPower.apply(new Node(n.power, r)));
				}
			}
		}
		return powers;
	}

	public double calcUnrest(World w) {
		double unrest = unrestPopular;
		if (religion.religion == Religion.IRUHAN && religion != Ideology.VESSEL_OF_FAITH) {
			unrest = Math.max(unrest, -w.getNation(kingdom).goodwill / 100);
		}
		if (noble != null && noble.name != "" && !"".equals(noble.name)) {
			unrest = Math.max(noble.unrest, unrest);
		}
		return Math.min(1, Math.max(0, unrest));
	}

	public double calcMinConquestStrength(World w) {
		double base = Math.sqrt(population) * 6 / 100 * (1 - calcUnrest(w) / 2);
		double mods = 1;
		if (noble != null && noble.hasTag("Loyal")) mods += 1;
		if (noble != null && noble.hasTag("Desperate")) mods -= 2;
		if (w.getNation(kingdom).hasTag("Stoic")) mods += .75;
		mods += calcFortification() - 1;
		return Math.max(0, base * mods);
	}

	public double calcMinPatrolStrength(World w) {
		double mods = 1;
		mods += calcUnrest(w) * 2 - .7;
		return Math.sqrt(population) * 3 / 100 * mods;
	}

	public double calcFortification() {
		double fort = 1;
		for (Construction c : constructions) if (c.type.equals("fortifications")) fort += .15;
		return Math.min(5, fort);
	}

	private static String ideologyToReligion(String ideology) {
		if ("Company".equals(ideology)) return ideology;
		return ideology.substring(0, ideology.indexOf("(") - 1);
	}
}

final class Construction {
	String type;
	Ideology religion; // Only for type == "temple".
	double originalCost;
}

