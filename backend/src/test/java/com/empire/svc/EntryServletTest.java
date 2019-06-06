package com.empire.svc;

import com.empire.Nation;
import com.empire.NationData;
import com.empire.Orders;
import com.empire.World;
import com.empire.store.DatastoreClient;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EntryServletTest {
  private EntryServlet servlet;
  private DatastoreClient dsClient;
  private LoginCache cache;
  private HttpServletRequest httpReq;
  private HttpServletResponse httpResp;
  private Request req;

  private final String emailTest = "test@email.com";
  private final long gameIdTest = -1;
  private final int turnTest = 5;
  private final String kingdomTest = "TEST_KINGDOM";
  private final String pwTest = "0123456789ABCDEF";
  private final String pwEncTest = "CB469D8CFBD5CB38935AD4C8CBAE397A41A42ADCB85D2D5858550465F7BD31FC";

  @Before
  public void setup() {
    req = mock(Request.class);
    when(req.getGameId()).thenReturn(gameIdTest);
    when(req.getKingdom()).thenReturn(kingdomTest);
    when(req.getTurn()).thenReturn(turnTest);

    httpReq = mock(HttpServletRequest.class);
    RequestFactory.factory = mock(RequestFactory.class);

    try {
      when(RequestFactory.factory.fromHttpServletRequest(httpReq)).thenReturn(req);
    } catch (IOException e) {
      fail("IOException when mocking RequestFactory.factory.fromHttpServletRequest()");
    }

    httpResp = mock(HttpServletResponse.class);

    try {
      when(httpResp.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
    } catch (IOException e) {
      fail("IOException when mocking httpResp.getOutputStream");
    }

    dsClient = mock(DatastoreClient.class);
    cache = mock(LoginCache.class);
    servlet = new EntryServlet(dsClient, cache);
  }

  private void passPasswordCheck() {
    when(req.getPassword()).thenReturn(pwTest);
    when(dsClient.getWorldDate(gameIdTest)).thenReturn(Optional.of(turnTest));
    when(dsClient.getOrders(gameIdTest, kingdomTest, turnTest)).thenReturn(Optional.empty());

    NationData k = mock(NationData.class);
    k.email = emailTest;

    World w = mock(World.class);
    w.gmPasswordHash = pwTest;
    w.obsPasswordHash = pwTest;
    when(w.getNation(kingdomTest)).thenReturn(k);
    when(w.getNationNames()).thenReturn(Collections.singleton(kingdomTest));
    when(dsClient.getWorld(gameIdTest, turnTest)).thenReturn(Optional.of(w));

    Player p = mock(Player.class);
    when(p.getPassHash()).thenReturn(pwEncTest);
    when(dsClient.getPlayer(emailTest)).thenReturn(Optional.of(p));
  }

  @Test
  public void getWithBadPathReturnsError404() {
    when(httpReq.getRequestURI()).thenReturn("BOGUS");

    try {
      servlet.doGet(httpReq, httpResp);
      verify(httpResp).sendError(404, "No such path.");
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void pingReturnsSuccess(){
    when(httpReq.getRequestURI()).thenReturn("/entry/ping");

    try {
      servlet.doGet(httpReq, httpResp);
      verify(httpResp).getOutputStream();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void getOrdersPasswordFailReturnsNull() {
    when(httpReq.getRequestURI()).thenReturn("/entry/orders");
    when(req.getPassword()).thenReturn(null);

    try {
      servlet.doGet(httpReq, httpResp);
      verify(httpResp).sendError(404, "No such entity.");
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void getOrdersDatastoreNoOrdersReturnsNull() {
    when(httpReq.getRequestURI()).thenReturn("/entry/orders");
    passPasswordCheck();

    when(dsClient.getOrders(gameIdTest, kingdomTest, turnTest)).thenReturn(Optional.empty());

    try {
      servlet.doGet(httpReq, httpResp);
      verify(dsClient).getOrders(gameIdTest, kingdomTest, turnTest);
      verify(httpResp).sendError(404, "No such entity.");
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void getOrdersDatastoreFoundOrders() {
    when(httpReq.getRequestURI()).thenReturn("/entry/orders");
    passPasswordCheck();

    Orders orders = mock(Orders.class);
    when(orders.getOrders()).thenReturn(new HashMap<>());
    when(dsClient.getOrders(gameIdTest, kingdomTest, turnTest)).thenReturn(Optional.of(orders));

    try {
      servlet.doGet(httpReq, httpResp);
      verify(dsClient).getOrders(gameIdTest, kingdomTest, turnTest);
      verify(httpResp).getOutputStream();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void getSetupDatastoreNoNationReturnsNull() {
    when(httpReq.getRequestURI()).thenReturn("/entry/setup");
    when(dsClient.getNation(gameIdTest, kingdomTest)).thenReturn(Optional.empty());

    try {
      servlet.doGet(httpReq, httpResp);
      verify(dsClient).getNation(gameIdTest, kingdomTest);
      verify(httpResp).sendError(404, "No such entity.");
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

  @Test
  public void getSetupDatastoreFoundNation() {
    when(httpReq.getRequestURI()).thenReturn("/entry/setup");

    Nation nation = new Nation();
    when(dsClient.getNation(gameIdTest, kingdomTest)).thenReturn(Optional.of(nation));

    try {
      servlet.doGet(httpReq, httpResp);
      verify(dsClient).getNation(gameIdTest, kingdomTest);
      verify(httpResp).getOutputStream();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }
}
