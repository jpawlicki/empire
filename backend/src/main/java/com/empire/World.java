package com.empire;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;
import com.google.common.primitives.Ints;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

interface GoodwillProvider {
	double getGoodwill(String nation);
}

class World implements GoodwillProvider {
	private static final String TYPE = "World";
	private static final Logger log = Logger.getLogger(World.class.getName());

	int date;
	private Map<String, NationData> kingdoms = new HashMap<>();
	List<Region> regions = new ArrayList<>();
	List<Army> armies = new ArrayList<>();
	List<Character> characters = new ArrayList<>();
	List<Communication> communications = new ArrayList<>();
	Pirate pirate = new Pirate();
	Tivar tivar = new Tivar();
	String gmPasswordHash;
	String obsPasswordHash;
	List<Notification> notifications = new ArrayList<>();
	List<Message> rtc = new ArrayList<>();
	List<Integer> cultRegions = new ArrayList<>();
	Schedule turnSchedule = new Schedule();
	int inspiresHint;
	int ruleSet;
	long nextTurn;
	boolean gameover;

	private static Gson getGson() {
		return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
	}

	private static String loadJson(long gameId, int turn, DatastoreService service) throws EntityNotFoundException {
		Entity e = service.get(KeyFactory.createKey(TYPE, gameId + "_" + turn));
		if (e.hasProperty("json")) {
			return new String(((Text)e.getProperty("json")).getValue());
		} else {
			return Compressor.decompress(((Blob)e.getProperty("json_gzip")).getBytes());
		}
	}

	public static World load(long gameId, int turn, DatastoreService service) throws EntityNotFoundException {
		return fromJson(loadJson(gameId, turn, service));
	}

	public static World fromJson(String json) {
		return getGson().fromJson(json, World.class);
	}

