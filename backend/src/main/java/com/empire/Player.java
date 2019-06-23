package com.empire;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public final class Player {
	public static String TYPE = "Player";

	public final String email;
	public final String passHash;

	public static Player loadPlayer(String email, DatastoreService service) throws EntityNotFoundException {
		Entity e = service.get(KeyFactory.createKey(TYPE, email));
		return new Player(email, (String)e.getProperty("passHash"));
	}

	public Player(String email, String passHash) {
		this.email = email;
		this.passHash = passHash;
	}

	public Entity toEntity() {
		Entity e = new Entity(TYPE, email);
		e.setProperty("passHash", passHash);
		return e;
	}
}
