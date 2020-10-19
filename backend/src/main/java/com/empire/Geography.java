package com.empire;

import com.google.gson.GsonBuilder;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Geography {
	private static final transient Map<Integer, Map<Integer, Geography>> cache = new ConcurrentHashMap<>();

	List<Region> regions;
	List<Border> borders;
	List<Kingdom> kingdoms;
	int players;
	int holycity;

	static class Region {
		int id;
		com.empire.Region.Type type;
		String name;
		int core;
	}

	static class Border {
		int a;
		Integer b; // b can be omitted for map boundary borders.
		int w;
		List<Point> path;
	}

	static class Point {
		double x;
		double y;
	}

	public static class Kingdom {
		public String name;
		Culture culture;
	}

	public static Geography loadGeography(int ruleSetId, int numPlayers) throws IOException {
		Map<Integer, Geography> ruleGeography = cache.computeIfAbsent(ruleSetId, unused -> new ConcurrentHashMap<>());
		if (!ruleGeography.containsKey(numPlayers)) {
			ruleGeography.put(numPlayers, readGeography(ruleSetId, numPlayers));
		}
		return ruleGeography.get(numPlayers);
	}

	private static Geography readGeography(int ruleSetId, int numPlayers) throws IOException {
		try (FileReader r = new FileReader("resources/rules/" + ruleSetId + "/maps/" + numPlayers + ".json")) {
			return new GsonBuilder().create().fromJson(r, Geography.class);
		} catch (FileNotFoundException e) {
			try (InputStreamReader r = new InputStreamReader(Geography.class.getResourceAsStream("/rules/" + ruleSetId + "/maps/" + numPlayers + ".json"), StandardCharsets.UTF_8)) {
				return new GsonBuilder().create().fromJson(r, Geography.class);
			}
		}
	}

	public List<Kingdom> getKingdoms() {
		return kingdoms;
	}

	Kingdom getKingdom(String name) {
		for (Kingdom k : kingdoms) if (k.name.equals(name)) return k;
		throw new IllegalArgumentException("No such kingdom \"" + name + "\" on this map.");
	}

	int getKingdomId(String name) {
		for (int i = 0; i < kingdoms.size(); i++) if (kingdoms.get(i).name.equals(name)) return i;
		throw new IllegalArgumentException("No such kingdom \"" + name + "\" on this map.");
	}

	@Override
	public String toString() {
		return new GsonBuilder().create().toJson(this);
	}

	private Geography() {}
}
