package com.empire.svc;

import com.empire.NationData;
import com.empire.World;
import com.empire.store.DatastoreClient;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PasswordValidatorTest {
  private static DatastoreClient dsClient;
  private static PasswordValidator passVal;
  private static Request req;

  private final String emailTest = "test@email.com";
  private final long gameIdTest = -1;
  private final int dateTest = 5;
  private final String kingdomTest = "TEST_KINGDOM";
  private final String pwTest = "0123456789ABCDEF";
  private final String pwFailTest = "FEDCBA9876543210";
  private final String pwEncTest = "CB469D8CFBD5CB38935AD4C8CBAE397A41A42ADCB85D2D5858550465F7BD31FC";

  @Before
  public void setup() {
    dsClient = mock(DatastoreClient.class);
    passVal = new PasswordValidator(dsClient);

    req = mock(Request.class);
    when(req.getGameId()).thenReturn(gameIdTest);
    when(req.getTurn()).thenReturn(dateTest);
    when(req.getKingdom()).thenReturn(kingdomTest);
  }

  @Test
  public void nullPassworldReturnsFailure() {
    when(req.getPassword()).thenReturn(null);

    assertEquals(PasswordValidator.PasswordCheck.FAIL, passVal.checkPassword(req));
  }

  @Test
  public void noDateFoundReturnsNoEntity() {
    when(req.getPassword()).thenReturn(pwTest);
    when(dsClient.getWorldDate(gameIdTest)).thenReturn(Optional.empty());

    assertEquals(PasswordValidator.PasswordCheck.NO_ENTITY, passVal.checkPassword(req));
  }

  @Test
  public void noWorldFoundReturnsNoEntity() {
    when(req.getPassword()).thenReturn(pwTest);
    when(dsClient.getWorldDate(gameIdTest)).thenReturn(Optional.of(dateTest));
    when(dsClient.getWorld(gameIdTest, dateTest)).thenReturn(Optional.empty());

    assertEquals(PasswordValidator.PasswordCheck.NO_ENTITY, passVal.checkPassword(req));
  }

  @Test
  public void noPlayerFoundReturnsNoEntity() {
    when(req.getPassword()).thenReturn(pwTest);
    when(dsClient.getWorldDate(gameIdTest)).thenReturn(Optional.of(dateTest));

    NationData k = mock(NationData.class);
    k.email = emailTest;

    World w = mock(World.class);
    when(w.getNation(kingdomTest)).thenReturn(k);
    when(dsClient.getWorld(gameIdTest, dateTest)).thenReturn(Optional.of(w));

    when(dsClient.getPlayer(emailTest)).thenReturn(Optional.empty());

    assertEquals(PasswordValidator.PasswordCheck.NO_ENTITY, passVal.checkPassword(req));
  }

  @Test
  public void noKingdomInWorldReturnsPasswordFail() {
    when(req.getPassword()).thenReturn(pwTest);
    when(dsClient.getWorldDate(gameIdTest)).thenReturn(Optional.of(dateTest));

    NationData k = mock(NationData.class);
    k.email = emailTest;

    World w = mock(World.class);
    w.gmPasswordHash = pwFailTest;
    w.obsPasswordHash = pwFailTest;
    when(w.getNation(kingdomTest)).thenReturn(k);
    when(w.getNationNames()).thenReturn(Collections.emptySet());
    when(dsClient.getWorld(gameIdTest, dateTest)).thenReturn(Optional.of(w));

    Player p = mock(Player.class);
    when(p.getPassHash()).thenReturn(pwEncTest);
    when(dsClient.getPlayer(emailTest)).thenReturn(Optional.of(p));

    assertEquals(PasswordValidator.PasswordCheck.FAIL, passVal.checkPassword(req));
  }

  @Test
  public void playerPasswordWrongReturnsPasswordFail() {
    when(req.getPassword()).thenReturn(pwTest);
    when(dsClient.getWorldDate(gameIdTest)).thenReturn(Optional.of(dateTest));

    NationData k = mock(NationData.class);
    k.email = emailTest;

    World w = mock(World.class);
    w.gmPasswordHash = pwFailTest;
    w.obsPasswordHash = pwFailTest;
    when(w.getNation(kingdomTest)).thenReturn(k);
    when(w.getNationNames()).thenReturn(Collections.singleton(kingdomTest));
    when(w.getNationNames()).thenReturn(Collections.emptySet());
    when(dsClient.getWorld(gameIdTest, dateTest)).thenReturn(Optional.of(w));

    Player p = mock(Player.class);
    when(p.getPassHash()).thenReturn(pwEncTest);
    when(dsClient.getPlayer(emailTest)).thenReturn(Optional.of(p));

    assertEquals(PasswordValidator.PasswordCheck.FAIL, passVal.checkPassword(req));
  }

  @Test
  public void playerPasswordMatchReturnsPassPlayer() {
    when(req.getPassword()).thenReturn(pwTest);
    when(dsClient.getWorldDate(gameIdTest)).thenReturn(Optional.of(dateTest));

    NationData k = mock(NationData.class);
    k.email = emailTest;

    World w = mock(World.class);
    when(w.getNation(kingdomTest)).thenReturn(k);
    when(w.getNationNames()).thenReturn(Collections.singleton(kingdomTest));
    when(dsClient.getWorld(gameIdTest, dateTest)).thenReturn(Optional.of(w));

    Player p = mock(Player.class);
    when(p.getPassHash()).thenReturn(pwEncTest);
    when(dsClient.getPlayer(emailTest)).thenReturn(Optional.of(p));

    assertEquals(PasswordValidator.PasswordCheck.PASS_PLAYER, passVal.checkPassword(req));
  }

  @Test
  public void gmPasswordMatchReturnsPassGm() {
    when(req.getPassword()).thenReturn(pwTest);
    when(dsClient.getWorldDate(gameIdTest)).thenReturn(Optional.of(dateTest));

    NationData k = mock(NationData.class);
    k.email = emailTest;

    World w = mock(World.class);
    w.gmPasswordHash = pwEncTest;
    w.obsPasswordHash = pwFailTest;
    when(w.getNation(kingdomTest)).thenReturn(k);
    when(dsClient.getWorld(gameIdTest, dateTest)).thenReturn(Optional.of(w));

    Player p = mock(Player.class);
    when(dsClient.getPlayer(emailTest)).thenReturn(Optional.of(p));

    assertEquals(PasswordValidator.PasswordCheck.PASS_GM, passVal.checkPassword(req));
  }

  @Test
  public void obsPasswordMatchReturnsPassObs() {
    when(req.getPassword()).thenReturn(pwTest);
    when(dsClient.getWorldDate(gameIdTest)).thenReturn(Optional.of(dateTest));

    NationData k = mock(NationData.class);
    k.email = emailTest;

    World w = mock(World.class);
    w.gmPasswordHash = pwFailTest;
    w.obsPasswordHash = pwEncTest;
    when(w.getNation(kingdomTest)).thenReturn(k);
    when(dsClient.getWorld(gameIdTest, dateTest)).thenReturn(Optional.of(w));

    Player p = mock(Player.class);
    when(dsClient.getPlayer(emailTest)).thenReturn(Optional.of(p));

    assertEquals(PasswordValidator.PasswordCheck.PASS_OBS, passVal.checkPassword(req));
  }
}
