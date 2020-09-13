package com.empire;

import com.empire.util.StringUtil;
import com.google.common.collect.HashMultiset;
import com.google.common.primitives.Ints;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

interface GoodwillProvider {
	double getGoodwill(String nation);
}

public class World extends RulesObject implements GoodwillProvider {
	static final String TYPE = "World";
	private static final Logger log = Logger.getLogger(World.class.getName());

	int date;
	private Map<String, Nation> kingdoms = new HashMap<>();
	List<Region> regions = new ArrayList<>();
	List<Army> armies = new ArrayList<>();
	List<Character> characters = new ArrayList<>();
	List<Communication> communications = new ArrayList<>();
	List<Plot> plots = new ArrayList<>();
	List<SpyRing> spyRings = new ArrayList<>();
	Pirate pirate = new Pirate();
	Tivar tivar = new Tivar();
	List<Notification> notifications = new ArrayList<>();
	List<Integer> cultRegions = new ArrayList<>();
	Church church = new Church();
	Schedule turnSchedule = new Schedule();
	List<CultCache> cultCaches = new ArrayList<>();
	boolean cultTriggered;
	int inspiresHint;
	int ruleSet;
	int numPlayers;
	long nextTurn;
	boolean gameover;
	String filteredTo = null; // This should never be serialized to the backend, only to the client.

	public int getRuleSet() {
		return ruleSet;
	}

	public int getNumPlayers() {
		return numPlayers;
	}

	public int getDate() {
		return date;
	}

	public List<Character> getCharacters() {
		return characters;
	}

	public long getNextTurn() {
		return nextTurn;
	}

	public void setNextTurn(long nextTurn) {
		this.nextTurn = nextTurn;
	}

	public boolean isGameover() {
		return gameover;
	}

	public List<SpyRing> getSpyRings() {
		return spyRings;
	}

	public Optional<Character> getCharacterByName(String name) {
		for (Character c : characters) if (c.name.equals(name)) return Optional.of(c);
		return Optional.empty();
	}

	public Optional<Character> getRuler(String kingdom) {
		for (Character c : characters) if (c.hasTag(Character.Tag.RULER) && c.kingdom.equals(kingdom)) return Optional.of(c);
		return Optional.empty();
	}

	private transient Geography geography;


	private static Gson getGson(Rules rules) {
		InstanceCreator<World> icw = unused -> World.newWorld(rules);
		InstanceCreator<Region> icr = unused -> Region.newRegion(rules);
		InstanceCreator<Character> icc = unused -> Character.newCharacter(rules);
		InstanceCreator<Army> ica = unused -> Army.newArmy(rules);
		InstanceCreator<Noble> icn = unused -> Noble.newNoble(rules);
		InstanceCreator<SpyRing> icsr = unused -> SpyRing.newSpyRing(rules);
		InstanceCreator<Plot> icp = unused -> Plot.newPlot(rules);

		return new GsonBuilder()
				.enableComplexMapKeySerialization()
				.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
				.registerTypeAdapter(World.class, icw)
				.registerTypeAdapter(Region.class, icr)
				.registerTypeAdapter(Character.class, icc)
				.registerTypeAdapter(Army.class, ica)
				.registerTypeAdapter(Noble.class, icn)
				.registerTypeAdapter(SpyRing.class, icsr)
				.registerTypeAdapter(Plot.class, icp)
				.registerTypeAdapter(Percentage.class, new Percentage.TypeAdapter())
				.create();
	}

	public static World fromJson(String json) throws IOException {
		RuleSet set = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().fromJson(json, RuleSet.class);
		World w = getGson(Rules.loadRules(set.ruleSet)).fromJson(json, World.class);
		w.geography = Geography.loadGeography(w.ruleSet, w.numPlayers);
		return w;
	}

	private static class RuleSet { int ruleSet; };

	public static World startNew(Lobby lobby) throws IOException {
		Map<String, NationSetup> nationSetup = lobby.getNations();
		World w = newWorld(Rules.loadRules(lobby.getRuleSet()));
		w.ruleSet = lobby.getRuleSet();
		w.numPlayers = lobby.getNumPlayers();
		w.geography = Geography.loadGeography(w.ruleSet, w.numPlayers);
		w.date = 1;
		w.turnSchedule = lobby.getSchedule();
		w.nextTurn = w.turnSchedule.getNextTimeFirstTurn();
		w.regions = new ArrayList<>();
		w.pirate.threat = 4;
		w.church.setDoctrine(Church.Doctrine.ANTIAPOSTASY);
		w.church.setDoctrine(Church.Doctrine.ANTIECUMENISM);
		w.church.setDoctrine(Church.Doctrine.ANTISCHISMATICISM);
		w.church.setDoctrine(Church.Doctrine.ANTITERRORISM);
		w.church.setDoctrine(Church.Doctrine.DEFENDERS_OF_FAITH);
		w.church.setDoctrine(Church.Doctrine.WORKS_OF_IRUHAN);
		// Set up regions (culture, popular unrest).
		for (Geography.Region r : w.geography.regions) {
			Region rr = Region.newRegion(w.getRules());
			rr.id = w.regions.size();
			rr.type = r.type;
			rr.name = r.name;
			if (rr.isLand()) {
				rr.culture = w.geography.kingdoms.get(r.core).culture;
				rr.unrestPopular = new Percentage(0.12);
			}
			w.regions.add(rr);
		}
		while (w.cultRegions.size() < 10) {
			int rid = (int)(Math.random() * w.regions.size());
			while (w.regions.get(rid).isSea() || w.cultRegions.contains(rid)) rid = (int)(Math.random() * w.regions.size());
			w.cultRegions.add(rid);
		}
		double totalPopulation = w.numPlayers * 1000000;
		double totalFood = totalPopulation * 10;
		double totalNavy = totalPopulation / 26000 * 3;
		double totalArmy = totalPopulation / 26000 * 100;
		double totalGold = (totalPopulation / 10000 - totalNavy / 3 - totalArmy / 100) * 20;
		double totalSharesGold = 0;
		double totalSharesArmy = 0;
		double totalSharesNavy = 0;
		int unruledNations = w.geography.kingdoms.size() - nationSetup.keySet().size();
		double totalSharesFood = unruledNations / 2.0;
		double totalSharesPopulation = unruledNations / 2.0;
		for (String kingdom : nationSetup.keySet()) {
			NationSetup setup = nationSetup.get(kingdom);
			totalSharesGold += 1;
			totalSharesArmy += 1;
			totalSharesNavy += 1;
			totalSharesFood += 1;
			totalSharesPopulation += 1;
			if (setup.hasTag(Nation.Tag.MERCANTILE)) totalSharesGold += 0.5;
			if (setup.hasTag(Nation.Tag.PATRIOTIC)) totalSharesArmy += 0.15;
			if (setup.hasTag(Nation.Tag.REBELLIOUS)) totalSharesArmy += 0.5;
			if (setup.hasTag(Nation.Tag.REBELLIOUS)) totalSharesGold += 5;
			if (setup.hasTag(Nation.Tag.SEAFARING)) totalSharesNavy += 0.15;
			if (setup.hasTag(Nation.Tag.WARLIKE)) totalSharesArmy += 0.15;
			if (setup.hasTag(Nation.Tag.WELCOMING)) totalSharesPopulation += 0.1;
			if ("food".equals(setup.bonus)) totalSharesFood += 0.5;
			else if ("armies".equals(setup.bonus)) totalSharesArmy += 0.5;
			else if ("navies".equals(setup.bonus)) totalSharesNavy += 0.5;
			else if ("gold".equals(setup.bonus)) totalSharesGold += 1.5;
		}
		HashSet<String> regionNames = new HashSet<>();
		for (Region r : w.regions) regionNames.add(r.name);
		for (String kingdom : nationSetup.keySet()) {
			NationSetup setup = nationSetup.get(kingdom);
			Nation nation = new Nation();
			nation.email = setup.email;
			nation.culture = w.geography.getKingdom(kingdom).culture;
			nation.coreRegions = new ArrayList<>();
			for (int i = 0; i < w.geography.regions.size(); i++) if (w.geography.regions.get(i).core == w.geography.getKingdomId(kingdom)) nation.coreRegions.add(i);
			for (int i = 0; i < nation.coreRegions.size(); i++) {
				if (setup.regionNames.size() > i) {
					String rename = StringUtil.sanitizeName(setup.regionNames.get(i));
					if (rename != null && rename.length() > 0 && !regionNames.contains(rename)) {
						w.regions.get(nation.coreRegions.get(i)).name = rename;
						regionNames.add(rename);
					}
				}
			}
			nation.goodwill = setup.hasTag(Nation.Tag.HOLY) ? 15 : 5;
			for (Nation.Gothi g : Nation.Gothi.values()) nation.gothi.put(g, false);
			for (Nation.Tag trait : setup.traits) nation.addTag(trait);
			for (Nation.ScoreProfile profile : Nation.ScoreProfile.values()) {
				if (setup.hasScoreProfile(profile)) nation.addProfile(profile);
			}
			for (String k2 : nationSetup.keySet()) {
				if (k2.equals(kingdom)) continue;
				Relationship r = new Relationship();
				r.battle = Relationship.War.NEUTRAL;
				r.refugees = Relationship.Refugees.ACCEPT;
				r.tribute = 0;
				r.construct = Relationship.Construct.FORBID;
				r.cede = Relationship.Cede.ACCEPT;
				r.fealty = Relationship.Fealty.REFUSE;
				nation.setRelationship(k2, r);
			}
			double sharesGold = 1;
			double sharesFood = 1;
			double sharesPopulation = 1;
			if (setup.hasTag(Nation.Tag.MERCANTILE)) sharesGold += 0.5;
			if (setup.hasTag(Nation.Tag.REBELLIOUS)) sharesGold += 5;
			if (setup.hasTag(Nation.Tag.WELCOMING)) sharesPopulation += 0.1;
			if ("food".equals(setup.bonus)) sharesFood += 0.5;
			else if ("gold".equals(setup.bonus)) sharesGold += 0.5;
			double population = totalPopulation * sharesPopulation / totalSharesPopulation;
			double food = totalFood * sharesFood / totalSharesFood;
			nation.gold = totalGold * sharesGold / totalSharesGold;
			int remaining = nation.coreRegions.size();
			double remainingShare = remaining;
			for (Integer r : nation.coreRegions) {
				double flexFactor = remaining == 1 ? remainingShare : Math.min(remainingShare / 2, Math.random() * .4 + .8);
				remainingShare -= flexFactor;
				w.regions.get(r).population = population * flexFactor / nation.coreRegions.size();
				w.regions.get(r).crops = w.regions.get(r).population * w.getRules().setupCropsPerCitizen;
				w.regions.get(r).food = food * flexFactor / nation.coreRegions.size();
				w.regions.get(r).setKingdomNoScore(kingdom);
				remaining--;
			}
			w.kingdoms.put(kingdom, nation);
		}
		// Give unowned regions population and such.
		int unownedRegions = 0;
		for (Region r : w.regions) {
			if (r.isSea()) continue;
			if (r.getKingdom() != null) continue;
			unownedRegions++;
		}
		for (Region r : w.regions) {
			if (r.isSea()) continue;
			if (r.getKingdom() == null) {
				r.setKingdomNoScore("Unruled");
				r.population = totalPopulation / unownedRegions / totalSharesPopulation * unruledNations / 2.0;
				r.unrestPopular = new Percentage(Math.random() * .3 + .1);
				r.food = totalFood / unownedRegions / totalSharesPopulation * unruledNations / 2.0;
			}
		}
		// Un-own rebellious regions.
		HashSet<String> rebelliousNations = new HashSet<>();
		for (String kingdom : nationSetup.keySet()) {
			NationSetup setup = nationSetup.get(kingdom);
			if (!setup.hasTag(Nation.Tag.REBELLIOUS)) continue;
			rebelliousNations.add(kingdom);
			ArrayList<Region> ownedRegions = new ArrayList<>();
			for (Region r : w.regions) if (kingdom.equals(r.getKingdom())) ownedRegions.add(r);
			// Remove a random coastal region. This will be the player's only starting region.
			Collections.shuffle(ownedRegions);
			for (Region r : ownedRegions) {
				if (r.isCoastal(w)) {
					ownedRegions.remove(r);
					break;
				}
			}
			for (Region r : ownedRegions) r.setKingdomNoScore(null);
		}
		// Allocate nobles as necessary.
		for (String kingdom : nationSetup.keySet()) {
			NationSetup setup = nationSetup.get(kingdom);
			Culture culture = w.geography.getKingdom(kingdom).culture;
			if (setup.hasTag(Nation.Tag.REPUBLICAN)) continue;
			ArrayList<Integer> ownedRegions = new ArrayList<>();
			for (int i = 0; i < w.regions.size(); i++) if (kingdom.equals(w.regions.get(i).getKingdom())) ownedRegions.add(i);
			Collections.shuffle(ownedRegions);
			int placements = (int) Math.ceil(ownedRegions.size() * culture.nobleFraction.apply(w.getRules()));
			if (setup.hasTag(Nation.Tag.ARISTOCRATIC)) placements = ownedRegions.size();
			for (int i = 0; i < placements; i++) {
				w.regions.get(ownedRegions.get(i)).noble = Noble.newNoble(culture, 0, w.getRules());
			}
		}
		// Place shipyards.
		for (String kingdom : nationSetup.keySet()) {
			ArrayList<Region> regions = new ArrayList<>();
			for (Region r : w.regions) if (kingdom.equals(r.getKingdom()) && r.isCoastal(w)) regions.add(r);
			if (!regions.isEmpty()) {
				for (int i = 0; i < w.getRules().setupShipyardsPerNation; i++) {
					regions.get((int)(Math.random() * regions.size())).constructions.add(Construction.makeShipyard(w.getRules().baseCostShipyard));
				}
			}
		}
		// Place spy rings.
		for (String kingdom : nationSetup.keySet()) {
			List<Integer> candidates = new ArrayList<>();
			for (int i = 0; i < w.regions.size(); i++) if (kingdom.equals(w.regions.get(i).getKingdom())) candidates.add(i);
			w.spyRings.add(SpyRing.newSpyRing(w.getRules(), kingdom, w.getRules().setupSpyRingStrength, candidates.get((int)(Math.random() * candidates.size()))));
			if (nationSetup.get(kingdom).hasTag(Nation.Tag.SNEAKY)) {
				int num = 2;
				while (num > 0) {
					int loc = (int)(Math.random() * w.regions.size());
					if (!w.regions.get(loc).isLand()) continue;
					if (w.spyRings.stream().filter(s -> s.getNation().equals(kingdom) && s.getLocation() == loc).count() > 0) continue;
					w.spyRings.add(SpyRing.newSpyRing(w.getRules(), kingdom, w.getRules().setupSpyRingStrength, candidates.get(loc)));
					num--;
				}
			}
		}
		// Add characters, incl Cardinals
		HashSet<String> characterNames = new HashSet<String>();
		characterNames.add("");
		for (String kingdom : nationSetup.keySet()) {
			NationSetup setup = nationSetup.get(kingdom);
			ArrayList<Integer> regions = new ArrayList<>();
			for (int i = 0; i < w.regions.size(); i++) if (kingdom.equals(w.regions.get(i).getKingdom())) regions.add(i);
			ArrayList<Character> characters = new ArrayList<>();
			int numCharacters = 4;
			boolean cardinal = false;
			if (setup.dominantIdeology.religion == Religion.IRUHAN && setup.dominantIdeology != Ideology.VESSEL_OF_FAITH) {
				numCharacters++;
				cardinal = true;
			}
			if (setup.hasTag(Nation.Tag.HEROIC)) numCharacters += 2;
			if (setup.hasTag(Nation.Tag.REPUBLICAN)) numCharacters += 1;
			for (int i = 0; i < numCharacters; i++) {
				Character c = Character.newCharacter(w.getRules());
				c.kingdom = kingdom;
				if (setup.characters.size() > i) {
					NationSetup.CharacterSetup csetup = setup.characters.get(i);
					c.name = StringUtil.sanitizeName(csetup.name);
					c.portrait = csetup.portrait;
					for (int j = 0; j < 3; j++) {
						if (csetup.skill == NationSetup.CharacterSetup.Skill.GENERAL) c.addExperienceGeneral();
						else if (csetup.skill == NationSetup.CharacterSetup.Skill.GOVERNOR) c.addExperienceGovernor();
						else if (csetup.skill == NationSetup.CharacterSetup.Skill.ADMIRAL) c.addExperienceAdmiral();
						else c.addExperienceSpy();
					}
				} else {
					c.name = WorldConstantData.getRandomName(w.geography.getKingdom(kingdom).culture, Math.random() < 0.5 ? WorldConstantData.Gender.MAN : WorldConstantData.Gender.WOMAN);
					c.portrait = -1;
					for (int j = 0; j < 3; j++) c.addExperienceSpy();
				}
				// Rename if name collision.
				for (String suffix : new String[]{"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"}) {
					if (!characterNames.contains(c.name + suffix)) {
						c.name = c.name + suffix;
						break;
					}
				}
				while (characterNames.contains(c.name)) {
					c.name = WorldConstantData.getRandomName(w.geography.getKingdom(kingdom).culture, Math.random() < 0.5 ? WorldConstantData.Gender.MAN : WorldConstantData.Gender.WOMAN);
				}
				characterNames.add(c.name);
				if (i == 0) {
					c.honorific = setup.title;
					c.addTag(Character.Tag.RULER);
				}
				if (i == 4 && cardinal) c.addTag(Character.Tag.CARDINAL);
				c.location = regions.get((int)(Math.random() * regions.size()));
				c.orderhint = "Stay in " + w.regions.get(c.location).name;
				w.characters.add(c);
			}
		}
		// Allocate unowned regions between non-rebellious nations.
		int numUnownedRegions = 0;
		for (Region r : w.regions) {
			if (r.isSea()) continue;
			if (r.getKingdom() != null) continue;
			numUnownedRegions++;
		}
		while (numUnownedRegions > 0) {
			double totalWeight = 0;
			ArrayList<Geography.Border> borders = new ArrayList<>();
			// Find all borders between owned and unowned land regions.
			for (Geography.Border bo : w.geography.borders) {
				if (bo.b == null) continue;
				Region a = w.regions.get(bo.a);
				Region b = w.regions.get(bo.b);
				if ((a.getKingdom() == null && b.getKingdom() == null) || (a.getKingdom() != null && b.getKingdom() != null)) continue;
				if (rebelliousNations.contains(a.getKingdom()) || rebelliousNations.contains(b.getKingdom())) continue;
				if (!a.isLand() || !b.isLand()) continue;
				totalWeight += 1.0 / bo.w;
				borders.add(bo);
			}
			if (!borders.isEmpty()) {
				// Pick a random border weighted by 1/border-size.
				double v = Math.random() * totalWeight;
				for (Geography.Border b : borders) {
					if (b.b == null) continue;
					v -= 1.0 / b.w;
					if (v > 0) continue;
					Region ra = w.regions.get(b.a);
					Region rb = w.regions.get(b.b);
					// Give the region to that kingdom.
					if (ra.getKingdom() == null) ra.setKingdomNoScore(rb.getKingdom());
					else rb.setKingdomNoScore(ra.getKingdom());
					break;
				}
			} else {
				// If there are no borders, give the first unowned land region to a random non-rebellious kingdom that shares a sea.
				for (Region r : w.regions) {
					if (r.isSea()) continue;
					if (r.getKingdom() != null) continue;
					Set<String> eligibleNations = new HashSet<>();
					r.getNeighbors(w).stream().filter(rr -> rr.isSea()).forEach(sea -> sea.getNeighbors(w).stream().map(rr -> rr.getKingdom()).filter(k -> k != null).forEach(k -> eligibleNations.add(k)));
					eligibleNations.removeIf(k -> rebelliousNations.contains(k));
					if (eligibleNations.isEmpty()) {
						eligibleNations.addAll(w.kingdoms.keySet());
						eligibleNations.removeIf(k -> rebelliousNations.contains(k));
					}
					if (eligibleNations.isEmpty()) {
						numUnownedRegions = 0; // All nations are rebellious. Bail out.
					} else {
						List<String> choice = new ArrayList<>(eligibleNations);
						r.setKingdomNoScore(choice.get((int)(Math.random() * choice.size())));
					}
					break;
				}
			}
			numUnownedRegions--;
		}
		// Place armies, navies.
		for (String kingdom : nationSetup.keySet()) {
			NationSetup setup = nationSetup.get(kingdom);
			double sharesNavy = 1;
			double sharesArmy = 1;
			if (setup.hasTag(Nation.Tag.PATRIOTIC)) sharesArmy += 0.15;
			if (setup.hasTag(Nation.Tag.REBELLIOUS)) sharesArmy += 0.5;
			if (setup.hasTag(Nation.Tag.SEAFARING)) sharesNavy += 0.15;
			if (setup.hasTag(Nation.Tag.WARLIKE)) sharesArmy += 0.15;
			if ("armies".equals(setup.bonus)) sharesArmy += 0.5;
			else if ("navies".equals(setup.bonus)) sharesNavy += 0.5;
			double armies = totalArmy * sharesArmy / totalSharesArmy;
			double navies = totalNavy * sharesNavy / totalSharesNavy;
			int numRegions = 0;
			int numSeaBorders = 0;
			for (Region r : w.regions) if (kingdom.equals(r.getKingdom())) numRegions++;
			for (Region r : w.regions) {
				if (r.isSea()) for (Region rr : r.getNeighbors(w)) if (kingdom.equals(rr.getKingdom())) numSeaBorders++;
			}
			for (Region r : w.regions) {
				if (kingdom.equals(r.getKingdom())) {
					Army army = Army.newArmy(w.getRules());
					army.type = Army.Type.ARMY;
					army.size = armies / numRegions;
					army.tags.addAll(r.getArmyTags());
					army.location = w.regions.indexOf(r);
					army.id = w.getNewArmyId();
					army.kingdom = kingdom;
					army.composition.put("r_" + army.location, army.size);
					army.orderhint = "";
					w.armies.add(army);
				} else if (r.isSea()) {
					int borderCount = 0;
					for (Region rr : r.getNeighbors(w)) if (kingdom.equals(rr.getKingdom())) borderCount++;
					if (borderCount > 0) {
						// Place navy.
						Army army = Army.newArmy(w.getRules());
						army.type = Army.Type.NAVY;
						army.size = navies * borderCount / numSeaBorders;
						army.tags = new ArrayList<>();
						army.location = w.regions.indexOf(r);
						army.id = w.getNewArmyId();
						army.kingdom = kingdom;
						army.orderhint = "";
						w.armies.add(army);
					}
				}
			}
		}
		// Set religions for all regions to new owners.
		for (Region r : w.regions) {
			if (r.isSea()) continue;
			if (r.getKingdom() == null) throw new RuntimeException(r.name + " is still unowned!");
			if ("Unruled".equals(r.getKingdom())) {
				List<Ideology> possibilities = Ideology.getIdeologiesByReligion(r.culture.religion);
				r.religion = possibilities.get((int)(Math.random() * possibilities.size()));
			} else {
				r.religion = nationSetup.get(r.getKingdom()).dominantIdeology;
			}
		}
		// Place defensive fortifications.
		for (Region r : w.regions) {
			if (nationSetup.containsKey(r.getKingdom()) && nationSetup.get(r.getKingdom()).hasTag(Nation.Tag.DEFENSIVE)) {
				r.constructions.add(Construction.makeFortifications(w.getRules().baseCostFortifications));
				r.constructions.add(Construction.makeFortifications(w.getRules().baseCostFortifications));
			}
			if (nationSetup.containsKey(r.getKingdom()) && nationSetup.get(r.getKingdom()).hasTag(Nation.Tag.MYSTICAL)) {
				r.constructions.add(Construction.makeTemple(r.religion, w.getRules().baseCostTemple));
			}
		}
		return w;
	}

