package com.empire.svc;

import com.empire.World;
import com.empire.store.DatastoreClient;
import com.google.common.io.BaseEncoding;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;


class PasswordValidator {
  private static final Logger log = Logger.getLogger(PasswordValidator.class.getName());
  private static final String PASSWORD_SALT = "~ Empire_Password Salt ~123`";

  private final DatastoreClient dsClient;

  PasswordValidator(DatastoreClient dsClient) {
    this.dsClient = dsClient;
  }

  public PasswordCheck checkPassword(Request r) {
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

  public String encodePassword(String pw) {
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

  enum PasswordCheck {
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
}
