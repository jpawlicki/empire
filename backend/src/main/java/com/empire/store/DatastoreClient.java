package com.empire.store;

import java.util.Set;

public interface DatastoreClient {
     Player getPlayer(String email);
     boolean putPlayer(Player player);

     Nation getNation(long gameId, String nation);
     String getNationJson(long gameId, String nation);
     boolean putNation(long gameId, String nationName, Nation nation);

     Orders getOrders(long gameId, String kingdom, int turn);
     boolean putOrders(Orders orders);

     World getWorld(long gameId, int turn);
     boolean putWorld(long gameId, World world);

     int getWorldDate(long gameId);
     boolean putWorldDate(long gameId, int date);

     LoginKey getLogin(long gameId, int date, String email);
     void putLogin(long gameId, int date, String email);

     Set<Long> getActiveGames();
     boolean putActiveGames(Set<Long> activeGames);
}
