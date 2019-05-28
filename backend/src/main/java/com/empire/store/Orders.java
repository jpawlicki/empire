package com.empire.store;

import java.util.Map;

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
}