	public static World startNew(String gmPasswordHash, String obsPasswordHash, Map<String, Nation.NationGson> nationSetup) {
		World w = new World();
		w.date = 1;
		w.gmPasswordHash = gmPasswordHash;
		w.obsPasswordHash = obsPasswordHash;
		w.turnSchedule.days = new ArrayList<>();
		w.turnSchedule.days.add(DayOfWeek.MONDAY);
		w.turnSchedule.days.add(DayOfWeek.WEDNESDAY);
		w.turnSchedule.days.add(DayOfWeek.FRIDAY);
		w.turnSchedule.time = 150;
		w.turnSchedule.locale = "America/Los_Angeles";
		w.nextTurn = w.turnSchedule.getNextTime();
		w.regions = new ArrayList<>();
		w.pirate.threat = 4;
		// Set up regions (culture, climate, popular unrest).
		for (WorldConstantData.Region r : WorldConstantData.regions) {
			Region rr = new Region();
			rr.climate = r.climate;
			rr.type = r.land ? Region.Type.LAND : Region.Type.WATER;
			rr.name = r.name;
			if (r.land) {
				rr.unrestPopular = 0.12;
			}
			w.regions.add(rr);
		}
		for (WorldConstantData.Kingdom k : WorldConstantData.kingdoms.values()) {
			for (Integer i : k.coreRegions) {
				w.regions.get(i).culture = k.culture;
			}
		}
		while (w.cultRegions.size() < 10) {
			int rid = (int)(Math.random() * w.regions.size());
			while (w.regions.get(rid).isSea() || w.cultRegions.contains(rid)) rid = (int)(Math.random() * w.regions.size());
			w.cultRegions.add(rid);
		}
		double totalPopulation = 21000000;
		double totalFood = totalPopulation * 10;
		double totalNavy = totalPopulation / 26000 * 3;
		double totalArmy = totalPopulation / 26000 * 100;
		double totalGold = (totalPopulation / 10000 - totalNavy / 3 - totalArmy / 100) * 5;
		double totalSharesGold = 0;
		double totalSharesArmy = 0;
		double totalSharesNavy = 0;
		int unruledNations = WorldConstantData.kingdoms.size() - nationSetup.keySet().size();
		double totalSharesFood = unruledNations / 2.0;
		double totalSharesPopulation = unruledNations / 2.0;
		for (String kingdom : nationSetup.keySet()) {
			Nation.NationGson setup = nationSetup.get(kingdom);
			totalSharesGold += 1;
			totalSharesArmy += 1;
			totalSharesNavy += 1;
			totalSharesFood += 1;
			totalSharesPopulation += 1;
			if ("Mercantile".equals(setup.trait1) || "Mercantile".equals(setup.trait2)) totalSharesGold += 0.5;
			if ("Patriotic".equals(setup.trait1) || "Patriotic".equals(setup.trait2)) totalSharesArmy += 0.15;
			if ("Rebellious".equals(setup.trait1) || "Rebellious".equals(setup.trait2)) totalSharesArmy += 0.5;
			if ("Rebellious".equals(setup.trait1) || "Rebellious".equals(setup.trait2)) totalSharesGold += 5;
			if ("Ruined".equals(setup.trait1) || "Ruined".equals(setup.trait2)) totalSharesPopulation -= 0.5;
			if ("Ruined".equals(setup.trait1) || "Ruined".equals(setup.trait2)) totalSharesGold += 2;
			if ("Seafaring".equals(setup.trait1) || "Seafaring".equals(setup.trait2)) totalSharesNavy += 0.15;
			if ("War-like".equals(setup.trait1) || "War-like".equals(setup.trait2)) totalSharesArmy += 0.15;
			if ("food".equals(setup.bonus)) totalSharesFood += 0.5;
			else if ("armies".equals(setup.bonus)) totalSharesArmy += 0.5;
			else if ("navies".equals(setup.bonus)) totalSharesNavy += 0.5;
			else if ("gold".equals(setup.bonus)) totalSharesGold += 0.5;
		}
		for (String kingdom : nationSetup.keySet()) {
			Nation.NationGson setup = nationSetup.get(kingdom);
			NationData nation = new NationData();
			WorldConstantData.Kingdom con = WorldConstantData.kingdoms.get(kingdom);
			nation.email = setup.email;
			nation.colorFg = con.colorFg;
			nation.colorBg = con.colorBg;
			nation.culture = con.culture;
			nation.coreRegions = Ints.asList(con.coreRegions);
			nation.goodwill = ("Holy".equals(setup.trait1) || "Holy".equals(setup.trait2)) ? 15 : setup.dominantIdeology.religion == Religion.IRUHAN ? 5 : -55;
			nation.gothi = new HashMap<String, Boolean>();
			nation.gothi.put("Alyrja", false);
			nation.gothi.put("Lyskr", false);
			nation.gothi.put("Rjinku", false);
			nation.gothi.put("Syrjen", false);
			nation.addTag(setup.trait1);
			nation.addTag(setup.trait2);
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
			if ("Mercantile".equals(setup.trait1) || "Mercantile".equals(setup.trait2)) sharesGold += 0.5;
			if ("Rebellious".equals(setup.trait1) || "Rebellious".equals(setup.trait2)) sharesGold += 5;
			if ("Ruined".equals(setup.trait1) || "Ruined".equals(setup.trait2)) sharesPopulation -= 0.5;
			if ("Ruined".equals(setup.trait1) || "Ruined".equals(setup.trait2)) sharesGold += 2;
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
				w.regions.get(r).crops = w.regions.get(r).population * Constants.setupCropsPerCitizen;
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
				r.unrestPopular = Math.random() * .3 + .1;
				r.food = totalFood / unownedRegions / totalSharesPopulation * unruledNations / 2.0;
			}
		}
		// Un-own rebellious regions.
		HashSet<String> rebelliousNations = new HashSet<>();
		for (String kingdom : nationSetup.keySet()) {
			Nation.NationGson setup = nationSetup.get(kingdom);
			if (!"Rebellious".equals(setup.trait1) && !"Rebellious".equals(setup.trait2)) continue;
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
			Nation.NationGson setup = nationSetup.get(kingdom);
			Culture culture = WorldConstantData.kingdoms.get(kingdom).culture;
			if ("Republican".equals(setup.trait1) || "Republican".equals(setup.trait2)) continue;
			// 10 nobles.
			ArrayList<Noble> nobles = new ArrayList<>();
			for (String trait : new String[]{ "Inspiring", "Frugal", "Soothing", "Meticulous", "Loyal", "Policing", "Generous", "Pious", "Rationing", "Patronizing"}) {
				Noble n = new Noble();
				n.name = WorldConstantData.getRandomName(culture, Math.random() < 0.5 ? WorldConstantData.Gender.MAN : WorldConstantData.Gender.WOMAN);
				n.addTag(trait);
				n.unrest = 0.15;
				n.crisis = new Crisis();
				n.crisis.type = Crisis.Type.NONE;
				nobles.add(n);
			}
			Collections.shuffle(nobles);
			ArrayList<Integer> ownedRegions = new ArrayList<>();
			for (int i = 0; i < w.regions.size(); i++) if (kingdom.equals(w.regions.get(i).getKingdom())) ownedRegions.add(i);
			Collections.shuffle(ownedRegions);
			int placements = Math.min(ownedRegions.size(), "anpilayn".equals(culture) ? 6 : "eolsung".equals(culture) || "hansa".equals(culture) ? 3 : 0);
			for (int i = 0; i < placements; i++) {
				w.regions.get(ownedRegions.get(i)).noble = nobles.get(i);
				nobles.get(i).crisis.deadline = 6;
			}
			for (int i = placements; i < 10; i++) {
				w.getNation(kingdom).court.add(nobles.get(i));
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
			ArrayList<WorldConstantData.Border> borders = new ArrayList<>();
			// Find all borders between owned and unowned land regions.
			for (WorldConstantData.Border bo : WorldConstantData.borders) {
				Region a = w.regions.get(bo.a);
				Region b = w.regions.get(bo.b);
				if ((a.getKingdom() == null && b.getKingdom() == null) || (a.getKingdom() != null && b.getKingdom() != null)) continue;
				if (rebelliousNations.contains(a.getKingdom()) || rebelliousNations.contains(b.getKingdom())) continue;
				if (!a.isLand() || !b.isLand()) continue;
				totalWeight += 1.0 / bo.size;
				borders.add(bo);
			}
			if (!borders.isEmpty()) {
				// Pick a random border weighted by 1/border-size.
				double v = Math.random() * totalWeight;
				for (WorldConstantData.Border b : borders) {
					v -= 1.0 / b.size;
					if (v > 0) continue;
					Region ra = w.regions.get(b.a);
					Region rb = w.regions.get(b.b);
					// Give the region to that kingdom.
					if (ra.getKingdom() == null) ra.setKingdomNoScore(rb.getKingdom());
					else rb.setKingdomNoScore(ra.getKingdom());
					break;
				}
			} else {
				// If there are no borders, give the first unowned land region to a random non-rebellious kingdom.
				for (Region r : w.regions) {
					if (r.isSea()) continue;
					if (r.getKingdom() != null) continue;
					while (r.getKingdom() == null || rebelliousNations.contains(r.getKingdom())) r.setKingdomNoScore(new ArrayList<String>(w.getNationNames()).get((int)(Math.random() * w.kingdoms.size())));
					break;
				}
			}
			numUnownedRegions--;
		}
		// Place armies, navies.
		for (String kingdom : nationSetup.keySet()) {
			Nation.NationGson setup = nationSetup.get(kingdom);
			double sharesNavy = 1;
			double sharesArmy = 1;
			if ("Patriotic".equals(setup.trait1) || "Patriotic".equals(setup.trait2)) sharesArmy += 0.15;
			if ("Rebellious".equals(setup.trait1) || "Rebellious".equals(setup.trait2)) sharesArmy += 0.5;
			if ("Seafaring".equals(setup.trait1) || "Seafaring".equals(setup.trait2)) sharesNavy += 0.15;
			if ("War-like".equals(setup.trait1) || "War-like".equals(setup.trait2)) sharesArmy += 0.15;
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
					Army army = new Army();
					army.type = Army.Type.ARMY;
					army.size = armies / numRegions;
					army.tags = r.getArmyTags();
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
						Army army = new Army();
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
		// Place shipyards. Want ~80 total, split among all nations.
		int shipyardsPerNation = (int)(Math.ceil(80.0 / nationSetup.keySet().size()));
		for (String kingdom : nationSetup.keySet()) {
			ArrayList<Region> regions = new ArrayList<>();
			for (Region r : w.regions) if (kingdom.equals(r.getKingdom()) && r.isCoastal(w)) regions.add(r);
			if (!regions.isEmpty()) {
				for (int i = 0; i < shipyardsPerNation; i++) {
					Construction c = new Construction();
					c.type = "shipyard";
					c.originalCost = 80;
					regions.get((int)(Math.random() * regions.size())).constructions.add(c);
				}
			}
		}
		// Add characters, incl Cardinals
		for (String kingdom : nationSetup.keySet()) {
			Nation.NationGson setup = nationSetup.get(kingdom);
			ArrayList<Integer> regions = new ArrayList<>();
			for (int i = 0; i < w.regions.size(); i++) if (kingdom.equals(w.regions.get(i).getKingdom())) regions.add(i);
			log.log(Level.INFO, "Setting up " + kingdom + ", " + regions.size());
			ArrayList<Character> characters = new ArrayList<>();
			for (int i = 0; i < (setup.dominantIdeology.religion == Religion.IRUHAN && setup.dominantIdeology != Ideology.VESSEL_OF_FAITH ? 5 : 4); i++) {
				Character c = new Character();
				c.name = WorldConstantData.getRandomName(WorldConstantData.kingdoms.get(kingdom).culture, Math.random() < 0.5 ? WorldConstantData.Gender.MAN : WorldConstantData.Gender.WOMAN);
				if (i == 0) {
					c.name = setup.rulerName;
					c.honorific = setup.title;
				}
				c.kingdom = kingdom;
				if (i == 0) {
					c.addTag("Ruler");
					if ("checked".equals(setup.food)) c.values.add("food");
					if ("checked".equals(setup.happiness)) c.values.add("happiness");
					if ("checked".equals(setup.territory)) c.values.add("territory");
					if ("checked".equals(setup.glory)) c.values.add("glory");
					if ("checked".equals(setup.religion)) c.values.add("religion");
					if ("checked".equals(setup.ideology)) c.values.add("ideology");
					if ("checked".equals(setup.security)) c.values.add("security");
					if ("checked".equals(setup.riches)) c.values.add("riches");
					if ("checked".equals(setup.culture)) c.values.add("culture");
				} else {
					c.values.add("food");
					if (Math.random() < 0.1) c.values.add("happiness");
					if (Math.random() < 0.1) c.values.add("territory");
					if (Math.random() < 0.1) c.values.add("glory");
					if (Math.random() < 0.1) c.values.add("religion");
					if (Math.random() < 0.1) c.values.add("ideology");
					if (Math.random() < 0.1) c.values.add("security");
					if (Math.random() < 0.1) c.values.add("riches");
					if (Math.random() < 0.1) c.values.add("culture");
				}
				if (i == 4) c.addTag("Cardinal");
				c.experience.put("general", i == 2 ? 3.0 : 1.0);
				c.experience.put("admiral", i == 3 ? 3.0 : 1.0);
				c.experience.put("governor", i == 0 ? 3.0 : 1.0);
				c.experience.put("spy", i == 1 ? 3.0 : 1.0);
				c.location = regions.get((int)(Math.random() * regions.size()));
				c.orderhint = "Stay in " + w.regions.get(c.location).name;
				w.characters.add(c);
			}
		}
		return w;
	}

	public NationData getNation(String nation) {
		if (NationData.PIRATE_NAME.equals(nation)) return NationData.PIRATE;
		if (NationData.UNRULED_NAME.equals(nation)) return NationData.UNRULED;
		return kingdoms.get(nation);
	}

	public Set<String> getNationNames() {
		return kingdoms.keySet();
	}

	public void addRtc(String json, String from) {
		Message m = getGson().fromJson(json, Message.class);
		// TODO - test legality of message.
		m.from = from;
		rtc.add(m);
	}

	public Entity toEntity(long gameId) {
		Entity e = new Entity(TYPE, gameId + "_" + date);
		e.setProperty("json_gzip", new Blob(Compressor.compress(getGson().toJson(this))));
		return e;
	}

	@Override
	public String toString() {
		return getGson().toJson(this);
	}

	/**
	 * Advances the game state to the next turn.
	 * @return a map of e-mail notifications to send.
	 */
	public Map<String, String> advance(Map<String, Map<String, String>> orders) {
		return new Advancer(orders).advance();
	}

	private static class Budget {
		double incomeTax;
		double incomeSea;
		double incomeTribute;
		double incomeChurch;
		double incomeGift;
		double incomeRaze;
		double incomeExecution;
		double incomeArmyDelivery;
		double spentTribute;
		double spentSoldiers;
		double spentRecruits;
		double spentGift;
		double spentConstruction;
		double spentFoodTransfers;
		double spentBribes;

		public double sum() {
			return incomeTax + incomeSea + incomeTribute + incomeChurch + incomeGift + incomeRaze + incomeExecution + incomeArmyDelivery - spentTribute - spentSoldiers - spentRecruits - spentConstruction - spentGift - spentBribes - spentFoodTransfers;
		}
	}

	class Advancer {
		final Map<String, Map<String, String>> orders;
		HashMap<String, List<String>> tributes = new HashMap<>();
		HashSet<String> lastStands = new HashSet<>();
		HashSet<String> abdications = new HashSet<>();
		HashMap<String, Budget> incomeSources = new HashMap<>();
		int inspires;
		HashMap<Army, Character> leaders = new HashMap<>();
		HashMap<String, Double> pirateThreatSources = new HashMap<>();
		HashMap<Region, ArrayList<Character>> governors = new HashMap<>();
		HashMap<Army, Double> attritionLosses = new HashMap<>();
		HashSet<Region> builds = new HashSet<>();
		HashSet<Region> templeBuilds = new HashSet<>();
		HashSet<String> battlingNations = new HashSet<>();
		HashSet<Army> battlingArmies = new HashSet<>();
		HashMap<String, Double> rationing = new HashMap<String, Double>();
		HashMap<String, Double> taxationRates = new HashMap<String, Double>();

		Advancer(Map<String, Map<String, String>> orders) {
			this.orders = orders;
		}

		Map<String, String> advance() {
			reset();
			synthesizeOrders();
			updateEconomyHints();
			markLastStands();
			updateScoreProfiles();
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
			orderOverrides();
			armyActionsNonTravel();
			armyActionsTravel();
			characterActions();
			destroyPiratesInAlyrjaRegions();
			spawnCultists();
			bribePirates();
			spawnPirates();
			joinBattles();
			captureNavies();
			deliverGoldFromArmies();
			miscPerTurnEffects();
			noblesRebel();
			gainIncome();
			churchOpinionChangesDueToStateReligion();
			selectTiecel();
			gainChurchIncome();
			payTribute();
			applyIncome();
			adjustUnrestDueToTaxation();
			transferFood();
			payTroops();
			reapHarvests();
			cedeRegions();
			recruitTroops();
			sendBudgetNotifications();
			eatFood();
			growPopulations();
			evaluateGothiSpells();
			nobleCrises();
			checkCultVictory();
			miscScoreProfiles();
			appointHeirs();
			takeFinalActions();
			notifyPirateThreats();
			notifyInspires();
			advanceDate();
			Map<String, String> emails = checkGameEnd();
			if (emails == null) {
				emails = prepareEmails();
			}
			return emails;
		}

		void reset() {
			for (String k : kingdoms.keySet()) {
				getNation(k).resetAccessToken();
			}
			notifications.clear();
			rtc.clear();
		}

		void synthesizeOrders() {
			for (String k : kingdoms.keySet()) {
				if (!orders.containsKey(k)) {
					// AI: Snythesize orders from hints.
					HashMap<String, String> aiOrders = new HashMap<>();
					aiOrders.put("economy_tax", getNation(k).taxratehint);
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
					aiOrders.put("plot_type", "defend");
					orders.put(k, aiOrders);
				}
			}
		}

		void updateEconomyHints() {
			for (String k : kingdoms.keySet()) {
				if (orders.containsKey(k)) {
					getNation(k).taxratehint = orders.getOrDefault(k, new HashMap<String, String>()).getOrDefault("economy_tax", "100");
					getNation(k).signingbonushint = orders.getOrDefault(k, new HashMap<String, String>()).getOrDefault("economy_recruit_bonus", "0");
					getNation(k).rationhint = orders.getOrDefault(k, new HashMap<String, String>()).getOrDefault("economy_ration", "100");
				}
			}
		}

		void markLastStands() {
			for (String k : orders.keySet()) {
				if ("last_stand".equals(orders.get(k).get("final_action"))) lastStands.add(k);
				if ("abdicate".equals(orders.get(k).get("final_action"))) abdications.add(k);
			}
		}

		void updateScoreProfiles() {
			if (date % 7 == 0) {
				for (String k : orders.keySet()) {
					Character ruler = null;
					for (Character c : characters) if (k.equals(c.kingdom) && c.hasTag("Ruler")) ruler = c;
					Map<String, String> kOrders = orders.get(k);
					ruler.values.clear();
					for (String o : kOrders.keySet()) {
						if (o.startsWith("score_") && kOrders.get(o).equals("checked")) ruler.values.add(o.replace("score_", ""));
					}
				}
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
					r.battle = Relationship.War.valueOf(kOrders.get("rel_" + kk + "_attack"));
					r.refugees = Relationship.Refugees.valueOf(kOrders.get("rel_" + kk + "_refugees"));
					r.tribute = Math.max(0, Math.min(1, Double.parseDouble(kOrders.get("rel_" + kk + "_tribute")))) / totalTribute;
					if (r.tribute > 0) r.battle = Relationship.War.DEFEND;
					r.construct = Relationship.Construct.valueOf(kOrders.get("rel_" + kk + "_construct"));
					r.cede = Relationship.Cede.valueOf(kOrders.get("rel_" + kk + "_cede"));
					r.fealty = Relationship.Fealty.valueOf(kOrders.get("rel_" + kk + "_fealty"));
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
			for (String k : kingdoms.keySet()) incomeSources.put(k, new Budget());
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
					notifications.add(new Notification(k, "Gold Not Sent", "Our treasurers were ordered to transfer " + totalGifts + " gold but only " + getNation(k).gold + " was available. Fearing some mistake, they did not transfer any."));
					continue;
				}
				for (String o : kOrders.keySet()) {
					if (o.startsWith("nations_gift_amount")) {
						double amount = Double.parseDouble(kOrders.get(o));
						if (amount <= 0) continue;
						String who = kOrders.get(o.replace("amount", "target"));
						notifications.add(new Notification(who, "Gold From " + k, k + " has sent us " + amount + " gold."));
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
			HashSet<String> passedSpells = new HashSet<>();
			for (String g : new String[]{"Alyrja", "Rjinku", "Lyskr", "Syrjen"}) {
				int votes = 0;
				int votesTotal = 0;
				for (String k : kingdoms.keySet()) {
					getNation(k).gothi.put(g, orders.getOrDefault(k, new HashMap<String, String>()).getOrDefault("gothi_" + g, getNation(k).gothi.get(g) ? "checked" : "").equals("checked"));
				}
				for (Region r : regions) {
					if (r.religion == null) continue;
					if (r.religion.toString().toLowerCase().equals(g.toLowerCase())) {
						votesTotal++;
						if (getNation(r.getKingdom()).gothi.getOrDefault(g, false)) votes++;
					}
				}
				if (votes >= 5 && 3 * votes >= 2 * votesTotal) passedSpells.add(g);
			}
			// Goodwill effects.
			for (String k : kingdoms.keySet()) {
				boolean voting = false;
				for (String g : new String[]{"Alyrja", "Rjinku", "Lyskr", "Syrjen"}) {
					if (getNation(k).gothi.get(g)) voting = true;
				}
				if (voting) getNation(k).goodwill -= 30;
			}
			if (passedSpells.contains("Rjinku")) {
				if (!tivar.quake)	notifyAllPlayers("The Quake Begins", "The necessary number of gothi of Rjinku have agreed to call forth the Quake. Buildings and crops will be destroyed every week until the earthquakes end.");
				tivar.quake = true;
			} else if (tivar.quake) {
				tivar.quake = Math.random() < 0.5;
				if (!tivar.quake) notifyAllPlayers("Earthquakes End", "The magical earthquakes wracking the land have finally been ended.");
			}
			if (passedSpells.contains("Syrjen")) {
				if (!tivar.deluge) notifyAllPlayers("The Deluge Begins", "The necessary number of gothi of Syrjen have agreed to call forth the Deluge. Terrible floods make all land regions treacherous (if navigable by fleets), and more crops are drowned in the muddy waters every week.");
				tivar.deluge = true;
			} else if (tivar.deluge) {
				tivar.deluge = Math.random() < 0.5;
				if (!tivar.deluge) notifyAllPlayers("Deluge Ends", "The magical deluge drowning the land has finally ceased.");
			}
			if (passedSpells.contains("Lyskr")) {
				if (!tivar.veil) notifyAllPlayers("The Veil Falls", "The necessary number of gothi of Lyskr have agreed to call forth the Veil. Heavy fog chokes out the sun and reduces visibility severely. Barred from the sunlight, crops will be lost every week until the fog lifts.");
				tivar.veil = true;
			} else if (tivar.veil) {
				tivar.veil = Math.random() < 0.5;
				if (!tivar.veil) notifyAllPlayers("Veil Ends", "The magical fog blanketing the land has finally lifted.");
			}
			if (passedSpells.contains("Alyrja")) {
				if (!tivar.warwinds) notifyAllPlayers("The Warwinds Howl", "The necessary number of gothi of Alyrja have agreed to call forth the Warwinds. Titanic waves make seagoing treacherous, and powerful winds blow all vessels at sea off course. The temperature plummets, killing crops every week until the storm ceases.");
				tivar.warwinds = true;
			} else if (tivar.warwinds) {
				tivar.warwinds = Math.random() < 0.5;
				if (!tivar.warwinds) notifyAllPlayers("Warwinds End", "The magical storms devastating the land have finally calmed.");
			}
		}

		void setDefaultOrderHints() {
			for (Character c : characters) {
				c.orderhint = orders.getOrDefault("".equals(c.captor) ? c.kingdom : c.captor, new HashMap<String, String>()).getOrDefault("action_" + c.name.replace(" ", "_").replace("'", "_"), "");
				c.leadingArmy = 0;
			}
		}

		void countInspires() {
			for (Character c : characters) {
				String action = orders.getOrDefault("".equals(c.captor) ? c.kingdom : c.captor, new HashMap<String, String>()).getOrDefault("action_" + c.name.replace(" ", "_").replace("'", "_"), "");
				Region region = regions.get(c.location);
				if (!"".equals(c.captor)) continue; // inspire can't be done by captives.
				if (action.equals("Inspire the Faithful") && "Sancta Civitate".equals(region.name)) {
					c.addExperience("spy", World.this);
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
						Army nu = new Army();
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
				String action = orders.getOrDefault("".equals(c.captor) ? c.kingdom : c.captor, new HashMap<String, String>()).getOrDefault("action_" + c.name.replace(" ", "_").replace("'", "_"), "");
				Region region = regions.get(c.location);
				if (!"".equals(c.captor)) continue; // lead cannot be done by captives.
				if (action.startsWith("Lead ")) {
					c.orderhint = action;
					int targetId = Integer.parseInt(action.substring(action.lastIndexOf(" ") + 1, action.length()));
					Army target = null;
					for (Army aa : armies) if (aa.id == targetId) target = aa;
					if (target == null) continue; // ???
					if (!target.kingdom.equals(c.kingdom) || c.location != target.location) continue;
					String d = target.isArmy() ? "general" : "admiral";
					if (leaders.get(target) == null || leaders.get(target).calcLevel(d) < c.calcLevel(d)) {
						Character prev = leaders.put(target, c);
						if (prev != null) prev.orderhint = "";
					}
				}
			}
		}

		void mergeArmies() {
			HashMap<Army, Double> originalSizes = new HashMap<>();
			for (Army a : armies) originalSizes.put(a, a.size);
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
						if (src.hasTag("Undead") != target.hasTag("Undead")) continue;
						if (!src.tags.equals(target.tags)) {
							double threatIncrease = src.size * 0.33 * (src.isArmy() ? 1.0 / 100 : 1);
							pirate.threat += threatIncrease;
							pirate.bribes.put(src.kingdom, pirate.bribes.getOrDefault(src.kingdom, 0.0) + threatIncrease / 100);
							pirateThreatSources.put("Army Merges / Disbands", pirateThreatSources.getOrDefault("Army Merges / Disbands", 0.0) + threatIncrease);
							src.size *= .67;
						}
						alreadyMerged.put(sourceId, targetId);
						target.size += src.size;
						target.gold += src.gold;
						armies.remove(src);

						if (leaders.get(src) != null) {
							leaders.get(src).orderhint = "";
							String d = target.isArmy() ? "general" : "admiral";
							if (leaders.get(target) == null || leaders.get(target).calcLevel(d) < leaders.get(src).calcLevel(d)) leaders.put(target, leaders.get(src));
						}
					}
				}
			}
			for (Army a : armies) if (a.size >= 2 * originalSizes.get(a)) a.preparation.clear();
			for (Army a : leaders.keySet()) {
				Character l = leaders.get(a);
				leaders.get(a).addExperience(a.isArmy() ? "general" : "admiral", World.this);
				leaders.get(a).leadingArmy = a.id;
			}
		}

		void resolveIntrigue() {
			ArrayList<String> boosts = new ArrayList<>();
			ArrayList<Plot> kingdomPlotsToResolve = new ArrayList<>();
			for (String k : orders.keySet()) if ("defend".equals(orders.get(k).get("plot_type"))) boosts.add(k);
			for (String k : orders.keySet()) {
				Map<String, String> kOrders = orders.get(k);
				if (!"defend".equals(kOrders.get("plot_type"))) {
					kingdomPlotsToResolve.add(new Plot(k, PlotType.valueOf(kOrders.get("plot_type")), kOrders.get("plot_target"), kOrders.get("plot_action"), boosts, inspires, leaders.values()));
				}
			}
			Collections.sort(kingdomPlotsToResolve);
			for (Plot p : kingdomPlotsToResolve) p.evaluate(orders);
		}

		void orderOverrides() {
			for (Army army : armies) {
				if (army.hasTag("Higher Power")) {
					// If they are adjacent to a special region, move into it, regardless of other orders.
					String originalOrder = orders.getOrDefault(army.kingdom, new HashMap<String, String>()).getOrDefault("action_army_" + army.id, "");
					for (Region r : regions.get(army.location).getNeighbors(World.this)) {
						if (cultRegions.contains(regions.indexOf(r))) orders.get(army.kingdom).put("action_army_" + army.id, "Travel to " + r.name);
					}
					if (!orders.getOrDefault(army.kingdom, new HashMap<String, String>()).getOrDefault("action_army_" + army.id, "").equals(originalOrder)) notifications.add(new Notification(army.kingdom, "Army " + army.id + " Ignores Orders", "An army we control that serves a Higher Power has ignored your orders!"));
				}
				if (abdications.contains(army.kingdom)) {
					if (army.hasTag("Higher Power")) {
						army.kingdom = "Pirate";
					} else {
						orders.get(army.kingdom).put("action_army_" + army.id, "Disband");
					}
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
			HashSet<Region> conqueredRegions = new HashSet<>();
			sortByStrength(actors, leaders, inspires, lastStands);
			for (Army army : actors) {
				String action = orders.getOrDefault(army.kingdom, new HashMap<String, String>()).get("action_army_" + army.id);
				Region region = regions.get(army.location);
				if (action.startsWith("Patrol")) {
					if (!army.isArmy()) continue;
					if (army.calcStrength(World.this, leaders.get(army), inspires, lastStands.contains(army.kingdom)) < region.calcMinPatrolStrength(World.this)) {
						notifications.add(new Notification(army.kingdom, "Patrol Ineffective", "Army " + army.id + " is not strong enough to effectively patrol " + region.name + "."));
						continue;
					}
					ArrayList<String> who = new ArrayList<>();
					for (Character c : characters) {
						if (c.location == army.location) {
							who.add(c.name + " (" + c.kingdom + ")" + ("".equals(c.captor) ? "" : " (captive of " + c.captor + ")"));
							if (NationData.isEnemy(c.kingdom, army.kingdom, World.this) && "".equals(c.captor)) {
								boolean guard = false;
								for (Army a : armies) if (a.isArmy() && a.location == c.location && NationData.isFriendly(a.kingdom, c.kingdom, World.this)) guard = true;
								if (!guard) {
									c.captor = army.kingdom;
									notifications.add(new Notification(c.kingdom, c.name + " Captured", c.name + " was captured by a patrolling army."));
								}
							}
						}
					}
				} else if (action.startsWith("Raze ")) {
					incomeSources.getOrDefault(army.kingdom, new Budget()).incomeRaze += army.raze(World.this, action, leaders.get(army), inspires, lastStands.contains(army.kingdom));
				} else if (action.startsWith("Force civilians to ")) {
					if (!army.isArmy()) continue;
					// Must be strongest in region (not counting other armies of the same ruler). Target region must allow refugees.
					boolean stopped = false;
					for (Army a : armies) {
						if (a.isArmy() && a.location == army.location && !a.kingdom.equals(army.kingdom) && a.calcStrength(World.this, leaders.get(a), inspires, lastStands.contains(a.kingdom)) > army.calcStrength(World.this, leaders.get(army), inspires, lastStands.contains(army.kingdom))) {
							stopped = true;
							notifications.add(new Notification(army.kingdom, "Forced Relocation Failed", "Army " + army.id + " was prevented from forcibly relocating the population of " + region.name + " by other armies present in the region."));
							break;
						}
					}
					if (stopped) continue;
					Region target = null;
					for (Region r : region.getNeighbors(World.this)) if (r.name.equals(action.replace("Force civilians to ", ""))) target = r;
					if (target == null) throw new RuntimeException("No neighbor of " + region.name + " satisfies " + action);
					if (!target.getKingdom().equals(region.getKingdom()) && getNation(target.getKingdom()).getRelationship(region.getKingdom()).refugees == Relationship.Refugees.REFUSE) {
						notifications.add(new Notification(army.kingdom, "Forced Relocation Failed", target.getKingdom() + ", the owner of " + target.name + ", refuses to accept refugees from " + region.getKingdom() + ", and turned them back at the border. The people were allowed to return to their homes."));
						continue;
					}
					// Relocate 10 civilians per soldier.
					// Unrest in target region increases (new population is furious).
					// Unrest in source increases (2 angry per moved).
					// Church wrath increases.
					double populationMoved = Math.min(region.population - 1, army.size * 10);
					if (!target.getKingdom().equals(army.kingdom)) notifications.add(new Notification(target.getKingdom(), "Refugees from " + region.name, "An army of " + army.kingdom + " has forced " + Math.round(populationMoved) + " civilians from " + region.name + " to move to " + target.name + "."));
					if (!region.getKingdom().equals(army.kingdom) && !region.getKingdom().equals(target.getKingdom())) notifications.add(new Notification(target.getKingdom(), "Forced Relocation from " + region.name, "An army of " + army.kingdom + " has forced " + Math.round(populationMoved) + " civilians from " + region.name + " to move to " + target.name + "."));
					sendRefugees(region, target, populationMoved, false, army.kingdom, false);
				} else if (action.startsWith("Transfer ")) {
					String target = action.substring("Transfer army to ".length(), action.length());
					if (!kingdoms.containsKey(target)) throw new RuntimeException("Unknown kingdom \"" + target + "\".");
					if (getNation(target).getRelationship(army.kingdom).fealty == Relationship.Fealty.ACCEPT) {
						notifications.add(new Notification(target, "Soldiers From " + army.kingdom, army.kingdom + " has transferred " + army.size + " " + (army.isArmy() ? "soldiers" : "warships") + " in " + region.name + " to our control."));
						army.kingdom = target;
						army.orderhint = "";
					} else {
						notifications.add(new Notification(target, "Soldiers From " + army.kingdom, army.kingdom + " has attempted to transfer " + (int) Math.round(army.size) + " " + (army.isArmy() ? "soldiers" : "warships") + " in " + region.name + " to our control, but we refused to accept them."));
						notifications.add(new Notification(army.kingdom, "Soldiers To " + target, target + " has refused to accept responsibility for our soldiers."));
					}
				} else if (action.startsWith("Disband")) {
					if (army.hasTag("Higher Power")) continue;
					double threatIncrease = 0;
					if (region.isLand()) {
						addPopulation(region, army.size * .67);
						threatIncrease = army.size * .33 / (army.isArmy() ? 100 : 1);
					} else {
						threatIncrease = army.size / (army.isArmy() ? 100 : 1);
					}
					pirate.threat += threatIncrease;
					pirate.bribes.put(army.kingdom, pirate.bribes.getOrDefault(army.kingdom, 0.0) + threatIncrease / 100);
					pirateThreatSources.put("Army Merges / Disbands", pirateThreatSources.getOrDefault("Army Merges / Disbands", 0.0) + threatIncrease);
					armies.remove(army);
				} else if (action.startsWith("Conquer")) {
					army.conquer(World.this, action, conqueredRegions, tributes, leaders, inspires, lastStands);
				} else if (action.startsWith("Slay Civilians")) {
					if (!army.isArmy()) continue;
					// Must be strongest in region (not counting other armies of the same ruler). Target region must allow refugees.
					boolean stopped = false;
					for (Army a : armies) {
						if (a.isArmy() && a.location == army.location && !a.kingdom.equals(army.kingdom) && a.calcStrength(World.this, leaders.get(a), inspires, lastStands.contains(army.kingdom)) > army.calcStrength(World.this, leaders.get(army), inspires, lastStands.contains(army.kingdom))) {
							stopped = true;
							notifications.add(new Notification(army.kingdom, "Forced Relocation Failed", "Army " + army.id + " was prevented from forcibly relocating the population of " + region.name + " by other armies present in the region."));
							break;
						}
					}
					if (stopped) continue;
					double slain = Math.min(region.population - 1, army.size * 10);
					if (army.hasTag("Undead")) army.size += slain / 10;
					region.unrestPopular = Math.min(1, region.unrestPopular + 3 * slain / region.population);
					getNation(army.kingdom).goodwill -= 200;
					addPopulation(region, -slain);
					double refugees = Math.min(region.population - 1, army.size * 10);
					if (refugees > 0) sendRefugees(region, null, refugees, true, null, false);
					for (Region r : regions) if (r.getKingdom() != null && r.getKingdom().equals(army.kingdom) && r.noble != null) r.noble.unrest = Math.min(1, r.noble.unrest + .2);
				} else if (action.startsWith("Oust ")) {
					if (!army.isArmy()) continue;
					if (region.noble == null) continue;
					if (!region.getKingdom().equals(army.kingdom)) continue;
					if (army.calcStrength(World.this, leaders.get(army), inspires, lastStands.contains(army.kingdom)) < region.calcMinPatrolStrength(World.this)) {
						notifications.add(new Notification(army.kingdom, "Ousting Failed", "Army " + army.id + " is not strong enough to oust the nobility of " + region.name + "."));
						continue;
					}
					for (Region r : regions) if (army.kingdom.equals(r.getKingdom()) && r.noble != null) r.noble.unrest = Math.min(1, r.noble.unrest + .2);
					notifications.add(new Notification(army.kingdom, "Ousted Nobility of " + region.name, "Our army ousted the nobility of " + region.name + ". " + region.noble.name + " and their family " + (Math.random() < 0.5 ? " fought to the last member and were completely wiped out." : " accepted their defeat graciously, but were found dead the next morning.")));
					region.noble = null;
					army.orderhint = "";
				}
			}
		}

		void armyActionsTravel() {
			for (Army army : armies) {
				String action = orders.getOrDefault(army.kingdom, new HashMap<String, String>()).getOrDefault("action_army_" + army.id, "");
				if (army.hasTag("Unpredictable")) {
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
				if (!tivar.deluge && army.isNavy() && region.isLand() && regions.get(toId).isLand()) continue;
				int crossing = -1;
				for (WorldConstantData.Border b : WorldConstantData.borders) if ((b.a == army.location && b.b == toId) || (b.b == army.location && b.a == toId)) crossing = b.size;
				Preparation prep = null;
				for (Preparation p : army.preparation) if (p.to == toId) prep = p;
				int travelAmount = 1;
				if (army.hasTag("Riders")) travelAmount = 2;
				if (army.hasTag("Pathfinders") && regions.get(toId).getKingdom() != null && NationData.isFriendly(army.kingdom, regions.get(toId).getKingdom(), World.this)) travelAmount = 3;
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
				if (!"".equals(c.captor)) c.addExperience("spy", World.this);
				String action = orders.getOrDefault("".equals(c.captor) ? c.kingdom : c.captor, new HashMap<String, String>()).getOrDefault("action_" + c.name.replace(" ", "_").replace("'", "_"), "");
				Region region = regions.get(c.location);
				c.hidden = action.startsWith("Hide in ");
				if (action.startsWith("Stay in ")) {
					if ("".equals(c.captor)) c.addExperience("*", World.this);
				} else if (action.startsWith("Hide in ") || action.startsWith("Travel to ")) {
					if ("".equals(c.captor)) {
						if (c.hidden) c.addExperience("spy", World.this);
						else c.addExperience("*", World.this);
					}
					String destination = action.replace("Travel to ", "").replace("Hide in ", "");
					if (region.name.equals(destination)) continue;
					boolean isNeighbor = false;
					for (Region r : region.getNeighbors(World.this)) if (r.name.equals(destination)) isNeighbor = true;
					if (!isNeighbor) throw new RuntimeException("Cannot move " + c.name + " to " + destination + ": " + action);
					int toId = -1;
					for (int i = 0; i < regions.size(); i++) {
						if (regions.get(i).name.equals(destination)) toId = i;
					}
					int crossing = -1;
					for (WorldConstantData.Border b : WorldConstantData.borders) if ((b.a == c.location && b.b == toId) || (b.b == c.location && b.a == toId)) crossing = b.size;
					Preparation prep = null;
					for (Preparation p : c.preparation) if (p.to == toId) prep = p;
					if (prep == null) {
						prep = new Preparation();
						prep.to = toId;
						prep.amount = 1;
						c.preparation.add(prep);
					} else {
						prep.amount++;
					}
					if (prep.amount >= crossing) {
						c.location = toId;
						c.orderhint = c.hidden ? "Hide in " + regions.get(toId).name : "";
						c.preparation.clear();
					}
				} else if (action.startsWith("Build ")) {
					c.addExperience("governor", World.this);
					if (!c.kingdom.equals(region.getKingdom()) && getNation(region.getKingdom()).getRelationship(c.kingdom).construct == Relationship.Construct.FORBID) {
						notifications.add(new Notification(c.kingdom, "Construction Failed", region.getKingdom() + " does not permit us to build in " + region.name + "."));
					} else {
						double cost = 0;
						double costMod = 1;
						Construction ct = new Construction();
						if (action.contains("Shipyard")) {
							ct.type = "shipyard";
							cost = 80;
						} else if (action.contains("Temple")) {
							ct.type = "temple";
							ct.religion = Ideology.fromString(action.replace("Build Temple (", "").replace(")", ""));
							cost = 30;
							if (getNation(c.kingdom).hasTag("Mystical")) costMod -= .5;
							if (getNation(c.kingdom).hasTag("Evangelical") && region.religion != NationData.getStateReligion(c.kingdom, World.this)) costMod -= 1;
							if (ct.religion.religion == Religion.IRUHAN && region.religion.religion != Religion.IRUHAN && getDominantIruhanIdeology() == Ideology.VESSEL_OF_FAITH) costMod -= 1;
							if (region.religion == Ideology.TAPESTRY_OF_PEOPLE) {
								boolean templeBonus = true;
								for (Region r : region.getNeighbors(World.this)) if (r.isLand() && (r.religion != region.religion || r.culture != region.culture)) templeBonus = false;
								if (templeBonus) costMod -= 1;
							}
						} else if (action.contains("Fortifications")) {
							ct.type = "fortifications";
							cost = 20;
							if (Ideology.FLAME_OF_KITH == region.religion) costMod -= 1;
						}
						if (getNation(c.kingdom).hasTag("Industrial")) costMod -= .25;
						if (region.noble != null && region.noble.hasTag("Patronizing")) costMod -= .5;
						if (region.noble != null && region.noble.hasTag("Broke")) costMod += 1;
						cost *= Math.max(0, costMod);
						ct.originalCost = cost;
						if (cost <= getNation(c.kingdom).gold) {
							getNation(c.kingdom).gold -= cost;
							incomeSources.getOrDefault(c.kingdom, new Budget()).spentConstruction += cost;
							region.constructions.add(ct);
							if (ct.type.equals("temple")) {
								Ideology r = region.religion;
								region.setReligion(ct.religion, World.this);
								if (r != Ideology.VESSEL_OF_FAITH && region.religion == Ideology.VESSEL_OF_FAITH) {
									for (String kingdom : kingdoms.keySet()) if (Ideology.VESSEL_OF_FAITH == NationData.getStateReligion(kingdom, World.this)) {
										for (Region rr : regions) if (kingdom.equals(rr.getKingdom())) rr.unrestPopular = Math.max(0, rr.unrestPopular - .1);
									}
								}
								if (getNation(c.kingdom).hasTag("Mystical")) region.unrestPopular = Math.max(0, region.unrestPopular - .1);
								if (ct.religion.religion != Religion.IRUHAN) getNation(c.kingdom).goodwill -= 20;
								else if (ct.religion != Ideology.VESSEL_OF_FAITH) getNation(c.kingdom).goodwill += 15;
							}
							if (ct.type.equals("shipyard") && getNation(c.kingdom).hasTag("Ship-Building")) buildShips(c.kingdom, c.location, Constants.numShipsBuiltPerShipyard * Constants.shipBuildingTraitWeeksProduction);
							builds.add(region);
							if (ct.type.equals("temple")) templeBuilds.add(region);
							if (ct.type.equals("fortifications") && getNation(c.kingdom).hasTag("Defensive")) {
								Construction c2 = new Construction();
								c2.type = "fortifications";
								c2.originalCost = 0;
								region.constructions.add(c2);
							}
						} else {
							notifications.add(new Notification(c.kingdom, "Construction Failed", "We did not have the " + Math.round(cost) + "gold necessary to construct as ordered in " + region.name + "."));
						}
					}
				} else if (action.startsWith("Instate Noble")) {
					if (!region.isLand() || !region.getKingdom().equals(c.kingdom) || region.noble != null) continue;
					for (Noble n : getNation(c.kingdom).court) if (String.join(", ", n.tags).equals(action.replace("Instate Noble (", "").replace(")", ""))) {
						getNation(c.kingdom).court.remove(n);
						region.noble = n;
						n.crisis.deadline = date + 6;
						break;
					}
					c.orderhint = "";
					c.addExperience("governor", World.this);
				} else if (action.startsWith("Govern")) {
					if (!region.isLand() || !region.getKingdom().equals(c.kingdom)) continue;
					ArrayList<Character> gov = governors.getOrDefault(region, new ArrayList<Character>());
					gov.add(c);
					governors.put(region, gov);
					c.addExperience("governor", World.this);
				} else if (action.startsWith("Transfer character to ")) {
					String target = action.replace("Transfer character to ", "");
					if (!kingdoms.containsKey(target)) throw new RuntimeException("Unknown kingdom \"" + target + "\".");
					if ("".equals(c.captor)) {
						notifications.add(new Notification(target, "Hero from " + c.kingdom, c.name + ", formerly a hero of " + c.kingdom + " has sworn fealty and loyalty to us."));
						c.kingdom = target;
					} else {
						notifications.add(new Notification(target, "Captive from " + c.captor, c.name + ", a hero of " + c.kingdom + " and formerly a captive of " + c.captor + " has been transferred to our care."));
						c.captor = target;
					}
					c.orderhint = "";
				} else if (action.startsWith("Execute")) {
					String notification = "The sovereign power " + c.captor + " has tried, convicted, and executed " + c.name + ".";
					getNation(c.captor).goodwill -= 50;
					if (region.getKingdom() == null || region.getKingdom().equals(c.captor)) notification += " " + c.name + " was not present at their trial.";
					String[] flavor = new String[]{
						c.name + "'s last words were loving reassurances to their family.",
						c.name + "'s last words reaffirmed their loyalty to " + c.kingdom + " and condemned " + c.captor + ".",
						c.name + " attempted a daring escape at the last moment, but was unable to get free."
					};
					notification += flavor[(int)(Math.random() * flavor.length)];
					if (getNation(c.captor).hasTag("Bloodthirsty") && region.getKingdom().equals(c.captor)) {
						getNation(c.captor).gold += 300;
						incomeSources.getOrDefault(c.captor, new Budget()).incomeExecution += 300;
						notification += " The inhabitants of " + c.captor + " celebrated the event with offerings to their ruler.";
					}
					notifyAllPlayers("Execution of " + c.name, notification);
					removeCharacters.add(c);
					if (c.hasTag("Ruler")) {
						notifications.add(new Notification(c.kingdom, c.name + " Killed", " You have been killed. Your nation mourns, but your government is prepared for this eventuality, and another ruler rises to power. Your new ruler may have different values and therefore change what you earn or lose score points for. Points accumulated so far are kept."));
					}
				} else if (action.startsWith("Set Free")) {
					notifications.add(new Notification(c.kingdom, c.name + " Freed", c.name + "'s captors have released " + c.name + " from captivity."));
					c.captor = "";
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
				notifications.add(new Notification(regions.get(a.location).getKingdom(), "Pirates Destroyed", "The pirates that have wandered into " + regions.get(a.location).name + " have been thoroughly destroyed by the occupants of that region."));
				armies.remove(a);
			}
		}

		void spawnCultists() {
			for (String kingdom : orders.keySet()) {
				if ("checked".equals(orders.get(kingdom).get("plot_cult")) && !getNation(kingdom).loyalToCult) {
					getNation(kingdom).loyalToCult = true;
					ArrayList<Integer> kRegions = new ArrayList<>();
					for (int i = 0; i < regions.size(); i++) {
						Region r = regions.get(i);
						if (kingdom.equals(r.getKingdom())) {
							kRegions.add(i);
							if (!r.gotCultFood) {
								r.food += r.population * 2;
								r.gotCultFood = true;
							}
						}
					}
					if (kRegions.size() != 0) {
						int spawnRegion = kRegions.get((int)(Math.random() * kRegions.size()));
						Army a = new Army();
						a.location = spawnRegion;
						a.type = Army.Type.ARMY;
						a.size = 5000;
						a.tags = new ArrayList<>();
						a.id = getNewArmyId();
						a.kingdom = kingdom;
						a.composition.put("Undead", a.size);
						a.orderhint = "";
						a.addTag("Undead");
						a.addTag("Higher Power");
						armies.add(a);
						notifyAllPlayers(kingdom + " Joins Cult", "Throughout " + kingdom + ", the dead crawl forth from their graves, animated by some ancient magic. Meats of unknown origin begin to appear in butchershops, and the shambling sight of a corpse searching for an unknown quarry becomes commonplace. A skeletal army gathers in " + regions.get(spawnRegion).name + ".");
					}
				}
			}
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
			pirate.threat *= .75;
			List<String> pirateNotes = new ArrayList<>();
			while (piratesSpawned < pirateArmies) {
				for (Region r : regions) totalPirateThreat += r.calcPirateThreat(World.this);
				totalPirateThreat *= Math.random();
				for (int i = 0; i < regions.size(); i++) {
					totalPirateThreat -= regions.get(i).calcPirateThreat(World.this);
					if (totalPirateThreat < 0) {
						Army a = new Army();
						a.location = i;
						a.type = Army.Type.ARMY;
						a.size = Math.min(pirateArmies - piratesSpawned, 2000);
						a.tags = new ArrayList<>();
						a.id = getNewArmyId();
						a.kingdom = "Pirate";
						a.composition.put("Pirate", a.size);
						a.orderhint = "";
						a.addTag("Pillagers");
						a.addTag("Unpredictable");
						armies.add(a);
						piratesSpawned += a.size;
						pirateNotes.add(regions.get(i).name);
						break;
					}
				}
			}
			for (String bribe : pirate.bribes.keySet()) pirate.bribes.put(bribe, pirate.bribes.get(bribe) * .75);
			notifyAllPlayers("Piracy", (int) Math.ceil(piratesSpawned) + " total pirates have appeared in " + StringUtil.and(pirateNotes) + ".");
		}

		void joinBattles() {
			for (int i = 0; i < regions.size(); i++) {
				Region region = regions.get(i);
				ArrayList<Army> localArmies = new ArrayList<>();
				for (Army a : armies) if (a.location == i) {
					if (a.isNavy() && region.isLand() && !tivar.deluge) continue;
					localArmies.add(a);
				}
				double combatFactor = Math.random() * .3 + 1.7;
				double totalArmyStrength = 0;
				for (Army a : localArmies) totalArmyStrength += Math.pow(a.calcStrength(World.this, leaders.get(a), inspires, lastStands.contains(a.kingdom)), combatFactor);
				HashMap<Army, Double> casualties = new HashMap<>();
				HashMap<Army, Double> hansaImpressment = new HashMap<>();
				HashMap<Army, Double> goldThefts = new HashMap<>();
				for (Army a : localArmies) {
					double enemyStrength = 0;
					for (Army b : localArmies) {
						if (NationData.isEnemy(a.kingdom, b.kingdom, World.this, region) && (!a.hasTag("Higher Power") || !b.hasTag("Higher Power"))) enemyStrength += Math.pow(b.calcStrength(World.this, leaders.get(b), inspires, lastStands.contains(b.kingdom)), combatFactor);
					}
					if (enemyStrength == 0) continue;
					double cf = enemyStrength / totalArmyStrength;
					if (a.hasTag("Formations") && cf > .45 && cf < .9) cf = .45;
					if (region.getKingdom() != null && NationData.isFriendly(region.getKingdom(), a.kingdom, World.this)) {
						cf *= Math.max(0, Math.min(1, 1 - (region.calcFortificationMod())));
					}
					if (cf >= .9) cf = 1;
					if (cf != 0) casualties.put(a, cf);
					for (Army b : localArmies) {
						if (!NationData.isEnemy(a.kingdom, b.kingdom, World.this, region) || (a.hasTag("Higher Power") && b.hasTag("Higher Power"))) continue;
						double casualtyRateCaused = cf * Math.pow(b.calcStrength(World.this, leaders.get(b), inspires, lastStands.contains(b.kingdom)), combatFactor) / enemyStrength;
						if (a.isArmy() && b.hasTag("Impressment")) hansaImpressment.put(b, hansaImpressment.getOrDefault(b, 0.0) + a.size * .15 * casualtyRateCaused);
						if (getNation(a.kingdom) != null && getNation(b.kingdom) != null && getNation(a.kingdom).goodwill <= -75) getNation(b.kingdom).goodwill += 3 * a.size / 100 * casualtyRateCaused;
						goldThefts.put(b, goldThefts.getOrDefault(b, 0.0) + a.gold * casualtyRateCaused);
					}
				}
				double dead = 0;
				HashSet<String> nonPirateBelligerents = new HashSet<>();
				for (Army c : casualties.keySet()) {
					battlingArmies.add(c);
					battlingNations.add(c.kingdom);
					if (!"Pirate".equals(c.kingdom)) nonPirateBelligerents.add(c.kingdom);
					double cf = casualties.get(c);
					double lost = cf * c.size;
					if (!c.hasTag("Undead")) dead += lost;
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
				if (nonPirateBelligerents.size() > 1 && region.name.equals("Sancta Civitate")) {
					for (String k : nonPirateBelligerents) getNation(k).goodwill -= 40;
				}
				for (Army h : hansaImpressment.keySet()) {
					h.size += hansaImpressment.get(h);
					dead -= hansaImpressment.get(h);
				}
				for (Army h : goldThefts.keySet()) {
					h.gold += goldThefts.get(h);
				}
				String battleDetails = "During the fighting";
				double foodWrecked = 1;
				double cropsWrecked = 1;
				double popKilled = 1;
				double unrest = 0;
				if (region.isLand()) {
					for (int p = 1000; p < dead; p += 1000) {
						if (Math.random() < 1 / 4.0) {
							// Nothing happens.	
						} else if (Math.random() < 1 / 3.0) {
							foodWrecked *= .85;
						} else if (Math.random() < 1 / 2.0) {
							cropsWrecked *= .7;
						} else {
							popKilled *= .98;
							unrest += .1;
						}
					}
					if (foodWrecked < 1) {
						battleDetails += ", " + Math.round((1 - foodWrecked) * region.food / 1000) + "k measures of food were spoiled";
						region.food *= foodWrecked;
					}
					if (cropsWrecked < 1) {
						battleDetails += ", " + Math.round((1 - cropsWrecked) * 100) + "% of the crops were destroyed";
						region.crops *= cropsWrecked;
					}
					if (popKilled < 1) {
						battleDetails += ", " + Math.round((1 - popKilled) * region.population) + " civilians were mistakenly killed, and as many more fled for their lives to surrounding regions";
						region.population *= popKilled;
						region.unrestPopular = Math.min(1, region.unrestPopular + unrest);
						sendRefugees(region, null, region.population * (1 - popKilled), true, null, false);
					}
				}
				ArrayList<Army> localUndeadArmies = new ArrayList<>();
				for (Army a : localArmies) if (a.hasTag("Undead") && casualties.getOrDefault(a, 0.0) < 1) localUndeadArmies.add(a);
				if (localUndeadArmies.size() > 0) {
					battleDetails += ", " + Math.round(dead / 2) + " soldiers rose from the dead to serve the Cult";
					double raised = dead / 2 / localUndeadArmies.size();
					for (Army u : localUndeadArmies) {
						u.size += raised;
						u.composition.put("Undead", u.composition.getOrDefault("Undead", 0.0) + raised);
					}
				}
				battleDetails += ".";
				if (battleDetails.equals("During the fighting.")) battleDetails = "";
				HashSet<String> localKingdoms = new HashSet<>();
				for (Army a : localArmies) localKingdoms.add(a.kingdom);
				if (!casualties.isEmpty()) {
					for (String k : localKingdoms) {
						String armySummary = "";
						for (Army a : localArmies) if (casualties.containsKey(a)) {
							if (isHidden(a, k)) armySummary += "\n" + (a.isNavy() ? "A navy" : "An army") + " of an unknown nation suffered " + Math.round(casualties.get(a) * 100) + "% losses.";
							else armySummary += "\n" + (a.isNavy() ? "A navy" : "An army") + " of " + a.kingdom + " suffered " + Math.round(casualties.get(a) * 100) + "% losses.";
						}
						notifications.add(new Notification(k, "Battle in " + region.name, "Over a week of fighting in " + region.name + ":" + armySummary + "\n" + battleDetails));
					}
				}
			}
		}

		void captureNavies() {
			if (!tivar.deluge) {
				ArrayList<Army> removals = new ArrayList<>();
				for (int i = 0; i < regions.size(); i++) {
					Region region = regions.get(i);
					if (region.isSea()) continue;
					for (Army a : armies) if (a.location == i && a.isNavy()) {
						Army max = getMaxArmyInRegion(i, leaders, inspires, lastStands);
						if (max != null && NationData.isEnemy(a.kingdom, max.kingdom, World.this)) {
							notifications.add(new Notification(a.kingdom, "Fleet Captured", "Our fleet of " + Math.round(a.size) + " warships in " + region.name + " was seized by " + max.kingdom + "."));
							a.kingdom = max.kingdom;
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
				double ration = rationing.getOrDefault(r.getKingdom(), 1.0);
				if (ration < 0.9) unrestMod += .15;
				if (ration > 1.1) unrestMod -= .1;
				if (getNation(r.getKingdom()).hasTag("Republican")) unrestMod -= .03;
				if (r.noble != null && r.noble.hasTag("Soothing")) unrestMod -= .06;
				if (r.noble != null && r.noble.hasTag("Workaholic")) unrestMod += .03;
				if (r.noble != null && r.noble.hasTag("Generous") && isHarvestTurn()) unrestMod -= .5;
				if (r.religion == Ideology.VESSEL_OF_FAITH) unrestMod -= .06;
				if (Ideology.ALYRJA == r.religion && r.food < turnsUntilHarvest() * r.calcConsumption(World.this, 1)) unrestMod += .03;
				if (Ideology.RJINKU == r.religion && !battlingNations.contains(r.getKingdom())) unrestMod += .02;
				if (r.getKingdom() != null && getNation(r.getKingdom()).hasTag("Imperialistic")) {
					int tributeC = 0;
					for (String k : tributes.keySet()) if (tributes.get(k).contains(r.getKingdom())) tributeC++;
					unrestMod -= 0.03 * tributeC;
				}
				if (r.getKingdom() != null && !NationData.UNRULED_NAME.equals(r.getKingdom())) {
					for (String k : tributes.get(r.getKingdom())) if (getNation(k).hasTag("Imperialistic")) unrestMod -= 0.03;
				}
				for (Construction c : r.constructions) if (c.type.equals("temple")) unrestMod -= 0.02;
				r.unrestPopular = Math.min(1, Math.max(0, r.unrestPopular + unrestMod));

				if (r.noble != null && r.noble.hasTag("Snubbed")) r.noble.unrest = Math.min(1, r.noble.unrest + .02);
				if (r.noble != null) for (String k : tributes.keySet()) if (tributes.get(k).contains(r.getKingdom()) && NationData.isEnemy(k, r.getKingdom(), World.this) && getNation(k).previousTributes.contains(r.getKingdom())) r.noble.unrest = Math.min(1, r.noble.unrest + .04);
				if (r.noble != null && r.noble.hasTag("Policing")) pirate.bribes.put(r.getKingdom(), pirate.bribes.getOrDefault(r.getKingdom(), 0.0) - 8);
			}
			// Syrjen unrest mods.
			HashMap<Region, Double> popularUnrests = new HashMap<>();
			for (Region r : regions) if (r.isLand()) popularUnrests.put(r, r.unrestPopular);
			for (Region r : regions) if (Ideology.SYRJEN == r.religion) {
				double maxNeighborUnrest = 0;
				for (Region n : r.getNeighbors(World.this)) if (popularUnrests.getOrDefault(n, 0.1) > maxNeighborUnrest) maxNeighborUnrest = popularUnrests.getOrDefault(n, 0.1);
				if (maxNeighborUnrest > r.unrestPopular) {
					r.unrestPopular = Math.min(maxNeighborUnrest, r.unrestPopular + 0.05);
				}
			}
			// Sword of Truth destruction
			for (String k : kingdoms.keySet()) {
				if (NationData.getStateReligion(k, World.this) != Ideology.SWORD_OF_TRUTH) continue;
				HashSet<Region> neighboringEnemies = new HashSet<>();
				for (Region r : regions) if (k.equals(r.getKingdom())) for (Region n : r.getNeighbors(World.this)) if (n.getKingdom() != null && NationData.isEnemy(k, n.getKingdom(), World.this)) neighboringEnemies.add(n);
				for (Region n : neighboringEnemies) {
					if (Math.random() < 0.5) continue;
					for (Construction c : n.constructions) {
						if (c.type.equals("fortification")) {
							n.constructions.remove(c);
							notifications.add(new Notification(n.getKingdom(), "Sabotage in " + n.name, k + " saboteurs have destroyed a fortification in " + n.name + ", claiming it was the will of Iruhan."));
							notifications.add(new Notification(k, "Friendly sabotage in " + n.name, "Civilian saboteurs loyal to us have destroyed a fortification in " + n.name + ", a region of " + n.getKingdom() + ", claiming it was the will of Iruhan."));
							break;
						}
					}
				}
			}
			// Tavian abductions 
			HashMap<Region, Double> totalAbductions = new HashMap<>();
			for (Region r : regions) {
				if (r.getKingdom() == null) continue;
				if (Ideology.FLAME_OF_KITH != NationData.getStateReligion(r.getKingdom(), World.this)) continue;
				ArrayList<Region> abductees = new ArrayList<>();
				for (Region n : r.getNeighbors(World.this)) if (!r.getKingdom().equals(n.getKingdom())) abductees.add(n);
				if (abductees.isEmpty()) continue;
				for (Region a : abductees) {
					double transfer = a.population * 0.01;
					addPopulation(r, transfer);
					addPopulation(a, -transfer);
					totalAbductions.put(a, totalAbductions.getOrDefault(a, 0.0) + transfer);
				}
			}
			for (Region r : totalAbductions.keySet()) {
				r.unrestPopular = Math.min(1, r.unrestPopular + 0.02);
				notifications.add(new Notification(r.getKingdom(), "Slavers in " + r.name, "Traffickers from neighboring lands that follow the Tavian (Flame of Kith) religion have carried off " + Math.round(totalAbductions.get(r)) + " of the inhabitants of " + r.name + " to sell as slaves."));
			}
		}

		void noblesRebel() {
			for (String k : kingdoms.keySet()) {
				boolean rebels = false;
				for (Region r : regions) if (k.equals(r.getKingdom()) && r.noble != null && r.noble.unrest >= .75) rebels = true;
				if (rebels) {
					ArrayList<String> rebelTo = new ArrayList<>();
					for (String kk : kingdoms.keySet()) if (NationData.isEnemy(k, kk, World.this)) rebelTo.add(kk);
					if (rebelTo.isEmpty()) {
						HashMap<String, ArrayList<Double>> unrests = new HashMap<>();
						for (Region r : regions) if (!k.equals(r.getKingdom()) && r.noble != null) {
							if (!unrests.containsKey(r.getKingdom())) unrests.put(r.getKingdom(), new ArrayList<Double>());
							unrests.get(r.getKingdom()).add(r.noble.unrest);
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
						for (Region r : regions) if (k.equals(r.getKingdom()) && r.noble != null && r.noble.unrest >= 0.5) {
							r.setKingdom(World.this, rebelToChosen);
							r.noble.unrest = 0.15;
						}
					}
				}
			}
		}

		void gainIncome() {
			for (String k : kingdoms.keySet()) taxationRates.put(k, Double.parseDouble(orders.getOrDefault(k, new HashMap<String, String>()).getOrDefault("economy_tax", "100")) / 100);
			for (int i = 0; i < regions.size(); i++) {
				Region r = regions.get(i);
				if (r.isLand()) {
					String whoTaxes = r.getKingdom();
					double income = r.calcTaxIncome(World.this, governors.get(r), taxationRates.getOrDefault(r.getKingdom(), 1.0), rationing.getOrDefault(r.getKingdom(), 1.0));
					Army max = getMaxArmyInRegion(i, leaders, inspires, lastStands);
					if (max != null && !NationData.isFriendly(max.kingdom, r.getKingdom(), World.this) && max.hasTag("Pillagers")) {
						max.gold += income;
					} else {
						incomeSources.getOrDefault(r.getKingdom(), new Budget()).incomeTax += income;
					}
				} else {
					if (tivar.warwinds) continue; // No naval income while warwinds are active.
					// Get navy powers, 20 gold to biggest, 10 to runner-up; if tie; split 30 between all tied
					ArrayList<Army> localNavies = new ArrayList<>();
					for (Army a : armies) if (a.location == i && a.isNavy()) localNavies.add(a);
					sortByStrength(localNavies, leaders, inspires, lastStands);
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
					double firstStrength = localNavies.get(0).calcStrength(World.this, leaders.get(localNavies.get(0)), inspires, lastStands.contains(localNavies.get(0).kingdom));
					for (int j = 1; j < localNavies.size(); j++) {
						Army a = localNavies.get(j);
						if (a.calcStrength(World.this, leaders.get(a), inspires, lastStands.contains(a.kingdom)) == firstStrength) {
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
							double secondStrength = tiedForSecond.get(0).calcStrength(World.this, leaders.get(tiedForSecond.get(0)), inspires, lastStands.contains(tiedForSecond.get(0).kingdom));
							for (int j = 2; j < localNavies.size(); j++) {
								Army a = localNavies.get(j);
								if (a.calcStrength(World.this, leaders.get(a), inspires, lastStands.contains(a.kingdom)) == secondStrength) {
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
				if (NationData.getStateReligion(k, World.this) == Ideology.SYRJEN) seaMods += 1;
				if (getNation(k).hasTag("Seafaring")) seaMods += 1/3.0;
				incomeSources.getOrDefault(k, new Budget()).incomeSea *= seaMods;
			}
		}

		void churchOpinionChangesDueToStateReligion() {
			for (String k : kingdoms.keySet()) {
				if (NationData.getStateReligion(k, World.this).religion != Religion.IRUHAN) getNation(k).goodwill -= 5;
				if (getNation(k).loyalToCult) getNation(k).goodwill -= 10;
			}
		}

		void selectTiecel() {
			boolean isTiecel = false;
			for (Character c : characters) if (c.hasTag("Tiecel")) isTiecel = true;
			if (!isTiecel) {
				HashMap<Integer, ArrayList<Character>> cardinalCount = new HashMap<>();
				int totalCardinals = 0;
				for (Character c: characters) if (c.hasTag("Cardinal")) {
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
						max.addTag("Tiecel");
						notifyAllPlayers("Tiecel Elected", "The Cardinals of the Church of Iruhan have elected " + max.name + " as Tiecel. Consequently, " + max.kingdom + " is expected to play a much larger role in the Church than they have in the past.");
					}
				}
			}
		}

		void gainChurchIncome() {
			double churchIncome = 200 + inspires * 20;
			HashMap<String, Double> foodBalance = new HashMap<>();
			for (Region r : regions) {
				if (r.isSea()) continue;
				double balance = r.calcConsumption(World.this, 1) * turnsUntilHarvest() - r.food;
				foodBalance.put(r.getKingdom(), foodBalance.getOrDefault(r.getKingdom(), 0.0) + balance);
			}
			double totalMeasuresDeficit = 0;
			for (String k : foodBalance.keySet()) if (foodBalance.get(k) > 0) totalMeasuresDeficit += foodBalance.get(k);
			HashMap<String, Double> shares = new HashMap<>();
			for (String k : foodBalance.keySet()) if (foodBalance.get(k) > 0) shares.put(k, shares.getOrDefault(k, 0.0) + foodBalance.get(k) / totalMeasuresDeficit);
			double totalGoodwill = 0;
			for (String k : kingdoms.keySet()) if (getNation(k).goodwill > 0) totalGoodwill += getNation(k).goodwill * ((getDominantIruhanIdeology().equals(NationData.getStateReligion(k, World.this)) && getNation(k).hasTag("Holy")) ? 2 : 1);
			for (String k : kingdoms.keySet()) if (getNation(k).goodwill > 0) shares.put(k, shares.getOrDefault(k, 0.0) + getNation(k).goodwill * ((getDominantIruhanIdeology().equals(NationData.getStateReligion(k, World.this)) && getNation(k).hasTag("Holy")) ? 2 : 1) * 2 / totalGoodwill);
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
					incomeSources.getOrDefault(kk, new Budget()).incomeTribute += t;
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
				for (Region r : regions) if (r.getKingdom() != null && r.getKingdom().equals(k)) r.unrestPopular = Math.min(1, Math.max(0, r.unrestPopular + unrest));
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
						notifications.add(new Notification(k, "Food Transfer from " + from.name + " Failed", "We lost control of the region before the food could be transferred from it!"));
						continue;
					}
					double amount = Math.max(0, Math.min(from.food, Double.parseDouble(kOrders.get(o)) * 1000));
					if (amount == 0) continue;
					double cost = amount / 50000;
					if (cost > getNation(k).gold) {
						amount *= getNation(k).gold / cost;
						cost = getNation(k).gold;
					}
					if (!from.canFoodTransferTo(World.this, to)) throw new RuntimeException("Can't transfer food from " + fromName + " to " + toName);
					notifications.add(new Notification(to.getKingdom(), "Food Transfer to " + to.name, Math.round(amount / 1000) + "k measures of food were transferred from " + from.name + " to " + to.name));
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
				if ("Pirate".equals(a.kingdom)) continue;
				if (a.hasTag("Higher Power")) continue;
				double cost = a.size;
				double mods = 1;
				if (a.isArmy()) cost *= 1.0 / 100;
				else cost *= 1 / 3.0;
				if (a.hasTag("Crafts-soldiers") && !orders.getOrDefault(a.kingdom, new HashMap<String, String>()).getOrDefault("action_army_" + a.id, "").startsWith("Travel ")) {
					mods -= 0.5;
				}
				if (Ideology.COMPANY == NationData.getStateReligion(a.kingdom, World.this)) mods -= 0.5;
				if (getNation(a.kingdom).hasTag("Rebellious") && getNation(a.kingdom).coreRegions.contains(a.location)) {
					mods -= 0.5;
				}
				cost = Math.max(0, cost * mods);
				payments.put(a.kingdom, payments.getOrDefault(a.kingdom, 0.0) + cost);
			}
			for (String k : payments.keySet()) {
				double owed = payments.get(k);
				if (owed > getNation(k).gold) {
					double desertion = (1 - (getNation(k).gold / owed)) / 3.0;
					incomeSources.getOrDefault(k, new Budget()).spentSoldiers = getNation(k).gold;
					getNation(k).gold = 0;
					for (Army a : armies) if (a.kingdom.equals(k)) {
						if (a.hasTag("Higher Power")) continue;
						double des = a.size * desertion;
						a.size -= des;
						double threatIncrease = a.isNavy() ? des : des / 100;
						pirate.threat += threatIncrease;
						pirate.bribes.put(a.kingdom, pirate.bribes.getOrDefault(a.kingdom, 0.0) + threatIncrease / 100);
						pirateThreatSources.put("Desertion from " + a.kingdom, pirateThreatSources.getOrDefault("Desertion from " + a.kingdom, 0.0) + threatIncrease);
					}
					notifications.add(new Notification(k, "Desertion", Math.round(desertion * 100) + "% of our troops deserted due to lack of pay."));
				} else {
					incomeSources.getOrDefault(k, new Budget()).spentSoldiers = owed;
					getNation(k).gold -= owed;
				}
			}
		}

		void reapHarvests() {
			Set<String> stoicNations = new HashSet<>();
			for (String k : kingdoms.keySet()) if (kingdoms.get(k).hasTag("Stoic")) stoicNations.add(k);
			if (isHarvestTurn()) {
				for (Region r : regions) r.harvest(stoicNations, World.this);
			}
			for (Region r : regions) r.plant(isHarvestTurn());
		}

		void cedeRegions() {
			for (int i = 0; i < regions.size(); i++) {
				Region r = regions.get(i);
				if (!r.isLand()) continue;
				if (r.noble != null) continue;
				String c = orders.getOrDefault(r.getKingdom(), new HashMap<String, String>()).getOrDefault("nations_cede_" + i, "(Nobody)");
				if (kingdoms.containsKey(c)) {
					if (getNation(c).getRelationship(r.getKingdom()).cede != Relationship.Cede.ACCEPT) {
						notifications.add(new Notification(r.getKingdom(), "Cannot Cede " + r.name, c + " refused to accept rulership of " + r.name + "."));
						notifications.add(new Notification(c, r.name + " Cede Refused", r.getKingdom() + " attempted to cede rulership of " + r.name + " to us, but we refused."));
						continue;
					}
					notifyAllPlayers(r.name + " Ceded", r.name + ", formerly a region of " + r.getKingdom() + ", has been ceded to " + c + ".");
					r.setKingdom(World.this, c);
				}
			}
		}

		void recruitTroops() {
			for (int i = 0; i < regions.size(); i++) {
				Region r = regions.get(i);
				if (NationData.UNRULED_NAME.equals(r.getKingdom())) continue;
				int shipyards = 0;
				for (Construction c: r.constructions) {
					if ("shipyard".equals(c.type)) shipyards++;
				}
				if (shipyards > 0) {
					Army max = getMaxArmyInRegion(i, leaders, inspires, lastStands);
					if (max == null || !NationData.isEnemy(r.getKingdom(), max.kingdom, World.this)) {
						buildShips(r.getKingdom(), i, shipyards * Constants.numShipsBuiltPerShipyard);
					}
				}
			}
			for (String k : kingdoms.keySet()) {
				double signingBonus = Double.parseDouble(orders.getOrDefault(k, new HashMap<String, String>()).getOrDefault("economy_recruit_bonus", "0"));
				double soldiers = 0;
				double likelyRecruits = 0;
				for (Army a : armies) if (k.equals(a.kingdom) && !a.hasTag("Higher Power")) soldiers += a.size;
				for (Region r : regions) if (k.equals(r.getKingdom())) likelyRecruits += r.calcRecruitment(World.this, governors.get(r), signingBonus, battlingNations.contains(r.getKingdom()), rationing.getOrDefault(r.getKingdom(), 1.0), getMaxArmyInRegion(regions.indexOf(r), leaders, inspires, lastStands));
				if (signingBonus > 0 && (soldiers + likelyRecruits) / 100 * signingBonus > getNation(k).gold) signingBonus = getNation(k).gold * 100 / (soldiers + likelyRecruits);
				if (signingBonus > 0 && signingBonus < 1) signingBonus = 0;
				if (signingBonus > 0) {
					for (Army a : armies) if (k.equals(a.kingdom) && !a.hasTag("Higher Power")) {
						getNation(k).gold -= signingBonus * a.size / 100;
						incomeSources.getOrDefault(k, new Budget()).spentRecruits += signingBonus * a.size / 100;
					}
				}
				for (int i = 0; i < regions.size(); i++) {
					Region r = regions.get(i);
					if (r.isSea()) continue;
					if (!r.getKingdom().equals(k)) continue;
					double recruits = r.calcRecruitment(World.this, governors.get(r), signingBonus, battlingNations.contains(r.getKingdom()), rationing.getOrDefault(r.getKingdom(), 1.0), getMaxArmyInRegion(regions.indexOf(r), leaders, inspires, lastStands));
					if (recruits <= 0) continue;
					if (signingBonus > 0) {
						getNation(k).gold -= signingBonus * recruits / 100;
						incomeSources.getOrDefault(k, new Budget()).spentRecruits += signingBonus * recruits / 100;
					}
					List<String> tags = r.getArmyTags();
					Army merge = null;
					outer: for (Army a : armies) {
						if (a.location == i && a.isArmy() && a.kingdom.equals(k)) {
							for (String t : tags) if (!a.hasTag(t)) continue outer;
							merge = a;
							break;
						}
					}
					if (merge != null) {
						merge.size += recruits;
					} else {
						Army army = new Army();
						army.type = Army.Type.ARMY;
						army.size = recruits;
						army.tags = tags;
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

		void sendBudgetNotifications() {
			for (String k : kingdoms.keySet()) {
				Budget b = incomeSources.getOrDefault(k, new Budget());
				String notification = "Our treasurer reports a net change in the treasury of " + Math.round(b.sum()) + " gold:";
				if (b.incomeTax > 0) notification += "\n" + Math.round(b.incomeTax) + " gold earned from taxation.";
				if (b.incomeSea > 0) notification += "\n" + Math.round(b.incomeSea) + " gold earned from sea trade.";
				if (b.incomeChurch > 0) notification += "\n" + Math.round(b.incomeChurch) + " gold charitably donated by the Church of Iruhan.";
				if (b.incomeTribute > 0) notification += "\n" + Math.round(b.incomeTribute) + " gold gained from the tribute of other nations.";
				if (b.incomeGift > 0) notification += "\n" + Math.round(b.incomeGift) + " gold gained from other nations (non-tribute).";
				if (b.incomeRaze > 0) notification += "\n" + Math.round(b.incomeRaze) + " gold gained from razing constructions.";
				if (b.incomeExecution > 0) notification += "\n" + Math.round(b.incomeExecution) + " gold gained from ceremonial execution rituals.";
				if (b.incomeArmyDelivery > 0) notification += "\n" + Math.round(b.incomeArmyDelivery) + " gold delivered from our armies.";
				if (b.spentTribute > 0) notification += "\n" + Math.round(b.spentTribute) + " gold spent paying tribute to other nations.";
				if (b.spentSoldiers > 0) notification += "\n" + Math.round(b.spentSoldiers) + " gold spent to pay our sailors and soldiers.";
				if (b.spentRecruits > 0) notification += "\n" + Math.round(b.spentRecruits) + " gold spent to pay our soldiers' bonuses.";
				if (b.spentConstruction > 0) notification += "\n" + Math.round(b.spentConstruction) + " gold spent constructing buildings.";
				if (b.spentGift > 0) notification += "\n" + Math.round(b.spentGift) + " gold given to other nations (non-tribute).";
				if (b.spentFoodTransfers > 0) notification += "\n" + Math.round(b.spentFoodTransfers) + " gold spent to transfer food.";
				if (b.spentBribes > 0) notification += "\n" + Math.round(b.spentBribes) + " gold spent to bribe pirates.";
				notifications.add(new Notification(k, "Budget", notification));
			}
		}

		void eatFood() {
			HashMap<Region, Double> starvation = new HashMap<>();
			for (int i = 0; i < regions.size(); i++) {
				Region r = regions.get(i);
				if (r.isSea()) continue;
				double hungry = r.calcConsumption(World.this, rationing.getOrDefault(r.getKingdom(), 1.0));
				if (kingdoms.containsKey(r.getKingdom())) {
					double fed = Math.min(hungry, r.food) / hungry * r.population;
					double ration = rationing.getOrDefault(r.getKingdom(), 1.0);
					if (ration > 1) score(r.getKingdom(), "food", fed * Constants.foodFedPlentifulPointFactor);
					else if (ration == 1) score(r.getKingdom(), "food", fed * Constants.foodFedPointFactor);
				}
				if (hungry <= r.food) {
					r.food -= hungry;
				} else {
					double starving = Math.min(r.population - 1, (hungry - r.food) * .05);
					r.food = 0;
					r.unrestPopular = Math.min(1, r.unrestPopular + starving / r.population * 3);
					starvation.put(r, starving);
					addPopulation(r, -starving);
					if (kingdoms.containsKey(r.getKingdom()) && !getNation(r.getKingdom()).coreRegions.contains(i)) score(r.getKingdom(), "food", -1 / 25000.0 * starving);
					for (String k : tributes.getOrDefault(r.getKingdom(), new ArrayList<String>())) score(k, "food", -1 / 12000.0 * starving);
					for (String k : kingdoms.keySet()) if (getNation(k).coreRegions.contains(i)) score(k, "food", -1 / 6000.0 * starving);
				}
			}
			for (Region r : starvation.keySet()) {
				sendRefugees(r, null, starvation.get(r), false, null, true);
			}
			for (String k : kingdoms.keySet()) {
				String notification = "";
				for (Region r : starvation.keySet()) if (r.getKingdom().equals(k)) {
					notification += Math.round(starvation.get(r)) + " have starved to death in " + r.name + ".\n";
				}
				if (!"".equals(notification)) notifications.add(new Notification(k, "Starvation", notification));
			}
		}

		void growPopulations() {
			for (Region r : regions) r.population *= 1.001;
		}

		void evaluateGothiSpells() {
			if (tivar.warwinds) {
				HashSet<Character> moved = new HashSet<>();
				for (Army a : armies) {
					Region r = regions.get(a.location);
					if (r.isSea()) {
						List<Region> n = new ArrayList<>(r.getNeighbors(World.this));
						Region d = n.get((int)(Math.random() * n.size()));
						a.location = regions.indexOf(d);
						if (getAttrition(a, d)) a.size *= .75;
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
				for (Region r : regions) r.crops *= .97;
			}
			if (tivar.quake) {
				HashMap<String, HashMap<String, Integer>> wreckage = new HashMap<>();
				for (Region r : regions) {
					ArrayList<Construction> destroyed = new ArrayList<>();
					for (Construction c : r.constructions) {
						if (Math.random() < 0.33) destroyed.add(c);
					}
					for (Construction c : destroyed) {
						r.constructions.remove(c);
						if (!wreckage.containsKey(r.getKingdom())) wreckage.put(r.getKingdom(), new HashMap<String, Integer>());
						wreckage.get(r.getKingdom()).put(c.type, wreckage.get(r.getKingdom()).getOrDefault(c.type, 0) + 1);
					}
					r.setReligion(null, World.this);
				}
				for (Region r : regions) r.crops *= .97;
				for (String k : wreckage.keySet()) {
					String notification = "The terrible magical earthquakes triggered by the followers of Rjinku have taken their toll on our nation, destroying:";
					for (String t : wreckage.get(k).keySet()) {
						notification += "\n" + wreckage.get(k).get(t) + " " + t + (t.endsWith("s") || wreckage.get(k).get(t) == 1 ? "" : "s");
					}
					notifications.add(new Notification(k, "Earthquakes", notification));
				}
			}
			if (tivar.veil) {
				for (Region r : regions) r.crops *= .97;
			}
			if (tivar.deluge) {
				for (Region r : regions) r.crops *= .97;
				
			}
		}

		void nobleCrises() {
			for (Region r : regions) {
				if (r.noble == null) continue;
				switch (r.noble.crisis.type) {
					case NONE:
						break;
					case WEDDING:
						boolean rulerPresent = false;
						for (Character c : characters) if (c.hasTag("Ruler") && c.kingdom.equals(r.getKingdom()) && c.location == regions.indexOf(r)) rulerPresent = true;
						if (rulerPresent) {
							r.noble.addTag("Loyal");
							notifications.add(new Notification(r.getKingdom(), "Noble Crisis Resolved", "The wedding of the daughter of " + r.noble.name + " was a spectacular affair. It is rare for the nobility to find much joy in their political marriage, but in this case the couple's obvious love for one another warmed your heart to witness. " + r.noble.name + " could not thank you enough for attending, and swore never to forget this day."));
							r.noble.crisis.type = Crisis.Type.NONE;
							r.noble.unrest = Math.max(0, r.noble.unrest - .25);
						}
						break;
					case RECESSION:
						if (getNation(r.getKingdom()).gold >= 140) {
							r.noble.addTag("Frugal");
							notifications.add(new Notification(r.getKingdom(), "Noble Crisis Resolved", "By learning from your example (and taking out a small loan from your treasury), " + r.noble.name + " has solved the financial worries in " + r.name + " and has stimulated the local economy."));
							r.noble.crisis.type = Crisis.Type.NONE;
							r.noble.unrest = Math.max(0, r.noble.unrest - .25);
						}
						break;
					case BANDITRY:
						boolean armyPresent = false;
						for (Army a : armies) if (a.isArmy() && a.calcStrength(World.this, leaders.get(a), inspires, lastStands.contains(a.kingdom)) > r.calcMinPatrolStrength(World.this) && a.location == regions.indexOf(r) && a.kingdom.equals(r.getKingdom())) armyPresent = true;
						if (armyPresent) {
							r.noble.addTag("Policing");
							notifications.add(new Notification(r.getKingdom(), "Noble Crisis Resolved", "With the aid of our troops, " + r.noble.name + " has eliminated the bandit threat from " + r.name + ", and has established a personal police to ensure the region remains secure."));
							r.noble.crisis.type = Crisis.Type.NONE;
							r.noble.unrest = Math.max(0, r.noble.unrest - .25);
						}
						break;
					case BORDER:
						boolean badNeighbor = false;
						for (Region n : r.getNeighbors(World.this)) if (n.getKingdom() != null && NationData.isEnemy(r.getKingdom(), n.getKingdom(), World.this)) badNeighbor = true;
						if (!badNeighbor) {
							r.noble.addTag("Inspiring");
							notifications.add(new Notification(r.getKingdom(), "Noble Crisis Resolved", r.noble.name + " has capitalized on the new security of the borders of " + r.name + ", citing this as an example of the glorious purpose of our armies to potential recruits."));
							r.noble.crisis.type = Crisis.Type.NONE;
							r.noble.unrest = Math.max(0, r.noble.unrest - .25);
						}
						break;
					case ENNUI:
						if (rationing.getOrDefault(r.getKingdom(), 1.0) > 1) {
							r.noble.addTag("Generous");
							notifications.add(new Notification(r.getKingdom(), "Noble Crisis Resolved", r.noble.name + " was refreshed by witnessing the feasting and jubilation in " + r.name + ", and has discovered a new joy in generousity."));
							r.noble.crisis.type = Crisis.Type.NONE;
							r.noble.unrest = Math.max(0, r.noble.unrest - .25);
						}
						break;
					case CULTISM:
						if (templeBuilds.contains(r)) {
							r.noble.addTag("Pious");
							notifications.add(new Notification(r.getKingdom(), "Noble Crisis Resolved", "With people flocking to the new temple in " + r.name + ", " + r.noble.name + " has been able to turn them away from assisting the Cult."));
							r.noble.crisis.type = Crisis.Type.NONE;
							r.noble.unrest = Math.max(0, r.noble.unrest - .25);
						}
						break;
					case OVERWHELMED:
						if (governors.containsKey(r)) {
							r.noble.addTag("Meticulous");
							notifications.add(new Notification(r.getKingdom(), "Noble Crisis Resolved", "Assisted by our governance of " + r.name + ", " + r.noble.name + " has gotten back on their feet - and picked up a trick or two!"));
							r.noble.crisis.type = Crisis.Type.NONE;
							r.noble.unrest = Math.max(0, r.noble.unrest - .25);
						}
						break;
					case UPRISING:
						if (r.unrestPopular <= .5) {
							r.noble.addTag("Soothing");
							notifications.add(new Notification(r.getKingdom(), "Noble Crisis Resolved", "As unrest settles in " + r.name + ", " + r.noble.name + " has made a name for themself among the people, listening to concerns and addressing sources of conflict."));
							r.noble.crisis.type = Crisis.Type.NONE;
							r.noble.unrest = Math.max(0, r.noble.unrest - .25);
						}
						break;
					case STARVATION:
						if (r.food > 0) {
							r.noble.addTag("Rationing");
							notifications.add(new Notification(r.getKingdom(), "Noble Crisis Resolved", "With the immediate starvation in " + r.name + " addressed, " + r.noble.name + " has made reforms in how food is handled or wasted to help ensure that starvation does not become a problem again."));
							r.noble.crisis.type = Crisis.Type.NONE;
							r.noble.unrest = Math.max(0, r.noble.unrest - .25);
						}
						break;
					case GUILD:
						if (builds.contains(r)) {
							r.noble.addTag("Patronizing");
							notifications.add(new Notification(r.getKingdom(), "Noble Crisis Resolved", "By clever hiring of persons to fill our construction order, " + r.noble.name + " has given the guilds in " + r.name + " a reputation of ineffectiveness and curtailed their growth. They have promised to subsidize future constructions in the region as well."));
							r.noble.crisis.type = Crisis.Type.NONE;
							r.noble.unrest = Math.max(0, r.noble.unrest - .25);
						}
						break;
				}
				// Deadlines.
				if (r.noble.crisis.deadline == date) {
					switch (r.noble.crisis.type) {
						case NONE:
							break;
						case WEDDING:
							r.noble.addTag("Snubbed");
							notifications.add(new Notification(r.getKingdom(), "Noble Crisis Expired", "The wedding " + r.noble.name + " invited you to in " + r.name + " has taken place without you and " + r.noble.name + " is very cross."));
							r.noble.unrest = Math.min(1, r.noble.unrest + .12);
							break;
						case RECESSION:
							r.noble.addTag("Hoarding");
							notifications.add(new Notification(r.getKingdom(), "Noble Crisis Expired", "The economic issues in " + r.name + " have been resolved, but " + r.noble.name + " has pledged to never let go of gold so easily again - not even to our tax collectors."));
							r.noble.unrest = Math.min(1, r.noble.unrest + .12);
							break;
						case BANDITRY:
							r.noble.addTag("Shady Connections");
							notifications.add(new Notification(r.getKingdom(), "Noble Crisis Expired", "The bandits in " + r.name + " have been legitimized by the " + r.noble.name + ", in a deal to avoid future incidents. Regrettably, in so doing " + r.name + " has become a gathering place of undesireables."));
							r.noble.unrest = Math.min(1, r.noble.unrest + .12);
							break;
						case BORDER:
							r.noble.addTag("Untrusting");
							notifications.add(new Notification(r.getKingdom(), "Noble Crisis Expired", r.noble.name + " has dealt with their fears by building up a large personal guard. Unfortunately, they take the best of " + r.name + " recruits, leaving the kingdom with only the bottom quality."));
							r.noble.unrest = Math.min(1, r.noble.unrest + .12);
							break;
						case ENNUI:
							r.noble.addTag("Workaholic");
							notifications.add(new Notification(r.getKingdom(), "Noble Crisis Expired", r.noble.name + " has found purpose in their work, but regrettably demands everyone else in " + r.name + " work just as tirelessly."));
							r.noble.unrest = Math.min(1, r.noble.unrest + .12);
							break;
						case CULTISM:
							r.noble.addTag("Cultist");
							notifications.add(new Notification(r.getKingdom(), "Noble Crisis Expired", r.noble.name + " has dealt with the cultists by agreeing to grant them access to the section of " + r.name + " they desire."));
							r.noble.unrest = Math.min(1, r.noble.unrest + .12);
							break;
						case OVERWHELMED:
							r.noble.addTag("Wasteful");
							notifications.add(new Notification(r.getKingdom(), "Noble Crisis Expired", r.noble.name + " has resolved their troubles " + r.name + " but acquired a habit of accepting wastefulness."));
							r.noble.unrest = Math.min(1, r.noble.unrest + .12);
							break;
						case UPRISING:
							r.noble.addTag("Tyrannical");
							notifications.add(new Notification(r.getKingdom(), "Noble Crisis Expired", "The uprising in " + r.name + " came to a head this week when the dissidents stormed the home of " + r.noble.name + " and slew almost all the inhabitants. In grief and rage, " + r.noble.name + " retaliated in kind, wiping out the rebels and their families, and vows to never let this repeat."));
							r.noble.unrest = Math.min(1, r.noble.unrest + .12);
							break;
						case STARVATION:
							r.noble.addTag("Desperate");
							notifications.add(new Notification(r.getKingdom(), "Noble Crisis Expired", "Faced with rampant starvation in " + r.name + ", " + r.noble.name + " has despaired of help from our kingdom and solicited other rulers to take over. We should no longer trust the region's natural defenses."));
							r.noble.unrest = Math.min(1, r.noble.unrest + .12);
							break;
						case GUILD:
							r.noble.addTag("Broke");
							notifications.add(new Notification(r.getKingdom(), "Noble Crisis Expired", r.noble.name + " has dealt with the guilds in " + r.name + " by personally financing a trade war against them. Although successful, they are now thoroughly broke and attempting to rebuild their wealth by high permitting costs. We can expect any construction in the region to be more expensive."));
							r.noble.unrest = Math.min(1, r.noble.unrest + .12);
							break;
					}
					ArrayList<Crisis.Type> possibleCrises = new ArrayList<>();
					if (getNation(r.getKingdom()).gold < 20) possibleCrises.add(Crisis.Type.RECESSION);
					HashSet<Integer> closeRegions = new HashSet<>();
					{
						class Node {
							final int r;
							final int dist;
							Node(int r, int dist) { this.r = r; this.dist = dist; }
						}
						PriorityQueue<Node> queue = new PriorityQueue<>(100, new Comparator<Node>() {
							@Override
							public int compare(Node a, Node b) {
								return a.dist > b.dist ? 1 : a.dist < b.dist ? -1 : 0;
							}
						});
						queue.add(new Node(regions.indexOf(r), 0));
						while (!queue.isEmpty()) {
							Node n = queue.poll();
							if (closeRegions.contains(n.r)) continue;
							closeRegions.add(n.r);
							for (WorldConstantData.Border b : WorldConstantData.borders) {
								if (b.a == n.r && b.size + n.dist < 6) {
									queue.add(new Node(b.b, b.size + n.dist));
								} else if (b.b == n.r && b.size + n.dist < 6) {
									queue.add(new Node(b.a, b.size + n.dist));
								}
							}
						}
					}
					boolean characterInRange = false;
					boolean rulerInRange = false;
					for (Character c : characters) {
						if (c.kingdom.equals(r.getKingdom()) && c.captor.equals("") && closeRegions.contains(c.location)) {
							characterInRange = true;
							if (c.hasTag("Ruler")) rulerInRange = true;
						}
					}
					double troopsInRegion = 0;
					for (Army a : armies) if (a.isArmy() && a.kingdom.equals(r.getKingdom()) && a.location == regions.indexOf(r)) troopsInRegion += a.size;
					boolean unsafeBorder = false;
					for (Region n : r.getNeighbors(World.this)) if (n.getKingdom() != null && NationData.isEnemy(r.getKingdom(), n.getKingdom(), World.this)) unsafeBorder = true;
					if (!r.noble.hasTag("Frugal") && !r.noble.hasTag("Hoarding") && getNation(r.getKingdom()).gold < 20) possibleCrises.add(Crisis.Type.RECESSION);
					if (!r.noble.hasTag("Loyal") && !r.noble.hasTag("Snubbed") && rulerInRange) possibleCrises.add(Crisis.Type.WEDDING);
					if (!r.noble.hasTag("Policing") && !r.noble.hasTag("Shady Connections") && troopsInRegion < 1000) possibleCrises.add(Crisis.Type.BANDITRY);
					if (!r.noble.hasTag("Inspiring") && !r.noble.hasTag("Untrusting") && unsafeBorder) possibleCrises.add(Crisis.Type.BORDER);
					if (!r.noble.hasTag("Generous") && !r.noble.hasTag("Workaholic") && r.unrestPopular > .15 && characterInRange) possibleCrises.add(Crisis.Type.ENNUI);
					if (!r.noble.hasTag("Pious") && !r.noble.hasTag("Cultist") && cultRegions.contains(regions.indexOf(r)) && characterInRange) possibleCrises.add(Crisis.Type.CULTISM);
					if (!r.noble.hasTag("Meticulous") && !r.noble.hasTag("Wasteful") && characterInRange) possibleCrises.add(Crisis.Type.OVERWHELMED);
					if (!r.noble.hasTag("Soothing") && !r.noble.hasTag("Tyrannical") && r.unrestPopular > .5) possibleCrises.add(Crisis.Type.UPRISING);
					if (!r.noble.hasTag("Rationing") && !r.noble.hasTag("Desperate") && r.food == 0) possibleCrises.add(Crisis.Type.STARVATION);
					if (!r.noble.hasTag("Patronizing") && !r.noble.hasTag("Broke") && characterInRange) possibleCrises.add(Crisis.Type.GUILD);
					if (possibleCrises.isEmpty()) {
						r.noble.crisis.type = Crisis.Type.NONE;
						r.noble.crisis.deadline = date + 1;
					} else {
						r.noble.crisis.deadline = date + 6;
						r.noble.crisis.type = possibleCrises.get((int)(Math.random() * possibleCrises.size()));
						switch (r.noble.crisis.type) {
							case NONE:
								break;
							case WEDDING:
								notifications.add(new Notification(r.getKingdom(), "Noble Crisis (Wedding)", r.noble.name + "'s daughter is being wed in " + r.name + " and they request your presence as ruler."));
								break;
							case RECESSION:
								notifications.add(new Notification(r.getKingdom(), "Noble Crisis (Recession)", "Economic issues plague " + r.name + " and " + r.noble.name + " seeks to take out a loan from the kingdom - build up a treasury of at least 140 gold to support them."));
								break;
							case BANDITRY:
								notifications.add(new Notification(r.getKingdom(), "Noble Crisis (Banditry)", "Bow-wielding outlaws in " + r.name + " have been attacking merchants and agents of the nobility. " + r.noble.name + " requests the presence of a sizeable army in the region to help eliminate them."));
								break;
							case BORDER:
								notifications.add(new Notification(r.getKingdom(), "Noble Crisis (Unsafe Border)", r.noble.name + ", the noble ruling " + r.name + ", has become deeply concerned with the neighboring enemy region, and requests that you deal with the situation one way or another."));
								break;
							case ENNUI:
								notifications.add(new Notification(r.getKingdom(), "Noble Crisis (Ennui)", r.noble.name + " has lost the luster of life and sinks into deep depressions. Implement generous rationing to show them the joy in life."));
								break;
							case CULTISM:
								notifications.add(new Notification(r.getKingdom(), "Noble Crisis (Cultism)", r.noble.name + " has written you, warning that the Cult is especially active in " + r.name + " and is gradually taking power there. They suggest that construction of a temple might turn the people back toward a safer religion."));
								break;
							case OVERWHELMED:
								notifications.add(new Notification(r.getKingdom(), "Noble Crisis (Overwhelmed)", r.noble.name + " can't keep up with the stresses of managing a " + r.name + " and has asked for one of our agents to assist by governing it for a week."));
								break;
							case UPRISING:
								notifications.add(new Notification(r.getKingdom(), "Noble Crisis (Uprising)", "The rampant popular unrest " + r.name + " has triggered numerous incidents and " + r.noble.name + " can no longer deal with it on their own. They request the nation take some action, urgently, to decrease popular unrest back to manageable levels."));
								break;
							case STARVATION:
								notifications.add(new Notification(r.getKingdom(), "Noble Crisis (Starvation)", r.noble.name + " feels deeply for their subjects in " + r.name + " and has asked the nation to fix the starvation situation immediately."));
								break;
							case GUILD:
								notifications.add(new Notification(r.getKingdom(), "Noble Crisis (Guild)", r.noble.name + " has notified us that the guilds forming in " + r.name + " are becoming economic powerhouses, threatening to cut out the nobility altogether. They request we construct an improvement in " + r.name + " as part of a plan to out-compete the guilds."));
								break;
						}
					}
				}
			}
		}

		void checkCultVictory() {
			if (!cultRegions.isEmpty()) {
				ArrayList<Integer> removals = new ArrayList<>();
				for (int i : cultRegions) {
					Region r = regions.get(i);
					if (getNation(r.getKingdom()).loyalToCult) removals.add(i);
					if (r.noble != null && r.noble.hasTag("Cultist")) removals.add(i);
					for (Army a : armies) if (a.hasTag("Higher Power") && a.location == i) removals.add(i);
				}
				for (Integer i : removals) cultRegions.remove(i);
				if (cultRegions.size() <= 3) {
					notifyAllPlayers("Cult Objectives Accomplished", "Rumors abound that Cult has accomplished its mysterious objective - the world waits with bated breath.");
				}
			}
		}

		void miscScoreProfiles() {
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
					if (below25 > pop * 0.99999) score(k, "happiness", 1);
					if (below35 > pop * 0.9) score(k, "happiness", 1);
					if (above25 > pop * 0.25) score(k, "happiness", -1);
					if (above50 > pop * 0.33) score(k, "happiness", -2);
				}
			}
			// Riches
			for (String k : kingdoms.keySet()) {
				int richer = 0;
				for (String kk : kingdoms.keySet()) if (getNation(kk).gold > getNation(k).gold) richer++;
				score(k, "riches", richer / (double)kingdoms.size() <= .25 ? 1 : -1);
			}
			// Glory
			for (String k : kingdoms.keySet()) {
				double battles = 0;
				double peace = 0;
				for (Army a : armies) if (a.kingdom.equals(k)) {
					if (battlingArmies.contains(a)) battles += a.size * (a.isNavy() ? 100 : 1);
					else peace += a.size * (a.isNavy() ? 50 : 1);
				}
				double frac = battles / (battles + peace);
				score(k, "glory", frac >= .30 ? 1 : -1);
			}
			// Security
			for (String k : kingdoms.keySet()) {
				int moreSoldiers = 0;
				int moreWarships = 0;
				int warships = 0;
				int soldiers = 0;
				for (Army a : armies) if (a.kingdom.equals(k)) {
					if (a.isArmy()) soldiers += a.size;
					else warships += a.size;
				}
				for (String kk : kingdoms.keySet()) {
					int kWarships = 0;
					int kSoldiers = 0;
					for (Army a : armies) if (a.kingdom.equals(kk)) {
						if (a.isArmy()) kSoldiers += a.size;
						else kWarships += a.size;
					}
					if (kWarships > warships) moreWarships++;
					if (kSoldiers > soldiers) moreSoldiers++;
				}
				if (moreSoldiers == 0 || moreWarships == 0) score(k, "security", 5);
				if (moreWarships >= 3 && moreSoldiers >= 3) score(k, "security", -1);
			}
			// Culture
			for (String k : kingdoms.keySet()) {
				double happyUs = 0;
				double totalUs = 0;
				double happyOther = 0;
				double totalOther = 0;
				for (Region r : regions) if (r.culture != null) {
					if (r.culture.equals(getNation(k).culture)) {
						happyUs += (1 - r.calcUnrest(World.this)) * r.population;
						totalUs += r.population;
					} else {
						happyOther += (1 - r.calcUnrest(World.this)) * r.population;
						totalOther += r.population;
					}
				}
				if (totalUs > 0 && totalOther > 0) score(k, "culture", happyUs / totalUs > happyOther / totalOther ? 2 : -2);
			}
		}

		void appointHeirs() {
			HashSet<String> unruled = new HashSet<>();
			for (String k : kingdoms.keySet()) unruled.add(k);
			for (Character c : characters) if (c.hasTag("Ruler")) unruled.remove(c.kingdom);
			ArrayList<String> remove = new ArrayList<>();
			for (String k : unruled) {
				ArrayList<Character> cc = new ArrayList<Character>();
				for (Character c : characters) if (c.kingdom.equals(k)) cc.add(c);
				if (!cc.isEmpty()) {
					Character c = cc.get((int)(Math.random() * cc.size()));
					c.addTag("Ruler");
					notifyAllPlayers(k + " Succession", c.name + " has emerged as the new de facto ruler of " + k + ".");
					remove.add(k);
				}
			}
			for (String k : remove) unruled.remove(k);
			for (String k : unruled) {
				Character c = new Character();
				c.name = WorldConstantData.getRandomName(WorldConstantData.kingdoms.get(k).culture, Math.random() < 0.5 ? WorldConstantData.Gender.MAN : WorldConstantData.Gender.WOMAN);
				c.honorific = "Protector ";
				c.kingdom = k;
				c.addTag("Ruler");
				c.values.add("food");
				if (Math.random() < 0.1) c.values.add("happiness");
				if (Math.random() < 0.1) c.values.add("territory");
				if (Math.random() < 0.1) c.values.add("glory");
				if (Math.random() < 0.1) c.values.add("religion");
				if (Math.random() < 0.1) c.values.add("ideology");
				if (Math.random() < 0.1) c.values.add("security");
				if (Math.random() < 0.1) c.values.add("riches");
				if (Math.random() < 0.1) c.values.add("culture");
				c.experience.put("general", Math.floor(Math.random() * 10 + 1));
				c.experience.put("admiral", Math.floor(Math.random() * 10 + 1));
				c.experience.put("governor", Math.floor(Math.random() * 10 + 1));
				c.experience.put("spy", Math.floor(Math.random() * 10 + 1));
				List<Region> spawnRegions = new ArrayList<>();
				for (Region r : regions) if (k.equals(r.getKingdom())) spawnRegions.add(r);
				if (spawnRegions.isEmpty()) spawnRegions = regions;
				c.location = regions.indexOf(spawnRegions.get((int)(Math.random() * spawnRegions.size())));
				characters.add(c);
				notifyAllPlayers(k + " Succession", c.name + " has emerged as the new de facto ruler of the greatly weakened " + k + ".");
			}
		}

		void takeFinalActions() {
			for (String k : orders.keySet()) {
				String final_action = orders.get(k).getOrDefault("final_action", "continue_ruling");
				if ("continue_ruling".equals(final_action)) {
					continue;
				}
				Character ruler = null;
				for (Character c : characters) if (k.equals(c.kingdom) && c.hasTag("Ruler")) ruler = c;
				if ("salt_the_earth".equals(final_action)) {
					for (Region r : regions) if (k.equals(r.getKingdom())) {
						r.climate = "treacherous";
						r.food /= 2;
					}
					notifyAllPlayers(k + " Salts the Earth", "In a final act of defiance, " + ruler.name + " orders their agents to destroy their lands, making them unfit for inhabitation. In a week of violence and terror, the people rise up and overthrow their rulers, but not before much of " + k + " is ruined beyond recognition.");
				}
				if ("last_stand".equals(final_action)) {
					for (Army a : armies) if (k.equals(a.kingdom)) a.kingdom = "Pirate";
					notifyAllPlayers(k + " Makes a Final Stand", "In a final act of defiance, " + ruler.name + " impassions their soldiers and unfetters them from the chain of command. They fight heroically, but as the week closes, " + ruler.name + " is rumored to be dead and " + k + " ceases to exist as a nation.");
				}
				if ("exodus".equals(final_action)) {
					double navalStrength = 0;
					double enemyNavalStrength = 1;
					for (Army a : armies) if (a.isNavy()) {
						double strength = a.calcStrength(World.this, leaders.get(a), inspires, lastStands.contains(a.kingdom));
						if (NationData.isFriendly(a.kingdom, k, World.this)) navalStrength += strength;
						if (NationData.isEnemy(a.kingdom, k, World.this)) enemyNavalStrength += strength;
					}
					for (Integer rid : getNation(k).coreRegions) regions.get(rid).population *= 1 - navalStrength / enemyNavalStrength;
					List<Army> removals = new ArrayList<>();
					for (Army a : armies) if (k.equals(a.kingdom)) removals.add(a);
					armies.removeAll(removals);
					notifyAllPlayers(k + " Departs", ruler.name + " has gathered those loyal to them, including " + Math.round(navalStrength / enemyNavalStrength * 100) + "% of the population of their core regions and set sail for distant lands across the sea, perhaps never to be heard from again.");
				}
				if ("abdication".equals(final_action)) {
					notifyAllPlayers(k + " Dissolves", "After deep reflection, " + ruler.name + " has elected to place the affairs of their nation in order and then dissolve its central authority. Its people celebrate the history of their union and look hopefully forward to the future and their right to self-rule.");
				}
				List<Character> removals = new ArrayList<>();
				for (Character c : characters) if (k.equals(c.kingdom)) removals.add(c);
				characters.removeAll(removals);
				List<Army> aremovals = new ArrayList<>();
				for (Army a : armies) if (k.equals(a.kingdom)) {
					if (a.isNavy()) {
						pirate.threat += a.size;
						pirateThreatSources.put("Final Actions", pirateThreatSources.getOrDefault("Final Actions", 0.0) + a.size);
						aremovals.add(a);
					} else {
						a.kingdom = NationData.PIRATE_NAME;
						a.addTag("Unpredictable");
					}
				}
				armies.removeAll(aremovals);
				for (Region r : regions) if (k.equals(r.getKingdom())) r.setKingdom(World.this, "Unruled");
				kingdoms.remove(k);
			}
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
				notifyAllPlayers("Pirate Threat", "Pirate Threat has increased by " + Math.round(totalIncrease * 100) + " pirates, a quarter of which appeared this week. The increase was driven by: " + sources);
			}
		}

		void notifyInspires() {
			inspiresHint = inspires;
		}

		void advanceDate() {
			Season currentSeason = getSeason();
			date++;
			nextTurn = turnSchedule.getNextTime();
			for (String k : kingdoms.keySet()) getNation(k).previousTributes = tributes.get(k);

			// Notify of season change.
			if (getSeason() != currentSeason) {
				if (getSeason() == Season.WINTER) {
					notifyAllPlayers("Winter Arrives", "The days are shortening and the temperature plummets. The air carries the smell of distant snow. It is now winter.");
				} else {
					notifyAllPlayers("Summer Arrives", "The days are lengthening and the flowers beginning to bloom. The air carries the sounds of songbirds. It is now summer.");
				}
			}

			// Notify of upcoming harvest.
			if (isHarvestTurn()) {
				notifyAllPlayers("Harvest Preparations", "People all over the world prepare to reap the harvest upon which they have labored, and hunger is (perhaps only temporarily) banished. All regions will produce their harvest this turn (before eating).");
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
				String ruler = null;
				for (Character c : characters) if (c.kingdom.equals(kingdom) && c.hasTag("Ruler")) ruler = c.honorific + " " + c.name;
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
				email += "\n\nYou can issue your orders at https://pawlicki.kaelri.com/empire/map1.html?g=%GAMEID%.\nIf you wish to retire from the game and give your nation to a player on the wait list, reply \"RETIRE\" to this e-mail.\nIf you wish for the GM or AI to make move on your behalf this turn, reply \"AUTO\" to this e-mail.";
				emails.put(getNation(kingdom).email, email);
			}
			return emails;
		}
	}

	private void addPopulation(Region r, double amount) {
		r.population += amount;
		int rid = regions.indexOf(r);
	}

	private int getNewArmyId() {
		int nextId = 1;
		for (Army aa : armies) if (aa.id >= nextId) nextId = aa.id + 1;
		return nextId;
	}

	private boolean getAttrition(Army army, Region region) {
		if (army.hasTag("Weathered")) return false;
		if (region.isLand() && NationData.isFriendly(region.getKingdom(), army.kingdom, this)) return false;
		if (region.climate.equals("treacherous") || (region.climate.equals("seasonal") && getSeason() == Season.WINTER)) return true;
		if (region.isLand() && tivar.deluge) return true;
		if (region.type.equals("sea") && tivar.warwinds) return true;
		if ("Pirate".equals(army.kingdom) && region.getKingdom() != null && getNation(region.getKingdom()).hasTag("Disciplined")) return true;
		return false;
	}

	private void sortByStrength(List<Army> actors, Map<Army, Character> leaders, int finspires, HashSet<String> lastStands) {
		Collections.sort(actors, (Army a, Army b) -> {
			double as = a.calcStrength(this, leaders.get(a), finspires, lastStands.contains(a.kingdom));
			double bs = b.calcStrength(this, leaders.get(b), finspires, lastStands.contains(b.kingdom));
			return as > bs ? -1 : as < bs ? 1 : 0;
		});
	}

	private void buildShips(String kingdom, int location, double amount) {
		for (Army a : armies) {
			if (a.isNavy() && a.location == location && a.kingdom.equals(kingdom)) {
				a.size += amount;
				return;
			}
		}
		Army a = new Army();
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
				pop.put(r.religion, pop.getOrDefault(r.religion, 0.0) + r.population * (r.noble != null && r.noble.hasTag("Pious") ? 3 : 1));
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

	private void sendRefugees(Region from, Region to, double amount, boolean notify, String whoBlame, boolean foodOnly) {
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
			if (getNation(target.getKingdom()).hasTag("Nomadic")) {
				targetUnrestFactor = 0.2;
				fromUnrestFactor = 0.2;
			}
			for (String k : kingdoms.keySet()) if (getNation(k).hasTag("Ruined") && getNation(k).coreRegions.contains(regions.indexOf(target)) && !getNation(k).coreRegions.contains(regions.indexOf(from))) {
				targetUnrestFactor = 0;
				fromUnrestFactor = 0;
			}
			if (Ideology.CHALICE_OF_COMPASSION == getDominantIruhanIdeology()) {
				if (target.religion.religion == Religion.IRUHAN) targetUnrestFactor = 0;
				if (from.religion.religion == Religion.IRUHAN) fromUnrestFactor = 0;
			}
			target.unrestPopular = amount * targetUnrestFactor / destinations.size() / (target.population + amount / destinations.size()) + target.unrestPopular * target.population / (target.population + amount / destinations.size());
			from.unrestPopular = Math.min(1, from.unrestPopular + amount * fromUnrestFactor / destinations.size() * 2 / (from.population - amount / destinations.size()));
			addPopulation(target, amount / destinations.size());
			addPopulation(from, -amount / destinations.size());
			if (notify) notifications.add(new Notification(target.getKingdom(), "Refugees Arrive in " + target.name, "Refugees from " + from.name + " have arrived in " + target.name + "." + (targetUnrestFactor > 0 ? " They are distraught and upset, causing increased popular unrest." : "")));
			if (whoBlame != null) {
				getNation(whoBlame).goodwill -= (targetUnrestFactor + fromUnrestFactor) / 2 / 5000 * amount;
			}
		}
	}

	private class Plot implements Comparable<Plot> {
		final String perpetrator;
		final String action;
		final PlotType type;
		final String target;
		final Map<String, Double> power;
		final int targetRegion;
		final Collection<Character> leaders;

		public Plot(String perpetrator, PlotType type, String target, String action, List<String> boosts, int inspires, Collection<Character> leaders) {
			this.perpetrator = perpetrator;
			this.type = type;
			this.target = target;
			this.action = action;
			this.leaders = leaders;
			int targetRegion = -1;
			switch (type) {
				case CHARACTER:
					for (Character c : characters) if (c.name.equals(target)) {
						targetRegion = c.location;
					}
					break;
				case REGION:
					for (int i = 0; i < regions.size(); i++) if (regions.get(i).name.equals(target)) {
						targetRegion = i;
					}
					break;
				case CHURCH:
					for (int i = 0; i < regions.size(); i++) if (regions.get(i).name.equals("Sancta Civitate")) {
						targetRegion = i;
					}
					break;
				case INTERNATIONAL:
					for (Character c : characters) if (c.kingdom.equals(target) && c.hasTag("Ruler")) {
						targetRegion = c.location;
					}
					break;
			}
			this.targetRegion = targetRegion;
			if (targetRegion != -1) this.power = regions.get(targetRegion).calcPlotPowers(World.this, boosts, inspires);
			else this.power = new HashMap<>();
		}

		@Override
		public int compareTo(Plot other) {
			return power.get(perpetrator) > other.power.get(other.perpetrator) ? -1 : power.get(perpetrator) < other.power.get(other.perpetrator) ? 1 : 0;
		}

		public void evaluate(Map<String, Map<String, String>> orders) {
			if (targetRegion == -1) {
				notifications.add(new Notification(perpetrator, "Plot Invalid", type == PlotType.INTERNATIONAL ? target + " currently has no ruler, therefore our plot could not be enacted." : "The target of our plot does not exist."));
				return;
			}
			String defender = "";
			switch (type) {
				case CHARACTER:
					for (Character c : characters) if (c.name.equals(target)) {
						defender = "".equals(c.captor) ? c.kingdom : c.captor;
					}
					if ("".equals(defender)) {
						notifications.add(new Notification(perpetrator, "Plot Pre-empted", target + " was killed and therefore our plot could not be enacted."));
						return;
					}
					break;
				case REGION:
					for (int i = 0; i < regions.size(); i++) if (regions.get(i).name.equals(target)) {
						defender = regions.get(i).getKingdom();
					}
					break;
				case CHURCH:
					for (int i = 0; i < regions.size(); i++) if (regions.get(i).name.equals("Sancta Civitate")) {
						defender = regions.get(i).getKingdom();
					}
					break;
				case INTERNATIONAL:
					defender = target;
					break;
			}
			boolean success = power.get(perpetrator) > power.get(defender) || perpetrator.equals(defender);
			boolean partialSuccess = success;
			if (type == PlotType.CHURCH) {
				success = success || (NationData.isFriendly(target, defender, World.this) && "praise".equals(action)) || (NationData.isEnemy(target, defender, World.this) && "denounce".equals(action));
			} else if (type == PlotType.CHARACTER) {
				for (Character cc : leaders) if (cc.name.equals(target) && power.get(perpetrator) < power.get(defender) * 1.5) partialSuccess = false;
			}
			String title = success ? (partialSuccess ? "Successful Plot: " : "Partially Successful Plot: ") : "Failed Plot: ";
			String details = "Our spies have become aware of " + (success ? "a successful" : "an unsuccessful") + " plot to ";
			if (type == PlotType.REGION) {
				if ("burn".equals(action)) {
					title += "Burn Food in " + regions.get(targetRegion).name;
					details += "destroy " + Math.round(regions.get(targetRegion).food / 2000) + "k measures of food in " + regions.get(targetRegion).name;
					if (success) regions.get(targetRegion).food /= 2;
				} else if ("rebel".equals(action)) {
					title += "Incite Rebellion in " + regions.get(targetRegion).name;
					details += "incite popular unrest in " + regions.get(targetRegion).name;
					if (success) regions.get(targetRegion).unrestPopular = Math.min(1, regions.get(targetRegion).unrestPopular + 0.4);
				} else {
					throw new RuntimeException("Unreocognized plot action: " + action);
				}
			} else if (type == PlotType.CHARACTER) {
				Character c = null;
				for (Character cc : characters) if (cc.name.equals(target)) c = cc;
				String order = "";
				if (c != null && orders.get(c.kingdom) != null) {
					order = "They were ordered to " + orders.get(c.kingdom).get("action_" + c.name.replace(" ", "_").replace("'", "_"));
				}
				if ("find".equals(action)) {
					title += "Find " + target;
					details += "locate " + target + (c != null ? ", a hero of " + c.kingdom : "") + ".";
					if (success) {
						if (c == null) notifications.add(new Notification(perpetrator, "Location of " + target, "We located " + target + " in " + regions.get(targetRegion).name + ", but by the time our agents got there, they were already dead. " + order));
						else notifications.add(new Notification(perpetrator, "Location of " + target, "We have located " + target + " in " + regions.get(targetRegion).name + ". " + order));
					}
				} else if ("arrest".equals(action)) {
					title += "Arrest " + target;
					details += "locate " + target + " and arrest them if trespassing.";
					if (success) {
						if (c == null) {
							notifications.add(new Notification(perpetrator, "Location of " + target, "We located " + target + " in " + regions.get(targetRegion).name + ", but by the time our agents got there, they were already dead. " + order));
						} else {
							notifications.add(new Notification(perpetrator, "Location of " + target, "We have located " + target + " in " + regions.get(targetRegion).name + ". " + order));
							if (perpetrator.equals(regions.get(targetRegion).getKingdom())) {
								if (partialSuccess) {
									details += " They were trespassing and arrested.";
									c.captor = perpetrator; 
									c.orderhint = "";
								} else {
									details += " The soldiers they were leading saved them from arrest.";
								}
							} else {
								details += " They were not trespassing and therefore left alone.";
							}
						}
					}
				} else if ("capture".equals(action)) {
					title += "Abduct " + target;
					details += "abduct " + target + (c != null ? ", a hero of " + c.kingdom : "") + ".";
					if (success) {
						if (c == null) {
							notifications.add(new Notification(perpetrator, "Location of " + target, "We located " + target + " in " + regions.get(targetRegion).name + ", but by the time our agents got there, they were already dead. " + order));
						} else {
							notifications.add(new Notification(perpetrator, "Location of " + target, "We have located " + target + " in " + regions.get(targetRegion).name + ". " + order));
							if (partialSuccess) {
								c.captor = perpetrator; 
								c.orderhint = "";
							} else {
								details += " The soldiers they were leading saved them from captured.";
							}
						}
					}
				} else if ("rescue".equals(action)) {
					title += "Rescue " + target;
					details += "free " + target + (c != null ? ", a hero of " + c.kingdom : "") + " from captivity.";
					if (success) {
						if (c == null) {
							notifications.add(new Notification(perpetrator, "Location of " + target, "We located " + target + " in " + regions.get(targetRegion).name + ", but by the time our agents got there, they were already dead. " + order));
						} else if ("".equals(c.captor)) {
							notifications.add(new Notification(perpetrator, "Location of " + target, "We located " + target + " in " + regions.get(targetRegion).name + ", but by the time our agents got there, they were not in captivity. " + order));
						} else {
							notifications.add(new Notification(perpetrator, "Location of " + target, "We have located " + target + " in " + regions.get(targetRegion).name + ". " + order));
							c.captor = ""; 
							c.orderhint = "";
						}
					}
				} else if ("kill".equals(action)) {
					title += "Assassinate " + target;
					details += "murder " + target + (c != null ? ", a hero of " + c.kingdom : "") + ".";
					if (success) {
						if (c == null) {
							notifications.add(new Notification(perpetrator, "Location of " + target, "We located " + target + " in " + regions.get(targetRegion).name + ", but by the time our agents got there, they were already dead. " + order));
						} else {
							if (partialSuccess) {
								notifications.add(new Notification(perpetrator, "Location of " + target, "We have located " + target + " in " + regions.get(targetRegion).name + ". " + order));
								characters.remove(c);
								if (c.hasTag("Ruler")) {
									notifications.add(new Notification(c.kingdom, c.name + " Killed", " You have been killed. Your nation mourns, but your government is prepared for this eventuality, and another ruler rises to power. Your new ruler may have different values and therefore change what you earn or lose score points for. Points accumulated so far are kept."));
								}
							} else {
								details += " The soldiers they were leading saved them from being killed.";
							}
						}
					}
				} else {
					throw new RuntimeException("Unreocognized plot action: " + action);
				} 
			} else if (type == PlotType.CHURCH) {
				double amount = 10;
				if (Ideology.VESSEL_OF_FAITH != getDominantIruhanIdeology()) for (Character c : characters) if (c.hasTag("Tiecel") && c.kingdom.equals(perpetrator)) amount = 50;
				if ("praise".equals(action)) {
					title += "Praise " + target + " in Sancta Civitate";
					details += "sing the praises of " + target + " among the clergy of Iruhan.";
					if (success) {
						getNation(target).goodwill += amount;
					}
				} else if ("denounce".equals(action)) {
					title += "Denounce " + target + " in Sancta Civitate";
					details += "announce and condemn the crimes of " + target + " among the clergy of Iruhan.";
					if (success) {
						getNation(target).goodwill -= amount;
					}
				} else {
					throw new RuntimeException("Unreocognized plot action: " + action);
				} 
			} else if (type == PlotType.INTERNATIONAL) {
				if ("eavesdrop".equals(action)) {
					title += "Eavesdrop on " + target;
					details += "intercept and monitor the communications of " + target + ".";
					if (success) {
						for (Communication c : communications) {
							if (c.postDate == date && (target.equals(c.from) || c.to.contains(target))) c.intercepted.add(perpetrator);
						}
					}
				} else {
					throw new RuntimeException("Unreocognized plot action: " + action);
				} 
			}
			ArrayList<String> knows = new ArrayList<>();
			for (String kingdom : power.keySet()) {
				if (kingdom.equals(perpetrator)) continue;
				ArrayList<String> suspects = new ArrayList<>();
				if (power.get(kingdom) > power.get(perpetrator)) {
					suspects.add(perpetrator);
				} else {
					for (String k : power.keySet()) {
						if (k.equals(kingdom)) continue;
						if (power.get(k) <= power.get(kingdom)) continue;
						suspects.add(k);
					}
				}
				if (suspects.size() == 1) {
					knows.add(kingdom);
					notifications.add(new Notification(kingdom, title, details + "\nWe are confident that the plot was orchestrated by " + suspects.get(0) + "."));
				} else {
					notifications.add(new Notification(kingdom, title, details + "\nWe are confident that the plot was orchestrated by one of " + String.join(", ", suspects) + "."));
				}
			}
			notifications.add(new Notification(perpetrator, "Our " + title, "We were discovered by " + String.join(", ", knows) + ". Other nations likely suspect us but lack proof."));
			if (knows.size() >= 5) {
				for (Region r : regions) {
					if (perpetrator.equals(r.getKingdom()) && Ideology.LYSKR == r.religion) r.unrestPopular = Math.min(1, r.unrestPopular + .1);
				}
			}
		}
	}

	private static enum PlotType {
		CHARACTER,
		REGION,
		CHURCH,
		INTERNATIONAL;
	}

	void notifyAllPlayers(String title, String notification) {
		for (String k : kingdoms.keySet()) {
			notifications.add(new Notification(k, title, notification));
		}
	}

	void score(String kingdom, String profile, double amount) {
		for (Character c : characters) if (kingdom.equals(c.kingdom) && c.hasTag("Ruler") && c.values.contains(profile)) getNation(kingdom).score.put(profile, getNation(kingdom).score.getOrDefault(profile, 0.0) + amount);
	}

	private Army getMaxArmyInRegion(int location, Map<Army, Character> leaders, int inspires, HashSet<String> lastStands) {
		Army max = null;
		for (Army b : armies) if (b.location == location && b.isArmy()) {
			if (max == null || max.calcStrength(this, leaders.get(max), inspires, lastStands.contains(max.kingdom)) < b.calcStrength(this, leaders.get(b), inspires, lastStands.contains(b.kingdom))) max = b;
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
		if (NationData.getStateReligion(kingdom, this) == Ideology.ALYRJA) {
			if (isHiddenAlyrjaHelper(a.location, kingdom)) return false;
		}
		if (tivar.veil) return true;
		if (!a.hasTag("Raiders") || regions.get(a.location).getKingdom() == null || kingdom.equals(regions.get(a.location).getKingdom()) || !NationData.isFriendly(a.kingdom, regions.get(a.location).getKingdom(), this)) return false;
		return true;
	}

	private boolean isHidden(Character c, String kingdom) {
		if (c.kingdom.equals(kingdom)) return false;
		if (c.captor.equals(kingdom)) return false;
		if (NationData.getStateReligion(kingdom, this) == Ideology.ALYRJA) {
			if (isHiddenAlyrjaHelper(c.location, kingdom)) return false;
		}
		if (c.hidden) return true;
		if (c.leadingArmy != -1) {
			Army a = null;
			for (Army i : armies) if (i.id == c.leadingArmy) a = i;
			if (a != null && isHidden(a, kingdom)) return true;
		}
		if (tivar.veil) return true;
		return false;
	}

	// Filter the data to a specific kingdom's point of view.
	public void filter(String kingdom) {
		gmPasswordHash = "";
		obsPasswordHash = "";
		for (String k : kingdoms.keySet()) {
			getNation(k).password = "";
			getNation(k).email = "";
			getNation(k).accessToken = "";
		}
		if ("(Observer)".equals(kingdom)) return;
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
		// Filter communications.
		ArrayList<Communication> removeComm = new ArrayList<>();
		for (Communication c : communications) {
			if (c.from.equals(kingdom) || c.to.contains(kingdom) || c.intercepted.contains(kingdom)) continue;
			removeComm.add(c);
		}
		for (Communication c : removeComm) communications.remove(c);
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
		// Filter RTC.
		ArrayList<Message> removeList = new ArrayList<Message>();
		for (Message m : rtc) if (!m.from.equals(kingdom) && !m.to.contains(kingdom)) removeList.add(m);
		for (Message m : removeList) rtc.remove(m);
	}

	static enum Season {
		SUMMER,
		WINTER
	}

	public Season getSeason() {
		return (date + 52 - 13) % 52 < 26 ? Season.SUMMER: Season.WINTER;
	}

	public boolean isHarvestTurn() {
		return (date + 52 - 12) % 13 == 0;
	}

	public int turnsUntilHarvest() {
		return (13 - ((date + 52 - 12) % 13)) % 13;
	}

	@Override
	public double getGoodwill(String nation) {
		return getNation(nation).goodwill;
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
	List<String> intercepted = new ArrayList<>();
	String text = "";
	int postDate = -1;
}

class Pirate {
	double threat;
	Map<String, Double> bribes = new HashMap<>();
}

final class Tivar {
	boolean warwinds;
	boolean deluge;
	boolean quake;
	boolean veil;
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

final class Message {
	String from;
	List<String> to;
	String text;
}
