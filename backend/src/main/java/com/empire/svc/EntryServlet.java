package com.empire.svc;

import com.empire.Nation;
import com.empire.Orders;
import com.empire.StartWorld;
import com.empire.World;
import com.empire.store.DatastoreClient;
import com.empire.store.GaeDatastoreClient;
import com.empire.store.MultiPutRequest;
import com.empire.util.JsonUtils;
import com.google.common.io.BaseEncoding;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

// TODO: Unit tests for EntryServlet

@WebServlet(name = "EntryServlet", value = "/entry/*")
public class EntryServlet extends HttpServlet {
	private static final Logger log = Logger.getLogger(EntryServlet.class.getName());
	private static final String PASSWORD_SALT = "~ Empire_Password Salt ~123`";

	private final DatastoreClient dsClient;
	private final LoginCache cache;

	/* This constructor is needed in order to start the service with GoogleAppEngine */
	public EntryServlet(){
		this(GaeDatastoreClient.getInstance(), LoginCache.getInstance());
	}

	/* This constructor is needed to enable testing */
	EntryServlet(DatastoreClient dsClient, LoginCache cache){
		super();
		this.dsClient = dsClient;
		this.cache = cache;
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		Request r = RequestFactory.from(req);
		resp.setHeader("Access-Control-Allow-Origin", "*");

		String json;

		switch(req.getRequestURI()) {
			case "/entry/ping":
				json = "{\"status\": \"success\"}";
				break;
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
		Request r = RequestFactory.from(req);

		boolean success = false;
		String err;

		switch(req.getRequestURI()) {
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
			/*case "/entry/migrate":
				success = migrate(r);
				err = "Failure.";
				break;*/
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
		Optional<Orders> orders = dsClient.getOrders(r.getGameId(), r.getKingdom(), r.getTurn());

		if(orders.isPresent()) {
			resp.setHeader("SJS-Version", String.valueOf(orders.get().version));
			return JsonUtils.toJson(orders.get().getOrders());
		} else {
			log.severe("Unable to get orders for gameId=" + r.getGameId() + ", kingdom=" + r.getKingdom() + ", turn=" + r.getTurn());
			return null;
		}
	}

	// TODO - should filter this data or display it.
	private String getSetup(Request r) {
		Optional<Nation> nation = dsClient.getNation(r.getGameId(), r.getKingdom());

		if(nation.isPresent()) {
			return JsonUtils.toJson(nation.get());
		} else {
			log.severe("Unable to complete setup request for gameId=" + r.getGameId() + ", kingdom=" + r.getKingdom());
			return null;
		}
	}

	private String getWorld(Request r) {
		PasswordCheck result = checkPassword(r);
		if (!result.passesRead()) return null;

		Optional<Integer> dateOpt = dsClient.getWorldDate(r.getGameId());

		if (!dateOpt.isPresent()) {
			log.severe("Unable to retrieve date for gameId=" + r.getGameId());
			return null;
		}

		int date = r.getTurn() != 0 ? r.getTurn() : dateOpt.get();
		Optional<World> worldOpt = dsClient.getWorld(r.getGameId(), date);

		if(!worldOpt.isPresent()) {
			log.severe("Unable to retrieve world for gameId=" + r.getGameId() + ", turn=" + date);
			return null;
		}

		World w = worldOpt.get();
		if (result == PasswordCheck.PASS_PLAYER && r.getTurn() == 0) cache.recordLogin(r.getGameId(), date, w.getNation(r.getKingdom()).email);
		w.filter(r.getKingdom());

		return JsonUtils.toJson(w);
	}

	private String getAdvancePoll() {
		Optional<Set<Long>> activeGamesOpt = dsClient.getActiveGames();

		if(!activeGamesOpt.isPresent()) {
			log.severe("Poller failed, could not retrieve active games");
			return null;
		}

		for (Long gameId : activeGamesOpt.get()) {
			Optional<World> worldOpt = dsClient.getWorld(gameId, dsClient.getWorldDate(gameId).orElse(-1));

			if(!worldOpt.isPresent()) {
				log.severe("Poller failed, unable to retrieve current world for gameId=" + gameId);
				return null;
			}

			World w = worldOpt.get();

			if (w.nextTurn >= Instant.now().toEpochMilli()) continue;

			Map<String, Map<String, String>> orders = new HashMap<>();
			for (String kingdom : w.getNationNames()) {
				Optional<Orders> ordersKingdom = dsClient.getOrders(gameId, kingdom, w.date);

				if(ordersKingdom.isPresent()) {
					orders.put(kingdom, ordersKingdom.get().getOrders());
				} else {
					log.warning("Cannot find orders for gameId=" + gameId + ", kingdom=" + kingdom + ", turn=" + w.date);
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
				emails.forEach((k, v) -> mail(k, "👑 Empire: Turn Advances", v.replace("%GAMEID%", "" + gameId)));
			}
		}

		return "";
	}

	private String getActivity(Request r) {
		if (checkPassword(r) != PasswordCheck.PASS_GM) return null;

		Optional<Integer> dateOpt = dsClient.getWorldDate(r.getGameId());

		if (!dateOpt.isPresent()) {
			log.warning("Unable to retrieve date for gameId=" + r.getGameId());
			return null;
		}

		int date = r.getTurn() != 0 ? r.getTurn() : dateOpt.get();
		Optional<World> worldOpt = dsClient.getWorld(r.getGameId(), date);

		if(!worldOpt.isPresent()) {
			log.severe("Unable to retrieve world for gameId=" + r.getGameId() + ", turn=" + date);
			return null;
		}

		World w = worldOpt.get();

		List<String> emails = w.getNationNames().stream().map(s -> w.getNation(s).email).collect(Collectors.toList());
		List<List<Boolean>> actives = cache.fetchLoginHistory(r.getGameId(), date, emails);
		List<Map<String, Boolean>> result = actives.stream()
				.map(a -> IntStream.range(0, emails.size()).boxed().collect(Collectors.toMap(emails::get, a::get)))
				.collect(Collectors.toList());

		return JsonUtils.toJson(result);
	}

	private boolean postOrders(Request r) {
		if (!checkPassword(r).passesWrite()) return false;
		Optional<Integer> dateOpt = dsClient.getWorldDate(r.getGameId());

		if (!dateOpt.isPresent()) {
			log.severe("Unable to retrieve date for gameId=" + r.getGameId());
			return false;
		}

		if (r.getTurn() != dateOpt.get()) return false;

		Map<String, String> orders = JsonUtils.fromJson(r.getBody(), new TypeToken<Map<String, String>>(){}.getType());
		return dsClient.putOrders(new Orders(r.getGameId(), r.getKingdom(), r.getTurn(), orders, r.getVersion()));
	}

	// TODO: remove, or check that the request bears the GM password - this is insecure as-is (anyone can advance).
	private boolean postAdvanceWorld(Request r) {
		Optional<World> worldOpt = dsClient.getWorld(r.getGameId(), r.getTurn());

		if(!worldOpt.isPresent()) {
			log.severe("Unable to retrieve world for gameId=" + r.getGameId() + ", turn=" + r.getTurn());
			return false;
		}

		World w = worldOpt.get();
		w.nextTurn = 0;
		dsClient.putWorld(r.getGameId(), w);
		getAdvancePoll();
		return true;
	}

	private boolean postStartWorld(Request r) {
		StartWorld s = JsonUtils.fromJson(r.getBody(), StartWorld.class);
		String passHash = encodePassword(s.gmPassword);
		String obsPassHash = encodePassword(s.obsPassword);

		if (Objects.isNull(passHash) || Objects.isNull(obsPassHash)) {
			log.severe("Error while encoding GM and/or observer passwords, unable to start game (gameId=" + r.getGameId() + ")");
			return false;
		}

		// Collect setups.
		Set<String> addresses = new HashSet<>();
		Map<String, Nation> nations = new HashMap<>();

		for (String kingdom : s.kingdoms) {
			log.info("Checking kingdom '" + kingdom + "'...");
			Optional<Nation> nation = dsClient.getNation(r.getGameId(), kingdom);

			if(nation.isPresent()) {
				nations.put(kingdom, nation.get());
				addresses.add(nation.get().email);
				log.info("Kingdom '" + kingdom + "' successfully included");
			} else {
				log.severe("Unable to include kingdom '" + kingdom + "' in nation setups");
			}
		}

		World w = World.startNew(passHash, obsPassHash, nations);
		Set<Long> activeGames = dsClient.getActiveGames().orElse(new HashSet<>());
		activeGames.add(r.getGameId());

		boolean response = MultiPutRequest.create()
				.addWorld(r.getGameId(), w)
				.addWorldDate(r.getGameId(), 1)
				.addActiveGames(activeGames)
				.put(dsClient);

		if(response) {
			mail(addresses, "👑 Empire: Game Begins", "A game of Empire that you are playing in has started! You can make your orders for the first turn at http://pawlicki.kaelri.com/empire/map1.html?gid=" + r.getGameId() + ".");
		}

		return response;
	}

	private boolean postSetup(Request r) {
		Optional<Nation> nationCheck = dsClient.getNation(r.getGameId(), r.getKingdom());

		if (nationCheck.isPresent()) return false; // We expect nation to not be found

		Nation nation = JsonUtils.fromJson(r.getBody(), Nation.class);
		nation.password = encodePassword(nation.password);

		if(nation.password == null) {
			log.severe("Failure during post-setup, unable to encode password");
			return false;
		}

		return MultiPutRequest.create()
				.addNation(r.getGameId(), r.getKingdom(), nation)
				.addPlayer(new Player(nation.email, nation.password))
				.put(dsClient);
	}

	private boolean postRealTimeCommunication(Request r) {
		if (!checkPassword(r).passesWrite()) return false;

		Optional<World> worldOpt = dsClient.getWorld(r.getGameId(), r.getTurn());
		if (!worldOpt.isPresent()) {
			log.severe("Could not add real-tome communication, unable to retrieve world for gameId=" + r.getGameId() + ", turn=" + r.getTurn());
			return false;
		}

		World w = worldOpt.get();
		w.addRtc(r.getKingdom(), JsonUtils.fromJson(r.getBody(), com.empire.Message.class));
		dsClient.putWorld(r.getGameId(), w);
		return true;
	}

	private boolean postChangePlayer(Request r) {
		if (!checkPassword(r).passesWrite()) return false;

		Optional<Integer> dateOpt = dsClient.getWorldDate(r.getGameId());

		if (!dateOpt.isPresent()) {
			log.severe("Unable to retrieve date for gameId=" + r.getGameId());
			return false;
		}

		int date = r.getTurn() != 0 ? r.getTurn() : dateOpt.get();
		Optional<World> worldOpt = dsClient.getWorld(r.getGameId(), date);

		if(!worldOpt.isPresent()) {
			log.severe("Unable to retrieve world for gameId=" + r.getGameId() + ", turn=" + date);
			return false;
		}

		World w = worldOpt.get();
		ChangePlayerRequestBody body = JsonUtils.fromJsonCamel(r.getBody(), ChangePlayerRequestBody.class);
		Optional<Player> playerOpt = dsClient.getPlayer(body.email);

		if(!playerOpt.isPresent()) {
			log.severe("Player not found for email=" + body.email);
			return false;
		}

		w.getNation(r.getKingdom()).email = body.email;
		w.getNation(r.getKingdom()).password = playerOpt.get().getPassHash();
		dsClient.putWorld(r.getGameId(), w);
		return true;
	}

	private static final class ChangePlayerRequestBody {
		public String email;
		public String password;
	}

	/*
	// TODO: remove
	private boolean migrate(Request r) {
		Optional<World> worldOpt = dsClient.getWorld(4, dsClient.getWorldDate(4).orElse(-1));

		if (!worldOpt.isPresent()) {
			log.severe( "Could not migrate, world not found!");
			return true;
		}

		World w = worldOpt.get();

		for (Character c : w.characters) if (c.getName().equals("Ea Rjinkuki")) c.setLocation(101);
		dsClient.putWorld(4, w);
		return true;
	}
	*/

	private enum PasswordCheck {
		PASS_GM(true, true),
		PASS_OBS(true, false),
		PASS_PLAYER(true, true),
		FAIL(false, false),
		NO_ENTITY(false, false);

		private final boolean passesRead;
		private final boolean passesWrite;

		PasswordCheck(boolean passesRead, boolean passesWrite) {
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

	private PasswordCheck checkPassword(Request r) {
		if (r.getPassword() == null) return PasswordCheck.FAIL;

		byte[] pwHash = hashPassword(r.getPassword());
		if (pwHash == null) return PasswordCheck.FAIL;

		Optional<Integer> dateOpt = dsClient.getWorldDate(r.getGameId());

		if (!dateOpt.isPresent()) {
			log.severe("Unable to retrieve date for gameId=" + r.getGameId());
			return PasswordCheck.NO_ENTITY;
		}

		Optional<World> worldOpt = dsClient.getWorld(r.getGameId(), dateOpt.get());

		if(!worldOpt.isPresent()) {
			log.severe("Unable to retrieve world for gameId=" + r.getGameId() + ", turn=" + dateOpt.get());
			return PasswordCheck.NO_ENTITY;
		}

		World w = worldOpt.get();

		byte[] gmPassHash = decodePassword(w.gmPasswordHash);
		byte[] obsPassHash = decodePassword(w.obsPasswordHash);
		Optional<Player> player = dsClient.getPlayer(w.getNation(r.getKingdom()).email);

		if(!player.isPresent()) {
			log.severe("Unable to retrieve for for email=" + w.getNation(r.getKingdom()).email);
			return PasswordCheck.NO_ENTITY;
		}

		if (w.getNationNames().contains(r.getKingdom()) && Arrays.equals(pwHash, decodePassword(player.get().getPassHash()))) return PasswordCheck.PASS_PLAYER;
		if (Arrays.equals(pwHash, gmPassHash)) return PasswordCheck.PASS_GM;
		if (Arrays.equals(pwHash, obsPassHash)) return PasswordCheck.PASS_OBS;
		return PasswordCheck.FAIL;
	}

	private void mail(String address, String subject, String body) {
		mail(Collections.singleton(address), subject, body);
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

	private String encodePassword(String pw) {
		byte[] hashPw = hashPassword(pw);
		return hashPw != null ? BaseEncoding.base16().encode(hashPw) : null;
	}

	private byte[] decodePassword(String pw) {
		return BaseEncoding.base16().decode(pw);
	}

	private byte[] hashPassword(String pw) {
		try {
			return MessageDigest.getInstance("SHA-256").digest((PASSWORD_SALT + pw).getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			log.log(Level.SEVERE, "Unable to hash password", e);
			return null;
		}
	}
}
