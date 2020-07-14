package com.empire;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.FieldNamingPolicy;

public class NationSetup {
	public static enum Bonus {
		ARMIES,
		NAVIES,
		FOOD,
		GOLD;
	}

	public static CharacterSetup {
		public static enum Skill {
			GOVERNOR,
			ADMIRAL,
			GENERAL,
			SPY;
		}

		public String name;
		public Skill skill;
		public int portrait;
	}

	public static String TYPE = "Nation";

	public String name = "";
	public String title;
	public Set<Nation.Tag> traits;
	public Ideology dominantIdeology;
	public String email = "";
	public Bonus bonus;
	public List<CharacterSetup> characters;
	public List<String> regionNames;
	public Set<Nation.ScoreProfile> profiles;

	private static Gson getGson() {
		return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
	}

	public static NationSetup fromJson(String json) {
		NationSetup s = getGson().fromJson(json, NationSetup.class);
		if (s.profiles.contains(Nation.ScoreProfile.CULTIST)) throw IllegalArgumentException("Cannot setup with cultist profile.");
		if (traits.size() != 2) throw IllegalArgumentException("Must have two traits.");
		if (title.equals("")) throw IllegalArgumentException("Must have a title.");
		if (regionNames.stream().anyMatch(s -> s.equals(""))) throw IllegalArgumentException("Cannot submit empty region name.");
		if (characters.stream().anyMatch(s -> s.name.equals("") || s.skill == null || s.portrait < 0)) throw IllegalArgumentException("Bad character.");
		return s;
	}

	public boolean hasTag(Nation.Tag tag) {
		return trait1 == tag || trait2 == tag;
	}

	public boolean hasScoreProfile(Nation.ScoreProfile profile) {
		return profiles.contains(profile);
	}
}
