package com.empire.svc;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;

class ActiveGames {
	ArrayList<Long> activeGameIds = new ArrayList<>();

	static ActiveGames fromGson(String s) {
		return getGson().fromJson(s, ActiveGames.class);
	}

	private static Gson getGson() {
		return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
	}

	String toJson() {
		return getGson().toJson(this);
	}
}
