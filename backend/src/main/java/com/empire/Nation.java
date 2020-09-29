package com.empire;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.security.SecureRandom;

public class Nation {
	enum Gothi {
		@SerializedName("Alyrja") ALYRJA,
		@SerializedName("Rjinku") RJINKU,
		@SerializedName("Lyskr") LYSKR,
		@SerializedName("Syrjen") SYRJEN,
	}

	enum Tag {	
		@SerializedName("Aristocratic") ARISTOCRATIC,
		@SerializedName("Defensive") DEFENSIVE,
		@SerializedName("Disciplined") DISCIPLINED,
		@SerializedName("Evangelical") EVANGELICAL,
		@SerializedName("Heroic") HEROIC,
		@SerializedName("Holy") HOLY,
		@SerializedName("Imperialistic") IMPERIALISTIC,
		@SerializedName("Industrial") INDUSTRIAL,
		@SerializedName("Mercantile") MERCANTILE,
		@SerializedName("Mystical") MYSTICAL,
		@SerializedName("Patriotic") PATRIOTIC,
		@SerializedName("Rebellious") REBELLIOUS,
		@SerializedName("Republican") REPUBLICAN,
		@SerializedName("Seafaring") SEAFARING,
		@SerializedName("Ship-Building") SHIP_BUILDING,
		@SerializedName("Sneaky") SNEAKY,
		@SerializedName("Stoic") STOIC,
		@SerializedName("War-like") WARLIKE,
		@SerializedName("Welcoming") WELCOMING;
	}

	enum ScoreProfile {
		CULTIST,
		GLORY,
		HAPPINESS,
		PROSPERITY,
		RELIGION,
		RICHES,
		TERRITORY;
	}

	public static final String PIRATE_NAME = "Pirate";
	public static final String UNRULED_NAME = "Unruled";
	public static final Nation UNRULED;
	public static final Nation PIRATE;
	static {
		UNRULED = new Nation(UNRULED_NAME) {
			@Override
			Relationship getRelationship(String who) { return Relationship.NPC_RELATION; }
		};
		PIRATE = new Nation(PIRATE_NAME) {
			@Override
			Relationship getRelationship(String who) { return Relationship.NPC_RELATION; }
		};
	}

	static boolean isFriendly(String a, String b, World w) {
		if (a.equals(b)) return true;
		Nation aa = w.getNation(a);
		Nation bb = w.getNation(b);
		return Relationship.War.DEFEND == aa.getRelationship(b).battle && Relationship.War.DEFEND == bb.getRelationship(a).battle;
	}

	static boolean isEnemy(String a, String b, World w) {
		return isEnemy(a, b, w, null);
	}

	static boolean isEnemy(String a, String b, World w, Region region) {
		if (a.equals(b)) return false;
		Nation aa = w.getNation(a);
		Nation bb = w.getNation(b);
		if (Relationship.War.ATTACK == aa.getRelationship(b).battle || Relationship.War.ATTACK == bb.getRelationship(a).battle) {
			return true;
		}
		if (region != null && region.getKingdom() != null && region.getKingdom().equals(a) && Relationship.War.NEUTRAL == aa.getRelationship(b).battle) return true;
		if (region != null && region.getKingdom() != null && region.getKingdom().equals(b) && Relationship.War.NEUTRAL == bb.getRelationship(a).battle) return true;
		return false;
	}

	static boolean isAttackingOnSight(String a, String b, World w) {
		return !a.equals(b) && w.getNation(a).getRelationship(b).battle == Relationship.War.ATTACK;
	}

	static Ideology getStateReligion(String kingdom, World w) {
		HashMap<Ideology, Double> weights = new HashMap<>();
		for (Region r : w.regions) {
			if (!kingdom.equals(r.getKingdom())) continue;
			weights.put(r.religion, weights.getOrDefault(r.religion, 0.0) + r.population);
		}
		Ideology max = Ideology.COMPANY;
		double maxVal = 0;
		for (Ideology n : weights.keySet()) {
			if (weights.get(n) > maxVal) {
				maxVal = weights.get(n);
				max = n;
			}
		}
		return max;
	}

