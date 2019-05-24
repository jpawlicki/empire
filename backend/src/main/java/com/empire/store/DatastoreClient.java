package com.empire.store;

import com.empire.Nation;
import com.empire.Order;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DatastoreClient {
    private static final String playerType = "Player";
    private static final String nationType = "Nation";
    private static final String orderType = "Order";

    private static final String jsonProp = "json";
    private static final String passhashProp = "passHash";
    private static final String versionProp = "version";

    private static DatastoreClient instance = null;

    private final DatastoreService service;

    public static DatastoreClient getInstance() {
        if(instance == null) {
            instance = new DatastoreClient(DatastoreServiceFactory.getDatastoreService());
        }

        return instance;
    }

    private DatastoreClient(DatastoreService service) {
        this.service = service;
    }

    // Player

    public Player loadPlayer(String email) throws EntityNotFoundException {
        Entity e = service.get(KeyFactory.createKey(playerType, email));
        return new Player(email, (String)e.getProperty(passhashProp));
    }

    private Entity playerToEntity(Player player) {
        Entity e = new Entity(playerType, player.email);
        e.setProperty(passhashProp, player.passHash);
        return e;
    }

    // Nation

    public Nation.NationGson loadNation(String nation, long gameId) throws EntityNotFoundException {
        Entity e = service.get(KeyFactory.createKey(nationType, createNationkey(nation, gameId)));
        String jsonStr = (String) e.getProperty(jsonProp);
        return getGson().fromJson(jsonStr, Nation.NationGson.class);
    }

    private Entity nationToEntity(String nation, long gameId) {
        Entity e = new Entity(nationType, createNationkey(nation, gameId));
        e.setProperty(jsonProp, getGson().toJson(this));
        return e;
    }

    private String createNationkey(String nation, long gameId) {
        return gameId + "_" + nation;
    }

    private Gson getGson() {
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    }

    // Order

    public Order loadOrder(long gameId, String kingdom, int turn) throws EntityNotFoundException {
        Entity e = service.get(KeyFactory.createKey(orderType, createOrderkey(gameId, kingdom, turn)));
        return new Order(gameId, kingdom, turn, (int)((Long)e.getProperty("version")).longValue(), ((Text)e.getProperty("json")).getValue());
    }

//    public HashMap<String, String> getOrders() {
//        return
//                new GsonBuilder()
//                        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
//                        .create()
//                        .fromJson(json, Order.OrderGson.class).orders;
//    }

    private Entity orderToEntity(Order order) {
        Entity e = new Entity(orderType, createOrderkey(order.gameId, order.kingdom, order.turn));
        e.setProperty(jsonProp, new Text(order.json));
        e.setProperty(versionProp, order.version);
        return e;
    }

    private String createOrderkey(long gameId, String kingdom, int turn) {
        return gameId + "_" + turn + "_" + kingdom;
    }
}
