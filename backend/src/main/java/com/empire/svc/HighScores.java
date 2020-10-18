package com.empire.svc;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;

class HighScores {
	// By turn and kingdom, a set of all recorded scores.
	List<Map<String, Collection<Double>>> scores = new ArrayList<>();

	public static HighScores fromJson(String data) {
		return getGson().fromJson(data, HighScores.class);
	}

	public void record(int turn, String kingdom, double score) {
		while (scores.size() <= turn) scores.add(new HashMap<String, Collection<Double>>());
		Map<String, Collection<Double>> turnScores = scores.get(turn);
		if (!turnScores.containsKey(kingdom)) turnScores.put(kingdom, new ArrayList<Double>());
		turnScores.get(kingdom).add(score);
	}

	@Override
	public String toString() {
		return getGson().toJson(this);
	}

	private static Gson getGson() {
		return new GsonBuilder()
				.enableComplexMapKeySerialization()
				.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
				.create();
	}
}
