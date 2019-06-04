package com.empire.store;

import com.empire.util.JsonUtils;
import com.empire.Nation;
import com.empire.Orders;
import com.empire.World;
import com.empire.svc.LoginKey;
import com.empire.svc.Player;
import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;


/** DatastoreClient implementation using Google App Engine's DatastoreService */
public class GaeDatastoreClient implements DatastoreClient{
    private static final Logger log = Logger.getLogger(GaeDatastoreClient.class.getName());

    private static final String playerType = "Player";
    private static final String nationType = "Nation";
    private static final String orderType = "Order";
    private static final String worldType = "World";
    private static final String activeType = "Active";
    private static final String currentDateType = "CurrentDate";
    private static final String activeGamesType = "ActiveGames";

    private static final String dateProp = "date";
    private static final String jsonProp = "json";
    private static final String jsonGzipProp = "json_gzip";
    private static final String loginProp = "login";
    private static final String activeGamesKey = "_";

    private static GaeDatastoreClient instance = null;

    private final DatastoreService service;

    public static DatastoreClient getInstance() {
        if(instance == null) {
            instance = new GaeDatastoreClient(DatastoreServiceFactory.getDatastoreService());
        }

        return instance;
    }

    private GaeDatastoreClient(DatastoreService service) {
        this.service = service;
    }

