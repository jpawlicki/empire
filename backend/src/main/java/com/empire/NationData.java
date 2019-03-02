package com.empire;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.security.SecureRandom;

final class NationData {
	HashMap<String, Double> score = new HashMap<>();
	double gold;
	Map<String, Relationship> relationships = new HashMap<>();
	Map<String, Boolean> gothi = new HashMap<>();
	double goodwill;
	boolean loyalToCult;
	List<Noble> court = new ArrayList<>();
	String colorFg;
	String colorBg;
	String culture;
	List<Integer> coreRegions = new ArrayList<>();
	ArrayList<String> tags = new ArrayList<>();
	ArrayList<String> previousTributes = new ArrayList<>();
	String taxratehint;
	String rationhint;
	String signingbonushint;
	String password;
	String email;
	String accessToken;

	void resetAccessToken() {
		accessToken = Long.toString(new SecureRandom().nextLong(), java.lang.Character.MAX_RADIX);
	}

	static boolean rulerValues(String kingdom, String value, World w) {
		for (Character c : w.characters) if (c.kingdom.equals(kingdom) && c.tags.contains("Ruler")) {
			return c.values.contains(value);
		}
		return false;
	}

	static boolean isFriendly(String a, String b, World w) {
		if (a.equals(b)) return true;
		if (a.equals("Pirate") || b.equals("Pirate")) return false;
		NationData aa = w.kingdoms.get(a);
		NationData bb = w.kingdoms.get(b);
		return Relationship.War.DEFEND == aa.relationships.get(b).battle && Relationship.War.DEFEND == bb.relationships.get(a).battle;
	}

	static boolean isEnemy(String a, String b, World w) {
		return isEnemy(a, b, w, null);
	}

	static boolean isEnemy(String a, String b, World w, Region region) {
		if (a.equals(b)) return false;
		if (a.equals("Pirate") || b.equals("Pirate")) return true;
		NationData aa = w.kingdoms.get(a);
		NationData bb = w.kingdoms.get(b);
		if (Relationship.War.ATTACK == aa.relationships.get(b).battle || Relationship.War.ATTACK == bb.relationships.get(a).battle) {
			return true;
		}
		if (region != null && region.kingdom != null && region.kingdom.equals(a) && Relationship.War.NEUTRAL == aa.relationships.get(b).battle) return true;
		if (region != null && region.kingdom != null && region.kingdom.equals(b) && Relationship.War.NEUTRAL == bb.relationships.get(a).battle) return true;
		return false;
	}

	static String getStateReligion(String kingdom, World w) {
		HashMap<String, Double> weights = new HashMap<>();
		for (Region r : w.regions) {
			if (!kingdom.equals(r.kingdom)) continue;
			weights.put(r.religion, weights.getOrDefault(r.religion, 0.0) + r.population * (r.noble != null && r.noble.tags.contains("Pious") ? 3 : 1));
		}
		String max = "Company";
		double maxVal = 0;
		for (String n : weights.keySet()) {
			if (weights.get(n) > maxVal) {
				maxVal = weights.get(n);
				max = n;
			}
		}
		return max;
	}
}

