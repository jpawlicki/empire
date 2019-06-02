package com.empire;

import com.empire.store.DatastoreClient;
import com.empire.store.GaeDatastoreClient;
import com.empire.svc.Player;
import com.empire.svc.LoginCache;
import com.empire.svc.Request;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.common.io.BaseEncoding;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
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
GET /entry/setup?gid=1234&k=Aefoss
  Return customization info for kingdom. If none, empty string.
GET /entry/world?gid=1234&k=Aefoss&password=foobar&t=22
	Check password and return the visible world view JSON.
GET /entry/advancegamepoll
  Advance all games one turn.

POST /entry/orders?gid=1234&k=Aefoss&password=foobar&t=22
	Check password and post the given order data.
POST /entry/setup?gid=1234&k=Aefoss&t=0&password=foobar
	Set customization info for the kingdom, if unset.
POST /entry/advanceworld?gid=1234
	Check cadence and possibly advance world to next step, mail players.
POST /entry/startworld?gid=1234
	Start a new game.

TODO: Will eventually need a changePassword/change-email.
*/

@WebServlet(name = "EntryServlet", value = "/entry/*")
public class EntryServlet extends HttpServlet {
	private static final Logger log = Logger.getLogger(EntryServlet.class.getName());
	private static final String PASSWORD_SALT = "~ Empire_Password Salt ~123`";

	private static final DatastoreClient dsClient = GaeDatastoreClient.getInstance();

  	@Override
  	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		Request r = Request.from(req);
		resp.setHeader("Access-Control-Allow-Origin", "*");
		String json = "";
		if (req.getRequestURI().equals("/entry/orders")) {
			json = getOrders(r, resp);
		} else if (req.getRequestURI().equals("/entry/setup")) {
			json = getSetup(r);
		} else if (req.getRequestURI().equals("/entry/world")) {
			json = getWorld(r);
		} else if (req.getRequestURI().equals("/entry/advanceworldpoll")) {
			json = getAdvancePoll();
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
		if (!checkPassword(r).passesRead()) return null;
		Orders orders = dsClient.getOrders(r.gameId, r.kingdom, r.turn);
		resp.setHeader("SJS-Version", "" + orders.version);
		return GaeDatastoreClient.gson.toJson(orders.orders);
	}

	private String getSetup(Request r) {
		// TODO - should filter this data or display it.
		return dsClient.getNationJson(r.gameId, r.kingdom);
	}

	private String getWorld(Request r) {
		CheckPasswordResult result = checkPassword(r);
		if (!result.passesRead()) {
			return null;
		}

		int worldDate = dsClient.getWorldDate(r.gameId);
		if (worldDate == -1) {
			log.log(Level.INFO, "No such world.");
			return null;
		}
		int date = r.turn != 0 ? r.turn : worldDate;
		World w = dsClient.getWorld(r.gameId, date);
		if (result == CheckPasswordResult.PASS_PLAYER && r.turn == 0) LoginCache.getInstance().recordLogin(r.gameId, date, w.getNation(r.kingdom).email);
		w.filter(r.kingdom);
		return w.toString();
	}

	private String getActivity(Request r) {
		if (checkPassword(r) != CheckPasswordResult.PASS_GM) return null;
		int worldDate = dsClient.getWorldDate(r.gameId);
		if (worldDate == -1) {
			log.log(Level.WARNING, "No such world.");
			return null;
		}
		int date = r.turn != 0 ? r.turn : worldDate;
		HashMap<String, ArrayList<String>> nationEmails = new HashMap<>();
		World w = dsClient.getWorld(r.gameId, date);
		List<String> emails = w.getNationNames().stream().map(s -> w.getNation(s).email).collect(Collectors.toList());
		List<List<Boolean>> actives = LoginCache.getInstance().fetchLoginHistory(r.gameId, date, emails);
		List<Map<String, Boolean>> result = new ArrayList<>();
		for (List<Boolean> turnActives : actives) {
			HashMap<String, Boolean> turn = new HashMap<>();
			for (int i = 0; i < emails.size(); i++) {
				turn.put(emails.get(i), turnActives.get(i));
			}
			result.add(turn);
		}
		return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(result);
	}

