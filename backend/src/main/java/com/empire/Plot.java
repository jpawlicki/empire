package com.empire;

import com.empire.util.StringUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

class Plot extends RulesObject {
	static Plot newPlot(Rules rules) { // For GSON.
		return new Plot(rules);
	}

	static Plot newPlot(Rules rules, Set<Integer> existingPlotIds, PlotType type, String targetId, List<String> conspirators) {
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
		p.type = type;
		p.targetId = targetId;
		p.conspirators = new HashSet<>(conspirators);
		return p;
	}

	enum PlotType {
		// Character
		ASSASSINATE(
			(targetId, world) -> "Assassinate " + targetId,
			(targetId, world) -> "assassinate " + targetId + world.getCharacterByName(targetId).map(c -> " (a hero of " + c.kingdom + ")").orElse(""),
			PlotType::getTargetRegionCharacter,
			PlotType::getDefenderCharacter,
			(targetId, world, conspirators) -> {
				world.characters.remove(world.getCharacterByName(targetId).get());
			},
			PlotType::characterOutcomeInfluencer),

		// Region
		BURN_SHIPYARD(
			(targetId, world) -> "Burn Shipyard in " + getTargetRegionRegion(targetId, world).map(r -> r.name).orElse(" an Unknown Region"),
			(targetId, world) -> "Burn Shipyard in " + getTargetRegionRegion(targetId, world).map(r -> r.name).orElse(" an Unknown Region") + " (a region of " + getTargetRegionRegion(targetId, world).map(r -> r.getKingdom()).orElse(" an Unknown Nation") + ")",
			PlotType::getTargetRegionRegion,
			PlotType::getDefenderRegion,
			(targetId, world, conspirators) -> {
				getTargetRegionRegion(targetId, world).ifPresent(r -> {
					r.constructions
							.stream()
							.filter(c -> c.type == Construction.Type.SHIPYARD)
							.reduce((a, b) -> a.originalCost > b.originalCost ? a : b)
							.ifPresent(c -> r.constructions.remove(c));
				});
			},
			PlotType::noOpOutcomeInfluencer),
		SABOTAGE_FORTIFICATIONS(
			(targetId, world) -> "Sabotage Fortifications in " + getTargetRegionRegion(targetId, world).map(r -> r.name).orElse(" an Unknown Region"),
			(targetId, world) -> "Sabotage Fortifications in " + getTargetRegionRegion(targetId, world).map(r -> r.name).orElse(" an Unknown Region") + " (a region of " + getTargetRegionRegion(targetId, world).map(r -> r.getKingdom()).orElse(" an Unknown Nation") + ")",
			PlotType::getTargetRegionRegion,
			PlotType::getDefenderRegion,
			(targetId, world, conspirators) -> {
				getTargetRegionRegion(targetId, world).ifPresent(r -> {
					r.constructions
							.stream()
							.filter(c -> c.type == Construction.Type.FORTIFICATIONS)
							.reduce((a, b) -> a.originalCost > b.originalCost ? a : b)
							.ifPresent(c -> r.constructions.remove(c));
				});
			},
			PlotType::noOpOutcomeInfluencer),
		SPOIL_FOOD(
			(targetId, world) -> "Spoil Food in " + getTargetRegionRegion(targetId, world).map(r -> r.name).orElse(" an Unknown Region"),
			(targetId, world) -> "Spoil Food in " + getTargetRegionRegion(targetId, world).map(r -> r.name).orElse(" an Unknown Region") + " (a region of " + getTargetRegionRegion(targetId, world).map(r -> r.getKingdom()).orElse(" an Unknown Nation") + ")",
			PlotType::getTargetRegionRegion,
			PlotType::getDefenderRegion,
			(targetId, world, conspirators) -> {
				getTargetRegionRegion(targetId, world).ifPresent(r -> r.food /= 2);
			},
			PlotType::noOpOutcomeInfluencer),
		SPOIL_CROPS(
			(targetId, world) -> "Spoil Crops in " + getTargetRegionRegion(targetId, world).map(r -> r.name).orElse(" an Unknown Region"),
			(targetId, world) -> "Spoil Crops in " + getTargetRegionRegion(targetId, world).map(r -> r.name).orElse(" an Unknown Region") + " (a region of " + getTargetRegionRegion(targetId, world).map(r -> r.getKingdom()).orElse(" an Unknown Nation") + ")",
			PlotType::getTargetRegionRegion,
			PlotType::getDefenderRegion,
			(targetId, world, conspirators) -> {
				getTargetRegionRegion(targetId, world).ifPresent(r -> r.crops /= 2);
			},
			PlotType::noOpOutcomeInfluencer),
		INCITE_UNREST(
			(targetId, world) -> "Incite Unrest in " + getTargetRegionRegion(targetId, world).map(r -> r.name).orElse(" an Unknown Region"),
			(targetId, world) -> "Incite Unrest in " + getTargetRegionRegion(targetId, world).map(r -> r.name).orElse(" an Unknown Region") + " (a region of " + getTargetRegionRegion(targetId, world).map(r -> r.getKingdom()).orElse(" an Unknown Nation") + ")",
			PlotType::getTargetRegionRegion,
			PlotType::getDefenderRegion,
			(targetId, world, conspirators) -> {
				getTargetRegionRegion(targetId, world).ifPresent(r -> r.unrestPopular = Math.min(1, r.unrestPopular + 0.4));
			},
			PlotType::noOpOutcomeInfluencer),
		PIN_FOOD(
			(targetId, world) -> "Pin Food in " + getTargetRegionRegion(targetId, world).map(r -> r.name).orElse(" an Unknown Region"),
			(targetId, world) -> "Pin Food in " + getTargetRegionRegion(targetId, world).map(r -> r.name).orElse(" an Unknown Region") + " (a region of " + getTargetRegionRegion(targetId, world).map(r -> r.getKingdom()).orElse(" an Unknown Nation") + ")",
			PlotType::getTargetRegionRegion,
			PlotType::getDefenderRegion,
			(targetId, world, conspirators) -> {
				getTargetRegionRegion(targetId, world).ifPresent(r -> r.pinFood());
			},
			PlotType::noOpOutcomeInfluencer),
		MURDER_NOBLE(
			(targetId, world) -> "Murder Noble in " + getTargetRegionRegion(targetId, world).map(r -> r.name).orElse(" an Unknown Region"),
			(targetId, world) -> "Murder Noble in " + getTargetRegionRegion(targetId, world).map(r -> r.name).orElse(" an Unknown Region") + " (a region of " + getTargetRegionRegion(targetId, world).map(r -> r.getKingdom()).orElse(" an Unknown Nation") + ")",
			PlotType::getTargetRegionRegion,
			PlotType::getDefenderRegion,
			(targetId, world, conspirators) -> {
				getTargetRegionRegion(targetId, world)
						.ifPresent(
								r -> {
									if (r.hasNoble()) r.noble = Noble.newNoble(r.culture, world.date, world.getRules());
								});
			},
			PlotType::noOpOutcomeInfluencer),
		POISON_RELATIONS(
			(targetId, world) -> "Upset Noble in " + getTargetRegionRegion(targetId, world).map(r -> r.name).orElse(" an Unknown Region"),
			(targetId, world) -> "Upset Noble in " + getTargetRegionRegion(targetId, world).map(r -> r.name).orElse(" an Unknown Region") + " (a region of " + getTargetRegionRegion(targetId, world).map(r -> r.getKingdom()).orElse(" an Unknown Nation") + ")",
			PlotType::getTargetRegionRegion,
			PlotType::getDefenderRegion,
			(targetId, world, conspirators) -> {
				getTargetRegionRegion(targetId, world).map(r -> r.noble).ifPresent(n -> n.unrest = Math.min(1, n.unrest + .15));
			},
			PlotType::noOpOutcomeInfluencer),

