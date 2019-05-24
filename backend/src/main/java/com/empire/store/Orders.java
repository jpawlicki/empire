package com.empire.store;

import java.util.HashMap;

public class Orders {

    public final long gameId;
    public final int turn;
    public final int version;
    public final String kingdom;
    public final HashMap<String, String> orders;


    public Orders(long gameId, int turn, int version, String kingdom, HashMap<String, String> orders) {
        this.gameId = gameId;
        this.turn = turn;
        this.version = version;
        this.kingdom = kingdom;
        this.orders = orders;
    }
}
