package com.empire.store;

import com.empire.Compressor;
import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

public class GaeDatastoreClient implements DatastoreClient{
    private static final Logger log = Logger.getLogger(GaeDatastoreClient.class.getName());

    private static final String playerType = "Player";
    private static final String nationType = "Nation";
    private static final String orderType = "Order";
    private static final String worldType = "World";
    private static final String activeType = "Active";
    private static final String currentDateType = "CURRENTDATE";
    private static final String activeGamesType = "ACTIVEGAMES";

    private static final String dateProp = "date";
    private static final String jsonProp = "json";
    private static final String jsonGzipProp = "json_gzip";
//    private static final String passhashProp = "passHash";
//    private static final String versionProp = "version";
    private static final String loginProp = "login";
    private static final String activeGamesProp = "active_games";

    private static GaeDatastoreClient instance = null;
    public static Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    private final DatastoreService service;

    public static GaeDatastoreClient getInstance() {
        if(instance == null) {
            instance = new GaeDatastoreClient(DatastoreServiceFactory.getDatastoreService());
        }

        return instance;
    }

    private GaeDatastoreClient(DatastoreService service) {
        this.service = service;
    }

    private boolean putEntitiesInTransaction(Iterable<Entity> entities){
        Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));

        try {
            service.put(entities);
            txn.commit();
            return true;
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    private boolean putEntityInTransaction(Entity entity){
        return putEntitiesInTransaction(Collections.singletonList(entity));
    }

    // Player

    @Override
    public Player getPlayer(String email) {
        try {
            Entity e = service.get(KeyFactory.createKey(playerType, createPlayerKey(email)));
            String jsonStr = (String) e.getProperty(jsonProp);
            return gson.fromJson(jsonStr, Player.class);
        } catch (EntityNotFoundException e){
            log.severe("Enable to retrieve Player with key " + email);
            return null;
        }
    }

    @Override
    public boolean putPlayer(Player player) {
        return putEntityInTransaction(playerToEntity(player));
    }

    private Entity playerToEntity(Player player) {
        Entity e = new Entity(playerType, createPlayerKey(player.email));
        e.setProperty(jsonProp, gson.toJson(player));
        return e;
    }

    private String createPlayerKey(String email){
        return email;
    }

    // Nation

    @Override
    public Nation getNation(long gameId, String nation) {
        return gson.fromJson(getNationJson(gameId, nation), Nation.class);
    }

    @Override
    public String getNationJson(long gameId, String nation) {
        try {
            Entity e = service.get(KeyFactory.createKey(nationType, createNationkey(gameId, nation)));
            return (String) e.getProperty(jsonProp);
        } catch (EntityNotFoundException e) {
            log.severe("Enable to retrieve Nation with key " + createNationkey(gameId, nation));
            return null;
        }
    }

    @Override
    public boolean putNation(long gameId, String nationName, Nation nation) {
        return putEntityInTransaction(nationToEntity(gameId, nationName, nation));
    }

    private Entity nationToEntity(long gameId, String nationName, Nation nation) {
        Entity e = new Entity(nationType, createNationkey(gameId, nationName));
        e.setProperty(jsonProp, gson.toJson(nation));
        return e;
    }

    private String createNationkey(long gameId, String nation) {
        return gameId + "_" + nation;
    }

    // Order

    @Override
    public Orders getOrders(long gameId, String kingdom, int turn) {
        try {
            Entity e = service.get(KeyFactory.createKey(orderType, createOrderkey(gameId, kingdom, turn)));
            String jsonStr = (String) e.getProperty(jsonProp);
            return gson.fromJson(jsonStr, Orders.class);
        } catch (EntityNotFoundException e){
            log.severe("Enable to retrieve Orders with key " + createOrderkey(gameId, kingdom, turn));
            return null;
        }
    }

    @Override
    public boolean putOrders(Orders orders) {
        return putEntityInTransaction(orderToEntity(orders));
    }

    private Entity orderToEntity(Orders orders) {
        Entity e = new Entity(orderType, createOrderkey(orders.gameId, orders.kingdom, orders.turn));
        e.setProperty(jsonProp, gson.toJson(orders));
        return e;
    }

    private String createOrderkey(long gameId, String kingdom, int turn) {
        return gameId + "_" + turn + "_" + kingdom;
    }

    // World

    @Override
    public World getWorld(long gameId, int turn) {
        try {
            Entity e = service.get(KeyFactory.createKey(worldType, createWorldKey(gameId, turn)));
            String jsonStr;

            if (e.hasProperty(jsonProp)) {
                jsonStr = (String) e.getProperty(jsonProp);
            } else {
                jsonStr = Compressor.decompress(((Blob) e.getProperty(jsonGzipProp)).getBytes());

            }

            return gson.fromJson(jsonStr, World.class);
        } catch (EntityNotFoundException e){
            log.severe("Enable to retrieve World with key " + createWorldKey(gameId, turn));
            return null;
        }
    }

    @Override
    public boolean putWorld(long gameId, World world) {
        return putEntityInTransaction(worldToEntity(gameId, world));
    }

    private Entity worldToEntity(long gameId, World world) {
		Entity e = new Entity(worldType, createWorldKey(gameId, world.date));
		String jsonStr = gson.toJson(world);
		Blob jsonBlob = new Blob(Compressor.compress(jsonStr));
		e.setProperty(jsonGzipProp, jsonBlob);
		return e;
	}

	private String createWorldKey(long gameId, int turn){
        return gameId + "_" + turn;
    }

    // World Date

    @Override
    public int getWorldDate(long gameId) {
        try {
            Entity e = service.get(KeyFactory.createKey(currentDateType, createCurrentDateKey(gameId)));
            Long date = (Long) e.getProperty(dateProp);
            return (int) date.longValue();

        } catch (EntityNotFoundException e){
            log.severe("Enable to retrieve Current Date with key " + createCurrentDateKey(gameId));
            return -1;
        }
    }

    @Override
    public boolean putWorldDate(long gameId, int date) {
        return putEntityInTransaction(dateToEntity(gameId, date));
    }

    private Entity dateToEntity(long gameId, int date) {
        Entity entity = new Entity(currentDateType, createCurrentDateKey(gameId));
        entity.setProperty(dateProp, (long) date);
        return entity;
    }

    private String createCurrentDateKey(long gameId){
        return "game_" + gameId;
    }

    // Login

    @Override
    public LoginKey getLogin(String email, long gameId, int date) {
        try {
            Entity entity = service.get(KeyFactory.createKey(activeType, createLoginKey(gameId, date, email)));
            String jsonStr = (String) entity.getProperty(jsonProp);
            return gson.fromJson(jsonStr, LoginKey.class);
        } catch (EntityNotFoundException e) {
            log.severe("Enable to retrieve Login with key " + createLoginKey(gameId, date, email));
            return null;
        }
    }

    @Override
    public boolean putLogin(String email, long gameId, int date) {
        return putEntityInTransaction(loginToEntity(email, gameId, date));
    }

    private Entity loginToEntity(String email, long gameId, int date){
        Entity entity = new Entity(activeType, createLoginKey(gameId, date, email));
        entity.setProperty(jsonProp, gson.toJson(new LoginKey(email, gameId, date)));
        entity.setProperty(loginProp, true);
        return entity;
    }

    private String createLoginKey(long gameId, int date, String email){
        return gameId + "_" + date + "_" + email;
    }

    // Active games

    @Override
    public Set<Long> getActiveGames() {
        try {
            Entity e = service.get(KeyFactory.createKey(activeGamesType, createActiveGamesKey()));
            String jsonStr = (String) e.getProperty(activeGamesProp);
            Type listType = new TypeToken<Set<Long>>(){}.getType();
            return gson.fromJson(jsonStr, listType);
        } catch (EntityNotFoundException e) {
            log.severe("Enable to retrieve active games with key " + createActiveGamesKey());
            return null;
        }
    }

    @Override
    public boolean putActiveGames(Set<Long> activeGames) {
        return putEntityInTransaction(activeGamesToEntity(activeGames));
    }

    private Entity activeGamesToEntity(Set<Long> activeGames){
        Entity entity = new Entity(activeGamesType, createActiveGamesKey());
        entity.setProperty(activeGamesProp, gson.toJson(activeGames));
        return entity;
    }

    private String createActiveGamesKey(){
        return "_";
    }

    public static void main(String[] args) {
        GaeDatastoreClient client = GaeDatastoreClient.getInstance();

        Player p = new Player("email@email.com", "0123456789ABCDEFGF");
        System.out.println(p);

        String s = GaeDatastoreClient.gson.toJson(p);
        System.out.println(s);

        Player p2 = GaeDatastoreClient.gson.fromJson(s, Player.class);
        System.out.println(p2);
    }
}
