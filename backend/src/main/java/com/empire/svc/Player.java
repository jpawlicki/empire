package com.empire.svc;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import java.util.ArrayList;
import java.util.List;

public final class Player {
	public final String email;
	public final String passHash;
	public String alias;
	public String passwordOtp;
	public boolean emailConfirmed;
	public final List<Integer> activeGames = new ArrayList<>();
	public final List<Integer> oldGames = new ArrayList<>();

	public Player(String email, String passHash) {
		this.email = email;
		this.passHash = passHash;
	}

	public Player withNewPassword(String newPassHash) {
		return new Player(email, newPassHash);
	}
}
