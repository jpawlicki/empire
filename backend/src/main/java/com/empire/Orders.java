package com.empire;

import java.util.Map;
import java.util.Objects;

public class Orders {
	public final long gameId;
	public final String kingdom;
	public final int turn;
	public final Map<String, String> orders;
	public final int version;

	public Orders(long gameId, String kingdom, int turn, Map<String, String> orders, int version) {
		this.gameId = gameId;
		this.kingdom = kingdom;
		this.turn = turn;
		this.orders = orders;
		this.version = version;
	}

	public Map<String, String> getOrders() {
		return orders;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Orders orders1 = (Orders) o;
		return gameId == orders1.gameId &&
				turn == orders1.turn &&
				version == orders1.version &&
				Objects.equals(kingdom, orders1.kingdom) &&
				Objects.equals(orders, orders1.orders);
	}

	@Override
	public int hashCode() {
		return Objects.hash(gameId, kingdom, turn, orders, version);
	}
}
