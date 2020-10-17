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
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  List all past and current games a player is in.

POST /entry/orders?gid=1234&k=Aefoss&password=foobar&t=22
	Check password and post the given order data.
POST /entry/setup?gid=1234&k=Aefoss&t=0&password=foobar
	Set customization info for the kingdom, if unset.
POST /entry/advanceworld?gid=1234
	Check cadence and possibly advance world to next step, mail players.
POST /entry/startlobby?gid=1234
	Start a new lobby.
POST /entry/newplayer
  Create a new player account.
POST /entry/resetpassword
	Trigger password reset.
POST /entry/changepassword
	Change the user's password.
*/

@WebServlet(name = "EntryServlet", value = "/entry/*")
public class EntryServlet extends HttpServlet {
	private static final Logger log = Logger.getLogger(EntryServlet.class.getName());

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
			} else if (req.getRequestURI().equals("/entry/scores")) {
				json = getScores(r);
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
		try {
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
			} else if (req.getRequestURI().equals("/entry/newplayer")) {
				if (!postNewPlayer(r)) {
					err = "Failure.";
				}
			} else if (req.getRequestURI().equals("/entry/changepassword")) {
				if (!postChangePassword(r)) {
					err = "Failure.";
				}
			} else if (req.getRequestURI().equals("/entry/resetpassword")) {
				if (!postResetPassword(r)) {
					err = "Failure.";
				}
			} else {
				err = "No such path.";
			}
		} catch (PasswordException e) {
			resp.setHeader("X-Empire-Error-Cause", "PasswordException");
			resp.sendError(403, "Password failure: " + e);
			return;
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

	private String getOrders(Request r, HttpServletResponse resp) throws PasswordException {
		try (DataSource dataSource = DataSource.transactional()) {
			if (!dataSource.loadPlayer(r.player).checkPassword(r.password)) throw new PasswordException("Password does not pass read ACL.");
			Orders o = dataSource.loadOrder(r.gameId, r.player, r.turn);
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
		final String chosen;
		final NationSetup chosenConfig;
		final Geography geography;
		GetSetupResponse(Lobby lobby, String email) throws IOException {
			this.ruleSet = lobby.getRuleSet();
			this.numPlayers = lobby.getNumPlayers();
			this.takenNations = lobby.getNations().keySet();
			this.geography = Geography.loadGeography(lobby.getRuleSet(), lobby.getNumPlayers());
			String whoami = null;
			NationSetup whoamiConfig = null;
			for (Map.Entry<String, NationSetup> e : lobby.getNations().entrySet()) {
				if (e.getValue().email.equals(email)) {
					whoami = e.getKey();
					whoamiConfig = e.getValue();
				}
			}
			this.chosen = whoami;
			this.chosenConfig = whoamiConfig;
		}
	}
	private String getSetup(Request r) {
		try (DataSource dataSource = DataSource.nontransactional()) {
			Player p;
			try {
				p = dataSource.loadPlayer(r.player);
				if (!p.checkPassword(r.password)) {
					throw new PasswordException("Password does not pass read ACL.");
				}
			} catch (EntityNotFoundException e) {
				throw new PasswordException("Password does not pass read ACL.");
			}
			return getGson().toJson(new GetSetupResponse(dataSource.loadLobby(r.gameId), r.player));
		} catch (EntityNotFoundException | IOException | PasswordException e) {
			log.log(Level.WARNING, "Failed to fetch setup information for game " + r.gameId, e);
			return null;
		}
	}

	private static class GetIndexResponse {
		boolean mailConfirmed = false;
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
		try (DataSource dataSource = DataSource.transactional()) {
			Player p;
			try {
				p = dataSource.loadPlayer(r.player);
				if (!p.checkPassword(r.password)) {
					throw new PasswordException("Password does not pass read ACL.");
				}
			} catch (EntityNotFoundException e) {
				throw new PasswordException("Password does not pass read ACL.");
			}
			Set<Long> games = new HashSet<>(dataSource.loadActiveGames().activeGameIds);
			GetIndexResponse response = new GetIndexResponse();
			p.activeGames.stream().forEach(gid -> response.addGame(gid, games.contains(gid)));
			dataSource.loadAllLobbies().forEach(lobby -> {
				response.addLobby(lobby.getGameId(), lobby.getNations().size(), lobby.getNumPlayers(), lobby.getStartAt(), lobby.getSchedule());
			});
			return getGson().toJson(response);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to read rule data.", e);
			return null;
		}
	}

	private String getScores(Request r) {
		try (DataSource dataSource = DataSource.nontransactional()) {
			return getGson().toJson(dataSource.loadHighScores());
		}
	}

	private String getWorld(Request r) throws PasswordException {
		try (DataSource dataSource = DataSource.nontransactional()) {
			if (!dataSource.loadPlayer(r.player).checkPassword(r.password)) throw new PasswordException("Password does not pass read ACL.");
			int date = r.hasTurn() ? r.turn : dataSource.loadCurrentDate(r.gameId);
			World w = dataSource.loadWorld(r.gameId, date);
			Optional<String> k = w.getNationName(r.player);
			if (!k.isPresent()) return null;
			w.filter(k.get());
			return w.toString();
		} catch (EntityNotFoundException e) {
			log.log(Level.INFO, "No such world.", e);
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
							orders.put(kingdom, dataSource.loadOrder(gameId, w.getNation(kingdom).email, w.getDate()).getOrders());
						} catch (EntityNotFoundException e) {
							// Can't load the relevant orders - tool will make default orders.
						}
					}
					Map<String, String> emails = w.advance(orders);
					HighScores scores = dataSource.loadHighScores();
					for (String k : w.getNationNames()) scores.record(w.getDate(), k, w.getNation(k).getScore());
					dataSource.save(scores);
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
			} catch (Exception e) {
				log.log(Level.SEVERE, "Uncaught advancement exception:", e);
				throw e;
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

	private boolean postAdvanceWorld(Request r) throws PasswordException {
		if (!Player.passesGmPassword(r.password)) throw new PasswordException("Password fails GM ACL.");
		try (DataSource dataSource = DataSource.transactional()) {
			HashSet<String> kingdoms = new HashSet<>();
			World	w = dataSource.loadWorld(r.gameId, r.turn);
			w.setNextTurn(0);
			dataSource.saveCurrentDate(r.turn, r.gameId);
			dataSource.save(w, r.gameId);
			dataSource.commit();
		} catch (EntityNotFoundException e) {
			return false;
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to read rule data.", e);
			return false;
		}
		getAdvancePoll(true);
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
			ActiveGames activeGames = dataSource.loadActiveGames();
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
		StartLobbyBody startLobby = getGson().fromJson(r.body, StartLobbyBody.class);

		try (DataSource dataSource = DataSource.transactional()) {
			// Sanity scale-guard check that there aren't more than 100 open lobbies.
			if (dataSource.loadAllLobbies().count() > 100) return false;
			long gameId = -1;
			boolean gameOk = false;
			for (int i = 0; i < 15; i++) {
				gameId = (long) (Math.random() * 10000000L);
				try {
					Lobby exists = dataSource.loadLobby(r.gameId);
					continue;
				} catch (EntityNotFoundException expected) {}
				try {
					World exists = dataSource.loadWorld(r.gameId, 1);
					continue;
				} catch (EntityNotFoundException expected) {}
				gameOk = true;
			}
			if (!gameOk) return false;
			if (startLobby.players <= 1 || startLobby.players > 26) return false;
			if (startLobby.minPlayers < 1 || startLobby.minPlayers > startLobby.players) return false;
			if (startLobby.startAtMillis == -1) {
				startLobby.startAtMillis = startLobby.schedule.getNextPeriod(3);
			}
			dataSource.save(Lobby.newLobby(gameId, Rules.LATEST, startLobby.players, startLobby.schedule, startLobby.minPlayers, startLobby.startAtMillis));
			dataSource.commit();
			QueueFactory.getDefaultQueue().add(TaskOptions.Builder.withUrl("/entry/lobbypoll").etaMillis(startLobby.startAtMillis).method(TaskOptions.Method.GET));
		} catch (IOException e) {
			log.log(Level.SEVERE, "IOException Starting Lobby", e);
			return false;
		}
		return true;
	}

	private boolean postOrders(Request r) {
		try (DataSource dataSource = DataSource.transactional()) {
			if (!dataSource.loadPlayer(r.player).checkPassword(r.password)) return false;
			if (r.turn != dataSource.loadCurrentDate(r.gameId)) return false;
			dataSource.save(new Orders(r.gameId, r.player, r.turn, r.version, r.body));
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

	private boolean postNewPlayer(Request r) {
		try (DataSource dataSource = DataSource.transactional()) {
			try {
				dataSource.loadPlayer(r.player);
				return false;
			} catch (EntityNotFoundException expected) {
			}
			dataSource.save(new Player(r.player, r.password));
			dataSource.commit();
			// TODO: do email confirmation.
			return true;
		} catch (IOException e) {
			log.log(Level.SEVERE, "postNewPlayer exception", e);
			return false;
		}
	}

	private boolean postChangePassword(Request r) {
		try (DataSource dataSource = DataSource.transactional()) {
			Player p = dataSource.loadPlayer(r.player);
			if (!p.checkPassword(r.password)) return false;
			p.passwordOtp = null;
			p.setPassword(r.body);
			dataSource.save(p);
			dataSource.commit();
			return true;
		} catch (EntityNotFoundException e) {
			// Pretend no error - don't disclose the existence of the account to the requestor.
			return true;
		} catch (IOException e) {
			log.log(Level.SEVERE, "postNewPlayer exception", e);
			return false;
		}
	}

	private boolean postResetPassword(Request r) {
		try (DataSource dataSource = DataSource.transactional()) {
			Player p = dataSource.loadPlayer(r.player);
			p.passwordOtp = new BigInteger(256, new SecureRandom()).toString(Character.MAX_RADIX);
			p.passwordOtpDeadline = Instant.now().plus(10, ChronoUnit.MINUTES).toEpochMilli();
			dataSource.save(p);
			dataSource.commit();
			mail(r.player, "Empire Password Reset", "The password reset flow has been triggered for your Empire account. In addition to your normal password, you may use the following password to log in to your account:<br/><br/>" + p.passwordOtp + "<br/><br/>The above password and any login sessions associated with it expire ten minutes from now.");
			return true;
		} catch (EntityNotFoundException e) {
			// Pretend no error - don't disclose the existence of the account to the requestor.
			return true;
		} catch (IOException e) {
			log.log(Level.SEVERE, "postResetPassword exception", e);
			return false;
		}
	}

	// TODO: remove
	private boolean migrate(Request rr) {
		final long gameId = 9;
		if (!Player.passesGmPassword(rr.password)) return false;
		try (DataSource dataSource = DataSource.nontransactional()) {
			World w = dataSource.loadWorld(9293657, 1);
			for (String s : w.getNationNames()) w.getNation(s).email = w.getNation(s).email.toLowerCase();
			dataSource.save(w, 9293657);
			dataSource.commit();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception", e);
			return false;
		}
		return true;
	}

	private boolean postSetup(Request r) {
		try (DataSource dataSource = DataSource.transactional()) {
			Player p = dataSource.loadPlayer(r.player);
			if (!p.checkPassword(r.password)) return false;
			Lobby lobby = dataSource.loadLobby(r.gameId);
			Geography geo = Geography.loadGeography(lobby.getRuleSet(), lobby.getNumPlayers());
			NationSetup nation = NationSetup.fromJson(r.body);
			nation.email = p.email;
			if (!geo.getKingdoms().stream().anyMatch(k -> k.name.equals(nation.name))) {
				log.log(Level.WARNING, "postSetup kingdom matching failure for " + r.gameId + ", " + nation.name);
				return false;
			}
			Lobby.UpdateResult result = lobby.update(nation.name, nation);
			if (result == Lobby.UpdateResult.DENIED) {
				log.log(Level.WARNING, "postSetup lobby update failure for " + r.gameId + ", " + nation.name);
				return false;
			}
			dataSource.save(lobby);
			if (result == Lobby.UpdateResult.ADDED) {
				p.activeGames.add(r.gameId);
				dataSource.save(p);
			}
			if (lobby.canStart(Instant.now()) == Lobby.StartResult.START) {
				startWorld(lobby, dataSource);
			}
			dataSource.commit();
		} catch (EntityNotFoundException | IOException ee) {
			log.log(Level.SEVERE, "postSetup Failure for " + r.gameId, ee);
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
			msg.setFrom(new InternetAddress("gilgarn@gmail.com", "Joshua Pawlicki (Empire)"));
			for (String address : addresses) {
				msg.addRecipient(Message.RecipientType.BCC, new InternetAddress(address, "Empire Player"));
			}
			msg.setSubject(subject);
			msg.setText(body);
			Transport.send(msg);
		} catch (MessagingException | UnsupportedEncodingException | NoClassDefFoundError | OverQuotaException e) {
			log.log(Level.WARNING, "Failed to send mail", e);
		}
	}
}
