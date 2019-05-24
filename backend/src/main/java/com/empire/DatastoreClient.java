package com.empire;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;

public class DatastoreClient {
    private static final String playerType = "Player";
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

    public Player loadPlayer(String email) throws EntityNotFoundException {
        Entity e = service.get(KeyFactory.createKey(playerType, email));
        return new Player(email, (String)e.getProperty(passhashProp));
    }

    public Entity playerToEntity(Player player) {
        Entity e = new Entity(playerType, player.email);
        e.setProperty(passhashProp, player.passHash);
        return e;
    }
}
