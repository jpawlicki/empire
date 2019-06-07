package com.empire.svc;

import com.empire.Nation;
import com.empire.Orders;
import com.empire.StartWorld;
import com.empire.World;
import com.empire.store.DatastoreClient;
import com.empire.store.GaeDatastoreClient;
import com.empire.store.MultiPutRequest;
import com.empire.util.JsonUtils;
import com.google.gson.reflect.TypeToken;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
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

class EntryServletBackend {
  private static final Logger log = Logger.getLogger(EntryServletBackend.class.getName());

  private static final String emailFrom = "gilgarn@gmail.com";
  private static final String emailFromPersonal = "Joshua Pawlicki";
  private static final String emailRecipPersonal = "Empire Player";
  private static final String emailsubject = "👑 Empire: Turn Advances";


  private final DatastoreClient dsClient;
  private final PasswordValidator passVal;
  private final LoginCache cache;

  static EntryServletBackend create(){
    DatastoreClient dsClient = GaeDatastoreClient.getInstance();
    return new EntryServletBackend(dsClient, new PasswordValidator(dsClient), LoginCache.getInstance());
  }

  EntryServletBackend(DatastoreClient dsClient, PasswordValidator passVal, LoginCache cache){
    this.dsClient = dsClient;
    this.passVal = passVal;
    this.cache = cache;
  }

  Optional<Orders> getOrders(Request r) {
    if (!passVal.checkPassword(r).passesRead()) return Optional.empty();
    Optional<Orders> orders = dsClient.getOrders(r.getGameId(), r.getKingdom(), r.getTurn());

    if(!orders.isPresent()) {
      log.severe("Unable to get orders for gameId=" + r.getGameId() + ", kingdom=" + r.getKingdom() + ", turn=" + r.getTurn());

    }

    return orders;
  }

  // TODO - should filter this data or display it.
  Optional<Nation> getSetup(Request r) {
    Optional<Nation> nation = dsClient.getNation(r.getGameId(), r.getKingdom());

    if(!nation.isPresent()) {
      log.severe("Unable to complete setup request for gameId=" + r.getGameId() + ", kingdom=" + r.getKingdom());
    }

    return nation;
  }

  Optional<World> getWorld(Request r) {
    PasswordValidator.PasswordCheck result = passVal.checkPassword(r);
    if (!result.passesRead()) return Optional.empty();

    Optional<Integer> dateOpt = dsClient.getWorldDate(r.getGameId());

    if (!dateOpt.isPresent()) {
      log.severe("Unable to retrieve date for gameId=" + r.getGameId());
      return Optional.empty();
    }

    int date = r.getTurn() != 0 ? r.getTurn() : dateOpt.get();
    Optional<World> worldOpt = dsClient.getWorld(r.getGameId(), date);

    if(!worldOpt.isPresent()) {
      log.severe("Unable to retrieve world for gameId=" + r.getGameId() + ", turn=" + date);
      return Optional.empty();
    }

    World w = worldOpt.get();
    if (result == PasswordValidator.PasswordCheck.PASS_PLAYER && r.getTurn() == 0) cache.recordLogin(r.getGameId(), date, w.getNation(r.getKingdom()).email);
    w.filter(r.getKingdom());

    return Optional.of(w);
  }

  Optional<String> getAdvancePoll() {
    Optional<Set<Long>> activeGamesOpt = dsClient.getActiveGames();

    if(!activeGamesOpt.isPresent()) {
      log.severe("Poller failed, could not retrieve active games");
      return Optional.empty();
    }

    for (Long gameId : activeGamesOpt.get()) {
      Optional<World> worldOpt = dsClient.getWorld(gameId, dsClient.getWorldDate(gameId).orElse(-1));

      if(!worldOpt.isPresent()) {
        log.severe("Poller failed, unable to retrieve current world for gameId=" + gameId);
        return Optional.empty();
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
        emails.forEach((k, v) -> mail(k, emailsubject, v.replace("%GAMEID%", Long.toString(gameId))));
      }
    }

    return Optional.of("");
  }

  Optional<List<Map<String, Boolean>>> getActivity(Request r) {
    if (passVal.checkPassword(r) != PasswordValidator.PasswordCheck.PASS_GM) return Optional.empty();

    Optional<Integer> dateOpt = dsClient.getWorldDate(r.getGameId());

    if (!dateOpt.isPresent()) {
      log.warning("Unable to retrieve date for gameId=" + r.getGameId());
      return Optional.empty();
    }

    int date = r.getTurn() != 0 ? r.getTurn() : dateOpt.get();
    Optional<World> worldOpt = dsClient.getWorld(r.getGameId(), date);

    if(!worldOpt.isPresent()) {
      log.severe("Unable to retrieve world for gameId=" + r.getGameId() + ", turn=" + date);
      return Optional.empty();
    }

    World w = worldOpt.get();

    List<String> emails = w.getNationNames().stream().map(s -> w.getNation(s).email).collect(Collectors.toList());
    List<List<Boolean>> actives = cache.fetchLoginHistory(r.getGameId(), date, emails);
    List<Map<String, Boolean>> result = actives.stream()
        .map(a -> IntStream.range(0, emails.size()).boxed().collect(Collectors.toMap(emails::get, a::get)))
        .collect(Collectors.toList());

    return Optional.of(result);
  }

  boolean postOrders(Request r) {
    if (!passVal.checkPassword(r).passesWrite()) return false;
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
  boolean postAdvanceWorld(Request r) {
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

  boolean postStartWorld(Request r) {
    StartWorld s = JsonUtils.fromJson(r.getBody(), StartWorld.class);
    String passHash = passVal.encodePassword(s.gmPassword);
    String obsPassHash = passVal.encodePassword(s.obsPassword);

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

  boolean postSetup(Request r) {
    Optional<Nation> nationCheck = dsClient.getNation(r.getGameId(), r.getKingdom());

    if (nationCheck.isPresent()) return false; // We expect nation to not be found

    Nation nation = JsonUtils.fromJson(r.getBody(), Nation.class);
    nation.password = passVal.encodePassword(nation.password);

    if(nation.password == null) {
      log.severe("Failure during post-setup, unable to encode password");
      return false;
    }

    return MultiPutRequest.create()
        .addNation(r.getGameId(), r.getKingdom(), nation)
        .addPlayer(new Player(nation.email, nation.password))
        .put(dsClient);
  }

  boolean postRealTimeCommunication(Request r) {
    if (!passVal.checkPassword(r).passesWrite()) return false;

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

  boolean postChangePlayer(Request r) {
    if (!passVal.checkPassword(r).passesWrite()) return false;

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

  private void mail(String address, String subject, String body) {
    mail(Collections.singleton(address), subject, body);
  }

  private void mail(Set<String> addresses, String subject, String body) {
    try {
      Message msg = new MimeMessage(Session.getDefaultInstance(new Properties(), null));
      msg.setFrom(new InternetAddress(emailFrom, emailFromPersonal));
      for (String address : addresses) {
        msg.addRecipient(Message.RecipientType.BCC, new InternetAddress(address, emailRecipPersonal));
      }
      msg.setSubject(subject);
      msg.setText(body);
      Transport.send(msg);
    } catch (MessagingException | UnsupportedEncodingException | NoClassDefFoundError e) {
      log.log(Level.SEVERE, "Failed to send mail", e);
    }
  }
}