		// Church
		PRAISE(
			(targetId, world) -> "Praise " + targetId,
			(targetId, world) -> "Praise " + targetId,
			PlotType::getTargetRegionChurch,
			(targetId, world) -> {
				// Defender is the nation with immediately greater goodwill; or the runner up if the target has the most.
				List<String> nations = new ArrayList<>(world.getNationNames());
				Collections.sort(nations, Comparator.comparingDouble(n -> world.getNation(n).goodwill));
				int index = nations.indexOf(targetId);
				return Optional.of(nations.get(index == nations.size() - 1 ? index - 1 : index + 1));
			},
			(targetId, world, conspirators) -> {
				world.getNation(targetId).goodwill += 20;
			},
			PlotType::noOpOutcomeInfluencer),
		DENOUNCE(
			(targetId, world) -> "Denounce " + targetId,
			(targetId, world) -> "Denounce " + targetId,
			PlotType::getTargetRegionChurch,
			(targetId, world) -> Optional.of(targetId),
			(targetId, world, conspirators) -> {
				world.getNation(targetId).goodwill -= 20;
			},
			PlotType::noOpOutcomeInfluencer),

		// Nation
		INTERCEPT_COMMUNICATIONS(
			(targetId, world) -> "Intercept " + targetId + " Communications",
			(targetId, world) -> "Intercept the communications of " + targetId,
			PlotType::getTargetRegionNation,
			PlotType::getDefenderNation,
			(targetId, world, conspirators) -> {
				for (Communication c : world.communications) if (c.postDate == world.date && (c.from.equals(targetId) || c.to.contains(targetId))) c.intercepted.addAll(conspirators);
			},
			PlotType::noOpOutcomeInfluencer),
		SURVEY_NATION(
			(targetId, world) -> "Survey " + targetId,
			(targetId, world) -> "Compile a report on the treasury and armed forces of " + targetId,
			PlotType::getTargetRegionNation,
			PlotType::getDefenderNation,
			(targetId, world, conspirators) -> {
				double soldiers = 0;
				double warships = 0;
				for (Army a : world.armies) {
					if (!a.kingdom.equals(targetId)) continue;
					if (a.type == Army.Type.ARMY) soldiers += a.size;
					else warships += a.size;
				}
				for (String conspirator : conspirators) world.notifications.add(new Notification(conspirator, "Report on " + targetId, "Treasury: " + world.getNation(targetId).gold + "\nSoldiers: " + soldiers + "\nWarships: " + warships));
			},
			PlotType::noOpOutcomeInfluencer);

