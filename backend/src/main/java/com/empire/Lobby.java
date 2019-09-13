package com.empire;

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
	private long gameId;
	private int numPlayers;
	private int ruleSet;
	private Schedule schedule;
	private Map<String, NationSetup> nations = new HashMap<>();
	private int minPlayers;
	private long startAt;

	public static Lobby fromJson(String json) {
		return getGson().fromJson(json, Lobby.class);
	}

	public static Lobby newLobby(long gameId, int ruleSet, int numPlayers, Schedule schedule, int minPlayers, long startAt) {
		return new Lobby(gameId, ruleSet, numPlayers, schedule, minPlayers, startAt);
	}

	public boolean update(String nation, NationSetup setup) {
		// TODO: prevent one account from registering multiple nations.
		return nations.putIfAbsent(nation, setup) == null;
	}

	private static Gson getGson() {
		return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
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

	private Lobby() {} // For GSON.

	private Lobby(long gameId, int ruleSet, int numPlayers, Schedule schedule, int minPlayers, long startAt) {
		this.gameId = gameId;
		this.ruleSet = ruleSet;
		this.numPlayers = numPlayers;
		this.schedule = schedule;
		this.minPlayers = minPlayers;
		this.startAt = startAt;
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

	public long getStartAt() {
		return startAt;
	}

	@Override
	public String toString() {
		return getGson().toJson(this);
	}
}
