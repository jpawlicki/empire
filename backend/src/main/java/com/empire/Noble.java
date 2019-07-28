package com.empire;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

class Noble extends RulesObject {
	String name;
	Crisis crisis;
	double unrest;
	private double experience;

	enum Action {
		SOOTHE,
		LEVY,
		CONSCRIPT,
		OTHER;
	}
	transient Action action = Action.OTHER;

	static Noble newNoble(Rules rules) {
		return new Noble(rules);
	}

	static Noble newNoble(Culture culture, int date, Rules rules) {
		Noble n = newNoble(rules);
		n.name = WorldConstantData.getRandomName(culture, Math.random() < 0.5 ? WorldConstantData.Gender.MAN : WorldConstantData.Gender.WOMAN);
		n.unrest = 0;
		n.crisis = new Crisis();
		n.crisis.type = Crisis.Type.NONE;
		n.crisis.deadline = date + rules.nobleCrisisFrequency;		
		return n;
	}

	double calcLevel() {
		return Math.sqrt(experience + 1);
	}

	double calcPlantMod() {
		return calcLevel() * getRules().noblePlantModPerLevel;
	}

	double calcRecruitMod() {
		double mod = calcLevel() * getRules().nobleRecruitModPerLevel;
		if (action == Action.CONSCRIPT) mod += getRules().nobleActionConscriptionMod;
		return mod;
	}

	double calcTaxMod() {
		double mod = calcLevel() * getRules().nobleTaxModPerLevel;
		if (action == Action.LEVY) mod += getRules().nobleActionLevyMod;
		if (action == Action.SOOTHE) mod += getRules().nobleActionSootheMod;
		return mod;
	}

	void addExperience() {
		experience++;
	}

	/**
	 * Checks the noble's crisis to see if it has been resolved or deadlined, and applies those effects.
	 * @return a notification that should be added to the world.
	 */	
	Optional<Notification> resolveCrisis(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
		if (crisis.type.isSolved(w, r, leaders, governors, builds, templeBuilds, rationing, lastStands, inspires)) {
			crisis.type = Crisis.Type.NONE;
			unrest = Math.max(0, unrest + getRules().nobleCrisisSuccessUnrest);
			return Optional.of(new Notification(r.getKingdom(), "Noble Crisis Resolved", crisis.type.getSuccessMessage(name, r.name)));
		} else if (crisis.type != Crisis.Type.NONE && crisis.deadline == w.date) {
			unrest = Math.min(1, unrest + getRules().nobleCrisisFailedUnrest);
			return Optional.of(new Notification(r.getKingdom(), "Noble Crisis Expired", crisis.type.getFailMessage(name, r.name)));
		}
		return Optional.empty();
	}

	/**
	 * Creates a new crisis if necessary.
	 * @return a notification that should be added to the world.
	 */
	Optional<Notification> createCrisis(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
		if (crisis.deadline > w.date) return Optional.empty();
		List<Crisis.Type> possibleCrises = new ArrayList<>();
		for (Crisis.Type type : Crisis.Type.values()) {
			if (type.isCreateable(w, r, getRules(), leaders, governors, builds, templeBuilds, rationing, lastStands, inspires)) possibleCrises.add(type);
		}
		if (possibleCrises.isEmpty()) {
			crisis.type = Crisis.Type.NONE;
			crisis.deadline = w.date + 1;
			return Optional.empty();
		} else {
			crisis.deadline = w.date + 6;
			crisis.type = possibleCrises.get((int)(Math.random() * possibleCrises.size()));
			return Optional.of(new Notification(r.getKingdom(), "Noble Crisis (" + crisis.type.toString().toLowerCase() + ")", crisis.type.getStartMessage(name, r.name)));
		}
	}

	private Noble(Rules rules) {
		super(rules);
	}
}

final class Crisis {
	Type type;
	int deadline;