		private static Optional<Region> getTargetRegionCharacter(String id, World w) {
			return w.getCharacterByName(id).map(c -> c.getLocationRegion(w));
		}

		private static Optional<Region> getTargetRegionRegion(String id, World w) {
			return w.regions.stream().filter(r -> r.name.equals(id)).findAny();
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
			void accept(String targetId, World world, OutcomeWeights outcome, Function<Army, Double> armyStrengthProvider);
		}

		private static interface OutcomeAction {
			void accept(String targetId, World world, Collection<String> conspirators);
		}

		private static void noOpOutcomeInfluencer(String targetId, World w, OutcomeWeights outcome, Function<Army, Double> armyStrengthProvider) {
			/** Do nothing */
		}

		private static void characterOutcomeInfluencer(String targetId, World w, OutcomeWeights outcome, Function<Army, Double> armyStrengthProvider) {
			Optional<Character> target = w.getCharacterByName(targetId);
			if (!target.isPresent()) return;
			Optional<String> defender = getDefenderCharacter(targetId, w);
			if (!defender.isPresent()) return;
			for (Army a : w.armies) if (a.location == target.get().location && a.kingdom.equals(defender.get())) {
				double mod = 1;
				if (Nation.getStateReligion(a.kingdom, w) == Ideology.ALYRJA) mod += 1;
				outcome.addFailureChance(armyStrengthProvider.apply(a) * mod);
			}
		}

		private final BiFunction<String, World, String> title;
		private final BiFunction<String, World, String> details;
		private final BiFunction<String, World, Optional<Region>> targetFunction;
		private final BiFunction<String, World, Optional<String>> defenderFunction;
		private final OutcomeAction onSuccess;
		private final OutcomeInfluencer outcomeInfluencer;

		private PlotType(
				BiFunction<String, World, String> title,
				BiFunction<String, World, String> details,
				BiFunction<String, World, Optional<Region>> targetFunction,
				BiFunction<String, World, Optional<String>> defenderFunction,
				OutcomeAction onSuccess,
				OutcomeInfluencer outcomeInfluencer) {
			this.title = title;
			this.details = details;
			this.targetFunction = targetFunction;
			this.defenderFunction = defenderFunction;
			this.onSuccess = onSuccess;
			this.outcomeInfluencer = outcomeInfluencer;
		}

		String getTitle(String targetId, World world) {
			return title.apply(targetId, world);
		}

