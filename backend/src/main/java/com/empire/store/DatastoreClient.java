package com.empire.store;

public interface DatastoreClient {
     Player loadPlayer(String email);
     Nation loadNation(String nation, long gameId);
     Orders loadOrders(long gameId, String kingdom, int turn);
     World loadWorld(long gameId, int turn);
}