    private boolean putEntityInTransaction(Entity entity){
        Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));

        try {
            service.put(entity);
            txn.commit();
            return true;
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    // Player

    @Override
    public Optional<Player> getPlayer(String email) {
        try {
            Entity e = service.get(KeyFactory.createKey(playerType, createPlayerKey(email)));
            String jsonStr = (String) e.getProperty(jsonProp);
            return Optional.of(JsonUtils.fromJson(jsonStr, Player.class));
        } catch (EntityNotFoundException e){
            log.info("No Player entity found having key " + createPlayerKey(email));
            return Optional.empty();
        }
    }

    @Override
    public boolean putPlayer(Player player) {
        return putEntityInTransaction(playerToEntity(player));
    }

    private Entity playerToEntity(Player player) {
        Entity e = new Entity(playerType, createPlayerKey(player.getEmail()));
        e.setProperty(jsonProp, JsonUtils.toJson(player));
        return e;
    }

    private String createPlayerKey(String email){
        return email;
    }

    // Nation

    @Override
    public Optional<Nation> getNation(long gameId, String nation) {
        try {
            Entity e = service.get(KeyFactory.createKey(nationType, createNationkey(gameId, nation)));
            String jsonStr =  (String) e.getProperty(jsonProp);
            return Optional.of(JsonUtils.fromJson(jsonStr, Nation.class));
        } catch (EntityNotFoundException e) {
            log.info("No Nation entity found having key " + createNationkey(gameId, nation));
            return Optional.empty();
        }
    }

    @Override
    public boolean putNation(long gameId, String nationName, Nation nation) {
        return putEntityInTransaction(nationToEntity(gameId, nationName, nation));
    }

    private Entity nationToEntity(long gameId, String nationName, Nation nation) {
        Entity e = new Entity(nationType, createNationkey(gameId, nationName));
        e.setProperty(jsonProp, JsonUtils.toJson(nation));
        return e;
    }

    private String createNationkey(long gameId, String nation) {
        return gameId + "_" + nation;
    }

    // Order

    @Override
    public Optional<Orders> getOrders(long gameId, String kingdom, int turn) {
        try {
            Entity e = service.get(KeyFactory.createKey(orderType, createOrderkey(gameId, kingdom, turn)));
            String jsonStr = (String) e.getProperty(jsonProp);
            return Optional.of(JsonUtils.fromJson(jsonStr, Orders.class));
        } catch (EntityNotFoundException e){
            log.info("No Orders entity found having key " + createOrderkey(gameId, kingdom, turn));
            return Optional.empty();
        }
    }

    @Override
    public boolean putOrders(Orders orders) {
        return putEntityInTransaction(orderToEntity(orders));
    }

    private Entity orderToEntity(Orders orders) {
        Entity e = new Entity(orderType, createOrderkey(orders.gameId, orders.kingdom, orders.turn));
        e.setProperty(jsonProp, JsonUtils.toJson(orders));
        return e;
    }

    private String createOrderkey(long gameId, String kingdom, int turn) {
        return gameId + "_" + turn + "_" + kingdom;
    }

    // World

    @Override
    public Optional<World> getWorld(long gameId, int turn) {
        try {
            Entity e = service.get(KeyFactory.createKey(worldType, createWorldKey(gameId, turn)));
            String jsonStr = e.hasProperty(jsonProp) ? (String) e.getProperty(jsonProp) : Compressor.decompress(((Blob) e.getProperty(jsonGzipProp)).getBytes());
            return Optional.of(JsonUtils.fromJson(jsonStr, World.class));
        } catch (EntityNotFoundException e){
            log.info("No World entity having key " + createWorldKey(gameId, turn));
            return Optional.empty();
        }
    }

    @Override
    public boolean putWorld(long gameId, World world) {
        return putEntityInTransaction(worldToEntity(gameId, world));
    }

    private Entity worldToEntity(long gameId, World world) {
		Entity e = new Entity(worldType, createWorldKey(gameId, world.date));
		String jsonStr = JsonUtils.toJson(world);
		Blob jsonBlob = new Blob(Compressor.compress(jsonStr));
		e.setProperty(jsonGzipProp, jsonBlob);
		return e;
	}

	  private String createWorldKey(long gameId, int turn){
        return gameId + "_" + turn;
    }

    // World Date

    @Override
    public Optional<Integer> getWorldDate(long gameId) {
        try {
            Entity e = service.get(KeyFactory.createKey(currentDateType, createCurrentDateKey(gameId)));
            Long date = (Long) e.getProperty(dateProp);
            return Optional.of((int) date.longValue());

        } catch (EntityNotFoundException e){
            log.info("CurrentDate entity having key " + createCurrentDateKey(gameId));
            return Optional.empty();
        }
    }

    @Override
    public boolean putWorldDate(long gameId, int date) {
        return putEntityInTransaction(worldDateToEntity(gameId, date));
    }

    private Entity worldDateToEntity(long gameId, int date) {
        Entity entity = new Entity(currentDateType, createCurrentDateKey(gameId));
        entity.setProperty(dateProp, (long) date);
        return entity;
    }

    private String createCurrentDateKey(long gameId){
        return "game_" + gameId;
    }

    // Login

    @Override
    public Optional<LoginKey> getLogin(String email, long gameId, int date) {
        try {
            Entity entity = service.get(KeyFactory.createKey(activeType, createLoginKey(gameId, date, email)));
            String jsonStr = (String) entity.getProperty(jsonProp);
            return Optional.of(JsonUtils.fromJson(jsonStr, LoginKey.class));
        } catch (EntityNotFoundException e) {
            log.info("No LoginKey entity having key " + createLoginKey(gameId, date, email));
            return Optional.empty();
        }
    }

    @Override
    public boolean putLogin(String email, long gameId, int date) {
        return putEntityInTransaction(loginToEntity(email, gameId, date));
    }

    private Entity loginToEntity(String email, long gameId, int date){
        Entity entity = new Entity(activeType, createLoginKey(gameId, date, email));
        entity.setProperty(jsonProp, JsonUtils.toJson(new LoginKey(email, gameId, date)));
        entity.setProperty(loginProp, true);
        return entity;
    }

    private String createLoginKey(long gameId, int date, String email){
        return gameId + "_" + date + "_" + email;
    }

    // Active games

    @Override
    public Optional<Set<Long>> getActiveGames() {
        try {
            Entity e = service.get(KeyFactory.createKey(activeGamesType, createActiveGamesKey()));
            String jsonStr = (String) e.getProperty(jsonProp);
            Type listType = new TypeToken<Set<Long>>(){}.getType();
            return Optional.of(JsonUtils.fromJson(jsonStr, listType));
        } catch (EntityNotFoundException e) {
            log.info("Unable to retrieve active games with key " + createActiveGamesKey());
            return Optional.empty();
        }
    }

    @Override
    public boolean putActiveGames(Set<Long> activeGames) {
        return putEntityInTransaction(activeGamesToEntity(activeGames));
    }

    private Entity activeGamesToEntity(Set<Long> activeGames){
        Entity entity = new Entity(activeGamesType, createActiveGamesKey());
        entity.setProperty(jsonProp, JsonUtils.toJson(activeGames));
        return entity;
    }

    private String createActiveGamesKey(){
        return activeGamesKey;
    }

    @Override
    public boolean multiPut(MultiPutRequest m) {
        Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
        boolean putSuccessful;

        try {
            for(MultiPutRequest.PutParams k : m.getPuts()) {
                Entity entity;

                switch(k.getType()) {
                    case PLAYER:
                        entity = playerToEntity(k.player);
                        break;
                    case NATION:
                        entity = nationToEntity(k.gameId, k.nationName, k.nation);
                        break;
                    case ORDERS:
                        entity = orderToEntity(k.orders);
                        break;
                    case WORLD:
                        entity = worldToEntity(k.gameId, k.world);
                        break;
                    case WORLD_DATE:
                        entity = worldDateToEntity(k.gameId, k.date);
                        break;
                    case LOGIN:
                        entity = loginToEntity(k.email, k.gameId, k.date);
                        break;
                    case ACTIVE_GAMES:
                        entity = activeGamesToEntity(k.activeGames);
                        break;
                    default:
                        log.info("Malformed request, unknown type " + k.getType() + " in multiput");
                        continue;
                }

                service.put(entity);
            }

            txn.commit();
            putSuccessful = true;
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                putSuccessful = false;
            }
        }

        return putSuccessful;
    }
}