	public Nation getNation(String nation) {
		if (Nation.PIRATE_NAME.equals(nation)) return Nation.PIRATE;
		if (Nation.UNRULED_NAME.equals(nation)) return Nation.UNRULED;
		return kingdoms.get(nation);
	}

	public Set<String> getNationNames() {
		return kingdoms.keySet();
	}

	@Override
	public String toString() {
		try {
			return getGson(getRules()).toJson(this);
		} catch (IllegalArgumentException e) {
			log.log(
				Level.SEVERE,
				new GsonBuilder()
						.enableComplexMapKeySerialization()
						.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
						.serializeSpecialFloatingPointValues()
						.create().toJson(this));
			throw e;
		}
	}

	/**
	 * Advances the game state to the next turn.
	 * @return a map of e-mail notifications to send.
	 */
	public Map<String, String> advance(Map<String, Map<String, String>> orders) throws IOException {
		return new Advancer(orders).advance();
	}

	private static class Budget {
		double start;
		double incomeTax;
		double incomeIntrigue;
		double incomeSea;
		double incomeTribute;
		double incomeChurch;
		double incomeGift;
		double incomeArmyDelivery;
		double incomeShipSales;
		double spentTribute;
		double spentIntrigue;
		double spentSoldiers;
		double spentRecruits;
		double spentGift;
		double spentConstruction;
		double spentFoodTransfers;
		double spentBribes;
		double spentSpyEstablishments;

		public double sum() {
			return incomeTax + incomeIntrigue + incomeSea + incomeTribute + incomeChurch + incomeGift + incomeArmyDelivery + incomeShipSales - spentTribute - spentIntrigue - spentSoldiers - spentRecruits - spentConstruction - spentGift - spentBribes - spentFoodTransfers - spentSpyEstablishments;
		}
	}

	class Advancer {
		final Map<String, Map<String, String>> orders;
		HashMap<String, List<String>> tributes = new HashMap<>();
		HashMap<String, Budget> incomeSources = new HashMap<>();
		int inspires;
		HashMap<Army, Character> leaders = new HashMap<>();
		HashMap<String, Double> pirateThreatSources = new HashMap<>();
		HashMap<Army, Double> attritionLosses = new HashMap<>();
		HashSet<Region> builds = new HashSet<>();
		HashSet<Region> templeBuilds = new HashSet<>();
		HashMap<String, Double> rationing = new HashMap<String, Double>();
		HashMap<String, Double> taxationRates = new HashMap<String, Double>();
		Set<Region> patrolledRegions = new HashSet<>();

		Advancer(Map<String, Map<String, String>> orders) throws IOException {
			this.orders = orders;
		}

		Map<String, String> advance() {
			reset();
			synthesizeOrders();
			updateEconomyHints();
			deliverLetters();
			updateRelationships();
			transferGold();
			setGothiSpellStatus();
			setDefaultOrderHints();
			countInspires();
			splitArmies();
			markLeaders();
			mergeArmies();
			resolveIntrigue();
			doctrineChanges();
			orderOverrides();
			armyActionsNonTravel();
			armyActionsTravel();
			nobleActions();
			characterActions();
			destroyPiratesInAlyrjaRegions();
			spawnCultists();
			bribePirates();
			spawnPirates();
			joinBattles();
			captureNavies();
			miscPerTurnEffects();
			noblesRebel();
			churchOpinionChangesDueToStateReligion();
			selectTiecel();
			adjustUnrestDueToTaxation();
			transferFood();
			payTroops();
			recruitTroops();
			reapHarvests();
			cedeRegions();
			eatFood();
			growPopulations();
			evaluateGothiSpells();
			checkCultVictory();
			appointHeirs();
			deliverGoldFromArmies();
			gainIncome();
			gainChurchIncome();
			payTribute();
			applyIncome();
			removeInvalidPlots();
			nobleCrises();
			sendBudgetNotifications();
			notifyPirateThreats();
			notifyInspires();
			miscScoreProfiles();
			advanceDate();
			Map<String, String> emails = checkGameEnd();
			if (emails == null) {
				emails = prepareEmails();
			}
			return emails;
		}

		void reset() {
			notifications.clear();
		}

		void synthesizeOrders() {
			for (String k : kingdoms.keySet()) {
				if (!orders.containsKey(k)) {
					getNation(k).idleStrikes++;

					// AI: Snythesize orders from hints.
					HashMap<String, String> aiOrders = new HashMap<>();
					aiOrders.put("economy_tax", getNation(k).taxratehint);
					aiOrders.put("economy_ship", getNation(k).shipratehint);
					aiOrders.put("economy_ration", getNation(k).rationhint);
					aiOrders.put("economy_recruit_bonus", getNation(k).signingbonushint);
					for (Character c : characters) {
						if (k.equals(c.kingdom) && null != c.orderhint && !"".equals(c.orderhint)) aiOrders.put("action_" + c.name.replace(" ", "_").replace("'", "_"), c.orderhint);
					}
					for (Army a : armies) {
						if (k.equals(a.kingdom) && null != a.orderhint && !"".equals(a.orderhint)) aiOrders.put("action_army_" + a.id, a.orderhint);
					}
					for (String kk : kingdoms.keySet()) {
						if (k.equals(kk)) continue;
						aiOrders.put("rel_" + kk + "_attack", getNation(k).getRelationship(kk).battle.toString());
						aiOrders.put("rel_" + kk + "_tribute", Double.toString(getNation(k).getRelationship(kk).tribute));
						aiOrders.put("rel_" + kk + "_cede", getNation(k).getRelationship(kk).cede.toString());
						aiOrders.put("rel_" + kk + "_refugees", getNation(k).getRelationship(kk).refugees.toString());
						aiOrders.put("rel_" + kk + "_fealty", getNation(k).getRelationship(kk).fealty.toString());
						aiOrders.put("rel_" + kk + "_construct", getNation(k).getRelationship(kk).construct.toString());
					}

					// Experimental AI.
					if (getNation(k).idleStrikes >= 2) aiOrders.putAll(ArtificialIntelligence.getOrders(k, World.this));

					orders.put(k, aiOrders);
				} else {
					getNation(k).idleStrikes = 0;
				}
			}
		}

		void updateEconomyHints() {
			for (String k : kingdoms.keySet()) {
				Nation n = getNation(k);
				Map<String, String> no = orders.getOrDefault(k, new HashMap<String, String>());
				if (orders.containsKey(k)) {
					if (no.containsKey("send_email")) n.sendEmail = "checked".equals(no.get("send_email"));
					getNation(k).taxratehint = orders.getOrDefault(k, new HashMap<String, String>()).getOrDefault("economy_tax", "100");
					getNation(k).shipratehint = orders.getOrDefault(k, new HashMap<String, String>()).getOrDefault("economy_ship", "5");
					getNation(k).signingbonushint = orders.getOrDefault(k, new HashMap<String, String>()).getOrDefault("economy_recruit_bonus", "0");
					getNation(k).rationhint = orders.getOrDefault(k, new HashMap<String, String>()).getOrDefault("economy_ration", "100");
				}
				taxationRates.put(k, Double.parseDouble(orders.getOrDefault(k, new HashMap<String, String>()).getOrDefault("economy_tax", "100")) / 100);
			}
		}

		void deliverLetters() {
			for (String k : orders.keySet()) {
				Map<String, String> kOrders = orders.get(k);
				for (String o : kOrders.keySet()) {
					if (o.startsWith("letter_") && o.endsWith("_text") && !"".equals(kOrders.get(o))) {
						String prefix = o.substring(0, o.lastIndexOf("_"));
						Communication c = new Communication();
						c.from = k;
						c.postDate = date;
						c.text = kOrders.get(prefix + "_greeting") + "\n\n" + kOrders.get(prefix + "_text");
						String signature = kOrders.get(prefix + "_sig");
						if (signature.contains(k)) {
							c.signed = signature;
						} else {
							c.signed = "Anonymous";
						}
						for (String kk : kingdoms.keySet()) if ("checked".equals(kOrders.get(prefix + "_to_" + kk))) c.to.add(kk);
						communications.add(c);
					}
				}
			}
		}

		void updateRelationships() {
			ArrayList<String> changes = new ArrayList<>();
			for (String k : orders.keySet()) {
				Map<String, String> kOrders = orders.get(k);
				// If a nation's total outgoing tribute exceeds 1.0, weight it by shares and split it.
				double totalTribute = 0;
				for (String kk : kingdoms.keySet()) {
					if (kk.equals(k)) continue;
					totalTribute += Math.max(0, Math.min(1, Double.parseDouble(kOrders.getOrDefault("rel_" + kk + "_tribute", "0"))));
				}
				if (totalTribute < 1) totalTribute = 1;
				for (String kk : kingdoms.keySet()) {
					if (k.equals(kk)) continue;
					Relationship r = getNation(k).getRelationship(kk);
					Relationship old = new Relationship(r);
					r.battle = Relationship.War.valueOf(kOrders.getOrDefault("rel_" + kk + "_attack", "NEUTRAL"));
					r.refugees = Relationship.Refugees.valueOf(kOrders.getOrDefault("rel_" + kk + "_refugees", "ACCEPT"));
					r.tribute = Math.max(0, Math.min(1, Double.parseDouble(kOrders.getOrDefault("rel_" + kk + "_tribute", "0")))) / totalTribute;
					if (r.tribute > 0) r.battle = Relationship.War.DEFEND;
					r.construct = Relationship.Construct.valueOf(kOrders.getOrDefault("rel_" + kk + "_construct", "FORBID"));
					r.cede = Relationship.Cede.valueOf(kOrders.getOrDefault("rel_" + kk + "_cede", "ACCEPT"));
					r.fealty = Relationship.Fealty.valueOf(kOrders.getOrDefault("rel_" + kk + "_fealty", "ACCEPT"));
					String d = r.diff(old, k, kk);
					if (!"".equals(d)) changes.add(d);
				}
			}
			if (!changes.isEmpty()) {
				notifyAllPlayers("Political Shifts", String.join("\n", changes));
			}
			for (String k : kingdoms.keySet()) {
				tributes.put(k, new ArrayList<String>());
				for (String kk : kingdoms.keySet()) {
					if (k.equals(kk)) continue;
					if (getNation(k).getRelationship(kk).tribute > 0) tributes.get(k).add(kk);
				}
			}
		}

