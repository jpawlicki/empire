package com.empire;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DatastoreClient {
    private static final String playerType = "Player";
    private static final String nationType = "Nation";

    private static final String jsonProp = "json";
    private static final String passhashProp = "passHash";

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

    public Entity playerToEntity(Player player) {
        Entity e = new Entity(playerType, player.email);
        e.setProperty(passhashProp, player.passHash);
        return e;
    }

    // Nation

    public Nation.NationGson loadNation(String nation, long gameId) throws EntityNotFoundException {
        Entity e = service.get(KeyFactory.createKey(nationType, gameId + "_" + nation));
        String jsonStr = (String) e.getProperty(jsonProp);
        return getGson().fromJson(jsonStr, Nation.NationGson.class);
    }

    public Entity nationToEntity(String nation, long gameId) {
        Entity e = new Entity(nationType, gameId + "_" + nation);
        e.setProperty(jsonProp, getGson().toJson(this));
        return e;
    }

    private Gson getGson() {
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    }
}
