package com.empire.svc;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import java.util.ArrayList;
import java.util.List;

public final class Player {
	public static String TYPE = "Player";

	public final String email;
	public final String passHash;
	public final String alias;
	public final String passwordOtp;
	public boolean emailConfirmed;
	public final List<Integer> activeGames = new ArrayList<>();
	public final List<Integer> oldGames = new ArrayList<>();

	public static Player loadPlayer(String email, DatastoreService service) throws EntityNotFoundException {
		Entity e = service.get(KeyFactory.createKey(TYPE, email));
		return new Player(email, (String)e.getProperty("passHash"));
	}

	public Player(String email, String passHash) {
		this.email = email;
		this.passHash = passHash;
	}

	public Player withNewPassword(String newPassHash) {
		return new Player(email, newPassHash);
	}

	public Entity toEntity() {
		Entity e = new Entity(TYPE, email);
		e.setProperty("passHash", passHash);
		return e;
	}
}