		void transferGold() {
			for (String k : kingdoms.keySet()) {
				Budget b = new Budget();
				b.start = getNation(k).gold;
				incomeSources.put(k, b);
			}
			// Gold transfers take place.
			HashMap<String, Double> credits = new HashMap<>();
			for (String k : orders.keySet()) {
				Map<String, String> kOrders = orders.get(k);
				double totalGifts = 0;
				for (String o : kOrders.keySet()) {
					if (o.startsWith("nations_gift_amount")) {
						totalGifts += Math.max(0, Double.parseDouble(kOrders.get(o)));
					}
				}
				if (totalGifts > getNation(k).gold) {
					notifyPlayer(k, "Gold Not Sent", "Our treasurers were ordered to transfer " + totalGifts + " gold but only " + getNation(k).gold + " was available. Fearing some mistake, they did not transfer any.");
					continue;
				}
				for (String o : kOrders.keySet()) {
					if (o.startsWith("nations_gift_amount")) {
						double amount = Double.parseDouble(kOrders.get(o));
						if (amount <= 0) continue;
						String who = kOrders.get(o.replace("amount", "target"));
						notifyPlayer(who, "Gold From " + k, k + " has sent us " + amount + " gold.");
						getNation(k).gold -= amount;
						incomeSources.getOrDefault(k, new Budget()).spentGift += amount;
						credits.put(who, credits.getOrDefault(who, 0.0) + amount);
					}
				}
			}
			for (String k : credits.keySet()) {
				getNation(k).gold += credits.get(k);
				incomeSources.getOrDefault(k, new Budget()).incomeGift += credits.get(k);
			}
		}

		void setGothiSpellStatus() {
			Set<Nation.Gothi> passedSpells = new HashSet<>();
			for (Nation.Gothi g : Nation.Gothi.values()) {
				int votes = 0;
				int votesTotal = 0;
				for (String k : kingdoms.keySet()) {
					getNation(k).gothi.put(g, orders.getOrDefault(k, new HashMap<String, String>()).getOrDefault("gothi_" + g.toString().toLowerCase(), getNation(k).gothi.get(g) ? "checked" : "").equals("checked"));
				}
				for (Region r : regions) {
					if (r.religion == null) continue;
					if (r.religion.toString().toLowerCase().contains(g.toString().toLowerCase())) {
						votesTotal++;
						if (getNation(r.getKingdom()).gothi.getOrDefault(g, false)) votes++;
					}
				}
				if (votes >= 5 && 3 * votes >= 2 * votesTotal) passedSpells.add(g);
			}
			// Goodwill effects.
			for (String k : kingdoms.keySet()) {
				boolean voting = getNation(k).gothi.values().stream().anyMatch(a -> a);
				if (church.hasDoctrine(Church.Doctrine.ANTITERRORISM) && voting) getNation(k).goodwill += getRules().antiterrorismOpinion;
			}
			// Spell tickers.
			if (passedSpells.contains(Nation.Gothi.RJINKU)) {
				if (tivar.quake == 0)	notifyAllPlayers("The Quake Begins", "The necessary number of gothi have agreed to call forth the Quake. In his underground domain, the eldritch diety Rjinku hammers at the pillars of the world, shaking the surface. Buildings will be destroyed every week until the earthquakes end. If they do not end soon, destruction of crops will follow.");
				tivar.quake++;
			} else if (tivar.quake > 0) {
				if (Math.random() > getRules().tivarSpellContinueChance) {
					tivar.quake = 0;
					notifyAllPlayers("Earthquakes End", "Rjinku has grown weary, and the magical earthquakes wracking the land have finally been ended.");
				} else {
					tivar.quake++;
				}
			}
			if (passedSpells.contains(Nation.Gothi.SYRJEN)) {
				if (tivar.deluge == 0) notifyAllPlayers("The Deluge Begins", "The necessary number of gothi have agreed to call forth the Deluge. From her riverbeds and springs, the eldritch diety Syrjen sends forth torrential waters, swelling rivers and lakes enough to cause land regions to be navigable by warships. Flash floods wash away a quarter of any ships or soldiers trying to traverse them. As the floods intensify, crops will be lost to the rising waters.");
				tivar.deluge++;
			} else if (tivar.deluge > 0) {
				if (Math.random() > getRules().tivarSpellContinueChance) {
					tivar.deluge = 0;
					notifyAllPlayers("Deluge Ends", "Syrjen has been appeased, and the magical deluge drowning the land has finally ceased.");
				} else {
					tivar.deluge++;
				}
			}
			if (passedSpells.contains(Nation.Gothi.LYSKR)) {
				if (tivar.veil == 0) notifyAllPlayers("The Veil Falls", "The necessary number of gothi have agreed to call forth the Veil. Beneath the sea, the eldritch diety Lyskr breathes out a heavy fog, choking out the sun and concealing the movements of fleets and armies. Lyskr's presence can be felt aiding those who plot in secret. If the fog does not lift soon, crops will starve.");
				tivar.veil++;
			} else if (tivar.veil > 0) {
				if (Math.random() > getRules().tivarSpellContinueChance) {
					tivar.veil = 0;
					notifyAllPlayers("Veil Ends", "Lyskr has returned to slumber, and the magical fog blanketing the land has finally lifted.");
				} else {
					tivar.veil++;
				}
			}
			if (passedSpells.contains(Nation.Gothi.ALYRJA)) {
				if (tivar.warwinds == 0) notifyAllPlayers("The Warwinds Howl", "The necessary number of gothi have agreed to call forth the Warwinds. From her high mountaintop, the eldritch diety Alyrja throws forth horrific winds, flattening crops and summoning titanic waves. Powerful winds blow all vessels at sea off course, and send many straight to the depths. If the storm does not abate soon, crops will be lost.");
				tivar.warwinds++;
			} else if (tivar.warwinds > 0) {
				if (Math.random() > getRules().tivarSpellContinueChance) {
					tivar.warwinds = 0;
					notifyAllPlayers("Warwinds End", "Alyrja has calmed, and the magical storms devastating the land have finally calmed.");
				} else {
					tivar.warwinds++;
				}
			}
		}

		void setDefaultOrderHints() {
			for (Character c : characters) {
				c.orderhint = orders.getOrDefault(c.kingdom, new HashMap<String, String>()).getOrDefault("action_" + c.name.replace(" ", "_").replace("'", "_"), "");
				c.leadingArmy = 0;
			}
		}

		void countInspires() {
			for (Character c : characters) {
				String action = orders.getOrDefault(c.kingdom, new HashMap<String, String>()).getOrDefault("action_" + c.name.replace(" ", "_").replace("'", "_"), "");
				Region region = regions.get(c.location);
				if (action.equals("Inspire the Faithful") && "Sancta Civitate".equals(region.name)) {
					c.addExperienceSpy();
					getNation(c.kingdom).goodwill += 5;
					inspires++;
				}
			}
		}

		void splitArmies() {
			for (String k : orders.keySet()) {
				Map<String, String> kOrders = orders.get(k);
				HashMap<String, String> newOrders = new HashMap<>();
				for (String o : kOrders.keySet()) {
					if (o.startsWith("div_parent_")) {
						int parent = Integer.parseInt(kOrders.get(o));
						Army a = null;
						for (Army aa : armies) if (aa.id == parent) a = aa;
						if (a == null || !a.kingdom.equals(k)) throw new RuntimeException(k + " ordered a split of " + parent);
						double size = Math.min(a.size - 1, Double.parseDouble(kOrders.get(o.replace("parent", "size"))));
						if (size < 0) continue;
						a.size -= size;
						Army nu = Army.newArmy(getRules());
						nu.id = getNewArmyId();
						nu.type = a.type;
						nu.size = size;
						nu.kingdom = a.kingdom;
						nu.location = a.location;
						nu.preparation = new ArrayList<>();
						nu.tags = new ArrayList<>(a.tags);
						nu.composition = new HashMap<>();
						double reduction = size / (size + a.size);
						for (String c : a.composition.keySet()) {
							a.composition.put(c, a.composition.get(c) * (1 - reduction));
							nu.composition.put(c, a.composition.get(c) * reduction);
						}
						for (Preparation p : a.preparation) {
							nu.preparation.add(new Preparation(p));
						}
						armies.add(nu);
						newOrders.put("action_army_" + nu.id, kOrders.get(o.replace("div_parent", "action_div")));
						for (String oo : kOrders.keySet()) {
							if (kOrders.get(oo).equals("Lead division " + o.replace("div_parent_", ""))) newOrders.put(oo, "Lead " + nu.type + " " + nu.id);
						}
					}
				}
				for (String a : newOrders.keySet()) kOrders.put(a, newOrders.get(a));
			}
		}

		void markLeaders() {
			for (Character c : characters) {
				String action = orders.getOrDefault(c.kingdom, new HashMap<String, String>()).getOrDefault("action_" + c.name.replace(" ", "_").replace("'", "_"), "");
				Region region = regions.get(c.location);
				if (action.startsWith("Lead ")) {
					c.orderhint = action;
					int targetId = Integer.parseInt(action.substring(action.lastIndexOf(" ") + 1, action.length()));
					Army target = null;
					for (Army aa : armies) if (aa.id == targetId) target = aa;
					if (target == null) continue; // ???
					if (!target.kingdom.equals(c.kingdom) || c.location != target.location) continue;
					if (leaders.get(target) == null || leaders.get(target).calcLeadMod(target.type) < c.calcLeadMod(target.type)) {
						Character prev = leaders.put(target, c);
						if (prev != null) prev.orderhint = "";
					}
				}
			}
		}

		void mergeArmies() {
			HashMap<Army, Double> originalSizes = new HashMap<>();
			for (Army a : armies) originalSizes.put(a, a.size);
			Map<Integer, Army> pirateLeaders = new HashMap<>();
			for (Army a : armies) {
				if (!Nation.PIRATE_NAME.equals(a.kingdom)) continue;
				if (pirateLeaders.containsKey(a.location)) {
					Army target = pirateLeaders.get(a.location);
					target.size += a.size;
					target.gold += a.gold;
					a.size = 0;
				} else {
					pirateLeaders.put(a.location, a);
				}
			}
			armies.removeIf(a -> a.size == 0);
			for (String k : orders.keySet()) {
				Map<String, String> kOrders = orders.get(k);
				Map<Integer, Integer> alreadyMerged = new HashMap<>(); 
				for (String o : kOrders.keySet()) {
					if (o.startsWith("action_army_") && kOrders.get(o).startsWith("Merge into ")) {
						String order = kOrders.get(o);
						int targetId = Integer.parseInt(order.substring(order.lastIndexOf(" ") + 1, order.length()));
						int sourceId = Integer.parseInt(o.replace("action_army_", ""));
						Army target = null;
						Army src = null;
						while (alreadyMerged.containsKey(targetId)) targetId = alreadyMerged.get(targetId);
						for (Army aa : armies) {
							if (aa.id == targetId) target = aa;
							if (aa.id == sourceId) src = aa;
						}
						if (src == target) continue;
						if (target == null || src == null || target.location != src.location || !target.kingdom.equals(k) || !src.kingdom.equals(k) || !src.type.equals(target.type)) throw new RuntimeException("Cannot execute " + k + ": " + o + ": " + order);
						if (src.hasTag(Army.Tag.UNDEAD) != target.hasTag(Army.Tag.UNDEAD)) continue;
						if (!src.tags.equals(target.tags)) {
							double threatIncrease = src.getEffectiveSize() * 0.33 / 100;
							pirate.threat += threatIncrease;
							pirate.bribes.put(src.kingdom, pirate.bribes.getOrDefault(src.kingdom, 0.0) + threatIncrease / 2);
							pirateThreatSources.put("Army Merges / Disbands", pirateThreatSources.getOrDefault("Army Merges / Disbands", 0.0) + threatIncrease);
							src.size *= .67;
						}
						alreadyMerged.put(sourceId, targetId);
						target.size += src.size;
						target.gold += src.gold;
						armies.remove(src);

						if (leaders.get(src) != null) {
							leaders.get(src).orderhint = "";
							if (leaders.get(target) == null || leaders.get(target).calcLeadMod(target.type) < leaders.get(src).calcLeadMod(target.type)) leaders.put(target, leaders.get(src));
						}
					}
				}
			}
			for (Army a : armies) if (a.size >= 2 * originalSizes.get(a)) a.preparation.clear();
			for (Army a : leaders.keySet()) {
				Character l = leaders.get(a);
				if (a.isArmy()) {
					leaders.get(a).addExperienceGeneral();
					if (a.hasTag(Army.Tag.SCHEMING)) leaders.get(a).addExperienceSpy();
				} else {
					leaders.get(a).addExperienceAdmiral();
				}
				leaders.get(a).leadingArmy = a.id;
			}
		}

		void resolveIntrigue() {
			// Update spy ring orders.
			for (SpyRing ring : spyRings) {
				try {
					String involveIn = orders.getOrDefault(ring.getNation(), new HashMap<String, String>()).get("spyring_" + ring.getLocation());
					SpyRing.InvolvementDisposition involveType = SpyRing.InvolvementDisposition.valueOf(orders.getOrDefault(ring.getNation(), new HashMap<String, String>()).getOrDefault("spyring_type_" + ring.getLocation(), ""));
					if (involveIn == null || involveType == null) continue; // No orders exist for the spy ring - assume no change.
					int plotId = Integer.parseInt(involveIn);
					plots
							.stream()
							.filter(p -> p.getId() == plotId)
							.filter(p -> p.hasConspirator(ring.getNation()))
							.findAny()
							.ifPresent(p -> ring.involve(p.getId(), involveType));
				} catch (NumberFormatException e) {
					log.log(Level.WARNING, "Bad spy ring order: " + ring.getNation() + ", " + ring.getLocation(), e);
				} catch (IllegalArgumentException e) {
					// Do nothing. This is just a missing involveType order.
				}
			}
			// Evaluate current plots.
			Collections.shuffle(plots);
			plots.removeIf(
					p -> p.check(
							World.this,
							p.getConspirators()
									.stream()
									.map(e -> orders.getOrDefault(e, new HashMap<>()).get("plot_execute_" + p.getId()))
									.anyMatch(v -> "checked".equals(v)),
							a -> a.calcStrength(World.this, leaders.get(a), inspires)));
			for (String k : kingdoms.keySet()) {
				incomeSources.get(k).incomeIntrigue += getNation(k).goldStolenGained;
				incomeSources.get(k).spentIntrigue += getNation(k).goldStolenLost;
			}

			// Create new plots.
			for (String k : orders.keySet()) {
				for (String o : orders.get(k).keySet()) {
					if (!o.startsWith("plot_new_type")) continue;
					try {
						int plotId = Integer.parseInt(o.substring("plot_new_type_".length()));
						if (plotId < 0 || plotId > getRules().plotInstigationLimit) continue;
					} catch (NumberFormatException e) {
						continue; // Forged orders - ignore.
					}
					Plot.PlotType type = Plot.PlotType.valueOf(orders.get(k).get(o));
					if (type == null) continue;
					String target = orders.get(k).get(o.replace("type", "target"));
					List<String> conspirators = new ArrayList<>();
					conspirators.add(k);
					for (String s : getNationNames()) if ("checked".equals(orders.get(k).get(o.replace("type", "involve_" + s)))) conspirators.add(s);
					plots.add(Plot.newPlot(getRules(), plots.stream().map(p -> p.getId()).collect(Collectors.toSet()), type, target, conspirators));
				}
			}

			// Update conspirators for old plots.
			for (String k : orders.keySet()) {
				for (String o : orders.get(k).keySet()) {
					if (!o.startsWith("plot_invite_")) continue;
					if (!"checked".equals(orders.get(k).get(o))) continue;
					String[] op = o.substring("plot_invite_".length()).split("_");
					if (op.length != 2) continue;
					if (!getNationNames().contains(op[1])) continue;
					try {
						int plotId = Integer.parseInt(op[0]);
						plots
								.stream()
								.filter(p -> p.getId() == plotId)
								.filter(p -> p.hasConspirator(k))
								.findAny()
								.ifPresent(p -> p.addConspirator(op[1]));
					} catch (NumberFormatException e) {
						log.log(Level.WARNING, "Bad plot invitation: " + k + ", " + o + ": " + orders.get(k).get(o), e);
					}
				}
			}

			// Grow spy rings.
			for (SpyRing s : spyRings) s.grow();
		}

		void doctrineChanges() {
			characters.stream().filter(c -> c.hasTag(Character.Tag.TIECEL)).findAny().ifPresent(tiecel -> {
				Map<String, String> kOrders = orders.get(tiecel.kingdom);
				for (Church.Doctrine policy : Church.Doctrine.values()) {
					String orderKey = "church_" + policy;
					if (kOrders.containsKey(orderKey)) {
						if ("checked".equals(kOrders.get(orderKey))) {
							if (!church.hasDoctrine(policy)) notifyAllPlayers("Doctrine Invoked", policy.getSetNotification(tiecel.name, tiecel.kingdom));
							church.setDoctrine(policy);
						} else {
							if (church.hasDoctrine(policy)) notifyAllPlayers("Doctrine Repealed", policy.getUnsetNotification(tiecel.name, tiecel.kingdom));
							church.unsetDoctrine(policy);
						}
					}
				}
			});
		}

		void orderOverrides() {
			for (Army army : armies) {
				if (army.hasTag(Army.Tag.HIGHER_POWER)) {
					String originalOrder = orders.getOrDefault(army.kingdom, new HashMap<String, String>()).getOrDefault("action_army_" + army.id, "");
					if ("Disband".equals(originalOrder)) {
						army.addTag(Army.Tag.PILLAGERS);
						army.addTag(Army.Tag.UNPREDICTABLE);
						army.kingdom = "Pirate";
						notifyPlayer(army.kingdom, "Army Ignores Orders", "An army we control that serves a Higher Power has refused to disband. Instead they now rove the world as pirates!");
					} else {
						// If they are adjacent to a special region, move into it, regardless of other orders.
						for (Region r : regions.get(army.location).getNeighbors(World.this)) {
							if (cultRegions.contains(regions.indexOf(r)) && !r.cultAccessed) orders.get(army.kingdom).put("action_army_" + army.id, "Travel to " + r.name);
						}
						if (!orders.getOrDefault(army.kingdom, new HashMap<String, String>()).getOrDefault("action_army_" + army.id, "").equals(originalOrder)) notifyPlayer(army.kingdom, "Army Ignores Orders", "An army we control that serves a Higher Power has ignored your orders!");
					}
				}
			}
		}

