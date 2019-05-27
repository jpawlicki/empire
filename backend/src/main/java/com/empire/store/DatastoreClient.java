package com.empire.store;

public interface DatastoreClient {
     Player getPlayer(String email);
     Nation getNation(String nation, long gameId);
     Orders getOrders(long gameId, String kingdom, int turn);
     World getWorld(long gameId, int turn);
     LoginKey getLogin(long gameId, int date, String email);
     void putLogin(long gameId, int date, String email);
}
