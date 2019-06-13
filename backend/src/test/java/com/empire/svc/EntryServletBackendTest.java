package com.empire.svc;

import com.empire.Nation;
import com.empire.NationData;
import com.empire.Orders;
import com.empire.World;
import com.empire.store.DatastoreClient;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class EntryServletBackendTest {
  private static EntryServletBackend backend;
  private static DatastoreClient dsClient;
  private static PasswordValidator passVal;
  private static LoginCache cache;
  private static Request req;

  private static final long gameIdTest = 42;
  private static final String kingdomTest = "TEST_KINGDOM";
  private static final int turnTest = 2;
  private static final int versionTest = 5;
  private static final String emailTest = "email@test.com";

  @Before
  public void setup() {
    dsClient = mock(DatastoreClient.class);
    passVal = mock(PasswordValidator.class);
    cache = mock(LoginCache.class);
    backend = new EntryServletBackend(dsClient, passVal, cache);

    req = mock(Request.class);
    when(req.getGameId()).thenReturn(gameIdTest);
    when(req.getKingdom()).thenReturn(kingdomTest);
    when(req.getTurn()).thenReturn(turnTest);
    when(req.getVersion()).thenReturn(versionTest);
  }

  @Test
  public void getOrdersRequiresPasswordSuccess() {
    when(passVal.checkPassword(req)).thenReturn(PasswordValidator.PasswordCheck.FAIL);

    assertFalse(backend.getOrders(req).isPresent());
    verifyZeroInteractions(dsClient);
  }

  @Test
  public void getOrdersNotFoundReturnsEmpty() {
    when(passVal.checkPassword(req)).thenReturn(PasswordValidator.PasswordCheck.PASS_PLAYER);
    when(dsClient.getOrders(gameIdTest, kingdomTest, turnTest)).thenReturn(Optional.empty());

    assertFalse(backend.getOrders(req).isPresent());
    verify(dsClient).getOrders(gameIdTest, kingdomTest, turnTest);
  }

  @Test
  public void getOrdersFoundReturnsOrders() {
    when(passVal.checkPassword(req)).thenReturn(PasswordValidator.PasswordCheck.PASS_PLAYER);
    Orders orders = mock(Orders.class);
    when(dsClient.getOrders(gameIdTest, kingdomTest, turnTest)).thenReturn(Optional.of(orders));

    assertEquals(orders, backend.getOrders(req).orElse(mock(Orders.class)));
    verify(dsClient).getOrders(gameIdTest, kingdomTest, turnTest);
  }

  @Test
  public void getSetupNotFoundReturnsEmpty() {
    when(dsClient.getNation(gameIdTest, kingdomTest)).thenReturn(Optional.empty());

    assertFalse(backend.getSetup(req).isPresent());
    verify(dsClient).getNation(gameIdTest, kingdomTest);
  }

  @Test
  public void getSetupFoundReturnsNation() {
    Nation nation = mock(Nation.class);
    when(dsClient.getNation(gameIdTest, kingdomTest)).thenReturn(Optional.of(nation));

    assertEquals(nation, backend.getSetup(req).orElse(mock(Nation.class)));
    verify(dsClient).getNation(gameIdTest, kingdomTest);
  }

  @Test
  public void getWorldRequiresPasswordSuccess() {
    when(passVal.checkPassword(req)).thenReturn(PasswordValidator.PasswordCheck.FAIL);

    assertFalse(backend.getWorld(req).isPresent());
    verifyZeroInteractions(dsClient);
  }

  @Test
  public void getWorldDateNotFoundReturnsEmpty() {
    when(passVal.checkPassword(req)).thenReturn(PasswordValidator.PasswordCheck.PASS_PLAYER);
    when(dsClient.getWorldDate(gameIdTest)).thenReturn(Optional.empty());

    assertFalse(backend.getWorld(req).isPresent());
    verify(dsClient).getWorldDate(gameIdTest);
  }

  @Test
  public void getWorldNotFoundReturnsEmpty() {
    when(passVal.checkPassword(req)).thenReturn(PasswordValidator.PasswordCheck.PASS_PLAYER);
    when(dsClient.getWorldDate(gameIdTest)).thenReturn(Optional.of(turnTest));
    when(dsClient.getWorld(gameIdTest, turnTest)).thenReturn(Optional.empty());

    assertFalse(backend.getWorld(req).isPresent());
    verify(dsClient).getWorldDate(gameIdTest);
    verify(dsClient).getWorld(gameIdTest, turnTest);
  }

  @Test
  public void getWorldFoundReturnsFilteredWorld() {
    when(passVal.checkPassword(req)).thenReturn(PasswordValidator.PasswordCheck.PASS_PLAYER);
    when(dsClient.getWorldDate(gameIdTest)).thenReturn(Optional.of(turnTest));
    World world = mock(World.class);
    when(dsClient.getWorld(gameIdTest, turnTest)).thenReturn(Optional.of(world));

    assertEquals(world, backend.getWorld(req).orElse(mock(World.class)));
    verify(dsClient).getWorldDate(gameIdTest);
    verify(dsClient).getWorld(gameIdTest, turnTest);
    verify(world).filter(kingdomTest);
  }

  @Test
  public void getAdvancePollActiveGamesNotFoundReturnsEmpty() {
    when(dsClient.getActiveGames()).thenReturn(Optional.empty());

    assertFalse(backend.getAdvancePoll().isPresent());
    verify(dsClient).getActiveGames();
  }

  @Test
  public void getAdvancePollWorldDateNotFoundReturnsEmpty() {
    when(dsClient.getActiveGames()).thenReturn(Optional.of(Collections.singleton(gameIdTest)));
    when(dsClient.getWorldDate(gameIdTest)).thenReturn(Optional.empty());
    when(dsClient.getWorld(gameIdTest, -1)).thenReturn(Optional.empty());

    assertFalse(backend.getAdvancePoll().isPresent());
    verify(dsClient).getWorldDate(gameIdTest);
  }

  @Test
  public void getAdvancePollWorldNotFoundReturnsEmpty() {
    when(dsClient.getActiveGames()).thenReturn(Optional.of(Collections.singleton(gameIdTest)));
    when(dsClient.getWorldDate(gameIdTest)).thenReturn(Optional.of(turnTest));
    when(dsClient.getWorld(gameIdTest, turnTest)).thenReturn(Optional.empty());

    assertFalse(backend.getAdvancePoll().isPresent());
    verify(dsClient).getWorld(gameIdTest, turnTest);
  }

  @Test
  public void getAdvancePollNotReadyReturnsEmpty() {
    when(dsClient.getActiveGames()).thenReturn(Optional.of(Collections.singleton(gameIdTest)));
    when(dsClient.getWorldDate(gameIdTest)).thenReturn(Optional.of(turnTest));
    World world = mock(World.class);
    world.nextTurn = Instant.now().toEpochMilli() + 1000000000;
    when(dsClient.getWorld(gameIdTest, turnTest)).thenReturn(Optional.of(world));

    assertTrue(backend.getAdvancePoll().isPresent());
    verify(dsClient).getWorld(gameIdTest, turnTest);
    verify(world, never()).advance(any());
  }

  @Test
  public void getAdvancePollAdvancesWorld() {
    when(dsClient.getActiveGames()).thenReturn(Optional.of(Collections.singleton(gameIdTest)));
    when(dsClient.getWorldDate(gameIdTest)).thenReturn(Optional.of(turnTest));

    World world = mock(World.class);
    world.nextTurn = Instant.now().toEpochMilli() - 100;
    world.date = turnTest;
    world.gameover = false;
    when(world.getNationNames()).thenReturn(Collections.singleton(kingdomTest));
    when(dsClient.getWorld(gameIdTest, turnTest)).thenReturn(Optional.of(world));

    Map<String, String> ordersMap = Collections.emptyMap();
    Orders orders = mock(Orders.class);
    when(orders.getOrders()).thenReturn(ordersMap);
    when(dsClient.getOrders(gameIdTest, kingdomTest, turnTest)).thenReturn(Optional.of(orders));

    Map<String, Map<String, String>> gameOrdersMap = new HashMap<>();
    gameOrdersMap.put(kingdomTest, ordersMap);

    assertTrue(backend.getAdvancePoll().isPresent());
    verify(world).advance(gameOrdersMap);
    verify(dsClient).multiPut(anyObject()); // TODO: This is not an adequate test condition
  }

  @Test
  public void getActivityRequiresGmPasswordSuccess() {
    when(passVal.checkPassword(req)).thenReturn(PasswordValidator.PasswordCheck.PASS_PLAYER);

    assertFalse(backend.getActivity(req).isPresent());
    verifyZeroInteractions(dsClient);
  }

  @Test
  public void getActivityDateNotFoundReturnsEmpty() {
    when(passVal.checkPassword(req)).thenReturn(PasswordValidator.PasswordCheck.PASS_GM);
    when(dsClient.getWorldDate(gameIdTest)).thenReturn(Optional.empty());

    assertFalse(backend.getActivity(req).isPresent());
    verify(dsClient).getWorldDate(gameIdTest);
  }

  @Test
  public void getActivityWorldNotFoundReturnsEmpty() {
    when(passVal.checkPassword(req)).thenReturn(PasswordValidator.PasswordCheck.PASS_GM);
    when(dsClient.getWorldDate(gameIdTest)).thenReturn(Optional.of(turnTest));
    when(dsClient.getWorld(gameIdTest, turnTest)).thenReturn(Optional.empty());

    assertFalse(backend.getActivity(req).isPresent());
    verify(dsClient).getWorld(gameIdTest, turnTest);
  }

  @Test
  public void getActivityReturnsPlayerActivity() {
    when(passVal.checkPassword(req)).thenReturn(PasswordValidator.PasswordCheck.PASS_GM);
    when(dsClient.getWorldDate(gameIdTest)).thenReturn(Optional.of(turnTest));

    NationData nation = mock(NationData.class);
    nation.email = emailTest;

    World world = mock(World.class);
    when(world.getNationNames()).thenReturn(Collections.singleton(kingdomTest));
    when(world.getNation(kingdomTest)).thenReturn(nation);

    when(dsClient.getWorld(gameIdTest, turnTest)).thenReturn(Optional.of(world));

    when(cache.fetchLoginHistory(Collections.singletonList(emailTest), gameIdTest, turnTest))
        .thenReturn(Collections.singletonList(Arrays.asList(false, true)));

    List<Map<String, Boolean>> result = Arrays.asList(Collections.singletonMap(emailTest, false), Collections.singletonMap(emailTest, true));

    Optional<List<Map<String, Boolean>>> activityOpt = backend.getActivity(req);

    assertTrue(activityOpt.isPresent());
    assertEquals(result, activityOpt.get());
    verify(dsClient).getWorld(gameIdTest, turnTest);
  }

  @Test
  public void postOrdersRequiresPasswordSuccess() {
    when(passVal.checkPassword(req)).thenReturn(PasswordValidator.PasswordCheck.FAIL);

    assertFalse(backend.postOrders(req));
    verify(passVal).checkPassword(req);
    verifyZeroInteractions(dsClient);
  }

  @Test
  public void postOrdersDateNotFoundReturnsFalse() {
    when(passVal.checkPassword(req)).thenReturn(PasswordValidator.PasswordCheck.PASS_PLAYER);
    when(dsClient.getWorldDate(gameIdTest)).thenReturn(Optional.empty());

    assertFalse(backend.postOrders(req));
    verify(dsClient).getWorldDate(gameIdTest);
  }

  @Test
  public void postOrdersDateNotCurrentReturnsFalse() {
    when(passVal.checkPassword(req)).thenReturn(PasswordValidator.PasswordCheck.PASS_PLAYER);
    when(dsClient.getWorldDate(gameIdTest)).thenReturn(Optional.of(turnTest - 1));

    assertFalse(backend.postOrders(req));
    verify(dsClient).getWorldDate(gameIdTest);
  }

  @Test
  public void postOrdersPutsOrdersAndReturnsTrue() {
    when(passVal.checkPassword(req)).thenReturn(PasswordValidator.PasswordCheck.PASS_PLAYER);
    when(dsClient.getWorldDate(gameIdTest)).thenReturn(Optional.of(turnTest));
    when(req.getBody()).thenReturn("{}");
    when(dsClient.putOrders(any())).thenReturn(true);

    assertTrue(backend.postOrders(req));
    verify(dsClient).putOrders(new Orders(gameIdTest, kingdomTest, turnTest, Collections.emptyMap(), versionTest));
  }

  @Test
  public void postAdvanceWorldRequiresGmPasswordSuccess() {
    when(passVal.checkPassword(req)).thenReturn(PasswordValidator.PasswordCheck.PASS_PLAYER);

    assertFalse(backend.postAdvanceWorld(req));
    verifyZeroInteractions(dsClient);
  }

  @Test
  public void postAdvanceWorldWorldNotFoundReturnsFalse() {
    when(passVal.checkPassword(req)).thenReturn(PasswordValidator.PasswordCheck.PASS_GM);
    when(dsClient.getWorld(gameIdTest, turnTest)).thenReturn(Optional.empty());

    assertFalse(backend.postAdvanceWorld(req));
    verify(dsClient).getWorld(gameIdTest, turnTest);
  }

  @Test
  public void postAdvanceWorldWriteAdvancedWorld() {
    // TODO: This test is not sufficient, should refactor single advance world method in backend
    // This next line is necessary because postAdvanceWorld calls getAdvancePoll(), this is bad
    when(dsClient.getActiveGames()).thenReturn(Optional.empty());

    when(passVal.checkPassword(req)).thenReturn(PasswordValidator.PasswordCheck.PASS_GM);
    World world = mock(World.class);

    when(dsClient.getWorld(gameIdTest, turnTest)).thenReturn(Optional.of(world));

    assertTrue(backend.postAdvanceWorld(req));
    verify(dsClient).putWorld(gameIdTest, world);
  }
}