		void buildAction(String action, String kingdom, Region region) {
			double costMod = 1;
			Nation nation = getNation(kingdom);
			if (nation.hasTag(Nation.Tag.INDUSTRIAL)) costMod -= .35;
			Construction ct;
			if (action.contains("Shipyard")) {
				ct = Construction.makeShipyard(Math.max(0, getRules().baseCostShipyard * costMod));
			} else if (action.contains("Temple")) {
				Ideology ideo = Ideology.fromString(action.replace("Build Temple (", "").replace(")", ""));
				if (nation.hasTag(Nation.Tag.EVANGELICAL) && region.religion != Nation.getStateReligion(kingdom, World.this)) costMod -= 1;
				if (ideo.religion == Religion.IRUHAN && region.religion.religion != Religion.IRUHAN && getDominantIruhanIdeology() == Ideology.VESSEL_OF_FAITH) costMod -= 1;
				if (region.religion == Ideology.TAPESTRY_OF_PEOPLE) {
					boolean templeBonus = true;
					for (Region r : region.getNeighbors(World.this)) if (r.isLand() && (r.religion != region.religion || r.culture != region.culture)) templeBonus = false;
					if (templeBonus) costMod -= 1;
				}
				ct = Construction.makeTemple(ideo, Math.max(0, getRules().baseCostTemple * costMod));
			} else if (action.contains("Fortifications")) {
				ct = Construction.makeFortifications(Math.max(0, getRules().baseCostFortifications * costMod));
			} else {
				return;
			}
			double cost = ct.originalCost;
			if (cost <= nation.gold) {
				nation.gold -= cost;
				incomeSources.getOrDefault(kingdom, new Budget()).spentConstruction += cost;
				region.constructions.add(ct);
				if (ct.type == Construction.Type.TEMPLE) {
					region.setReligion(ct.religion, World.this);
					if (ct.religion == Ideology.VESSEL_OF_FAITH) {
						for (Region rr : regions) if (rr.isLand() && rr.religion == Ideology.VESSEL_OF_FAITH) rr.unrestPopular.add(-0.06);
					}
					if (church.hasDoctrine(Church.Doctrine.ANTIECUMENISM) && ct.religion.religion != Religion.IRUHAN) nation.goodwill += getRules().antiecumenismConstructionOpinion;
					if (church.hasDoctrine(Church.Doctrine.ANTISCHISMATICISM) && ct.religion == Ideology.VESSEL_OF_FAITH) nation.goodwill += getRules().antischismaticismConstructionOpinion;
					if (church.hasDoctrine(Church.Doctrine.WORKS_OF_IRUHAN) && ct.religion.religion == Religion.IRUHAN) nation.goodwill += getRules().worksOfIruhanConstructionOpinion;
				}
				if (ct.type == Construction.Type.SHIPYARD && nation.hasTag(Nation.Tag.SHIP_BUILDING)) buildShips(kingdom, regions.indexOf(region), getRules().numShipsBuiltPerShipyard * getRules().shipBuildingTraitWeeksProduction);
				builds.add(region);
				if (ct.type == Construction.Type.TEMPLE) templeBuilds.add(region);
				if (ct.type == Construction.Type.FORTIFICATIONS && nation.hasTag(Nation.Tag.DEFENSIVE)) {
					region.constructions.add(Construction.makeFortifications(0));
				}
			} else {
				notifyPlayer(kingdom, "Construction Failed", "We did not have the " + Math.round(cost) + "gold necessary to construct as ordered in " + region.name + ".");
			}
		}

		void nobleActions() {
			for (int rid = 0; rid < regions.size(); rid++) {
				Region r = regions.get(rid);
				if (!r.hasNoble()) continue;
				String action = orders.getOrDefault(r.getKingdom(), new HashMap<String, String>()).getOrDefault("action_noble_" + rid, "Train");
				if (action.startsWith("Build ")) {
					buildAction(action, r.getKingdom(), r);
				} else if (action.equals("Train")) {
					r.noble.addExperience(getNation(r.getKingdom()).hasTag(Nation.Tag.ARISTOCRATIC));
				} else if (action.equals("Relax")) {
					r.noble.unrest.add(getRules().nobleActionRelaxUnrest);
				} else if (action.equals("Establish Spy Ring")) {
					String kingdom = r.getKingdom();
					final int ridf = rid;
					if (kingdom == null || spyRings.stream().filter(rr -> rr.getNation().equals(r.getKingdom()) && rr.getLocation() == ridf).count() != 0) continue;
					double cost = getRules().spyRingEstablishCost;
					if (Nation.getStateReligion(kingdom, World.this) == Ideology.LYSKR) cost = 0;
					if (getNation(kingdom).gold < cost) {
						notifyPlayer(kingdom, "Spy Ring Establishment Failed", r.noble.name + " needed " + Math.ceil(cost) + " gold to establish a spy ring in " + r.name + ", but only " + Math.floor(getNation(kingdom).gold) + " was available.");
						continue;
					}
					getNation(kingdom).gold -= cost;
					incomeSources.get(kingdom).spentSpyEstablishments += cost;
					spyRings.add(SpyRing.newSpyRing(getRules(), kingdom, 10, rid));
				}
			}
		}

		void armyActionsNonTravel() {
			ArrayList<Army> actors = new ArrayList<>();
			for (Army a : armies) {
				String order = orders.getOrDefault(a.kingdom, new HashMap<String, String>()).get("action_army_" + a.id);
				a.orderhint = order;
				if (order != null && !order.startsWith("Travel")) actors.add(a);
			}
			Set<String> excommunicatedNations = new HashSet<>();
			for (String nation : kingdoms.keySet()) if (getNation(nation).goodwill < 0) excommunicatedNations.add(nation);
			HashSet<Region> conqueredRegions = new HashSet<>();
			sortByStrength(actors, leaders, inspires);
			for (Army army : actors) {
				String action = orders.getOrDefault(army.kingdom, new HashMap<String, String>()).get("action_army_" + army.id);
				Region region = regions.get(army.location);
				if (action.startsWith("Patrol")) {
					if (!army.isArmy()) continue;
					if (!Nation.isFriendly(army.kingdom, region.getKingdom(), World.this)) continue;
					if (army.calcStrength(World.this, leaders.get(army), inspires) < region.calcMinPatrolStrength(World.this)) {
						notifyPlayer(army.kingdom, "Patrol Ineffective", "Army " + army.id + " is not strong enough to effectively patrol " + region.name + ".");
						continue;
					}
					patrolledRegions.add(region);
					ArrayList<String> who = new ArrayList<>();
					for (Character c : characters) {
						if (c.location == army.location) {
							who.add(c.name + " (" + c.kingdom + ")");
							if (Nation.isEnemy(c.kingdom, army.kingdom, World.this)) {
								boolean guard = false;
								for (Army a : armies) if (a.isArmy() && a.location == c.location && Nation.isFriendly(a.kingdom, c.kingdom, World.this)) guard = true;
								if (!guard) {
									notifyPlayer(c.kingdom, c.name + " Discovered", c.name + " was discovered by a patrolling army.");
								}
							}
						}
					}
					List<SpyRing> removals = new ArrayList<>();
					for (SpyRing r : spyRings) {
						if (r.isExposed() && Nation.isEnemy(r.getNation(), army.kingdom, World.this, region) && r.getLocation() == army.location) {
							notifyPlayer(r.getNation(), "Spy Ring Destroyed", "Our spy ring in " + region.name + " has been destroyed by a patrolling army.");
							removals.add(r);
						}
					}
					spyRings.removeAll(removals);
				} else if (action.startsWith("Raze ")) {
					army.raze(World.this, action, leaders.get(army), inspires);
				} else if (action.startsWith("Force civilians to ")) {
					if (!army.isArmy()) continue;
					// Must be strongest in region (not counting other armies of the same ruler). Target region must allow refugees.
					boolean stopped = false;
					for (Army a : armies) {
						if (a.isArmy() && a.location == army.location && !a.kingdom.equals(army.kingdom) && a.calcStrength(World.this, leaders.get(a), inspires) > army.calcStrength(World.this, leaders.get(army), inspires)) {
							stopped = true;
							notifyPlayer(army.kingdom, "Forced Relocation Failed", "Army " + army.id + " was prevented from forcibly relocating the population of " + region.name + " by other armies present in the region.");
							break;
						}
					}
					if (stopped) continue;
					Region target = null;
					for (Region r : region.getNeighbors(World.this)) if (r.name.equals(action.replace("Force civilians to ", ""))) target = r;
					if (target == null) throw new RuntimeException("No neighbor of " + region.name + " satisfies " + action);
					if (!target.getKingdom().equals(region.getKingdom()) && getNation(target.getKingdom()).getRelationship(region.getKingdom()).refugees == Relationship.Refugees.REFUSE) {
						notifyPlayer(army.kingdom, "Forced Relocation Failed", target.getKingdom() + ", the owner of " + target.name + ", refuses to accept refugees from " + region.getKingdom() + ", and turned them back at the border. The people were allowed to return to their homes.");
						continue;
					}
					// Relocate 10 civilians per soldier.
					double populationMoved = Math.min(region.population - 1, army.size * 10);
					if (!target.getKingdom().equals(army.kingdom)) notifyPlayer(target.getKingdom(), "Refugees from " + region.name, "An army of " + army.kingdom + " has forced " + Math.round(populationMoved) + " civilians from " + region.name + " to move to " + target.name + ".");
					if (!region.getKingdom().equals(army.kingdom) && !region.getKingdom().equals(target.getKingdom())) notifyPlayer(target.getKingdom(), "Forced Relocation from " + region.name, "An army of " + army.kingdom + " has forced " + Math.round(populationMoved) + " civilians from " + region.name + " to move to " + target.name + ".");
					sendRefugees(region, target, populationMoved, false, false);
				} else if (action.startsWith("Transfer ")) {
					String target = action.substring("Transfer army to ".length(), action.length());
					if (!kingdoms.containsKey(target)) throw new RuntimeException("Unknown kingdom \"" + target + "\".");
					if (getNation(target).getRelationship(army.kingdom).fealty == Relationship.Fealty.ACCEPT) {
						notifyPlayer(target, "Soldiers From " + army.kingdom, army.kingdom + " has transferred " + army.size + " " + (army.isArmy() ? "soldiers" : "warships") + " in " + region.name + " to our control.");
						army.kingdom = target;
						army.orderhint = "";
					} else {
						notifyPlayer(target, "Soldiers From " + army.kingdom, army.kingdom + " has attempted to transfer " + (int) Math.round(army.size) + " " + (army.isArmy() ? "soldiers" : "warships") + " in " + region.name + " to our control, but we refused to accept them.");
						notifyPlayer(army.kingdom, "Soldiers To " + target, target + " has refused to accept responsibility for our soldiers.");
					}
				} else if (action.startsWith("Disband")) {
					double threatIncrease = 0;
					if (region.isLand()) {
						addPopulation(region, army.getEffectiveSize() * .67);
						threatIncrease = army.getEffectiveSize() * .33 / 100;
					} else {
						threatIncrease = army.getEffectiveSize() / 100;
					}
					pirate.threat += threatIncrease;
					pirate.bribes.put(army.kingdom, pirate.bribes.getOrDefault(army.kingdom, 0.0) + threatIncrease / 2);
					pirateThreatSources.put("Army Merges / Disbands", pirateThreatSources.getOrDefault("Army Merges / Disbands", 0.0) + threatIncrease);
					armies.remove(army);
				} else if (action.startsWith("Conquer")) {
					army.conquer(World.this, action, conqueredRegions, tributes, leaders, inspires, excommunicatedNations);
				} else if (action.startsWith("Oust ")) {
					if (!army.isArmy()) continue;
					if (region.noble == null) continue;
					if (!region.getKingdom().equals(army.kingdom)) continue;
					if (army.calcStrength(World.this, leaders.get(army), inspires) < region.calcMinPatrolStrength(World.this)) {
						notifyPlayer(army.kingdom, "Ousting Failed", "Army " + army.id + " is not strong enough to oust the nobility of " + region.name + ".");
						continue;
					}
					for (Region r : regions) if (army.kingdom.equals(r.getKingdom()) && r.noble != null) r.noble.unrest.add(.2);
					notifyPlayer(army.kingdom, "Ousted Nobility of " + region.name, "Our army ousted the nobility of " + region.name + ". " + region.noble.name + " and their family " + (Math.random() < 0.5 ? " fought to the last member and were completely wiped out." : " accepted their defeat graciously, but were found dead the next morning."));
					region.noble = null;
					army.orderhint = "";
				}
			}
		}

		void armyActionsTravel() {
			for (Army army : armies) {
				String action = orders.getOrDefault(army.kingdom, new HashMap<String, String>()).getOrDefault("action_army_" + army.id, "");
				if (army.hasTag(Army.Tag.UNPREDICTABLE)) {
					List<Region> n = new ArrayList<>(regions.get(army.location).getNeighbors(World.this));
					ArrayList<Region> except = new ArrayList<>();
					for (Region r : n) if (r.isSea()) except.add(r);
					for (Region r : except) n.remove(r);
					if (!n.isEmpty()) action = "Travel to " + n.get((int)(Math.random() * n.size())).name;
				}
				if (!action.startsWith("Travel to ")) continue;
				Region region = regions.get(army.location);
				String destination = action.replace("Travel to ", "");
				boolean isNeighbor = false;
				for (Region r : region.getNeighbors(World.this)) if (r.name.equals(destination)) isNeighbor = true;
				if (!isNeighbor) throw new RuntimeException("Cannot move " + army.id + " to " + destination + ": " + action);
				int toId = -1;
				for (int i = 0; i < regions.size(); i++) {
					if (regions.get(i).name.equals(destination)) toId = i;
				}
				if (tivar.deluge < 2 && army.isNavy() && region.isLand() && regions.get(toId).isLand()) continue;
				int crossing = -1;
				for (Geography.Border b : geography.borders) if ((b.b != null && b.a == army.location && b.b == toId) || (b.b != null && b.b == army.location && b.a == toId)) crossing = b.w;
				Preparation prep = null;
				for (Preparation p : army.preparation) if (p.to == toId) prep = p;
				int travelAmount = 1;
				if (army.hasTag(Army.Tag.PATHFINDERS)) travelAmount = 3;
				if (prep == null) {
					prep = new Preparation();
					prep.to = toId;
					prep.amount = travelAmount;
					army.preparation.add(prep);
				} else {
					prep.amount += travelAmount;
				}
				if (prep.amount >= crossing) {
					army.location = toId;
					army.preparation.clear();
					army.orderhint = "";
					// Attrition
					region = regions.get(toId);
					if (getAttrition(army, region)) {
						attritionLosses.put(army, army.size * .25);
						army.size *= .75;
					}
				}
				Character leader = leaders.get(army);
				if (leader != null) {
					leader.location = army.location;
					leader.preparation.clear();
					for (Preparation p : army.preparation) leader.preparation.add(new Preparation(p));
				}
			}
		}

		void characterActions() {
			ArrayList<Character> removeCharacters = new ArrayList<>();
			for (Character c : characters) {
				String action = orders.getOrDefault(c.kingdom, new HashMap<String, String>()).getOrDefault("action_" + c.name.replace(" ", "_").replace("'", "_"), "");
				Region region = regions.get(c.location);
				c.hidden = action.startsWith("Hide in ") || action.equals("Establish Spy Ring");
				if (action.startsWith("Stay in ")) {
					c.addExperienceAll();
				} else if (action.startsWith("Hide in ") || action.startsWith("Travel to ")) {
					if (c.hidden) c.addExperienceSpy();
					else c.addExperienceAll();
					String destination = action.replace("Travel to ", "").replace("Hide in ", "");
					if (region.name.equals(destination)) continue;
					boolean isNeighbor = false;
					for (Region r : region.getNeighbors(World.this)) if (r.name.equals(destination)) isNeighbor = true;
					if (!isNeighbor) throw new RuntimeException("Cannot move " + c.name + " to " + destination + ": " + action);
					int toId = -1;
					for (int i = 0; i < regions.size(); i++) {
						if (regions.get(i).name.equals(destination)) toId = i;
					}
					c.location = toId;
					c.orderhint = c.hidden ? "Hide in " + regions.get(toId).name : "";
					c.preparation.clear(); // Can still have preparation from travelling w/ an army.
				} else if (action.startsWith("Build ")) {
					c.addExperienceGovernor();
					if (Ideology.VESSEL_OF_FAITH != Nation.getStateReligion(c.kingdom, World.this) && !c.kingdom.equals(region.getKingdom()) && getNation(region.getKingdom()).getRelationship(c.kingdom).construct == Relationship.Construct.FORBID) {
						notifyPlayer(c.kingdom, "Construction Failed", region.getKingdom() + " does not permit us to build in " + region.name + ".");
					} else {
						buildAction(action, c.kingdom, region);
					}
				} else if (action.startsWith("Instate Noble")) {
					if (!region.isLand() || !region.getKingdom().equals(c.kingdom) || region.noble != null) continue;
					region.noble = Noble.newNoble(region.culture, date, getRules());
					c.orderhint = "";
					c.addExperienceGovernor();
				} else if (action.startsWith("Establish Spy Ring")) {
					if (!region.isLand() || spyRings.stream().filter(r -> r.getNation().equals(c.kingdom) && r.getLocation() == c.location).count() != 0) continue;
					c.addExperienceSpy();
					double cost = getRules().spyRingEstablishCost;
					if (Nation.getStateReligion(c.kingdom, World.this) == Ideology.LYSKR) cost = 0;
					if (getNation(c.kingdom).gold < cost) {
						notifyPlayer(c.kingdom, "Spy Ring Establishment Failed", c.name + " needed " + Math.ceil(cost) + " gold to establish a spy ring in " + regions.get(c.location).name + ", but only " + Math.floor(getNation(c.kingdom).gold) + " was available.");
						continue;
					}
					getNation(c.kingdom).gold -= cost;
					incomeSources.get(c.kingdom).spentSpyEstablishments += cost;
					spyRings.add(SpyRing.newSpyRing(getRules(), c.kingdom, c.calcSpyRingEstablishmentStrength(), c.location));
					c.orderhint = "Hide in " + region.name;
				} else if (action.startsWith("Transfer character to ")) {
					String target = action.replace("Transfer character to ", "");
					if (!kingdoms.containsKey(target)) throw new RuntimeException("Unknown kingdom \"" + target + "\".");
					notifyPlayer(target, "Hero from " + c.kingdom, c.name + ", formerly a hero of " + c.kingdom + " has sworn fealty and loyalty to us.");
					c.kingdom = target;
					c.orderhint = "";
				}
			}
			for (Character c : removeCharacters) characters.remove(c);
		}

