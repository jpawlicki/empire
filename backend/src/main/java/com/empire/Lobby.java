package com.empire;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class Lobby {
	public static final String TYPE = "Lobby";

	int numPlayers;
	int ruleSet;
	Schedule schedule;
	Map<String, NationSetup> nations = new HashMap<>();
	int minPlayers;
	long startAt;

	private static Gson getGson() {
		return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
	}

	private static String loadJson(long gameId, DatastoreService service) throws EntityNotFoundException {
		Entity e = service.get(KeyFactory.createKey(TYPE, "_" + gameId));
		return new String(((Text)e.getProperty("json")).getValue());
	}

	private static Lobby fromJson(String json) {
		return getGson().fromJson(json, Lobby.class);
	}

	public static Lobby load(long gameId, DatastoreService service) throws EntityNotFoundException {
		return fromJson(loadJson(gameId, service));
	}

	public boolean update(String nation, NationSetup setup) {
		return nations.putIfAbsent(nation, setup) == null;
	}

	public boolean canStart(Instant now) {
		return nations.size() >= minPlayers && (nations.size() == numPlayers || now.toEpochMilli() >= startAt);
	}

	public void save(long gameId, DatastoreService service) {
		Entity lobby = new Entity(TYPE, "_" + gameId);
		lobby.setProperty("json", new Text(getGson().toJson(this)));
		service.put(lobby);
	}

	private Lobby() {} // For GSON.

	private Lobby(int ruleSet, int numPlayers, Schedule schedule, int minPlayers, long startAt) {
		this.ruleSet = ruleSet;
		this.numPlayers = numPlayers;
		this.schedule = schedule;
		this.minPlayers = minPlayers;
		this.startAt = startAt;
	}

	public static Lobby newLobby(int ruleSet, int numPlayers, Schedule schedule, int minPlayers, long startAt) {
		return new Lobby(ruleSet, numPlayers, schedule, minPlayers, startAt);
	}

	public int getNumPlayers() {
		return numPlayers;
	}

	public int getRuleSet() {
		return ruleSet;
	}

	public Map<String, NationSetup> getNations() {
		return nations;
	}

	@Override
	public String toString() {
		return getGson().toJson(this);
	}
}
