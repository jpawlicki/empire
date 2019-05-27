package com.empire.store;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GaeDatastoreClient implements DatastoreClient{
    private static final String playerType = "Player";
    private static final String nationType = "Nation";
    private static final String orderType = "Order";
    private static final String worldType = "World";

    private static final String jsonProp = "json";
    private static final String jsonGzipProp = "json_gzip";
    private static final String passhashProp = "passHash";
    private static final String versionProp = "version";

    private static GaeDatastoreClient instance = null;


    private final DatastoreService service;
    private final Gson gson;

    public static GaeDatastoreClient getInstance() {
        if(instance == null) {
            instance = new GaeDatastoreClient(DatastoreServiceFactory.getDatastoreService(), createGson());
        }

        return instance;
    }

    private static Gson createGson() {
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    }

    private GaeDatastoreClient(DatastoreService service, Gson gson) {
        this.service = service;
        this.gson = gson;
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

    public Nation loadNation(String nation, long gameId) throws EntityNotFoundException {
        Entity e = service.get(KeyFactory.createKey(nationType, createNationkey(nation, gameId)));
        String jsonStr = (String) e.getProperty(jsonProp);
        return gson.fromJson(jsonStr, Nation.class);
    }

    private Entity nationToEntity(String nation, long gameId) {
        Entity e = new Entity(nationType, createNationkey(nation, gameId));
        e.setProperty(jsonProp, gson.toJson(this));
        return e;
    }

    private String createNationkey(String nation, long gameId) {
        return gameId + "_" + nation;
    }

    // Order

    public Orders loadOrders(long gameId, String kingdom, int turn) throws EntityNotFoundException {
        Entity e = service.get(KeyFactory.createKey(orderType, createOrderkey(gameId, kingdom, turn)));
        String jsonStr = (String) e.getProperty(jsonProp);
        return gson.fromJson(jsonStr, Orders.class);
    }

    private Entity orderToEntity(Orders orders) {
        Entity e = new Entity(orderType, createOrderkey(orders.gameId, orders.kingdom, orders.turn));
        e.setProperty(jsonProp, gson.toJson(orders.orders));
        e.setProperty(versionProp, orders.version);
        return e;
    }

    private String createOrderkey(long gameId, String kingdom, int turn) {
        return gameId + "_" + turn + "_" + kingdom;
    }

    // World

    private World loadWorld(long gameId, int turn) throws EntityNotFoundException {
        Entity e = service.get(KeyFactory.createKey(worldType, createWorldKey(gameId, turn)));
        String jsonStr;

        if (e.hasProperty(jsonProp)) {
            jsonStr = (String) e.getProperty(jsonProp);
        } else {
            jsonStr = Compressor.decompress(((Blob)e.getProperty(jsonGzipProp)).getBytes());

        }

        return gson.fromJson(jsonStr, World.class);
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

    public static void main(String[] args) {
        GaeDatastoreClient client = GaeDatastoreClient.getInstance();

        Player p = new Player("email@email.com", "0123456789ABCDEFGF");
        System.out.println(p);

        String s = client.gson.toJson(p);
        System.out.println(s);

        Player p2 = client.gson.fromJson(s, Player.class);
        System.out.println(p2);
    }
}
