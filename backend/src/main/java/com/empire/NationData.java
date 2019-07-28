package com.empire;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.security.SecureRandom;

public class NationData {
	enum Gothi {
		@SerializedName("Alyrja") ALYRJA,
		@SerializedName("Rjinku") RJINKU,
		@SerializedName("Lyskr") LYSKR,
		@SerializedName("Syrjen") SYRJEN,
	}

	enum Tag {	
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
		@SerializedName("Stoic") STOIC,
		@SerializedName("War-like") WARLIKE,
		@SerializedName("Welcoming") WELCOMING;
	}

	enum ScoreProfile {
		CULTURE,
		GLORY,
		HAPPINESS,
		IDEOLOGY,
		PROSPERITY,
		RELIGION,
		RICHES,
		SECURITY,
		TERRITORY;
	}

	public static final String PIRATE_NAME = "Pirate";
	public static final String UNRULED_NAME = "Unruled";
	public static final NationData UNRULED;
	public static final NationData PIRATE;
	static {
		UNRULED = new NationData() {
			@Override
			Relationship getRelationship(String who) { return Relationship.NPC_RELATION; }
		};
		PIRATE = new NationData() {
			@Override
			Relationship getRelationship(String who) { return Relationship.NPC_RELATION; }
		};
	}

	static boolean isFriendly(String a, String b, World w) {
		if (a.equals(b)) return true;
		NationData aa = w.getNation(a);
		NationData bb = w.getNation(b);
		return Relationship.War.DEFEND == aa.getRelationship(b).battle && Relationship.War.DEFEND == bb.getRelationship(a).battle;
	}

	static boolean isEnemy(String a, String b, World w) {
		return isEnemy(a, b, w, null);
	}

	static boolean isEnemy(String a, String b, World w, Region region) {
		if (a.equals(b)) return false;
		NationData aa = w.getNation(a);
		NationData bb = w.getNation(b);
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

	private Map<ScoreProfile, Double> score = new HashMap<>();
	private Map<ScoreProfile, Double> shadowScore = new HashMap<>(); // shadowScore tracks points the ruler would have scored, if they cared for the profile.
	private Set<ScoreProfile> profiles = new HashSet<>();
	double gold;
	private Map<String, Relationship> relationships = new HashMap<>();
	Map<Gothi, Boolean> gothi = new HashMap<>();
	double goodwill;
	boolean loyalToCult;
	List<Noble> court = new ArrayList<>();
	String colorFg;
	String colorBg;
	Culture culture;
	List<Integer> coreRegions = new ArrayList<>();
	private List<Tag> tags = new ArrayList<>();
	List<String> previousTributes = new ArrayList<>();
	String taxratehint = "100";
	String shipratehint = "5";
	String rationhint = "100";
	String signingbonushint = "0";
	String password;
	String email;
	String accessToken;

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	void resetAccessToken() {
		accessToken = Long.toString(new SecureRandom().nextLong(), java.lang.Character.MAX_RADIX);
	}

	Relationship getRelationship(String who) {
		if (UNRULED_NAME.equals(who) || PIRATE_NAME.equals(who)) return Relationship.NPC_RELATION;
		return relationships.get(who);
	}

	void setRelationship(String who, Relationship r) {
		relationships.put(who, r);
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

	void score(ScoreProfile p, double amount) {
		if (profiles.contains(p)) score.put(p, score.getOrDefault(p, 0.0) + amount);
		else shadowScore.put(p, shadowScore.getOrDefault(p, 0.0) + amount);
	}

	void filterForView(boolean filterScore) {
		password = "";
		email = "";
		accessToken = "";
		if (filterScore) {
			score = new HashMap<>();
			shadowScore = new HashMap<>();
			profiles = new HashSet<>();
		}
	}
}

