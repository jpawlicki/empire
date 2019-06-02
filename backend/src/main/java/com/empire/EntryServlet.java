package com.empire;

import com.empire.store.DatastoreClient;
import com.empire.store.GaeDatastoreClient;
import com.empire.store.MultiPutRequest;
import com.empire.svc.LoginCache;
import com.empire.svc.Player;
import com.empire.svc.Request;
import com.google.common.io.BaseEncoding;
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

		String json;

		switch(req.getRequestURI()){
			case "/entry/orders":
				json = getOrders(r, resp);
				break;
			case "/entry/setup":
				json = getSetup(r);
				break;
			case "/entry/world":
				json = getWorld(r);
				break;
			case "/entry/advanceworldpoll":
				json = getAdvancePoll();
				break;
			case "/entry/activity":
				json = getActivity(r);
				break;
			default:
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

  // TODO: Does this method need to write the response to OutputStream like doGet does?
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.addHeader("Access-Control-Allow-Origin", "*");
		resp.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		Request r = Request.from(req);

		boolean success = false;
		String err;

		switch(req.getRequestURI()){
			case "/entry/orders":
				success = postOrders(r);
				err = "Failure.";
				break;
			case "/entry/advanceworld":
				success = postAdvanceWorld(r);
				err = "Failure.";
				break;
			case "/entry/startworld":
				success = postStartWorld(r);
				err = "Failure.";
				break;
			case "/entry/setup":
				success = postSetup(r);
				err = "Not allowed.";
				break;
			case "/entry/rtc":
				success = postRealTimeCommunication(r);
				err = "Not allowed.";
				break;
			case "/entry/changeplayer":
				success = postChangePlayer(r);
				err = "Not allowed.";
				break;
			case "/entry/migrate":
				success = migrate(r);
				err = "Failure.";
				break;
			default:
				err = "No such path.";
				break;
		}

		if (success) {
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
		Optional<Orders> orders = dsClient.getOrders(r.gameId, r.kingdom, r.turn);

		if(orders.isPresent()) {
			resp.setHeader("SJS-Version", "" + orders.get().version);
			return GaeDatastoreClient.gson.toJson(orders.get().orders);
		} else {
			log.severe("Unable to get orders");
			return null;
		}
	}

	private String getSetup(Request r) {
		// TODO - should filter this data or display it.
		Optional<Nation> nation = dsClient.getNation(r.gameId, r.kingdom);

		if(nation.isPresent()) {
			return GaeDatastoreClient.gson.toJson(nation.get());
		} else {
			log.severe("Unable to complete setup request");
			return null;
		}
	}

	private String getWorld(Request r) {
		CheckPasswordResult result = checkPassword(r);
		if (!result.passesRead()) return null;

		Optional<Integer> dateOpt = dsClient.getWorldDate(r.gameId);

		if (!dateOpt.isPresent()) {
			log.severe("No such world.");
			return null;
		}

		int date = r.turn != 0 ? r.turn : dateOpt.get();
		Optional<World> worldOpt = dsClient.getWorld(r.gameId, date);

		if(!worldOpt.isPresent()) return null;
		World w = worldOpt.get();
		if (result == CheckPasswordResult.PASS_PLAYER && r.turn == 0) LoginCache.getInstance().recordLogin(r.gameId, date, w.getNation(r.kingdom).email);
		w.filter(r.kingdom);

		return GaeDatastoreClient.gson.toJson(w);
	}

	private String getAdvancePoll() {
		Optional<Set<Long>> activeGamesOpt = dsClient.getActiveGames();

		if(!activeGamesOpt.isPresent()) {
			log.log(Level.SEVERE, "Poller failed.");
			return null;
		}

		for (Long gameId : activeGamesOpt.get()) {
			Optional<Integer> dateOpt = dsClient.getWorldDate(gameId);
			Optional<World> worldOpt = dsClient.getWorld(gameId, dateOpt.orElse(-1));

			if(!(dateOpt.isPresent() && worldOpt.isPresent())) {
				log.log(Level.SEVERE, "World issue.");
				return null;
			}

			World w = worldOpt.get();

			if (w.nextTurn >= Instant.now().toEpochMilli()) continue;

			Map<String, Map<String, String>> orders = new HashMap<>();
			for (String kingdom : w.getNationNames()) {
				Optional<Orders> ordersKingdom = dsClient.getOrders(gameId, kingdom, w.date);

				ordersKingdom.ifPresent(o -> orders.put(kingdom, o.orders));

				if(ordersKingdom.isPresent()) {
					orders.put(kingdom, ordersKingdom.get().orders);
				} else {
					log.warning("Cannot find orders for " + kingdom);
					orders.put(kingdom,  null);
				}
			}

			Map<String, String> emails = w.advance(orders);

			MultiPutRequest mp = MultiPutRequest.create()
					.addWorld(gameId, w)
					.addWorldDate(gameId, w.date);

			if (w.gameover) {
				Set<Long> newActiveGames = dsClient.getActiveGames().orElse(new HashSet<>());
				newActiveGames.remove(gameId);
				mp.addActiveGames(newActiveGames);
			}

			boolean response = mp.put(dsClient);

			if(response) {
				emails.keySet().forEach(m -> mail(m, "👑 Empire: Turn Advances", emails.get(m).replace("%GAMEID%", "" + gameId)));
			}
		}

		return "";
	}

	private String getActivity(Request r) {
		if (checkPassword(r) != CheckPasswordResult.PASS_GM) return null;

		Optional<Integer> dateOpt = dsClient.getWorldDate(r.gameId);
		if (!dateOpt.isPresent()) {
			log.log(Level.WARNING, "No such world.");
			return null;
		}
		int date = r.turn != 0 ? r.turn : dateOpt.get();

		Optional<World> worldOpt = dsClient.getWorld(r.gameId, date);
		if(!worldOpt.isPresent()) return null;
		World w = worldOpt.get();

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
		return GaeDatastoreClient.gson.toJson(result);
	}

	// TODO: better json conversion
	private boolean postOrders(Request r) {
		if (!checkPassword(r).passesWrite()) return false;
		Optional<Integer> dateOpt = dsClient.getWorldDate(r.gameId);

		if (!dateOpt.isPresent()) {
			log.log(Level.WARNING, "No current turn for " + r.gameId + ".");
			return false;
		}

		if (r.turn != dateOpt.get()) return false;
		Type t = new TypeToken<Map<String, String>>(){}.getType();
		Map<String, String> orders = GaeDatastoreClient.gson.fromJson(r.body, t);
		return dsClient.putOrders(new Orders(r.gameId, r.kingdom, r.turn, orders, r.version));
	}

	// TODO: remove, or check that the request bears the GM password - this is insecure as-is (anyone can advance).
	private boolean postAdvanceWorld(Request r) {
		Optional<World> worldOpt = dsClient.getWorld(r.gameId, r.turn);
		if(!worldOpt.isPresent()) return false;

		World w = worldOpt.get();
		w.nextTurn = 0;
		dsClient.putWorld(r.gameId, w);
		getAdvancePoll();
		return true;
	}

	private boolean postStartWorld(Request r) {
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

		// Collect setups.
		Set<String> addresses = new HashSet<>();
		Map<String, Nation> nations = new HashMap<>();

		for (String kingdom : s.kingdoms) {
			log.log(Level.INFO, "Checking kingdom \"" + kingdom + "\"...");
			Optional<Nation> nation = dsClient.getNation(r.gameId, kingdom);
			if(nation.isPresent()){
				nations.put(kingdom, nation.get());
				addresses.add(nation.get().email);
			} else {
				log.severe(kingdom + " was not added to nation setups");
			}
		}

		World w = World.startNew(passHash, obsPassHash, nations);
		Set<Long> activeGames = dsClient.getActiveGames().orElse(new HashSet<>());
		activeGames.add(r.gameId);

		boolean response = MultiPutRequest.create()
				.addWorld(r.gameId, w)
				.addWorldDate(r.gameId, 1)
				.addActiveGames(activeGames)
				.put(dsClient);

		if(response) {
			mail(addresses, "👑 Empire: Game Begins", "A game of Empire that you are playing in has started! You can make your orders for the first turn at http://pawlicki.kaelri.com/empire/map1.html?gid=" + r.gameId + ".");
		}

		return response;
	}

	private boolean postSetup(Request r) {
		Optional<Nation> nationCheck = dsClient.getNation(r.gameId, r.kingdom);

		if (!nationCheck.isPresent()) return false; // We expect nation to not be found

		Nation nation = GaeDatastoreClient.gson.fromJson(r.body, Nation.class);

		try {
			nation.password = BaseEncoding.base16().encode(MessageDigest.getInstance("SHA-256").digest((PASSWORD_SALT + nation.password).getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e){
			log.log(Level.SEVERE, "postSetup Failure", e);
			return false;
		}

		return MultiPutRequest.create()
				.addNation(r.gameId, r.kingdom, nation)
				.addPlayer(new Player(nation.email, nation.password))
				.put(dsClient);
	}

	private boolean postRealTimeCommunication(Request r) {
		if (!checkPassword(r).passesWrite()) return false;

		Optional<World> worldOpt = dsClient.getWorld(r.gameId, r.turn);
		if (!worldOpt.isPresent()) {
			log.log(Level.INFO, "Not found for " + r.gameId + ", " + r.kingdom);
			return true;
		}

		World w = worldOpt.get();

		com.empire.Message msg = GaeDatastoreClient.gson.fromJson(r.body, com.empire.Message.class);
		w.addRtc(r.kingdom, msg);
		dsClient.putWorld(r.gameId, w);

		return true;
	}

	private boolean postChangePlayer(Request r) {
		if (!checkPassword(r).passesWrite()) return false;

		Optional<Integer> dateOpt = dsClient.getWorldDate(r.gameId);
		int date = r.turn != 0 ? r.turn : dateOpt.orElse(-1);
		Optional<World> worldOpt = dsClient.getWorld(r.gameId, date);

		if(!(dateOpt.isPresent() && worldOpt.isPresent())) {
			log.log(Level.INFO, "Not found for " + r.gameId + ", " + r.kingdom);
			return false;
		}

		World w = worldOpt.get();
		ChangePlayerRequestBody body = new GsonBuilder().create().fromJson(r.body, ChangePlayerRequestBody.class);
		Optional<Player> p = dsClient.getPlayer(body.email);

		if(!p.isPresent()) {
			log.severe("Player not found, request to change player failed");
			return false;
		}

		w.getNation(r.kingdom).email = body.email;
		w.getNation(r.kingdom).password = p.get().passHash;
		dsClient.putWorld(r.gameId, w);
		return true;
	}

	private static final class ChangePlayerRequestBody {
		public String email;
		public String password;
	}

	// TODO: remove
	private boolean migrate(Request r) {
		Optional<World> worldOpt = dsClient.getWorld(4, dsClient.getWorldDate(4).orElse(-1));

		if (!worldOpt.isPresent()) {
			log.log(Level.INFO, "Not found!");
			return true;
		}

		World w = worldOpt.get();

		for (Character c : w.characters) if (c.name.equals("Ea Rjinkuki")) c.location = 101;
		dsClient.putWorld(4, w);
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

		CheckPasswordResult(boolean passesRead, boolean passesWrite) {
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

			Optional<Integer> dateOpt = dsClient.getWorldDate(r.gameId);
			Optional<World> worldOpt = dsClient.getWorld(r.gameId, dateOpt.orElse(-1));
			if(!(dateOpt.isPresent() & worldOpt.isPresent())) {
				log.log(Level.INFO, "No world for " + r.gameId + ", " + r.kingdom);
				return CheckPasswordResult.NO_ENTITY;
			}
			World w = worldOpt.get();

			byte[] gmPassHash = BaseEncoding.base16().decode(w.gmPasswordHash);
			byte[] obsPassHash = BaseEncoding.base16().decode(w.obsPasswordHash);
			Optional<Player> player = dsClient.getPlayer(w.getNation(r.kingdom).email);
			if(!player.isPresent()) {
				log.severe("Player not found, password check failed");
				return CheckPasswordResult.NO_ENTITY;
			}

			if (w.getNationNames().contains(r.kingdom) && Arrays.equals(attemptHash, BaseEncoding.base16().decode(player.get().passHash))) return CheckPasswordResult.PASS_PLAYER;
			if (Arrays.equals(attemptHash, gmPassHash)) return CheckPasswordResult.PASS_GM;
			if (Arrays.equals(attemptHash, obsPassHash)) return CheckPasswordResult.PASS_OBS;
			return CheckPasswordResult.FAIL;
		} catch (NoSuchAlgorithmException e) {
			log.log(Level.SEVERE, "CheckPassword Failure", e);
			return CheckPasswordResult.FAIL;
		}
	}

	private void mail(String address, String subject, String body) {
		HashSet<String> s = new HashSet<>();
		s.add(address);
		mail(s, subject, body);
	}

	private void mail(Set<String> addresses, String subject, String body) {
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
