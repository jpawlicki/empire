package com.empire.svc;

import com.empire.Nation;
import com.empire.Orders;
import com.empire.World;
import com.empire.store.DatastoreClient;
import com.empire.store.GaeDatastoreClient;
import com.empire.store.MultiPutRequest;
import com.empire.util.JsonUtils;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.servlet.http.HttpServletResponse;

class EntryServletBackend {
  private static final Logger log = Logger.getLogger(EntryServletBackend.class.getName());


  private final DatastoreClient dsClient;

  public static EntryServletBackend create(){
    return new EntryServletBackend(GaeDatastoreClient.getInstance());
  }

  EntryServletBackend(DatastoreClient dsClient){
    this.dsClient = dsClient;
  }

  public Map<String, String> getOrders(Request r, HttpServletResponse resp) {
//    if (!checkPassword(r).passesRead()) return null;
    Optional<Orders> orders = dsClient.getOrders(r.getGameId(), r.getKingdom(), r.getTurn());

    if(orders.isPresent()) {
      resp.setHeader("SJS-Version", String.valueOf(orders.get().version));
      return orders.get().getOrders();
    } else {
      log.severe("Unable to get orders for gameId=" + r.getGameId() + ", kingdom=" + r.getKingdom() + ", turn=" + r.getTurn());
      return null;
    }
  }

  // TODO - should filter this data or display it.
  private String getSetup(Request r) {
    Optional<Nation> nation = dsClient.getNation(r.getGameId(), r.getKingdom());

    if(nation.isPresent()) {
      return JsonUtils.toJson(nation.get());
    } else {
      log.severe("Unable to complete setup request for gameId=" + r.getGameId() + ", kingdom=" + r.getKingdom());
      return null;
    }
  }

  private String getWorld(Request r) {
//    EntryServlet.PasswordCheck result = checkPassword(r);
//    if (!result.passesRead()) return null;

    Optional<Integer> dateOpt = dsClient.getWorldDate(r.getGameId());

    if (!dateOpt.isPresent()) {
      log.severe("Unable to retrieve date for gameId=" + r.getGameId());
      return null;
    }

    int date = r.getTurn() != 0 ? r.getTurn() : dateOpt.get();
    Optional<World> worldOpt = dsClient.getWorld(r.getGameId(), date);

    if(!worldOpt.isPresent()) {
      log.severe("Unable to retrieve world for gameId=" + r.getGameId() + ", turn=" + date);
      return null;
    }

    World w = worldOpt.get();
//    if (result == EntryServlet.PasswordCheck.PASS_PLAYER && r.getTurn() == 0) cache.recordLogin(r.getGameId(), date, w.getNation(r.getKingdom()).email);
    w.filter(r.getKingdom());

    return JsonUtils.toJson(w);
  }

  private String getAdvancePoll() {
    Optional<Set<Long>> activeGamesOpt = dsClient.getActiveGames();

    if(!activeGamesOpt.isPresent()) {
      log.severe("Poller failed, could not retrieve active games");
      return null;
    }

    for (Long gameId : activeGamesOpt.get()) {
      Optional<World> worldOpt = dsClient.getWorld(gameId, dsClient.getWorldDate(gameId).orElse(-1));

      if(!worldOpt.isPresent()) {
        log.severe("Poller failed, unable to retrieve current world for gameId=" + gameId);
        return null;
      }

      World w = worldOpt.get();

      if (w.nextTurn >= Instant.now().toEpochMilli()) continue;

      Map<String, Map<String, String>> orders = new HashMap<>();
      for (String kingdom : w.getNationNames()) {
        Optional<Orders> ordersKingdom = dsClient.getOrders(gameId, kingdom, w.date);

        if(ordersKingdom.isPresent()) {
          orders.put(kingdom, ordersKingdom.get().getOrders());
        } else {
          log.warning("Cannot find orders for gameId=" + gameId + ", kingdom=" + kingdom + ", turn=" + w.date);
          orders.put(kingdom,  null);
        }
      }

      Map<String, String> emails = w.advance(orders);

      MultiPutRequest mp = MultiPutRequest.create()
          .addWorld(gameId, w)
          .addWorldDate(gameId, w.date);

      if (w.gameover) {
        Set<Long> newActiveGames = dsClient.getActiveGames().orElse(new HashSet<>());
        newActiveGames.remove(gameId);
        mp.addActiveGames(newActiveGames);
      }

      boolean response = mp.put(dsClient);

//      if(response) {
//        emails.forEach((k, v) -> mail(k, "👑 Empire: Turn Advances", v.replace("%GAMEID%", "" + gameId)));
//      }
    }

    return "";
  }

  private String getActivity(Request r) {
//    if (checkPassword(r) != EntryServlet.PasswordCheck.PASS_GM) return null;

    Optional<Integer> dateOpt = dsClient.getWorldDate(r.getGameId());

    if (!dateOpt.isPresent()) {
      log.warning("Unable to retrieve date for gameId=" + r.getGameId());
      return null;
    }

    int date = r.getTurn() != 0 ? r.getTurn() : dateOpt.get();
    Optional<World> worldOpt = dsClient.getWorld(r.getGameId(), date);

    if(!worldOpt.isPresent()) {
      log.severe("Unable to retrieve world for gameId=" + r.getGameId() + ", turn=" + date);
      return null;
    }

    World w = worldOpt.get();

    List<String> emails = w.getNationNames().stream().map(s -> w.getNation(s).email).collect(Collectors.toList());
//    List<List<Boolean>> actives = cache.fetchLoginHistory(r.getGameId(), date, emails);
//    List<Map<String, Boolean>> result = actives.stream()
//        .map(a -> IntStream.range(0, emails.size()).boxed().collect(Collectors.toMap(emails::get, a::get)))
//        .collect(Collectors.toList());
//
//    return JsonUtils.toJson(result);
    return null;
  }
}
