package com.empire;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

class Noble extends RulesObject {
	String name;
	Crisis crisis;
	Percentage unrest;
	private double experience;

	static Noble newNoble(Rules rules) {
		return new Noble(rules);
	}

	static Noble newNoble(Culture culture, int date, Rules rules) {
		Noble n = newNoble(rules);
		n.name = WorldConstantData.getRandomName(culture, Math.random() < 0.5 ? WorldConstantData.Gender.MAN : WorldConstantData.Gender.WOMAN);
		n.unrest = new Percentage(0);
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
		return calcLevel() * getRules().nobleRecruitModPerLevel;
	}

	double calcTaxMod() {
		return calcLevel() * getRules().nobleTaxModPerLevel;
	}

	double calcPosthumousSpyRingStrength() {
		return experience;
	}

	void addExperience(boolean aristocraticNation) {
		experience++;
		if (aristocraticNation) experience += 0.5;
	}

	/**
	 * Checks the noble's crisis to see if it has been resolved or deadlined, and applies those effects.
	 * @return a notification that should be added to the world.
	 */	
	Optional<Notification> resolveCrisis(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
		if (crisis.type.isSolved(w, r, leaders, governors, builds, templeBuilds, rationing, inspires)) {
			crisis.type = Crisis.Type.NONE;
			unrest.add(getRules().nobleCrisisSuccessUnrest);
			experience += getRules().nobleCrisisSuccessExperience;
			return Optional.of(new Notification(r.getKingdom(), "Noble Crisis Resolved", crisis.type.getSuccessMessage(name, r.name)));
		} else if (crisis.type != Crisis.Type.NONE && crisis.deadline == w.date) {
			unrest.add(getRules().nobleCrisisFailedUnrest);
			return Optional.of(new Notification(r.getKingdom(), "Noble Crisis Expired", crisis.type.getFailMessage(name, r.name)));
		}
		return Optional.empty();
	}

	/**
	 * Creates a new crisis if necessary.
	 * @return a notification that should be added to the world.
	 */
	Optional<Notification> createCrisis(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
		if (crisis.deadline > w.date) return Optional.empty();
		List<Crisis.Type> possibleCrises = new ArrayList<>();
		for (Crisis.Type type : Crisis.Type.values()) {
			if (!type.isCreateable(w, r, getRules(), leaders, governors, builds, templeBuilds, rationing, inspires)) continue;
			if (type.isSolved(w, r, leaders, governors, builds, templeBuilds, rationing, inspires)) continue;
			possibleCrises.add(type);
		}
		if (possibleCrises.isEmpty()) {
			crisis.type = Crisis.Type.NONE;
			crisis.deadline = w.date + 1;
			return Optional.empty();
		} else {
			crisis.deadline = w.date + 6;
			crisis.type = possibleCrises.get((int)(Math.random() * possibleCrises.size()));
			return Optional.of(new Notification(r.getKingdom(), "Noble Crisis", crisis.type.getStartMessage(name, r.name)));
		}
	}

	private Noble(Rules rules) {
		super(rules);
	}
}

final class Crisis {
	Type type;
	String targetCharacter;
	int targetRegion;
	int deadline;

