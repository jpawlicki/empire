package com.empire;

import com.empire.util.StringUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Collection;
import java.util.Collections;

class Plot extends RulesObject {
	static Plot newPlot(Rules rules) { // For GSON.
		return new Plot(rules);
	}

	static Plot newPlot(Rules rules, Set<Integer> existingPlotIds) {
		Plot p = new Plot(rules);
		int i = (int) (Math.random() * 10000);
		int trials = 0;
		while (existingPlotIds.contains(i) && trials < 100) { // Ideally, pick a random plot ID so as not to disclose how many plots are in existence.
			i = (int) (Math.random() * 10000);
			trials++;
		}
		if (existingPlotIds.contains(i)) {
			// Give up the random picking - at this point it's more important to not let a single game consume too much CPU.
			i = Collections.max(existingPlotIds) + 1;
		}
		p.plotId = i;
	}

	abstract enum PlotType {
		// Character
		ASSASSINATE(
			(targetId, world) -> "Assassinate " + targetId,
			(targetId, world) -> "assassinate " + targetId + world.getCharacterByName(targetId).map(c -> " (a hero of " + c.kingdom + ")").orElse(""),
			PlotType::getTargetRegionCharacter,
			PlotType::getDefenderCharacter,
			(targetId, world, perpetrator) -> {
				world.characters.remove(world.getCharacterByName(targetId).get());
			},
			PlotType::characterOutcomeInfluencer),
		CAPTURE(
			(targetId, world) -> "Capture " + targetId,
			(targetId, world) -> "capture " + targetId + world.getCharacterByName(targetId).map(c -> " (a hero of " + c.kingdom + ")").orElse(""),
			PlotType::getTargetRegionCharacter,
			PlotType::getDefenderCharacter,
			(targetId, world, perpetrator) -> {
				world.getCharacterByName(targetId).ifPresent(c -> c.captor = perpetrator);
			},
			PlotType::characterOutcomeInfluencer),
		RESCUE(
			(targetId, world) -> "Rescue " + targetId,
			(targetId, world) -> "rescue " + targetId + world.getCharacterByName(targetId).map(c -> " (a hero of " + c.kingdom + ")").orElse(""),
			PlotType::getTargetRegionCharacter,
			PlotType::getDefenderCharacter,
			(targetId, world, perpetrator) {
				world.getCharacterByName(targetId).ifPresent(c -> c.captor = "");
			},
			PlotType::characterOutcomeInfluencer),

