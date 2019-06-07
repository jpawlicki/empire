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
import org.mockito.Mockito;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EntryServletTest {
  private EntryServlet servlet;
  private EntryServletBackend backend;
  private HttpServletRequest httpReq;
  private HttpServletResponse httpResp;
  private ServletOutputStream os;
  private Request req;

  @Before
  public void setup() {
    httpReq = mock(HttpServletRequest.class);
    RequestFactory.factory = mock(RequestFactory.class);
    req = mock(Request.class);

    try {
      when(RequestFactory.factory.fromHttpServletRequest(httpReq)).thenReturn(req);
    } catch (IOException e) {
      fail("IOException when mocking RequestFactory.factory.fromHttpServletRequest()");
    }

    httpResp = mock(HttpServletResponse.class);
    os = mock(ServletOutputStream.class);

    try {
      when(httpResp.getOutputStream()).thenReturn(os);
    } catch (IOException e) {
      fail("IOException when mocking httpResp.getOutputStream");
    }

    backend = mock(EntryServletBackend.class);
    servlet = new EntryServlet(backend);
  }

  /** This is our defnition of success for the get methods */
  private void assertGetSuccess() throws IOException {
    verify(httpResp, never()).sendError(Mockito.anyInt(), Mockito.anyString());
    verify(httpResp).setStatus(200);
    verify(os).flush();
  }

  private void assertGetFailure() throws IOException {
    verify(httpResp).sendError(eq(404), Mockito.anyString());
  }

  @Test
  public void getWithBadPathReturnsFailureResponse() {
    when(httpReq.getRequestURI()).thenReturn("BOGUS");

    try {
      servlet.doGet(httpReq, httpResp);
      assertGetFailure();
    } catch (IOException e) {
      fail("IOException occurred during test");
    }
  }

  @Test
  public void successfulPingRequestReturnsSuccessResponse(){
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.pingRoute);

    try {
      servlet.doGet(httpReq, httpResp);
      assertGetSuccess();
    } catch (IOException e) {
      fail("IOException occurred during test");
    }
  }

  @Test
  public void getOrdersBackendEmptyReturnsFailureResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.ordersRoute);
    when(backend.getOrders(req)).thenReturn(Optional.empty());

    try {
      servlet.doGet(httpReq, httpResp);
      verify(backend).getOrders(req);
      assertGetFailure();
    } catch (IOException e) {
      fail("IOException occurred during test");
    }
  }

  @Test
  public void getOrdersBackendFoundReturnsSuccessResponse() {
    when(httpReq.getRequestURI()).thenReturn(EntryServlet.ordersRoute);

    Orders orders = mock(Orders.class);
    when(orders.getVersion()).thenReturn(42);
    when(orders.getOrders()).thenReturn(new HashMap<>());
    when(backend.getOrders(req)).thenReturn(Optional.of(orders));

    try {
      servlet.doGet(httpReq, httpResp);
      verify(backend).getOrders(req);
      assertGetSuccess();
    } catch (IOException e) {
      fail("IOException occurred");
    }
  }

//  @Test
//  public void getSetupDatastoreNoNationReturnsNull() {
//    when(httpReq.getRequestURI()).thenReturn(EntryServlet.setupRoute);
//    when(dsClient.getNation(gameIdTest, kingdomTest)).thenReturn(Optional.empty());
//
//    try {
//      servlet.doGet(httpReq, httpResp);
//      verify(dsClient).getNation(gameIdTest, kingdomTest);
//      verify(httpResp).sendError(404, "No such entity.");
//    } catch (IOException e) {
//      fail("IOException occurred");
//    }
//  }
//
//  @Test
//  public void getSetupDatastoreFoundNation() {
//    when(httpReq.getRequestURI()).thenReturn(EntryServlet.setupRoute);
//
//    Nation nation = new Nation();
//    when(dsClient.getNation(gameIdTest, kingdomTest)).thenReturn(Optional.of(nation));
//
//    try {
//      servlet.doGet(httpReq, httpResp);
//      verify(dsClient).getNation(gameIdTest, kingdomTest);
//      verify(httpResp).getOutputStream();
//    } catch (IOException e) {
//      fail("IOException occurred");
//    }
//  }
}