		void destroyPiratesInAlyrjaRegions() {
			ArrayList<Army> remove = new ArrayList<>();
			for (Army a : armies) {
				if ("Pirate".equals(a.kingdom) && Ideology.ALYRJA == regions.get(a.location).religion) remove.add(a);
			}
			for (Army a : remove) {
				String kingdom = regions.get(a.location).getKingdom();
				notifyPlayer(kingdom, "Pirates Destroyed", "The pirates that have wandered into " + regions.get(a.location).name + " have been thoroughly destroyed by the occupants of that region.");
				getNation(kingdom).gold += a.gold;
				incomeSources.get(kingdom).incomeArmyDelivery += a.gold;
				armies.remove(a);
			}
		}

		void spawnCultists() {
			HashSet<String> newCultists = new HashSet<>();
			for (String kingdom : orders.keySet()) {
				if ("checked".equals(orders.get(kingdom).get("plot_cult")) && !getNation(kingdom).loyalToCult) {
					getNation(kingdom).loyalToCult = true;
					getNation(kingdom).toggleProfile(Nation.ScoreProfile.CULTIST);
					newCultists.add(kingdom);
					notifyAllPlayers(kingdom + " Joins Cult", "Throughout " + kingdom + ", the dead crawl forth from their graves, animated by some ancient magic. The shambling sight of a corpse searching for an unknown quarry becomes commonplace.");
				}
			}
			new ArrayList<CultCache>(cultCaches)
					.stream()
					.forEach(c -> {
							HashSet<String> claims = new HashSet<>();
							for (String k : newCultists) if (c.isEligible(k)) claims.add(k);
							if (claims.isEmpty()) return;
							for (String kingdom : claims) {
								Army a = Army.newArmy(getRules());
								a.location = c.getLocation();
								a.type = Army.Type.ARMY;
								a.size = c.getSize() / claims.size();
								a.tags = new ArrayList<>();
								a.id = getNewArmyId();
								a.kingdom = kingdom;
								a.composition.put("Undead", a.size);
								a.orderhint = "";
								a.addTag(Army.Tag.UNDEAD);
								a.addTag(Army.Tag.HIGHER_POWER);
								armies.add(a);
							}
							cultCaches.remove(c);
						});
		}

		void bribePirates() {
			for (String kingdom : orders.keySet()) {
				for (String o : orders.get(kingdom).keySet()) {
					if (o.startsWith("economy_bribe_amount_")) {
						double amount = Math.max(0, Math.min(getNation(kingdom).gold, Double.parseDouble(orders.get(kingdom).get(o))));
						String target = orders.get(kingdom).get(o.replace("amount", "target"));
						boolean attack = orders.get(kingdom).get(o.replace("amount", "action")).equals("Attack");
						getNation(kingdom).gold -= amount;
						incomeSources.getOrDefault(kingdom, new Budget()).spentBribes += amount;
						pirate.bribes.put(target, pirate.bribes.getOrDefault(target, 0.0) + (attack ? amount : -amount));
					}
				}
			}
		}

		void spawnPirates() {
			double totalPirateThreat = 0;
			double pirateArmies = pirate.threat * .25 * 100;
			double piratesSpawned = 0;
			Set<String> pirateNotes = new HashSet<>();
			while (piratesSpawned < pirateArmies) {
				for (Region r : regions) totalPirateThreat += r.calcPirateThreat(World.this, patrolledRegions.contains(r));
				if (totalPirateThreat == 0) return; // Probably 0% unhappiness globally.
				totalPirateThreat *= Math.random();
				for (int i = 0; i < regions.size(); i++) {
					totalPirateThreat -= regions.get(i).calcPirateThreat(World.this, patrolledRegions.contains(regions.get(i)));
					if (totalPirateThreat < 0) {
						int loc = i;
						Optional<Army> pirates = armies.stream().filter(a -> Nation.PIRATE_NAME.equals(a.kingdom)).filter(a -> a.location == loc).findAny();
						double size = Math.min(pirateArmies - piratesSpawned, 2000);
						if (pirates.isPresent()) {
							pirates.get().size += size;
						} else {
							Army a = Army.newArmy(getRules());
							a.location = i;
							a.type = Army.Type.ARMY;
							a.size = size;
							a.tags = new ArrayList<>();
							a.id = getNewArmyId();
							a.kingdom = "Pirate";
							a.composition.put("Pirate", a.size);
							a.orderhint = "";
							a.addTag(Army.Tag.PILLAGERS);
							a.addTag(Army.Tag.UNPREDICTABLE);
							armies.add(a);
						}
						piratesSpawned += size;
						pirateNotes.add(regions.get(i).name);
						break;
					}
				}
			}
			pirate.threat *= .75;
			for (String bribe : pirate.bribes.keySet()) pirate.bribes.put(bribe, pirate.bribes.get(bribe) * .75);
			notifyAllPlayers("Piracy", (int) Math.ceil(piratesSpawned) + " total pirates have appeared in " + StringUtil.and(pirateNotes) + ".");
		}

		void joinBattles() {
			Set<String> excommunicatedNations = new HashSet<>();
			for (String nation : kingdoms.keySet()) if (getNation(nation).goodwill < 0) excommunicatedNations.add(nation);
			Map<String, Double> casualtiesCaused = new HashMap<>();
			Map<String, Double> casualtiesSuffered = new HashMap<>();
			for (int i = 0; i < regions.size(); i++) {
				Region region = regions.get(i);
				ArrayList<Army> localArmies = new ArrayList<>();
				for (Army a : armies) if (a.location == i) {
					if (a.isNavy() && region.isLand() && tivar.deluge == 0) continue;
					localArmies.add(a);
				}
				double combatFactor = Math.random() * .3 + 1.7;
				double totalArmyStrength = 0;
				for (Army a : localArmies) totalArmyStrength += Math.pow(a.calcStrength(World.this, leaders.get(a), inspires), combatFactor);
				HashMap<Army, Double> casualties = new HashMap<>();
				HashMap<Army, Double> hansaImpressment = new HashMap<>();
				HashMap<Army, Double> undeadImpressment = new HashMap<>();
				HashMap<Army, Double> goldThefts = new HashMap<>();
				for (Army a : localArmies) {
					double enemyStrength = 0;
					for (Army b : localArmies) {
						if (Nation.isEnemy(a.kingdom, b.kingdom, World.this, region) && (!a.hasTag(Army.Tag.HIGHER_POWER) || !b.hasTag(Army.Tag.HIGHER_POWER))) enemyStrength += Math.pow(b.calcStrength(World.this, leaders.get(b), inspires), combatFactor);
					}
					if (enemyStrength == 0) continue;
					double cf = enemyStrength / totalArmyStrength;
					if (a.hasTag(Army.Tag.FORMATIONS) && cf > .45 && cf < .9) cf = .45;
					if (region.getKingdom() != null && Nation.isFriendly(region.getKingdom(), a.kingdom, World.this)) {
						cf *= Math.max(0, Math.min(1, 1 - (region.calcFortificationMod())));
					}
					if (cf >= .9) cf = 1;
					if (cf != 0) casualties.put(a, cf);
					casualtiesSuffered.put(a.kingdom, casualtiesSuffered.getOrDefault(a.kingdom, 0.0) + cf * a.getEffectiveSize());
					for (Army b : localArmies) {
						if (!Nation.isEnemy(a.kingdom, b.kingdom, World.this, region) || (a.hasTag(Army.Tag.HIGHER_POWER) && b.hasTag(Army.Tag.HIGHER_POWER))) continue;
						double casualtyRateCaused = cf * Math.pow(b.calcStrength(World.this, leaders.get(b), inspires), combatFactor) / enemyStrength;
						casualtiesCaused.put(b.kingdom, casualtiesCaused.getOrDefault(b.kingdom, 0.0) + a.getEffectiveSize() * casualtyRateCaused);
						if (a.isArmy() && b.hasTag(Army.Tag.IMPRESSMENT)) hansaImpressment.put(b, hansaImpressment.getOrDefault(b, 0.0) + a.getEffectiveSize() * .15 * casualtyRateCaused);
						if (a.isArmy() && !a.hasTag(Army.Tag.UNDEAD) && b.hasTag(Army.Tag.UNDEAD)) undeadImpressment.put(b, undeadImpressment.getOrDefault(b, 0.0) + a.getEffectiveSize() * .5 * casualtyRateCaused);
						if (church.hasDoctrine(Church.Doctrine.DEFENDERS_OF_FAITH) && getNation(a.kingdom) != null && getNation(b.kingdom) != null && excommunicatedNations.contains(a.kingdom)) getNation(b.kingdom).goodwill += getRules().defendersOfFaithCasualtyOpinion * a.size * casualtyRateCaused;
						goldThefts.put(b, goldThefts.getOrDefault(b, 0.0) + a.gold * casualtyRateCaused);
					}
				}
				for (Army c : casualties.keySet()) {
					double cf = casualties.get(c);
					if (!c.hasTag(Army.Tag.UNDEAD)) {
						addCultCache(c.getEffectiveSize() * cf * getRules().cultRaiseFraction, c.kingdom, c.location);
					}
					if (cf >= 1) {
						armies.remove(c);
					} else {
						c.size -= c.size * cf;
						c.gold -= c.gold * cf;
					}
					for (String s : c.composition.keySet()) {
						c.composition.put(s, c.composition.get(s) * (1 - cf));
					}
				}
				for (Army h : hansaImpressment.keySet()) {
					h.size += hansaImpressment.get(h);
				}
				for (Army h : undeadImpressment.keySet()) {
					h.size += undeadImpressment.get(h);
				}
				for (Army h : goldThefts.keySet()) {
					h.gold += goldThefts.get(h);
				}
				String battleDetails = "";
				HashSet<String> localKingdoms = new HashSet<>();
				for (Army a : localArmies) localKingdoms.add(a.kingdom);
				if (!casualties.isEmpty()) {
					for (String k : localKingdoms) {
						HashMultiset<String> armyFates = HashMultiset.create();
						for (Army a : localArmies) if (casualties.containsKey(a)) {
							armyFates.add((a.isNavy() ? "A navy" : "An army") + " of " + (isHidden(a, k) ? "an unknown nation" : a.kingdom) + " suffered " + Math.round(casualties.get(a) * 100) + "% losses.");
						}
						String armySummary =
								armyFates
										.entrySet()
										.stream()
										.sorted((a, b) -> a.getElement().compareTo(b.getElement()))
										.map(e -> e.getCount() == 1 ? e.getElement() : e.getElement().replace("A navy", e.getCount() + " navies").replace("An army", e.getCount() + " armies"))
										.reduce((a, b) -> a + "\n" + b)
										.orElse("");
						notifyPlayer(k, "Battle in " + region.name, "Over a week of fighting in " + region.name + ":\n" + armySummary + "\n" + battleDetails);
					}
				}
			}
			for (String k : kingdoms.keySet()) {
				score(k, Nation.ScoreProfile.GLORY, -1);
				score(k, Nation.ScoreProfile.GLORY, (casualtiesCaused.getOrDefault(k, 0.0) + casualtiesSuffered.getOrDefault(k, 0.0)) / 1000.0);
			}
		}

		void captureNavies() {
			if (tivar.deluge == 0) {
				ArrayList<Army> removals = new ArrayList<>();
				for (int i = 0; i < regions.size(); i++) {
					Region region = regions.get(i);
					if (region.isSea()) continue;
					for (Army a : armies) if (a.location == i && a.isNavy()) {
						Army max = getMaxArmyInRegion(i, leaders, inspires);
						if (max != null && Nation.isEnemy(a.kingdom, max.kingdom, World.this, region)) {
							notifyPlayer(a.kingdom, "Fleet Captured", "Our fleet of " + Math.round(a.size) + " warships in " + region.name + " was seized by " + max.kingdom + ".");
							a.kingdom = max.kingdom;
							if (leaders.containsKey(a)) leaders.get(a).leadingArmy = -1;
							if (max.kingdom.equals("Pirate")) {
								pirate.threat += a.size;
								pirateThreatSources.put("Naval Captures", pirateThreatSources.getOrDefault("Naval Captures", 0.0) + a.size);
								removals.add(a);
							}
						}
					}
				}
				for (Army a : removals) armies.remove(a);
			}
		}

		void deliverGoldFromArmies() {
			for (Army a : armies) {
				if (kingdoms.containsKey(a.kingdom) && a.kingdom.equals(regions.get(a.location).getKingdom())) {
					getNation(a.kingdom).gold += a.gold;
					incomeSources.getOrDefault(a.kingdom, new Budget()).incomeArmyDelivery += a.gold;
					a.gold = 0;
				}
			}
		}

		void miscPerTurnEffects() {
			for (String k : kingdoms.keySet()) rationing.put(k, Double.parseDouble(orders.getOrDefault(k, new HashMap<String, String>()).getOrDefault("economy_ration", "100")) / 100);
			// Most unrest mods.
			for (Region r : regions) if (r.isLand()) {
				double unrestMod = 0;
				if (getNation(r.getKingdom()).hasTag(Nation.Tag.REPUBLICAN)) unrestMod -= .03;
				if (r.getKingdom() != null && getNation(r.getKingdom()).hasTag(Nation.Tag.IMPERIALISTIC)) {
					int tributeC = 0;
					for (String k : tributes.keySet()) if (tributes.get(k).contains(r.getKingdom())) tributeC++;
					unrestMod -= 0.03 * tributeC;
				}
				if (r.getKingdom() != null && !Nation.UNRULED_NAME.equals(r.getKingdom())) {
					for (String k : tributes.get(r.getKingdom())) if (getNation(k).hasTag(Nation.Tag.IMPERIALISTIC)) unrestMod -= 0.03;
				}
				for (Construction c : r.constructions) if (c.type == Construction.Type.TEMPLE && (c.religion == Ideology.VESSEL_OF_FAITH || c.religion == Ideology.RIVER_OF_KUUN)) unrestMod -= 0.02;
				r.unrestPopular.add(unrestMod);

				if (r.noble != null) for (String k : tributes.keySet()) if (tributes.get(k).contains(r.getKingdom()) && Nation.isEnemy(k, r.getKingdom(), World.this) && getNation(k).previousTributes.contains(r.getKingdom())) r.noble.unrest.add(.08);
			}
			// Character experience.
			for (Character c : characters) {
				Ideology r = Nation.getStateReligion(c.kingdom, World.this);
				if (r == Ideology.RJINKU) c.addExperienceGeneral();
				else if (r == Ideology.FLAME_OF_KITH) c.addExperienceGovernor();
			}
		}

		void noblesRebel() {
			for (String k : kingdoms.keySet()) {
				boolean rebels = false;
				for (Region r : regions) if (k.equals(r.getKingdom()) && r.noble != null && r.noble.unrest.get() >= .75) rebels = true;
				if (rebels) {
					ArrayList<String> rebelTo = new ArrayList<>();
					for (String kk : kingdoms.keySet()) if (Nation.isEnemy(k, kk, World.this) && !getNation(kk).hasTag(Nation.Tag.REPUBLICAN)) rebelTo.add(kk);
					if (rebelTo.isEmpty()) {
						HashMap<String, ArrayList<Double>> unrests = new HashMap<>();
						for (Region r : regions) if (!k.equals(r.getKingdom()) && r.noble != null) {
							if (!unrests.containsKey(r.getKingdom())) unrests.put(r.getKingdom(), new ArrayList<Double>());
							unrests.get(r.getKingdom()).add(r.noble.unrest.get());
						}
						double min = Double.MAX_VALUE;
						String minKey = null;
						for (String s : unrests.keySet()) {
							double mean = 0;
							for (Double d : unrests.get(s)) mean += d / unrests.get(s).size();
							if (mean < min) {
								min = mean;
								minKey = s;
							}
						}
						if (minKey != null) rebelTo.add(minKey);
					}
					if (!rebelTo.isEmpty()) {
						String rebelToChosen = rebelTo.get((int)(Math.random() * rebelTo.size()));
						notifyAllPlayers("Rebellion in " + k, "Enraged with the curent rule, nobles of " + k + " have engaged in open rebellion. They now declare alliegence to " + rebelToChosen + ".");
						for (Region r : regions) if (k.equals(r.getKingdom()) && r.noble != null && r.noble.unrest.get() >= 0.5) {
							r.setKingdom(World.this, rebelToChosen);
							r.noble.unrest.set(0.15);
						}
					}
				}
			}
		}

