package com.empire.store;

import java.util.Map;

public class Orders {
    public final long gameId;
    public final int turn;
    public final int version;
    public final String kingdom;
    public final Map<String, String> orders;

    public Orders(long gameId, int turn, int version, String kingdom, Map<String, String> orders) {
        this.gameId = gameId;
        this.turn = turn;
        this.version = version;
        this.kingdom = kingdom;
        this.orders = orders;
    }
}
