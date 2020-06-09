package com.empire.svc;

import com.empire.Geography;
import com.empire.Lobby;
import com.empire.NationSetup;
import com.empire.Orders;
import com.empire.Rules;
import com.empire.Schedule;
import com.empire.World;
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
	private static byte[] GM_PASSWORD_HASH = BaseEncoding.base16().decode("DFEC33349F0EE2E0BC2085D761553BDDF1753698DDAD94491664F14EA58EA072");

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		Request r = Request.from(req);
		resp.setHeader("Access-Control-Allow-Origin", "*");
		resp.setHeader("Access-Control-Allow-Headers", "Authorization");
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
		resp.setHeader("Access-Control-Allow-Headers", "Authorization");
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
		resp.addHeader("Access-Control-Allow-Headers", "Authorization, X-PINGOTHER, Content-Type");
		super.doOptions(req, resp);
	}

	private static Gson getGson() {
		return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
	}

	private String getOrders(Request r, HttpServletResponse resp) {
		try (DataSource dataSource = DataSource.transactional()) {
			if (!checkPassword(r, dataSource, dataSource.loadWorld(r.gameId, r.turn)).passesRead()) return null;
			Orders o = dataSource.loadOrder(r.gameId, r.kingdom, r.turn);
			resp.setHeader("SJS-Version", "" + o.version);
			return o.json;
		} catch (EntityNotFoundException | IOException e) {
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
		try (DataSource dataSource = DataSource.nontransactional()) {
			return getGson().toJson(new GetSetupResponse(dataSource.loadLobby(r.gameId)));
		} catch (EntityNotFoundException | IOException e) {
			log.log(Level.WARNING, "Failed to fetch setup information for game " + r.gameId, e);
			return null;
		}
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
		if (r.newAccount) {
			try {
				Player.loadPlayer(r.kingdom, service);
				throw new PasswordException("Account exists.");
			} catch (EntityNotFoundException expected) {}
			service.put(new Player(r.kingdom, BaseEncoding.base16().encode(hashPassword(r.password))).toEntity());
		} else {
			CheckPasswordResult result = checkPassword(r, service);
			if (!result.passesRead()) {
				throw new PasswordException("Password does not pass read ACL.");
			}
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
		try (DataSource dataSource = DataSource.nontransactional()) {
			int date = r.hasTurn() ? r.turn : dataSource.loadCurrentDate(r.gameId);
			World w = dataSource.loadWorld(r.gameId, date);
			CheckPasswordResult result = checkPassword(r, dataSource, w);
			if (!result.passesRead()) return null;
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
		try (DataSource dataSource = DataSource.nontransactional()) {
			int date = r.hasTurn() ? r.turn : dataSource.loadCurrentDate(r.gameId);
			World w = dataSource.loadWorld(r.gameId, date);
			return Geography.loadGeography(w.getRuleSet(), w.getNumPlayers()).toString();
		} catch (EntityNotFoundException e) {
			log.log(Level.INFO, "No such world.");
			return null;
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to read rule/map data.", e);
			return null;
		}
	}

	private String getAdvancePoll(boolean skipMail) {
		List<Long> activeGames;
		try (DataSource dataSource = DataSource.nontransactional()) {
			activeGames = dataSource.loadActiveGames().activeGameIds;
		} catch (EntityNotFoundException e) {
			log.log(Level.SEVERE, "Poller failed.", e);
			return null;
		}
		for (Long gameId : activeGames) {
			try (DataSource dataSource = DataSource.transactional()) {
				int date = dataSource.loadCurrentDate(gameId);
				World	w = dataSource.loadWorld(gameId, date);
				if (w.getNextTurn() < Instant.now().toEpochMilli()) {
					HashSet<String> kingdoms = new HashSet<>();
					HashMap<String, Map<String, String>> orders = new HashMap<>();
					for (String kingdom : w.getNationNames()) {
						kingdoms.add(kingdom);
						try {
							orders.put(kingdom, dataSource.loadOrder(gameId, kingdom, w.getDate()).getOrders());
						} catch (EntityNotFoundException e) {
							// Can't load the relevant orders - tool will make default orders.
						}
					}
					Map<String, String> emails = w.advance(orders);
					dataSource.save(w, gameId);
					dataSource.saveCurrentDate(w.getDate(), gameId);
					if (!skipMail && Instant.ofEpochMilli(w.getNextTurn()).isAfter(Instant.now().plus(5, ChronoUnit.HOURS))) {
						for (String mail : emails.keySet()) {
							mail(mail, "👑 Empire: Turn Advances", emails.get(mail).replace("%GAMEID%", "" + gameId));
						}
					}
					if (w.isGameover()) {
						ActiveGames newActiveGames = dataSource.loadActiveGames();
						newActiveGames.activeGameIds.remove(gameId);
						dataSource.save(newActiveGames);
					} else {
						QueueFactory.getDefaultQueue().add(TaskOptions.Builder.withUrl("/entry/advanceworldpoll").etaMillis(w.getNextTurn()).method(TaskOptions.Method.GET));
					}
				}
				dataSource.commit();
			} catch (EntityNotFoundException e) {
				log.log(Level.SEVERE, "World issue.", e);
			} catch (IOException e) {
				log.log(Level.SEVERE, "Failed to read rule data.", e);
				return null;
			}
		}
		return "";
	}

	private String getLobbyPoll() {
		try (DataSource dataSource = DataSource.transactional()) {
			dataSource.loadAllLobbies().forEach(lobby -> {
				Lobby.StartResult start = lobby.canStart(Instant.now());
				if (start == Lobby.StartResult.START) {
					startWorld(lobby, dataSource);
				} else if (start == Lobby.StartResult.ABANDON) {
					HashSet<String> addresses = new HashSet<String>();
					for (NationSetup nation : lobby.getNations().values()) addresses.add(nation.email);
					mail(addresses, "👑 Empire: Game Failed to Start", "A lobby of Empire that you were in didn't get enough players by the deadline and has expired.");
					dataSource.delete(lobby);
				}
			});
			dataSource.commit();
		}
		return "";
	}

	private boolean postAdvanceWorld(Request r) {
		if (!passesGmPassword(Hasher.hashPassword(r.password))) return false;
		try (DataSource dataSource = DataSource.transactional()) {
			HashSet<String> kingdoms = new HashSet<>();
			World	w = dataSource.loadWorld(r.gameId, r.turn);
			w.setNextTurn(0);
			dataSource.save(w, r.gameId);
			dataSource.commit();
		} catch (EntityNotFoundException e) {
			return false;
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to read rule data.", e);
			return false;
		}
		getAdvancePoll(r.skipMail);
		return true;
	}

	private boolean startWorld(Lobby lobby, DataSource dataSource) {
		HashSet<String> addresses = new HashSet<String>();
		long gameId = lobby.getGameId();
		try {
			for (NationSetup nation : lobby.getNations().values()) addresses.add(nation.email);
			World w = World.startNew(lobby);
			dataSource.save(w, gameId);
			dataSource.saveCurrentDate(1, gameId);
			ActiveGames activeGames;
			try {
				activeGames = dataSource.loadActiveGames();
			} catch (EntityNotFoundException e) {
				// No active game registry - create it.
				activeGames = new ActiveGames();
				activeGames.activeGameIds = new ArrayList<>();
			}
			activeGames.activeGameIds.add(gameId);
			dataSource.save(activeGames);
			dataSource.delete(lobby);
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

		try (DataSource dataSource = DataSource.transactional()) {
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
			dataSource.save(Lobby.newLobby(gameId, Rules.LATEST, startLobby.players, startLobby.schedule, startLobby.minPlayers, startLobby.startAtMillis));
			dataSource.commit();
			QueueFactory.getDefaultQueue().add(TaskOptions.Builder.withUrl("/entry/lobbypoll").etaMillis(startLobby.startAtMillis).method(TaskOptions.Method.GET));
		} catch (IOException e) {
			log.log(Level.SEVERE, "IOException Starting Lobby", e);
			return false;
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

	private boolean passesGmPassword(byte[] passwordHash) {
		return Arrays.equals(GM_PASSWORD_HASH, passwordHash);
	}

	private CheckPasswordResult checkPassword(Request r, DatastoreService service) {
		try {
			if (r.password == null) return CheckPasswordResult.FAIL;
			byte[] attemptHash = hashPassword(r.password);
			if (passesGmPassword(attemptHash)) return CheckPasswordResult.PASS_GM;
			Player p = null;
			if (r.gameId != -1) {
				World w = World.load(r.gameId, getWorldDate(r.gameId, service), service);
				if (w.getNationNames().contains(r.kingdom) && Arrays.equals(attemptHash, BaseEncoding.base16().decode(dataSource.loadPlayer(w.getNation(r.kingdom).getEmail()).passHash))) return CheckPasswordResult.PASS_PLAYER;
			} else {
				if (Arrays.equals(attemptHash, BaseEncoding.base16().decode(dataSource.loadPlayer(r.kingdom).passHash))) return CheckPasswordResult.PASS_PLAYER;
			}
		} catch (EntityNotFoundException e) {
			log.log(Level.INFO, "No player for " + w.getNation(r.kingdom).getEmail() + " in " + r.gameId);
			return CheckPasswordResult.NO_ENTITY;
		} catch (IOException e) {
			log.log(Level.SEVERE, "IOException finding player " + w.getNation(r.kingdom).getEmail(), e);
			return CheckPasswordResult.NO_ENTITY;
		}
		return CheckPasswordResult.FAIL;
	}

	private boolean postOrders(Request r) {
		try (DataSource dataSource = DataSource.transactional()) {
			World w = dataSource.loadWorld(r.gameId, r.turn);
			if (!checkPassword(r, dataSource, w).passesWrite()) return false;
			if (r.turn != dataSource.loadCurrentDate(r.gameId)) return false;
			dataSource.save(new Orders(r.gameId, r.kingdom, r.turn, r.version, r.body));
			dataSource.commit();
		} catch (EntityNotFoundException e) {
			log.log(Level.WARNING, "No current turn for " + r.gameId + ".", e);
			return false;
		} catch (IOException e) {
			log.log(Level.WARNING, "Failed to initialize world for " + r.gameId + ".", e);
			return false;
		}
		return true;
	}

	// TODO: remove
	private boolean migrate(Request rr) {
		final long gameId = 9;
		if (!passesGmPassword(Hasher.hashPassword(rr.password))) return false;
		try (DataSource dataSource = DataSource.transactional()) {
			World w = dataSource.loadWorld(gameId, dataSource.loadCurrentDate(gameId));
			//for (com.empire.Region r : w.regions) if (r.population <= 0) r.population = 1;
			dataSource.save(w, gameId);
			dataSource.commit();
		} catch (EntityNotFoundException e) {
			log.log(Level.INFO, "Not found!", e);
			return false;
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to read rule data.", e);
			return false;
		}
		return true;
	}

	// Don't update the password if the player exists - instead we require a standard login.
	// We should respond with a particular error in this case, though, so that the UI can offer
	// password reset.
	private boolean postSetup(Request r) {
		try (DataSource dataSource = DataSource.transactional()) {
			Lobby lobby = dataSource.loadLobby(r.gameId);
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
			dataSource.save(lobby);
			String password = BaseEncoding.base16().encode(Hasher.hashPassword(nation.password));
			try {
				// TODO: remove this vulnerability.
				dataSource.save(dataSource.loadPlayer(nation.email).withNewPassword(password));
			} catch (EntityNotFoundException e) {
				// New player.
				dataSource.save(new Player(nation.email, password));
			}
			if (lobby.canStart(Instant.now()) == Lobby.StartResult.START) {
				startWorld(lobby, dataSource);
			}
			dataSource.commit();
		} catch (EntityNotFoundException | IOException ee) {
			log.log(Level.SEVERE, "postSetup Failure for " + r.gameId + ", " + r.kingdom, ee);
			return false;
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