		void gainIncome() {
			for (int i = 0; i < regions.size(); i++) {
				Region r = regions.get(i);
				if (r.isLand()) {
					String whoTaxes = r.getKingdom();
					double income = r.calcTaxIncome(World.this, null, taxationRates.getOrDefault(r.getKingdom(), 1.0), rationing.getOrDefault(r.getKingdom(), 1.0));
					Army max = getMaxArmyInRegion(i, leaders, inspires);
					if (max != null && !Nation.isFriendly(max.kingdom, r.getKingdom(), World.this) && max.hasTag(Army.Tag.PILLAGERS)) {
						max.gold += income;
					} else {
						incomeSources.getOrDefault(r.getKingdom(), new Budget()).incomeTax += income;
					}
				} else {
					if (tivar.warwinds != 0) continue; // No naval income while warwinds are active.
					// Get navy powers, 20 gold to biggest, 10 to runner-up; if tie; split 30 between all tied
					ArrayList<Army> localNavies = new ArrayList<>();
					for (Army a : armies) if (a.location == i && a.isNavy()) localNavies.add(a);
					sortByStrength(localNavies, leaders, inspires);
					if (localNavies.isEmpty()) continue;
					ArrayList<Army> toRemove = new ArrayList<>();
					for (int k = 0; k < localNavies.size(); k++) {
						for (int j = k + 1; j < localNavies.size(); j++) {
							if (localNavies.get(j).kingdom.equals(localNavies.get(k).kingdom)) toRemove.add(localNavies.get(j));
						}
					}
					for (Army a : toRemove) localNavies.remove(a);
					ArrayList<Army> tiedForFirst = new ArrayList<Army>();
					tiedForFirst.add(localNavies.get(0));
					double firstStrength = localNavies.get(0).calcStrength(World.this, leaders.get(localNavies.get(0)), inspires);
					for (int j = 1; j < localNavies.size(); j++) {
						Army a = localNavies.get(j);
						if (a.calcStrength(World.this, leaders.get(a), inspires) == firstStrength) {
							tiedForFirst.add(a);
						} else {
							break;
						}
					}
					if (tiedForFirst.size() > 1) {
						// divide 30 gold between 1sts
						for (Army a : tiedForFirst) incomeSources.getOrDefault(a.kingdom, new Budget()).incomeSea += 20 / tiedForFirst.size();
					} else if (tiedForFirst.size() == 1) {
						// give 20 gold to 1st
						incomeSources.getOrDefault(tiedForFirst.get(0).kingdom, new Budget()).incomeSea += 20;
						if (tiedForFirst.size() < localNavies.size()) {
							// give 10 gold to 2nd
							ArrayList<Army> tiedForSecond = new ArrayList<Army>();
							tiedForSecond.add(localNavies.get(tiedForFirst.size()));
							double secondStrength = tiedForSecond.get(0).calcStrength(World.this, leaders.get(tiedForSecond.get(0)), inspires);
							for (int j = 2; j < localNavies.size(); j++) {
								Army a = localNavies.get(j);
								if (a.calcStrength(World.this, leaders.get(a), inspires) == secondStrength) {
									tiedForSecond.add(a);
								} else {
									break;
								}
							}
							for (Army a : tiedForSecond) incomeSources.getOrDefault(a.kingdom, new Budget()).incomeSea += 10 / tiedForSecond.size();
						}
					}
				}
			}
			for (String k : kingdoms.keySet()) {
				double seaMods = 1;
				if (Nation.getStateReligion(k, World.this) == Ideology.SYRJEN) seaMods += 1;
				if (getNation(k).hasTag(Nation.Tag.SEAFARING)) seaMods += 1/3.0;
				incomeSources.getOrDefault(k, new Budget()).incomeSea *= seaMods;
			}
		}

		void churchOpinionChangesDueToStateReligion() {
			Map<String, Ideology> stateIdeologies = kingdoms.keySet().stream().collect(Collectors.toMap(k -> k, k -> Nation.getStateReligion(k, World.this)));
			for (String k : kingdoms.keySet()) {
				Religion religion = stateIdeologies.get(k).religion;
				if (church.hasDoctrine(Church.Doctrine.ANTIECUMENISM) && religion != Religion.IRUHAN && religion != Religion.NONE) getNation(k).goodwill += getRules().antiecumenismStateOpinion;
				if (church.hasDoctrine(Church.Doctrine.ANTIAPOSTASY) && getNation(k).loyalToCult) getNation(k).goodwill += getRules().antiapostasyOpinion;
			}
			Set<String> iruhanNations = stateIdeologies.entrySet().stream().filter(e -> e.getValue().religion == Religion.IRUHAN).map(Map.Entry::getKey).collect(Collectors.toSet());
			if (church.hasDoctrine(Church.Doctrine.INQUISITION)) {
				Predicate<String> isNotAttackingSomeVesselOfFaith =
						k -> 
								stateIdeologies
										.entrySet()
										.stream()
										.filter(e -> e.getValue() == Ideology.VESSEL_OF_FAITH)
										.map(Map.Entry::getKey)
										.anyMatch(v -> !Nation.isAttackingOnSight(k, v, World.this));
				iruhanNations
						.stream()
						.filter(isNotAttackingSomeVesselOfFaith)
						.map(World.this::getNation)
						.forEach(n -> n.goodwill += getRules().inquisitionOpinion);
			}
			if (church.hasDoctrine(Church.Doctrine.CRUSADE)) {
				Predicate<String> isNotAttackingSomeHeathen =
						k -> 
								stateIdeologies
										.entrySet()
										.stream()
										.filter(e -> e.getValue().religion != Religion.NONE && e.getValue().religion != Religion.IRUHAN)
										.map(Map.Entry::getKey)
										.anyMatch(t -> !Nation.isAttackingOnSight(k, t, World.this));
				iruhanNations
						.stream()
						.filter(isNotAttackingSomeHeathen)
						.map(World.this::getNation)
						.forEach(n -> n.goodwill += getRules().crusadeOpinion);
			}
			if (church.hasDoctrine(Church.Doctrine.FRATERNITY)) {
				iruhanNations
						.stream()
						.filter(k -> 
								iruhanNations
										.stream()
										.anyMatch(t -> Nation.isAttackingOnSight(k, t, World.this)))
						.map(World.this::getNation)
						.forEach(n -> n.goodwill += getRules().fraternityOpinion);
			}
			if (church.hasDoctrine(Church.Doctrine.MANDATORY_MINISTRY)) {
				iruhanNations
						.stream()
						.filter(k ->
								characters.stream().anyMatch(c -> c.hasTag(Character.Tag.CARDINAL) && k.equals(c.kingdom) && regions.get(c.location).name != "Sancta Civitate"))
						.map(World.this::getNation)
						.forEach(n -> n.goodwill += getRules().mandatoryMinistryOpinion);
			}
		}

		void selectTiecel() {
			boolean isTiecel = false;
			for (Character c : characters) if (c.hasTag(Character.Tag.TIECEL)) isTiecel = true;
			if (!isTiecel) {
				HashMap<Integer, ArrayList<Character>> cardinalCount = new HashMap<>();
				int totalCardinals = 0;
				for (Character c: characters) if (c.hasTag(Character.Tag.CARDINAL)) {
					totalCardinals++;
					if (!cardinalCount.containsKey(c.location)) cardinalCount.put(c.location, new ArrayList<Character>());
					cardinalCount.get(c.location).add(c);
				}
				for (Integer i : cardinalCount.keySet()) {
					if (3 * cardinalCount.get(i).size() >= 2 * totalCardinals) {
						// Find the present Cardinal with the greatest favor.
						double maxFavor = Double.MIN_VALUE;
						Character max = null;
						for (Character t : cardinalCount.get(i)) {
							if (getNation(t.kingdom).goodwill > maxFavor) {
								max = t;
								maxFavor = getNation(t.kingdom).goodwill;
							}
						}
						max.addTag(Character.Tag.TIECEL);
						notifyAllPlayers("Tiecel Elected", "The Cardinals of the Church of Iruhan have elected " + max.name + " as Tiecel. Consequently, " + max.kingdom + " is expected to play a much larger role in the Church than they have in the past.");
					}
				}
			}
		}

		void gainChurchIncome() {
			double churchIncome = getRules().churchIncomePerPlayer * numPlayers + inspires * 20;
			double mods = 1;
			if (Ideology.TAPESTRY_OF_PEOPLE == getDominantIruhanIdeology()) mods += 1;
			mods += inspires * .2;

			HashMap<String, Double> foodBalance = new HashMap<>();
			for (Region r : regions) {
				if (r.isSea()) continue;
				int turnsUntilHarvest = 3 - (date + 1) % 4; // Note that gainChurchIncome runs after the harvest and eat steps, hence the use of date + 1.
				double balance = r.calcConsumption() * turnsUntilHarvest - r.food;
				foodBalance.put(r.getKingdom(), foodBalance.getOrDefault(r.getKingdom(), 0.0) + balance);
			}
			double totalMeasuresDeficit = 0;
			for (String k : foodBalance.keySet()) if (foodBalance.get(k) > 0) totalMeasuresDeficit += foodBalance.get(k);
			HashMap<String, Double> shares = new HashMap<>();
			for (String k : foodBalance.keySet()) if (foodBalance.get(k) > 0) shares.put(k, shares.getOrDefault(k, 0.0) + foodBalance.get(k) / totalMeasuresDeficit);
			double totalGoodwill = 0;
			for (String k : kingdoms.keySet()) if (getNation(k).goodwill > 0) totalGoodwill += getNation(k).goodwill * ((getDominantIruhanIdeology().equals(Nation.getStateReligion(k, World.this)) && getNation(k).hasTag(Nation.Tag.HOLY)) ? 2 : 1);
			for (String k : kingdoms.keySet()) if (getNation(k).goodwill > 0) shares.put(k, shares.getOrDefault(k, 0.0) + getNation(k).goodwill * ((getDominantIruhanIdeology().equals(Nation.getStateReligion(k, World.this)) && getNation(k).hasTag(Nation.Tag.HOLY)) ? 2 : 1) * 2 / totalGoodwill);
			double totalShares = 0;
			for (String k : shares.keySet()) totalShares += shares.get(k);
			for (String k : shares.keySet()) incomeSources.getOrDefault(k, new Budget()).incomeChurch += shares.get(k) / totalShares * churchIncome;
		}

		void payTribute() {
			for (String k : kingdoms.keySet()) {
				Budget bk = incomeSources.getOrDefault(k, new Budget());
				double income = bk.incomeTax + bk.incomeSea + bk.incomeChurch;
				double totalTribute = 0;
				for (String kk : kingdoms.keySet()) if (!k.equals(kk)) totalTribute += getNation(k).getRelationship(kk).tribute;
				for (String kk : kingdoms.keySet()) if (!k.equals(kk)) {
					double t = getNation(k).getRelationship(kk).tribute * income / (totalTribute > 1 ? totalTribute : 1);
					double mod = 1;
					if (Nation.getStateReligion(k, World.this) == Ideology.RIVER_OF_KUUN || Nation.getStateReligion(kk, World.this) == Ideology.RIVER_OF_KUUN) mod += 0.33;
					incomeSources.getOrDefault(kk, new Budget()).incomeTribute += t * mod;
					incomeSources.getOrDefault(k, new Budget()).spentTribute += t;
				}
			}
		}

		void applyIncome() {
			for (String k : kingdoms.keySet()) {
				Budget bk = incomeSources.getOrDefault(k, new Budget());
				getNation(k).gold += bk.incomeTax + bk.incomeSea + bk.incomeChurch + bk.incomeTribute - bk.spentTribute;
			}
		}

		void adjustUnrestDueToTaxation() {
			for (String k : kingdoms.keySet()) {
				double taxRate = taxationRates.get(k);
				double unrest = 0;
				if (taxRate <= 1) unrest = (-(1.25 - taxRate) / .25) / 100 * 4;
				else unrest = ((taxRate - 1) / .25 * ((taxRate - 1) / .25 + 1) / 2) / 100 * 4;
				for (Region r : regions) if (r.getKingdom() != null && r.getKingdom().equals(k)) r.unrestPopular.add(unrest);
			}
		}

		void transferFood() {
			for (String k : orders.keySet()) {
				Map<String, String> kOrders = orders.get(k);
				TreeSet<Integer> transfers = new TreeSet<>();
				for (String o : kOrders.keySet()) {
					if (o.startsWith("economy_amount_")) transfers.add(Integer.parseInt(o.replace("economy_amount_", "")));
				}
				for (Integer i : transfers) {
					String o = "economy_amount_" + i;
					String fromName = kOrders.get(o.replace("amount", "from"));
					String toName = kOrders.get(o.replace("amount", "to"));
					toName = toName.substring(toName.indexOf(") ") + 2, toName.length());
					Region from = null;
					Region to = null;
					for (Region r : regions) {
						if (r.isSea()) continue;
						if (r.name.equals(fromName)) from = r;
						if (r.name.equals(toName)) to = r;
					}
					if (from == null || to == null) throw new RuntimeException("Can't find region " + fromName + " or " + toName);
					if (from == to) continue;
					if (!k.equals(from.getKingdom())) {
						notifyPlayer(k, "Food Transfer from " + from.name + " Failed", "We lost control of the region before the food could be transferred from it!");
						continue;
					}
					double amount = Math.max(0, Math.min(from.food, Double.parseDouble(kOrders.get(o)) * 1000));
					if (amount == 0) continue;
					if (from.getFoodPinned() || to.getFoodPinned()) {
						notifyPlayer(k, "Food Transfer from " + from.name + " Failed", "Due to a nefarious plot, our orders to transfer food from " + from.name + " to " + to.name + " were lost!");
						continue;
					}
					double cost = amount / 50000;
					if (Nation.getStateReligion(k, World.this) == Ideology.CHALICE_OF_COMPASSION) cost = 0;
					if (cost > getNation(k).gold) {
						amount *= getNation(k).gold / cost;
						cost = getNation(k).gold;
					}
					if (!from.canFoodTransferTo(World.this, to)) throw new RuntimeException("Can't transfer food from " + fromName + " to " + toName);
					notifyPlayer(to.getKingdom(), "Food Transfer", Math.round(amount / 1000) + "k measures of food were transferred from " + from.name + " to " + to.name);
					getNation(k).gold -= cost;
					incomeSources.getOrDefault(k, new Budget()).spentFoodTransfers += cost;
					from.food -= amount;
					to.food += amount;
				}
			}
		}

		void payTroops() {
			HashMap<String, Double> payments = new HashMap<String, Double>();
			for (Army a : armies) {
				double cost = a.getCost(World.this, orders.getOrDefault(a.kingdom, new HashMap<String, String>()).getOrDefault("action_army_" + a.id, ""));
				if (cost == 0) continue;
				if ("Pirate".equals(a.kingdom)) {
					if (a.gold < cost) {
						a.gold -= cost;
					} else {
						double deserting = (1 - (a.gold / cost)) * a.size * 0.33;
						a.size -= deserting;
						regions.get(a.location).population += deserting;
						a.gold = 0;
					}
					continue;
				}
				payments.put(a.kingdom, payments.getOrDefault(a.kingdom, 0.0) + cost);
			}
			for (String k : payments.keySet()) {
				double owed = payments.get(k);
				if (owed > getNation(k).gold) {
					double desertion = (1 - (getNation(k).gold / owed)) / 3.0;
					incomeSources.getOrDefault(k, new Budget()).spentSoldiers = getNation(k).gold;
					getNation(k).gold = 0;
					for (Army a : armies) if (a.kingdom.equals(k)) {
						if (a.hasTag(Army.Tag.HIGHER_POWER)) continue;
						a.size *= 1 - desertion;
						double threatIncrease = a.getEffectiveSize() * desertion / 100;
						pirate.threat += threatIncrease;
						pirate.bribes.put(a.kingdom, pirate.bribes.getOrDefault(a.kingdom, 0.0) + threatIncrease / 2);
						pirateThreatSources.put("Desertion from " + a.kingdom, pirateThreatSources.getOrDefault("Desertion from " + a.kingdom, 0.0) + threatIncrease);
					}
					notifyPlayer(k, "Desertion", Math.round(desertion * 100) + "% of our troops deserted due to lack of pay.");
				} else {
					incomeSources.getOrDefault(k, new Budget()).spentSoldiers = owed;
					getNation(k).gold -= owed;
				}
			}
		}

		void reapHarvests() {
			Set<String> stoicNations = new HashSet<>();
			for (String k : kingdoms.keySet()) if (kingdoms.get(k).hasTag(Nation.Tag.STOIC)) stoicNations.add(k);
			if (Season.isHarvest(date)) {
				for (Region r : regions) r.harvest(stoicNations, World.this);
			}
			for (Region r : regions) r.plant(date);
		}