	enum Type {
		NONE("", "", "") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
				return false;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
				return false;
			}
		},
		WEDDING("%NOBLENAME%'s daughter is being wed in %REGIONNAME% and they request your presence as ruler.", "The wedding of the daughter of %NOBLENAME% was a spectacular affair. It is rare for the nobility to find much joy in their political marriage, but in this case the couple's obvious love for one another warmed your heart to witness. %NOBLENAME% could not thank you enough for attending, and swore never to forget this day.", "The wedding %NOBLENAME% invited you to in %REGIONNAME% has taken place without you and %NOBLENAME% is very cross.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
				for (Character c : w.characters) if (c.hasTag(Character.Tag.RULER) && c.kingdom.equals(r.getKingdom()) && c.location == w.regions.indexOf(r)) return true;
				return false;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
				Set<Integer> closeRegions = r.getCloseRegionIds(w, rules.nobleCrisisFrequency);
				for (Character c : w.characters) {
					if (c.kingdom.equals(r.getKingdom()) && c.hasTag(Character.Tag.RULER) && closeRegions.contains(c.location)) {
						return true;
					}
				}
				return false;
			}
		},
		RECESSION("Economic issues plague %REGIONNAME% and %NOBLENAME% seeks to take out a loan from the kingdom - build up a treasury of at least 140 gold to support them.", "By learning from your example (and taking out a small loan from your treasury), %NOBLENAME% has solved the financial worries in %REGIONNAME% and has stimulated the local economy.", "The economic issues in %REGIONNAME% have been resolved, but %NOBLENAME% has pledged to never let go of gold so easily again - not even to our tax collectors.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
				return w.getNation(r.getKingdom()).gold >= 140;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
				return w.getNation(r.getKingdom()).gold < 20;
			}
		},
		BANDITRY("Bow-wielding outlaws in %REGIONNAME% have been attacking merchants and agents of the nobility. %NOBLENAME% requests the presence of a sizeable army in the region to help eliminate them.", "With the aid of our troops, %NOBLENAME% has eliminated the bandit threat from %REGIONNAME%, and has established a personal police to ensure the region remains secure.", "The bandits in %REGIONNAME% have been legitimized by the %NOBLENAME%, in a deal to avoid future incidents. Regrettably, in so doing %REGIONNAME% has become a gathering place of undesireables.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
				for (Army a : w.armies) if (a.isArmy() && a.calcStrength(w, leaders.get(a), inspires, lastStands.contains(a.kingdom)) > r.calcMinPatrolStrength(w) && a.location == w.regions.indexOf(r) && a.kingdom.equals(r.getKingdom())) return true;
				return false;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
				double troopsInRegion = 0;
				for (Army a : w.armies) if (a.isArmy() && a.kingdom.equals(r.getKingdom()) && a.location == w.regions.indexOf(r)) troopsInRegion += a.size;
				return troopsInRegion < 1000;
			}
		},
		BORDER("%NOBLENAME%, the noble ruling %REGIONNAME%, has become deeply concerned with the neighboring enemy region, and requests that you deal with the situation one way or another.", "%NOBLENAME% has capitalized on the new security of the borders of %REGIONNAME%, citing this as an example of the glorious purpose of our armies to potential recruits.", "%NOBLENAME% has dealt with their fears by building up a large personal guard. Unfortunately, they take the best of %REGIONNAME% recruits, leaving the kingdom with only the bottom quality.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
				for (Region n : r.getNeighbors(w)) if (n.getKingdom() != null && NationData.isEnemy(r.getKingdom(), n.getKingdom(), w)) return false;
				return true;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
				for (Region n : r.getNeighbors(w)) if (n.getKingdom() != null && NationData.isEnemy(r.getKingdom(), n.getKingdom(), w)) return true;
				return false;
			}
		},
		ENNUI("%NOBLENAME% has lost the luster of life and sinks into deep depressions. Implement generous rationing to show them the joy in life.", "%NOBLENAME% was refreshed by witnessing the feasting and jubilation in %REGIONNAME%, and has discovered a new joy in generousity.", "%NOBLENAME% has found purpose in their work, but regrettably demands everyone else in %REGIONNAME% work just as tirelessly.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
				return rationing.getOrDefault(r.getKingdom(), 1.0) > 1;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
				return true;
			}
		},
		CULTISM("%NOBLENAME% has written you, warning that the Cult is especially active in %REGIONNAME% and is gradually taking power there. They suggest that construction of a temple might turn the people back toward a safer religion.", "With people flocking to the new temple in %REGIONNAME%, %NOBLENAME% has been able to turn them away from assisting the Cult.", "%NOBLENAME% has dealt with the cultists by agreeing to grant them access to the section of %REGIONNAME% they desire.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
				return templeBuilds.contains(r);
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
				return true;
			}
		},
		OVERWHELMED("%NOBLENAME% can't keep up with the stresses of managing a %REGIONNAME% and has asked for one of our agents to assist by governing it for a week.", "Assisted by our governance of %REGIONNAME%, %NOBLENAME% has gotten back on their feet - and picked up a trick or two!", "%NOBLENAME% has resolved their troubles %REGIONNAME% but acquired a habit of accepting wastefulness.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
				return governors.containsKey(r);
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
				Set<Integer> closeRegions = r.getCloseRegionIds(w, rules.nobleCrisisFrequency);
				for (Character c : w.characters) {
					if (c.kingdom.equals(r.getKingdom()) && closeRegions.contains(c.location)) {
						return true;
					}
				}
				return false;
			}
		},
		UPRISING("The rampant popular unrest %REGIONNAME% has triggered numerous incidents and %NOBLENAME% can no longer deal with it on their own. They request the nation take some action, urgently, to decrease popular unrest back to manageable levels.", "As unrest settles in %REGIONNAME%, %NOBLENAME% has made a name for themself among the people, listening to concerns and addressing sources of conflict.", "The uprising in %REGIONNAME% came to a head this week when the dissidents stormed the home of %NOBLENAME% and slew almost all the inhabitants. In grief and rage, %NOBLENAME% retaliated in kind, wiping out the rebels and their families, and vows to never let this repeat.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
				return r.unrestPopular <= .5;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
				return r.unrestPopular > .5;
			}
		},
		STARVATION("%NOBLENAME% feels deeply for their subjects in %REGIONNAME% and has asked the nation to fix the starvation situation immediately.", "With the immediate starvation in %REGIONNAME% addressed, %NOBLENAME% has made reforms in how food is handled or wasted to help ensure that starvation does not become a problem again.", "Faced with rampant starvation in %REGIONNAME%, %NOBLENAME% has despaired of help from our kingdom and solicited other rulers to take over. We should no longer trust the region's natural defenses.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
				return r.food > 0;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
				return r.food == 0;
			}
		},
		GUILD("%NOBLENAME% has notified us that the guilds forming in %REGIONNAME% are becoming economic powerhouses, threatening to cut out the nobility altogether. They request we construct an improvement in %REGIONNAME% as part of a plan to out-compete the guilds.", "By clever hiring of persons to fill our construction order, %NOBLENAME% has given the guilds in %REGIONNAME% a reputation of ineffectiveness and curtailed their growth. They have promised to subsidize future constructions in the region as well.", "%NOBLENAME% has dealt with the guilds in %REGIONNAME% by personally financing a trade war against them. Although successful, they are now thoroughly broke and attempting to rebuild their wealth by high permitting costs. We can expect any construction in the region to be more expensive.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
				return builds.contains(r);
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires) {
				return true;
			}
		};

		private final String startMessage;
		private final String successMessage;
		private final String failMessage;

		abstract boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires);
		abstract boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, Set<String> lastStands, int inspires);

		String getStartMessage(String nobleName, String regionName) {
			return format(startMessage, nobleName, regionName);
		}

		String getSuccessMessage(String nobleName, String regionName) {
			return format(successMessage, nobleName, regionName);
		}

		String getFailMessage(String nobleName, String regionName) {
			return format(failMessage, nobleName, regionName);
		}

		private static String format(String message, String nobleName, String regionName) {
			return message.replace("%NOBLENAME%", nobleName).replace("%REGIONNAME%", regionName);
		}

		Type(String startMessage, String successMessage, String failMessage) {
			this.startMessage = startMessage;
			this.successMessage = successMessage;
			this.failMessage = failMessage;
		}
	}
}
