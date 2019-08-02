package com.empire.svc;

import com.empire.Geography;
import com.empire.Lobby;
import com.empire.Nation;
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

POST /entry/orders?gid=1234&k=Aefoss&password=foobar&t=22
	Check password and post the given order data.
POST /entry/setup?gid=1234&k=Aefoss&t=0&password=foobar
	Set customization info for the kingdom, if unset.
POST /entry/advanceworld?gid=1234
	Check cadence and possibly advance world to next step, mail players.
POST /entry/startworld?gid=1234
	Start a new game.
POST /entry/startlobby?gid=1234
	Start a new lobby.

TODO: Will eventually need a changePassword/change-email.
*/

@WebServlet(name = "EntryServlet", value = "/entry/*")
public class EntryServlet extends HttpServlet {
	private static final Logger log = Logger.getLogger(EntryServlet.class.getName());
	private static final String PASSWORD_SALT = "~ Empire_Password Salt ~123`";

		@Override
		public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		Request r = Request.from(req);
		resp.setHeader("Access-Control-Allow-Origin", "*");
		String json = "";
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
		} else if (req.getRequestURI().equals("/entry/activity")) {
			json = getActivity(r);
		} else {
			resp.sendError(404, "No such path.");
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
		} else if (req.getRequestURI().equals("/entry/startworld")) {
			if (!postStartWorld(r)) {
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
			return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(new GetSetupResponse(Lobby.load(r.gameId, DatastoreServiceFactory.getDatastoreService())));
		} catch (EntityNotFoundException | IOException e) {
			log.log(Level.WARNING, "Failed to fetch setup information for game " + r.gameId, e);
			return null;
		}
	}

	private int getWorldDate(long gameId, DatastoreService service) throws EntityNotFoundException {
		return (int)((Long)(service.get(KeyFactory.createKey("CURRENTDATE", "game_" + gameId)).getProperty("date"))).longValue();
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
			return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(result);
		} catch (EntityNotFoundException e) {
			log.log(Level.WARNING, "No such world.", e);
			return null;
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to read rule data.", e);
			return null;
		}
	}

	private static class ActiveGames {
		public ArrayList<Long> activeGameIds;
		static ActiveGames fromGson(String s) {
			return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().fromJson(s, ActiveGames.class);
		}
	}

	private String getAdvancePoll(boolean skipMail) {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		try {
			for (Long gameId : ActiveGames.fromGson((String)service.get(KeyFactory.createKey("ACTIVEGAMES", "_")).getProperty("active_games")).activeGameIds) {
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
							activeGames.setProperty("active_games", new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(newActiveGames));
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
		} catch (EntityNotFoundException e) {
			log.log(Level.SEVERE, "Poller failed.", e);
			return null;
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to read rule data.", e);
			return null;
		}
		return "";
	}

	// TODO: remove, or check that the request bears the GM password - this is insecure as-is (anyone can advance).
	private boolean postAdvanceWorld(Request r) {
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

	private boolean postStartWorld(Request r) {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		StartWorld s = StartWorld.fromJson(r.body);
		String passHash;
		String obsPassHash;
		try {
			passHash = BaseEncoding.base16().encode(MessageDigest.getInstance("SHA-256").digest((PASSWORD_SALT + s.gmPassword).getBytes(StandardCharsets.UTF_8)));
			obsPassHash = BaseEncoding.base16().encode(MessageDigest.getInstance("SHA-256").digest((PASSWORD_SALT + s.obsPassword).getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			log.log(Level.SEVERE, "Hash error", e);
			return false;
		}
		Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
		HashSet<String> addresses = new HashSet<String>();
		try {
			// Collect setups.
			Lobby lobby = Lobby.load(r.gameId, service);
			for (Nation nation : lobby.getNations().values()) addresses.add(nation.email);
			World w = World.startNew(passHash, obsPassHash, lobby);
			service.put(w.toEntity(r.gameId));
			Entity g = new Entity("CURRENTDATE", "game_" + r.gameId);
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
			activeGames.activeGameIds.add(r.gameId);
			Entity games = new Entity("ACTIVEGAMES", "_");
			games.setProperty("active_games", new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(activeGames));
			service.put(games);
			txn.commit();
			QueueFactory.getDefaultQueue().add(TaskOptions.Builder.withUrl("/entry/advanceworldpoll").etaMillis(w.getNextTurn()).method(TaskOptions.Method.GET));
		} catch (IOException | EntityNotFoundException e) {
			log.log(Level.SEVERE, "Failed to start game " + r.gameId, e);
			return false;
		} finally {
			if (txn.isActive()) txn.rollback();
		}
		mail(addresses, "👑 Empire: Game Begins", "A game of Empire that you are playing in has started! You can make your orders for the first turn at http://pawlicki.kaelri.com/empire/map1.html?gid=" + r.gameId + ".");
		return true;
	}

	private static class StartLobbyBody {
		int players;
		Schedule schedule;
	}

	private boolean postStartLobby(Request r) {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
		try {
			// If it exists, don't create another one.
			try {
				Lobby exists = Lobby.load(r.gameId, service);
				return false;
			} catch (EntityNotFoundException expected) {}
			StartLobbyBody startLobby = new Gson().fromJson(r.body, StartLobbyBody.class);
			Lobby.newLobby(Rules.LATEST, startLobby.players, startLobby.schedule).save(r.gameId, service);
			txn.commit();
		} finally {
			if (txn.isActive()) txn.rollback();
		}
		return true;
	}

	private enum CheckPasswordResult {
		PASS_GM(true, true),
		PASS_OBS(true, false),
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
	private CheckPasswordResult checkPassword(Request r, DatastoreService service) {
		try {
			if (r.password == null) return CheckPasswordResult.FAIL;
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] attemptHash = digest.digest((PASSWORD_SALT + r.password).getBytes(StandardCharsets.UTF_8));
			int date = getWorldDate(r.gameId, service);
			World w = World.load(r.gameId, date, service);
			byte[] gmPassHash = BaseEncoding.base16().decode(w.getGmPasswordHash());
			byte[] obsPassHash = BaseEncoding.base16().decode(w.getObsPasswordHash());
			if (w.getNationNames().contains(r.kingdom) && Arrays.equals(attemptHash, BaseEncoding.base16().decode(Player.loadPlayer(w.getNation(r.kingdom).getEmail(), service).passHash))) return CheckPasswordResult.PASS_PLAYER;
			if (Arrays.equals(attemptHash, gmPassHash)) return CheckPasswordResult.PASS_GM;
			if (Arrays.equals(attemptHash, obsPassHash)) return CheckPasswordResult.PASS_OBS;
			return CheckPasswordResult.FAIL;
		} catch (EntityNotFoundException e) {
			log.log(Level.INFO, "No world for " + r.gameId + ", " + r.kingdom);
			return CheckPasswordResult.NO_ENTITY;
		} catch (NoSuchAlgorithmException e) {
			log.log(Level.SEVERE, "CheckPassword Failure", e);
			return CheckPasswordResult.FAIL;
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to read rule data.", e);
			return CheckPasswordResult.FAIL;
		}
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
			ChangePlayerRequestBody body = new GsonBuilder().create().fromJson(r.body, ChangePlayerRequestBody.class);
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
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
		try {
			World w = World.load(4, getWorldDate(4, service), service);
			for (com.empire.Character c : w.getCharacters()) if (c.getName().equals("Ea Rjinkuki")) c.setLocation(101);
			service.put(w.toEntity(4));
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
			Nation nation = Nation.fromJson(r.body);
			if (!lobby.update(r.kingdom, nation)) {
				log.log(Level.WARNING, "postSetup lobby update failure for " + r.gameId + ", " + r.kingdom);
				return false;
			}
			lobby.save(r.gameId, service);
			String password = BaseEncoding.base16().encode(MessageDigest.getInstance("SHA-256").digest((PASSWORD_SALT + nation.password).getBytes(StandardCharsets.UTF_8)));
			try {
				service.put(Player.loadPlayer(nation.email, service).withNewPassword(password).toEntity());
			} catch (EntityNotFoundException e) {
				// New player.
				service.put(new Player(nation.email, password).toEntity());
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
