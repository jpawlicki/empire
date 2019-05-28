package com.empire.store;

import java.util.List;

public interface DatastoreClient {
     Player getPlayer(String email);
     boolean putPlayer(Player player);
     Nation getNation(String nation, long gameId);
     String getNationJson(String nation, long gameId);
     boolean putNation(long gameId, String nation);
     Orders getOrders(long gameId, String kingdom, int turn);
     boolean putOrders(Orders orders);
     World getWorld(long gameId, int turn);
     boolean putWorld(long gameId, World world);
     int getWorldDate(long gameId);
     boolean putWorldDate(long gameId, int date);
     LoginKey getLogin(long gameId, int date, String email);
     void putLogin(long gameId, int date, String email);
     List<Long> getActiveGames();
     boolean putActiveGames(List<Long> activeGames);
}