	// TODO: single transaction put
	private String getAdvancePoll() {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();

		Set<Long> activeGames = dsClient.getActiveGames();
		if(activeGames == null) {
			log.log(Level.SEVERE, "Poller failed.");
			return null;
		}

		for (Long gameId : activeGames) {
			Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
			try {
				int date = (int)((Long)(service.get(KeyFactory.createKey("CURRENTDATE", "game_" + gameId)).getProperty("date"))).longValue();
				World w = dsClient.getWorld(gameId, date);
				if (w.nextTurn < Instant.now().toEpochMilli()) {
					HashSet<String> kingdoms = new HashSet<>();
					HashMap<String, Map<String, String>> orders = new HashMap<>();
					for (String kingdom : w.getNationNames()) {
						kingdoms.add(kingdom);
						Orders ordersKingdom = dsClient.getOrders(gameId, kingdom, w.date);
						if (ordersKingdom == null) log.warning("Cannot find orders for " + kingdom);
						orders.put(kingdom, ordersKingdom.orders);
					}
					Map<String, String> emails = w.advance(orders);
					dsClient.putWorld(gameId, w);
					Entity nudate = new Entity("CURRENTDATE", "game_" + gameId);
					nudate.setProperty("date", (long)w.date);
					service.put(nudate);
					for (String mail : emails.keySet()) {
						mail(mail, "👑 Empire: Turn Advances", emails.get(mail).replace("%GAMEID%", "" + gameId));
					}
					if (w.gameover) {
						Set<Long> newActiveGames = dsClient.getActiveGames();
						newActiveGames.remove(gameId);
						dsClient.putActiveGames(newActiveGames);
					}
				}
				txn.commit();
			} catch (EntityNotFoundException e) {
				log.log(Level.SEVERE, "World issue.", e);
			} finally {
				if (txn.isActive()) txn.rollback();
			}
		}

		return "";
	}

	// TODO: remove, or check that the request bears the GM password - this is insecure as-is (anyone can advance).
	private boolean postAdvanceWorld(Request r) {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
		HashSet<String> kingdoms = new HashSet<>();
		try {
      World w = dsClient.getWorld(r.gameId, r.turn);
      if(w == null) return false;
			w.nextTurn = 0;
			dsClient.putWorld(r.gameId, w);
			txn.commit();
		} finally {
			if (txn.isActive()) txn.rollback();
		}
		getAdvancePoll();
		return true;
	}