		String getDetails(String targetId, World world) {
			return details.apply(targetId, world);
		}

		Optional<Region> getTargetRegion(String targetId, World w) {
			return targetFunction.apply(targetId, w);
		}

		Optional<String> getDefender(String targetId, World w) {
			return defenderFunction.apply(targetId, w);
		}

		void onSuccess(String targetId, World w, Collection<String> conspirators) {
			onSuccess.accept(targetId, w, conspirators);
		}
		
		void influenceOutcome(String targetId, World w, OutcomeWeights outcome, Function<Army, Double> armyStrengthProvider) {
			outcomeInfluencer.accept(targetId, w, outcome, armyStrengthProvider);
		}
	}

	static class OutcomeWeights extends RulesObject {
		private double success = 0;
		private double failure = 0;
		private double criticalFailure = 0;

		// Assuming all sabotaging rings are supporting.
		private double pretendSuccess = 0;
		private double pretendFailure = 0;
		private double pretendCriticalFailure = 0;

		public OutcomeWeights(Rules rules) {
			super(rules);
		}

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

		public double getSuccess() {
			return success;
		}

		public double getFailure() {
			return failure;
		}

		public double getCriticalFailure() {
			return criticalFailure;
		}
	}

	/** A unique ID. */
	private int plotId;

	/**
	 * The target ID format depends on the plot type:
	 * <ul>
	 *  <li>Character: a character name</li>
	 *  <li>Region: a region name</li>
	 *  <li>Church: a nation name</li>
	 *  <li>Nation: a nation name</li>
	 * </ul>
	 */
	private String targetId;

	private PlotType type;

	private double strengthBoost;

	private Set<String> conspirators;

	private double powerHintRandomizer;
	private double powerHintRandomizerTotal;
	private double powerHint;
	private double powerHintTotal;

	public int getId() {
		return plotId;
	}

	public void addConspirator(String kingdom) {
		conspirators.add(kingdom);
	}

	public boolean hasConspirator(String nation) {
		return conspirators.contains(nation);
	}

	public Set<String> getConspirators() {
		return Collections.unmodifiableSet(conspirators);
	}

	public boolean hasAnySupport(Collection<SpyRing> rings) {
		for (SpyRing ring : rings) {
			Optional<SpyRing.InvolvementDisposition> involvedIn = ring.getInvolvementIn(plotId);
			if (involvedIn.isPresent() && involvedIn.get() == SpyRing.InvolvementDisposition.SUPPORTING) return true;
		}
		return false;
	}

	private OutcomeWeights getOutcomeWeights(World w, Function<Army, Double> armyStrengthProvider) {
		// Find target.
		Optional<String> defender = type.getDefender(targetId, w);
		Optional<Region> targetRegion = type.getTargetRegion(targetId, w);
		if (!targetRegion.isPresent() || !defender.isPresent()) return null;

		OutcomeWeights outcome = new OutcomeWeights(getRules());
		type.influenceOutcome(targetId, w, outcome, armyStrengthProvider);
		for (SpyRing ring : w.getSpyRings()) ring.addContributionTo(plotId, targetRegion.get(), defender.get(), w, outcome);
		outcome.success *= 1 + strengthBoost;

		return outcome;
	}