		void cedeRegions() {
			for (int i = 0; i < regions.size(); i++) {
				Region r = regions.get(i);
				if (!r.isLand()) continue;
				if (r.noble != null) continue;
				String c = orders.getOrDefault(r.getKingdom(), new HashMap<String, String>()).getOrDefault("nations_cede_" + i, "(Nobody)");
				if (kingdoms.containsKey(c)) {
					if (getNation(c).getRelationship(r.getKingdom()).cede != Relationship.Cede.ACCEPT) {
						notifyPlayer(r.getKingdom(), "Cannot Cede " + r.name, c + " refused to accept rulership of " + r.name + ".");
						notifyPlayer(c, r.name + " Cede Refused", r.getKingdom() + " attempted to cede rulership of " + r.name + " to us, but we refused.");
						continue;
					}
					notifyAllPlayers("Region Ceded", r.name + ", formerly a region of " + r.getKingdom() + ", has been ceded to " + c + ".");
					r.setKingdom(World.this, c);
				}
			}
		}

		void recruitTroops() {
			for (int i = 0; i < regions.size(); i++) {
				Region r = regions.get(i);
				if (Nation.UNRULED_NAME.equals(r.getKingdom())) continue;
				int shipyards = 0;
				for (Construction c: r.constructions) {
					if (c.type == Construction.Type.SHIPYARD) shipyards++;
				}
				if (shipyards > 0) {
					Army max = getMaxArmyInRegion(i, leaders, inspires);
					if (max == null || !Nation.isEnemy(r.getKingdom(), max.kingdom, World.this)) {
						double shipRate = Math.min(getRules().numShipsBuiltPerShipyard, Math.max(0, Integer.parseInt(orders.getOrDefault(r.getKingdom(), new HashMap<String, String>()).getOrDefault("economy_ship", "5"))));
						buildShips(r.getKingdom(), i, shipyards * shipRate);
						double profit = shipyards * (getRules().numShipsBuiltPerShipyard - shipRate) * getRules().shipSellProfit;
						getNation(r.getKingdom()).gold += profit;
						incomeSources.get(r.getKingdom()).incomeShipSales += profit;
					}
				}
			}
			for (String k : kingdoms.keySet()) {
				double signingBonus = Double.parseDouble(orders.getOrDefault(k, new HashMap<String, String>()).getOrDefault("economy_recruit_bonus", "0"));
				double soldiers = 0;
				double likelyRecruits = 0;
				for (Army a : armies) if (k.equals(a.kingdom) && !a.hasTag(Army.Tag.HIGHER_POWER)) soldiers += a.size;
				for (Region r : regions) if (k.equals(r.getKingdom())) likelyRecruits += r.calcRecruitment(World.this, null, signingBonus, rationing.getOrDefault(r.getKingdom(), 1.0), getMaxArmyInRegion(regions.indexOf(r), leaders, inspires));
				if (signingBonus > 0 && (soldiers + likelyRecruits) / 100 * signingBonus > getNation(k).gold) signingBonus = getNation(k).gold * 100 / (soldiers + likelyRecruits);
				if (signingBonus > 0 && signingBonus < 1) signingBonus = 0;
				if (signingBonus > 0) {
					for (Army a : armies) if (k.equals(a.kingdom) && !a.hasTag(Army.Tag.HIGHER_POWER)) {
						getNation(k).gold -= signingBonus * a.size / 100;
						incomeSources.getOrDefault(k, new Budget()).spentRecruits += signingBonus * a.size / 100;
					}
				}
				for (int i = 0; i < regions.size(); i++) {
					Region r = regions.get(i);
					if (r.isSea()) continue;
					if (!r.getKingdom().equals(k)) continue;
					double recruits = r.calcRecruitment(World.this, null, signingBonus, rationing.getOrDefault(r.getKingdom(), 1.0), getMaxArmyInRegion(regions.indexOf(r), leaders, inspires));
					if (recruits <= 0) continue;
					if (signingBonus > 0) {
						getNation(k).gold -= signingBonus * recruits / 100;
						incomeSources.getOrDefault(k, new Budget()).spentRecruits += signingBonus * recruits / 100;
					}
					List<Army.Tag> tags = r.getArmyTags();
					Army merge = null;
					outer: for (Army a : armies) {
						if (a.location == i && a.isArmy() && a.kingdom.equals(k)) {
							for (Army.Tag t : tags) if (!a.hasTag(t)) continue outer;
							merge = a;
							break;
						}
					}
					if (merge != null) {
						merge.size += recruits;
					} else {
						Army army = Army.newArmy(getRules());
						army.type = Army.Type.ARMY;
						army.size = recruits;
						army.tags.addAll(tags);
						army.location = i;
						army.id = getNewArmyId();
						army.kingdom = k;
						army.composition.put("r_" + army.location, army.size);
						army.orderhint = "";
						armies.add(army);
					}
					addPopulation(r, -recruits);
				}
			}
		}

		private class BudgetLine {
			public final double amount;
			public final String source;
			public BudgetLine(double amount, String source) {
				this.amount = amount;
				this.source = source;
			}
		}

		void sendBudgetNotifications() {
			for (String k : kingdoms.keySet()) {
				Budget b = incomeSources.getOrDefault(k, new Budget());
				String notification = "Our treasurer reports a net change in the treasury of " + Math.round(b.sum()) + " gold:";
				notification += "\n" + Math.round(b.start) + " gold treasured prior to this week.";
				double amt = b.start;
				ArrayList<Supplier<BudgetLine>> lines = new ArrayList<>();
				lines.add(() -> new BudgetLine(-b.spentGift, " sent to other nations"));
				lines.add(() -> new BudgetLine(b.incomeGift, " received from other nations"));
				lines.add(() -> new BudgetLine(b.incomeIntrigue, " earned by plots"));
				lines.add(() -> new BudgetLine(-b.spentIntrigue, " lost due to plots"));
				lines.add(() -> new BudgetLine(-b.spentConstruction, " spent constructing"));
				lines.add(() -> new BudgetLine(-b.spentSpyEstablishments, " spent establishing spy rings"));
				lines.add(() -> new BudgetLine(-b.spentBribes, " spent bribing pirates"));
				lines.add(() -> new BudgetLine(-b.spentFoodTransfers, " spent transferring food"));
				lines.add(() -> new BudgetLine(-b.spentSoldiers, " spent paying our soldiers and sailors"));
				lines.add(() -> new BudgetLine(-b.spentRecruits, " spent hiring new recruits"));
				lines.add(() -> new BudgetLine(b.incomeArmyDelivery, " delivered by our armies"));
				lines.add(() -> new BudgetLine(b.incomeTax, " earned from taxation"));
				lines.add(() -> new BudgetLine(b.incomeSea, " earned from sea trade"));
				lines.add(() -> new BudgetLine(b.incomeChurch, " donated to us by the Church of Iruhan"));
				lines.add(() -> new BudgetLine(b.incomeShipSales, " earned from shipyards"));
				lines.add(() -> new BudgetLine(b.incomeTribute, " gained from tribute"));
				lines.add(() -> new BudgetLine(-b.spentTribute, " spent on tribute"));

				for (Supplier<BudgetLine> lineItemSupplier : lines) {
					BudgetLine item = lineItemSupplier.get();
					amt += item.amount;
					if (item.amount != 0) notification += "\n" + Math.round(amt) + " (" + (item.amount > 0 ? "+" : "") + Math.round(item.amount) + item.source + ")";
				}
				notifyPlayer(k, "Budget", notification);
			}
		}

		void eatFood() {
			for (int i = 0; i < regions.size(); i++) {
				Region r = regions.get(i);
				String coreRegionOf = "Unruled";
				for (String k : kingdoms.keySet()) if (getNation(k).coreRegions.contains(i)) coreRegionOf = k;
				r.eat(rationing.getOrDefault(r.getKingdom(), 1.0), coreRegionOf, World.this);
			}
		}

		void growPopulations() {
			// Emigration
			class Emigration {
				final Region source;
				final Region destination;
				final double population;

				public Emigration(Region source, Region destination, double population) {
					this.source = source;
					this.destination = destination;
					this.population = population;
				}

				void apply() {
					addPopulation(source, -population);
					addPopulation(destination, population);
				}
			}
			List<Emigration> emigrations = new ArrayList<>();
			for (Region r : regions) {
				if (r.isSea()) continue;
				double eligibleToEmigrate = r.population * r.unrestPopular.get() * getRules().emigrationFactor;
				double mod = 1;
				if (getNation(r.getKingdom()).hasTag(Nation.Tag.STOIC)) mod -= 0.75;
				eligibleToEmigrate = Math.min(eligibleToEmigrate * mod, r.population - 1); // The last person never emigrates.
				Set<Region> destinations = new HashSet<Region>();
				for (Region n : r.getNeighbors(World.this)) {
					if (n.isLand()) destinations.add(n);
					else for (Region nn : n.getNeighbors(World.this)) {
						if (nn != r && nn.isLand()) destinations.add(nn);
					}
				}
				destinations.removeIf(d -> d.unrestPopular.get() >= r.unrestPopular.get());
				destinations.removeIf(d -> !d.getKingdom().equals(r.getKingdom()) && (patrolledRegions.contains(r) || getNation(d.getKingdom()).getRelationship(r.getKingdom()).refugees == Relationship.Refugees.REFUSE));
				if (destinations.isEmpty() || eligibleToEmigrate < 1) continue;
				double totalWeight = 0;
				for (Region d : destinations) totalWeight += d.calcImmigrationWeight(World.this);
				for (Region d : destinations) {
					emigrations.add(new Emigration(r, d, d.calcImmigrationWeight(World.this) / totalWeight * eligibleToEmigrate));
				}
			}
			Map<Region, List<Emigration>> relevantEmigrations = new HashMap<>();
			for (Emigration e : emigrations) {
				relevantEmigrations.merge(e.source, Arrays.asList(e), (a, b) -> { List<Emigration> r = new ArrayList<>(a); r.addAll(b); return r; });
				relevantEmigrations.merge(e.destination, Arrays.asList(e), (a, b) -> { List<Emigration> r = new ArrayList<>(a); r.addAll(b); return r; });
				e.apply();
			}
			for (String k : kingdoms.keySet()) {
				double popChange = 0;
				double absPopChange = 0;
				String note = "";
				for (Map.Entry<Region, List<Emigration>> e : relevantEmigrations.entrySet()) {
					if (!e.getKey().getKingdom().equals(k)) continue;
					String explain = "";
					double localTotal = 0;
					for (Emigration ee : e.getValue()) {
						double delta = ee.population * (ee.source == e.getKey() ? -1 : 1);
						explain += "\n&nbsp&nbsp&nbsp&nbsp(" + (delta > 0 ? "+" : "") + Math.round(delta) + " from " + (ee.source == e.getKey() ? ee.destination.name : ee.source.name) + ")";
						popChange += delta;
						localTotal += delta;
						absPopChange += ee.population;
					}
					note += "\n" + e.getKey().name + ": " + (localTotal > 0 ? "+" : "") + Math.round(localTotal) + " people." + explain;
				}
				if (absPopChange > 1000) {
					notifyPlayer(k, "Emigration / Immigration", "The population of regions you rule has changed by " + Math.round(popChange / 1000) + "k due to voluntarily population migrations:\n" + note);
				}
			}

			// Growth
			for (Region r : regions) r.grow();
		}

		private void damageCrops(int spellDuration) {
			spellDuration -= getRules().tivarSpellGracePeriod;
			if (spellDuration <= 0) return;
			double destruction = spellDuration * getRules().tivarSpellCropDestruction;
			if (destruction > 1) destruction = 1;
			for (Region r : regions) r.crops *= 1 - destruction;
		}

		void evaluateGothiSpells() {
			if (tivar.warwinds != 0) {
				HashSet<Character> moved = new HashSet<>();
				for (Army a : armies) {
					if (a.hasTag(Army.Tag.WEATHERED)) continue;
					Region r = regions.get(a.location);
					if (r.isSea()) {
						a.size *= 0.75;
						List<Region> n = new ArrayList<>(r.getNeighbors(World.this));
						Region d = n.get((int)(Math.random() * n.size()));
						a.location = regions.indexOf(d);
						Character leader = leaders.get(a);
						if (leader != null) {
							moved.add(leader);
							leader.location = a.location;
						}
					}
				}
				for (Character c : characters) {
					if (moved.contains(c)) continue;
					Region r = regions.get(c.location);
					if (r.isSea()) {
						moved.add(c);
						List<Region> n = new ArrayList<>(r.getNeighbors(World.this));
						c.location = regions.indexOf(n.get((int)(Math.random() * n.size())));
					}
				}
			}
			if (tivar.quake != 0) {
				Map<String, Map<Construction.Type, Integer>> wreckage = new HashMap<>();
				for (Region r : regions) {
					ArrayList<Construction> destroyed = new ArrayList<>();
					for (Construction c : r.constructions) {
						if (Math.random() < 0.33) destroyed.add(c);
					}
					for (Construction c : destroyed) {
						r.constructions.remove(c);
						if (!wreckage.containsKey(r.getKingdom())) wreckage.put(r.getKingdom(), new HashMap<Construction.Type, Integer>());
						wreckage.get(r.getKingdom()).put(c.type, wreckage.get(r.getKingdom()).getOrDefault(c.type, 0) + 1);
					}
					r.setReligion(null, World.this);
				}
				for (String k : wreckage.keySet()) {
					String notification = "The terrible magical earthquakes triggered by the followers of Rjinku have taken their toll on our nation, destroying:";
					for (Construction.Type t : wreckage.get(k).keySet()) {
						notification += "\n" + StringUtil.quantify(wreckage.get(k).get(t), t.toString().toLowerCase());
					}
					notifyPlayer(k, "Earthquakes", notification);
				}
			}
			damageCrops(tivar.deluge);
			damageCrops(tivar.quake);
			damageCrops(tivar.veil);
			damageCrops(tivar.warwinds);
		}

		void nobleCrises() {
			for (Region r : regions) {
				if (r.noble == null) continue;
				r.noble.resolveCrisis(World.this, r, leaders, governors, builds, templeBuilds, rationing, inspires).ifPresent(n -> notifyPlayer(n.who, n.title, n.text));
				r.noble.createCrisis(World.this, r, leaders, governors, builds, templeBuilds, rationing, inspires).ifPresent(n -> notifyPlayer(n.who, n.title, n.text));
			}
		}

		void checkCultVictory() {
			armies.stream().filter(a -> a.hasTag(Army.Tag.HIGHER_POWER)).map(a -> a.location).forEach(rid -> regions.get(rid).cultAccess(kingdoms.values(), cultRegions.contains(rid)));
			Set<String> cultists = getNationNames().stream().filter(n -> getNation(n).loyalToCult).collect(Collectors.toSet());
			regions.stream().filter(r -> cultists.contains(r.getKingdom())).forEach(r -> r.cultAccess(kingdoms.values(), cultRegions.contains(regions.indexOf(r))));
			if (!cultTriggered && cultRegions.stream().filter(id -> regions.get(id).hasBeenCultAccessed()).count() >= cultRegions.size() * getRules().cultEventTriggerFraction) {
				HashMap<String, Double> higherPowerCount = new HashMap<>();
				armies.stream().filter(a -> a.hasTag(Army.Tag.HIGHER_POWER)).forEach(a -> higherPowerCount.put(a.kingdom, higherPowerCount.getOrDefault(a.kingdom, 0.0) + a.size));
				Optional<String> overlord = getNationNames().stream().filter(n -> getNation(n).loyalToCult).max(Comparator.comparingDouble(a -> higherPowerCount.getOrDefault(a, 0.0)));
				if (!overlord.isPresent()) return;
				cultTriggered = true;
				Nation overlordNation = getNation(overlord.get());
				// All other Cultist players transfer all regions / armies / navies / non-Ruler characters, then lose the Cultist profile.
				armies.stream().filter(a -> cultists.contains(a.kingdom)).forEach(a -> a.kingdom = overlord.get());
				armies.stream().filter(a -> a.hasTag(Army.Tag.HIGHER_POWER)).forEach(a -> a.kingdom = overlord.get());
				characters.stream().filter(c -> !c.hasTag(Character.Tag.RULER)).filter(c -> cultists.contains(c.kingdom)).forEach(c -> c.kingdom = overlord.get());
				regions.stream().filter(r -> cultists.contains(r.getKingdom())).forEach(r -> r.setKingdom(World.this, overlord.get()));
				getNationNames().stream().forEach(k -> getNation(k).removeProfile(Nation.ScoreProfile.CULTIST));
				// The overlord loses all score profiles, gains Territory, are marked to turn off Reflect.
				overlordNation.clearProfiles();
				overlordNation.toggleProfile(Nation.ScoreProfile.TERRITORY);
				overlordNation.lockScoreProfiles();
				notifyAllPlayers("Cult Rises", "The Cult of the Witness has drawn close to accomplishing their mysterious objective! Agents of the Cult throughout the world reveal themselves, establishing de facto rule under " + overlord.get() + ". Other rulers loyal to the Cult are discarded, having outlived their usefulness, and are forced to flee for their lives with nothing but the clothes on their backs.");
			}
		}

