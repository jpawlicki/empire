package com.empire;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.FieldNamingPolicy;

public class NationSetup {
	public static String TYPE = "Nation";
	public String name = "";
	public String rulerName = "";
	public String title = "";
	public String prosperity = "";
	public String happiness = "";
	public String territory = "";
	public String glory = "";
	public String religion = "";
	public String ideology = "";
	public String security = "";
	public String riches = "";
	public String culture = "";
	public Nation.Tag trait1;
	public Nation.Tag trait2;
	public Ideology dominantIdeology;
	public String bonus = "";
	public String email = "";

	private static Gson getGson() {
		return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
	}

	public static NationSetup fromJson(String json) {
		return getGson().fromJson(json, NationSetup.class);
	}

	public boolean hasTag(Nation.Tag tag) {
		return trait1 == tag || trait2 == tag;
	}

	public boolean hasScoreProfile(Nation.ScoreProfile profile) {
		switch (profile) {
			case CULTIST: return false;
			case CULTURE: return "checked".equals(culture);
			case GLORY: return "checked".equals(glory);
			case HAPPINESS: return "checked".equals(happiness);
			case IDEOLOGY: return "checked".equals(ideology);
			case PROSPERITY: return "checked".equals(prosperity);
			case RELIGION: return "checked".equals(religion);
			case RICHES: return "checked".equals(riches);
			case SECURITY: return "checked".equals(security);
			case TERRITORY: return "checked".equals(territory);
			default: throw new IllegalArgumentException("Nation did not implement support for " + profile);
		}
	}
}
