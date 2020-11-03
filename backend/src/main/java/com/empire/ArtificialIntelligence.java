package com.empire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ArtificialIntelligence {
	public static Map<String, String> getOrders(String who, World world) {
		return new ArtificialIntelligence(who, world).getOrders();
	}

	private static class NoblePiece {
		final Noble noble;
		final int location;
		NoblePiece(Noble noble, int location) {
			this.noble = noble;
			this.location = location;
		}
	}
	private static class RelationshipPiece {
		final String who;
		RelationshipPiece(String who) {
			this.who = who;
		}
	}
	private static class FoodPiece {
		final int location;
		final double amount;
		FoodPiece(int location, double amount) {
			this.location = location;
			this.amount = amount;
		}
	}
	private static class GoldPiece {
		final double amount;
		GoldPiece(double amount) {
			this.amount = amount;
		}
	}
	private static class RationPolicyPiece {}
	private static class TaxPolicyPiece {}
	private static class RecruitmentPolicyPiece {}

	private final String whoami;
	private final World world;
	private ArtificialIntelligence(String whoami, World world) {
		this.whoami = whoami;
		this.world = world;
	}

	private Map<String, String> getOrders() {
		List<Intent> intents = getPossibleIntents();
		getPossiblePieces().assignAll(intents);
		HashMap<String, String> ret = new HashMap<>();
		for (Intent i : intents) ret.putAll(i.generateOrders());
		return ret;
	}

	private PieceSet getPossiblePieces() {
		PieceSet ret = new PieceSet();
		for (Army a : world.armies) if (whoami.equals(a.kingdom)) ret.add(a);
		for (Character c : world.getCharacters()) if (whoami.equals(c.kingdom)) ret.add(c);
		for (int i = 0; i < world.regions.size(); i++) {
			Region r = world.regions.get(i);
			if (!whoami.equals(r.getKingdom())) continue;
			if (r.noble != null) {
				ret.add(new NoblePiece(r.noble, i));
			}
			final int FOOD_CHUNK_SIZE = 50000;
			ret.add(new FoodPiece(i, r.food % FOOD_CHUNK_SIZE));
			for (int j = FOOD_CHUNK_SIZE; j <= r.food; j += FOOD_CHUNK_SIZE) {
				ret.add(new FoodPiece(i, FOOD_CHUNK_SIZE));
			}
		}
		for (SpyRing s : world.spyRings) if (whoami.equals(s.getNation())) ret.add(s);
		for (String k : world.getNationNames()) if (!whoami.equals(k)) ret.add(new RelationshipPiece(k));
		double gold = world.getNation(whoami).gold;
		final int GOLD_CHUNK_SIZE = 20;
		ret.add(new GoldPiece(gold % GOLD_CHUNK_SIZE));
		for (int i = GOLD_CHUNK_SIZE; i <= gold; i += GOLD_CHUNK_SIZE) ret.add(new GoldPiece(GOLD_CHUNK_SIZE));
		ret.add(new RationPolicyPiece());
		ret.add(new TaxPolicyPiece());
		ret.add(new RecruitmentPolicyPiece());
		return ret;
	}

	private int getNobleCount(String who) {
		int nobleCount = 0;
		for (Region rr : world.regions) if (who.equals(rr.getKingdom()) && rr.noble != null) nobleCount++;
		return nobleCount;
	}

	private List<Intent> getPossibleIntents() {
		ArrayList<Intent> ret = new ArrayList<>();
		ret.add(new StealGold());
		ret.add(new Defense());
		ret.add(new Attack());
		ret.add(new Happiness());
		ret.add(new PayTroops());
		ret.add(new BuildMilitary());
		ret.add(new EstablishNobles());
		ret.add(new TrainNoblePieces());
		ret.add(new RelaxNoblePieces());
		ret.add(new BuildForts());
		ret.add(new BuildSpies());
		ret.add(new TreasureGoldPiece());
		ret.add(new FeedPeople());
		ret.add(new DefaultRelations());
		return ret;
	}

	private static class PieceSet {
		final Set<Army> armies = new HashSet<>();
		final Set<Character> characters = new HashSet<>();
		final Set<NoblePiece> nobles = new HashSet<>();
		final Set<SpyRing> spyRings = new HashSet<>();
		final Set<RelationshipPiece> relationships = new HashSet<>();
		final Set<FoodPiece> foodStocks = new HashSet<>();
		final Set<GoldPiece> gold = new HashSet<>();
		final Set<RationPolicyPiece> rationPolicy = new HashSet<>();
		final Set<TaxPolicyPiece> taxPolicy = new HashSet<>();
		final Set<RecruitmentPolicyPiece> recruitmentPolicy = new HashSet<>();

		void add(Army piece) { armies.add(piece); }
		void add(Character piece) { characters.add(piece); }
		void add(NoblePiece piece) { nobles.add(piece); }
		void add(SpyRing piece) { spyRings.add(piece); }
		void add(RelationshipPiece piece) { relationships.add(piece); }
		void add(FoodPiece piece) { foodStocks.add(piece); }
		void add(GoldPiece piece) { gold.add(piece); }
		void add(RationPolicyPiece piece) { rationPolicy.add(piece); }
		void add(TaxPolicyPiece piece) { taxPolicy.add(piece); }
		void add(RecruitmentPolicyPiece piece) { recruitmentPolicy.add(piece); }

		void remove(Army piece) { armies.remove(piece); }
		void remove(Character piece) { characters.remove(piece); }
		void remove(NoblePiece piece) { nobles.remove(piece); }
		void remove(SpyRing piece) { spyRings.remove(piece); }
		void remove(RelationshipPiece piece) { relationships.remove(piece); }
		void remove(FoodPiece piece) { foodStocks.remove(piece); }
		void remove(GoldPiece piece) { gold.remove(piece); }
		void remove(RationPolicyPiece piece) { rationPolicy.remove(piece); }
		void remove(TaxPolicyPiece piece) { taxPolicy.remove(piece); }
		void remove(RecruitmentPolicyPiece piece) { recruitmentPolicy.remove(piece); }

		void addAll(PieceSet p) {
			armies.addAll(p.armies);
			characters.addAll(p.characters);
			nobles.addAll(p.nobles);
			spyRings.addAll(p.spyRings);
			relationships.addAll(p.relationships);
			foodStocks.addAll(p.foodStocks);
			gold.addAll(p.gold);
			rationPolicy.addAll(p.rationPolicy);
			taxPolicy.addAll(p.taxPolicy);
			recruitmentPolicy.addAll(p.recruitmentPolicy);
		}

		void clear() {
			armies.clear();
			characters.clear();
			nobles.clear();
			spyRings.clear();
			relationships.clear();
			foodStocks.clear();
			gold.clear();
			rationPolicy.clear();
			taxPolicy.clear();
			recruitmentPolicy.clear();
		}

		boolean isEmpty() {
			return armies.isEmpty() && characters.isEmpty() && nobles.isEmpty() && spyRings.isEmpty() && relationships.isEmpty() && foodStocks.isEmpty() && gold.isEmpty() && rationPolicy.isEmpty() && taxPolicy.isEmpty() && recruitmentPolicy.isEmpty();
		}

		private static class Assignment {
			final double value;
			final Runnable commitFunction;

			public Assignment(double value, Runnable commitFunction) {
				this.value = value;
				this.commitFunction = commitFunction;
			}
		}

		void assignAll(List<Intent> intents) {
			// O(pieces² × intents)
			boolean keepGoing = true;
			while (keepGoing) {
				if (intents.isEmpty()) return;
				while (!isEmpty()) {
					Assignment maxValue = new Assignment(0, this::clear);
					for (Intent intent : intents) {
						for (Army piece : armies) {
							Assignment hypothetical = new Assignment(intent.valueOf(piece), () -> { intent.allocate(piece); remove(piece); });
							if (maxValue.value < hypothetical.value) maxValue = hypothetical;
						}
						for (Character piece : characters) {
							Assignment hypothetical = new Assignment(intent.valueOf(piece), () -> { intent.allocate(piece); remove(piece); });
							if (maxValue.value < hypothetical.value) maxValue = hypothetical;
						}
						for (NoblePiece piece : nobles) {
							Assignment hypothetical = new Assignment(intent.valueOf(piece), () -> { intent.allocate(piece); remove(piece); });
							if (maxValue.value < hypothetical.value) maxValue = hypothetical;
						}
						for (SpyRing piece : spyRings) {
							Assignment hypothetical = new Assignment(intent.valueOf(piece), () -> { intent.allocate(piece); remove(piece); });
							if (maxValue.value < hypothetical.value) maxValue = hypothetical;
						}
						for (RelationshipPiece piece : relationships) {
							Assignment hypothetical = new Assignment(intent.valueOf(piece), () -> { intent.allocate(piece); remove(piece); });
							if (maxValue.value < hypothetical.value) maxValue = hypothetical;
						}
						for (FoodPiece piece : foodStocks) {
							Assignment hypothetical = new Assignment(intent.valueOf(piece), () -> { intent.allocate(piece); remove(piece); });
							if (maxValue.value < hypothetical.value) maxValue = hypothetical;
						}
						for (GoldPiece piece : gold) {
							Assignment hypothetical = new Assignment(intent.valueOf(piece), () -> { intent.allocate(piece); remove(piece); });
							if (maxValue.value < hypothetical.value) maxValue = hypothetical;
						}
						for (RationPolicyPiece piece : rationPolicy) {
							Assignment hypothetical = new Assignment(intent.valueOf(piece), () -> { intent.allocate(piece); remove(piece); });
							if (maxValue.value < hypothetical.value) maxValue = hypothetical;
						}
						for (TaxPolicyPiece piece : taxPolicy) {
							Assignment hypothetical = new Assignment(intent.valueOf(piece), () -> { intent.allocate(piece); remove(piece); });
							if (maxValue.value < hypothetical.value) maxValue = hypothetical;
						}
						for (RecruitmentPolicyPiece piece : recruitmentPolicy) {
							Assignment hypothetical = new Assignment(intent.valueOf(piece), () -> { intent.allocate(piece); remove(piece); });
							if (maxValue.value < hypothetical.value) maxValue = hypothetical;
						}
					}
					maxValue.commitFunction.run();
				}
				List<Intent> remove = new ArrayList<>();
				for (Intent i : intents) if (!i.feasible()) remove.add(i);
				for (Intent i : remove) {
					intents.remove(i);
					addAll(i.allocatedPieces);
				}
				keepGoing = !remove.isEmpty();
			}
		}
	}

	private static abstract class Intent {
		protected PieceSet allocatedPieces = new PieceSet();
		void allocate(Army piece) { allocatedPieces.add(piece); }
		void allocate(Character piece) { allocatedPieces.add(piece); }
		void allocate(NoblePiece piece) { allocatedPieces.add(piece); }
		void allocate(SpyRing piece) { allocatedPieces.add(piece); }
		void allocate(RelationshipPiece piece) { allocatedPieces.add(piece); }
		void allocate(FoodPiece piece) { allocatedPieces.add(piece); }
		void allocate(GoldPiece piece) { allocatedPieces.add(piece); }
		void allocate(RationPolicyPiece piece) { allocatedPieces.add(piece); }
		void allocate(TaxPolicyPiece piece) { allocatedPieces.add(piece); }
		void allocate(RecruitmentPolicyPiece piece) { allocatedPieces.add(piece); }

		/** Return the value of the given piece to the intent, from 0 to 1.0. */
		double valueOf(Army piece) { return 0; }
		double valueOf(Character piece) { return 0; }
		double valueOf(NoblePiece piece) { return 0; }
		double valueOf(SpyRing piece) { return 0; }
		double valueOf(RelationshipPiece piece) { return 0; }
		double valueOf(FoodPiece piece) { return 0; }
		double valueOf(GoldPiece piece) { return 0; }
		double valueOf(RationPolicyPiece piece) { return 0; }
		double valueOf(TaxPolicyPiece piece) { return 0; }
		double valueOf(RecruitmentPolicyPiece piece) { return 0; }

		/** Return true if the Intent is feasible to accomplish with the currently-allocated pieces. */
		abstract boolean feasible();

		/** Return a score for the importance of the intent, from 0 to positive infinity. */
		abstract double importance();

		/** Generate orders. */
		abstract Map<String, String> generateOrders();

		double currentlyAllocatedGold() {
			double avail = 0;
			for (GoldPiece piece : allocatedPieces.gold) {
				avail += piece.amount;
			}
			return avail;
		}
	}

	/** An intent to defend owned regions from attack. */
	private class Defense extends Intent {
		private class Threat {
			final String kingdom;
			final double threatValue;

			public Threat(String kingdom, double threatValue) {
				this.kingdom = kingdom;
				this.threatValue = threatValue;
			}
		}

		private final ArrayList<Threat> threats;
		private final int primaryResponseRegion;
		private final double neededSize;
		private final HashSet<Integer> ourRegions = new HashSet<>();
		private final HashSet<Integer> neighborRegions = new HashSet<>();

		// TODO: break into regional subintents.

		public Defense() {
			// Threat classification.
			// Armies adjacent to or occupying our regions have a threat based on their strength.
			// Nations that have declared war on us have a 2x threat multiplier.
			HashMap<String, Double> threats = new HashMap<>();
			for (int i = 0; i < world.regions.size(); i++) if (whoami.equals(world.regions.get(i).getKingdom())) ourRegions.add(i);
			for (Integer ourRegion : ourRegions) neighborRegions.addAll(world.regions.get(ourRegion).getNeighborsIds(world));
			neighborRegions.removeAll(ourRegions);
			for (String k : world.getNationNames()) if (!whoami.equals(k)) threats.put(k, 0.0);
			for (Army a : world.armies) {
				if (!threats.containsKey(a.kingdom)) continue;
				if (ourRegions.contains(a.location)) threats.put(a.kingdom, threats.get(a.kingdom) + a.size / 100 * 0.6);
				else if (neighborRegions.contains(a.location)) threats.put(a.kingdom, threats.get(a.kingdom) + a.size / 100);
			}
			threats.replaceAll((k, v) -> v * (world.getNation(k).getRelationship(whoami).battle == Relationship.War.ATTACK ? 2 : 1));
			this.threats = new ArrayList<Threat>();
			for (Map.Entry<String, Double> threat : threats.entrySet()) this.threats.add(new Threat(threat.getKey(), threat.getValue()));
			Collections.sort(this.threats, Comparator.comparingDouble((Threat t) -> t.threatValue).reversed());

			double neededSize = 0;
			int primaryResponseRegion = 0;
			for (Army a : world.armies) {
				if (whoami.equals(a.kingdom)) continue;
				if (!ourRegions.contains(a.location)) continue;
				if (a.size > neededSize) {
					neededSize = a.size;
					primaryResponseRegion = a.location;
				}
			}
			this.neededSize = neededSize;
			this.primaryResponseRegion = primaryResponseRegion;
		}

		@Override
		boolean feasible() {
			return true; // Not so sure about this.
		}

		@Override
		double valueOf(Army piece) {
			if (neededSize == 0) return 0;
			if (piece.type == Army.Type.NAVY) return 0;
			if (piece.size < neededSize) return 0;
			if (!ourRegions.contains(piece.location) && !neighborRegions.contains(piece.location)) return 0;
			return piece.size / neededSize;
		}

		@Override
		double valueOf(Character piece) {
			if (!allocatedPieces.characters.isEmpty()) return 0;
			for (Army a : allocatedPieces.armies) if (a.location == piece.location) return piece.calcLeadMod(Army.Type.ARMY);
			return 0;
		}

		@Override
		double valueOf(RelationshipPiece piece) {
			// Relationships for the top four threats are valuable, if threat is above 25.
			if (threats.size() > 0 && threats.get(0).kingdom.equals(piece.who) && threats.get(0).threatValue > 40) return 1.00;
			if (threats.size() > 1 && threats.get(1).kingdom.equals(piece.who) && threats.get(1).threatValue > 60) return 0.75;
			if (threats.size() > 2 && threats.get(2).kingdom.equals(piece.who) && threats.get(2).threatValue > 100) return 0.50;
			if (threats.size() > 3 && threats.get(3).kingdom.equals(piece.who) && threats.get(3).threatValue > 160) return 0.25;
			return 0;
		}

		@Override
		double importance() {
			return threats.stream().map(t -> t.threatValue / 40).reduce((a, b) -> a + b).orElse(0.0);
		}

		@Override
		Map<String, String> generateOrders() {
			HashMap<String, String> orders = new HashMap<>();
			// Tribute top threats.
			for (RelationshipPiece r : allocatedPieces.relationships) {
				double ourNavy = 0;
				double theirNavy = 0;
				for (Army a : world.armies) {
					if (a.type != Army.Type.NAVY) continue;
					if (whoami.equals(a.kingdom)) {
						ourNavy += a.size;
					} else if (r.who.equals(a.kingdom)) {
						theirNavy += a.size;
					}
				}
				if (getNobleCount(r.who) >= 2) {
					orders.put("rel_" + r.who + "_attack", "DEFEND");
					orders.put("rel_" + r.who + "_tribute", "0.25");
				} else if (ourNavy < theirNavy * 1.2) {
					orders.put("rel_" + r.who + "_attack", "ATTACK");
					orders.put("rel_" + r.who + "_tribute", "0");
				} else {
					orders.put("rel_" + r.who + "_attack", "NEUTRAL");
					orders.put("rel_" + r.who + "_tribute", "0");
				}
			}
			for (Army a : allocatedPieces.armies) {
				// If the army is in or can respond to the region, do so.
				String primaryResponseRegionName = world.regions.get(primaryResponseRegion).name;
				if (a.location == primaryResponseRegion) {
					orders.put("action_army_" + a.id, "Stay in " + world.regions.get(a.location).name);
				} else if (world.regions.get(a.location).getNeighbors(world).stream().anyMatch(r -> r.name.equals(primaryResponseRegionName))) {
					orders.put("action_army_" + a.id, "Travel to " + primaryResponseRegionName);
				} else {
					// Otherwise wander randomly in our territory.
					ArrayList<Region> neighbors = new ArrayList<>(world.regions.get(a.location).getNeighbors(world));
					neighbors.removeIf(r -> !whoami.equals(r.getKingdom()));
					if (!neighbors.isEmpty()) orders.put("action_army_" + a.id, "Travel to " + neighbors.get((int)(Math.random() * neighbors.size())).name);
				}
			}
			for (Character c : allocatedPieces.characters) {
				for (Army a : allocatedPieces.armies) {
					if (c.location == a.location) {
						orders.put("action_" + c.name.replace(" ", "_").replace("'", "_"), "Lead army " + a.id);
						break;
					}
				}
			}
			return orders;
		}
	}

	/** An intent to conquer a region. */
	private class Attack extends Intent {
		private final Region idealTarget;
		private final Army idealArmy;

		public Attack() {
			Army bestArmy = null;
			for (Army a : world.armies) if (whoami.equals(a.kingdom) && a.type == Army.Type.ARMY && (bestArmy == null || bestArmy.size < a.size)) bestArmy = a;
			if (bestArmy == null) {
				idealTarget = null;
				idealArmy = null;
			} else {
				Map<Region, Integer> regionDistances = world.regions.get(bestArmy.location).getRegionsByDistance(world);
				double bestScore = 0;
				Region bestRegion = null;
				for (Region r : world.regions) {
					double rScore = 1;
					if (r.isSea()) continue;
					if (whoami.equals(r.getKingdom())) continue;
					Relationship rel = world.getNation(whoami).getRelationship(r.getKingdom());
					if (rel.battle == Relationship.War.DEFEND) continue;
					if (r.calcMinConquestStrength(world) > bestArmy.calcStrength(world, null, 0)) continue;
					if (regionDistances.get(r) > 1) continue;
					if (world.getNation(r.getKingdom()).getRelationship(whoami).tribute >= 0.25 && getNobleCount(whoami) > 0) continue;
					if (world.getNation(whoami).coreRegions.contains(r.id)) rScore *= 4;
					if (rScore > bestScore) {
						bestScore = rScore;
						bestRegion = r;
					}
				}
				idealTarget = bestRegion;
				idealArmy = bestArmy;
			}
		}

		@Override
		boolean feasible() {
			return
					!allocatedPieces.relationships.isEmpty()
					&& idealArmy != null
					&& idealTarget != null
					&& allocatedPieces.armies.contains(idealArmy);
		}

		@Override
		double valueOf(Army a) {
			return a == idealArmy ? 1 : 0;
		}

		@Override
		double valueOf(RelationshipPiece piece) {
			if (idealTarget != null && idealTarget.getKingdom().equals(piece.who)) return 1;
			return 0;
		}

		@Override
		double importance() {
			return 1.6;
		}

		@Override
		Map<String, String> generateOrders() {
			HashMap<String, String> orders = new HashMap<>();
			for (RelationshipPiece r : allocatedPieces.relationships) {
				orders.put("rel_" + r.who + "_attack", "ATTACK");
				orders.put("rel_" + r.who + "_tribute", "0");
			}
			for (Army a : allocatedPieces.armies) {
				if (a.location == idealTarget.id) {
					orders.put("action_army_" + a.id, "Conquer");
				} else {
					orders.put("action_army_" + a.id, "Travel to " + idealTarget.name);
				}
			}
			return orders;
		}
	}

	// TODO: an intent to convert off non-VoF Iruhan religions if church opinion is negative.
	// TODO: an intent to deal with pirates.

	/** An intent to keep popular unrest down. */
	private class EstablishNobles extends Intent {
		public EstablishNobles() {
		}

		@Override
		boolean feasible() {
			// Republicans can never have nobles, but also don't start having nobles if previously avoided.
			return !world.getNation(whoami).hasTag(Nation.Tag.REPUBLICAN) && getNobleCount(whoami) != 0;
		}

		@Override
		double valueOf(Character c) {
			Region r = world.regions.get(c.location);
			if (whoami.equals(r.getKingdom()) && !r.hasNoble()) {
				for (Character cc : allocatedPieces.characters) if (cc.location == c.location) return 0;
				return 1;
			}
			return 0;
		}

		@Override
		double importance() {
			return 0.3;
		}

		@Override
		Map<String, String> generateOrders() {
			HashMap<String, String> orders = new HashMap<>();
			for (Character c : allocatedPieces.characters) {
				orders.put("action_" + c.name.replace(" ", "_").replace("'", "_"), "Establish Noble");
			}
			return orders;
		}
	}


	/** An intent to keep popular unrest down. */
	private class Happiness extends Intent {
		final double targetUnrestDrop;

		public Happiness() {
			ArrayList<Double> unrests = new ArrayList<>();
			for (Region r : world.regions) if (whoami.equals(r.getKingdom())) unrests.add(r.unrestPopular.get());
			Collections.sort(unrests);
			targetUnrestDrop = unrests.isEmpty() ? 0 : unrests.get((int)(unrests.size() * 0.65));
		}

		@Override
		boolean feasible() {
			return targetUnrestDrop > 0;
		}

		@Override
		double valueOf(RationPolicyPiece piece) {
			return 0.5;
		}

		@Override
		double valueOf(TaxPolicyPiece piece) {
			return 0.75;
		}

		// TODO: consider temple construction?
		// TODO: consider tribute to Imperialistic nations.

		@Override
		double importance() {
			return targetUnrestDrop * 2;
		}

		@Override
		Map<String, String> generateOrders() {
			HashMap<String, String> orders = new HashMap<>();
			double projection = 0;
			boolean hasTax = !allocatedPieces.taxPolicy.isEmpty();
			boolean hasRation = !allocatedPieces.rationPolicy.isEmpty();
			if (hasRation) {
				// Spend rations up to 1.15 or 1.25 depending on.
				double rationUp = Math.min(targetUnrestDrop, hasTax ? 0.15 : 0.25);
				projection = rationUp;
				orders.put("economy_ration", Double.toString(100 + rationUp * 100));
			}
			if (hasTax) {
				double tax = 1.0;
				if (targetUnrestDrop - projection > 0.20) tax = 0.0;
				else if (targetUnrestDrop - projection > 0.16) tax = 0.25;
				else if (targetUnrestDrop - projection > 0.12) tax = 0.50;
				else if (targetUnrestDrop - projection > 0.08) tax = 0.75;
				orders.put("economy_tax", Double.toString(100 * tax));
			}
			return orders;
		}
	}

	/** An intent to steal gold whenever possible. */
	private class StealGold extends Intent {
		public StealGold() {
		}

		@Override
		boolean feasible() {
			return true; // However much we can steal is good.
		}

		@Override
		double importance() {
			return 0.1;
		}

		@Override
		Map<String, String> generateOrders() {
			HashMap<String, String> orders = new HashMap<>();
			int plots = 0;
			for (String k : world.getNationNames()) {
				if (whoami.equals(k)) continue;
				double leverage = world.getNation(whoami).getLeverage(k);
				if (leverage <= 0) continue;
				orders.put("plot_type_" + plots, "steal");
				orders.put("plot_nation_" + plots, k);
				orders.put("plot_amount_" + plots, Double.toString(Math.floor(leverage * 4)));
				plots++;
			}
			return orders;
		}
	}

	/** An intent to pay existing sailors and soldiers. */
	private class PayTroops extends Intent {
		final double projectedCost;
		public PayTroops() {
			double projectedCost = 0;
			for (Army a : world.armies) if (whoami.equals(a.kingdom)) projectedCost += a.getCost(world, "Travel ");
			this.projectedCost = projectedCost;
		}

		@Override
		boolean feasible() {
			return true; // However much we can save to pay them is good.
		}

		@Override
		double valueOf(GoldPiece piece) {
			if (currentlyAllocatedGold() > projectedCost) return 0;
			return 1; 
		}

		@Override
		double valueOf(TaxPolicyPiece piece) {
			if (currentlyAllocatedGold() > projectedCost) return 0;
			return 0.2; 
		}

		// TODO: Tavian army pieces have a value here, since they cost less if not travelling.

		@Override
		double importance() {
			return 1.0;
		}

		@Override
		Map<String, String> generateOrders() {
			return new HashMap<>(); // This just consumes money, no orders for now.
		}
	}

	/** An intent to merge up troops into large armies, and build general / admiral expertise. */
	private class BuildMilitary extends Intent {
		@Override
		boolean feasible() {
			return true;
		}

		@Override
		double valueOf(Army piece) {
			// Navies at shore should go to sea (TODO: unless doing so is suicidal).
			if (piece.type == Army.Type.NAVY && world.regions.get(piece.location).type == Region.Type.LAND) return 1;
			// Higher-index armies cohabiting a region are very high value for merges.
			for (Army a : world.armies) {
				if (a == piece) break;
				if (whoami.equals(a.kingdom) && a.type == piece.type && a.location == piece.location) return 100;
			}
			return 0.05;
		}

		@Override
		double valueOf(Character piece) {
			// TODO: Nation's best general and admiral are valuable, other characters are not.
			return 0;
		}

		@Override
		double importance() {
			return 1.1;
		}

		@Override
		Map<String, String> generateOrders() {
			HashMap<String, String> orders = new HashMap<>();
			HashSet<Army> leftovers = new HashSet<>(allocatedPieces.armies);
			for (Army piece : allocatedPieces.armies) {
				// Navies at shore go to sea.
				if (piece.type == Army.Type.NAVY && world.regions.get(piece.location).type == Region.Type.LAND) {
					for (Region r : world.regions.get(piece.location).getNeighbors(world)) {
						if (r.type == Region.Type.WATER) {
							orders.put("action_army_" + piece.id, "Travel to " + r.name);
							leftovers.remove(piece);
							break;
						}
					}
					continue;
				}
				// Look for merges.
				for (Army a : world.armies) {
					if (a.location == piece.location && whoami.equals(a.kingdom) && a.id < piece.id && a.type == piece.type) {
						orders.put("action_army_" + piece.id, "Merge into " + a.id);
						leftovers.remove(piece);
						break;
					}
				}
			}

			// Leftover armies should try to travel to regions where they can merge up.
			HashMap<Integer, Double> regionScores = new HashMap<>();
			for (Army a : leftovers) {
				regionScores.put(a.location, regionScores.getOrDefault(a.location, 0.0) + a.size);
				for (Integer rid : world.regions.get(a.location).getNeighborsIds(world)) {
					regionScores.put(rid, regionScores.getOrDefault(rid, 0.0) + a.size);
				}
			}
			for (Army a : world.armies) {
				if (!whoami.equals(a.kingdom)) {
					regionScores.put(a.location, regionScores.getOrDefault(a.location, 0.0) - a.size * 2);
				}
			}
			for (int i = 0; i < world.regions.size(); i++) {
				if (whoami.equals(world.regions.get(i).getKingdom())) regionScores.put(i, regionScores.getOrDefault(i, 0.0) * 2);
			}
			for (Army a : leftovers) {
				int bestOption = -1;
				for (Integer rid : world.regions.get(a.location).getNeighborsIds(world)) {
					if ((bestOption == -1 || regionScores.get(rid) > regionScores.get(bestOption)) && (a.type != Army.Type.NAVY || world.regions.get(rid).type != Region.Type.LAND)) bestOption = rid;
				}
				if (bestOption == -1) {
					orders.put("action_army_" + a.id, "Stay in " + world.regions.get(a.location).name);
				} else {
					orders.put("action_army_" + a.id, (a.location == bestOption ? "Stay in " : "Travel to ") + world.regions.get(bestOption).name);
				}
			}
			return orders;
		}
	}

	/** An intent to train nobles. */
	private static class TrainNoblePieces extends Intent {
		@Override
		boolean feasible() {
			return true; // Can use any valued pieces.
		}

		@Override
		double valueOf(NoblePiece piece) {
			return 1.0 / piece.noble.calcLevel(); 
		}

		@Override
		double importance() {
			return 0.6;
		}

		@Override
		Map<String, String> generateOrders() {
			HashMap<String, String> orders = new HashMap<>();
			for (NoblePiece n : allocatedPieces.nobles) {
				orders.put("action_noble_" + n.location, "Train");
			}
			return orders;
		}
	}

	/** An intent to keep noble unrest low. */
	private class RelaxNoblePieces extends Intent {
		@Override
		boolean feasible() {
			return true; // Can use any valued pieces.
		}

		@Override
		double valueOf(NoblePiece piece) {
			return world.regions.get(piece.location).noble.unrest.get();
		}

		@Override
		double importance() {
			return 2;
		}

		@Override
		Map<String, String> generateOrders() {
			HashMap<String, String> orders = new HashMap<>();
			for (NoblePiece n : allocatedPieces.nobles) {
				orders.put("action_noble_" + n.location, "Relax");
			}
			return orders;
		}
	}

	/** An intent to fortify. */
	private static class BuildForts extends Intent {
		// TODO: break into regional sub-intents.
		@Override
		boolean feasible() {
			return currentlyAllocatedGold() > 20 && allocatedPieces.nobles.size() > 0;
		}

		@Override
		double valueOf(NoblePiece piece) {
			return 1;
		}

		@Override
		double valueOf(GoldPiece piece) {
			return currentlyAllocatedGold() > 100 ? 0 : 1;
		}

		@Override
		double importance() {
			// TODO: increase importance when being invaded or when neighbors have large neutral armies.
			return 0.3;
		}

		@Override
		Map<String, String> generateOrders() {
			double budget = currentlyAllocatedGold();
			HashMap<String, String> orders = new HashMap<>();
			for (NoblePiece n : allocatedPieces.nobles) {
				if (budget >= 20) {
					orders.put("action_noble_" + n.location, "Build Fortifications");
					budget -= 20;
				}
			}
			return orders;
		}
	}

	/** An intent to establish additional spy rings. */
	private class BuildSpies extends Intent {
		final HashSet<Integer> immediateBuildOpportunities;
		final int numSpyRings;

		public BuildSpies() {
			HashSet<Integer> noBuildLocations = new HashSet<>();
			immediateBuildOpportunities = new HashSet<>();
			int numSpyRings = 0;
			for (SpyRing s : world.spyRings) if (whoami.equals(s.getNation())) {
				noBuildLocations.add(s.getLocation());
				numSpyRings++;
			}
			this.numSpyRings = numSpyRings;
			for (int i = 0; i < world.regions.size(); i++) if (world.regions.get(i).type == Region.Type.WATER) noBuildLocations.add(i);
			// for (Character c : world.characters) if (whoami.equals(c.kingdom) && !noBuildLocations.contains(c.location)) immediateBuildOpportunities.add(c.location);
			for (int i = 0; i < world.regions.size(); i++) {
				Region r = world.regions.get(i);
				if (whoami.equals(r.getKingdom()) && r.noble != null && !noBuildLocations.contains(i)) immediateBuildOpportunities.add(i);
			}
		}

		@Override
		boolean feasible() {
			return !immediateBuildOpportunities.isEmpty() && (Nation.getStateReligion(whoami, world) == Ideology.LYSKR || currentlyAllocatedGold() > 50);
		}

		// TODO: characters can create spy rings.

		@Override
		double valueOf(NoblePiece piece) {
			// TODO: If the plot already has a character here, discount the piece.
			// If the noble is in a region with no spy ring, valuable.
			// Otherwise, not valuable.
			for (SpyRing s : world.spyRings) if (whoami.equals(s.getNation()) && s.getLocation() == piece.location) return 0;
			return 1;
		}

		@Override
		double valueOf(GoldPiece piece) {
			if (Nation.getStateReligion(whoami, world) == Ideology.LYSKR) return 0;
			if (currentlyAllocatedGold() >= immediateBuildOpportunities.size() * 50) return 0;
			return 1;
		}

		@Override
		double importance() {
			return 0.6;
		}

		@Override
		Map<String, String> generateOrders() {
			double budget = currentlyAllocatedGold();
			HashMap<String, String> orders = new HashMap<>();
			for (NoblePiece n : allocatedPieces.nobles) {
				if (budget >= 50) {
					orders.put("action_noble_" + n.location, "Establish Spy Ring");
					budget -= 50;
				}
			}
			return orders;
		}
	}

	// TODO: Intent to destroy enemy spy rings.

	/** An intent to save up gold. */
	private class TreasureGoldPiece extends Intent {
		@Override
		boolean feasible() {
			return true; // Always good to treasure gold.
		}

		@Override
		double valueOf(GoldPiece piece) {
			return 1;
		}

		@Override
		double valueOf(Character piece) {
			for (Character c : allocatedPieces.characters) if (c.location == piece.location) return 0;
			if (whoami.equals(world.regions.get(piece.location).getKingdom())) {
				return 0.05;
			}
			return 0;
		}

		// TODO: value of spy rings.

		@Override
		double importance() {
			Nation me = world.getNation(whoami);
			if (me.hasProfile(Nation.ScoreProfile.RICHES)) {
				if (me.gold < 225) return 1;
				if (me.gold < 550) return 0.5;
				if (me.gold < 2100) return 0.25;
			}
			return 0.1;
		}

		@Override
		Map<String, String> generateOrders() {
			HashMap<String, String> orders = new HashMap<>();
			for (Character c : allocatedPieces.characters) {
				orders.put("action_" + c.name.replace(" ", "_").replace("'", "_"), "Govern " + world.regions.get(c.location).name);
			}
			return orders;
		}
	}

	/** An intent to feed ruled citizens. */
	private class FeedPeople extends Intent {
		private final double foodProjection;

		FeedPeople() {
			int turnsUntilHarvest = 4 - world.date % 4;
			double totalStocks = 0;
			double totalDemand = 0;
			for (Region r : world.regions) {
				if (!whoami.equals(r.getKingdom())) continue;
				totalStocks += r.food;
				totalDemand += r.population;
			}
			foodProjection = turnsUntilHarvest == 0 ? 1.0 : totalStocks / (totalDemand * turnsUntilHarvest);
		}

		@Override
		boolean feasible() {
			// This intent can use all food and ration policy pieces it gets.
			return true;
		}

		@Override
		double valueOf(FoodPiece piece) {
			// Food pieces are critical if food is short, otherwise merely important.
			return foodProjection < 1.0 ? 1 : 0.75;
		}

		@Override
		double valueOf(RationPolicyPiece piece) {
			// If rationing is critical to feed people, owning the ration policy is important.
			// Otherwise, the rationing policy can be given to other intents if they are important.
			return foodProjection < 1.0 ? 1 : 0.2;
		}

		@Override
		double valueOf(TaxPolicyPiece piece) {
			// If rationing is below a given point, owning the tax policy is also important.
			// Otherwise, the tax policy can be given to other intents if they are important.
			return foodProjection < 0.92 ? 1 : 0.2;
		}

		@Override
		double importance() {
			if (world.getNation(whoami).hasProfile(Nation.ScoreProfile.PROSPERITY)) {
				return 2;
			}
			return 1;
		}

		@Override
		Map<String, String> generateOrders() {
			HashMap<String, String> orders = new HashMap<>();
			boolean hasTax = !allocatedPieces.taxPolicy.isEmpty();
			boolean hasRation = !allocatedPieces.rationPolicy.isEmpty();
			if (hasRation) {
				// Never set out to starve or eat plentifully.
				double rationPlan = Math.max(Math.min(foodProjection, 1.0), 0.75);
				orders.put("economy_ration", Double.toString(Math.floor(rationPlan * 100)));
			}
			if (hasTax) {
				double tax = 1.0;
				if (foodProjection < 0.8) tax = 0.0;
				else if (foodProjection < 0.84) tax = 0.25;
				else if (foodProjection < 0.88) tax = 0.50;
				else if (foodProjection < 0.92) tax = 0.75;
				orders.put("economy_tax", Double.toString(100 * tax));
			}
			return orders;
		}
	}

	/** An intent to just normalize relations. */
	private class DefaultRelations extends Intent {
		public DefaultRelations() {
		}

		@Override
		boolean feasible() {
			return true;
		}

		@Override
		double valueOf(RelationshipPiece piece) {
			return 1;
		}

		@Override
		double importance() {
			return 0.005;
		}

		@Override
		Map<String, String> generateOrders() {
			HashMap<String, String> orders = new HashMap<>();
			HashSet<Integer> myArmyLocs = new HashSet<>();
			for (Army c : world.armies) if (whoami.equals(c.kingdom)) myArmyLocs.add(c.location);
			for (RelationshipPiece r : allocatedPieces.relationships) {
				boolean sharesArea = false;
				for (Army c : world.armies) {
					if (r.who.equals(c.kingdom) && myArmyLocs.contains(c.location)) {
						sharesArea = true;
						break;
					}
				}
				if (sharesArea || (world.getNation(r.who).getRelationship(whoami).tribute >= 0.25 && getNobleCount(whoami) > 0)) {
					orders.put("rel_" + r.who + "_attack", "NEUTRAL");
					orders.put("rel_" + r.who + "_tribute", "0");
				} else {
					// An attack disposition is diplomatically risky, but cuts enemy leverage gains.
					orders.put("rel_" + r.who + "_attack", "ATTACK");
					orders.put("rel_" + r.who + "_tribute", "0");
				}
			}
			return orders;
		}
	}
}