		void miscScoreProfiles() {
			// Cultist
			getNationNames().stream().map(n -> getNation(n)).filter(n -> n.loyalToCult).forEach(n -> n.score(Nation.ScoreProfile.CULTIST, getRules().scoreCultistPerTurnPenalty));

			// Happiness
			for (String k : kingdoms.keySet()) {
				double below25 = 0;
				double below35 = 0;
				double above25 = 0;
				double above50 = 0;
				double pop = 0;
				for (Region r : regions) if (k.equals(r.getKingdom())) {
					double unrest = r.calcUnrest(World.this);
					if (unrest < .25) below25 += r.population;
					if (unrest < .35) below35 += r.population;
					if (unrest > .25) above25 += r.population;
					if (unrest > .50) above50 += r.population;
					pop += r.population;
				}
				if (pop > 0) {
					if (below25 > pop * 0.99999) score(k, Nation.ScoreProfile.HAPPINESS, 1);
					if (below35 > pop * 0.9) score(k, Nation.ScoreProfile.HAPPINESS, 1);
					if (above25 > pop * 0.25) score(k, Nation.ScoreProfile.HAPPINESS, -1);
					if (above50 > pop * 0.33) score(k, Nation.ScoreProfile.HAPPINESS, -2);
				}
			}
			// Riches
			for (String k : kingdoms.keySet()) {
				Nation n = getNation(k);
				if (n.gold > 2000) n.score(Nation.ScoreProfile.RICHES, 2);
				if (n.gold > 500) n.score(Nation.ScoreProfile.RICHES, 1);
				if (n.gold < 300) n.score(Nation.ScoreProfile.RICHES, -1);
			}
		}

		void appointHeirs() {
			HashSet<String> unruled = new HashSet<>();
			for (String k : kingdoms.keySet()) unruled.add(k);
			for (Character c : characters) if (c.hasTag(Character.Tag.RULER)) unruled.remove(c.kingdom);
			for (String k : unruled) {
				ArrayList<Character> cc = new ArrayList<Character>();
				for (Character c : characters) if (c.kingdom.equals(k)) cc.add(c);
				if (cc.isEmpty()) {
					cc.add(makeNewCharacter(k));
				}
				Character c = cc.get((int)(Math.random() * cc.size()));
				c.honorific = "Protector ";
				c.addTag(Character.Tag.RULER);
				notifyAllPlayers(k + " Succession", c.name + " has emerged as the new de facto ruler of " + k + ".");
			}
		}

		void removeInvalidPlots() {
			plots.removeIf(p -> !p.isValid(World.this));
		}

		void notifyPirateThreats() {
			double totalIncrease = 0;
			for (String k : pirateThreatSources.keySet()) {
				totalIncrease += pirateThreatSources.get(k);
			}
			if (totalIncrease > 0) {
				String sources = "";
				for (String k : pirateThreatSources.keySet()) {
					sources += "\n" + k + ": " + Math.round(pirateThreatSources.get(k) / totalIncrease * 100) + "%";
				}
				notifyAllPlayers("Piracy", "Pirate Threat has increased by " + Math.round(totalIncrease * 100) + " pirates. The increase was driven by: " + sources);
			}
		}

		void notifyInspires() {
			inspiresHint = inspires;
		}

		void advanceDate() {
			date++;
			nextTurn = turnSchedule.getNextTime();
			for (String k : kingdoms.keySet()) getNation(k).previousTributes = tributes.get(k);

			// Notify of upcoming harvest.
			if (Season.isHarvest(date)) {
				notifyAllPlayers("Harvest Preparations", "People all over the world prepare to reap the harvest upon which they have labored, and hunger is temporarily banished. All regions will produce their harvest this turn (before eating).");
			}

			Season season = Season.get(date);
			if (season != Season.get(date - 1)) {
				if (season == Season.WINTER) notifyAllPlayers("Winter Arrives", "It is now winter. Icy winds blow from the north, carrying snow and hail. Fewer crops are planted in winter than in autumn.");
				else if (season == Season.SPRING) notifyAllPlayers("Spring Arrives", "It is now spring. The days length, and the natural world wakes from hibernation. More crops are planted during the spring than during autumn or winter.");
				else if (season == Season.SUMMER) notifyAllPlayers("Summer Arrives", "It is now summer. The people celebrate the warm weather and long days. The most crops of any season are planted during summer.");
				else if (season == Season.AUTUMN) notifyAllPlayers("Autumn Arrives", "It is now autumn. The days shorten and the weather cools. The fat months of summer have passed, and the people look toward a lean winter.");
			}
		}

		Map<String, String> checkGameEnd() {
			// Check for game end.
			if (date >= 27 && (date - 27) % 6 == 0) {
				int votesToEnd = 0;
				double totalVotes = 0;
				for (String kingdom : kingdoms.keySet()) {
					if ("end".equals(orders.get(kingdom).getOrDefault("end_vote", "end"))) votesToEnd++;
					totalVotes++;
				}
				if (votesToEnd / totalVotes > 1.0 / 3.0) {
					// Game ends. Early return.
					gameover = true;
					HashMap<String, String> emails = new HashMap<>();
					for (String kingdom : kingdoms.keySet()) emails.put(getNation(kingdom).email, "The curtain has closed on this stage of history. Your decisions, and those of your fellow rulers, good and bad, have set the course for the known world and its inhabitants.\n\nTo review the final state of the game, as well as any letters you received from the final turn, you can view https://pawlicki.kaelri.com/empire/map1.html?g=%GAMEID%.\n\nThank you for playing and we hope to see you again soon!");
					return emails;
				}
			}
			return null;
		}

		Map<String, String> prepareEmails() {
			HashMap<String, String> emails = new HashMap<>();
			for (String kingdom : kingdoms.keySet()) {
				if (!getNation(kingdom).sendEmail) continue;
				String ruler = null;
				for (Character c : characters) if (c.kingdom.equals(kingdom) && c.hasTag(Character.Tag.RULER)) ruler = c.honorific + " " + c.name;
				if (ruler == null) ruler = "Ruler of " + kingdom;
				String email = ruler + ", a week has passed and your people again need your leadership.\n\nYour advisers wish to notify you of: ";
				for (Notification n : notifications) if (kingdom.equals(n.who)) email += "\n " + n.title;
				HashSet<Communication> letters = new HashSet<>();
				for (Communication m : communications) if (m.to.contains(kingdom) && m.postDate == date - 1) letters.add(m);
				if (!letters.isEmpty()) {
					email += "\n\nYou have received letters from:";
					for (Communication m : letters) {
						email += "\n " + m.signed.replace("Signed, ", "");
					}
				}
				email += "\n\nYou can issue your orders at https://pawlicki.kaelri.com/empire/map1.html?g=%GAMEID%.\nYou can silence these notifications by logging in and unchecking the \"Send Email\" box in the game settings.";
				emails.put(getNation(kingdom).email, email);
			}
			return emails;
		}
	}

	private void addCultCache(double size, String kingdom, int location) {
		for (CultCache c : cultCaches) {
			if (c.getLocation() == location && c.isEligible(kingdom)) {
				c.grow(size);
				return;
			}
		}
		cultCaches.add(new CultCache(size, kingdom, location));
	}

	private void addPopulation(Region r, double amount) {
		r.population = Math.max(1, r.population + amount);
	}

	private int getNewArmyId() {
		int nextId = 1;
		for (Army aa : armies) if (aa.id >= nextId) nextId = aa.id + 1;
		return nextId;
	}

	private boolean getAttrition(Army army, Region region) {
		if (army.hasTag(Army.Tag.WEATHERED)) return false;
		if (region.isLand() && tivar.deluge != 0) return true;
		return false;
	}

	private void sortByStrength(List<Army> actors, Map<Army, Character> leaders, int finspires) {
		Collections.sort(actors, (Army a, Army b) -> {
			double as = a.calcStrength(this, leaders.get(a), finspires);
			double bs = b.calcStrength(this, leaders.get(b), finspires);
			return as > bs ? -1 : as < bs ? 1 : 0;
		});
	}

	private void buildShips(String kingdom, int location, double amount) {
		if (amount == 0) return;
		for (Army a : armies) {
			if (a.isNavy() && a.location == location && a.kingdom.equals(kingdom)) {
				a.size += amount;
				return;
			}
		}
		Army a = Army.newArmy(getRules());
		a.type = Army.Type.NAVY;
		a.size = amount;
		a.location = location;
		a.kingdom = kingdom;
		a.tags = new ArrayList<>();
		a.id = getNewArmyId();
		a.orderhint = "";
		armies.add(a);
	}

	public Ideology getDominantIruhanIdeology() {
		HashMap<Ideology, Double> pop = new HashMap<>();
		for (Region r : regions) {
			if (r.religion != null && r.religion.religion == Religion.IRUHAN) {
				pop.put(r.religion, pop.getOrDefault(r.religion, 0.0) + r.population);
			}
		}
		return getMaxKey(pop);
	}

	private <T, V extends Comparable<V>> T getMaxKey(Map<T, V> m) {
		T max = null;
		for (T key : m.keySet()) {
			if (max == null || m.get(key).compareTo(m.get(max)) > 0) max = key;
		}
		return max;
	}

	private void sendRefugees(Region from, Region to, double amount, boolean notify, boolean foodOnly) {
		ArrayList<Region> destinations = new ArrayList<>();
		amount = Math.min(amount, from.population - 1);
		if (amount <= 0) return;
		if (to == null) {
			for (Region r : from.getNeighbors(this)) if (r.isLand() && (r.food > 0 || !foodOnly) && (r.getKingdom().equals(from.getKingdom()) || getNation(r.getKingdom()).getRelationship(from.getKingdom()).refugees == Relationship.Refugees.ACCEPT)) destinations.add(r);
		} else {
			destinations.add(to);
		}
		for (Region target : destinations) {
			double targetUnrestFactor = 1;
			double fromUnrestFactor = 1;
			if (Ideology.CHALICE_OF_COMPASSION == getDominantIruhanIdeology()) {
				if (target.religion.religion == Religion.IRUHAN) targetUnrestFactor = 0;
				if (from.religion.religion == Religion.IRUHAN) fromUnrestFactor = 0;
			}
			addPopulation(target, amount / destinations.size());
			addPopulation(from, -amount / destinations.size());
			if (notify) notifyPlayer(target.getKingdom(), "Refugees Arrive in " + target.name, "Refugees from " + from.name + " have arrived in " + target.name + "." + (targetUnrestFactor > 0 ? " They are distraught and upset, causing increased popular unrest." : ""));
		}
	}

	Geography getGeography() {
		return geography;
	}

	void notifyAllPlayers(String title, String notification) {
		for (String k : kingdoms.keySet()) notifyPlayer(k, title, notification);
	}

	void notifyPlayer(String kingdom, String title, String notification) {
		Nation recipient = getNation(kingdom);
		if (recipient == null || recipient == Nation.PIRATE || recipient == Nation.UNRULED) return;
		Optional<Notification> mergeInto = notifications.stream().filter(i -> i.who.equals(kingdom) && i.title.equals(title)).findFirst();
		if (mergeInto.isPresent()) {
			notifications.remove(mergeInto.get());
			notifications.add(new Notification(kingdom, title, mergeInto.get().text + "\n" + notification));
		} else {
			notifications.add(new Notification(kingdom, title, notification));
		}
	}

	void score(String kingdom, Nation.ScoreProfile profile, double amount) {
		getNation(kingdom).score(profile, amount);
	}

	private Army getMaxArmyInRegion(int location, Map<Army, Character> leaders, int inspires) {
		Army max = null;
		for (Army b : armies) if (b.location == location && b.isArmy()) {
			if (max == null || max.calcStrength(this, leaders.get(max), inspires) < b.calcStrength(this, leaders.get(b), inspires)) max = b;
		}
		return max;
	}

	private boolean isHiddenAlyrjaHelper(int loc, String kingdom) {
		HashSet<Region> unitLocs = new HashSet<>();
		for (Army a : armies) if (a.kingdom.equals(kingdom)) unitLocs.add(regions.get(a.location));
		for (Character c : characters) if (c.kingdom.equals(kingdom)) unitLocs.add(regions.get(c.location));
		List<Region> r = new ArrayList<Region>(regions.get(loc).getNeighbors(this));
		r.add(regions.get(loc));
		for (Region rr : r) {
			if (unitLocs.contains(rr)) return true;
		}
		return false;
	}

	private boolean isHidden(Army a, String kingdom) {
		if (a.kingdom.equals(kingdom)) return false;
		if (Nation.getStateReligion(kingdom, this) == Ideology.ALYRJA) {
			if (isHiddenAlyrjaHelper(a.location, kingdom)) return false;
		}
		if (tivar.veil != 0) return true;
		if (!a.hasTag(Army.Tag.RAIDERS) || regions.get(a.location).getKingdom() == null || kingdom.equals(regions.get(a.location).getKingdom()) || !Nation.isFriendly(a.kingdom, regions.get(a.location).getKingdom(), this)) return false;
		return true;
	}

	private boolean isHidden(Character c, String kingdom) {
		if (c.kingdom.equals(kingdom)) return false;
		if (Nation.getStateReligion(kingdom, this) == Ideology.ALYRJA) {
			if (isHiddenAlyrjaHelper(c.location, kingdom)) return false;
		}
		if (c.hidden) return true;
		if (c.leadingArmy != -1) {
			Army a = null;
			for (Army i : armies) if (i.id == c.leadingArmy) a = i;
			if (a != null && isHidden(a, kingdom)) return true;
		}
		if (tivar.veil != 0) return true;
		return false;
	}

	// Filter the data to a specific kingdom's point of view.
	public void filter(String kingdom) {
		filteredTo = kingdom;
		for (String k : kingdoms.keySet()) {
			getNation(k).filterForView(!kingdom.equals(k));
		}
		// Filter out cult regions.
		cultRegions = null;
		// Filter out gold.
		for (String k : kingdoms.keySet()) {
			if (k.equals(kingdom)) continue;
			getNation(k).gold = -1;
		}
		// Filter notifications.
		ArrayList<Notification> remove = new ArrayList<>();
		for (Notification n : notifications) {
			if (!kingdom.equals(n.who)) remove.add(n);
		}
		for (Notification n : remove) notifications.remove(n);
		// Filter plots. Plots disclose some details about spy rings in their power hints, so this must be done before filtering spy rings.
		plots.removeIf(p -> !p.hasConspirator(kingdom));
		for (Plot p : plots) p.filter(this, a -> a.calcStrength(this, characters.stream().filter(c -> c.leadingArmy == a.id).findAny().orElse(null), inspiresHint));
		// Filter spy rings.
		spyRings.removeIf(r -> !r.isExposed() && !r.getNation().equals(kingdom));
		for (SpyRing r : spyRings) if (!r.getNation().equals(kingdom)) {
			r.involve(-1, SpyRing.InvolvementDisposition.SUPPORTING);
		}
		// Filter communications.
		ArrayList<Communication> removeComm = new ArrayList<>();
		for (Communication c : communications) {
			if (c.from.equals(kingdom) || c.to.contains(kingdom) || c.intercepted.contains(kingdom)) {
				if (!c.from.equals(kingdom) && !c.intercepted.contains(kingdom)) c.to = Arrays.asList(kingdom);
			} else {
				removeComm.add(c);
			}
		}
		for (Communication c : removeComm) communications.remove(c);
		for (Communication c : communications) c.from = "";
		for (Communication c : communications) if (c.signed == "Anonymous" && !c.intercepted.contains(kingdom)) {
			c.to.clear();
			c.to.add(kingdom);
		}
		// Filter hidden characters, except with Alyrja.
		for (Character c : characters) {
			if (isHidden(c, kingdom)) c.location = -1;
		}
		// Filter armies, except with Alyrja.
		ArrayList<Army> removeArmy = new ArrayList<Army>();
		for (Army a : armies) if (isHidden(a, kingdom)) removeArmy.add(a);
		for (Army a : removeArmy) armies.remove(a);
		// Filter orderhints.
		for (Army a : armies) if (!a.kingdom.equals(kingdom)) a.orderhint = "";
		for (Character a : characters) if (!a.kingdom.equals(kingdom)) a.orderhint = "";
	}

	@Override
	public double getGoodwill(String nation) {
		return getNation(nation).goodwill;
	}

	public Character makeNewCharacter(String k) {
		Character c = Character.newCharacter(getRules());
		c.name = WorldConstantData.getRandomName(geography.getKingdom(k).culture, Math.random() < 0.5 ? WorldConstantData.Gender.MAN : WorldConstantData.Gender.WOMAN);
		c.kingdom = k;
		c.addExperienceGeneral();
		c.addExperienceAdmiral();
		c.addExperienceSpy();
		c.addExperienceGovernor();
		List<Region> spawnRegions = new ArrayList<>();
		for (Region r : regions) if (k.equals(r.getKingdom())) spawnRegions.add(r);
		if (spawnRegions.isEmpty()) spawnRegions = regions;
		c.location = regions.indexOf(spawnRegions.get((int)(Math.random() * spawnRegions.size())));
		characters.add(c);
		return c;
	}

	public Optional<String> getNationName(String email) {
		for (Map.Entry<String, Nation> n : kingdoms.entrySet()) if (n.getValue().email.equals(email)) return Optional.of(n.getKey());
		return Optional.empty();
	}
	
	private World(Rules rules) {
		super(rules);
	}

	static World newWorld(Rules rules) {
		return new World(rules);
	}
}

final class Preparation {
	int to = -1;
	int amount = -1;

	public Preparation() {}
	public Preparation(Preparation c) {
		this.to = c.to;
		this.amount = c.amount;
	}
}

final class Communication {
	String from = "";
	String signed = "";
	List<String> to = new ArrayList<>();
	Set<String> intercepted = new HashSet<>();
	String text = "";
	int postDate = -1;
}

class Pirate {
	double threat;
	Map<String, Double> bribes = new HashMap<>();
}

final class Tivar {
	int warwinds;
	int deluge;
	int quake;
	int veil;
}

final class Notification {
	String who;
	String title;
	String text;

	public Notification(String who, String title, String text) {
		this.who = who;
		this.title = title;
		this.text = text;
	}
}
