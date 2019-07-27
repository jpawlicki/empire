package com.empire;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.FieldNamingPolicy;

public class Nation {
	public static String TYPE = "Nation";
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
	public NationData.Tag trait1;
	public NationData.Tag trait2;
	public Ideology dominantIdeology;
	public String bonus = "";
	public String email = "";
	public String password = "";

	private static Gson getGson() {
		return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
	}

	public static Nation fromJson(String json) {
		return getGson().fromJson(json, Nation.class);
	}

	public boolean hasTag(NationData.Tag tag) {
		return trait1 == tag || trait2 == tag;
	}

	public boolean hasScoreProfile(NationData.ScoreProfile profile) {
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