		// Region
		BURN_SHIPYARD(
			(targetId, world) -> "Burn Shipyard in " + getTargetRegionRegion(targetId, world).map(r -> r.name).orElse(" an Unknown Region"),
			(targetId, world) -> "Burn Shipyard in " + getTargetRegionRegion(targetId, world).map(r -> r.name).name + " (a ,
			PlotType::getTargetRegionRegion,
			PlotType::getDefenderRegion,
			(targetId, world, perpetrator) {
				getTargetRegionRegion(targetId, world).ifPresent(r -> {
					r.constructions
							.stream()
							.filter(c -> c.type == Construction.Type.SHIPYARD)
							.reduce((a, b) -> a.cost > b.cost ? a : b)
							.ifPresent(c -> r.constructions.remove(c));
				});
			},
			PlotType::noOpOutcomeInfluencer),
		SABOTAGE_FORTIFICATIONS(
			"title",
			"details",
			PlotType::getTargetRegionRegion,
			PlotType::getDefenderRegion,
			(targetId, world, perpetrator) {
				getTargetRegionRegion(targetId, world).ifPresent(r -> {
					r.constructions
							.stream()
							.filter(c -> c.type == Construction.Type.FORTIFICATIONS)
							.reduce((a, b) -> a.cost > b.cost ? a : b)
							.ifPresent(c -> r.constructions.remove(c));
				});
			},
			PlotType::noOpOutcomeInfluencer),
		SPOIL_FOOD(
			"title",
			"details",
			PlotType::getTargetRegionRegion,
			PlotType::getDefenderRegion,
			(targetId, world, perpetrator) -> {
				getTargetRegionRegion(targetId, world).ifPresent(r -> r.food /= 2);
			},
			PlotType::noOpOutcomeInfluencer),
		SPOIL_CROPS(
			"title",
			"details",
			PlotType::getTargetRegionRegion,
			PlotType::getDefenderRegion,
			(targetId, world, perpetrator) -> {
				getTargetRegionRegion(targetId, world).ifPresent(r -> r.crops /= 2);
			},
			PlotType::noOpOutcomeInfluencer),
		INCITE_UNREST(
			"title",
			"details",
			PlotType::getTargetRegionRegion,
			PlotType::getDefenderRegion,
			(targetId, world, perpetrator) {
				getTargetRegionRegion(targetId, world).ifPresent(r -> r.unrestPopular = Math.min(1, r.unrestPopular + 0.4));
			},
			PlotType::noOpOutcomeInfluencer),
		PIN_FOOD(
			"title",
			"details",
			PlotType::getTargetRegionRegion,
			PlotType::getDefenderRegion,
			(targetId, world, perpetrator) {
				getTargetRegionRegion(targetId, world).ifPresent(r -> r.pinFood());
			},
			PlotType::noOpOutcomeInfluencer),
		MURDER_NOBLE(
			"title",
			"details",
			PlotType::getTargetRegionRegion,
			PlotType::getDefenderRegion,
			(targetId, world, perpetrator) {
				getTargetRegionRegion(targetId, world)
						.ifPresent(
								r -> {
									if (r.hasNoble()) r.noble = Noble.newNoble(r.culture, date, getRules());
								});
			},
			PlotType::noOpOutcomeInfluencer),
		POISON_RELATIONS(
			"title",
			"details",
			PlotType::getTargetRegionRegion,
			PlotType::getDefenderRegion,
			(targetId, world, perpetrator) {
				getTargetRegionRegion(targetId, world).map(r -> r.noble).ifPresent(n -> n.unrest = Math.min(1, n.unrest + .15));
			},
			PlotType::noOpOutcomeInfluencer),

		// Church
		PRAISE(
			"title",
			"details",
			PlotType::getTargetRegionChurch,
			() -> { /* defender */ },
			(targetId, world, perpetrator) -> {
				world.getNation(targetId).goodwill += 20;
			},
			PlotType::noOpOutcomeInfluencer),
		DENOUNCE(
			"title",
			"details",
			PlotType::getTargetRegionChurch,
			() -> { /* defender */ },
			(targetId, world, perpetrator) {
				world.getNation(targetId).goodwill -= 20;
			},
			PlotType::noOpOutcomeInfluencer),

		// Nation
		INTERCEPT_COMMUNICATIONS(
			"title",
			"details",
			PlotType::getTargetRegionNation,
			PlotType::getDefenderNation,
			(targetId, world, perpetrator) {
				for (Communication c : communications) if (c.from.equals(targetId) || c.to.contains(targetId)) c.intercepted.add(perpetrator);
			},
			PlotType::noOpOutcomeInfluencer),
		SURVEY_NATION(
			"title",
			"details",
			PlotType::getTargetRegionNation,
			PlotType::getDefenderNation
			(targetId, world, perpetrator) {
				double soldiers = 0;
				double warships = 0;
				for (Army a : world.armies) {
					if (!a.kingdom.equals(targetId)) continue;
					if (a.type == Army.Type.ARMY) soldiers += a.size;
					else warships += a.size;
				}
				world.notifications.add(new Notification(perpetrator, "Report on " + targetId, "Treasury: " + world.getNation(targetId).gold + "\nSoldiers: " + soldiers + "\nWarships: " + warships));
			},
			PlotType::noOpOutcomeInfluencer);

		private static Optional<Region> getTargetRegionCharacter(String id, World w) {
			return w.getCharacterByName(id).map(c -> c.getLocationRegion(w));
		}

		private static Optional<Region> getTargetRegionRegion(String id, World w) {
			return w.regions.get(Integer.parseInt(id));
		}

		private static Optional<Region> getTargetRegionChurch(String id, World w) {
			return Optional.of(w.regions.get(w.getGeography().holycity));
		}

