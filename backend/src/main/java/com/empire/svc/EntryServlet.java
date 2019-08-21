package com.empire.svc;

import com.empire.Geography;
import com.empire.Lobby;
import com.empire.NationSetup;
import com.empire.Orders;
import com.empire.Rules;
import com.empire.Schedule;
import com.empire.World;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.apphosting.api.ApiProxy.OverQuotaException;
import com.google.common.io.BaseEncoding;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
Basic design:
GET /entry/orders?gid=1234&k=Aefoss&password=foobar&t=22
	Check password and return the given orders entry JSON.
GET /entry/setup?gid=1234
	Return lobby information for a game. If none, empty string.
GET /entry/world?gid=1234&k=Aefoss&password=foobar&t=22
	Check password and return the visible world view JSON.
GET /entry/advanceworldpoll
	Advance all games one turn, if they need it.
GET /entry/lobbypoll
  Checks all lobbies for whether they can be launched.
GET /entry/index?k=example@example.com&password=foobar

POST /entry/orders?gid=1234&k=Aefoss&password=foobar&t=22
	Check password and post the given order data.
POST /entry/setup?gid=1234&k=Aefoss&t=0&password=foobar
	Set customization info for the kingdom, if unset.
POST /entry/advanceworld?gid=1234
	Check cadence and possibly advance world to next step, mail players.
POST /entry/startlobby?gid=1234
	Start a new lobby.

TODO: Will eventually need a changePassword/change-email.
*/

