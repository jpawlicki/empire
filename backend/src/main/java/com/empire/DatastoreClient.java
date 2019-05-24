package com.empire;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;

public class DatastoreClient {
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
}
