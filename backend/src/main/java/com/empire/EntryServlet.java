package com.empire;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.common.io.BaseEncoding;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
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
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		if (!checkPassword(r, service).passesRead()) return null;
		try {
			Order o = Order.loadOrder(r.gameId, r.kingdom, r.turn, DatastoreServiceFactory.getDatastoreService());
			resp.setHeader("SJS-Version", "" + o.version);
			return o.json;
		} catch (EntityNotFoundException e) {
			return null;
		}
	}

	private String getSetup(Request r) {
		// TODO - should filter this data or display it.
		try {
			return Nation.NationGson.loadJson(r.kingdom, r.gameId, DatastoreServiceFactory.getDatastoreService());
		} catch (EntityNotFoundException e) {
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
			int date = r.turn != 0 ? r.turn : getWorldDate(r.gameId, service);
			World w = World.load(r.gameId, date, service);
			if (result == CheckPasswordResult.PASS_PLAYER && r.turn == 0) LoginCache.getSingleton().recordLogin(r.gameId, date, w.kingdoms.get(r.kingdom).email, service);
			w.filter(r.kingdom);
			return w.toString();
		} catch (EntityNotFoundException e) {
			log.log(Level.INFO, "No such world.");
			return null;
		}
	}

	private String getActivity(Request r) {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		if (checkPassword(r, service) != CheckPasswordResult.PASS_GM) return null;
		try {
			int date = r.turn != 0 ? r.turn : getWorldDate(r.gameId, service);
			HashMap<String, List<String>> nationEmails = new HashMap<>();
			for (Map.Entry<String, NationData> kingdom : World.load(r.gameId, date, service).kingdoms.entrySet()) {
				if (nationEmails.containsKey(kingdom.getValue().email)) {
					nationEmails.get(kingdom.getValue().email).add(kingdom.getKey());
				} else {
					ArrayList<String> kingdoms = new ArrayList<>();
					kingdoms.add(kingdom.getKey());
					nationEmails.put(kingdom.getValue().email, kingdoms);
				}
			}
			ArrayList<String> emails = new ArrayList<String>(nationEmails.keySet());
			List<List<Boolean>> actives = LoginCache.getSingleton().fetchLoginHistory(r.gameId, date, emails, service);
			List<Map<String, Boolean>> result = new ArrayList<>();
			for (List<Boolean> turnActives : actives) {
				HashMap<String, Boolean> turn = new HashMap<>();
				for (int i = 0; i < emails.size(); i++) for (String nation : nationEmails.get(emails.get(i))) {
					turn.put(nation, turnActives.get(i));
				}
				result.add(turn);
			}
			return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(result);
		} catch (EntityNotFoundException e) {
			log.log(Level.WARNING, "No such world.", e);
			return null;
		}
	}

	private static class ActiveGames {
		public ArrayList<Long> activeGameIds;
		static ActiveGames fromGson(String s) {
			return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().fromJson(s, ActiveGames.class);
		}
	}

	private String getAdvancePoll() {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		try {
			for (Long gameId : ActiveGames.fromGson((String)service.get(KeyFactory.createKey("ACTIVEGAMES", "_")).getProperty("active_games")).activeGameIds) {
				Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
				try {
					int date = (int)((Long)(service.get(KeyFactory.createKey("CURRENTDATE", "game_" + gameId)).getProperty("date"))).longValue();
					World	w = World.load(gameId, date, service);
					if (w.nextTurn < Instant.now().toEpochMilli()) {
						HashSet<String> kingdoms = new HashSet<>();
						HashMap<String, Map<String, String>> orders = new HashMap<>();
						for (String kingdom : w.kingdoms.keySet()) {
							kingdoms.add(kingdom);
							try {
								orders.put(kingdom, Order.loadOrder(gameId, kingdom, w.date, service).getOrders());
							} catch (EntityNotFoundException e) {
								// Can't load the relevant orders - tool will make default orders.
							}
						}
						Map<String, String> emails = w.advance(orders);
						service.put(w.toEntity(gameId));
						Entity nudate = new Entity("CURRENTDATE", "game_" + gameId);
						nudate.setProperty("date", (long)w.date);
						service.put(nudate);
						for (String mail : emails.keySet()) {
							mail(mail, "👑 Empire: Turn Advances", emails.get(mail).replace("%GAMEID%", "" + gameId));
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
		}
		return "";
	}

	// TODO: remove.
	private boolean postAdvanceWorld(Request r) {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
		HashSet<String> kingdoms = new HashSet<>();
		try {
			World	w = World.load(r.gameId, r.turn, service);
			HashMap<String, Map<String, String>> orders = new HashMap<>();
			for (String kingdom : w.kingdoms.keySet()) {
				kingdoms.add(kingdom);
				try {
					orders.put(kingdom, Order.loadOrder(r.gameId, kingdom, w.date, service).getOrders());
				} catch (EntityNotFoundException e) {
					// Can't load the relevant orders - tool will make default orders.
				}
			}
			w.advance(orders);
			service.put(w.toEntity(r.gameId));
			Entity date = new Entity("CURRENTDATE", "game_" + r.gameId);
			date.setProperty("date", (long)w.date);
			service.put(date);
			txn.commit();
		} catch (EntityNotFoundException e) {
			return false;
		} finally {
			if (txn.isActive()) txn.rollback();
		}
		// Mail
		if (r.skipMail) return true;
		txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
		HashSet<String> addresses = new HashSet<>();
		try {
			for (String kingdom : kingdoms) {
				try {
					addresses.add(Nation.NationGson.loadNation(kingdom, r.gameId, service).email);
				} catch (EntityNotFoundException e) {
					log.log(Level.SEVERE, "Failed to find mailing address for " + kingdom + " (game " + r.gameId + ")", e);
				}
			}
		} finally {
			if (txn.isActive()) txn.rollback();
		}
		mail(addresses, "👑 Empire: Turn Advances", "A game of Empire that you are playing in has advanced to turn " + (r.turn + 1) + ". You can issue your orders at https://pawlicki.kaelri.com/empire/map1.html?g=" + r.gameId + ".");
		return true;
	}

	private boolean postStartWorld(Request r) {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		StartWorldGson s = StartWorldGson.fromJson(r.body);
		String passHash;
		try {
			passHash = BaseEncoding.base16().encode(MessageDigest.getInstance("SHA-256").digest((PASSWORD_SALT + s.gmPassword).getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			log.log(Level.SEVERE, "Hash error", e);
			return false;
		}
		Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
		HashSet<String> addresses = new HashSet<String>();
		try {
			// Collect setups.
			HashMap<String, Nation.NationGson> nations = new HashMap<>();
			for (String kingdom : s.kingdoms) {
				log.log(Level.INFO, "Checking kingdom \"" + kingdom + "\"...");
				try {
					nations.put(kingdom, Nation.NationGson.loadNation(kingdom, r.gameId, service));
					addresses.add(nations.get(kingdom).email);
				} catch (EntityNotFoundException e) {
					// Nation is not in the game.
				}
			}
			World w = World.startNew(passHash, nations);
			service.put(w.toEntity(r.gameId));
			Entity g = new Entity("CURRENTDATE", "game_" + r.gameId);
			g.setProperty("date", 1);
			service.put(g);
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
	private CheckPasswordResult checkPassword(Request r, DatastoreService service) {
		try {
			if (r.password == null) return CheckPasswordResult.FAIL;
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] attemptHash = digest.digest((PASSWORD_SALT + r.password).getBytes(StandardCharsets.UTF_8));
			// try {
			// 	byte[] passHash = BaseEncoding.base16().decode(Nation.NationGson.loadNation(r.kingdom, r.gameId, service).password);
			// 	if (Arrays.equals(attemptHash, passHash)) return CheckPasswordResult.PASS;
			// } catch (EntityNotFoundException e) {
			// 	// Do nothing.
			// }
			int date = getWorldDate(r.gameId, service);
			World w = World.load(r.gameId, date, service);
			byte[] gmPassHash = BaseEncoding.base16().decode(w.gmPasswordHash);
			byte[] obsPassHash = BaseEncoding.base16().decode(w.obsPasswordHash);
			if (w.kingdoms.containsKey(r.kingdom) && w.kingdoms.get(r.kingdom).accessToken.equals(r.password)) return CheckPasswordResult.PASS_PLAYER;
			if (w.kingdoms.containsKey(r.kingdom) && Arrays.equals(attemptHash, BaseEncoding.base16().decode(w.kingdoms.get(r.kingdom).password))) return CheckPasswordResult.PASS_PLAYER;
			if (w.kingdoms.containsKey(r.kingdom) && Arrays.equals(attemptHash, BaseEncoding.base16().decode(Player.loadPlayer(w.kingdoms.get(r.kingdom).email, service).passHash))) return CheckPasswordResult.PASS_PLAYER;
			if (Arrays.equals(attemptHash, gmPassHash)) return CheckPasswordResult.PASS_GM;
			if (Arrays.equals(attemptHash, obsPassHash)) return CheckPasswordResult.PASS_OBS;
			return CheckPasswordResult.FAIL;
		} catch (EntityNotFoundException e) {
			log.log(Level.INFO, "No world for " + r.gameId + ", " + r.kingdom);
			return CheckPasswordResult.NO_ENTITY;
		} catch (NoSuchAlgorithmException e) {
			log.log(Level.SEVERE, "CheckPassword Failure", e);
			return CheckPasswordResult.FAIL;
		}
	}

	private boolean postOrders(Request r) {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
		try {
			if (!checkPassword(r, service).passesWrite()) return false;
			if (r.turn != getWorldDate(r.gameId, service)) return false;
			service.put(new Order(r.gameId, r.kingdom, r.turn, r.version, r.body).toEntity());
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
			int date = r.turn != 0 ? r.turn : getWorldDate(r.gameId, service);
			World w = World.load(r.gameId, date, service);
			ChangePlayerRequestBody body = new GsonBuilder().create().fromJson(r.body, ChangePlayerRequestBody.class);
			w.kingdoms.get(r.kingdom).email = body.email;
			w.kingdoms.get(r.kingdom).password = BaseEncoding.base16().encode(MessageDigest.getInstance("SHA-256").digest((PASSWORD_SALT + body.password).getBytes(StandardCharsets.UTF_8)));
			service.put(w.toEntity(r.gameId));
			txn.commit();
		} catch (NoSuchAlgorithmException e) {
			log.log(Level.SEVERE, "Hash error", e);
			return false;
		} catch (EntityNotFoundException e) {
			log.log(Level.INFO, "Not found for " + r.gameId + ", " + r.kingdom, e);
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
		return true;
	}

	// TODO: remove
	private boolean migrate(Request r) {
		final int[] gameIds = new int[]{2, 3};
		final HashMap<String, String> playerPasswords = new HashMap<>();
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
		try {
			for (int i : gameIds) {
				World w = World.load(i, 14, service);
				for (String kingdom : w.kingdoms.keySet()) {
					playerPasswords.put(w.kingdoms.get(kingdom).email, w.kingdoms.get(kingdom).password);
				}
			}
			txn.commit();
			for (String email : playerPasswords.keySet()) {
				txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
				Player p = new Player(email, playerPasswords.get(email));
				service.put(p.toEntity());
				txn.commit();
			}
		} catch (EntityNotFoundException e) {
			log.log(Level.INFO, "Not found for " + r.gameId + ", " + r.kingdom, e);
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
		return true;
	}


	private boolean postSetup(Request r) {
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = service.beginTransaction();
		try {
			Nation.NationGson.loadNation(r.kingdom, r.gameId, service);
			return false; // We expect an EntityNotFoundException.
		} catch (EntityNotFoundException e) {
			try {
				Nation.NationGson nation = Nation.NationGson.fromJson(r.body);
				nation.password = BaseEncoding.base16().encode(MessageDigest.getInstance("SHA-256").digest((PASSWORD_SALT + nation.password).getBytes(StandardCharsets.UTF_8)));
				service.put(nation.toEntity(r.kingdom, r.gameId));
				txn.commit();
			} catch (NoSuchAlgorithmException ee) {
				log.log(Level.SEVERE, "postSetup Failure", ee);
				return false;
			}
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
		} catch (MessagingException | UnsupportedEncodingException e) {
			log.log(Level.SEVERE, "Failed to send mail", e);
		}
	}
}
