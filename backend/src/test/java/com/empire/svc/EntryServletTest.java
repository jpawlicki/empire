package com.empire.svc;

import com.empire.store.DatastoreClient;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class EntryServletTest {
  private EntryServlet servlet;
  private DatastoreClient dsClient;
  private LoginCache cache;
  private HttpServletRequest httpReq;
  private HttpServletResponse httpResp;
  private Request req;

  @Before
  public void setup() {
    dsClient = mock(DatastoreClient.class);
    cache = mock(LoginCache.class);
    httpReq = mock(HttpServletRequest.class);
    httpResp = mock(HttpServletResponse.class);
    req = mock(Request.class);
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
}