	// Instance members.
	public String name;
	public String email;

	double gold;
	Map<Gothi, Boolean> gothi = new HashMap<>();
	boolean loyalToCult;
	String colorFg;
	String colorBg;
	Culture culture;
	List<Integer> coreRegions = new ArrayList<>();
	List<String> previousTributes = new ArrayList<>();
	String taxratehint = "100";
	String shipratehint = "5";
	String rationhint = "100";
	String signingbonushint = "0";
	int idleStrikes = 0;
	boolean sendEmail = true;

	private double goodwill;
	private List<Tag> tags = new ArrayList<>();
	private Map<String, Relationship> relationships = new HashMap<>();
	private Map<ScoreProfile, Double> score = new HashMap<>();
	private Map<ScoreProfile, Double> shadowScore = new HashMap<>(); // shadowScore tracks points the ruler would have scored, if they cared for the profile.
	private Set<ScoreProfile> profiles = new HashSet<>();
	private Map<String, Double> leverage = new HashMap<>();

	transient double goldStolenLost = 0;
	transient double goldStolenGained = 0;

	public Nation(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	Relationship getRelationship(String who) {
		if (UNRULED_NAME.equals(who) || PIRATE_NAME.equals(who)) return Relationship.NPC_RELATION;
		return relationships.get(who);
	}

	void setRelationship(String who, Relationship r) {
		relationships.put(who, r);
	}

	double getLeverage(String who) {
		return leverage.getOrDefault(who, 0.0);
	}

	void addLeverage(World w, String who, double amount, double mod) {
		if (amount > 0) {
			if (w.tivar.veil != 0) mod += 1;
			if (hasTag(Nation.Tag.SNEAKY)) mod += 0.25;
			if (who.equals(name)) {
				mod += 0.1;
			} else {
				Relationship.War war = w.getNation(who).getRelationship(name).battle;
				if (war == Relationship.War.ATTACK) mod -= 0.5;
				else if (war == Relationship.War.DEFEND) mod += 0.5;
			}
		}
		leverage.put(who, Math.max(0, leverage.getOrDefault(who, 0.0) + amount * Math.max(0, mod)));
	}

	void decayLeverage() {
		for (String k : leverage.keySet()) {
			leverage.put(k, leverage.get(k) * 0.85);
		}
	}

	boolean hasTag(Tag tag) {
		return tags.contains(tag);
	}

	void addTag(Tag tag) {
		tags.add(tag);
	}

	void addProfile(ScoreProfile p) {
		profiles.add(p);
	}

	boolean hasProfile(ScoreProfile p) {
		return profiles.contains(p);
	}

	void toggleProfile(ScoreProfile p) {
		if (profiles.contains(p)) profiles.remove(p);
		else profiles.add(p);
	}

	void clearProfiles() {
		profiles.clear();
	}

	void removeProfile(ScoreProfile p) {
		profiles.remove(p);
	}

	void addGoodwill(double amount) {
		if (hasTag(Tag.HOLY) && amount > 0) amount *= 2;
		goodwill += amount;
	}

	double getGoodwill() {
		return goodwill;
	}

	void score(ScoreProfile p, double amount) {
		if (profiles.contains(p)) score.put(p, score.getOrDefault(p, 0.0) + amount);
		else shadowScore.put(p, shadowScore.getOrDefault(p, 0.0) + amount);
	}

	public double getScore() {
		double s = 0;
		for (Double d : score.values()) s += d;
		return s;
	}

	void filterForView(boolean otherNation) {
		email = "";
		idleStrikes = 0;
		if (otherNation) {
			sendEmail = false;
			score = new HashMap<>();
			shadowScore = new HashMap<>();
			profiles = new HashSet<>();
			leverage = new HashMap<>(); 
		}
	}
}