	private boolean postStartWorld(Request r) {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		StartWorldGson s = StartWorldGson.fromJson(r.body);
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
			HashMap<String, Nation> nations = new HashMap<>();
			for (String kingdom : s.kingdoms) {
				log.log(Level.INFO, "Checking kingdom \"" + kingdom + "\"...");
				nations.put(kingdom, dsClient.getNation(r.gameId, kingdom));
				addresses.add(nations.get(kingdom).email);
			}
			World w = World.startNew(passHash, obsPassHash, nations);
			dsClient.putWorld(r.gameId, w);
			Entity g = new Entity("CURRENTDATE", "game_" + r.gameId);
			g.setProperty("date", 1);
			service.put(g);

			Set<Long> activeGames = dsClient.getActiveGames();
			if(activeGames == null) activeGames = new HashSet<>();
			activeGames.add(r.gameId);
			dsClient.putActiveGames(activeGames);
			txn.commit();
		} finally {
			if (txn.isActive()) txn.rollback();
		}
		mail(addresses, "👑 Empire: Game Begins", "A game of Empire that you are playing in has started! You can make your orders for the first turn at http://pawlicki.kaelri.com/empire/map1.html?gid=" + r.gameId + ".");
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
	private CheckPasswordResult checkPassword(Request r) {
		try {
			if (r.password == null) return CheckPasswordResult.FAIL;
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] attemptHash = digest.digest((PASSWORD_SALT + r.password).getBytes(StandardCharsets.UTF_8));
			int date = dsClient.getWorldDate(r.gameId);
      World w = dsClient.getWorld(r.gameId, date);
      if(date == -1 || w == null) {
				log.log(Level.INFO, "No world for " + r.gameId + ", " + r.kingdom);
				return CheckPasswordResult.NO_ENTITY;
			}

			byte[] gmPassHash = BaseEncoding.base16().decode(w.gmPasswordHash);
			byte[] obsPassHash = BaseEncoding.base16().decode(w.obsPasswordHash);
			if (w.getNationNames().contains(r.kingdom) && Arrays.equals(attemptHash, BaseEncoding.base16().decode(dsClient.getPlayer(w.getNation(r.kingdom).email).passHash))) return CheckPasswordResult.PASS_PLAYER;
			if (Arrays.equals(attemptHash, gmPassHash)) return CheckPasswordResult.PASS_GM;
			if (Arrays.equals(attemptHash, obsPassHash)) return CheckPasswordResult.PASS_OBS;
			return CheckPasswordResult.FAIL;
		} catch (NoSuchAlgorithmException e) {
			log.log(Level.SEVERE, "CheckPassword Failure", e);
			return CheckPasswordResult.FAIL;
		}
	}

	// TODO: better json conversion
	private boolean postOrders(Request r) {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
		try {
			if (!checkPassword(r).passesWrite()) return false;
			int worldDate = dsClient.getWorldDate(r.gameId);
			if (worldDate == -1) {
				log.log(Level.WARNING, "No current turn for " + r.gameId + ".");
				return false;
			}
			if (r.turn != worldDate) return false;
			Type t = new TypeToken<Map<String, String>>(){}.getType();
			Map<String, String> orders = GaeDatastoreClient.gson.fromJson(r.body, t);
			dsClient.putOrders(new Orders(r.gameId, r.kingdom, r.turn, orders, r.version));
			txn.commit();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
		return true;
	}

	private boolean postRealTimeCommunication(Request r) {
		if (!checkPassword(r).passesWrite()) return false;
		World w = dsClient.getWorld(r.gameId, r.turn);

		if (w == null) {
			log.log(Level.INFO, "Not found for " + r.gameId + ", " + r.kingdom);
			return true;
		}

		com.empire.Message msg = GaeDatastoreClient.gson.fromJson(r.body, com.empire.Message.class);
		w.addRtc(r.kingdom, msg);
		dsClient.putWorld(r.gameId, w);

		return true;
	}

	private static final class ChangePlayerRequestBody {
		public String email;
		public String password;
	}

	private boolean postChangePlayer(Request r) {
		if (!checkPassword(r).passesWrite()) return false;
		int worldDate = dsClient.getWorldDate(r.gameId);
		if(worldDate == -1) log.log(Level.INFO, "Not found for " + r.gameId + ", " + r.kingdom);
		int date = r.turn != 0 ? r.turn : worldDate;
		World w = dsClient.getWorld(r.gameId, date);
		ChangePlayerRequestBody body = new GsonBuilder().create().fromJson(r.body, ChangePlayerRequestBody.class);
		Player p = dsClient.getPlayer(body.email);
		w.getNation(r.kingdom).email = body.email;
		w.getNation(r.kingdom).password = p.passHash;
		dsClient.putWorld(r.gameId, w);
		return true;
	}

	// TODO: remove
	private boolean migrate(Request rr) {
		World w = dsClient.getWorld(4, dsClient.getWorldDate(4));

		if (w == null) {
			log.log(Level.INFO, "Not found!");
			return true;
		}

		for (Character c : w.characters) if (c.name.equals("Ea Rjinkuki")) c.location = 101;
		dsClient.putWorld(4, w);
		return true;
	}


	//TODO: all puts in single transaction: Nation, Player
	private boolean postSetup(Request r) {
		Nation nation = dsClient.getNation(r.gameId, r.kingdom);
		if (nation == null) return false; // We expect nation to not be found

		nation = GaeDatastoreClient.gson.fromJson(r.body, Nation.class);

		try {
			nation.password = BaseEncoding.base16().encode(MessageDigest.getInstance("SHA-256").digest((PASSWORD_SALT + nation.password).getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e){
			log.log(Level.SEVERE, "postSetup Failure", e);
			return false;
		}

		dsClient.putNation(r.gameId, r.kingdom, nation);
		dsClient.putPlayer(new Player(nation.email, nation.password));
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
		} catch (MessagingException | UnsupportedEncodingException | NoClassDefFoundError e) {
			log.log(Level.SEVERE, "Failed to send mail", e);
		}
	}
}
