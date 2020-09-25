package com.empire;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NationSetup {
	public static enum Bonus {
		ARMIES,
		NAVIES,
		FOOD,
		GOLD;
	}

	public static class CharacterSetup {
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

	public static class Defaults {
		Map<String, NationSetup> nations;
	}

	public static Defaults loadDefaults(int ruleSetId) throws IOException {
		try (FileReader r = new FileReader("resources/rules/" + ruleSetId + "/defaultnations.json")) {
			return new GsonBuilder().create().fromJson(r, Defaults.class);
		} catch (FileNotFoundException e) {
			try (InputStreamReader r = new InputStreamReader(NationSetup.class.getResourceAsStream("/rules/" + ruleSetId + "/defaultnations.json"))) {
				return new GsonBuilder().create().fromJson(r, Defaults.class);
			}
		}
	}

	public static String TYPE = "Nation";

	public String name = "";
	public String title;
	public Set<Nation.Tag> traits;
	public Ideology religion;
	public String email = "";
	public Bonus bonus;
	public List<CharacterSetup> characters;
	public List<String> regionNames = new ArrayList<>();
	public Set<Nation.ScoreProfile> score;

	private static Gson getGson() {
		return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
	}

	public static NationSetup fromJson(String json) {
		NationSetup s = getGson().fromJson(json, NationSetup.class);
		if (s.score.contains(Nation.ScoreProfile.CULTIST)) throw new IllegalArgumentException("Cannot setup with cultist profile.");
		if (s.traits.size() != 2) throw new IllegalArgumentException("Must have two traits.");
		if (s.title.equals("")) throw new IllegalArgumentException("Must have a title.");
		if (s.traits.stream().anyMatch(e -> e == null)) throw new IllegalArgumentException("Cannot submit null traits.");
		if (s.characters.stream().anyMatch(e -> e.name.equals("") || e.skill == null || e.portrait < 0)) throw new IllegalArgumentException("Bad character.");
		return s;
	}

	public boolean hasTag(Nation.Tag tag) {
		return traits.contains(tag);
	}

	public boolean hasScoreProfile(Nation.ScoreProfile profile) {
		return score.contains(profile);
	}
}
