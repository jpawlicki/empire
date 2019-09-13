package com.empire.svc;

import com.empire.Lobby;
import com.empire.Orders;
import com.empire.World;
import com.empire.util.Compressor;
import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.BaseEncoding;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class DataSource implements AutoCloseable {
	static final Cache<String, World> worldCache = CacheBuilder.newBuilder().maximumSize(20).build();
	static final Cache<String, Orders> ordersCache = CacheBuilder.newBuilder().maximumSize(20).build();
	static final Cache<String, Lobby> lobbyCache = CacheBuilder.newBuilder().maximumSize(20).build();

	private static final String TYPE_LOBBY = "Lobby";
	private static final String TYPE_ORDERS = "Orders";
	private static final String TYPE_PLAYER = "Player";
	private static final String TYPE_WORLD = "World";
	static private interface CheckedFunction<S, T, E extends Throwable> {
		T apply(S s) throws E;
	}

	DatastoreService service = DatastoreServiceFactory.getDatastoreService();
	Transaction txn = null;


	static DataSource transactional() {
		return new DataSource(true);
	}

	static DataSource nontransactional() {
		return new DataSource(false);
	}

	void commit() {
		if (txn != null) txn.commit();
	}

	public void close() {
		if (txn != null && txn.isActive()) txn.rollback();
	}

	World loadWorld(long gameId, int turn) throws EntityNotFoundException, IOException {
		return
				loadEntity(
						KeyFactory.createKey(TYPE_WORLD, gameId + "_" + turn),
						World::fromJson,
						null /* worldCache */); // worldCache is buggy because getWorld() mutates the returned object to filter it to a particular player's view.
	}

	Orders loadOrder(long gameId, String kingdom, int turn) throws EntityNotFoundException, IOException {
		return
				loadEntity(
						KeyFactory.createKey(KeyFactory.createKey(TYPE_WORLD, gameId + "_" + turn), TYPE_ORDERS, gameId + "_" + turn + "_" + kingdom),
						body -> new Orders(gameId, kingdom, turn, 0, body),
						ordersCache);
	}

	int loadCurrentDate(long gameId) throws EntityNotFoundException {
		return ((Long)service.get(KeyFactory.createKey("CURRENTDATE", "game_" + gameId)).getProperty("date")).intValue();
	}

	ActiveGames loadActiveGames() throws EntityNotFoundException {
		return ActiveGames.fromGson((String)service.get(KeyFactory.createKey("ACTIVEGAMES", "_")).getProperty("active_games"));
	}

	Stream<Lobby> loadAllLobbies() {
		return StreamSupport.stream(service.prepare(new Query(TYPE_LOBBY)).asIterable().spliterator(), false).map(e -> Lobby.fromJson(getJson(e)));
	}

	Lobby loadLobby(long gameId) throws EntityNotFoundException, IOException {
		return
				loadEntity(
						KeyFactory.createKey(TYPE_LOBBY, "_" + gameId),
						Lobby::fromJson,
						lobbyCache);
	}

	Player loadPlayer(String email) throws EntityNotFoundException, IOException {
		Entity e = service.get(KeyFactory.createKey(TYPE_PLAYER, email));
		return new Player(email, (String)e.getProperty("passHash"));
	}

	void save(World world, long gameId) {
		Entity e = new Entity(TYPE_WORLD, gameId + "_" + world.getDate());
		setProperties(e, world.toString());
		service.put(e);
	}

	void save(Lobby lobby) {
		Entity e = new Entity(TYPE_LOBBY, "_" + lobby.getGameId());
		setProperties(e, lobby.toString());
		service.put(e);
	}

	void save(Orders orders) {
		Entity e = new Entity(TYPE_ORDERS, orders.gameId + "_" + orders.turn + "_" + orders.kingdom, KeyFactory.createKey(TYPE_WORLD, orders.gameId + "_" + orders.turn));
		setProperties(e, orders.json);
		service.put(e);
	}

	void save(ActiveGames games) {
		Entity activeGames = new Entity("ACTIVEGAMES", "_");
		activeGames.setProperty("active_games", games.toJson());
		service.put(activeGames);
	}

	void save(Player player) {
		Entity e = new Entity(TYPE_PLAYER, player.email);
		e.setProperty("passHash", player.passHash);
		service.put(e);
	}

	void saveCurrentDate(int date, long gameId) {
		Entity nudate = new Entity("CURRENTDATE", "game_" + gameId);
		nudate.setProperty("date", (long) date);
		service.put(nudate);
	}

	void delete(Lobby lobby) {
		service.delete(KeyFactory.createKey(TYPE_LOBBY, "_" + lobby.getGameId()));
	}

	private void setProperties(Entity e, String body) {
		e.setProperty("json_gzip", new Blob(Compressor.compress(body)));
		e.setProperty("eTag", BaseEncoding.base64Url().encode(Hasher.hashEtag(body)));
	}

	private String getJson(Entity e) {
		if (e.hasProperty("json")) {
			return new String(((Text)e.getProperty("json")).getValue());
		} else {
			return Compressor.decompress(((Blob)e.getProperty("json_gzip")).getBytes());
		}
	}

	private <T, E extends Exception> T loadEntity(Key key, CheckedFunction<String, T, E> factory, Cache<String, T> cache) throws E, IOException, EntityNotFoundException {
		Entity e = service.get(key);
		String eTag = null;
		if (e.hasProperty("eTag")) eTag = (String)e.getProperty("eTag");
		if (eTag == null || cache == null) return factory.apply(getJson(e));
		try {
			return cache.get(eTag, () -> factory.apply(getJson(e)));
		} catch (ExecutionException ex) {
			throw new IOException(ex.getCause());
		}
	}

	private DataSource(boolean transactional) {
		if (transactional) txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
	}
}
