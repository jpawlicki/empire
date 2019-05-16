package com.empire;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.security.SecureRandom;

class NationData {
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

	static boolean rulerValues(String kingdom, String value, World w) {
		for (Character c : w.characters) if (c.kingdom.equals(kingdom) && c.hasTag("Ruler")) {
			return c.values.contains(value);
		}
		return false;
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

	static Ideology getStateReligion(String kingdom, World w) {
		HashMap<Ideology, Double> weights = new HashMap<>();
		for (Region r : w.regions) {
			if (!kingdom.equals(r.getKingdom())) continue;
			weights.put(r.religion, weights.getOrDefault(r.religion, 0.0) + r.population * (r.noble != null && r.noble.hasTag("Pious") ? 3 : 1));
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

	HashMap<String, Double> score = new HashMap<>();
	double gold;
	private Map<String, Relationship> relationships = new HashMap<>();
	Map<String, Boolean> gothi = new HashMap<>();
	double goodwill;
	boolean loyalToCult;
	List<Noble> court = new ArrayList<>();
	String colorFg;
	String colorBg;
	Culture culture;
	List<Integer> coreRegions = new ArrayList<>();
	private ArrayList<String> tags = new ArrayList<>();
	List<String> previousTributes = new ArrayList<>();
	String taxratehint = "100";
	String rationhint = "100";
	String signingbonushint = "0";
	String password;
	String email;
	String accessToken;

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

	boolean hasTag(String tag) {
		return tags.contains(tag);
	}

	void addTag(String tag) {
		tags.add(tag);
	}
}