		private static Optional<Region> getTargetRegionNation(String id, World w) {
			return w.getRuler(id).map(c -> c.getLocationRegion(w));
		}

		private static Optional<String> getDefenderCharacter(String id, World w) {
			return w.getCharacterByName(id).map(c -> c.kingdom);
		}

		private static Optional<String> getDefenderRegion(String id, World w) {
			return getTargetRegionRegion(id, w).map(r -> r.getKingdom());
		}

		private static Optional<String> getDefenderNation(String id, World w) {
			return Optional.of(id);
		}

		private static interface OutcomeInfluencer {
			void accept(String targetId, World world, PlotOutcomeWeights outcome);
		}

		private static void noOpOutcomeInfluencer(String targetId, World w, PlotOutcomeWeights outcome, Function<Army, Double> armyStrengthProvider) {
			/** Do nothing */
		}

		private static void characterOutcomeInfluencer(String targetId, World w, PlotOutcomeWeights outcome, Function<Army, Double> armyStrengthProvider) {
			Optional<Character> target = w.getCharacterByName(targetId);
			if (target.isAbsent()) return;
			Optional<String> defender = getDefenderCharacter(targetId, w);
			if (defender.isAbsent()) return;
			for (Army a : w.armies) if (a.location == target.get().location && a.kingdom.equals(defender.get())) {
				outcome.addFailureChance(armyStrengthProvider(a));
			}
		}

		private final String title;
		private final String details;
		private final BiFunction<String, World, Optional<Region>> targetFunction;
		private final BiFunction<String, World, Optional<String>> defenderFunction;
		private final BiConsumer<String, World> onSuccess;
		private final OutcomeInfluencer outcomeInfluencer;

		private PlotType(
				String title,
				String details,
				BiFunction<String, World, Optional<Region>> targetFunction,
				BiFunction<String, World, Optional<String>> defenderFunction,
				BiConsumer<String, World> onSuccess,
				OutcomeInfluencer outcomeInfluencer) {
			this.title = title;
			this.details = details;
			this.targetFunction = targetFunction;
			this.defenderFunction = defenderFunction;
			this.onSuccess = onSuccess;
			this.outcomeInfluencer = outcomeInfluencer;
		}

		String getTitle(String targetId) {
			return title.replace("%TARGETID%", targetId);
		}

		String getDetails(String targetId) {
			return details.replace("%TARGETID%", targetId);
		}

		Optional<Region> getTargetRegion(String targetId, World w) {
			return targetFunction.apply(targetId, w);
		}

		Optional<String> getDefender(String targetId, World w) {
			return defenderFunction.apply(targetId, w);
		}

		void onSuccess(String targetId, World w) {
			onSuccess.accept(targetId, w);
		}
		
		void influenceOutcome(String targetId, World w, PlotOutcomeWeights outcome, armyStrengthProvider) {
			outcomeInfluencer.accept(targetId, w, outcome, armyStrengthProvider);
		}
	}

	class PlotOutcomeWeights {
		private double success = 0;
		private double failure = 0;
		private double criticalFailure = 0;

		// Assuming all sabotaging rings are supporting.
		private double pretendSuccess = 0;
		private double pretendFailure = 0;
		private double pretendCriticalFailure = 0;

		public void support(double amount) {
			success += amount;
			pretendSuccess += amount;
		}

		public void addFailureChance(double amount) {
			failure += amount;
			pretendFailure += amount;
		}

		public void defend(double amount) {
			failure += amount;
			criticalFailure += amount;
			pretendFailure += amount;
			pretendCriticalFailure += amount;
		}

		public void sabotage(double amount) {
			criticalFailure += getRules().sabotagingRingFactor * amount;
			pretendSuccess += amount;
		}
	}

	/** A unique ID. */
	private int plotId;

	/**
	 * The target ID format depends on the plot type:
	 * <ul>
	 *  <li>Character: a character name</li>
	 *  <li>Region: a region ID (a number)</li>
	 *  <li>Church: a nation name</li>
	 *  <li>Nation: a nation name</li>
	 * </ul>
	 */
	private String targetId;

	private PlotType type;

	private double strengthBoost;

	private List<String> consipirators;

