package com.empire.store;

import com.empire.Character;
import com.empire.StartWorldGson;
import com.google.common.io.BaseEncoding;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import java.io.IOException;
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

    /*
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
    */

    private String getOrders(Request r, HttpServletResponse resp) {
        if (!checkPassword(r).passesRead()) return null;
        Orders orders = dsClient.getOrders(r.gameId, r.kingdom, r.turn);
        resp.setHeader("SJS-Version", "" + orders.version);
        return GaeDatastoreClient.gson.toJson(orders.orders);
    }

    /*
    private String getSetup(Request r) {
        // TODO - should filter this data or display it.
        try {
            return Nation.NationGson.loadJson(r.kingdom, r.gameId, DatastoreServiceFactory.getDatastoreService());
        } catch (EntityNotFoundException e) {
            return null;
        }
    }
    */

    private String getSetup(Request r) {
        // TODO - should filter this data or display it.
        return dsClient.getNationJson(r.gameId, r.kingdom);
    }

    /*
    private int getWorldDate(long gameId, DatastoreService service) throws EntityNotFoundException {
        return (int)((Long)(service.get(KeyFactory.createKey("CURRENTDATE", "game_" + gameId)).getProperty("date"))).longValue();
    }
    */

    private int getWorldDate(long gameId) {
        return dsClient.getWorldDate(gameId);
    }

    /*
    private String getWorld(Request r) {
        DatastoreService service = DatastoreServiceFactory.getDatastoreService();
        CheckPasswordResult result = checkPassword(r, service);
        if (!result.passesRead()) {
            return null;
        }
        try {
            int date = r.turn != 0 ? r.turn : getWorldDate(r.gameId, service);
            World w = World.load(r.gameId, date, service);
            if (result == CheckPasswordResult.PASS_PLAYER && r.turn == 0) LoginCache.getSingleton().recordLogin(r.gameId, date, w.getNation(r.kingdom).email, service);
            w.filter(r.kingdom);
            return w.toString();
        } catch (EntityNotFoundException e) {
            log.log(Level.INFO, "No such world.");
            return null;
        }
    }
    */

    private String getWorld(Request r) {
        CheckPasswordResult result = checkPassword(r);

        if (!result.passesRead()) return null;

        int date = r.turn != 0 ? r.turn : getWorldDate(r.gameId);
        World w = dsClient.getWorld(r.gameId, date);

        if (w == null){
            log.log(Level.INFO, "No such world.");
            return null;
        }

        if (result == CheckPasswordResult.PASS_PLAYER && r.turn == 0) LoginCache.getInstance().recordLogin(r.gameId, date, w.getNation(r.kingdom).email);
        w.filter(r.kingdom);
        return w.toString();
    }

    /*
    private String getActivity(Request r) {
        DatastoreService service = DatastoreServiceFactory.getDatastoreService();
        if (checkPassword(r, service) != CheckPasswordResult.PASS_GM) return null;
        try {
            int date = r.turn != 0 ? r.turn : getWorldDate(r.gameId, service);
            HashMap<String, ArrayList<String>> nationEmails = new HashMap<>();
            World w = World.load(r.gameId, date, service);
            List<String> emails = w.getNationNames().stream().map(s -> w.getNation(s).email).collect(Collectors.toList());
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
        }
    }
    */

    private String getActivity(Request r) {
        if (checkPassword(r) != CheckPasswordResult.PASS_GM) return null;

        int worldDate = dsClient.getWorldDate(r.gameId);

        if (worldDate == -1) {
            log.log(Level.WARNING, "No such world.");
            return null;
        }

        int date = r.turn != 0 ? r.turn : worldDate;

        World w = dsClient.getWorld(r.gameId, date);
        List<String> emails = w.getNationNames().stream().map(s -> w.getNation(s).email).collect(Collectors.toList());
        List<List<Boolean>> actives = LoginCache.getInstance().fetchLoginHistory(r.gameId, date, emails);

        List<Map<String, Boolean>> result = new ArrayList<>();
        for (List<Boolean> turnActives : actives) {
            Map<String, Boolean> turn = new HashMap<>();
            for (int i = 0; i < emails.size(); i++) {
                turn.put(emails.get(i), turnActives.get(i));
            }
            result.add(turn);
        }

        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(result);
    }

    /*
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
                        for (String kingdom : w.getNationNames()) {
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
                        if (w.gameover) {
                            ActiveGames newActiveGames = ActiveGames.fromGson((String)service.get(KeyFactory.createKey("ACTIVEGAMES", "_")).getProperty("active_games"));
                            newActiveGames.activeGameIds.remove(gameId);
                            Entity activeGames = new Entity("ACTIVEGAMES", "_");
                            activeGames.setProperty("active_games", new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().toJson(newActiveGames));
                            service.put(activeGames);
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
    */

    // TODO: Include all puts in single transaction inside client
    private String getAdvancePoll() {
        for (Long gameId : dsClient.getActiveGames()) {
            int date = dsClient.getWorldDate(gameId);
            if (date == -1) continue;

            World w = dsClient.getWorld(gameId, date);
            if (w == null) continue;

            if (w.nextTurn < Instant.now().toEpochMilli()) {
                HashMap<String, Orders> orders = new HashMap<>();

                for (String kingdom : w.getNationNames()) {
                    Orders ordersKingdom = dsClient.getOrders(gameId, kingdom, w.date);
                    if (ordersKingdom == null) log.warning("Cannot find orders for " + kingdom);
                    orders.put(kingdom, ordersKingdom);
                }

                Map<String, String> emails = w.advance(orders);
                dsClient.putWorld(gameId, w);
                dsClient.putWorldDate(gameId, w.date);

                for (String mail : emails.keySet()) {
                    mail(mail, "👑 Empire: Turn Advances", emails.get(mail).replace("%GAMEID%", "" + gameId));
                }

                if (w.gameover) {
                    Set<Long> activeGames = dsClient.getActiveGames();
                    activeGames.remove(gameId);
                    dsClient.putActiveGames(activeGames);
                }
            }
        }

        return "";
    }

    /*
    // TODO: remove, or check that the request bears the GM password - this is insecure as-is (anyone can advance).
    private boolean postAdvanceWorld(Request r) {
        DatastoreService service = DatastoreServiceFactory.getDatastoreService();
        Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
        HashSet<String> kingdoms = new HashSet<>();
        try {
            World	w = World.load(r.gameId, r.turn, service);
            w.nextTurn = 0;
            service.put(w.toEntity(r.gameId));
            txn.commit();
        } catch (EntityNotFoundException e) {
            return false;
        } finally {
            if (txn.isActive()) txn.rollback();
        }
        getAdvancePoll();
        return true;
    }
    */

    // TODO: remove, or check that the request bears the GM password - this is insecure as-is (anyone can advance).
    private boolean postAdvanceWorld(Request r) {
        World w = dsClient.getWorld(r.gameId, r.turn);
        w.nextTurn = 0;
        dsClient.putWorld(r.gameId, w);
        getAdvancePoll();
        return true;
    }

    /*
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
            World w = World.startNew(passHash, obsPassHash, nations);
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
        } finally {
            if (txn.isActive()) txn.rollback();
        }
        mail(addresses, "👑 Empire: Game Begins", "A game of Empire that you are playing in has started! You can make your orders for the first turn at http://pawlicki.kaelri.com/empire/map1.html?gid=" + r.gameId + ".");
        return true;
    }
    */

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

        HashSet<String> addresses = new HashSet<>();

        // Collect setups.
        HashMap<String, Nation> nations = new HashMap<>();

        for (String kingdom : s.kingdoms) {
            log.log(Level.INFO, "Checking kingdom \"" + kingdom + "\"...");
            Nation nation = dsClient.getNation(r.gameId, kingdom);

            // Nation is not in the game.
            if(nation == null) continue;

            nations.put(kingdom, nation);
            addresses.add(nations.get(kingdom).email);
        }

        World w = World.startNew(passHash, obsPassHash, nations);
        dsClient.putWorld(r.gameId, w);
        dsClient.putWorldDate(r.gameId, 1);

        Set<Long> activeGames = dsClient.getActiveGames();
        if (activeGames == null) activeGames = new HashSet<>();
        activeGames.add(r.gameId);
        dsClient.putActiveGames(activeGames);

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

    /*
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
            // if (w.kingdoms.containsKey(r.kingdom) && w.getNation(r.kingdom).accessToken.equals(r.password)) return CheckPasswordResult.PASS_PLAYER;
            // if (w.kingdoms.containsKey(r.kingdom) && Arrays.equals(attemptHash, BaseEncoding.base16().decode(w.getNation(r.kingdom).password))) return CheckPasswordResult.PASS_PLAYER;
            if (w.getNationNames().contains(r.kingdom) && Arrays.equals(attemptHash, BaseEncoding.base16().decode(Player.loadPlayer(w.getNation(r.kingdom).email, service).passHash))) return CheckPasswordResult.PASS_PLAYER;
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
    */

    private CheckPasswordResult checkPassword(Request r) {
        try {
            if (r.password == null) return CheckPasswordResult.FAIL;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] attemptHash = digest.digest((PASSWORD_SALT + r.password).getBytes(StandardCharsets.UTF_8));
            int date = dsClient.getWorldDate(r.gameId);

            if (date == -1){
                log.log(Level.INFO, "No world for " + r.gameId + ", " + r.kingdom);
                return CheckPasswordResult.NO_ENTITY;
            }

            World w = dsClient.getWorld(r.gameId, date);

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

    /*
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
    */

    // TODO: Fix Request so work with Orders new def
    private boolean postOrders(Request r) {
        if (!checkPassword(r).passesWrite()) return false;
        int currentTurn = dsClient.getWorldDate(r.gameId);
        if (currentTurn == -1 || r.turn != currentTurn) return false;
//        dsClient.putOrders(new Orders(r.gameId, r.turn, r.version, r.kingdom, r.body));
        dsClient.putOrders(new Orders(r.gameId, r.turn, r.version, r.kingdom, null));
        return true;
    }

    /*
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
    */

    private boolean postRealTimeCommunication(Request r) {
        if (!checkPassword(r).passesWrite()) return false;
        World w = dsClient.getWorld(r.gameId, r.turn);
        w.addRtc(r.body, r.kingdom);
        dsClient.putWorld(r.gameId, w);
        return true;
    }

    private static final class ChangePlayerRequestBody {
        public String email;
        public String password;
    }

    /*
    private boolean postChangePlayer(Request r) {
        DatastoreService service = DatastoreServiceFactory.getDatastoreService();
        Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
        try {
            if (!checkPassword(r, service).passesWrite()) return false;
            int date = r.turn != 0 ? r.turn : getWorldDate(r.gameId, service);
            World w = World.load(r.gameId, date, service);
            ChangePlayerRequestBody body = new GsonBuilder().create().fromJson(r.body, ChangePlayerRequestBody.class);
            Player p = Player.loadPlayer(body.email, service);
            w.getNation(r.kingdom).email = body.email;
            w.getNation(r.kingdom).password = p.passHash;
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
    */

    private boolean postChangePlayer(Request r) {
        if (!checkPassword(r).passesWrite()) return false;
        int date = r.turn != 0 ? r.turn : dsClient.getWorldDate(r.gameId);
        World w = dsClient.getWorld(r.gameId, date);

        ChangePlayerRequestBody body = new GsonBuilder().create().fromJson(r.body, ChangePlayerRequestBody.class);
        Player p = dsClient.getPlayer(body.email);
        w.getNation(r.kingdom).email = body.email;
        w.getNation(r.kingdom).password = p.passHash;
        dsClient.putWorld(r.gameId, w);
        return true;
    }

    /*
    // TODO: remove
    private boolean migrate(Request rr) {
        DatastoreService service = DatastoreServiceFactory.getDatastoreService();
        Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
        try {
            World w = World.load(4, getWorldDate(4, service), service);
            for (Character c : w.characters) if (c.name.equals("Ea Rjinkuki")) c.location = 101;
            service.put(w.toEntity(4));
            txn.commit();
        } catch (EntityNotFoundException e) {
            log.log(Level.INFO, "Not found!", e);
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
        return true;
    }
    */

    // TODO: remove
    private boolean migrate(Request r) {
        World w = dsClient.getWorld(4, dsClient.getWorldDate(4));
        for (Character c : w.characters) if (c.getName().equals("Ea Rjinkuki")) c.setLocation(101);
        dsClient.putWorld(4, w);
        return true;
    }

    /*
    private boolean postSetup(Request r) {
        DatastoreService service = DatastoreServiceFactory.getDatastoreService();
        Transaction txn = service.beginTransaction(TransactionOptions.Builder.withXG(true));
        try {
            Nation.NationGson.loadNation(r.kingdom, r.gameId, service);
            return false; // We expect an EntityNotFoundException.
        } catch (EntityNotFoundException e) {
            try {
                Nation.NationGson nation = Nation.NationGson.fromJson(r.body);
                nation.password = BaseEncoding.base16().encode(MessageDigest.getInstance("SHA-256").digest((PASSWORD_SALT + nation.password).getBytes(StandardCharsets.UTF_8)));
                service.put(nation.toEntity(r.kingdom, r.gameId));
                service.put(new Player(nation.email, nation.password).toEntity());
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
    */

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

        dsClient.putNation(r.gameId, r.kingdom);
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

