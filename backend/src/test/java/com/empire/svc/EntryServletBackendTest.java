package com.empire.svc;

import com.empire.Orders;
import com.empire.store.DatastoreClient;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
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
  private static final int turnTest = 13;

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

  }

  @Test
  public void getOrdersRequiresPasswordSuccess() {
    when(passVal.checkPassword(req)).thenReturn(PasswordValidator.PasswordCheck.FAIL);

    assertFalse(backend.getOrders(req).isPresent());
    verifyZeroInteractions(dsClient);
  }

  @Test
  public void getOrdersNotFoundReturnsNothing() {
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
}