	enum Type {
		NONE("", "", "") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				return false;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				return false;
			}
		},
		CONQUEST("%NOBLENAME% is concerned that your nation is too small. Grow it to at least nine regions.", "%NOBLENAME% is pleased that your nation has extensive borders.", "%NOBLENAME% is upset that your nation remains too small.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				return w.regions.stream().filter(rr -> r.getKingdom().equals(rr.getKingdom())).count() >= 9;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				Nation n = w.getNation(r.getKingdom());
				return n.hasTag(Nation.Tag.REBELLIOUS) || n.hasTag(Nation.Tag.WARLIKE);
			}
		},
		FAITH("%NOBLENAME% is concerned that the people of %REGIONNAME% do not follow your state ideology. Match your state ideology and the regional ideology to make them happy.", "%NOBLENAME% is pleased that the people of %REGIONNAME% now follow your state ideology.", "%NOBLENAME% is upset that the people of %REGIONNAME% still do not follow your state ideology.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				return Nation.getStateReligion(r.getKingdom(), w) == r.religion;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				Nation n = w.getNation(r.getKingdom());
				if (!n.hasTag(Nation.Tag.EVANGELICAL) && !n.hasTag(Nation.Tag.HOLY) && !n.hasTag(Nation.Tag.MYSTICAL)) return false;
				if (Nation.getStateReligion(r.getKingdom(), w) != Ideology.TAPESTRY_OF_PEOPLE) return false;
				return true;
			}
		},
		FORTIFY("%NOBLENAME% is concerned that %REGIONNAME% does not have sufficient fortifications. Raise the fortification multiplier of the region to 175%.", "%NOBLENAME% is pleased that you have taken their advice and raised the fortifications of %REGIONNAME%.", "%NOBLENAME% is upset that you have ignored their advice to build fortifications in %REGIONNAME%.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				return r.calcFortificationPct() >= 1.75;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				Nation n = w.getNation(r.getKingdom());
				return n.hasTag(Nation.Tag.DEFENSIVE) || n.hasTag(Nation.Tag.STOIC);
			}
		},
		GROW("%NOBLENAME% wishes to welcome more people to %REGIONNAME%, and asks you to grow its population to 200k.", "%NOBLENAME% is very pleased that %REGIONNAME% is bursting with people.", "%NOBLENAME% is upset that %REGIONNAME% has not had its population increased.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				return r.population >= 200000;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				return w.getNation(r.getKingdom()).hasTag(Nation.Tag.WELCOMING);
			}
		},
		INSPIRE("%NOBLENAME% is dismayed that not a single cardinal of Iruhan is inspiring the faithful in the holy city. Make sure at least one does so soon.", "%NOBLENAME% is pleased that a cardinal of Iruhan is inspiring the faithful.", "%NOBLENAME% is upset that no cardinal of Iruhan has yet inspired the faithful.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				return inspires > 0;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				Nation n = w.getNation(r.getKingdom());
				if (!n.hasTag(Nation.Tag.HOLY)) return false;
				return true;
			}
		},
		NAVY("%NOBLENAME% is concerned that %REGIONNAME% does not contribute to our great navy, and wants it to host at least two shipyards.", "%NOBLENAME% is pleased that you have taken their advice and constructed shipyards in %REGIONNAME%.", "%NOBLENAME% is upset that you have ignored their advice to build shipyards in %REGIONNAME%.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				return r.constructions.stream().filter(c -> c.type == Construction.Type.SHIPYARD).count() >= 2;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				Nation n = w.getNation(r.getKingdom());
				return n.hasTag(Nation.Tag.SEAFARING) || n.hasTag(Nation.Tag.SHIP_BUILDING) || n.hasTag(Nation.Tag.INDUSTRIAL);
			}
		},
		NOBILITY("%NOBLENAME% of %REGIONNAME% has many friends who they believe ought to be raised to the nobility. Install nobles in all regions under your control.", "%NOBLENAME% is pleased that you have installed their friends as nobles.", "%NOBLENAME% is upset that you have failed to install additional nobles in your lands.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				return w.regions.stream().filter(rr -> rr.getKingdom() == r.getKingdom() && !rr.hasNoble()).count() == 0;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				return w.getNation(r.getKingdom()).hasTag(Nation.Tag.ARISTOCRATIC);
			}
		},
		PARADE("%NOBLENAME% wishes one of your heroic characters to visit %REGIONNAME% so that they may be thrown a parade!", "%NOBLENAME%'s parade was a huge success, and they are pleased that you made it possible.", "%NOBLENAME% is upset that %REGIONNAME% has not been visited by any heroic character.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				for (Character c : w.characters) if (c.kingdom.equals(r.getKingdom()) && c.location == w.regions.indexOf(r)) return true;
				return false;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				return w.getNation(r.getKingdom()).hasTag(Nation.Tag.HEROIC);
			}
		},
		PATROL("%NOBLENAME% is concerned about banditry and emigration, and asks you to position a force capable of patrolling the region in %REGIONNAME%.", "%NOBLENAME% is pleased that you have taken their advice and positioned forces to patrol %REGIONNAME%.", "%NOBLENAME% is upset that you have ignored their advice to bring troops to %REGIONNAME%.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				for (Army a : w.armies) if (a.isArmy() && a.calcStrength(w, leaders.get(a), inspires) > r.calcMinPatrolStrength(w) && a.location == w.regions.indexOf(r) && a.kingdom.equals(r.getKingdom())) return true;
				return false;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				Nation n = w.getNation(r.getKingdom());
				return n.hasTag(Nation.Tag.DISCIPLINED) || n.hasTag(Nation.Tag.PATRIOTIC);
			}
		},
		RECESSION("%NOBLENAME% of %REGIONNAME% is concerned about the national treasury, and advises building it up to at least 140 gold.", "%NOBLENAME% is pleased that you have taken their advice and increased your national treasury.", "%NOBLENAME% is upset that you have ignored their advice to increase the treasury.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				return w.getNation(r.getKingdom()).gold >= 200;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				Nation n = w.getNation(r.getKingdom());
				return n.hasTag(Nation.Tag.MERCANTILE) || n.hasTag(Nation.Tag.IMPERIALISTIC);
			}
		},
		SPY("%NOBLENAME% thinks that you should have a spy ring in %REGIONNAME%.", "%NOBLENAME% is pleased that you have established a spy ring in %REGIONNAME%, just as they planned...", "%NOBLENAME% is upset that %REGIONNAME% still does not have a spy ring.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				int rid = w.regions.indexOf(r);
				return w.spyRings.stream().filter(s -> s.getLocation() == rid && s.getNation() == r.getKingdom()).count() > 0;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				return w.getNation(r.getKingdom()).hasTag(Nation.Tag.SNEAKY);
			}
		},
		UPRISING("The rampant popular unrest %REGIONNAME% has triggered numerous incidents and %NOBLENAME% can no longer deal with it on their own. They request the nation take some action, urgently, to decrease popular unrest back to manageable levels.", "As unrest settles in %REGIONNAME%, %NOBLENAME% is thankful for your help.", "The uprising in %REGIONNAME% came to a head this week when the dissidents stormed the home of %NOBLENAME% and slew almost all the inhabitants. In grief and rage, %NOBLENAME% blames you for letting things get this bad.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				return r.unrestPopular.get() <= .5;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				return r.unrestPopular.get() > .5;
			}
		},
		WEDDING("%NOBLENAME%'s daughter is being wed in %REGIONNAME% and they request your presence as ruler.", "The wedding of the daughter of %NOBLENAME% was a spectacular affair. It is rare for the nobility to find much joy in their political marriage, but in this case the couple's obvious love for one another warmed your heart to witness. %NOBLENAME% could not thank you enough for attending, and swore never to forget this day.", "The wedding %NOBLENAME% invited you to in %REGIONNAME% has taken place without you and %NOBLENAME% is very cross.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				for (Character c : w.characters) if (c.hasTag(Character.Tag.RULER) && c.kingdom.equals(r.getKingdom()) && c.location == w.regions.indexOf(r)) return true;
				return false;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				Set<Integer> closeRegions = r.getCloseRegionIds(w, rules.nobleCrisisFrequency);
				for (Character c : w.characters) {
					if (c.kingdom.equals(r.getKingdom()) && c.hasTag(Character.Tag.RULER) && closeRegions.contains(c.location)) {
						return true;
					}
				}
				return false;
			}
		},
		WORSHIP("%NOBLENAME% is concerned that the people of %REGIONNAME% do not have enough temples to attend. Make sure there are at least three temples in %REGIONNAME%.", "%NOBLENAME% is pleased that the people of %REGIONNAME% now have ample places to worship.", "%NOBLENAME% is upset that the people of %REGIONNAME% still are lacking in temples.") {
			@Override
			boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				return r.constructions.stream().filter(c -> c.type == Construction.Type.TEMPLE).count() >= 3;
			}

			@Override
			boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires) {
				Nation n = w.getNation(r.getKingdom());
				if (!n.hasTag(Nation.Tag.EVANGELICAL) && !n.hasTag(Nation.Tag.HOLY) && !n.hasTag(Nation.Tag.MYSTICAL)) return false;
				return true;
			}
		};

		private final String startMessage;
		private final String successMessage;
		private final String failMessage;

		abstract boolean isSolved(World w, Region r, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires);
		abstract boolean isCreateable(World w, Region r, Rules rules, Map<Army, Character> leaders, Map<Region, Character> governors, Set<Region> builds, Set<Region> templeBuilds, Map<String, Double> rationing, int inspires);

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
