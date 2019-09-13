package com.empire;
import com.google.gson.GsonBuilder;
import com.google.gson.FieldNamingPolicy;
import java.util.HashMap;

public final class Orders {
	public final String kingdom;
	public final int turn;
	public final int version;
	public final String json;
	public final long gameId;

	public Orders(long gameId, String kingdom, int turn, int version, String json) {
		this.gameId = gameId;
		this.kingdom = kingdom;
		this.turn = turn;
		this.version = version;
		this.json = json;
	}

	public HashMap<String, String> getOrders() {
		return
				new GsonBuilder()
						.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
						.create()
				.fromJson(json, OrderGson.class).orders;
	}

	private static class OrderGson {
		HashMap<String, String> orders = new HashMap<>();
	}
}
