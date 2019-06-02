package com.empire.store;

import com.empire.Nation;
import com.empire.Orders;
import com.empire.World;
import com.empire.svc.Player;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class MultiPutRequest {
  private final List<PutParams> puts;

  public static MultiPutRequest create() {
    return new MultiPutRequest();
  }

  private MultiPutRequest() {
    this.puts = new LinkedList<>();
  }

  public List<PutParams> getPuts() {
    return puts;
  }

  public boolean put(DatastoreClient client) {
    return client.multiPut(this);
  }

  public MultiPutRequest addPlayer(Player player) {
    PutParams k = new PutParams(PutType.PLAYER);
    k.player = player;
    this.puts.add(k);
    return this;
  }

  public MultiPutRequest addNation(long gameId, String nationName, Nation nation) {
    PutParams k = new PutParams(PutType.NATION);
    k.gameId = gameId;
    k.nationName = nationName;
    k.nation = nation;
    this.puts.add(k);
    return this;
  }

  public MultiPutRequest addOrders(Orders orders) {
    PutParams k = new PutParams(PutType.ORDERS);
    k.orders = orders;
    this.puts.add(k);
    return this;
  }

  public MultiPutRequest addWorld(long gameId, World world) {
    PutParams k = new PutParams(PutType.WORLD);
    k.gameId = gameId;
    k.world = world;
    this.puts.add(k);
    return this;
  }

  public MultiPutRequest addWorldDate(long gameId, int date) {
    PutParams k = new PutParams(PutType.WORLD_DATE);
    k.gameId = gameId;
    k.date = date;
    this.puts.add(k);
    return this;
  }

  public MultiPutRequest addLogin(String email, long gameId, int date) {
    PutParams k = new PutParams(PutType.LOGIN);
    k.email = email;
    k.gameId = gameId;
    k.date = date;
    this.puts.add(k);
    return this;
  }

  public MultiPutRequest addActiveGames(Set<Long> activeGames) {
    PutParams k = new PutParams(PutType.ACTIVE_GAMES);
    k.activeGames = activeGames;
    this.puts.add(k);
    return this;
  }

  enum PutType {
    PLAYER,
    NATION,
    ORDERS,
    WORLD,
    WORLD_DATE,
    LOGIN,
    ACTIVE_GAMES
  }

  class PutParams {
    private PutType type;
    String email;
    Long gameId;
    String nationName;
    Integer date;

    Player player;
    Nation nation;
    Orders orders;
    World world;
    Set<Long> activeGames;

    private PutParams(PutType type) {
      this.type = type;
    }

    public PutType getType() {
      return type;
    }
  }
}
