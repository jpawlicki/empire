package com.empire;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import java.util.HashSet;

final class LoginCache {
	private static final LoginCache singleton = new LoginCache();
	static LoginCache getSingleton() {
		return singleton;
	}
	private static final class LoginKey {
		private final String email;
		private final long gameId;
		private final int date;

		LoginKey(String email, long gameId, int date) {
			this.email = email;
			this.gameId = gameId;
			this.date = date;
		}

		@Override
		public int hashCode() {
			return email.hashCode() ^ (int) (gameId << 16) ^ date;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof LoginKey)) return false;
			LoginKey k = (LoginKey) o;
			return
					(email == null ? k.email == null : email.equals(k.email))
					&& k.gameId == gameId
					&& k.date == date;
		}
	}

	private final HashSet<LoginKey> recordedKeys = new HashSet<>();
	private LoginCache() {}
	synchronized void recordLogin(long gameId, int date, String email, DatastoreService service) {
		LoginKey nu = new LoginKey(email, gameId, date);
		if (!recordedKeys.contains(nu)) {
			recordedKeys.add(nu);
			Entity e = new Entity("Active", nu.gameId + "_" + nu.date + "_" + nu.email);
			e.setProperty("login", true);
			service.put(e);
		}
	}
}
