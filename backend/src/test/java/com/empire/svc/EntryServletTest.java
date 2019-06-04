package com.empire.svc;

import com.empire.NationData;
import com.empire.World;
import com.empire.store.DatastoreClient;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
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
  private final byte[] pwHashTest = new byte[]{16, -37, -7, 12, -42, 50, 13, -53, 3, -49, 1, 29, 13, -82, -40, 25, -102, 35, 84,
      79, 100, -80, 107, 73, 57, 90, -24, 1, -31, 9, 41, 65};

  @Before
  public void setup() {
    req = mock(Request.class);
    when(req.getGameId()).thenReturn(gameIdTest);
    when(req.getKingdom()).thenReturn(kingdomTest);
    when(req.getTurn()).thenReturn(turnTest);

    dsClient = mock(DatastoreClient.class);
    cache = mock(LoginCache.class);

    httpReq = mock(HttpServletRequest.class);
    httpResp = mock(HttpServletResponse.class);

    servlet = new EntryServlet(dsClient, cache);

    RequestFactory.factory = mock(RequestFactory.class);

    try {
      when(RequestFactory.factory.fromHttpServletRequest(httpReq)).thenReturn(req);
    } catch (IOException e){
      fail("IOException when mocking RequestFactory conversion");
    }
  }

  @Test
  public void getOrdersPasswordFailReturnsNull(){
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
  public void getOrdersDatastoreNoOrdersReturnsNull(){
    when(httpReq.getRequestURI()).thenReturn("/entry/orders");
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

    when(dsClient.getOrders(gameIdTest, kingdomTest, turnTest)).thenReturn(Optional.empty());

    try {
      servlet.doGet(httpReq, httpResp);
      verify(dsClient).getOrders(gameIdTest, kingdomTest, turnTest);
      verify(httpResp).sendError(404, "No such entity.");
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }
}