	public void execute(World w, Function<Army, Double> armyStrengthProvider) {
		Optional<String> defender = type.getDefender(targetId, w);
		OutcomeWeights outcome = getOutcomeWeights(w, armyStrengthProvider);

		if (outcome == null || !defender.isPresent()) return;

		// Roll the outcome.
		double roll = Math.random() * (outcome.success + outcome.failure + outcome.criticalFailure);

		// If the plot is supported by a ring belonging to the defender, it always succeeds.
		for (SpyRing ring : w.getSpyRings()) if (ring.getInvolvementIn(plotId).orElse(null) == SpyRing.InvolvementDisposition.SUPPORTING && ring.belongsTo(defender.get())) roll = 0;

		String title = type.getTitle(targetId, w);
		String details = type.getDetails(targetId, w);
		String apparentChance = "\n\nIn the end, the plot had a " + Math.round(100 * outcome.pretendSuccess / (outcome.pretendSuccess + outcome.pretendFailure + outcome.pretendCriticalFailure)) + "% chancee of success, assuming no sabotage.";
		if (roll <= outcome.success) {
			type.onSuccess(targetId, w, conspirators);
			w.notifyAllPlayers("Plot: " + title, "A plot to " + details + " has succeeded." + apparentChance);
		} else if (roll <= outcome.failure + outcome.success) {
			// Reveal a random supporting spy ring to everyone.
			List<SpyRing> supporters = new ArrayList<SpyRing>();
			for (SpyRing ring : w.getSpyRings()) if (ring.getInvolvementIn(plotId).orElse(null) == SpyRing.InvolvementDisposition.SUPPORTING) supporters.add(ring);
			supporters.removeIf(r -> w.regions.get(r.getLocation()).religion == Ideology.LYSKR && r.getNation().equals(w.regions.get(r.getLocation()).getKingdom()));
			SpyRing unlucky = null;
			if (!supporters.isEmpty()) {
				unlucky = supporters.get((int) (Math.random() * supporters.size()));
				unlucky.expose();
			}
			w.notifyAllPlayers("Plot: " + title, "A plot to " + details + " has failed. " + (unlucky != null ? "The involvement of " + unlucky.getNation() + " was proven and their spy ring in " + w.regions.get(unlucky.getLocation()).name + " exposed." : "In the end, all kingdoms withdrew support from the plot.") + apparentChance);
		} else {
			// Build reveal set. - One random supporting ring, then all others have a 50% chance.
			List<SpyRing> supporters = new ArrayList<SpyRing>();
			for (SpyRing ring : w.getSpyRings()) if (ring.getInvolvementIn(plotId).orElse(null) == SpyRing.InvolvementDisposition.SUPPORTING) supporters.add(ring);
			supporters.removeIf(r -> w.regions.get(r.getLocation()).religion == Ideology.LYSKR && r.getNation().equals(w.regions.get(r.getLocation()).getKingdom()));
			Set<String> knownConspirators = new HashSet<String>();
			Set<String> exposureLocations = new HashSet<String>();
			if (!supporters.isEmpty()) {
				SpyRing unlucky = supporters.remove((int) (Math.random() * supporters.size()));
				unlucky.expose();
				unlucky.damage();
				knownConspirators.add(unlucky.getNation());
				exposureLocations.add(w.regions.get(unlucky.getLocation()).name);
			}
			for (SpyRing ring : supporters) if (Math.random() < 0.5) {
				ring.expose();
				ring.damage();
				knownConspirators.add(ring.getNation());
				exposureLocations.add(w.regions.get(ring.getLocation()).name);
			}
			w.notifyAllPlayers("Plot: " + title, "A plot to " + details + " has critically failed. " + (knownConspirators.isEmpty() ? "In the end, all conspirators withdrew support from the plot." : "The involvement of " + StringUtil.and(knownConspirators) + " was proven. Spy rings were exposed in " + StringUtil.and(exposureLocations) + ".") + apparentChance);
		}
	}

	/** Returns true if the plot has been executed. */
	public boolean check(World w, boolean triggered, Function<Army, Double> armyStrengthProvider) {
		if (triggered || Math.random() < getRules().plotEarlyTriggerChance) {
			execute(w, armyStrengthProvider);
			for (SpyRing s : w.spyRings) if (s.getInvolvementIn(plotId).isPresent()) s.involve(-1, SpyRing.InvolvementDisposition.SUPPORTING);
			return true;
		}
		strengthBoost += getRules().plotStrengthGrowth;
		powerHintRandomizer = Math.random() * .2 + .9;
		powerHintRandomizerTotal = Math.random() * .2 + .9;
		return false;
	}

	public void filter(World w, Function<Army, Double> armyStrengthProvider) {
		OutcomeWeights outcome = getOutcomeWeights(w, armyStrengthProvider);
		powerHint = outcome.pretendSuccess * powerHintRandomizer;
		powerHintTotal = (outcome.pretendSuccess + outcome.pretendFailure + outcome.pretendCriticalFailure) * powerHintRandomizerTotal;
		powerHintRandomizer = 0;
		powerHintRandomizerTotal = 0;
	}

	private Plot(Rules rules) {
		super(rules);
		powerHintRandomizer = Math.random() * .2 + .9;
		powerHintRandomizerTotal = Math.random() * .2 + .9;
	}
}