@WebServlet(name = "EntryServlet", value = "/entry/*")
public class EntryServlet extends HttpServlet {
	private static final Logger log = Logger.getLogger(EntryServlet.class.getName());
	private static final String PASSWORD_SALT = "~ Empire_Password Salt ~123`";
	private static byte[] GM_PASSWORD_HASH = BaseEncoding.base16().decode("DFEC33349F0EE2E0BC2085D761553BDDF1753698DDAD94491664F14EA58EA072");

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		Request r = Request.from(req);
		resp.setHeader("Access-Control-Allow-Origin", "*");
		String json = "";
		try {
			if (req.getRequestURI().equals("/entry/ping")) {
				json = "{\"status\": \"success\"}";
			} else if (req.getRequestURI().equals("/entry/orders")) {
				json = getOrders(r, resp);
			} else if (req.getRequestURI().equals("/entry/setup")) {
				json = getSetup(r);
			} else if (req.getRequestURI().equals("/entry/world")) {
				json = getWorld(r);
			} else if (req.getRequestURI().equals("/entry/geography")) {
				json = getGeography(r);
			} else if (req.getRequestURI().equals("/entry/advanceworldpoll")) {
				json = getAdvancePoll(r.skipMail);
			} else if (req.getRequestURI().equals("/entry/lobbypoll")) {
				json = getLobbyPoll();
			} else if (req.getRequestURI().equals("/entry/activity")) {
				json = getActivity(r);
			} else if (req.getRequestURI().equals("/entry/index")) {
				json = getIndex(r);
			} else {
				resp.sendError(404, "No such path.");
				return;
			}
		} catch (PasswordException e) {
			resp.setHeader("X-Empire-Error-Cause", "PasswordException");
			resp.sendError(403, "Password failure: " + e);
			return;
		}
		if (json == null) {
			resp.sendError(404, "No such entity.");
			return;
		}
		resp.setHeader("Access-Control-Expose-Headers", "SJS-Version");
		resp.setContentType("application/json");
		byte[] ojson = json.getBytes(StandardCharsets.UTF_8);
		resp.setContentLength(ojson.length);
		OutputStream os = resp.getOutputStream();
		os.write(ojson);
		os.flush();
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.addHeader("Access-Control-Allow-Origin", "*");
		resp.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		Request r = Request.from(req);
		String err = "";
		if (req.getRequestURI().equals("/entry/orders")) {
			if (!postOrders(r)) {
				err = "Failure.";
			}
		} else if (req.getRequestURI().equals("/entry/advanceworld")) {
			if (!postAdvanceWorld(r)) {
				err = "Failure.";
			}
		} else if (req.getRequestURI().equals("/entry/startlobby")) {
			if (!postStartLobby(r)) {
				err = "Failure.";
			}
		} else if (req.getRequestURI().equals("/entry/migrate")) {
			if (!migrate(r)) {
				err = "Failure.";
			}
		} else if (req.getRequestURI().equals("/entry/setup")) {
			if (!postSetup(r)) {
				err = "Not allowed.";
			}
		} else if (req.getRequestURI().equals("/entry/rtc")) {
			if (!postRealTimeCommunication(r)) {
				err = "Not allowed.";
			}
		} else if (req.getRequestURI().equals("/entry/changeplayer")) {
			if (!postChangePlayer(r)) {
				err = "Not allowed.";
			}
		} else {
			err = "No such path.";
		}
		if ("".equals(err)) {
			resp.setStatus(204);
		} else {
			resp.sendError(400, err);
		}
	}

	@Override
	public void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.addHeader("Access-Control-Allow-Origin", "*");
		resp.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		resp.addHeader("Access-Control-Allow-Headers", "X-PINGOTHER, Content-Type");
		super.doOptions(req, resp);
	}

	private static Gson getGson() {
		return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
	}

	private String getOrders(Request r, HttpServletResponse resp) {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		if (!checkPassword(r, service).passesRead()) return null;
		try {
			Orders o = Orders.loadOrder(r.gameId, r.kingdom, r.turn, DatastoreServiceFactory.getDatastoreService());
			resp.setHeader("SJS-Version", "" + o.version);
			return o.json;
		} catch (EntityNotFoundException e) {
			return null;
		}
	}

	private static class GetSetupResponse {
		final int ruleSet;
		final int numPlayers;
		final Set<String> takenNations;
		final Geography geography;
		GetSetupResponse(Lobby lobby) throws IOException {
			this.ruleSet = lobby.getRuleSet();
			this.numPlayers = lobby.getNumPlayers();
			this.takenNations = lobby.getNations().keySet();
			this.geography = Geography.loadGeography(lobby.getRuleSet(), lobby.getNumPlayers());
		}
	}
	private String getSetup(Request r) {
		try {
			return getGson().toJson(new GetSetupResponse(Lobby.load(r.gameId, DatastoreServiceFactory.getDatastoreService())));
		} catch (EntityNotFoundException | IOException e) {
			log.log(Level.WARNING, "Failed to fetch setup information for game " + r.gameId, e);
			return null;
		}
	}

	private int getWorldDate(long gameId, DatastoreService service) throws EntityNotFoundException {
		return (int)((Long)(service.get(KeyFactory.createKey("CURRENTDATE", "game_" + gameId)).getProperty("date"))).longValue();
	}

	private static class GetIndexResponse {
		List<LobbyData> lobbies = new ArrayList<>();
		List<ActiveGameData> games = new ArrayList<>();

		public void addLobby(long id, int players, int capacity, long deadline, Schedule schedule) {
			lobbies.add(new LobbyData(id, players, capacity, deadline, schedule));
		}

		public void addGame(long id, boolean active) {
			games.add(new ActiveGameData(id, active));
		}

		private static class LobbyData {
			long id;
			int players;
			int capacity;
			long deadline;
			Schedule schedule;

			public LobbyData(long id, int players, int capacity, long deadline, Schedule schedule) {
				this.id = id;
				this.players = players;
				this.capacity = capacity;
				this.deadline = deadline;
				this.schedule = schedule;
			}
		}
		private static class ActiveGameData {
			long id;
			boolean active;

			public ActiveGameData(long id, boolean active) {
				this.id = id;
				this.active = active;
			}
		}
	}
	private String getIndex(Request r) throws PasswordException {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		CheckPasswordResult result = checkPassword(r, service);
		if (!result.passesRead()) {
			throw new PasswordException("Password does not pass read ACL.");
		}
		/* // TODO: Need a more scalable thing here than loading every game from the DB. Consider storing it in the Player field?
		for (Long id : ActiveGames.load(service).activeGameIds) {
			// Check if the player is a player in this game.
		}
		*/
		GetIndexResponse response = new GetIndexResponse();
		Lobby.loadAll(service).forEach(lobby -> {
			response.addLobby(lobby.getGameId(), lobby.getNations().size(), lobby.getNumPlayers(), lobby.getStartAt(), lobby.getSchedule());
		});
		return getGson().toJson(response);
	}

	private String getWorld(Request r) {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		CheckPasswordResult result = checkPassword(r, service);
		if (!result.passesRead()) {
			return null;
		}
		try {
			int date = r.hasTurn() ? r.turn : getWorldDate(r.gameId, service);
			World w = World.load(r.gameId, date, service);
			if (result == CheckPasswordResult.PASS_PLAYER && r.turn == 0) LoginCache.getSingleton().recordLogin(r.gameId, date, w.getNation(r.kingdom).getEmail(), service);
			w.filter(r.kingdom);
			return w.toString();
		} catch (EntityNotFoundException e) {
			log.log(Level.INFO, "No such world.");
			return null;
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to read rule data.", e);
			return null;
		}
	}

	private String getGeography(Request r) {
		try {
			DatastoreService service = DatastoreServiceFactory.getDatastoreService();
			int date = r.hasTurn() ? r.turn : getWorldDate(r.gameId, service);
			World w = World.load(r.gameId, date, service);
			return Geography.loadGeography(w.getRuleSet(), w.getNumPlayers()).toString();
		} catch (EntityNotFoundException e) {
			log.log(Level.INFO, "No such world.");
			return null;
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to read rule/map data.", e);
			return null;
		}
	}

	private String getActivity(Request r) {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		if (checkPassword(r, service) != CheckPasswordResult.PASS_GM) return null;
		try {
			int date = r.hasTurn() ? r.turn : getWorldDate(r.gameId, service);
			HashMap<String, ArrayList<String>> nationEmails = new HashMap<>();
			World w = World.load(r.gameId, date, service);
			List<String> emails = w.getNationNames().stream().map(s -> w.getNation(s).getEmail()).collect(Collectors.toList());
			List<List<Boolean>> actives = LoginCache.getSingleton().fetchLoginHistory(r.gameId, date, emails, service);
			List<Map<String, Boolean>> result = new ArrayList<>();
			for (List<Boolean> turnActives : actives) {
				HashMap<String, Boolean> turn = new HashMap<>();
				for (int i = 0; i < emails.size(); i++) {
					turn.put(emails.get(i), turnActives.get(i));
				}
				result.add(turn);
			}
			return getGson().toJson(result);
		} catch (EntityNotFoundException e) {
			log.log(Level.WARNING, "No such world.", e);
			return null;
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to read rule data.", e);
			return null;
		}
	}

	private static class ActiveGames {
		public List<Long> activeGameIds = new ArrayList<>();
		static ActiveGames fromGson(String s) {
			return getGson().fromJson(s, ActiveGames.class);
		}

		static ActiveGames load(DatastoreService service) {
			try {
				return ActiveGames.fromGson((String)service.get(KeyFactory.createKey("ACTIVEGAMES", "_")).getProperty("active_games"));
			} catch (EntityNotFoundException e) {
				return new ActiveGames();
			}
		}
	}

	private String getAdvancePoll(boolean skipMail) {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		try {
			for (Long gameId : ActiveGames.load(service).activeGameIds) {
				Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
				try {
					int date = (int)((Long)(service.get(KeyFactory.createKey("CURRENTDATE", "game_" + gameId)).getProperty("date"))).longValue();
					World	w = World.load(gameId, date, service);
					if (w.getNextTurn() < Instant.now().toEpochMilli()) {
						HashSet<String> kingdoms = new HashSet<>();
						HashMap<String, Map<String, String>> orders = new HashMap<>();
						for (String kingdom : w.getNationNames()) {
							kingdoms.add(kingdom);
							try {
								orders.put(kingdom, Orders.loadOrder(gameId, kingdom, w.getDate(), service).getOrders());
							} catch (EntityNotFoundException e) {
								// Can't load the relevant orders - tool will make default orders.
							}
						}
						Map<String, String> emails = w.advance(orders);
						service.put(w.toEntity(gameId));
						Entity nudate = new Entity("CURRENTDATE", "game_" + gameId);
						nudate.setProperty("date", (long)w.getDate());
						service.put(nudate);
						if (!skipMail && Instant.ofEpochMilli(w.getNextTurn()).isAfter(Instant.now().plus(5, ChronoUnit.HOURS))) {
							for (String mail : emails.keySet()) {
								mail(mail, "👑 Empire: Turn Advances", emails.get(mail).replace("%GAMEID%", "" + gameId));
							}
						}
						if (w.isGameover()) {
							ActiveGames newActiveGames = ActiveGames.fromGson((String)service.get(KeyFactory.createKey("ACTIVEGAMES", "_")).getProperty("active_games"));
							newActiveGames.activeGameIds.remove(gameId);
							Entity activeGames = new Entity("ACTIVEGAMES", "_");
							activeGames.setProperty("active_games", getGson().toJson(newActiveGames));
							service.put(activeGames);
						} else {
							QueueFactory.getDefaultQueue().add(TaskOptions.Builder.withUrl("/entry/advanceworldpoll").etaMillis(w.getNextTurn()).method(TaskOptions.Method.GET));
						}
					}
					txn.commit();
				} catch (EntityNotFoundException e) {
					log.log(Level.SEVERE, "World issue.", e);
				} finally {
					if (txn.isActive()) txn.rollback();
				}
			}
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to read rule data.", e);
			return null;
		}
		return "";
	}

	private String getLobbyPoll() {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		Lobby.loadAll(service).forEach(lobby -> {
			Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
			try {
				Lobby.StartResult start = lobby.canStart(Instant.now());
				if (start == Lobby.StartResult.START) {
					startWorld(lobby, service);
				} else if (start == Lobby.StartResult.ABANDON) {
					HashSet<String> addresses = new HashSet<String>();
					for (NationSetup nation : lobby.getNations().values()) addresses.add(nation.email);
					mail(addresses, "👑 Empire: Game Failed to Start", "A lobby of Empire that you were in didn't get enough players by the deadline and has expired.");
					lobby.delete(service);
				}
				txn.commit();
			} finally {
				if (txn.isActive()) txn.rollback();
			}
		});
		return "";
	}

	private boolean postAdvanceWorld(Request r) {
		if (!passesGmPassword(hashPassword(r.password))) return false;
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
		HashSet<String> kingdoms = new HashSet<>();
		try {
			World	w = World.load(r.gameId, r.turn, service);
			w.setNextTurn(0);
			service.put(w.toEntity(r.gameId));
			txn.commit();
		} catch (EntityNotFoundException e) {
			return false;
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to read rule data.", e);
			return false;
		} finally {
			if (txn.isActive()) txn.rollback();
		}
		getAdvancePoll(r.skipMail);
		return true;
	}

	private boolean startWorld(Lobby lobby, DatastoreService service) {
		HashSet<String> addresses = new HashSet<String>();
		long gameId = lobby.getGameId();
		try {
			for (NationSetup nation : lobby.getNations().values()) addresses.add(nation.email);
			World w = World.startNew(lobby);
			service.put(w.toEntity(gameId));
			Entity g = new Entity("CURRENTDATE", "game_" + gameId);
			g.setProperty("date", 1);
			service.put(g);
			ActiveGames activeGames;
			try {
				activeGames = ActiveGames.fromGson((String)service.get(KeyFactory.createKey("ACTIVEGAMES", "_")).getProperty("active_games"));
			} catch (EntityNotFoundException e) {
				// No active game registry - create it.
				activeGames = new ActiveGames();
				activeGames.activeGameIds = new ArrayList<>();
			}
			activeGames.activeGameIds.add(gameId);
			Entity games = new Entity("ACTIVEGAMES", "_");
			games.setProperty("active_games", getGson().toJson(activeGames));
			lobby.delete(service);
			service.put(games);
			QueueFactory.getDefaultQueue().add(TaskOptions.Builder.withUrl("/entry/advanceworldpoll").etaMillis(w.getNextTurn()).method(TaskOptions.Method.GET));
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to start game " + gameId, e);
			return false;
		}
		mail(addresses, "👑 Empire: Game Begins", "A game of Empire that you are playing in has started! You can make your orders for the first turn at https://pawlicki.kaelri.com/empire/map1.html?g=" + gameId + ".");
		return true;
	}

	private static class StartLobbyBody {
		int players;
		int minPlayers;
		long startAtMillis;
		Schedule schedule;
	}

	private boolean postStartLobby(Request r) {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
		StartLobbyBody startLobby = getGson().fromJson(r.body, StartLobbyBody.class);
		// These checks limit the kinds of lobbies that can be started. While the player base is so small, the intent is to force them into the same kinds of games.
		if (startLobby.players != 26) return false;
		if (startLobby.minPlayers != 13) return false;

		// Sanity scale-guard check that there aren't more than 20 open lobbies.
		if (Lobby.loadAll(service).count() > 20) return false;

		try {
			long gameId = -1;
			boolean gameOk = false;
			for (int i = 0; i < 15; i++) {
				gameId = (long) (Math.random() * 10000000L);
				try {
					Lobby exists = Lobby.load(r.gameId, service);
					continue;
				} catch (EntityNotFoundException expected) {}
				try {
					World exists = World.load(r.gameId, 1, service);
					continue;
				} catch (EntityNotFoundException expected) {}
				gameOk = true;
			}
			if (!gameOk) return false;
			Lobby.newLobby(gameId, Rules.LATEST, startLobby.players, startLobby.schedule, startLobby.minPlayers, startLobby.startAtMillis).save(service);
			txn.commit();
			QueueFactory.getDefaultQueue().add(TaskOptions.Builder.withUrl("/entry/lobbypoll").etaMillis(startLobby.startAtMillis).method(TaskOptions.Method.GET));
		} catch (IOException e) {
			log.log(Level.SEVERE, "IOException Starting Lobby", e);
			return false;
		} finally {
			if (txn.isActive()) txn.rollback();
		}
		return true;
	}

	private enum CheckPasswordResult {
		PASS_GM(true, true),
		PASS_PLAYER(true, true),
		FAIL(false, false),
		NO_ENTITY(false, false);

		private final boolean passesRead;
		private final boolean passesWrite;

		private CheckPasswordResult(boolean passesRead, boolean passesWrite) {
			this.passesRead = passesRead;
			this.passesWrite = passesWrite;
		}

		public boolean passesRead() {
			return passesRead;
		}
		public boolean passesWrite() {
			return passesWrite;
		}
	}

	private byte[] hashPassword(String password) {
		try {
			return MessageDigest.getInstance("SHA-256").digest((PASSWORD_SALT + password).getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean passesGmPassword(byte[] passwordHash) {
		return Arrays.equals(GM_PASSWORD_HASH, passwordHash);
	}

	private CheckPasswordResult checkPassword(Request r, DatastoreService service) {
		if (r.password == null) return CheckPasswordResult.FAIL;
		byte[] attemptHash = hashPassword(r.password);
		if (passesGmPassword(attemptHash)) return CheckPasswordResult.PASS_GM;
		try {
			Player p = null;
			if (r.gameId != -1) {
				World w = World.load(r.gameId, getWorldDate(r.gameId, service), service);
				if (w.getNationNames().contains(r.kingdom) && Arrays.equals(attemptHash, BaseEncoding.base16().decode(Player.loadPlayer(w.getNation(r.kingdom).getEmail(), service).passHash))) return CheckPasswordResult.PASS_PLAYER;
			} else {
				if (Arrays.equals(attemptHash, BaseEncoding.base16().decode(Player.loadPlayer(r.kingdom, service).passHash))) return CheckPasswordResult.PASS_PLAYER;
			}
		} catch (EntityNotFoundException e) {
			log.log(Level.INFO, "No world for " + r.gameId + ", " + r.kingdom);
			return CheckPasswordResult.NO_ENTITY;
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to read rule data.", e);
		}
		return CheckPasswordResult.FAIL;
	}

	private boolean postOrders(Request r) {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
		try {
			if (!checkPassword(r, service).passesWrite()) return false;
			if (r.turn != getWorldDate(r.gameId, service)) return false;
			service.put(new Orders(r.gameId, r.kingdom, r.turn, r.version, r.body).toEntity());
			txn.commit();
		} catch (EntityNotFoundException e) {
			log.log(Level.WARNING, "No current turn for " + r.gameId + ".");
			return false;
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
		return true;
	}

	private boolean postRealTimeCommunication(Request r) {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
		try {
			if (!checkPassword(r, service).passesWrite()) return false;
			World w = World.load(r.gameId, r.turn, service);
			w.addRtc(r.body, r.kingdom);
			service.put(w.toEntity(r.gameId));
			txn.commit();
		} catch (EntityNotFoundException e) {
			log.log(Level.INFO, "Not found for " + r.gameId + ", " + r.kingdom, e);
			return false;
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to read rule data.", e);
			return false;
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
		return true;
	}

	private static final class ChangePlayerRequestBody {
		public String email;
		public String password;
	}
	private boolean postChangePlayer(Request r) {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
		try {
			if (!checkPassword(r, service).passesWrite()) return false;
			int date = r.hasTurn() ? r.turn : getWorldDate(r.gameId, service);
			World w = World.load(r.gameId, date, service);
			ChangePlayerRequestBody body = getGson().fromJson(r.body, ChangePlayerRequestBody.class);
			Player p = Player.loadPlayer(body.email, service);
			w.getNation(r.kingdom).setEmail(body.email);
			service.put(w.toEntity(r.gameId));
			txn.commit();
		} catch (EntityNotFoundException e) {
			log.log(Level.INFO, "Not found for " + r.gameId + ", " + r.kingdom, e);
			return false;
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to read rule data.", e);
			return false;
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
		return true;
	}

	// TODO: remove
	private boolean migrate(Request rr) {
		if (!passesGmPassword(hashPassword(rr.password))) return false;
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
		try {
			World w = World.load(9, getWorldDate(9, service), service);
			for (com.empire.Character c : w.getCharacters()) {
				if (c.getName().equals("Beste")) c.setLocation(153);
				if (c.getName().equals("Çağri")) c.setLocation(167);
				if (c.getName().equals("Sinem")) c.setLocation(162);
				if (c.getName().equals("Yavuz")) c.setLocation(153);
				if (c.getName().equals("Güvenç")) c.setLocation(159);
			}
			service.put(w.toEntity(9));
			txn.commit();
		} catch (EntityNotFoundException e) {
			log.log(Level.INFO, "Not found!", e);
			return false;
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to read rule data.", e);
			return false;
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
		return true;
	}

	// TODO - don't save the password in the Lobby.
	// Don't update the password if the player exists - instead we require a standard login.
	// We should respond with a particular error in this case, though, so that the UI can offer
	// password reset.
	private boolean postSetup(Request r) {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
		try {
			Lobby lobby = Lobby.load(r.gameId, service);
			Geography geo = Geography.loadGeography(lobby.getRuleSet(), lobby.getNumPlayers());
			if (!geo.getKingdoms().stream().anyMatch(k -> k.name.equals(r.kingdom))) {
				log.log(Level.WARNING, "postSetup kingdom matching failure for " + r.gameId + ", " + r.kingdom);
				return false;
			}
			NationSetup nation = NationSetup.fromJson(r.body);
			if (!lobby.update(r.kingdom, nation)) {
				log.log(Level.WARNING, "postSetup lobby update failure for " + r.gameId + ", " + r.kingdom);
				return false;
			}
			lobby.save(service);
			String password = BaseEncoding.base16().encode(MessageDigest.getInstance("SHA-256").digest((PASSWORD_SALT + nation.password).getBytes(StandardCharsets.UTF_8)));
			try {
				service.put(Player.loadPlayer(nation.email, service).withNewPassword(password).toEntity());
			} catch (EntityNotFoundException e) {
				// New player.
				service.put(new Player(nation.email, password).toEntity());
			}
			if (lobby.canStart(Instant.now()) == Lobby.StartResult.START) {
				startWorld(lobby, service);
			}
			txn.commit();
		} catch (NoSuchAlgorithmException | EntityNotFoundException | IOException ee) {
			log.log(Level.SEVERE, "postSetup Failure for " + r.gameId + ", " + r.kingdom, ee);
			return false;
		} finally {
			if (txn.isActive()) txn.rollback();
		}
		return true;
	}

	private void mail(String address, String subject, String body) {
		HashSet<String> s = new HashSet<>();
		s.add(address);
		mail(s, subject, body);
	}

	private void mail(HashSet<String> addresses, String subject, String body) {
		try {
			Message msg = new MimeMessage(Session.getDefaultInstance(new Properties(), null));
			msg.setFrom(new InternetAddress("gilgarn@gmail.com", "Joshua Pawlicki"));
			for (String address : addresses) {
				msg.addRecipient(Message.RecipientType.BCC, new InternetAddress(address, "Empire Player"));
			}
			msg.setSubject(subject);
			msg.setText(body);
			Transport.send(msg);
		} catch (MessagingException | UnsupportedEncodingException | NoClassDefFoundError | OverQuotaException e) {
			log.log(Level.SEVERE, "Failed to send mail", e);
		}
	}
}