	public void addConspirator(String kingdom) {
		conspirators.add(kingdom);
	}

	public boolean hasAnySupport(Collection<SpyRing> rings) {
		for (SpyRing ring : w.getSpyRings()) {
			Optional<InvolvementDisposition> involvedIn = ring.getInvolvementIn(plotId);
			if (involvedIn.isPresent() && involvedIn.get() == SpyRing.InvolvementDisposition.SUPPORTING) return true;
		}
		return false;
	}

	public void execute(World w) {
		// Find target.
		Region targetRegion = type.getTargetRegion(w, targetId);
		String defender = type.getDefender(w, targetId);

		PlotOutcomeWeights outcome = new PlotOutcomeWeights();
		type.influenceOutcome(outcome, targetId, w);

		// Get involved spy rings.
		for (SpyRing ring : w.getSpyRings()) ring.addContributionTo(plotId, targetRegion, defender, w, outcome);

		// Compute probability of success / fail / critical failure.
		double roll = Math.random() * (outcome.success + outcome.failure + outcome.criticalFailure);

		// If the plot is supported by a ring belonging to the defender, it always succeeds.
		for (SpyRing ring : w.getSpyRings()) if (ring.getInvolvementIn(plotId) == SpyRing.InvolvementDisposition.SUPPORTING && ring.belongsTo(defender)) roll = 0;

		String apparentChance = "\n\nIn the end, the plot had a " + Math.round(100 * outcome.pretendSuccess / (outcome.pretendSuccess + outcome.pretendFailure + outcome.pretendCriticalFailure)) + "% chancee of success, assuming no sabotge.";
		if (roll <=  outcome.success) {
			type.onSuccess(targetId);
			w.notifyAll("Plot: " + type.getTitle(targetId), "A plot to " + type.getDetails(targetId) + " has succeeded." + apparentChance);
		} else if (roll <= outcome.failure + outcome.success) {
			// Reveal a random supporting spy ring to everyone.
			List<SpyRing> supporters = new ArrayList<SpyRing>();
			for (SpyRing ring : w.getSpyRings()) if (ring.getInvolvementIn(plotId) == SpyRing.InvolvementDisposition.SUPPORTING) supporters.add(ring);
			SpyRing unlucky = null;
			if (!supporters.isEmpty()) {
				unlucky = supporters.get((int) (Math.random() * supporters.size()));
				unlucky.expose();
			}
			w.notifyAll("Plot: " + type.getTitle(targetId), "A plot to " + type.getDetails(targetId) + " has failed. " + (unlucky != null ? "The involvement of " + unlucky.getKingdom() + " was proven." : "In the end, all kingdoms withdrew support from the plot.") + apparentChance);
		} else {
			// Build reveal set. - One random supporting ring, then all others have a 50% chance.
			List<SpyRing> supporters = new ArrayList<SpyRing>();
			for (SpyRing ring : w.getSpyRings()) if (ring.getInvolvementIn(plotId) == SpyRing.InvolvementDisposition.SUPPORTING) supporters.add(ring);
			Set<String> knownConspirators = new HashSet<String>();
			if (!supporters.isEmpty()) {
				SpyRing unlucky = supporters.remove((int) (Math.random() * supporters.size()));
				unlucky.expose();
				unlucky.damage();
				knownConspirators.add(unlucky.getKingdom());
			}
			for (SpyRing ring : supporters) if (Math.random() < 0.5) {
				ring.expose();
				ring.damage();
				knownConspirators.add(ring.getKingdom());
			}
			w.notifyAll("Plot: " + type.getTitle(targetId), "A plot to " + type.getDetails(targetId) + " has critically failed. " + (knownConsipirators.isEmpty() ? "In the end, all conspirators withdrew support from the plot." : "The involvement of " + StringUtil.and(knownConspirators) + " was proven.") + apparentChance);
		}
	}

	/** Returns true if the plot has been executed. */
	public boolean check(World w) {
		if (Math.random() < getRules().plotEarlyTriggerChance) {
			execute(w);
			return true;
		}
		strengthBoost += getRules().plotStrengthGrowth;
		return false;
	}

	private Plot(Rules rules) {
		super(rules);
	}
}
