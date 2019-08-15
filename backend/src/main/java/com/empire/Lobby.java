package com.empire;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.datastore.Query;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Lobby {
	public static final String TYPE = "Lobby";

	private long gameId;
	private int numPlayers;
	private int ruleSet;
	private Schedule schedule;
	private Map<String, NationSetup> nations = new HashMap<>();
	private int minPlayers;
	private long startAt;

	private static Gson getGson() {
		return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
	}

	private static Lobby fromEntity(Entity e) {
		return getGson().fromJson(new String(((Text)e.getProperty("json")).getValue()), Lobby.class);
	}

	public static Stream<Lobby> loadAll(DatastoreService service) {
		return StreamSupport.stream(service.prepare(new Query(Lobby.TYPE)).asIterable().spliterator(), false).map(e -> fromEntity(e));
	}

	public static Lobby load(long gameId, DatastoreService service) throws EntityNotFoundException {
		return fromEntity(service.get(KeyFactory.createKey(TYPE, "_" + gameId)));
	}

	public void delete(DatastoreService service) {
		service.delete(KeyFactory.createKey(TYPE, "_" + gameId));
	}

	public boolean update(String nation, NationSetup setup) {
		return nations.putIfAbsent(nation, setup) == null;
	}

	public enum StartResult {
		WAIT,
		START,
		ABANDON
	}
	public StartResult canStart(Instant now) {
		if (nations.size() == numPlayers) return StartResult.START;
		if (now.toEpochMilli() < startAt) return StartResult.WAIT;
		if (nations.size() >= minPlayers) return StartResult.START;
		return StartResult.ABANDON;
	}

	public void save(DatastoreService service) {
		Entity lobby = new Entity(TYPE, "_" + gameId);
		lobby.setProperty("json", new Text(getGson().toJson(this)));
		service.put(lobby);
	}

	private Lobby() {} // For GSON.

	private Lobby(long gameId, int ruleSet, int numPlayers, Schedule schedule, int minPlayers, long startAt) {
		this.gameId = gameId;
		this.ruleSet = ruleSet;
		this.numPlayers = numPlayers;
		this.schedule = schedule;
		this.minPlayers = minPlayers;
		this.startAt = startAt;
	}

	public static Lobby newLobby(long gameId, int ruleSet, int numPlayers, Schedule schedule, int minPlayers, long startAt) {
		return new Lobby(gameId, ruleSet, numPlayers, schedule, minPlayers, startAt);
	}

	public long getGameId() {
		return gameId;
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

	public Schedule getSchedule() {
		return schedule;
	}

	@Override
	public String toString() {
		return getGson().toJson(this);
	}
}
