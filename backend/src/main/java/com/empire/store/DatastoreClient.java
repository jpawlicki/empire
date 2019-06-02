package com.empire.store;

import com.empire.Nation;
import com.empire.Orders;
import com.empire.World;
import com.empire.svc.LoginKey;
import com.empire.svc.Player;
import java.util.Optional;
import java.util.Set;

// TODO: consider changing active games to get, add(long), delete(long)

public interface DatastoreClient {
     Optional<Player> getPlayer(String email);
     boolean putPlayer(Player player);

     Optional<Nation> getNation(long gameId, String nation);
     boolean putNation(long gameId, String nationName, Nation nation);

     Optional<Orders> getOrders(long gameId, String kingdom, int turn);
     boolean putOrders(Orders orders);

     World getWorld(long gameId, int turn);
     boolean putWorld(long gameId, World world);

     int getWorldDate(long gameId);
     boolean putWorldDate(long gameId, int date);

     LoginKey getLogin(String email, long gameId, int date);
     boolean putLogin(String email, long gameId, int date);

     Set<Long> getActiveGames();
     boolean putActiveGames(Set<Long> activeGames);
}
